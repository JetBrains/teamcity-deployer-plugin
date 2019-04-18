package jetbrains.buildServer.deployer.agent.ssh;

class SSHProcessAdapterOptions {
  private boolean myEnableSshAgentForwarding;
  private boolean myFailBuildOnExitCode;

  SSHProcessAdapterOptions(boolean myEnableSshAgentForwarding, boolean myFailBuildOnExitCode) {
    this.myEnableSshAgentForwarding = myEnableSshAgentForwarding;
    this.myFailBuildOnExitCode = myFailBuildOnExitCode;
  }

  boolean shouldFailBuildOnExitCode() {
    return myFailBuildOnExitCode;
  }

  boolean enableSshAgentForwarding() {
    return myEnableSshAgentForwarding;
  }
}
