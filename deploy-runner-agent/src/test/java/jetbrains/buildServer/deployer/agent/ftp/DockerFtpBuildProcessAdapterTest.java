/*
 * Copyright 2000-2021 JetBrains s.r.o.
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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static org.testcontainers.utility.DockerImageName.*;

public class DockerFtpBuildProcessAdapterTest extends BaseDeployerTest {

  private File myRemoteDir;
  private int testPort;

  private final String myUsername = "guest";
  private final String myPassword = "guest";
  private List<ArtifactsCollection> myArtifactsCollections;
  private BuildRunnerContext myContext;
  private final Map<String, String> myRunnerParameters = new HashMap<String, String>();
  private final Map<String, String> mySharedConfigParameters = new HashMap<String, String>();
  private List<String> myResultingLog;
  private FixedHostPortGenericContainer ftp = new FixedHostPortGenericContainer("loicmathieu/vsftpd")
          .withFixedExposedPort(2020, 20)
          .withFixedExposedPort(2021, 21)
          .withFixedExposedPort(21100, 21100)
          .withFixedExposedPort(21101, 21101)
          .withFixedExposedPort(21102, 21102)
          .withFixedExposedPort(21103, 21103)
          .withFixedExposedPort(21104, 21104)
          .withFixedExposedPort(21105, 21105)
          .withFixedExposedPort(21106, 21106)
          .withFixedExposedPort(21107, 21107)
          .withFixedExposedPort(21108, 21108)
          .withFixedExposedPort(21109, 21109)
          .withFixedExposedPort(21110, 21110)
          ;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    myResultingLog = new LinkedList<String>();
    myRunnerParameters.put(FTPRunnerConstants.PARAM_FTP_MODE, "PASSIVE");
    myArtifactsCollections = new ArrayList<ArtifactsCollection>();
    myRemoteDir = createTempDir();

    final ListenerFactory factory = new ListenerFactory();
    ftp.addFileSystemBind(myRemoteDir.getAbsolutePath(), "/home/guest", BindMode.READ_WRITE);
    ftp.setCommand("ftps");
    ftp.start();
    testPort = 2021;

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
    ftp.stop();
    super.tearDown();
  }

  @Test
  public void testSimpleTransfer() throws Exception {
    myRunnerParameters.put(FTPRunnerConstants.PARAM_SSL_MODE, "2");
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

  private BuildProcess getProcess(String target) {
    return new FtpBuildProcessAdapter(myContext, target, myUsername, myPassword, myArtifactsCollections);
  }
}
