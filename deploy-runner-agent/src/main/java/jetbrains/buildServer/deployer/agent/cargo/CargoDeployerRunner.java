package jetbrains.buildServer.deployer.agent.cargo;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

public class CargoDeployerRunner implements AgentBuildRunner {

  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {

    final String username = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
    final String password = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);
    final String target = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
    final String sourcePath = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_SOURCE_PATH);

    return new CargoBuildProcessAdapter(target, username, password, context, sourcePath);
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new CargoDeployerRunnerInfo();
  }


}
