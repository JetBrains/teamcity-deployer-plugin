package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.assertEquals;

/**
 * Created by Nikita.Skvortsov
 * Date: 12/18/12, 4:15 PM
 */
@Test
public class SSHExecRunTypeTest {

    SSHExecRunType myRunType;
    Mockery myContext;

    @BeforeMethod
    public void setUp() {
        myContext = new Mockery();
        final RunTypeRegistry registry = myContext.mock(RunTypeRegistry.class);
        final PluginDescriptor descriptor = myContext.mock(PluginDescriptor.class);
        myContext.checking(new Expectations(){{
            allowing(registry).registerRunType(with(aNonNull(SSHExecRunType.class)));
        }});
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
