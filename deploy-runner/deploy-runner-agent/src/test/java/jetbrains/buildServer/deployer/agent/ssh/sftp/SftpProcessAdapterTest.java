package jetbrains.buildServer.deployer.agent.ssh.sftp;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.ssh.BaseSSHTransferTest;
import jetbrains.buildServer.deployer.agent.ssh.SSHSessionProvider;
import org.testng.annotations.Test;

@Test
public class SftpProcessAdapterTest extends BaseSSHTransferTest {
    @Override
    protected BuildProcess getProcess(String targetBasePath) throws Exception {
        final SSHSessionProvider provider = new SSHSessionProvider(targetBasePath, PORT_NUM, myUsername, myPassword, null);
        return new SftpBuildProcessAdapter(myContext, myArtifactsCollections, provider);
    }
}
