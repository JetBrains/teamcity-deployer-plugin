/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.deployer.agent.ssh.sftp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.jcraft.jsch.*;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.DeployerAgentUtils;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.UploadInterruptedException;
import jetbrains.buildServer.deployer.agent.ssh.JSchBuildLogger;
import jetbrains.buildServer.deployer.agent.ssh.SSHSessionProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;


public class SftpBuildProcessAdapter extends SyncBuildProcessAdapter {

  private static final Logger LOG = Logger.getInstance(SftpBuildProcessAdapter.class.getName());
  private final List<ArtifactsCollection> myArtifacts;
  private SSHSessionProvider mySessionProvider;

  public SftpBuildProcessAdapter(@NotNull final BuildRunnerContext context,
                                 @NotNull final List<ArtifactsCollection> artifactsCollections,
                                 @NotNull final SSHSessionProvider sessionProvider) {
    super(context.getBuild().getBuildLogger());
    myArtifacts = artifactsCollections;
    mySessionProvider = sessionProvider;
  }

  @Override
  public BuildFinishedStatus runProcess() {
    final String escapedRemotePath;
    Session session = null;

    JSch.setLogger(new JSchBuildLogger(myLogger));

    try {
      escapedRemotePath = mySessionProvider.getRemotePath();
      session = mySessionProvider.getSession();

      if (isInterrupted()) return BuildFinishedStatus.FINISHED_FAILED;

      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();

      if (StringUtil.isNotEmpty(escapedRemotePath)) {
        createRemotePath(channel, escapedRemotePath);
        channel.cd(escapedRemotePath);
      }

      myLogger.message("Starting upload via SFTP to " + mySessionProvider.getSessionString());
      final String baseDir = channel.pwd();
      for (ArtifactsCollection artifactsCollection : myArtifacts) {
        int count = 0;
        for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
          checkIsInterrupted();
          final File source = fileStringEntry.getKey();
          final String value = fileStringEntry.getValue();
          final String destinationPath = "".equals(value) ? "." : value;
          createRemotePath(channel, destinationPath);
          LOG.debug("Transferring [" + source.getAbsolutePath() + "] to [" + destinationPath + "] under [" + baseDir + "]");
          channel.put(source.getAbsolutePath(), destinationPath);
          LOG.debug("done transferring [" + source.getAbsolutePath() + "]");
          count++;
        }
        myLogger.message("Uploaded [" + count + "] files for [" + artifactsCollection.getSourcePath() + "] pattern");
      }
      channel.disconnect();
      return BuildFinishedStatus.FINISHED_SUCCESS;
    } catch (UploadInterruptedException e) {
      myLogger.warning("SFTP upload interrupted.");
      return BuildFinishedStatus.FINISHED_FAILED;
    } catch (JSchException e) {
      DeployerAgentUtils.logBuildProblem(myLogger, e.getMessage());
      LOG.warnAndDebugDetails("Error executing SFTP command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } catch (SftpException e) {
      DeployerAgentUtils.logBuildProblem(myLogger, e.getMessage());
      LOG.warnAndDebugDetails("Error executing SFTP command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } finally {
      if (session != null) {
        session.disconnect();
      }

      JSch.setLogger(null);
    }
  }

  private void createRemotePath(@NotNull final ChannelSftp channel,
                                @NotNull final String destination) throws SftpException {
    final int endIndex = destination.lastIndexOf('/');
    if (endIndex > 0) {
      createRemotePath(channel, destination.substring(0, endIndex));
    }
    try {
      channel.stat(destination);
    } catch (SftpException e) {
      // dir does not exist.
      if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
        channel.mkdir(destination);
      }
    }

  }

}
