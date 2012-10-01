package my.buildServer.deployer.agent.ssh.scp;

import com.intellij.util.WaitFor;
import com.jcraft.jsch.*;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.Map;

public class ScpProcessAdapter extends BuildProcessAdapter {

    private final String myTargetString;
    private final String myUsername;
    private final String myPassword;
    private final List<ArtifactsCollection> myArtifacts;
    private final File myWorkingDirectory;


    private volatile boolean hasFinished;
    private volatile boolean isInterrupted;


    public ScpProcessAdapter(@NotNull final BuildRunnerContext context,
                             @NotNull final String username,
                             @NotNull final String password,
                             @NotNull final String target,
                             @NotNull final List<ArtifactsCollection> artifactsCollections) {
        myTargetString = target;
        myUsername = username;
        myPassword = password;
        myWorkingDirectory = context.getWorkingDirectory();
        myArtifacts = artifactsCollections;

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
            upload(session, escapedRemotePath);

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

    private void upload(final @NotNull Session session,
                        final @NotNull String escapedRemoteBase) throws IOException, JSchException {

        assert session.isConnected();

        // exec 'scp -rt <remoteBase>' remotely
        final String command= "scp -rt " + (StringUtil.isEmptyOrSpaces(escapedRemoteBase) ? "." : escapedRemoteBase);
        final ChannelExec execChannel = (ChannelExec)session.openChannel("exec");
        execChannel.setCommand(command);

        // get I/O streams for remote scp
        final OutputStream out = execChannel.getOutputStream();
        final InputStream in = execChannel.getInputStream();

        execChannel.connect();
        ScpExecUtil.checkScpAck(in);

        try {
            for (ArtifactsCollection artifactCollection : myArtifacts) {
                for (Map.Entry<File, String> filePathEntry : artifactCollection.getFilePathMap().entrySet()) {
                    final File source = filePathEntry.getKey();
                    final String dest = filePathEntry.getValue();
                    final ScpOperation operationChain = ScpOperationChainBuilder.buildChain(source, dest);
                    operationChain.execute(out, in);
                }
            }
        } finally {
            FileUtil.close(out);
            FileUtil.close(in);
            execChannel.disconnect();
        }
    }
}
