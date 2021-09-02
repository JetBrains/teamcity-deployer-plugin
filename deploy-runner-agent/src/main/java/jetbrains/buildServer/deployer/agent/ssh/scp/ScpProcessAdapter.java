/*
 * Copyright 2000-2021 JetBrains s.r.o.
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


      final List<ArtifactsCollection> relativeDestinations = new LinkedList<>();
      final List<ArtifactsCollection> absDestinations = new LinkedList<>();
      boolean isRemoteBaseAbsolute = escapedRemotePath.startsWith("/");

      String escapedRemotePathFromDrive = escapedRemotePath;
      List<String> filePath = Stream.of(escapedRemotePath.replace('\\', '/').split("\\/"))
              .filter(it -> !it.isEmpty()).collect(Collectors.toList());
      String escapedRemoteBase = isRemoteBaseAbsolute ? "/" : ".";
      if (filePath.size() > 0 && filePath.get(0).matches("\\w\\:")) {
        // cases to of specific windows drive, like C:
        escapedRemoteBase = "/" + filePath.get(0);
        isRemoteBaseAbsolute = true;
        if (filePath.size() > 1) {
          List<String> remotePathNoDrive = filePath.subList(1, filePath.size());
          escapedRemotePathFromDrive = String.join("/", remotePathNoDrive);
        } else
          escapedRemotePathFromDrive = ".";
      }

      for (ArtifactsCollection artifactsCollection : myArtifacts) {
        if (artifactsCollection.getTargetPath().startsWith("/")) {
          absDestinations.add(new ArtifactsCollection(
              artifactsCollection.getSourcePath(),
              artifactsCollection.getTargetPath(),
              new HashMap<>(artifactsCollection.getFilePathMap())
          ));
        } else {
          final Map<File, String> newPathMap = new HashMap<>();
          for (Map.Entry<File, String> fileTargetEntry : artifactsCollection.getFilePathMap().entrySet()) {
            final String oldTarget = fileTargetEntry.getValue();
            newPathMap.put(fileTargetEntry.getKey(), escapedRemotePathFromDrive + "/" + oldTarget);
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
      upload(session, escapedRemoteBase, absDestinations);

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
