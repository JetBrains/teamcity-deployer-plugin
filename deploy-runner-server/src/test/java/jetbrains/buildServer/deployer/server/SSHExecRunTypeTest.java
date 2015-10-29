package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.assertEquals;

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
}
