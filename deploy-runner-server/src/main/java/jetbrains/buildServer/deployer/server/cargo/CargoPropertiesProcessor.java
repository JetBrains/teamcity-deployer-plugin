

package jetbrains.buildServer.deployer.server.cargo;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.server.DeployerPropertiesProcessor;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.util.StringUtil;

import java.util.Collection;
import java.util.Map;

/**
 * Created by Nikita.Skvortsov
 * date: 15.07.2016.
 */
class CargoPropertiesProcessor extends DeployerPropertiesProcessor {
  @Override
  public Collection<InvalidProperty> process(Map<String, String> properties) {
    Collection<InvalidProperty> result = super.process(properties);

    if (StringUtil.isEmptyOrSpaces(properties.get(DeployerRunnerConstants.PARAM_USERNAME))) {
      result.add(new InvalidProperty(DeployerRunnerConstants.PARAM_USERNAME, "Username must be specified."));
    }

    if (StringUtil.isEmptyOrSpaces(properties.get(DeployerRunnerConstants.PARAM_PASSWORD))) {
      result.add(new InvalidProperty(DeployerRunnerConstants.PARAM_PASSWORD, "Password must be specified"));
    }

    return result;
  }
}