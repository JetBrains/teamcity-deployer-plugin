package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Created by Nikita.Skvortsov
 * Date: 7/25/13, 4:55 PM
 */
public class SMBDeployerRunTypeTest extends  DeployerRunTypeTest {
    private SmbDeployerRunType myRunType;
    private PropertiesProcessor processor;

    @Override
    protected void createRunType(RunTypeRegistry registry, PluginDescriptor descriptor) {
        myRunType = new SmbDeployerRunType(registry, descriptor);
        processor = myRunType.getRunnerPropertiesProcessor();
    }

    @Test
    public void testUNCPathsPattern() throws Exception {
        assertIllegalTarget("\\\\\\\\host");
        assertIllegalTarget("host");
        assertIllegalTarget("..abracadabra");
        assertIllegalTarget("\\\\host");
        assertLegalTarget("\\\\host\\share");
        assertLegalTarget("\\\\host\\c$");
        assertLegalTarget("\\\\host\\share\\subdir");
    }

    private void assertIllegalTarget(String value) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(DeployerRunnerConstants.PARAM_TARGET_URL, value);
        final Collection<InvalidProperty> invalidProperties = processor.process(properties);
        assertEquals(invalidProperties.size(), 1, "Should report 1 invalid property");
        InvalidProperty next = invalidProperties.iterator().next();
        assertEquals(next.getPropertyName(), DeployerRunnerConstants.PARAM_TARGET_URL);
    }

    private void assertLegalTarget(String value) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(DeployerRunnerConstants.PARAM_TARGET_URL, value);
        final Collection<InvalidProperty> invalidProperties = processor.process(properties);
        assertEquals(invalidProperties.size(), 0, "Should not report any invalid property");
    }
}

