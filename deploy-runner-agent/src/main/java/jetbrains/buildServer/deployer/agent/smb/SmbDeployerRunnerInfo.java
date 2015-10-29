package jetbrains.buildServer.deployer.agent.smb;

import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;


class SmbDeployerRunnerInfo implements AgentBuildRunnerInfo {
  @NotNull
  @Override
  public String getType() {
    return DeployerRunnerConstants.SMB_RUN_TYPE;
  }

  @Override
  public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
    return true;
  }
}
