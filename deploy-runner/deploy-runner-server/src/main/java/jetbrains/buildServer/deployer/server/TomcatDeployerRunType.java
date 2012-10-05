package jetbrains.buildServer.deployer.server;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TomcatDeployerRunType extends RunType {

    private final PluginDescriptor myDescriptor;

    public TomcatDeployerRunType(@NotNull final RunTypeRegistry registry,
                                 @NotNull final PluginDescriptor descriptor) {
        registry.registerRunType(this);
        myDescriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return DeployerRunnerConstants.TOMCAT_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Tomcat Deployer";
    }

    @Override
    public String getDescription() {
        return "Runner able to deploy WAR apps to Tomcat server";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "editTomcatDeployerParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "viewTomcatDeployerParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> parameters) {
        final StringBuilder result = new StringBuilder();
        result.append("Target Tomcat url: ").append(parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL));
        result.append('\n');
        result.append("WAR archive: ").append(parameters.get(DeployerRunnerConstants.PARAM_SOURCE_PATH));
        final String customContext = parameters.get(DeployerRunnerConstants.PARAM_TOMCAT_CONTEXT_PATH);
        if (StringUtil.isNotEmpty(customContext)) {
            result.append('\n');
            result.append("Web app context: ").append(customContext);
        }
        return result.toString();
    }
}
