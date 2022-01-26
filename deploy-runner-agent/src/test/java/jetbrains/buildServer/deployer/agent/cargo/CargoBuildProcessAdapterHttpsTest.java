/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.deployer.agent.cargo;

import com.intellij.openapi.util.io.StreamUtil;
import jetbrains.buildServer.NetworkUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.deployer.agent.BaseDeployerTest;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;
import jetbrains.buildServer.deployer.common.CargoRunnerConstants;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat7xStandaloneLocalConfiguration;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 4:22 PM
 */
public class CargoBuildProcessAdapterHttpsTest extends BaseDeployerTest {

  private int testPort;
  private BuildRunnerContext myContext;
  private final Map<String, String> myRunnerParameters = new HashMap<String, String>();
  private InstalledLocalContainer myTomcat;
  private File workingDir;
  private HostnameVerifier myDefaultHostnameVerifier;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {

    final File extractDir = createTempDir();
    final File cfgDir = createTempDir();

    final String fileName = "apache-tomcat-7.0.54.zip";
    final File zipDistribution = getTestResource(fileName);
    final ZipURLInstaller installer = new ZipURLInstaller(zipDistribution.toURI().toURL());

    installer.setExtractDir(extractDir.getAbsolutePath());
    installer.install();

    LocalConfiguration configuration = new Tomcat7xStandaloneLocalConfiguration(cfgDir.getAbsolutePath());
    testPort = NetworkUtil.getFreePort(DEPLOYER_DEFAULT_PORT);
    configuration.setProperty(ServletPropertySet.PORT, String.valueOf(testPort));
    configuration.setProperty(ServletPropertySet.USERS, "tomcat:tomcat:manager-script");

    configuration.setProperty(TomcatPropertySet.AJP_PORT, String.valueOf(NetworkUtil.getFreePort(8009)));
    configuration.setProperty(TomcatPropertySet.CONNECTOR_KEY_STORE_FILE, getTestResource("ftpserver.jks").getAbsolutePath());
    configuration.setProperty(TomcatPropertySet.CONNECTOR_KEY_STORE_PASSWORD, "password");
    configuration.setProperty(TomcatPropertySet.CONNECTOR_KEY_ALIAS, "localhost");
    configuration.setProperty(TomcatPropertySet.HTTP_SECURE, "true");
    configuration.setProperty(GeneralPropertySet.PROTOCOL, "https");

    myDefaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
      @Override
      public boolean verify(String s, SSLSession sslSession) {
        return true; // new versions of Java do not allow to connect to localhost by SSL, here we suppress this behavior
      }
    });

    myTomcat = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(
        "tomcat7x", ContainerType.INSTALLED, configuration);

    myTomcat.setHome(installer.getHome());

    myTomcat.start();

    Mockery mockeryCtx = new Mockery();
    myContext = mockeryCtx.mock(BuildRunnerContext.class);
    final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
    final BuildProgressLogger logger = new NullBuildProgressLogger();
    workingDir = createTempDir();

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
    myRunnerParameters.put(CargoRunnerConstants.USE_HTTPS, "true");
  }

  @AfterMethod
  @Override
  public void tearDown() throws Exception {
    myTomcat.stop();
    HttpsURLConnection.setDefaultHostnameVerifier(myDefaultHostnameVerifier);
    super.tearDown();
  }

  @Test
  public void testSimpleDeploy() throws Exception {
    enableDebug();
    deployTestResourceAsArtifact("simple.war", "simple.war");
    assertUrlReturns(new URL("https://localhost:" + testPort + "/simple"), "Hello!  The time is now");
  }

  private void assertUrlReturns(URL url, String expected2) throws IOException {
    final InputStream stream2 = url.openStream();
    final String text2 = StreamUtil.readText(stream2);
    assertTrue(text2.contains(expected2));
  }


  private void deployTestResourceAsArtifact(String testResourceName, String artifactName) throws IOException, RunBuildException {
    FileUtil.copy(getTestResource(testResourceName), new File(workingDir, artifactName));
    InetAddress addr = NetworkUtil.getSelfAddresses(null)[0];
    String hostname = addr.getHostName();

    final BuildProcess process = getProcess(hostname + ":" + testPort, artifactName);
    DeployTestUtils.runProcess(process, 5000);
  }

  private BuildProcess getProcess(String target, String sourcePath) {
    String username = "tomcat";
    String password = "tomcat";
    return new CargoBuildProcessAdapter(target, username, password, myContext, sourcePath);
  }
}
