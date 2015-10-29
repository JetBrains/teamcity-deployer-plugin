package jetbrains.buildServer.deployer.agent.ssh;

import com.intellij.openapi.util.text.StringUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import java.io.IOException;

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
      channel = (ChannelExec) session.openChannel("exec");
      if (!StringUtil.isEmpty(pty)) {
        channel.setPty(true);
        channel.setPtyType(pty);
      }
      channel.setCommand(command);

      final InputStream inputStream = channel.getInputStream();
      final InputStream errorStream = channel.getErrStream();
      final StringBuilder result = new StringBuilder();
      byte[] buf = new byte[8192];

      channel.connect();
      while (!isInterrupted()) {
        boolean readFromInput = true;
        boolean readFromError = true;

        if (inputStream.available() > 0) {
          readFromInput = readStream(inputStream, result, buf, 8192);
        }
        if (errorStream.available() > 0) {
          readFromError = readStream(errorStream, result, buf, 8192);
        }

        boolean nothingWasRead = !readFromInput && !readFromError;
        if (nothingWasRead || channel.isClosed()) {
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
