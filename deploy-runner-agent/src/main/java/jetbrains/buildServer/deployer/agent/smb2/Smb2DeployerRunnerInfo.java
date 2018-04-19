package jetbrains.buildServer.deployer.agent.smb2;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.impl.BuildAgentUtil;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;


class Smb2DeployerRunnerInfo implements AgentBuildRunnerInfo {
  private static final Logger LOG = Logger.getInstance(Smb2DeployerRunnerInfo.class.getName());
  @NotNull
  @Override
  public String getType() {
    return DeployerRunnerConstants.SMB2_RUN_TYPE;
  }

  @Override
  public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
    final boolean java7 = SystemInfo.isJavaVersionAtLeast("1.7.0");
    if (!java7) {
      LOG.warn("SMB2 deployer can not be used. It requires JVM 7, but teamcity agent is running using version [" + SystemInfo.JAVA_VERSION + "]" +
              " please update the jvm");
      return false;
    }
    return true;
  }
}
