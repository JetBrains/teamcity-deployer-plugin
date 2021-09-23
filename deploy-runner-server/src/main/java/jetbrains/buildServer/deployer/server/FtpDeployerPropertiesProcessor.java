package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;

import java.util.Collection;
import java.util.Map;

public class FtpDeployerPropertiesProcessor extends DeployerPropertiesProcessor {

    @Override
    public Collection<InvalidProperty> process(Map<String, String> properties) {
        if (!FTPRunnerConstants.AUTH_METHOD_USER_PWD.equals(properties.get(FTPRunnerConstants.PARAM_AUTH_METHOD))) {
            properties.remove(DeployerRunnerConstants.PARAM_USERNAME);
            properties.remove(DeployerRunnerConstants.PARAM_PASSWORD);
        }
        return super.process(properties);
    }
}
