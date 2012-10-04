package jetbrains.buildServer.deployer.common;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:09
 */
public class DeployerRunnerConstants {
    public static final String SCP_RUN_TYPE = "scp-deploy-runner";
    public static final String SMB_RUN_TYPE = "smb-deploy-runner";
    public static final String FTP_RUN_TYPE = "ftp-deploy-runner" ;
    public static final String SFTP_RUN_TYPE = "sftp-deploy-runner";
    public static final String TOMCAT_RUN_TYPE = "tomcat-deploy-runner";


    public static final String PARAM_USERNAME = "jetbrains.buildServer.deployer.username";
    public static final String PARAM_PASSWORD = "jetbrains.buildServer.deployer.password";
    public static final String PARAM_TARGET_URL = "jetbrains.buildServer.deployer.targetUrl";
    public static final String PARAM_SOURCE_PATH = "jetbrains.buildServer.deployer.sourcePath";
    public static final String PARAM_TRANSPORT = "jetbrains.buildServer.deployer.useSftp";
}
