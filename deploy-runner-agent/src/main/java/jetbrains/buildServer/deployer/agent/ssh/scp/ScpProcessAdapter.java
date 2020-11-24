/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.deployer.agent.ssh.scp;

import com.intellij.openapi.diagnostic.Logger;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.ssh.SSHSessionProvider;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.deployer.agent.DeployerAgentUtils.logBuildProblem;

public class ScpProcessAdapter extends SyncBuildProcessAdapter {

  private static final Logger LOG = Logger.getInstance(ScpProcessAdapter.class.getName());
  private final List<ArtifactsCollection> myArtifacts;
  private static final Logger myInternalLog = Logger.getInstance(ScpProcessAdapter.class.getName());


  private SSHSessionProvider mySessionProvider;

  public ScpProcessAdapter(@NotNull final BuildRunnerContext context,
                           @NotNull final List<ArtifactsCollection> artifactsCollections,
                           @NotNull final SSHSessionProvider sessionProvider) {
    super(context.getBuild().getBuildLogger());
    myArtifacts = artifactsCollections;
    mySessionProvider = sessionProvider;

  }

  @Override
  public BuildFinishedStatus runProcess() {
    String escapedRemotePath;
    Session session = null;

    try {

      escapedRemotePath = mySessionProvider.getRemotePath();
      session = mySessionProvider.getSession();

      if (isInterrupted()) return BuildFinishedStatus.FINISHED_FAILED;

      myLogger.message("Starting upload via SCP to " + mySessionProvider.getSessionString());


      final List<ArtifactsCollection> relativeDestinations = new LinkedList<ArtifactsCollection>();
      final List<ArtifactsCollection> absDestinations = new LinkedList<ArtifactsCollection>();
      boolean isRemoteBaseAbsolute = escapedRemotePath.startsWith("/");


      for (ArtifactsCollection artifactsCollection : myArtifacts) {
        if (artifactsCollection.getTargetPath().startsWith("/")) {
          absDestinations.add(new ArtifactsCollection(
              artifactsCollection.getSourcePath(),
              artifactsCollection.getTargetPath(),
              new HashMap<File, String>(artifactsCollection.getFilePathMap())
          ));
        } else {
          final Map<File, String> newPathMap = new HashMap<File, String>();
          for (Map.Entry<File, String> fileTargetEntry : artifactsCollection.getFilePathMap().entrySet()) {
            final String oldTarget = fileTargetEntry.getValue();
            newPathMap.put(fileTargetEntry.getKey(), escapedRemotePath + "/" + oldTarget);
          }
          final ArtifactsCollection newCollection = new ArtifactsCollection(artifactsCollection.getSourcePath(), artifactsCollection.getTargetPath(), newPathMap);
          if (isRemoteBaseAbsolute) {
            absDestinations.add(newCollection);
          } else {
            relativeDestinations.add(newCollection);
          }

        }
      }

      upload(session, ".", relativeDestinations);
      upload(session, "/", absDestinations);

      return BuildFinishedStatus.FINISHED_SUCCESS;
    } catch (JSchException e) {
      logBuildProblem(myLogger, e.getMessage());
      LOG.warnAndDebugDetails("Error executing SCP command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } catch (IOException e) {
      logBuildProblem(myLogger, e.getMessage());
      LOG.warnAndDebugDetails("Error executing SCP command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } finally {
      if (session != null) {
        session.disconnect();
      }
    }
  }

  private void upload(final @NotNull Session session,
                      final @NotNull String escapedRemoteBase,
                      final @NotNull List<ArtifactsCollection> artifacts) throws IOException, JSchException {

    assert session.isConnected();

    // skip empty collections
    if (artifacts.size() == 0) {
      return;
    }

    // exec 'scp -prt <remoteBase>' remotely
    final String command = "scp -prt " + (StringUtil.isEmptyOrSpaces(escapedRemoteBase) ? "." : escapedRemoteBase);
    final ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
    execChannel.setCommand(command);

    // get I/O streams for remote scp
    final OutputStream out = execChannel.getOutputStream();
    final InputStream in = execChannel.getInputStream();

    execChannel.connect();
    ScpExecUtil.checkScpAck(in);

    try {
      for (ArtifactsCollection artifactCollection : artifacts) {
        int count = 0;
        for (Map.Entry<File, String> filePathEntry : artifactCollection.getFilePathMap().entrySet()) {
          final File source = filePathEntry.getKey();
          final String destination = filePathEntry.getValue();
          final ScpOperation operationChain = ScpOperationBuilder.getCopyFileOperation(source, destination);
          myInternalLog.debug("Transferring [" + source.getAbsolutePath() + "] to [" + destination + "]");
          checkIsInterrupted();
          operationChain.execute(out, in);
          myInternalLog.debug("done transferring [" + source.getAbsolutePath() + "]");
          count++;
        }
        myLogger.message("Uploaded [" + count + "] files for [" + artifactCollection.getSourcePath() + "] pattern");
      }
    } finally {
      FileUtil.close(out);
      FileUtil.close(in);
      execChannel.disconnect();
    }
  }
}
