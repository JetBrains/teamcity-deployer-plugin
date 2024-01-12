

package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;

/**
 * Created by Nikita.Skvortsov
 * Date: 7/25/13, 4:54 PM
 */
public abstract class DeployerRunTypeTest {
  Mockery myContext;

  @BeforeMethod
  public void setUp() {
    myContext = new Mockery();
    final RunTypeRegistry registry = myContext.mock(RunTypeRegistry.class);
    final PluginDescriptor descriptor = myContext.mock(PluginDescriptor.class);
    myContext.checking(new Expectations() {{
      allowing(registry).registerRunType(with(aNonNull(SSHExecRunType.class)));
    }});
    createRunType(registry, descriptor);
  }

  protected abstract void createRunType(RunTypeRegistry registry, PluginDescriptor descriptor);
}