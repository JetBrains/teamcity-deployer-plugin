

package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.ssh.sftp.SftpBuildProcessAdapter;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

@Test
public class SftpProcessAdapterTest extends BaseSSHTransferTest {

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected BuildProcess getProcess(String targetBasePath) {
    myRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, targetBasePath);

    final SSHSessionProvider provider = new SSHSessionProvider(myContext, myInternalPropertiesHolder, mySshKeyManager, myKnownHostsManager);
    return new SftpBuildProcessAdapter(myContext, myArtifactsCollections, provider);
  }
}