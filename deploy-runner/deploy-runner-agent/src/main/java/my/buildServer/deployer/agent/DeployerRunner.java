package my.buildServer.deployer.agent;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:26
 */
public class DeployerRunner implements AgentBuildRunner {

    @NotNull
    @Override
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) throws RunBuildException {

        final String username = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
        final String password = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);
        final String target = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
        final String sourcePath = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_SOURCE_PATH);

        return new BuildProcessAdapter() {
            @Override
            public void start() throws RunBuildException {
                // TODO implement connection
            }
        };
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new DeployerRunnerInfo();
    }

}
