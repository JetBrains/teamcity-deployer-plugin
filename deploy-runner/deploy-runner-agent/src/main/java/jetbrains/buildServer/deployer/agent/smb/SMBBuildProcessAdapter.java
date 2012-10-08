package jetbrains.buildServer.deployer.agent.smb;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


class SMBBuildProcessAdapter extends BuildProcessAdapter {
    public static final String SMB = "smb://";


    private volatile boolean hasFinished;
    private final String myTarget;
    private final String myUsername;
    private final String myPassword;
    private final List<ArtifactsCollection> myArtifactsCollections;
    private final String myDomain;

    public SMBBuildProcessAdapter(@NotNull final String username,
                                  @NotNull final String password,
                                  @Nullable final String domain,
                                  @NotNull final String target,
                                  @NotNull final List<ArtifactsCollection> artifactsCollections) {
        myTarget = target;
        myUsername = username;
        myPassword = password;
        myDomain = domain;
        myArtifactsCollections = artifactsCollections;
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

        jcifs.Config.setProperty("jcifs.smb.client.disablePlainTextPasswords", "false"); // ???

        String targetWithProtocol;
        if (myTarget.startsWith("\\\\")) {
            targetWithProtocol = SMB + myTarget.substring(2);
        } else if (!myTarget.startsWith(SMB)) {
            targetWithProtocol = SMB + myTarget;
        } else {
            targetWithProtocol = myTarget;
        }

        // Share and directories names require trailing /
        if (!targetWithProtocol.endsWith("/")) {
            targetWithProtocol = targetWithProtocol + "/";
        }

        targetWithProtocol = targetWithProtocol.replaceAll("\\\\", "/");

        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(myDomain == null ? "" : myDomain,
                myUsername, myPassword);

        final String settingsString = "Trying to connect with following parameters:\n" +
                "username=[" + myUsername + "]\n" +
                "password=[" + myPassword + "]\n" +
                "domain=[" + (myDomain == null ? "" : myDomain) + "]\n" +
                "target=[" + targetWithProtocol + "]";
        try {
            Loggers.AGENT.debug(settingsString);
            SmbFile destinationDir = new SmbFile(targetWithProtocol, auth);

            for (ArtifactsCollection artifactsCollection : myArtifactsCollections) {
                upload(artifactsCollection.getFilePathMap(), destinationDir);
            }

        } catch (Exception e) {
            Loggers.AGENT.error(settingsString, e);
            throw new RunBuildException(e);
        } finally {
            hasFinished = true;
        }
    }

    private void upload(Map<File, String> filePathMap, SmbFile destination) throws IOException {
        for (Map.Entry<File, String> fileDestEntry : filePathMap.entrySet()) {
            final File source = fileDestEntry.getKey();
            final SmbFile destDirectory = new SmbFile(destination, fileDestEntry.getValue() + "/");  // Share and directories names require trailing /
            final SmbFile destFile = new SmbFile(destDirectory, source.getName());

            Loggers.AGENT.debug("Uploading source=[" + source.getAbsolutePath() + "] to \n" +
                    "destDirectory=[" + destDirectory.getCanonicalPath() +
                    "] destFile=[" +  destFile.getCanonicalPath() +"]");

            FileInputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                if (!destDirectory.exists()) {
                    destDirectory.mkdirs();
                }
                inputStream = new FileInputStream(source);
                outputStream = destFile.getOutputStream();
                FileUtil.copy(inputStream, outputStream);
                outputStream.flush();
            } finally {
                FileUtil.close(inputStream);
                FileUtil.close(outputStream);
            }
        }

    }

}
