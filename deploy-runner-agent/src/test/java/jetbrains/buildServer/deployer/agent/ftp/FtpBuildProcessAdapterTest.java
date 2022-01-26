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

package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.NetworkUtil;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.BaseDeployerTest;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.util.WaitFor;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.aspectj.util.FileUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.*;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 4:22 PM
 */
public class FtpBuildProcessAdapterTest extends BaseDeployerTest {

  private FtpServer myServer;
  private File myRemoteDir;
  private int testPort;

  private final String myUsername = "myUsername";
  private final String myPassword = "myPassword";
  private List<ArtifactsCollection> myArtifactsCollections;
  private BuildRunnerContext myContext;
  private final Map<String, String> myRunnerParameters = new HashMap<String, String>();
  private final Map<String, String> mySharedConfigParameters = new HashMap<String, String>();
  private List<String> myResultingLog;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    myResultingLog = new LinkedList<String>();

    myRunnerParameters.put(FTPRunnerConstants.PARAM_FTP_MODE, "PASSIVE");
    myArtifactsCollections = new ArrayList<ArtifactsCollection>();
    myRemoteDir = createTempDir();
    final FtpServerFactory serverFactory = new FtpServerFactory();

    final ListenerFactory factory = new ListenerFactory();
    testPort = NetworkUtil.getFreePort(DEPLOYER_DEFAULT_PORT);
    factory.setPort(testPort);

    DataConnectionConfigurationFactory dataConnectionConfigurationFactory = new DataConnectionConfigurationFactory();
    dataConnectionConfigurationFactory.setActiveEnabled(false);
    factory.setDataConnectionConfiguration(dataConnectionConfigurationFactory.createDataConnectionConfiguration());

    final SslConfigurationFactory ssl = new SslConfigurationFactory();
    ssl.setKeystoreFile(getTestResource("ftpserver.jks"));
    ssl.setKeystorePassword("password");
    factory.setSslConfiguration(ssl.createSslConfiguration());
    serverFactory.addListener("default", factory.createListener());

    myServer = serverFactory.createServer();

