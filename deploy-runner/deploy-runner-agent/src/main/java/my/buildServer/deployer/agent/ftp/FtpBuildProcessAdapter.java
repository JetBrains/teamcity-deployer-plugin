package my.buildServer.deployer.agent.ftp;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;


class FtpBuildProcessAdapter extends BuildProcessAdapter {

    private final String myTarget;
    private final String myUsername;
    private final String myPassword;
    private final BuildRunnerContext myContext;
    private final List<ArtifactsCollection> myArtifacts;

    private volatile boolean hasFinished;

    public FtpBuildProcessAdapter(@NotNull final String target,
                                  @NotNull final String username,
                                  @NotNull final String password,
                                  @NotNull final BuildRunnerContext context,
                                  @NotNull final List<ArtifactsCollection> artifactsCollections) {
        myTarget = target;
        myUsername = username;
        myPassword = password;
        myContext = context;
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
    public void start() throws RunBuildException {

        FTPClient client = new FTPClient();
        try {
            client.connect(myTarget);

            if (StringUtil.isEmpty(myUsername)) {
                client.login("anonymous", "email@example.com");
            } else {
                client.login(myUsername, myPassword);
            }

            final String remoteRoot = client.currentDirectory();

            for (ArtifactsCollection artifactsCollection : myArtifacts) {
                for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                    final File source = fileStringEntry.getKey();
                    final String destinationDir = fileStringEntry.getValue();

                    try {
                        client.createDirectory(destinationDir);
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
                client.disconnect(true);
            } catch (Exception e) {
                Loggers.AGENT.error(e.getMessage(), e);
            }

            hasFinished = true;
        }
    }
}
