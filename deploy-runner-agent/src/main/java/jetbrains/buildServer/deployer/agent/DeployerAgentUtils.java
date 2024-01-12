

package jetbrains.buildServer.deployer.agent;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;

public class DeployerAgentUtils {
  public static void logBuildProblem(BuildProgressLogger logger, String message) {
    logger.logBuildProblem(BuildProblemData
            .createBuildProblem(String.valueOf(message.hashCode()),
                    DeployerRunnerConstants.BUILD_PROBLEM_TYPE,
                    "Deployment problem: " + message));
  }
}