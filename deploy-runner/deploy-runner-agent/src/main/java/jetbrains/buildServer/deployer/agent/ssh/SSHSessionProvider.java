package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;


public class SSHSessionProvider {
    // out
    private Session session;
    private String myHost;
    private int myPort;
    private String escapedRemotePath;

    // in
    private String myTarget;
    private File myKeyFile;
    private String myPassword;
    private String myUsername;



    public SSHSessionProvider(@NotNull final String target,
                              final int port,
                              @NotNull final String username,
                              @NotNull final String password,
                              @Nullable final File keyFile) throws JSchException {
        myTarget = target;
        myPort = port;
        myUsername = username;
        myPassword = password;
        myKeyFile = keyFile;
        invoke();
    }

    public SSHSessionProvider(BuildRunnerContext context) throws JSchException {
        myTarget = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
        final String portStr = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_PORT);
        try {
            myPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            myPort = 22;
        }
        myUsername = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
        myPassword = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);

        final String authMethod = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_AUTH_METOD);

        if ("DEFAULT_KEY".equals(authMethod)) {
            myKeyFile = new File(System.getProperty("user.home"), ".ssh" + File.separator + "id_rsa");
        } else if ("PRIVATE_KEY".equals(authMethod)) {
            final String keyFilePath = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_KEYFILE);
            myKeyFile = new File(context.getBuild().getCheckoutDirectory(), keyFilePath);
        } else {
            myKeyFile = null;
        }

        invoke();
    }

    public String getEscapedRemotePath() {
        return escapedRemotePath;
    }

    public Session getSession() {
        return session;
    }

    public String getSessionString() {
        return  (StringUtil.isNotEmpty(escapedRemotePath) ? "[" + escapedRemotePath + "] on " : "") + "host [" + myHost + ":" + myPort + "]";
    }

    public SSHSessionProvider invoke() throws JSchException {
        final int delimiterIndex = myTarget.indexOf(':');
        if (delimiterIndex > 0) {
            myHost = myTarget.substring(0, delimiterIndex);
            final String remotePath = myTarget.substring(delimiterIndex + 1);

            escapedRemotePath = remotePath.trim().replaceAll("\\\\", "/");
            if (new File(escapedRemotePath).isAbsolute() && !escapedRemotePath.startsWith("/")) {
                escapedRemotePath = "/" + escapedRemotePath;
            }
        } else {
            myHost = myTarget;
            escapedRemotePath = "";
        }

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");


        if (myKeyFile != null) {
            if (StringUtil.isNotEmpty(myPassword)) {
                jsch.addIdentity(myKeyFile.getAbsolutePath(), myPassword);
            } else {
                jsch.addIdentity(myKeyFile.getAbsolutePath());
            }
        }

        session = jsch.getSession(myUsername, myHost, myPort);
        if (myKeyFile == null) {
            session.setPassword(myPassword);
        }

        session.connect();
        return this;
    }
}
