package my.buildServer.deployer.agent.ssh.sftp;

import com.intellij.openapi.util.text.StringUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;


public class SftpBuildProcessAdapter extends BuildProcessAdapter {

    private final String myTarget;
    private final String myUsername;
    private final String myPassword;
    private final List<ArtifactsCollection> myArtifacts;

    private volatile boolean hasFinished;
    private BuildProgressLogger myLogger;

    public SftpBuildProcessAdapter(@NotNull final String target,
                                   @NotNull final String username,
                                   @NotNull final String password,
                                   @NotNull final BuildRunnerContext context,
                                   @NotNull final List<ArtifactsCollection> artifactsCollections) {
        myTarget = target;
        myUsername = username;
        myPassword = password;
        myLogger = context.getBuild().getBuildLogger();
        myArtifacts = artifactsCollections;
        hasFinished = false;
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
    public boolean isFinished() {
        return hasFinished;
    }

    @Override
    public void start() throws RunBuildException {
        final String host;
        final String escapedRemotePath;

        final int delimiterIndex = myTarget.indexOf(':');
        if (delimiterIndex > 0) {
            host = myTarget.substring(0, delimiterIndex);
            final String remotePath = myTarget.substring(delimiterIndex +1);

            escapedRemotePath = escapePathForSSH(remotePath);
        } else {
            host = myTarget;
            escapedRemotePath = "";
        }

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session session = null;

        try {
            session = jsch.getSession(myUsername, host, 22);
            session.setPassword(myPassword);
            session.connect();

            if (isInterrupted()) return;

            ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
            channel.connect();
            if (!StringUtil.isEmpty(escapedRemotePath)) {
                myLogger.message("Creating path [" + escapedRemotePath + "]");
                createRemotePath(channel, escapedRemotePath);
                channel.cd(escapedRemotePath);
            }

            for (ArtifactsCollection artifactsCollection : myArtifacts) {
                for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                    final File source = fileStringEntry.getKey();
                    final String destinationPath = escapePathForSSH(fileStringEntry.getValue());

                    myLogger.message(
                            "Copying [" + source.getAbsolutePath() + "] to [" + destinationPath + "]"
                    );
                    myLogger.message("creating artifact path [" + destinationPath + "]");
                    createRemotePath(channel, destinationPath);
                    channel.put(source.getAbsolutePath(), destinationPath);
                }

            }
            channel.disconnect();

        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
        hasFinished = true;
    }

    private void createRemotePath(@NotNull final ChannelSftp channel,
                                  @NotNull final String destination) throws SftpException {
        final int endIndex = destination.lastIndexOf('/');
        if (endIndex > 0) {
            createRemotePath(channel, destination.substring(0, endIndex));
        }
        try {
            myLogger.message("calling stat for [" + destination + "]");
            channel.stat(destination);
            myLogger.message("path [" + destination + "] exists");
        } catch (SftpException e) {
            // dir does not exist.
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                myLogger.message("no_such_file caught, calling mkdir [" + destination + "]");
                channel.mkdir(destination);
                myLogger.message("created path [" + destination + "]");
            }
        }

    }

    private String escapePathForSSH(String remotePath) {
        String escapedRemotePath;
        if (new File(remotePath).isAbsolute()) {
            escapedRemotePath = "/" + remotePath.trim().replaceAll("\\\\", "/");
        } else {
            escapedRemotePath = remotePath.trim().replaceAll("\\\\", "/");
        }
        return escapedRemotePath;
    }

}
