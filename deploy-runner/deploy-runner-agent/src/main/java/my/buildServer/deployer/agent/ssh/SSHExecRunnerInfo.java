package my.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import my.buildServer.deployer.common.SSHRunnerConstants;
import my.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;


class SSHExecRunnerInfo implements AgentBuildRunnerInfo {
    @NotNull
    @Override
    public String getType() {
        return SSHRunnerConstants.SSH_EXEC_RUN_TYPE;
    }

    @Override
    public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
        return true;
    }
}
