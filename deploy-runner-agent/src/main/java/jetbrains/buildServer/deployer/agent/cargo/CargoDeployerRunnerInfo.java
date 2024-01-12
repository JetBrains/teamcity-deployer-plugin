

package jetbrains.buildServer.deployer.agent.cargo;

import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;


class CargoDeployerRunnerInfo implements AgentBuildRunnerInfo {

  @NotNull
  @Override
  public String getType() {
    return DeployerRunnerConstants.CARGO_RUN_TYPE;
  }

  @Override
  public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
    return true;
  }
}