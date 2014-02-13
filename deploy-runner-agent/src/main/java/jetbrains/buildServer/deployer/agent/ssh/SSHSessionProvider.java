package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.*;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.InternalPropertiesHolder;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;


public class SSHSessionProvider {

    public static final String TEAMCITY_DEPLOYER_SSH_CONFIG_PATH = "teamcity.deployer.ssh.config.path";
    public static final String TEAMCITY_DEPLOYER_SSH_DEFAULT_KEY = "teamcity.deployer.ssh.default.key";

    private final Logger myLog = Logger.getLogger(this.getClass());

    private Session mySession;
    private String myHost;
    private int myPort;
    private String myRemotePath;


    public SSHSessionProvider(@NotNull final BuildRunnerContext context, @NotNull final InternalPropertiesHolder holder) throws JSchException {

        final String target = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
        final String portStr = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_PORT);
        try {
            myPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            myPort = 22;
        }

        final int delimiterIndex = target.indexOf(':');
        if (delimiterIndex > 0) {
            myHost = target.substring(0, delimiterIndex);
            final String remotePath = target.substring(delimiterIndex + 1);

            myRemotePath = remotePath.trim().replaceAll("\\\\", "/");
            if (new File(myRemotePath).isAbsolute() && !myRemotePath.startsWith("/")) {
                myRemotePath = "/" + myRemotePath;
            }
        } else {
            myHost = target;
            myRemotePath = "";
        }

        final String username = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
        final String password = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);
        final String authMethod = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_AUTH_METHOD);

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");

        myLog.debug("Initializing ssh session.");
        if (SSHRunnerConstants.AUTH_METHOD_DEFAULT_KEY.equals(authMethod)) {
            final String configPath = holder.getInternalProperty(TEAMCITY_DEPLOYER_SSH_CONFIG_PATH, System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "config");
            //noinspection ConstantConditions
            final File config = new File(configPath);
            if (config.exists()) {
                myLog.debug("Found config at [" + config.getAbsolutePath() + "], reading.");
                initSessionSSHConfig(jsch, config);
            } else {
                final String keyPath = holder.getInternalProperty(TEAMCITY_DEPLOYER_SSH_DEFAULT_KEY, System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_rsa");
                //noinspection ConstantConditions
                final File keyFile = new File(keyPath);
                myLog.debug("Using keyfile at [" + keyFile.getAbsolutePath() + "], load.");
                initSessionKeyFile(username, password, keyFile, jsch);
            }
        } else if (SSHRunnerConstants.AUTH_METHOD_CUSTOM_KEY.equals(authMethod)) {
            final String keyFilePath = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_KEYFILE);
            final File keyFile = new File(context.getBuild().getCheckoutDirectory(), keyFilePath);
            myLog.debug("Using keyfile at [" + keyFile.getAbsolutePath() + "], load.");
            initSessionKeyFile(username, password, keyFile, jsch);
        } else {
            myLog.debug("Using provided username/password");
            initSessionUserPassword(username, password, jsch);
        }

        mySession.connect();
    }

    private void initSessionSSHConfig(JSch jsch, File config) throws JSchException {
        try {
            final OpenSSHConfig sshConfig = OpenSSHConfig.parseFile(config.getCanonicalPath());
            jsch.setConfigRepository(sshConfig);
            mySession = jsch.getSession(myHost);
        } catch (IOException e) {
            throw new JSchException("Error parsing ssh config file", e);
        }
    }

    private void initSessionUserPassword(String username, String password, JSch jsch) throws JSchException {
        mySession = jsch.getSession(username, myHost, myPort);
        mySession.setPassword(password);
    }

    private void initSessionKeyFile(String username, String password, File keyFile, JSch jsch) throws JSchException {
        try {
            if (StringUtil.isNotEmpty(password)) {
                jsch.addIdentity(keyFile.getCanonicalPath(), password);
            } else {
                jsch.addIdentity(keyFile.getCanonicalPath());
            }
            mySession = jsch.getSession(username, myHost, myPort);
        } catch (IOException e) {
            throw new JSchException("Failed to use key file", e);
        }
    }

    public String getRemotePath() {
        return myRemotePath;
    }

    public Session getSession() {
        return mySession;
    }

    public String getSessionString() {
        return  (StringUtil.isNotEmpty(myRemotePath) ? "[" + myRemotePath + "] on " : "") + "host [" + myHost + ":" + myPort + "]";
    }

}
