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

package jetbrains.buildServer.deployer.common;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:09
 */
public class DeployerRunnerConstants {
  public static final String SSH_RUN_TYPE = "ssh-deploy-runner";
  public static final String SMB_RUN_TYPE = "smb-deploy-runner";
  public static final String SMB2_RUN_TYPE = "smb2-deploy-runner";
  public static final String FTP_RUN_TYPE = "ftp-deploy-runner";
  public static final String TOMCAT_RUN_TYPE = "tomcat-deploy-runner";
  public static final String CARGO_RUN_TYPE = "cargo-deploy-runner";


  public static final String PARAM_USERNAME = "jetbrains.buildServer.deployer.username";
  public static final String PARAM_PASSWORD = "secure:jetbrains.buildServer.deployer.password";
  @Deprecated
  public static final String PARAM_PLAIN_PASSWORD = "jetbrains.buildServer.deployer.password";
  @Deprecated
  public static final String PARAM_DOMAIN = "jetbrains.buildServer.deployer.domain";
  public static final String PARAM_TARGET_URL = "jetbrains.buildServer.deployer.targetUrl";
  public static final String PARAM_SOURCE_PATH = "jetbrains.buildServer.deployer.sourcePath";
  public static final String PARAM_CONTAINER_CONTEXT_PATH = "jetbrains.buildServer.deployer.container.contextPath";
  public static final String PARAM_CONTAINER_TYPE = "jetbrains.buildServer.deployer.container.type";

  public static final String BUILD_PROBLEM_TYPE = "jetbrains.buildServer.deployer";
}
