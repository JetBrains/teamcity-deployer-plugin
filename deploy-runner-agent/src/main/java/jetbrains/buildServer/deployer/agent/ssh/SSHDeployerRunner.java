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

package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.InternalPropertiesHolder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
import jetbrains.buildServer.deployer.agent.ssh.scp.ScpProcessAdapter;
import jetbrains.buildServer.deployer.agent.ssh.sftp.SftpBuildProcessAdapter;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:26
 */
public class SSHDeployerRunner extends BaseDeployerRunner {


  @NotNull
  private final InternalPropertiesHolder myInternalProperties;
  @NotNull
  private final AgentRunningBuildSshKeyManager mySshKeyManager;

  public SSHDeployerRunner(@NotNull final ExtensionHolder extensionHolder,
                           @NotNull final InternalPropertiesHolder holder,
                           @NotNull final AgentRunningBuildSshKeyManager sshKeyManager) {
    super(extensionHolder);
    myInternalProperties = holder;
    mySshKeyManager = sshKeyManager;
  }

  @Override
  protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                            @NotNull final String username,
                                            @NotNull final String password,
                                            @NotNull final String target,
                                            @NotNull final List<ArtifactsCollection> artifactsCollections) throws RunBuildException {

    final SSHSessionProvider provider = new SSHSessionProvider(context, myInternalProperties, mySshKeyManager);
    final String transport = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_TRANSPORT);
    if (SSHRunnerConstants.TRANSPORT_SCP.equals(transport)) {
      return new ScpProcessAdapter(context, artifactsCollections, provider);
    } else if (SSHRunnerConstants.TRANSPORT_SFTP.equals(transport)) {
      return new SftpBuildProcessAdapter(context, artifactsCollections, provider);
    } else {
      throw new RunBuildException("Unknown ssh transport [" + transport + "]");
    }
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new SSHDeployerRunnerInfo();
  }


}
