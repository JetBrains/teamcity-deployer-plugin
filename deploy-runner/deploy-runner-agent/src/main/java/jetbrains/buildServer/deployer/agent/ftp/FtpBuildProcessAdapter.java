package jetbrains.buildServer.deployer.agent.ftp;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


class FtpBuildProcessAdapter extends SyncBuildProcessAdapter {
    private static final String FTP_PROTOCOL = "ftp://";

    private final String myTarget;
    private final String myUsername;
    private final String myPassword;
    private final List<ArtifactsCollection> myArtifacts;

    public FtpBuildProcessAdapter(@NotNull final BuildRunnerContext context,
                                  @NotNull final String target,
                                  @NotNull final String username,
                                  @NotNull final String password,
                                  @NotNull final List<ArtifactsCollection> artifactsCollections) {
        super(context.getBuild().getBuildLogger());
        myTarget = target.toLowerCase().startsWith(FTP_PROTOCOL) ? target : FTP_PROTOCOL + target;
        myUsername = username;
        myPassword = password;
        myArtifacts = artifactsCollections;
    }

    @Override
    public void runProcess() throws RunBuildException {

        FTPClient client = new FTPClient();
        try {
            final URL targetUrl = new URL(myTarget);
            final String host = targetUrl.getHost();
            final int port = targetUrl.getPort();
            final String encodedPath = targetUrl.getPath();

            final String path;
            if (encodedPath.length() > 0) {
                path = URLDecoder.decode(encodedPath.substring(1), "UTF-8");
            } else {
                path = "";
            }

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

            myLogger.message("Starting upload via FTP to " + myTarget);

            for (ArtifactsCollection artifactsCollection : myArtifacts) {
                int count = 0;
                for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                    final File source = fileStringEntry.getKey();
                    final String destinationDir = fileStringEntry.getValue();

                    if (StringUtil.isNotEmpty(destinationDir)) {
                        createPath(client, destinationDir);
                        client.changeDirectory(destinationDir);
                    }

                    client.upload(source);
                    client.changeDirectory(remoteRoot);
                    count++;
                }
                myLogger.message("Uploaded [" + count + "] files for [" + artifactsCollection.getSourcePath() + "] pattern");
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
        }
    }

    private void createPath(@NotNull final FTPClient client,
                            @NotNull final String path) throws Exception {
        final String root = client.currentDirectory();
        final String normalisedPath = path.trim().replaceAll("\\\\","/");
        final StringTokenizer pathTokenizer = new StringTokenizer(normalisedPath, "/");
        if (path.startsWith("/")) {
            client.changeDirectory("/"); // support absolute paths
        }
        boolean prevDirExisted = true;
        while (pathTokenizer.hasMoreTokens()) {
            final String nextDir = pathTokenizer.nextToken();
            if (prevDirExisted && dirExists(nextDir, client)) {
                client.changeDirectory(nextDir);
            } else {
                Exception createException = null;
                try {
                    client.createDirectory(nextDir);
                    prevDirExisted = false;
                } catch (FTPException e) {
                    createException = e;
                }

                try {
                    client.changeDirectory(nextDir);
                } catch (FTPException f) {
                    String message = "Failed to change current dir to [" + nextDir + "]";
                    if (createException != null) {
                        message += "\n also failed to create this dir: [" + createException.getMessage() + "]";
                    }
                    throw new RunBuildException(message, f);
                }
            }
        }

        client.changeDirectory(root);
    }

    private boolean dirExists(@NotNull final String nextDir,
                              @NotNull final FTPClient client) throws Exception {
        final String[] strings = client.listNames();
        for (String string : strings) {
            if (string.equals(nextDir)) {
                return true;
            }
        }
        return false;
    }
}
