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

package jetbrains.buildServer.deployer.server;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SSHExecRunType extends RunType {

  private final PluginDescriptor myDescriptor;

  public SSHExecRunType(@NotNull final RunTypeRegistry registry,
                        @NotNull final PluginDescriptor descriptor) {
    registry.registerRunType(this);
    myDescriptor = descriptor;
  }

  @NotNull
  @Override
  public String getType() {
    return SSHRunnerConstants.SSH_EXEC_RUN_TYPE;
  }

  @Override
  public String getDisplayName() {
    return "SSH Exec";
  }

  @Override
  public String getDescription() {
    return "Runner able to execute commands over SSH";
  }

  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new SSHDeployerPropertiesProcessor() {
      @Override
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        final Collection<InvalidProperty> invalidProperties = new HashSet<InvalidProperty>();
        if (jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces(properties.get(DeployerRunnerConstants.PARAM_USERNAME)) &&
            !SSHRunnerConstants.AUTH_METHOD_DEFAULT_KEY.equals(properties.get(SSHRunnerConstants.PARAM_AUTH_METHOD))) {
          invalidProperties.add(new InvalidProperty(DeployerRunnerConstants.PARAM_USERNAME, "Username must be specified."));
        }

        if (jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces(properties.get(DeployerRunnerConstants.PARAM_TARGET_URL))) {
          invalidProperties.add(new InvalidProperty(DeployerRunnerConstants.PARAM_TARGET_URL, "The target must be specified."));
        }

        if (jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces(properties.get(SSHRunnerConstants.PARAM_COMMAND))) {
          invalidProperties.add(new InvalidProperty(SSHRunnerConstants.PARAM_COMMAND, "Remote command must be specified"));
        }
        return invalidProperties;
      }
    };
  }

  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath() + "editSSHExecParams.jsp";
  }

  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myDescriptor.getPluginResourcesPath() + "viewSSHExecParams.jsp";
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
    sb.append('\n');
    final String commands = parameters.get(SSHRunnerConstants.PARAM_COMMAND);
    if (commands != null) {
      final List<String> commandsList = Arrays.asList(commands.split("\\\\n"));
      final int size = commandsList.size();
      if (size > 0) {
        sb.append("Commands: ").append(commandsList.get(0));
        if (size > 1) {
          sb.append(" <and ").append(size - 1).append(" more line").append(size > 2 ? "s" : "").append(">");
        }
        return sb.toString();
      }
    }
    return sb.append("No commands defined").toString();
  }
}
