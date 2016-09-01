package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SSHExecRunner implements AgentBuildRunner {

  private final InternalPropertiesHolder myInternalProperties;
  @NotNull
  private final AgentRunningBuildSshKeyManager mySshKeyManager;

  public SSHExecRunner(@NotNull final InternalPropertiesHolder holder,
                       @NotNull final AgentRunningBuildSshKeyManager sshKeyManager) {
    myInternalProperties = holder;
    mySshKeyManager = sshKeyManager;
  }

  @NotNull
  public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild,
                                         @NotNull final BuildRunnerContext context) throws RunBuildException {

    final SSHSessionProvider provider = new SSHSessionProvider(context, myInternalProperties, mySshKeyManager);
    final Map<String, String> parameters = context.getRunnerParameters();
    final String command = StringUtil.notNullize(parameters.get(SSHRunnerConstants.PARAM_COMMAND));
    final String pty = parameters.get(SSHRunnerConstants.PARAM_PTY);
    return new SSHExecProcessAdapter(provider, command, pty, runningBuild.getBuildLogger(), runningBuild.getFailBuildOnExitCode());
  }

  @NotNull
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new SSHExecRunnerInfo();
  }


}
