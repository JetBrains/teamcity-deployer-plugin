package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
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
                              @Nullable final File keyFile) {
        myTarget = target;
        myPort = port;
        myUsername = username;
        myPassword = password;
        myKeyFile = keyFile;
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
