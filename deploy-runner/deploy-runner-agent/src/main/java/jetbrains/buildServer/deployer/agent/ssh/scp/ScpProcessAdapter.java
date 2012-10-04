package jetbrains.buildServer.deployer.agent.ssh.scp;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class ScpProcessAdapter extends BuildProcessAdapter {

    private final String myTargetString;
    private final String myUsername;
    private final String myPassword;
    private final List<ArtifactsCollection> myArtifacts;


    private volatile boolean hasFinished;
    private volatile boolean isInterrupted;


    public ScpProcessAdapter(@NotNull final String username,
                             @NotNull final String password,
                             @NotNull final String target,
                             @NotNull final List<ArtifactsCollection> artifactsCollections) {
        myTargetString = target;
        myUsername = username;
        myPassword = password;
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
    public boolean isFinished() {
        return hasFinished;
    }

    @Override
    public void start() throws RunBuildException {
        try {
            final String host;
            final String escapedRemotePath;

            final int delimiterIndex = myTargetString.indexOf(':');
            if (delimiterIndex > 0) {
                host = myTargetString.substring(0, delimiterIndex);
                final String remotePath = myTargetString.substring(delimiterIndex +1);

                if (new File(remotePath).isAbsolute()) {
                    escapedRemotePath = "/" + remotePath.trim().replaceAll("\\\\", "/");
                } else {
                    escapedRemotePath = remotePath.trim().replaceAll("\\\\", "/");
                }
            } else {
                host = myTargetString;
                escapedRemotePath = "";
            }

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
        } finally {
            hasFinished = true;
        }
    }

    private void createRemotePath(final @NotNull Session session,
                                  final @NotNull String escapedRemotePath) throws JSchException, IOException {
        if (StringUtil.isEmptyOrSpaces(escapedRemotePath)) {
            return;
        }

        assert session.isConnected();

        final String command= "scp -rt .";
        final ChannelExec execChannel = (ChannelExec)session.openChannel("exec");
        execChannel.setCommand(command);

        // get I/O streams for remote scp
        final OutputStream out = execChannel.getOutputStream();
        final InputStream in = execChannel.getInputStream();

        execChannel.connect();
        ScpExecUtil.checkScpAck(in);

        try {
            final ScpOperation createPathOperation = ScpOperationBuilder.getCreatePathOperation(escapedRemotePath);
            createPathOperation.execute(out, in);
        } finally {
            FileUtil.close(out);
            FileUtil.close(in);
            execChannel.disconnect();
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
                    final String destination = filePathEntry.getValue();
                    final ScpOperation operationChain = ScpOperationBuilder.getCopyFileOperation(source, destination);
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
