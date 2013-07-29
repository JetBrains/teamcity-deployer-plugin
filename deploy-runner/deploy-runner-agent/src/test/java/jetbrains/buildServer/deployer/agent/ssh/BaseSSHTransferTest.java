package jetbrains.buildServer.deployer.agent.ssh;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.filesystem.NativeFileSystemFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.security.PublicKey;
import java.util.*;

import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 3:13 PM
 */
public abstract class BaseSSHTransferTest {

    protected static final int PORT_NUM = 15655;
    protected static final String HOST_ADDR = "127.0.0.1";

    protected File myWorkingDir;
    protected String myUsername = "testuser";
    protected String myPassword = "testpassword";
    protected List<ArtifactsCollection> myArtifactsCollections;
    protected BuildRunnerContext myContext;
    protected File myPrivateKey;
    protected final Map<String, String> myRunnerParams = new HashMap<String, String>();
    protected final InternalPropertiesHolder myInternalProperties = new InternalPropertiesHolder() {
        @Nullable
        @Override
        public String getInternalProperty(@NotNull String s, String s2) {
            return null;
        }
    };

    private TempFiles myTempFiles;
    private File myRemoteDir = null;
    private String oldUserDir = null;
    private SshServer myServer;


    @BeforeMethod
    public void setUp() throws Exception {
        myTempFiles = new TempFiles();


        myRemoteDir = myTempFiles.createTempDir();

        myServer = SshServer.setUpDefaultServer();
        myServer.setPort(PORT_NUM);
        myServer.setCommandFactory(new ScpCommandFactory());
        myServer.setShellFactory(new ProcessShellFactory(new String[] { SystemInfo.isWindows ? "cmd" : "sh" }));
        myServer.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                return myUsername.equals(username) && myPassword.equals(password);
            }
        });

        File keyFile = new File("src/test/resources/hostkey.pem");
        if (!keyFile.exists()) {
            keyFile = new File("deploy-runner-agent/src/test/resources/hostkey.pem");
        }

        File privateKey = new File("src/test/resources/tmp_rsa");
        if (!privateKey.exists()) {
            privateKey = new File("deploy-runner-agent/src/test/resources/tmp_rsa");
        }

        myPrivateKey = privateKey.getAbsoluteFile();

        myServer.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            @Override
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                return true;
            }
        });
        myServer.setKeyPairProvider(new FileKeyPairProvider(new String[] { keyFile.getCanonicalPath() }));
        myServer.setFileSystemFactory(new NativeFileSystemFactory());
        myServer.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));

        myServer.start();

        Mockery mockeryCtx = new Mockery();
        myContext = mockeryCtx.mock(BuildRunnerContext.class);
        final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
        final BuildProgressLogger logger = new NullBuildProgressLogger();
        myWorkingDir = myTempFiles.createTempDir();

        mockeryCtx.checking(new Expectations() {{
            allowing(myContext).getWorkingDirectory(); will(returnValue(myWorkingDir));
            allowing(myContext).getBuild(); will(returnValue(build));
            allowing(myContext).getRunnerParameters(); will(returnValue(myRunnerParams));
            allowing(build).getBuildLogger(); will(returnValue(logger));
            allowing(build).getCheckoutDirectory(); will(returnValue(myWorkingDir));
        }});

        // need to change user.dir, so that NativeFileSystemFactory works inside temp directory
        oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", myRemoteDir.getAbsolutePath());

        myArtifactsCollections = new ArrayList<ArtifactsCollection>();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        myTempFiles.cleanup();
        myServer.stop(true);
        System.setProperty("user.dir", oldUserDir);
    }

    @Test
    public void testSimpleTransfer() throws Exception {
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest2"));
        final BuildProcess process = getProcess(HOST_ADDR);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    }

    @Test
    public void testTransferToRelativePath() throws Exception {
        final String subPath = "test_path/subdir";
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest2"));
        final BuildProcess process = getProcess(HOST_ADDR + ":" + subPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
    }

    @Test
    public void testTransferToRelativeTargetAndEmptyPath() throws Exception {
        final String subPath = "test_path/subdir";
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, ""));
        final BuildProcess process = getProcess(HOST_ADDR + ":" + subPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
    }

    @Test
    public void testTransferAbsoluteBasePath() throws Exception {
        final File absDestination = new File(myTempFiles.createTempDir(), "sub/path");
        final String absPath = absDestination.getCanonicalPath();
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest2"));
        final BuildProcess process = getProcess(HOST_ADDR + ":" + absPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(absDestination, myArtifactsCollections);
    }


    @Test
    public void testTransferToExistingPath() throws Exception {
        final String uploadDestination = "some/path";
        final String artifactDestination = "dest1/sub";

        final File existingPath = new File(myRemoteDir, uploadDestination);
        assertTrue(existingPath.mkdirs() || existingPath.exists());
        final File existingDestination = new File(existingPath, artifactDestination);
        assertTrue(existingDestination.mkdirs() || existingDestination.exists());

        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, artifactDestination, "dest2"));
        final BuildProcess process = getProcess(HOST_ADDR + ":" + uploadDestination);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(existingPath, myArtifactsCollections);
    }


    protected abstract BuildProcess getProcess(String targetBasePath) throws Exception;
}
