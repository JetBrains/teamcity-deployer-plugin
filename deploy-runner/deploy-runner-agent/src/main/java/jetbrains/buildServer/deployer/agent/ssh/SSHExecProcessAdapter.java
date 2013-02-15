package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;


class SSHExecProcessAdapter extends SyncBuildProcessAdapter {

    private final String myHost;
    private final int myPort;
    private final String myUsername;
    private final String myPassword;
    private final String myCommands;


    public SSHExecProcessAdapter(@NotNull final String host,
                                 final int port,
                                 @NotNull final String username,
                                 @NotNull final String password,
                                 @NotNull final String commands,
                                 @NotNull final BuildProgressLogger buildLogger) {
        super(buildLogger);
        myHost = host;
        myPort = port;
        myUsername = username;
        myPassword = password;
        myCommands = commands;
    }


    @Override
    public void runProcess() throws RunBuildException {

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session session = null;

        try {

            session = jsch.getSession(myUsername, myHost, myPort);
            session.setPassword(myPassword);
            session.connect();

            executeCommand(session, myCommands);

        } catch (RunBuildException e) {
            throw e;
        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = null;
        myLogger.message("Executing commands:\n" + command + "\non host [" + session.getHost() + "]");
        try {
            channel = (ChannelExec)session.openChannel("exec");
            channel.setCommand(command);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            final StringBuilder result = new StringBuilder();
            while (true) {
                final String str = reader.readLine();
                if (str != null) {
                    result.append(str).append('\n');
                } else {
                    break;
                }
            }
            myLogger.message("Exec output:\n" + result.toString());
        } finally {
            if (channel != null) {
                channel.disconnect();
                int exitCode = channel.getExitStatus();
                if (exitCode > 0) {
                    throw new RunBuildException("Non-zero exit code from ssh exec: [" + exitCode + "]");
                }
                myLogger.message("ssh exit-code: " + exitCode);
            }
        }

    }
}
