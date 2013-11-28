package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import java.io.IOException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


class SSHExecProcessAdapter extends SyncBuildProcessAdapter {

    private final String myCommands;
    private final SSHSessionProvider myProvider;


    public SSHExecProcessAdapter(@NotNull final SSHSessionProvider provider,
                                 @NotNull final String commands,
                                 @NotNull final BuildProgressLogger buildLogger) {
        super(buildLogger);
        myProvider = provider;
        myCommands = commands;
    }


    @Override
    public void runProcess() throws RunBuildException {

        final Session session = myProvider.getSession();

        try {
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

            final InputStream inputStream = channel.getInputStream();
            final InputStream errorStream = channel.getErrStream();
            final StringBuilder result = new StringBuilder();
            byte[] buf = new byte[8192];

            channel.connect();
            while (!isInterrupted()) {
                if (inputStream.available() > 0) {
                    if (!readStream(inputStream, result, buf, 8192)) {
                        break;
                    }
                }
                if (errorStream.available() > 0) {
                    if (!readStream(errorStream, result, buf, 8192)) {
                        break;
                    }
                }
                if (channel.isClosed()) {
                    break;
                }
            }

            myLogger.message("Exec output:\n" + result.toString());
            if (isInterrupted()) {
                myLogger.message("Interrupted.");
            }

        } finally {
            if (channel != null) {
                channel.disconnect();
                int exitCode = channel.getExitStatus();
                myLogger.message("ssh exit-code: " + exitCode);
                if (exitCode > 0) {
                    throw new RunBuildException("Non-zero exit code from ssh exec: [" + exitCode + "]");
                }
            }
        }

    }

    private boolean readStream(InputStream inputStream, StringBuilder appendTo, byte[] buffer, final int BUFFER_LENGTH) throws IOException {
        int i = inputStream.read(buffer, 0, BUFFER_LENGTH);
        if (i < 0) {
            return false;
        }
        appendTo.append(new String(buffer, 0, i));
        return true;
    }
}
