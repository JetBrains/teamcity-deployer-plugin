package jetbrains.buildServer.deployer.server.converter;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nikita.Skvortsov
 * date: 29.07.13.
 */
public class PasswordParameterConverter extends BuildServerAdapter {

    private boolean myServerHasStarted = false;

    public PasswordParameterConverter(@NotNull SBuildServer server) {
        server.addListener(this);
    }



    @Override
    public void buildTypeRegistered(SBuildType buildType) {
        if (myServerHasStarted) {
            return;
        }
        boolean persistBuildType = false;
        for (SBuildRunnerDescriptor descriptor : buildType.getBuildRunners()) {
            Map<String,String> runnerParams = descriptor.getParameters();
            final String plainPassword = runnerParams.get(DeployerRunnerConstants.PARAM_PLAIN_PASSWORD);
            if (plainPassword != null) {
                persistBuildType = true;
                Loggers.SERVER.debug("Scrambling password for runner [" + descriptor.getType() + "-" + descriptor.getName() + "] in [" + buildType.getName() + "]");
                final Map<String,String> newRunnerParams = new HashMap<String, String>(runnerParams);
                newRunnerParams.remove(DeployerRunnerConstants.PARAM_PLAIN_PASSWORD);
                newRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, plainPassword);
                buildType.updateBuildRunner(descriptor.getId(), descriptor.getName(), descriptor.getType(), newRunnerParams);
            }
        }
        if (persistBuildType) {
            buildType.persist();
        }
    }

    @Override
    public void serverStartup() {
        myServerHasStarted = true;
        Loggers.SERVER.debug("Server started up, will not convert passwords in configurations from now on.");
    }
}
