

package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.deployer.agent.ssh.scp.ScpProcessAdapter;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class ScpProcessAdapterTest extends BaseSSHTransferTest {

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected ScpProcessAdapter getProcess(String targetBasePath) {
    myRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, targetBasePath);

    final SSHSessionProvider provider = new SSHSessionProvider(myContext, myInternalPropertiesHolder, mySshKeyManager, myKnownHostsManager);
    return new ScpProcessAdapter(myContext, myArtifactsCollections, provider);
  }
}