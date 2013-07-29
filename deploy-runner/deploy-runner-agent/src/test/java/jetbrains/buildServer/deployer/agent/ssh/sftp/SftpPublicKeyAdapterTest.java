package jetbrains.buildServer.deployer.agent.ssh.sftp;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.ssh.BaseSSHTransferTest;
import jetbrains.buildServer.deployer.agent.ssh.SSHSessionProvider;

/**
 * Created by Nikita.Skvortsov
 * Date: 1/21/13, 6:39 PM
 */
public class SftpPublicKeyAdapterTest extends BaseSSHTransferTest {
    @Override
    protected BuildProcess getProcess(String targetBasePath) throws Exception {
        final SSHSessionProvider provider = new SSHSessionProvider(targetBasePath, PORT_NUM, myUsername, "passphrase", myPrivateKey);
        return new SftpBuildProcessAdapter(myContext, myArtifactsCollections, provider);
    }
}
