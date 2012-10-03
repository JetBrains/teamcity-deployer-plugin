package my.buildServer.deployer.agent.ssh.sftp;

import jetbrains.buildServer.agent.BuildProcess;
import my.buildServer.deployer.agent.ssh.BaseSSHTransferTest;

public class SftpProcessAdapterTest extends BaseSSHTransferTest {
    @Override
    protected BuildProcess getProcess(String targetBasePath) {
        return new SftpBuildProcessAdapter(targetBasePath, myUsername, myPassword, myContext, myArtifactsCollections);
    }
}
