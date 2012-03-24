package my.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kit
 * Date: 24.03.12 - 16:52
 */
public class DeployerRunType extends RunType {

    public DeployerRunType(@NotNull final RunTypeRegistry registry) {
        registry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.DEPLOYER_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Deployer";
    }

    @Override
    public String getDescription() {
        return "Runner able to deploy build artifacts to various targets";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return "editDeployerParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  "viewDeployerParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }
}
