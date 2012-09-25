package my.buildServer.deployer.agent.smb;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;


class SMBBuildProcessAdapter extends BuildProcessAdapter {
    public static final String SMB = "smb://";
    private final String target;
    private final String username;
    private final String password;
    private final BuildRunnerContext context;
    private final String sourcePath;

    private volatile boolean hasFinished;

    public SMBBuildProcessAdapter(String target, String username, String password, BuildRunnerContext context, String sourcePath) {
        this.target = target;
        this.username = username;
        this.password = password;
        this.context = context;
        this.sourcePath = sourcePath;
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
        String targetWithProtocol;
        if (target.startsWith("\\\\")) {
            targetWithProtocol = SMB + target.substring(2);
        } else if (!target.startsWith(SMB)) {
            targetWithProtocol = SMB + target;
        } else {
            targetWithProtocol = target;
        }

        // Share and directories names require trailing /
        if (!targetWithProtocol.endsWith("/")) {
            targetWithProtocol = targetWithProtocol + "/";
        }

        targetWithProtocol = targetWithProtocol.replaceAll("\\\\", "/");

        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", username, password);
        final String settingsString = "Trying to connect with following parameters:\n" +
                "username=[" + username + "]\n" +
                "password=[" + password + "]\n" +
                "target=[" + targetWithProtocol + "]";
        try {
            Loggers.AGENT.debug(settingsString);
            SmbFile destinationDir = new SmbFile(targetWithProtocol, auth);
            File source = new File(context.getWorkingDirectory(), sourcePath);
            if (source.isDirectory()) {
                File[] files = source.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory()) {
                            upload(file, destinationDir);
                        }
                    }
                }
            }  else {
                upload(source, destinationDir);
            }
        } catch (Exception e) {
            Loggers.AGENT.error(settingsString, e);
            throw new RunBuildException(e);
        } finally {
            hasFinished = true;
        }
    }

    private void upload(File source, SmbFile destinationDir) throws IOException {
        SmbFile destFile = new SmbFile(destinationDir, source.getName());
        Loggers.AGENT.debug("Uploading source=[" + source.getAbsolutePath() + "] to \n" +
                "target=[" + destFile.getCanonicalPath() + "]");
        FileInputStream inputStream = new FileInputStream(source);
        OutputStream outputStream = destFile.getOutputStream();
        try {
            FileUtil.copy(inputStream, outputStream);
            outputStream.flush();
        } finally {
            FileUtil.close(inputStream);
            FileUtil.close(outputStream);
        }
    }
}
