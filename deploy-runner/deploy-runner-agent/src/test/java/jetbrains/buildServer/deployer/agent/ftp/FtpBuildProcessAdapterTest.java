package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;
import jetbrains.buildServer.log.Loggers;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 4:22 PM
 */
public class FtpBuildProcessAdapterTest {

    private static final int TEST_PORT = 55369;

    private FtpServer myServer;
    private TempFiles myTempFiles;
    private File myRemoteDir;

    private final String myUsername = "myUsername";
    private final String myPassword = "myPassword";
    private List<ArtifactsCollection> myArtifactsCollections;
    private BuildRunnerContext myContext;

    @BeforeMethod
    public void setUp() throws Exception {

        myArtifactsCollections = new ArrayList<ArtifactsCollection>();

        myTempFiles = new TempFiles();
        myRemoteDir = myTempFiles.createTempDir();

        final FtpServerFactory serverFactory = new FtpServerFactory();
        final ListenerFactory factory = new ListenerFactory();
        factory.setPort(TEST_PORT);
        serverFactory.addListener("default", factory.createListener());

        myServer = serverFactory.createServer();

        final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setFile(myTempFiles.createTempFile());
        userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor());

        // init user manager
        final UserManager userManager = userManagerFactory.createUserManager();
        serverFactory.setUserManager(userManager);

        // add user
        final BaseUser user = new BaseUser();
        user.setName(myUsername);
        user.setPassword(myPassword);
        user.setHomeDirectory(myRemoteDir.getCanonicalPath());

        // give write permissions
        final List<Authority> authorities = new ArrayList<Authority>();
        final Authority auth = new WritePermission();
        authorities.add(auth);
        user.setAuthorities(authorities);

        userManager.save(user);

        // start the server
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
    }

    @AfterMethod
    public void tearDown() throws Exception {
        myServer.stop();
        myTempFiles.cleanup();
    }

    @Test
    public void testSimpleTransfer() throws Exception {
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest2"));
        final BuildProcess process = getProcess("127.0.0.1:" + TEST_PORT);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    }

    @Test
    public void testTransferToRelativePath() throws Exception {
        final String subPath = "test_path/subdir";
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest2"));
        final BuildProcess process = getProcess("127.0.0.1:" + TEST_PORT + "/" + subPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
    }


    @Test
    public void testTransferToRelativeSubPath() throws Exception {
        final String subPath = "test_path/subdir";
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1/subdest1", "dest2/subdest2"));
        final BuildProcess process = getProcess("127.0.0.1:" + TEST_PORT + "/" + subPath);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
    }

    @Test
    public void testTransferToEmptyPath() throws Exception {
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, ""));
        final BuildProcess process = getProcess("127.0.0.1:" + TEST_PORT);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    }

    @Test
    public void testTransferToExistingPath() throws Exception {
        final String uploadDestination = "some/path";
        final String artifactDestination = "dest1/sub";

        final File existingPath = new File(myRemoteDir, uploadDestination);
        assertTrue(existingPath.mkdirs());
        final File existingDestination = new File(existingPath, artifactDestination);
        assertTrue(existingDestination.mkdirs());

        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, artifactDestination, "dest2"));
        final BuildProcess process = getProcess("127.0.0.1:" + TEST_PORT + "/" + uploadDestination);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(existingPath, myArtifactsCollections);
    }

    @Test
    public void testTransferToExistingPath2() throws Exception {
        final String subPath = "test_path/subdir";
        myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(myTempFiles, "dest1", "dest1"));
        final BuildProcess process = getProcess("127.0.0.1:" + TEST_PORT);
        DeployTestUtils.runProcess(process, 5000);
        DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    }


    private BuildProcess getProcess(String target) {
        return new FtpBuildProcessAdapter(myContext, target, myUsername, myPassword, myArtifactsCollections);
    }
}
