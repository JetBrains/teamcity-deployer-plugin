package my.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:16
 */
public class DeployerPropertiesProcessor implements PropertiesProcessor {
    @Override
    public Collection<InvalidProperty> process(Map<String, String> properties) {
        return Collections.emptySet();
    }
}
