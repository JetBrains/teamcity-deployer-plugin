package my.buildServer.deployer.agent.ssh;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import my.buildServer.deployer.agent.util.DeployTestUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.filesystem.NativeFileSystemFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 3:13 PM
 */
public abstract class BaseSSHTransferTest {

    private static final int PORT_NUM = 22;

    protected String myUsername = "testuser";
    protected final String myPassword = "testpassword";
    protected List<ArtifactsCollection> myArtifactsCollections;
    protected BuildRunnerContext myContext;

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
        myServer.setKeyPairProvider(new FileKeyPairProvider(new String[] { keyFile.getCanonicalPath() }));
        myServer.setFileSystemFactory(new NativeFileSystemFactory());
        myServer.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));

        myServer.start();

        Mockery mockeryCtx = new Mockery();
        myContext = mockeryCtx.mock(BuildRunnerContext.class);
        final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
        final BuildProgressLogger logger = new NullBuildProgressLogger();
        final File workingDir = myTempFiles.createTempDir();

        mockeryCtx.checking(new Expectations() {{
            allowing(myContext).getWorkingDirectory(); will(returnValue(workingDir));
            allowing(myContext).getBuild(); will(returnValue(build));
            allowing(build).getBuildLogger(); will(returnValue(logger));
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
        final BuildProcess process = getProcess("127.0.0.1");
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    }

    @Test
    public void testTransferToRelativePath() throws Exception {
        final String subPath = "test_path/subdir";
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest2"));
        final BuildProcess process = getProcess("127.0.0.1:" + subPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
    }

    @Test
    public void testTransferAbsoluteBasePath() throws Exception {
        final File absDestination = new File(myTempFiles.createTempDir(), "sub/path");
        final String absPath = absDestination.getCanonicalPath();
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest2"));
        final BuildProcess process = getProcess("127.0.0.1:" + absPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(absDestination, myArtifactsCollections);
    }

    @Test
        public void testTransferAbsoluteCollectionPath() throws Exception {
        final String subPath = "test_path/subdir";
        final File tempDir1 = myTempFiles.createTempDir();
        final File tempDir2 = myTempFiles.createTempDir();
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, new File(tempDir1, "dest1").getCanonicalPath(), new File(tempDir1, "dest2").getCanonicalPath(), new File(tempDir2, "dest3").getCanonicalPath()));
        final BuildProcess process = getProcess("127.0.0.1:" + subPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    }

    protected abstract BuildProcess getProcess(String targetBasePath);
}
