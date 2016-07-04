package jetbrains.buildServer.deployer.agent.ssh;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.LineAwareByteArrayOutputStream;
import jetbrains.buildServer.StreamGobbler;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


class SSHExecProcessAdapter extends SyncBuildProcessAdapter {

  private static final Logger LOG = Logger.getInstance(SSHExecProcessAdapter.class.getName());
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
  public boolean runProcess() {

    Session session = null;
    try {
      session = myProvider.getSession();
      executeCommand(session, myPty, myCommands);
      return true;
    } catch (JSchException e) {
      myLogger.error(e.toString());
      LOG.warnAndDebugDetails(e.getMessage(), e);
      return false;
    } finally {
      if (session != null) {
        session.disconnect();
      }
    }
  }

  private void executeCommand(Session session, String pty, String command) throws JSchException {
    ChannelExec channel = null;
    myLogger.message("Executing commands:\n" + command + "\non host [" + session.getHost() + "]");
    try {
      channel = (ChannelExec) session.openChannel("exec");
      if (!StringUtil.isEmpty(pty)) {
        channel.setPty(true);
        channel.setPtyType(pty);
      }
      channel.setCommand(command);

      final LineAwareByteArrayOutputStream.LineListener lineListener = new LineAwareByteArrayOutputStream.LineListener() {
        @Override
        public void newLineDetected(@NotNull String line) {
          myLogger.message(line);
        }
      };
      final StreamGobbler outputGobbler = new StreamGobbler(channel.getInputStream(), null, "SSH session to [" + session.getHost() + "]",
          new LineAwareByteArrayOutputStream(Charset.forName("UTF-8"), lineListener));
      final StreamGobbler errGobbler = new StreamGobbler(channel.getErrStream(), null, "Unknown",
          new LineAwareByteArrayOutputStream(Charset.forName("UTF-8"), lineListener));

      outputGobbler.start();
      errGobbler.start();
      channel.connect();

      while (!isInterrupted()
          && channel.isConnected()
          && !channel.isEOF()
          && !channel.isClosed()) {

        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          checkIsInterrupted();
        }
      }

      outputGobbler.notifyProcessExit();
      errGobbler.notifyProcessExit();
      try {
        outputGobbler.join();
        outputGobbler.join();
      } catch (InterruptedException e) {
        LOG.warnAndDebugDetails(e.getMessage(), e);
      }

      if (isInterrupted()) {
        myLogger.message("Interrupted.");
      }
    } catch (IOException e) {
      myLogger.error(e.toString());
      LOG.warnAndDebugDetails(e.getMessage(), e);
    } finally {
      if (channel != null) {
        channel.disconnect();
        int exitCode = channel.getExitStatus();
        if (exitCode > 0) {
          myLogger.error("ssh exit-code: " + exitCode);
        } else {
          myLogger.message("ssh exit-code: " + exitCode);
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
