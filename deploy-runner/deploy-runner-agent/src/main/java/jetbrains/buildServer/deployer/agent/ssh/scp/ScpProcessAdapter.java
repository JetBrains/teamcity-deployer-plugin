package jetbrains.buildServer.deployer.agent.ssh.scp;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ScpProcessAdapter extends SyncBuildProcessAdapter {

    private final String myTargetString;
    private final String myUsername;
    private final String myPassword;
    private final List<ArtifactsCollection> myArtifacts;

    private final File myKeyFile;

    private final int myPort;


    public ScpProcessAdapter(@NotNull final String username,
                             @NotNull final String password,
                             @NotNull final String target,
                             final int port,
                             @NotNull final BuildRunnerContext context,
                             @NotNull final List<ArtifactsCollection> artifactsCollections) {
        this(null, username, password, target, port, context, artifactsCollections);
    }

    public ScpProcessAdapter(@Nullable final File privateKey,
                             @NotNull final String username,
                             @NotNull final String password,
                             @NotNull final String target,
                             final int port,
                             @NotNull final BuildRunnerContext context,
                             @NotNull final List<ArtifactsCollection> artifactsCollections) {
        super(context.getBuild().getBuildLogger());
        myKeyFile = privateKey;
        myTargetString = target;
        myUsername = username;
        myPassword = password;
        myArtifacts = artifactsCollections;
        myPort = port;
    }

    @Override
    public void runProcess() throws RunBuildException {
        final String host;
        String escapedRemotePath;

        final int delimiterIndex = myTargetString.indexOf(':');
        if (delimiterIndex > 0) {
            host = myTargetString.substring(0, delimiterIndex);
            final String remotePath = myTargetString.substring(delimiterIndex +1);

            escapedRemotePath = remotePath.trim().replaceAll("\\\\", "/");
            if (new File(escapedRemotePath).isAbsolute() && !escapedRemotePath.startsWith("/")) {
                escapedRemotePath = "/" + escapedRemotePath;
            }
        } else {
            host = myTargetString;
            escapedRemotePath = "";
        }

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session session = null;

        try {
            if (myKeyFile != null) {
                if (StringUtil.isNotEmpty(myPassword)) {
                    jsch.addIdentity(myKeyFile.getAbsolutePath(), myPassword);
                } else {
                    jsch.addIdentity(myKeyFile.getAbsolutePath());
                }
            }
            session = jsch.getSession(myUsername, host, myPort);
            if (myKeyFile == null) {
                session.setPassword(myPassword);
            }
            session.connect();

            // createRemotePath(session, escapedRemotePath);
            if (isInterrupted()) return;
            myLogger.message("Starting upload via SCP to " +
                    (StringUtil.isNotEmpty(escapedRemotePath) ?
                            "[" + escapedRemotePath + "] on " : "") + "host [" + host + ":" + myPort + "]");


            final List<ArtifactsCollection> relativeDestinations = new LinkedList<ArtifactsCollection>();
            final List<ArtifactsCollection> absDestinations = new LinkedList<ArtifactsCollection>();
            boolean isRemoteBaseAbsolute = escapedRemotePath.startsWith("/");


            for (ArtifactsCollection artifactsCollection : myArtifacts) {
                if (artifactsCollection.getTargetPath().startsWith("/")) {
                    absDestinations.add(new ArtifactsCollection(
                            artifactsCollection.getSourcePath(),
                            artifactsCollection.getTargetPath(),
                            new HashMap<File, String>(artifactsCollection.getFilePathMap())
                    ));
                } else {
                    final Map<File, String> newPathMap = new HashMap<File, String>();
                    for (Map.Entry<File, String> fileTargetEntry : artifactsCollection.getFilePathMap().entrySet()) {
                        final String oldTarget = fileTargetEntry.getValue();
                        newPathMap.put(fileTargetEntry.getKey(), escapedRemotePath + "/" + oldTarget);
                    }
                    final ArtifactsCollection newCollection = new ArtifactsCollection(artifactsCollection.getSourcePath(), artifactsCollection.getTargetPath(), newPathMap);
                    if (isRemoteBaseAbsolute) {
                        absDestinations.add(newCollection);
                    } else {
                        relativeDestinations.add(newCollection);
                    }

                }
            }

            upload(session, ".", relativeDestinations);
            upload(session, "/", absDestinations);


        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void upload(final @NotNull Session session,
                        final @NotNull String escapedRemoteBase,
                        final @NotNull List<ArtifactsCollection> artifacts) throws IOException, JSchException {

        assert session.isConnected();

        // skip empty collections
        if (artifacts.size() == 0) {
            return;
        }

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
            for (ArtifactsCollection artifactCollection : artifacts) {
                int count = 0;
                for (Map.Entry<File, String> filePathEntry : artifactCollection.getFilePathMap().entrySet()) {
                    final File source = filePathEntry.getKey();
                    final String destination = filePathEntry.getValue();
                    final ScpOperation operationChain = ScpOperationBuilder.getCopyFileOperation(source, destination);
                    operationChain.execute(out, in);
                    count++;
                }
                myLogger.message("Uploaded [" + count + "] files for [" + artifactCollection.getSourcePath() + "] pattern");
            }
        } finally {
            FileUtil.close(out);
            FileUtil.close(in);
            execChannel.disconnect();
        }
    }
}
