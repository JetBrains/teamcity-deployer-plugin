package jetbrains.buildServer.deployer.agent.ftp;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


class FtpBuildProcessAdapter extends BuildProcessAdapter {

    private final String myTarget;
    private final String myUsername;
    private final String myPassword;
    private final List<ArtifactsCollection> myArtifacts;
    private static final String FTP_PROTOCOL = "ftp://";

    private volatile boolean hasFinished;

    public FtpBuildProcessAdapter(@NotNull final String target,
                                  @NotNull final String username,
                                  @NotNull final String password,
                                  @NotNull final List<ArtifactsCollection> artifactsCollections) {
        myTarget = target.toLowerCase().startsWith(FTP_PROTOCOL) ? target : FTP_PROTOCOL + target;
        myUsername = username;
        myPassword = password;
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


        FTPClient client = new FTPClient();
        try {
            final URL targetUrl = new URL(myTarget);
            final String host = targetUrl.getHost();
            final int port = targetUrl.getPort();
            final String path = targetUrl.getPath();

            if (port > 0) {
                client.connect(host, port);
            } else {
                client.connect(host);
            }

            if (StringUtil.isEmpty(myUsername)) {
                client.login("anonymous", "email@example.com");
            } else {
                client.login(myUsername, myPassword);
            }

            if (!StringUtil.isEmpty(path)) {
                createPath(client, path);
                client.changeDirectory(path);
            }

            final String remoteRoot = client.currentDirectory();

            for (ArtifactsCollection artifactsCollection : myArtifacts) {
                for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                    final File source = fileStringEntry.getKey();
                    final String destinationDir = fileStringEntry.getValue();

                    try {
                        createPath(client, destinationDir);
                    } catch (FTPException e) {
                        // we can safely ignore if dir already exists
                        if (!e.getMessage().contains("Directory already exists")) {
                            throw e;
                        }
                    }
                    client.changeDirectory(destinationDir);
                    client.upload(source);
                    client.changeDirectory(remoteRoot);
                }
            }


        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            try {
                if (client.isConnected()) {
                    client.disconnect(true);
                }
            } catch (Exception e) {
                Loggers.AGENT.error(e.getMessage(), e);
            }

            hasFinished = true;
        }
    }

    private void createPath(@NotNull final FTPClient client,
                            @NotNull final String path) throws IOException, FTPIllegalReplyException, FTPException {
        final String normalisedPath = path.trim().replaceAll("\\\\","/");
        final StringTokenizer pathTokenizer = new StringTokenizer(normalisedPath, "/");
        final StringBuilder sb = new StringBuilder(pathTokenizer.nextToken());
        createDirSkipExisting(client, sb.toString());
        while(pathTokenizer.hasMoreTokens()) {
            sb.append('/').append(pathTokenizer.nextToken());
            createDirSkipExisting(client, sb.toString());
        }
    }

    private void createDirSkipExisting(FTPClient client, final String directoryName) throws IOException, FTPIllegalReplyException, FTPException {
        try {
            client.createDirectory(directoryName);
        } catch (FTPException e) {
            // we can safely ignore if dir already exists
            if (!e.getMessage().contains("already exists")) {
                throw e;
            }
        }
    }
}
