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
