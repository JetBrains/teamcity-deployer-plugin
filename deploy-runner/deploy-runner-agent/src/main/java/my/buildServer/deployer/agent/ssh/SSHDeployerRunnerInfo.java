package my.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

/**
* Created by Kit
* Date: 24.03.12 - 17:31
*/
class SSHDeployerRunnerInfo implements AgentBuildRunnerInfo {
    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.SCP_RUN_TYPE;
    }

    @Override
    public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
        return true;
    }
}
