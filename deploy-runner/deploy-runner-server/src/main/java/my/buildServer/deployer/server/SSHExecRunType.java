package my.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import my.buildServer.deployer.common.SSHRunnerConstants;
import my.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SSHExecRunType extends RunType {

    private final PluginDescriptor myDescriptor;

    public SSHExecRunType(@NotNull final RunTypeRegistry registry,
                          @NotNull final PluginDescriptor descriptor) {
        registry.registerRunType(this);
        myDescriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return SSHRunnerConstants.SSH_EXEC_RUN_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "SSH Exec";
    }

    @Override
    public String getDescription() {
        return "Runner able to execute commands over SSH";
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new DeployerPropertiesProcessor();
    }

    @Override
    public String getEditRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "editSSHExecParams.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath() {
        return  myDescriptor.getPluginResourcesPath() + "viewSSHExecParams.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<String, String>();
    }
}
