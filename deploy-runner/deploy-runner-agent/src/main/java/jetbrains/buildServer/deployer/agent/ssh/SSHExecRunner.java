package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;

public class SSHExecRunner implements AgentBuildRunner {

    @NotNull
    @Override
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {

        final String username = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_USERNAME);
        final String password = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_PASSWORD);
        final String host = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_HOST);
        final String command = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_COMMAND);

        final String portStr = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_PORT);
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            port = 22;
        }


        return new SSHExecProcessAdapter(host, port, username, password, command, runningBuild.getBuildLogger());
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new SSHExecRunnerInfo();
    }


}
