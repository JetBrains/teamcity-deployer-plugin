package my.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SSHDeployerRunType extends RunType {

    private final PluginDescriptor myDescriptor;

    public SSHDeployerRunType(@NotNull final RunTypeRegistry registry,
                              @NotNull final PluginDescriptor descriptor) {
        myDescriptor = descriptor;
        registry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.SCP_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "SSH Deployer";
    }

    @Override
    public String getDescription() {
        return "Runner able to deploy build artifacts via SSH";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return myDescriptor.getPluginResourcesPath() + "editSSHDeployerParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "viewSSHDeployerParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }
}
