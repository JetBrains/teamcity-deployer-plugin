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

package jetbrains.buildServer.deployer.common;

/**
 * Created by Nikita.Skvortsov
 * date 27.07.13.
 */
public class FTPRunnerConstants {
  public static final String PARAM_AUTH_METHOD = "jetbrains.buildServer.deployer.ftp.authMethod";
  public static final String PARAM_TRANSFER_MODE = "jetbrains.buildServer.deployer.ftp.transferMethod";
  public static final String TRANSFER_MODE_AUTO = "AUTO";
  public static final String TRANSFER_MODE_BINARY = "BINARY";
  public static final String TRANSFER_MODE_ASCII = "ASCII";
  public static final String AUTH_METHOD_USER_PWD = "USER_PWD";
  public static final String AUTH_METHOD_ANONYMOUS = "ANONYMOUS";
  public static final String PARAM_SSL_MODE = "jetbrains.buildServer.deployer.ftp.securityMode";
  public static final String PARAM_FTP_MODE = "jetbrains.buildServer.deployer.ftp.ftpMode";
  public static final String PARAM_FTP_CONNECT_TIMEOUT = "jetbrains.deployer.ftp.connectTimeout";
}
