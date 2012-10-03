package my.buildServer.deployer.agent.ssh.scp;

import my.buildServer.deployer.agent.ssh.BaseSSHTransferTest;

public class ScpProcessAdapterTest extends BaseSSHTransferTest {

    @Override
    protected ScpProcessAdapter getProcess(String targetBasePath) {
        return new ScpProcessAdapter(myUsername, myPassword, targetBasePath, myArtifactsCollections);
    }


}
