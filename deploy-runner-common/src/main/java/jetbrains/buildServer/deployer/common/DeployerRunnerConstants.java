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
  public static final String PARAM_PLAIN_PASSWORD = "jetbrains.buildServer.deployer.password";
  @Deprecated
  public static final String PARAM_DOMAIN = "jetbrains.buildServer.deployer.domain";
  public static final String PARAM_TARGET_URL = "jetbrains.buildServer.deployer.targetUrl";
  public static final String PARAM_SOURCE_PATH = "jetbrains.buildServer.deployer.sourcePath";
  public static final String PARAM_CONTAINER_CONTEXT_PATH = "jetbrains.buildServer.deployer.container.contextPath";
  public static final String PARAM_CONTAINER_TYPE = "jetbrains.buildServer.deployer.container.type";
}
