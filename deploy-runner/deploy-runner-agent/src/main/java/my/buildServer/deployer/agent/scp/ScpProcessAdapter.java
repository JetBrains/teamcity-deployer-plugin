package my.buildServer.deployer.agent.scp;

import com.intellij.util.WaitFor;
import com.jcraft.jsch.*;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * Created by Kit
 * Date: 21.04.12 - 22:00
 */
public class ScpProcessAdapter extends BuildProcessAdapter {

    private volatile boolean hasFinished;
    private volatile boolean isInterrupted;
    private final String myTargetString;
    private final String mySourcePath;
    private final String myUsername;
    private final String myPassword;
    private File myWorkingDirectory;

    public ScpProcessAdapter(String sourcePath, String targetHost, String username, String password, File workingDirectory) {
        myTargetString = targetHost;
        mySourcePath = sourcePath;
        myUsername = username;
        myPassword = password;
        myWorkingDirectory = workingDirectory;
        hasFinished = false;
        isInterrupted = false;
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
    public void interrupt() {
        isInterrupted = true;
    }

    @Override
    public boolean isInterrupted() {
        return isInterrupted;
    }

    @Override
    public void start() throws RunBuildException {
        final String host = myTargetString.substring(0, myTargetString.indexOf(':'));
        final String remotePath = myTargetString.substring(myTargetString.indexOf(':')+1);
        final String escapedRemotePath = remotePath.trim().replaceAll("\\\\", "/");

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session session = null;

        try {
            session = jsch.getSession(myUsername, host, 22);
            session.setPassword(myPassword);
            session.connect();

            createRemotePath(session, escapedRemotePath);
            if (isInterrupted()) return;
            copy(session, mySourcePath, escapedRemotePath);

        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
        hasFinished = true;
    }

    private void createRemotePath(final @NotNull Session session,
                                  final @NotNull String escapedRemoteBase) throws JSchException, IOException {

        if (StringUtil.isEmptyOrSpaces(escapedRemoteBase)) {
            return;
        }

        assert session.isConnected();

        final String command= "mkdir -p " + escapedRemoteBase;
        final ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        final ChannelExec execChannel = (ChannelExec) session.openChannel("exec");

        execChannel.setCommand(command);
        execChannel.setExtOutputStream(os);
        execChannel.connect();

        WaitFor waitFor = new WaitFor(5000) {
            @Override
            protected boolean condition() {
                return execChannel.isClosed();
            }
        };

        if (!waitFor.isConditionRealized()) {
            throw new IOException("Timed out waiting for remote command [" + command + "] to execute");
        }

        if(execChannel.getExitStatus() != 0) {
            throw new IOException(os.toString());
        }
    }

    private void copy(final @NotNull Session session,
                      final @NotNull String sourcePath,
                      final @NotNull String escapedRemoteBase) throws Exception {
        assert session.isConnected();
        final File sourceFile = new File(myWorkingDirectory, sourcePath);

        if (!sourceFile.exists()) {
            throw new IOException("Source [" + sourceFile.getAbsolutePath() + "] does not exists");
        }

        ScpOperation operationsChain = ScpOperationChainBuilder.buildChain(sourceFile);

        // exec 'scp -rt <remoteBase>' remotely
        String command= "scp -rt " + (StringUtil.isEmptyOrSpaces(escapedRemoteBase) ? "." : escapedRemoteBase);
        Channel channel=session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out=channel.getOutputStream();
        InputStream in=channel.getInputStream();

        channel.connect();
        ScpExecUtil.checkScpAck(in);

        try {
            operationsChain.execute(out, in);
        } finally {
            FileUtil.close(out);
            FileUtil.close(in);
            channel.disconnect();
        }
    }
}
