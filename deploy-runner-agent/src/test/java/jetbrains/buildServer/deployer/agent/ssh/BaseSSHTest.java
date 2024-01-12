

package jetbrains.buildServer.deployer.agent.ssh;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.NetworkUtil;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.deployer.agent.BaseDeployerTest;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.AbstractFileKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.util.*;

/**
 * Created by User
 * date: 29.07.13.
 */
public class BaseSSHTest extends BaseDeployerTest {

  private static final int SSH_DEFAULT_PORT = 15655;
  static final String HOST_ADDR = "127.0.0.1";
  final Map<String, String> myRunnerParams = new HashMap<String, String>();
  final Map<String, String> myInternalProperties = new HashMap<String, String>();

  final InternalPropertiesHolder myInternalPropertiesHolder = (s, s2) -> myInternalProperties.get(s) != null ? myInternalProperties.get(s) : s2;

  File myWorkingDir;
  private String myUsername = "testuser";
  private String myPassword = "testpassword";
  List<ArtifactsCollection> myArtifactsCollections;
  BuildRunnerContext myContext;
  File myPassphraselessKey;
  File myPrivateKey;
  File myRemoteDir = null;
  protected SshServer myServer;
  int testPort;

  AgentRunningBuildSshKeyManager mySshKeyManager;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    myRemoteDir = createTempDir();

    myServer = SshServer.setUpDefaultServer();
    testPort = NetworkUtil.getFreePort(SSH_DEFAULT_PORT);
    myServer.setPort(testPort);
    myServer.setCommandFactory(new ScpCommandFactory());
    myServer.setShellFactory(new ProcessShellFactory(SystemInfo.isWindows ? "cmd" : "sh"));
    myServer.setPasswordAuthenticator((username, password, session) -> myUsername.equals(username) && myPassword.equals(password));

    final File keyFile = getTestResource("hostkey.pem");
    myPrivateKey = getTestResource("tmp_rsa").getAbsoluteFile();
    myPassphraselessKey = getTestResource("passphraseless").getAbsoluteFile();

    myServer.setPublickeyAuthenticator((username, key, session) -> true);
    AbstractFileKeyPairProvider fileKeyPairProvider = SecurityUtils.createFileKeyPairProvider();
    fileKeyPairProvider.setFiles(Collections.singletonList(keyFile.getCanonicalFile()));
    myServer.setKeyPairProvider(fileKeyPairProvider);

    myServer.setFileSystemFactory(new VirtualFileSystemFactory(myRemoteDir.toPath()));
    myServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));

    myServer.start();

    Mockery mockeryCtx = new Mockery();
    myContext = mockeryCtx.mock(BuildRunnerContext.class);
    final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
    final BuildProgressLogger logger = new NullBuildProgressLogger();
    myWorkingDir = createTempDir();

    mySshKeyManager = mockeryCtx.mock(AgentRunningBuildSshKeyManager.class);
    final TeamCitySshKey sshKey = new TeamCitySshKey("Name", FileUtil.loadFileBytes(myPrivateKey), false);

    mockeryCtx.checking(new Expectations() {{
      allowing(myContext).getWorkingDirectory();
      will(returnValue(myWorkingDir));
      allowing(myContext).getBuild();
      will(returnValue(build));
      allowing(myContext).getRunnerParameters();
      will(returnValue(myRunnerParams));
      allowing(build).getBuildLogger();
      will(returnValue(logger));
      allowing(build).getCheckoutDirectory();
      will(returnValue(myWorkingDir));
      allowing(mySshKeyManager).getKey("key_id_value");
      will(returnValue(sshKey));
    }});

    myArtifactsCollections = new ArrayList<>();


    myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_USERNAME_PWD);
    myRunnerParams.put(DeployerRunnerConstants.PARAM_USERNAME, myUsername);
    myRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, myPassword);

    myRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, HOST_ADDR);
    myRunnerParams.put(SSHRunnerConstants.PARAM_PORT, String.valueOf(testPort));

    /* Uncomment to enable JSch debug logging
    JSch.setLogger(new Logger() {
      @Override
      public boolean isEnabled(int level) {
        return true;
      }

      @Override
      public void log(int level, String message) {
        System.out.println(message);
      }
    });
    */
  }

  @AfterMethod
  @Override
  public void tearDown() throws Exception {
    myServer.stop(true);
    super.tearDown();
  }
}