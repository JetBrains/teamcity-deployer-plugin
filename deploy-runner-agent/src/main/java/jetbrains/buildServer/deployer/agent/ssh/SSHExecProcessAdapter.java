package jetbrains.buildServer.deployer.agent.ssh;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.LineAwareByteArrayOutputStream;
import jetbrains.buildServer.StreamGobbler;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicLong;

import static jetbrains.buildServer.deployer.agent.DeployerAgentUtils.logBuildProblem;


class SSHExecProcessAdapter extends SyncBuildProcessAdapter {

  private static final Logger LOG = Logger.getInstance(SSHExecProcessAdapter.class.getName());
  private static final long CONNECTION_SILENCE_THRESHOLD_MS = 10 * 1000;
  private static final int CONNECTION_OPEN_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutes
  private final String myCommands;
  private final SSHSessionProvider myProvider;
  private final String myPty;
  private final boolean myFailOnExitCode;


  public SSHExecProcessAdapter(@NotNull final SSHSessionProvider provider,
                               @NotNull final String commands,
                               final String pty,
                               @NotNull final BuildProgressLogger buildLogger,
                               final boolean failOnExitCode) {
    super(buildLogger);
    myProvider = provider;
    myCommands = commands;
    myPty = pty;
    myFailOnExitCode = failOnExitCode;
  }


  @Override
  public BuildFinishedStatus runProcess() {
    JSch.setLogger(new JSchBuildLogger(myLogger));
    Session session = null;
    try {
      session = myProvider.getSession();
      return executeCommand(session, myPty, myCommands);
    } catch (JSchException e) {
      logBuildProblem(myLogger, e.getMessage());
      LOG.warnAndDebugDetails("Error executing SSH command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } finally {
      if (session != null) {
        session.disconnect();
      }
    }
  }

  private BuildFinishedStatus executeCommand(Session session, String pty, String command) throws JSchException {
    ChannelExec channel = null;
    BuildFinishedStatus result = BuildFinishedStatus.FINISHED_SUCCESS;
    myLogger.message("Executing commands:\n" + command + "\non host [" + session.getHost() + "]");
    try {
      channel = (ChannelExec) session.openChannel("exec");
      if (!StringUtil.isEmpty(pty)) {
        channel.setPty(true);
        channel.setPtyType(pty);
      }
      channel.setCommand(command);
      final AtomicLong lastOutputTimeStamp = new AtomicLong(System.currentTimeMillis());

      final LineAwareByteArrayOutputStream.LineListener lineListener = new LineAwareByteArrayOutputStream.LineListener() {
        @Override
        public void newLineDetected(@NotNull String line) {
          myLogger.message(line);
          lastOutputTimeStamp.set(System.currentTimeMillis());
        }
      };
      final LineAwareByteArrayOutputStream outputStream = new LineAwareByteArrayOutputStream(Charset.forName("UTF-8"), lineListener);
      final StreamGobbler outputGobbler = new StreamGobbler(channel.getInputStream(), null, "SSH session to [" + session.getHost() + "]", outputStream);
      final LineAwareByteArrayOutputStream errStream = new LineAwareByteArrayOutputStream(Charset.forName("UTF-8"), lineListener);
      final StreamGobbler errGobbler = new StreamGobbler(channel.getErrStream(), null, "Unknown", errStream);

      outputGobbler.start();
      errGobbler.start();
      channel.connect(CONNECTION_OPEN_TIMEOUT_MS);

      while (!isInterrupted()
          && channel.isConnected()
          && !channel.isEOF()
          && !channel.isClosed()) {

        try {
          Thread.sleep(500);
          // sometimes no newline chars are present, but still some output may be pending
          final boolean hasSomePendingOutput = outputGobbler.getLastActivityTimestamp() > lastOutputTimeStamp.get();
          final boolean waitingForTooLong = System.currentTimeMillis() - outputGobbler.getLastActivityTimestamp() > CONNECTION_SILENCE_THRESHOLD_MS;
          if (waitingForTooLong && hasSomePendingOutput) {
            // force dump of current pending output
            outputStream.write("\n".getBytes(Charset.forName("UTF-8")));
          }
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
        LOG.warnAndDebugDetails("SSH command interrupted", e);
      }

      if (isInterrupted()) {
        myLogger.message("Interrupted.");
      }
    } catch (IOException e) {
      myLogger.error(e.toString());
      LOG.warnAndDebugDetails(e.getMessage(), e);
      result = BuildFinishedStatus.FINISHED_FAILED;
    } finally {
      if (channel != null) {
        channel.disconnect();
        int exitCode = channel.getExitStatus();
        if (exitCode > 0) {
          if (myFailOnExitCode) {
            logExitCodeBuildProblem(exitCode);
            result = BuildFinishedStatus.FINISHED_WITH_PROBLEMS;
          } else {
            logBuildProblem(myLogger, "SSH exit-code [" + exitCode + "]");
          }
        } else {
          myLogger.message("SSH exit-code [" + exitCode + "]");
        }
      }
    }
    return result;
  }

  private void logExitCodeBuildProblem(int exitCode) {
    myLogger.logBuildProblem(BuildProblemData.createBuildProblem(SSHRunnerConstants.SSH_EXEC_RUN_TYPE + ":" + exitCode, BuildProblemTypes.TC_EXIT_CODE_TYPE, "SSH exit-code " + exitCode));
  }
}
