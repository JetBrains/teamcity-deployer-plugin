package jetbrains.buildServer.deployer.server;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
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
        return DeployerRunnerConstants.SSH_RUN_TYPE;
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

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL));
        final String port = parameters.get(SSHRunnerConstants.PARAM_PORT);
        if (StringUtil.isNotEmpty(port)) {
            sb.append(" Port: ").append(port);
        }
        sb.append('\n');
        sb.append("Protocol: ").append(parameters.get(SSHRunnerConstants.PARAM_TRANSPORT)).append('\n');
        sb.append("Artifacts paths: ").append(parameters.get(DeployerRunnerConstants.PARAM_SOURCE_PATH));
        return sb.toString();
    }
}
