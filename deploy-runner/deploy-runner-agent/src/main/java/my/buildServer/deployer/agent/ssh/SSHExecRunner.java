package my.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import my.buildServer.deployer.common.SSHExecRunnerConstants;
import org.jetbrains.annotations.NotNull;

public class SSHExecRunner implements AgentBuildRunner {

    @NotNull
    @Override
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {

        final String username = context.getRunnerParameters().get(SSHExecRunnerConstants.PARAM_USERNAME);
        final String password = context.getRunnerParameters().get(SSHExecRunnerConstants.PARAM_PASSWORD);
        final String host = context.getRunnerParameters().get(SSHExecRunnerConstants.PARAM_HOST);
        final String command = context.getRunnerParameters().get(SSHExecRunnerConstants.PARAM_COMMAND);

        return new SSHExecProcessAdapter(host, username, password, command, runningBuild.getBuildLogger());
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new SSHExecRunnerInfo();
    }


}
