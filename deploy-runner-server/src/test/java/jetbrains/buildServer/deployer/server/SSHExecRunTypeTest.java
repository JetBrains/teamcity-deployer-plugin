

package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 12/18/12, 4:15 PM
 */
@Test
public class SSHExecRunTypeTest extends DeployerRunTypeTest {

  SSHExecRunType myRunType;

  @Override
  protected void createRunType(RunTypeRegistry registry, PluginDescriptor descriptor) {
    myRunType = new SSHExecRunType(registry, descriptor);
  }

  public void testGetDescription() throws Exception {
    assertEquals(myRunType.getDescription(), "Runner able to execute commands over SSH");
  }

  public void testDescribeEmptyParameters() throws Exception {
    //noinspection unchecked
    myRunType.describeParameters(Collections.EMPTY_MAP);
  }

  public void testValidParameters() throws Exception {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(DeployerRunnerConstants.PARAM_TARGET_URL, "targethost");
    parameters.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_USERNAME_PWD);
    parameters.put(DeployerRunnerConstants.PARAM_USERNAME, "user");
    parameters.put(DeployerRunnerConstants.PARAM_PASSWORD, "password");
    parameters.put(SSHRunnerConstants.PARAM_COMMAND, "ls -la");

    final Collection<InvalidProperty> process = myRunType.getRunnerPropertiesProcessor().process(parameters);

    assertTrue(process.isEmpty());
  }
}