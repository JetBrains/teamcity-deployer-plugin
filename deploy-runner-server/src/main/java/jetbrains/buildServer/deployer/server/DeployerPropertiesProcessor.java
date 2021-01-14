/*
 * Copyright 2000-2021 JetBrains s.r.o.
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
