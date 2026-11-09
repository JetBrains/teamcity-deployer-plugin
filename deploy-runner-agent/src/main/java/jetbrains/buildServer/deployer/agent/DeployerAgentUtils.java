

package jetbrains.buildServer.deployer.agent;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

public class DeployerAgentUtils {
  public static void logBuildProblem(BuildProgressLogger logger, String message) {
    logger.logBuildProblem(BuildProblemData
            .createBuildProblem(String.valueOf(message.hashCode()),
                    DeployerRunnerConstants.BUILD_PROBLEM_TYPE,
                    "Deployment problem: " + message));
  }

  /** Rejects a remote name whose control characters (e.g. CR/LF) could inject commands into the line-oriented FTP or SCP protocol. */
  public static void checkHasNoControlCharacters(@NotNull final String name,
                                                 @NotNull final String description) throws UnsafeRemoteNameException {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isISOControl(name.charAt(i))) {
        throw new UnsafeRemoteNameException("Refusing to deploy: " + description + " contains a control character " +
                                            "that could inject commands into the remote connection.");
      }
    }
  }
}
