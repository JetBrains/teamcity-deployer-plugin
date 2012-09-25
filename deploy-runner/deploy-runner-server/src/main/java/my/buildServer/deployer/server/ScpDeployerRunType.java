package my.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ScpDeployerRunType extends RunType {

    public ScpDeployerRunType(@NotNull final RunTypeRegistry registry) {
        registry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.SCP_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "SCP Deployer";
    }

    @Override
    public String getDescription() {
        return "Runner able to deploy build artifacts via SCP (secure copy protocol)";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return "editScpDeployerParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  "viewScpDeployerParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }
}
