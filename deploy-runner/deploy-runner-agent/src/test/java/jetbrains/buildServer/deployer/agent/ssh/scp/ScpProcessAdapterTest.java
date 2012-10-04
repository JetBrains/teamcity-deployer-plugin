package jetbrains.buildServer.deployer.agent.ssh.scp;

import jetbrains.buildServer.deployer.agent.ssh.BaseSSHTransferTest;
import org.testng.annotations.Test;

@Test
public class ScpProcessAdapterTest extends BaseSSHTransferTest {

    @Override
    protected ScpProcessAdapter getProcess(String targetBasePath) {
        return new ScpProcessAdapter(myUsername, myPassword, targetBasePath, myArtifactsCollections);
    }
}
