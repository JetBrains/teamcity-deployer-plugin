package jetbrains.buildServer.deployer.server.converter;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nikita.Skvortsov
 * date: 29.07.13.
 */
public class PasswordParameterConverter extends BuildServerAdapter {

    private final SBuildServer myServer;

    public PasswordParameterConverter(@NotNull SBuildServer server) {
        myServer = server;
        myServer.addListener(this);
    }



    @Override
    public void buildTypeRegistered(SBuildType buildType) {
        boolean persistBuildType = false;
        for (SBuildRunnerDescriptor descriptor : buildType.getBuildRunners()) {
            final String runnerType = descriptor.getType();
            final Map<String,String> newRunnerParams = new HashMap<String, String>(descriptor.getParameters());

            final String plainPassword = newRunnerParams.get(DeployerRunnerConstants.PARAM_PLAIN_PASSWORD);
            if (plainPassword != null) {
                persistBuildType = true;
                Loggers.SERVER.debug("Scrambling password for runner [" + runnerType + "-" + descriptor.getName() + "] in [" + buildType.getName() + "]");
                newRunnerParams.remove(DeployerRunnerConstants.PARAM_PLAIN_PASSWORD);
                newRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, plainPassword);
                buildType.updateBuildRunner(descriptor.getId(), descriptor.getName(), runnerType, newRunnerParams);
            }

            if (DeployerRunnerConstants.SSH_RUN_TYPE.equals(runnerType) ||
                    SSHRunnerConstants.SSH_EXEC_RUN_TYPE.equals(runnerType)) {
                final String sshAuthMethod = newRunnerParams.get(SSHRunnerConstants.PARAM_AUTH_METHOD);
                if (StringUtil.isEmpty(sshAuthMethod)) {
                    persistBuildType = true;
                    Loggers.SERVER.debug("Setting default (username password) ssh authentication method for runner [" + runnerType + "-" + descriptor.getName() + "] in [" + buildType.getName() + "]");
                    newRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_USERNAME_PWD);
                    buildType.updateBuildRunner(descriptor.getId(), descriptor.getName(), runnerType, newRunnerParams);
                }
                if (SSHRunnerConstants.SSH_EXEC_RUN_TYPE.equals(runnerType)) {
                    final String oldUsername = newRunnerParams.get(SSHRunnerConstants.PARAM_USERNAME);
                    final String oldPassword = newRunnerParams.get(SSHRunnerConstants.PARAM_PASSWORD);
                    if (StringUtil.isNotEmpty(oldUsername)) {
                        persistBuildType = true;
                        newRunnerParams.remove(SSHRunnerConstants.PARAM_USERNAME);
                        newRunnerParams.put(DeployerRunnerConstants.PARAM_USERNAME, oldUsername);
                    }
                    if (StringUtil.isNotEmpty(oldPassword)) {
                        persistBuildType = true;
                        newRunnerParams.remove(SSHRunnerConstants.PARAM_PASSWORD);
                        newRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, oldPassword);
                    }
                }
            }

            if (DeployerRunnerConstants.FTP_RUN_TYPE.equals(runnerType)) {
                final String ftpAuthMethod = newRunnerParams.get(FTPRunnerConstants.PARAM_AUTH_METHOD);
                if (StringUtil.isEmpty(ftpAuthMethod)) {
                    persistBuildType = true;
                    Loggers.SERVER.debug("Setting ftp auth authentication method for runner [" + runnerType + "-" + descriptor.getName() + "] in [" + buildType.getName() + "]");
                    final String username = newRunnerParams.get(DeployerRunnerConstants.PARAM_USERNAME);
                    if (StringUtil.isEmpty(username)) {
                        newRunnerParams.put(FTPRunnerConstants.PARAM_AUTH_METHOD, FTPRunnerConstants.AUTH_METHOD_ANONYMOUS);
                    } else {
                        newRunnerParams.put(FTPRunnerConstants.PARAM_AUTH_METHOD, FTPRunnerConstants.AUTH_METHOD_USER_PWD);
                    }
                    buildType.updateBuildRunner(descriptor.getId(), descriptor.getName(), runnerType, newRunnerParams);
                }
            }
        }
        if (persistBuildType) {
            buildType.persist();
        }
    }

    @Override
    public void serverStartup() {
        myServer.removeListener(this);
        Loggers.SERVER.debug("Server started up, will not convert passwords in configurations from now on.");
    }
}
