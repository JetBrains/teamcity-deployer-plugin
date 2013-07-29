package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.deployer.agent.ssh.scp.ScpProcessAdapter;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class ScpProcessAdapterTest extends BaseSSHTransferTest {

    @BeforeMethod
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_USERNAME_PWD);
        myRunnerParams.put(DeployerRunnerConstants.PARAM_USERNAME, myUsername);
        myRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, myPassword);
        myRunnerParams.put(SSHRunnerConstants.PARAM_PORT, String.valueOf(PORT_NUM));
    }

    @Override
    protected ScpProcessAdapter getProcess(String targetBasePath) throws Exception {
        myRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, targetBasePath);

        final SSHSessionProvider provider = new SSHSessionProvider(myContext, myInternalProperties);
        return new ScpProcessAdapter(myContext, myArtifactsCollections, provider);
    }
}
