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

package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class FtpDeployerRunner extends BaseDeployerRunner {

  public FtpDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
    super(extensionHolder);
  }


  @Override
  protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                            @NotNull final String username,
                                            @NotNull final String password,
                                            @NotNull final String target,
                                            @NotNull final List<ArtifactsCollection> artifactsCollections) throws RunBuildException {
    final Map<String, String> runnerParameters = context.getRunnerParameters();
    final String authMethod = runnerParameters.get(FTPRunnerConstants.PARAM_AUTH_METHOD);

    if (FTPRunnerConstants.AUTH_METHOD_USER_PWD.equals(authMethod)) {
      return new FtpBuildProcessAdapter(context, target, username, password, artifactsCollections);
    } else if (FTPRunnerConstants.AUTH_METHOD_ANONYMOUS.equals(authMethod)) {
      return new FtpBuildProcessAdapter(context, target, "anonymous", " ", artifactsCollections);
    } else {
      throw new RunBuildException("Unknown FTP authentication method: [" + authMethod + "]");
    }

  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new FtpDeployerRunnerInfo();
  }


}
