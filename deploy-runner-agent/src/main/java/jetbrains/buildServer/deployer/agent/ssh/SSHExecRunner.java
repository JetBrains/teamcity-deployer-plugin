

package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.ssh.SshKnownHostsManager;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SSHExecRunner implements AgentBuildRunner {

  private final InternalPropertiesHolder myInternalProperties;
  @NotNull
  private final AgentRunningBuildSshKeyManager mySshKeyManager;
  @NotNull
  private final SshKnownHostsManager myKnownHostsManager;

  public SSHExecRunner(@NotNull final InternalPropertiesHolder holder,
                       @NotNull final AgentRunningBuildSshKeyManager sshKeyManager,
                       @NotNull final SshKnownHostsManager sshKnownHostsManager) {
    myInternalProperties = holder;
    mySshKeyManager = sshKeyManager;
    myKnownHostsManager = sshKnownHostsManager;
  }

  @NotNull
  public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild,
                                         @NotNull final BuildRunnerContext context) throws RunBuildException {

    final SSHSessionProvider provider = new SSHSessionProvider(context, myInternalProperties, mySshKeyManager, myKnownHostsManager);
    final Map<String, String> parameters = context.getRunnerParameters();
    final String command = StringUtil.notNullize(parameters.get(SSHRunnerConstants.PARAM_COMMAND));
    final String pty = parameters.get(SSHRunnerConstants.PARAM_PTY);
    boolean enableSshAgentForwarding =
            StringUtil.isTrue(runningBuild.getSharedConfigParameters().get(SSHRunnerConstants.ENABLE_SSH_AGENT_FORWARDING));
    SSHProcessAdapterOptions options =
            new SSHProcessAdapterOptions(runningBuild.getFailBuildOnExitCode(), enableSshAgentForwarding);

    return new SSHExecProcessAdapter(provider, command, pty, runningBuild.getBuildLogger(), options);
  }

  @NotNull
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new SSHExecRunnerInfo();
  }
}