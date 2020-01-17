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

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FtpDeployerRunType extends RunType {

  private final PluginDescriptor myDescriptor;

  public FtpDeployerRunType(@NotNull final RunTypeRegistry registry,
                            @NotNull final PluginDescriptor descriptor) {
    registry.registerRunType(this);
    myDescriptor = descriptor;
  }

  @NotNull
  @Override
  public String getType() {
    return DeployerRunnerConstants.FTP_RUN_TYPE;
  }

  @Override
  public String getDisplayName() {
    return "FTP Upload";
  }

  @Override
  public String getDescription() {
    return "Deploys files/directories via FTP";
  }

  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new DeployerPropertiesProcessor();
  }

  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath() + "editFtpDeployerParams.jsp";
  }

  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath() + "viewFtpDeployerParams.jsp";
  }

  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    final HashMap<String, String> defaults = new HashMap<String, String>();
    defaults.put(FTPRunnerConstants.PARAM_FTP_MODE, "PASSIVE");
    return defaults;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> parameters) {
    StringBuilder sb = new StringBuilder();
    sb.append("Target FTP server: ").append(parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL));
    return sb.toString();
  }
}
