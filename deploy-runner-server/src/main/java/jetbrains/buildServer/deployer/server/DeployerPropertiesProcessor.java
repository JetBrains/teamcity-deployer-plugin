

package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.StringUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:16
 */
public class DeployerPropertiesProcessor implements PropertiesProcessor {
  @Override
  public Collection<InvalidProperty> process(Map<String, String> properties) {
    Collection<InvalidProperty> result = new HashSet<InvalidProperty>();
    if (StringUtil.isEmptyOrSpaces(properties.get(DeployerRunnerConstants.PARAM_TARGET_URL))) {
      result.add(new InvalidProperty(DeployerRunnerConstants.PARAM_TARGET_URL, "The target must be specified."));
    }

    if (StringUtil.isEmptyOrSpaces(properties.get(DeployerRunnerConstants.PARAM_SOURCE_PATH))) {
      result.add(new InvalidProperty(DeployerRunnerConstants.PARAM_SOURCE_PATH, "Artifact to deploy must be specified"));
    }
    return result;
  }
}