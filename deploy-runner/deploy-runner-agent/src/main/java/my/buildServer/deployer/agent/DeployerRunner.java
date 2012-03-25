package my.buildServer.deployer.agent;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:26
 */
public class DeployerRunner implements AgentBuildRunner {

    private static final String SMB = "smb://";

    @NotNull
    @Override
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {

        final String username = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
        final String password = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);
        final String target = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
        final String sourcePath = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_SOURCE_PATH);

        return new BuildProcessAdapter() {
            @Override
            public void start() throws RunBuildException {
                final String targetWithProtocol;
                if (!target.startsWith(SMB)) {
                    targetWithProtocol = SMB + target;
                } else {
                    targetWithProtocol = target;
                }
                NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", username, password);
                try {
                    Loggers.AGENT.debug("Trying to connect with following parameters:\n" +
                            "username=[" + username + "]\n" +
                            "password=[" + password + "]\n" +
                            "target=[" + targetWithProtocol + "]");
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
                    throw new RunBuildException(e);
                }
            }

            private void upload(File source, SmbFile destinationDir) throws IOException {
                SmbFile destFile = new SmbFile(destinationDir, source.getName());
                Loggers.AGENT.debug("Uploading source=[" + source.getAbsolutePath() + "] to \n" +
                        "target=[" + destFile.getCanonicalPath() + "]");
                FileUtil.copy(new FileInputStream(source), destFile.getOutputStream());
            }
        };
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new DeployerRunnerInfo();
    }

}