    final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
    userManagerFactory.setFile(createTempFile());
    userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor());

    // init user manager
    final UserManager userManager = userManagerFactory.createUserManager();
    serverFactory.setUserManager(userManager);

    // add user
    final BaseUser user = new BaseUser();
    user.setName(myUsername);
    user.setPassword(myPassword);
    user.setHomeDirectory(myRemoteDir.getCanonicalPath());

    // give write permissions
    final List<Authority> authorities = new ArrayList<Authority>();
    final Authority auth = new WritePermission();
    authorities.add(auth);
    user.setAuthorities(authorities);

    userManager.save(user);

    // start the server
    myServer.start();

    Mockery mockeryCtx = new Mockery();
    myContext = mockeryCtx.mock(BuildRunnerContext.class);
    final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
    final BuildProgressLogger logger = new NullBuildProgressLogger() {
      @Override
      public void message(String message) {
        myResultingLog.add(message);
      }
    };
    final File workingDir = createTempDir();

    mockeryCtx.checking(new Expectations() {{
      allowing(myContext).getWorkingDirectory();
      will(returnValue(workingDir));
      allowing(myContext).getBuild();
      will(returnValue(build));
      allowing(myContext).getRunnerParameters();
      will(returnValue(myRunnerParameters));
      allowing(build).getBuildLogger();
      will(returnValue(logger));
      allowing(build).getSharedConfigParameters();
      will(returnValue(mySharedConfigParameters));
    }});
  }

  @AfterMethod
  @Override
  public void tearDown() throws Exception {
    myServer.stop();
    super.tearDown();
  }

  @Test
  public void testSimpleTransfer() throws Exception {
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(),
        "dest1",
        "dest2",
        "dest3",
        "dest4",
        "dest5",
        "dest6",
        "dest7",
        "dest8",
        "dest9",
        "dest10",
        "dest11",
        "dest12",
        "dest13",
        "dest14",
        "dest15",
        "dest16",
        "dest17",
        "dest18",
        "dest19",
        "dest20",
        "dest21"));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);

    assertTrue(myResultingLog.contains("< and continued >"));
  }

  @Test
  public void testTransferInActiveMode() throws Exception {
    myRunnerParameters.put(FTPRunnerConstants.PARAM_FTP_MODE, "ACTIVE");
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1", "dest2"));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort);
    process.start();
    new WaitFor(5000) {
      @Override
      protected boolean condition() {
        return process.isFinished();
      }
    };
    assertTrue("Failed to finish test in time", process.isFinished());
    assertEquals(BuildFinishedStatus.FINISHED_FAILED, process.waitFor());
  }

  @Test
  public void testTransferToRelativePath() throws Exception {
    final String subPath = "test_path/subdir";
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1", "dest2"));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort + "/" + subPath);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
  }


  @Test
  public void testTransferToRelativeSubPath() throws Exception {
    final String subPath = "test_path/subdir";
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1/subdest1", "dest2/subdest2"));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort + "/" + subPath);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
  }

  @Test
  public void testTransferToEmptyPath() throws Exception {
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), ""));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
  }

  @Test
  public void testTransferToDot() throws Exception {
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "."));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
  }


  @Test
  public void testTransferToExistingPath() throws Exception {
    final String uploadDestination = "some/path";
    final String artifactDestination = "dest1/sub";

    final File existingPath = new File(myRemoteDir, uploadDestination);
    assertTrue(existingPath.mkdirs());
    final File existingDestination = new File(existingPath, artifactDestination);
    assertTrue(existingDestination.mkdirs());

    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), artifactDestination, "dest2"));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort + "/" + uploadDestination);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(existingPath, myArtifactsCollections);
  }

  @Test
  public void testTransferUTF8BOM() throws Exception {

    myRunnerParameters.put(FTPRunnerConstants.PARAM_TRANSFER_MODE, FTPRunnerConstants.TRANSFER_MODE_BINARY);
    File sourceXml = new File("src/test/resources/data.xml");
    if (!sourceXml.exists()) {
      sourceXml = new File("deploy-runner-agent/src/test/resources/data.xml");
    }

    Map<File, String> map = new HashMap<File, String>();
    map.put(sourceXml, "");
    myArtifactsCollections.add(new ArtifactsCollection("", "", map));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort);
    DeployTestUtils.runProcess(process, 5000);

    File[] files = myRemoteDir.listFiles();
    assertNotNull(files);

    assertEquals(files[0].length(), sourceXml.length());
  }

  @Test
  public void testSecureConnection() throws Exception {
//        Following code can help to test real certificates
//        System.setProperty("javax.net.ssl.trustStore", getTestResource("ftpserver.jks").getAbsolutePath());
//        System.setProperty("javax.net.ssl.trustStorePassword", "password");

    myRunnerParameters.put(FTPRunnerConstants.PARAM_SSL_MODE, "2");
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1", "dest2"));
    final BuildProcess process = getProcess("localhost:" + testPort);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
  }

  @Test
  public void testNotAuthorized() throws Exception {
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1", "dest2"));
    final BuildProcess process = new FtpBuildProcessAdapter(myContext, "127.0.0.1:" + testPort, myUsername, "wrongpassword", myArtifactsCollections);

    process.start();
    new WaitFor(5000) {
      @Override
      protected boolean condition() {
        return process.isFinished();
      }
    };
    assertTrue("Failed to finish test in time", process.isFinished());
    assertEquals(process.waitFor(), BuildFinishedStatus.FINISHED_FAILED);
    assertEquals(FileUtil.listFiles(myRemoteDir).length, 0);
  }

  private BuildProcess getProcess(String target) {
    return new FtpBuildProcessAdapter(myContext, target, myUsername, myPassword, myArtifactsCollections);
  }
}
