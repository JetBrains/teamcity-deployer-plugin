package my.buildServer.deployer.agent.smb;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import my.buildServer.deployer.agent.DeployerRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
* Created by Kit
* Date: 21.04.12 - 21:58
*/
class SMBBuildProcessAdapter extends BuildProcessAdapter {
    private final String target;
    private final String username;
    private final String password;
    private final BuildRunnerContext context;
    private final String sourcePath;

    public SMBBuildProcessAdapter(String target, String username, String password, BuildRunnerContext context, String sourcePath) {
        this.target = target;
        this.username = username;
        this.password = password;
        this.context = context;
        this.sourcePath = sourcePath;
    }

    @Override
    public void start() throws RunBuildException {
        final String targetWithProtocol;
        if (!target.startsWith(DeployerRunner.SMB)) {
            targetWithProtocol = DeployerRunner.SMB + target;
        } else {
            targetWithProtocol = target;
        }
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
