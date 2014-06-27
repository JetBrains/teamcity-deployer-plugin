package jetbrains.buildServer.deployer.server.cargo;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.server.DeployerPropertiesProcessor;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CargoDeployerRunType extends RunType {

    private final PluginDescriptor myDescriptor;

    public CargoDeployerRunType(@NotNull final RunTypeRegistry registry,
                                @NotNull final PluginDescriptor descriptor) {
        registry.registerRunType(this);
        myDescriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.CARGO_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Container Deployer";
    }

    @Override
    public String getDescription() {
        return "Runner able to deploy WAR apps to different containers";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "editCargoDeployerParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "viewCargoDeployerParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> parameters) {
        return "Target container url: " + parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL);
    }
}
