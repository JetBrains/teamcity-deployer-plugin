

package jetbrains.buildServer.deployer.agent.ssh;

class SSHProcessAdapterOptions {
  private boolean myFailBuildOnExitCode;
  private boolean myEnableSshAgentForwarding;

  SSHProcessAdapterOptions(boolean myFailBuildOnExitCode, boolean myEnableSshAgentForwarding) {
    this.myFailBuildOnExitCode = myFailBuildOnExitCode;
    this.myEnableSshAgentForwarding = myEnableSshAgentForwarding;
  }

  boolean shouldFailBuildOnExitCode() {
    return myFailBuildOnExitCode;
  }

  boolean enableSshAgentForwarding() {
    return myEnableSshAgentForwarding;
  }
}