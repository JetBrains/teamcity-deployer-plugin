package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.*;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;

import java.io.*;


class SSHExecProcessAdapter extends BuildProcessAdapter {

    private final String myHost;
    private final String myUsername;
    private final String myPassword;
    private final String myCommands;
    private final BuildProgressLogger myLogger;

    private volatile boolean hasFinished;


    public SSHExecProcessAdapter(@NotNull final String host,
                                 @NotNull final String username,
                                 @NotNull final String password,
                                 @NotNull final String commands,
                                 @NotNull final BuildProgressLogger buildLogger) {
        myHost = host;
        myUsername = username;
        myPassword = password;
        myCommands = commands;
        hasFinished = false;
        myLogger = buildLogger;
    }


    @NotNull
    @Override
    public BuildFinishedStatus waitFor() throws RunBuildException {
        while (!isInterrupted() && !hasFinished) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RunBuildException(e);
            }
        }
        return hasFinished ? BuildFinishedStatus.FINISHED_SUCCESS :
                BuildFinishedStatus.INTERRUPTED;
    }

    @Override
    public void start() throws RunBuildException {

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session session = null;

        try {

            session = jsch.getSession(myUsername, myHost, 22);
            session.setPassword(myPassword);
            session.connect();

            executeCommand(session, myCommands);

        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
            hasFinished = true;
        }
    }

    private void executeCommand(Session session, String command) throws JSchException, IOException {
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
                if (channel.isClosed()){
                    myLogger.message("ssh exit-code: " + channel.getExitStatus());
                }
                channel.disconnect();
            }
        }

    }
}
