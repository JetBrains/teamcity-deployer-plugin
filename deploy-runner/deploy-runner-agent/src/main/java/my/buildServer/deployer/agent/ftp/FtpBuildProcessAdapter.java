package my.buildServer.deployer.agent.ftp;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;


class FtpBuildProcessAdapter extends BuildProcessAdapter {
    public static final String SMB = "smb://";
    private final String target;
    private final String username;
    private final String password;
    private final BuildRunnerContext context;
    private final String sourcePath;

    private volatile boolean hasFinished;

    public FtpBuildProcessAdapter(String target, String username, String password, BuildRunnerContext context, String sourcePath) {
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

        FTPClient client = new FTPClient();
        try {
            client.connect(target);

            if (StringUtil.isEmpty(username)) {
                client.login("anonymous", "email@example.com");
            } else {
                client.login(username, password);
            }

            final File workingDirectory = context.getWorkingDirectory();

            File source = new File(workingDirectory, sourcePath);
            client.upload(source);

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
