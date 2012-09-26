package my.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SftpDeployerRunType extends RunType {

    private final PluginDescriptor myDescriptor;

    public SftpDeployerRunType(@NotNull final RunTypeRegistry registry,
                               @NotNull final PluginDescriptor descriptor) {
        registry.registerRunType(this);
        myDescriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.SFTP_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "SFTP Deployer";
    }

    @Override
    public String getDescription() {
        return "Runner able to deploy build artifacts via SFTP (SSH file transfer protocol)";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "editSftpDeployerParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "viewSftpDeployerParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }
}
