package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SmbDeployerRunType extends RunType {

    private final PluginDescriptor myDescriptor;

    public SmbDeployerRunType(@NotNull final RunTypeRegistry registry,
                              @NotNull final PluginDescriptor descriptor) {
        registry.registerRunType(this);
        myDescriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.SMB_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "SMB Deployer";
    }

    @Override
    public String getDescription() {
        return "Runner able to deploy build artifacts via SMB (Windows share)";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "editSmbDeployerParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "viewSmbDeployerParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target SMB share: ").append(parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL));
        return sb.toString();
    }
}
