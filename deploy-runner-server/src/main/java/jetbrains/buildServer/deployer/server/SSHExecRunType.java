package jetbrains.buildServer.deployer.server;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        return new SSHDeployerPropertiesProcessor();
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

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(parameters.get(DeployerRunnerConstants.PARAM_TARGET_URL));
        final String port = parameters.get(SSHRunnerConstants.PARAM_PORT);
        if (StringUtil.isNotEmpty(port)) {
            sb.append('\n').append(" Port: ").append(port);
        }
        sb.append('\n');
        final String commands = parameters.get(SSHRunnerConstants.PARAM_COMMAND);
        if (commands != null) {
            final List<String> commandsList = Arrays.asList(commands.split("\\\\n"));
            final int size = commandsList.size();
            if (size > 0) {
                sb.append("Commands: ").append(commandsList.get(0));
                if (size > 1) {
                    sb.append(" <and ").append(size - 1).append(" more line").append(size > 2 ? "s" : "").append(">");
                }
                return sb.toString();
            }
        }
        return sb.append("No commands defined").toString();
    }
}
