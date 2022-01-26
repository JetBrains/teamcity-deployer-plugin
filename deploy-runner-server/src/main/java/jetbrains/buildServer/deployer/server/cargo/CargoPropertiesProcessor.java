/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
