package jetbrains.buildServer.deployer.agent.ssh.scp;

import jetbrains.buildServer.deployer.agent.ssh.BaseSSHTransferTest;
import jetbrains.buildServer.deployer.agent.ssh.SSHSessionProvider;
import org.testng.annotations.Test;

@Test
public class ScpProcessAdapterTest extends BaseSSHTransferTest {

    @Override
    protected ScpProcessAdapter getProcess(String targetBasePath) throws Exception {
        final SSHSessionProvider provider = new SSHSessionProvider(targetBasePath, PORT_NUM, myUsername, myPassword, null);
        return new ScpProcessAdapter(myContext, myArtifactsCollections, provider);
    }
}
