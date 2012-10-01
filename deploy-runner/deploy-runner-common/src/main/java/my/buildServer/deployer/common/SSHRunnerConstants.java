package my.buildServer.deployer.common;

/**
 * Created by Nikita.Skvortsov
 * Date: 9/28/12, 2:53 PM
 */
public class SSHRunnerConstants {

    public static final String SSH_EXEC_RUN_TYPE = "ssh-exec-runner";

    public static final String PARAM_HOST = "my.buildServer.sshexec.host";
    public static final String PARAM_USERNAME = "my.buildServer.sshexec.username";
    public static final String PARAM_PASSWORD = "my.buildServer.sshexec.password";
    public static final String PARAM_COMMAND = "my.buildServer.sshexec.command";

    public static final String PARAM_TRANSPORT = "my.buildServer.deployer.ssh.transport";

    public static final String TRANSPORT_SCP = "my.buildServer.deployer.ssh.transport.scp";
    public static final String TRANSPORT_SFTP = "my.buildServer.deployer.ssh.transport.sftp";

}
