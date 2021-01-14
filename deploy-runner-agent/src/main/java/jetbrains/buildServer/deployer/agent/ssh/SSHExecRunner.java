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

package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SSHExecRunner implements AgentBuildRunner {

  private final InternalPropertiesHolder myInternalProperties;
  @NotNull
  private final AgentRunningBuildSshKeyManager mySshKeyManager;

  public SSHExecRunner(@NotNull final InternalPropertiesHolder holder,
                       @NotNull final AgentRunningBuildSshKeyManager sshKeyManager) {
    myInternalProperties = holder;
    mySshKeyManager = sshKeyManager;
  }

  @NotNull
  public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild,
                                         @NotNull final BuildRunnerContext context) throws RunBuildException {

    final SSHSessionProvider provider = new SSHSessionProvider(context, myInternalProperties, mySshKeyManager);
    final Map<String, String> parameters = context.getRunnerParameters();
    final String command = StringUtil.notNullize(parameters.get(SSHRunnerConstants.PARAM_COMMAND));
    final String pty = parameters.get(SSHRunnerConstants.PARAM_PTY);
    boolean enableSshAgentForwarding =
            StringUtil.isTrue(runningBuild.getSharedConfigParameters().get(SSHRunnerConstants.ENABLE_SSH_AGENT_FORWARDING));
    SSHProcessAdapterOptions options =
            new SSHProcessAdapterOptions(runningBuild.getFailBuildOnExitCode(), enableSshAgentForwarding);

    return new SSHExecProcessAdapter(provider, command, pty, runningBuild.getBuildLogger(), options);
  }

  @NotNull
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new SSHExecRunnerInfo();
  }
}

