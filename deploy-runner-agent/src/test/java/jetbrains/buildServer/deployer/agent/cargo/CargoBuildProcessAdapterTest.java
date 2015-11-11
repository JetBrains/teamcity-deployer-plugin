package jetbrains.buildServer.deployer.agent.cargo;

import com.intellij.openapi.util.io.StreamUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.deployer.agent.BaseDeployerTest;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat7xStandaloneLocalConfiguration;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 4:22 PM
 */
public class CargoBuildProcessAdapterTest extends BaseDeployerTest {

  private static final int TEST_PORT = 55369;

  private BuildRunnerContext myContext;
  private final Map<String, String> myRunnerParameters = new HashMap<String, String>();
  private InstalledLocalContainer myTomcat;
  private File workingDir;


  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    final File extractDir = myTempFiles.createTempDir();
    final File cfgDir = myTempFiles.createTempDir();

    final String fileName = "apache-tomcat-7.0.54.zip";
    final File zipDistribution = getTestResource(fileName);
    final ZipURLInstaller installer = new ZipURLInstaller(zipDistribution.toURI().toURL());

    installer.setExtractDir(extractDir.getAbsolutePath());
    installer.install();

    LocalConfiguration configuration = new Tomcat7xStandaloneLocalConfiguration(cfgDir.getAbsolutePath());
    configuration.setProperty(ServletPropertySet.PORT, String.valueOf(TEST_PORT));
    configuration.setProperty(ServletPropertySet.USERS, "tomcat:tomcat:manager-script");

    myTomcat = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(
        "tomcat7x", ContainerType.INSTALLED, configuration);

    myTomcat.setHome(installer.getHome());

    myTomcat.start();

    Mockery mockeryCtx = new Mockery();
    myContext = mockeryCtx.mock(BuildRunnerContext.class);
    final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
    final BuildProgressLogger logger = new NullBuildProgressLogger();
    workingDir = myTempFiles.createTempDir();

    mockeryCtx.checking(new Expectations() {{
      allowing(myContext).getWorkingDirectory();
      will(returnValue(workingDir));
      allowing(myContext).getBuild();
      will(returnValue(build));
      allowing(myContext).getRunnerParameters();
      will(returnValue(myRunnerParameters));
      allowing(build).getBuildLogger();
      will(returnValue(logger));
    }});

    myRunnerParameters.put(DeployerRunnerConstants.PARAM_CONTAINER_TYPE, "tomcat7x");
  }

  @AfterMethod
  @Override
  public void tearDown() throws Exception {
    myTomcat.stop();
    super.tearDown();
  }

  @Test
  public void testSimpleDeploy() throws Exception {
    deployTestResourceAsArtifact("simple.war", "simple.war");
    assertUrlReturns(new URL("http://127.0.0.1:" + TEST_PORT + "/simple"), "Hello!  The time is now");
  }

  @Test
  public void testReDeploy() throws Exception {
    final URL url = new URL("http://127.0.0.1:" + TEST_PORT + "/simple");

    deployTestResourceAsArtifact("simple.war", "simple.war");
    assertUrlReturns(url, "Hello!  The time is now");

    deployTestResourceAsArtifact("simple2.war", "simple.war");
    assertUrlReturns(url, "Hello v2!  The time is now");
  }


  @Test
  public void testDeployWithContext() throws Exception {
    deployTestResourceAsArtifact("simple-with-context.war", "simple-with-context.war");
    assertUrlReturns(new URL("http://127.0.0.1:" + TEST_PORT + "/somepath/myapp"), "Hello!  The time is now");
  }


  @Test
  public void testDeployRootContext() throws Exception {
    deployTestResourceAsArtifact("simple.war", "ROOT.war");
    assertUrlReturns(new URL("http://127.0.0.1:" + TEST_PORT + "/"), "Hello!  The time is now");
  }


  @Test
  public void testReDeployRootContext() throws Exception {
    final URL url = new URL("http://127.0.0.1:" + TEST_PORT + "/");

    deployTestResourceAsArtifact("simple.war", "ROOT.war");
    assertUrlReturns(url, "Hello!  The time is now");

    deployTestResourceAsArtifact("simple2.war", "ROOT.war");
    assertUrlReturns(url, "Hello v2!  The time is now");
  }

  private void assertUrlReturns(URL url, String expected2) throws IOException {
    final InputStream stream2 = url.openStream();
    final String text2 = StreamUtil.readText(stream2);
    assertThat(text2).contains(expected2);
  }


  private void deployTestResourceAsArtifact(String testResourceName, String artifactName) throws IOException, RunBuildException {
    FileUtil.copy(getTestResource(testResourceName), new File(workingDir, artifactName));
    final BuildProcess process = getProcess("127.0.0.1:" + TEST_PORT, artifactName);
    DeployTestUtils.runProcess(process, 5000);
  }

  private BuildProcess getProcess(String target, String sourcePath) {
    String username = "tomcat";
    String password = "tomcat";
    return new CargoBuildProcessAdapter(target, username, password, myContext, sourcePath);
  }
}
