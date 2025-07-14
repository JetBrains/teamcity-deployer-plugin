package jetbrains.buildServer.deployer.server;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.ssh.BeforeBuildStartSshKnownHostsProcessor;
import org.jetbrains.annotations.NotNull;

public class DeployerKnownHostsPreProcessor implements BeforeBuildStartSshKnownHostsProcessor {
  @NotNull
  @Override
  public Collection<String> getSshHostAndPort(@NotNull SRunningBuild runningBuild) {
    return runningBuild.getBuildPromotion().getBuildSettings().getBuildRunners().stream()
                .filter(runner -> runner.getType().equals(SSHRunnerConstants.SSH_EXEC_RUN_TYPE) || runner.getType().equals(DeployerRunnerConstants.SSH_RUN_TYPE))
                .map(runner -> processParameters(runner.getParameters()))
                .filter(Objects::nonNull).collect(Collectors.toList());
  }

  private static String processParameters(@NotNull Map<String, String> parameters) {
    String url = parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL);
    String portStr = parameters.get(SSHRunnerConstants.PARAM_PORT);
    if (url == null) {
      return null;
    }

    int port = 22;
    if (portStr != null) {
      port = Integer.parseInt(portStr);
    }
    String host;
    final int delimiterIndex = url.indexOf(':');
    // remove port part
    if (delimiterIndex > 0) {
      host = url.substring(0, delimiterIndex);
    } else {
      host = url;
    }

    return host + ":" + port;
  }
}
