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
  public static final String DEBUG = "jetbrains.buildServer.deployer.ftp.debug";
}
