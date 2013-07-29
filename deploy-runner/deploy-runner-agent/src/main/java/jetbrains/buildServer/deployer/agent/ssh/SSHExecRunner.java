package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.JSchException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class SSHExecRunner implements AgentBuildRunner {

    @NotNull
    @Override
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {

        final SSHSessionProvider provider;
        try {
            provider = new SSHSessionProvider(context);
        } catch (JSchException e) {
            throw new RunBuildException(e);
        }

        final String command = StringUtil.notNullize(context.getRunnerParameters().get(SSHRunnerConstants.PARAM_COMMAND));
        return new SSHExecProcessAdapter(provider, command, runningBuild.getBuildLogger());
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new SSHExecRunnerInfo();
    }


}
