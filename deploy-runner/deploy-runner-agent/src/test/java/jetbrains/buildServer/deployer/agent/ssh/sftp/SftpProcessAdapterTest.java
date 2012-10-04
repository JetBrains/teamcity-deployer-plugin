package jetbrains.buildServer.deployer.agent.ssh.sftp;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.ssh.BaseSSHTransferTest;
import org.testng.annotations.Test;

@Test
public class SftpProcessAdapterTest extends BaseSSHTransferTest {
    @Override
    protected BuildProcess getProcess(String targetBasePath) {
        return new SftpBuildProcessAdapter(targetBasePath, myUsername, myPassword, myContext, myArtifactsCollections);
    }
}
