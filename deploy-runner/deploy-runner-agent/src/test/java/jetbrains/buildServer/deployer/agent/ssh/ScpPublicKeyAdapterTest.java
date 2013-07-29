package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.ssh.scp.ScpProcessAdapter;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import org.testng.annotations.BeforeMethod;

/**
 * Created by Nikita.Skvortsov
 * Date: 1/21/13, 7:48 PM
 */
public class ScpPublicKeyAdapterTest extends BaseSSHTransferTest {

    @BeforeMethod
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_CUSTOM_KEY);
        myRunnerParams.put(SSHRunnerConstants.PARAM_KEYFILE, FileUtil.getRelativePath(myWorkingDir, myPrivateKey));
        myRunnerParams.put(DeployerRunnerConstants.PARAM_USERNAME, myUsername);
        myRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, "passphrase");
        myRunnerParams.put(SSHRunnerConstants.PARAM_PORT, String.valueOf(PORT_NUM));
    }

    @Override
    protected BuildProcess getProcess(String targetBasePath) throws Exception {
        myRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, targetBasePath);

        final SSHSessionProvider provider = new SSHSessionProvider(myContext, myInternalProperties);
        return new ScpProcessAdapter(myContext, myArtifactsCollections, provider);
    }
}
