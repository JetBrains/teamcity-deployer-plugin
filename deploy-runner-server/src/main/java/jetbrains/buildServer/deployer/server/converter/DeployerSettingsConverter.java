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

package jetbrains.buildServer.deployer.server.converter;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nikita.Skvortsov
 * date: 29.07.13.
 */
public class DeployerSettingsConverter extends BuildServerAdapter {

  private final SBuildServer myServer;

  public DeployerSettingsConverter(@NotNull SBuildServer server) {
    myServer = server;

    if (TeamCityProperties.getBoolean("teamcity.deployer.passwordParametersConverter.enabled")) {
      myServer.addListener(this);
    }
  }


  @Override
  public void buildTypeRegistered(@NotNull SBuildType buildType) {
    boolean persistBuildType = false;
    for (SBuildRunnerDescriptor descriptor : buildType.getBuildRunners()) {
      boolean runnerUpdated = false;
      final String runnerType = descriptor.getType();
      final Map<String, String> newRunnerParams = new HashMap<String, String>(descriptor.getParameters());

      final String plainPassword = newRunnerParams.get(DeployerRunnerConstants.PARAM_PLAIN_PASSWORD);
      if (plainPassword != null) {
        runnerUpdated = true;
        Loggers.SERVER.debug("Scrambling password for runner [" + runnerType + "-" + descriptor.getName() + "] in [" + buildType.getName() + "]");
        newRunnerParams.remove(DeployerRunnerConstants.PARAM_PLAIN_PASSWORD);
        newRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, plainPassword);
      }

      if (DeployerRunnerConstants.SSH_RUN_TYPE.equals(runnerType) ||
          SSHRunnerConstants.SSH_EXEC_RUN_TYPE.equals(runnerType)) {
        final String sshAuthMethod = newRunnerParams.get(SSHRunnerConstants.PARAM_AUTH_METHOD);
        if (StringUtil.isEmpty(sshAuthMethod)) {
          runnerUpdated = true;
          Loggers.SERVER.debug("Setting default (username password) ssh authentication method for runner [" + runnerType + "-" + descriptor.getName() + "] in [" + buildType.getName() + "]");
          newRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_USERNAME_PWD);
        }

        if (SSHRunnerConstants.SSH_EXEC_RUN_TYPE.equals(runnerType)) {
          final String oldUsername = newRunnerParams.get(SSHRunnerConstants.PARAM_USERNAME);
          final String oldPassword = newRunnerParams.get(SSHRunnerConstants.PARAM_PASSWORD);
          final String oldHost = newRunnerParams.get(SSHRunnerConstants.PARAM_HOST);
          if (StringUtil.isNotEmpty(oldUsername)) {
            runnerUpdated = true;
            newRunnerParams.remove(SSHRunnerConstants.PARAM_USERNAME);
            newRunnerParams.put(DeployerRunnerConstants.PARAM_USERNAME, oldUsername);
          }
          if (StringUtil.isNotEmpty(oldPassword)) {
            runnerUpdated = true;
            newRunnerParams.remove(SSHRunnerConstants.PARAM_PASSWORD);
            newRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, oldPassword);
          }
          if (StringUtil.isNotEmpty(oldHost)) {
            runnerUpdated = true;
            newRunnerParams.remove(SSHRunnerConstants.PARAM_HOST);
            newRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, oldHost);
          }
        }
      }

      if (DeployerRunnerConstants.FTP_RUN_TYPE.equals(runnerType)) {
        final String ftpAuthMethod = newRunnerParams.get(FTPRunnerConstants.PARAM_AUTH_METHOD);
        if (StringUtil.isEmpty(ftpAuthMethod)) {
          runnerUpdated = true;
          Loggers.SERVER.debug("Setting ftp auth authentication method for runner [" + runnerType + "-" + descriptor.getName() + "] in [" + buildType.getName() + "]");
          final String username = newRunnerParams.get(DeployerRunnerConstants.PARAM_USERNAME);
          if (StringUtil.isEmpty(username)) {
            newRunnerParams.put(FTPRunnerConstants.PARAM_AUTH_METHOD, FTPRunnerConstants.AUTH_METHOD_ANONYMOUS);
          } else {
            newRunnerParams.put(FTPRunnerConstants.PARAM_AUTH_METHOD, FTPRunnerConstants.AUTH_METHOD_USER_PWD);
          }
        }
      }

      if (DeployerRunnerConstants.SMB_RUN_TYPE.equals(runnerType)) {
        final String domain = newRunnerParams.get(DeployerRunnerConstants.PARAM_DOMAIN);
        if (StringUtil.isNotEmpty(domain)) {
          runnerUpdated = true;
          newRunnerParams.remove(DeployerRunnerConstants.PARAM_DOMAIN);
          final String username = newRunnerParams.get(DeployerRunnerConstants.PARAM_USERNAME);
          newRunnerParams.put(DeployerRunnerConstants.PARAM_USERNAME, domain + "\\" + username);
        }
      }

      if (runnerUpdated) {
        persistBuildType = true;
        buildType.updateBuildRunner(descriptor.getId(), descriptor.getName(), runnerType, newRunnerParams);
      }
    }
    if (persistBuildType) {
      buildType.persist();
    }
  }

  @Override
  public void serverStartup() {
    myServer.removeListener(this);
    Loggers.SERVER.debug("Server started up, will not convert passwords in configurations from now on.");
  }
}
