package jetbrains.buildServer.deployer.agent.ssh.scp;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.ssh.BaseSSHTransferTest;

/**
 * Created by Nikita.Skvortsov
 * Date: 1/21/13, 7:48 PM
 */
public class ScpPublicKeyAdapterTest extends BaseSSHTransferTest {
    @Override
    protected BuildProcess getProcess(String targetBasePath) {
        return new ScpProcessAdapter(myPrivateKey, myUsername, "passphrase", targetBasePath, PORT_NUM, myContext, myArtifactsCollections);
    }
}
