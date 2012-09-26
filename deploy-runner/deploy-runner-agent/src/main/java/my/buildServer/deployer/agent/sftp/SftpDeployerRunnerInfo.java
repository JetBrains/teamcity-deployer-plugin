package my.buildServer.deployer.agent.sftp;

import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;


class SftpDeployerRunnerInfo implements AgentBuildRunnerInfo {
    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.SFTP_RUN_TYPE;
    }

    @Override
    public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
        return true;
    }
}
