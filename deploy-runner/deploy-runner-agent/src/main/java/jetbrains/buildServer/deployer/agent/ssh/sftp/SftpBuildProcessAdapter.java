package jetbrains.buildServer.deployer.agent.ssh.sftp;

import com.intellij.openapi.util.text.StringUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.ssh.SSHSessionProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;


public class SftpBuildProcessAdapter extends SyncBuildProcessAdapter {

    private final List<ArtifactsCollection> myArtifacts;
    private SSHSessionProvider mySessionProvider;

    public SftpBuildProcessAdapter(@NotNull final BuildRunnerContext context,
                                   @NotNull final List<ArtifactsCollection> artifactsCollections,
                                   @NotNull final SSHSessionProvider sessionProvider) {
        super(context.getBuild().getBuildLogger());
        myArtifacts = artifactsCollections;
        mySessionProvider = sessionProvider;
    }

    @Override
    public void runProcess() throws RunBuildException {
        final String escapedRemotePath;
        Session session = null;

        try {
            escapedRemotePath = mySessionProvider.getEscapedRemotePath();
            session = mySessionProvider.getSession();

            if (isInterrupted()) return;

            ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
            channel.connect();

            if (StringUtil.isNotEmpty(escapedRemotePath)) {
                createRemotePath(channel, escapedRemotePath);
                channel.cd(escapedRemotePath);
            }

            myLogger.message("Starting upload via SFTP to " + mySessionProvider.getSessionString());

            for (ArtifactsCollection artifactsCollection : myArtifacts) {
                int count = 0;
                for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                    final File source = fileStringEntry.getKey();
                    final String destinationPath = fileStringEntry.getValue();
                    createRemotePath(channel, destinationPath);
                    channel.put(source.getAbsolutePath(), destinationPath);
                    count++;
                }
                myLogger.message("Uploaded [" + count + "] files for [" + artifactsCollection.getSourcePath() + "] pattern");
            }
            channel.disconnect();

        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void createRemotePath(@NotNull final ChannelSftp channel,
                                  @NotNull final String destination) throws SftpException {
        final int endIndex = destination.lastIndexOf('/');
        if (endIndex > 0) {
            createRemotePath(channel, destination.substring(0, endIndex));
        }
        try {
            channel.stat(destination);
        } catch (SftpException e) {
            // dir does not exist.
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                channel.mkdir(destination);
            }
        }

    }

}
