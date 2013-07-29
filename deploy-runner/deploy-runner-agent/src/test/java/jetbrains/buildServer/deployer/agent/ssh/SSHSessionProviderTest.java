package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * date: 29.07.13.
 */
@Test
public class SSHSessionProviderTest extends BaseSSHTest {

    public void testUsernamePassword() throws Exception {
        assertSessionIsConnected();
    }

    public void testCustomKey() throws Exception {
        myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_CUSTOM_KEY);
        myRunnerParams.put(SSHRunnerConstants.PARAM_KEYFILE, FileUtil.getRelativePath(myWorkingDir, myPrivateKey));
        myRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, "passphrase");
        assertSessionIsConnected();
    }

    public void testDefaultConfig() throws Exception {
        myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_DEFAULT_KEY);
        final File tempConfig = myTempFiles.createTempFile("Host *\n" +
                "    Port " + String.valueOf(PORT_NUM) + "\n" +
                "    IdentityFile " + myPassphraselessKey.getCanonicalPath());
        myInternalProperties.put(SSHSessionProvider.TEAMCITY_DEPLOYER_SSH_CONFIG_PATH, tempConfig.getCanonicalPath());
        assertSessionIsConnected();
    }

    public void testDefaultConfigMatching() throws Exception {
        myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_DEFAULT_KEY);
        final File tempConfig = myTempFiles.createTempFile(
                        "Host foo\n" +
                        "    Hostname 127.0.0.1\n" +
                        "    Port " + String.valueOf(PORT_NUM) + "\n" +
                        "    IdentityFile " + myPassphraselessKey.getCanonicalPath() + "\n" +
                        "Host *\n" +
                        "    Port " + String.valueOf(22) + "\n" +
                        "    IdentityFile " + myPrivateKey.getCanonicalPath()
        );
        myRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, "foo");
        myInternalProperties.put(SSHSessionProvider.TEAMCITY_DEPLOYER_SSH_CONFIG_PATH, tempConfig.getCanonicalPath());
        assertSessionIsConnected();
    }

    public void testDefaultConfigMissing() throws Exception {
        myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_DEFAULT_KEY);

        myInternalProperties.put(SSHSessionProvider.TEAMCITY_DEPLOYER_SSH_CONFIG_PATH, "some/not/existing/path");
        myInternalProperties.put(SSHSessionProvider.TEAMCITY_DEPLOYER_SSH_DEFAULT_KEY, myPassphraselessKey.getCanonicalPath());
        assertSessionIsConnected();
    }

    private void assertSessionIsConnected() throws JSchException {
        Session session = null;
        try {
            final SSHSessionProvider provider = new SSHSessionProvider(myContext, myInternalPropertiesHolder);
            session = provider.getSession();
            assertTrue(session.isConnected());
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
