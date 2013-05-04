package jetbrains.buildServer.deployer.agent.ssh.scp;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.ssh.BaseSSHTransferTest;
import jetbrains.buildServer.deployer.agent.ssh.SSHSessionProvider;

/**
 * Created by Nikita.Skvortsov
 * Date: 1/21/13, 7:48 PM
 */
public class ScpPublicKeyAdapterTest extends BaseSSHTransferTest {
    @Override
    protected BuildProcess getProcess(String targetBasePath) throws Exception {
        SSHSessionProvider provider = new SSHSessionProvider(targetBasePath, PORT_NUM, myUsername, "passphrase", myPrivateKey).invoke();
        return new ScpProcessAdapter(myContext, myArtifactsCollections, provider);
    }
}
