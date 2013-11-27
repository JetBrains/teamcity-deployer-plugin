package jetbrains.buildServer.deployer.agent.ssh;

import com.intellij.openapi.util.text.StringUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;


class SSHExecProcessAdapter extends SyncBuildProcessAdapter {

    private final String myCommands;
    private final SSHSessionProvider myProvider;
    private final String myPty;


    public SSHExecProcessAdapter(@NotNull final SSHSessionProvider provider,
                                 @NotNull final String commands,
                                 final String pty,
                                 @NotNull final BuildProgressLogger buildLogger) {
        super(buildLogger);
        myProvider = provider;
        myCommands = commands;
        myPty = pty;
    }


    @Override
    public void runProcess() throws RunBuildException {

        final Session session = myProvider.getSession();

        try {
            executeCommand(session, myPty, myCommands);
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

    private void executeCommand(Session session, String pty, String command) throws Exception {
        ChannelExec channel = null;
        myLogger.message("Executing commands:\n" + command + "\non host [" + session.getHost() + "]");
        try {
            channel = (ChannelExec)session.openChannel("exec");
            if (!StringUtil.isEmpty(pty)) {
                channel.setPty(true);
                channel.setPtyType(pty);
            }
            channel.setCommand(command);

            final InputStream inputStream = channel.getInputStream();
            final StringBuilder result = new StringBuilder();
            byte[] buf = new byte[8192];

            channel.connect();
            while (!isInterrupted()) {
                if (inputStream.available() > 0) {
                    int i = inputStream.read(buf, 0, 8192);
                    if (i < 0) {
                        break;
                    }
                    result.append(new String(buf, 0, i));
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
}
