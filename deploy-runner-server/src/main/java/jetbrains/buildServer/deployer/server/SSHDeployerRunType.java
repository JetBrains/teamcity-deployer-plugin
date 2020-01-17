/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SSHDeployerRunType extends RunType {

  private final PluginDescriptor myDescriptor;

  public SSHDeployerRunType(@NotNull final RunTypeRegistry registry,
                            @NotNull final PluginDescriptor descriptor) {
    myDescriptor = descriptor;
    registry.registerRunType(this);
  }

  @NotNull
  @Override
  public String getType() {
    return DeployerRunnerConstants.SSH_RUN_TYPE;
  }

  @Override
  public String getDisplayName() {
    return "SSH Upload";
  }

  @Override
  public String getDescription() {
    return "Deploys files/directories via SSH";
  }

  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new SSHDeployerPropertiesProcessor();
  }

  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath() + "editSSHDeployerParams.jsp";
  }

  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath() + "viewSSHDeployerParams.jsp";
  }

  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    return new HashMap<String, String>();
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> parameters) {
    StringBuilder sb = new StringBuilder();
    sb.append("Target: ").append(parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL));
    final String port = parameters.get(SSHRunnerConstants.PARAM_PORT);
    if (StringUtil.isNotEmpty(port)) {
      sb.append('\n').append(" Port: ").append(port);
    }
    final Map<String, String> transportTypeValues = new SSHRunnerConstants().getTransportTypeValues();
    sb.append('\n').append("Protocol: ").append(transportTypeValues.get(parameters.get(SSHRunnerConstants.PARAM_TRANSPORT)));
    return sb.toString();
  }

}
