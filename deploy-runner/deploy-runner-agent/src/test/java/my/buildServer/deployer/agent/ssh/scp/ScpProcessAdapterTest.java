package my.buildServer.deployer.agent.ssh.scp;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.WaitFor;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.filesystem.NativeFileSystemFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ScpProcessAdapterTest {

    private static final int PORT_NUM = 22;
    final String myUsername = "testuser";
    final String myPassword = "testpassword";

    private TempFiles myTempFiles;
    private File myWorkingDir = null;
    private File myRemoteDir = null;

    private String oldUserDir = null;
    private SshServer myServer;
    private BuildRunnerContext myContext;
    private List<ArtifactsCollection> myArtifactsCollections;


    @BeforeMethod
    public void setUp() throws Exception {
        myTempFiles = new TempFiles();

        myWorkingDir = myTempFiles.createTempDir();
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

        // myServer.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));

        myServer.start();

        Mockery mockeryCtx = new Mockery();
        myContext = mockeryCtx.mock(BuildRunnerContext.class);
        mockeryCtx.checking(new Expectations() {{
            allowing(myContext).getWorkingDirectory(); will(returnValue(myWorkingDir));
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

    @Test(timeOut = 5000)
    public void testSimpleTransfer() throws Exception {
        myArtifactsCollections.add(buildArtifactsCollection("dest1", "dest2"));
        final BuildProcess process = new ScpProcessAdapter(myContext, myUsername, myPassword, "127.0.0.1", myArtifactsCollections);
        runProcess(process, 5000);
        assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    }

    @Test
    public void testTransferToRelativePath() throws Exception {
        final String subPath = "test_path/subdir";
        myArtifactsCollections.add(buildArtifactsCollection("dest1", "dest2"));
        final BuildProcess process = new ScpProcessAdapter(myContext, myUsername, myPassword, "127.0.0.1:"+subPath, myArtifactsCollections);
        runProcess(process, 5000);
        assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
    }



    @Test
    public void testTransferAbsPath() throws Exception {
        final File absDestination = new File(myTempFiles.createTempDir(), "sub/path");
        final String absPath = absDestination.getCanonicalPath();
        myArtifactsCollections.add(buildArtifactsCollection("dest1", "dest2"));
        final BuildProcess process = new ScpProcessAdapter(myContext, myUsername, myPassword, "127.0.0.1:"+absPath, myArtifactsCollections);
        runProcess(process, 5000);
        assertCollectionsTransferred(absDestination, myArtifactsCollections);
    }
/*
    @Test
    public void testTransferToAbsPath_NotAllowed() throws Exception {
        final File f = new File(myWorkingDir, "file.txt");
        FileUtil.writeFile(f, "Some sample text\n");
        String remotePath = "/dir/sub/sub";
        ScpProcessAdapter scpProcess = createScpProcAdapter(f.getName(), remotePath);
        try {
            scpProcess.start();
            Assert.fail("Should have thrown an exception");
        } catch (RunBuildException e) {
            Assert.assertEquals("mkdir: cannot create directory `/dir': Permission denied\n", e.getCause().getMessage());
        }
    }

    @Test
    public void testMultiTransfer() throws Exception {
        final File dir = new File(myWorkingDir, "dir");
        final File sub1 = new File(dir, "sub1");
        final File sub2 = new File(sub1, "sub2");
        Assert.assertTrue(sub2.mkdirs());

        final File f1 = new File(sub1, "file1.txt");
        FileUtil.writeFile(f1, "Some sample text #1\n");

        final File f2 = new File(sub2, "file2.txt");
        FileUtil.writeFile(f2, "Some sample text #2\n");

        ScpProcessAdapter scpProcess = createScpProcAdapter("dir/sub1", "sub0");
        scpProcess.start();
    }

    private ScpProcessAdapter createScpProcAdapter(String srcPath, String remotePath) {
        return null;
    }
    */

    private void runProcess(final BuildProcess process, final int timeout) throws RunBuildException {
        process.start();
        new WaitFor(timeout) {
            @Override
            protected boolean condition() {
                return process.isFinished();
            }
        };
        assertTrue(process.isFinished(), "Failed to finish test in time");
    }

    private ArtifactsCollection buildArtifactsCollection(String... destinationDirs) throws IOException {

        final Map<File, String> filePathMap = new HashMap<File, String>();
        for (String destinationDir : destinationDirs) {
            final File content = myTempFiles.createTempFile(100);
            filePathMap.put(content, destinationDir);
        }
        return new ArtifactsCollection("dirFrom/**", "dirTo", filePathMap);
    }

    private void assertCollectionsTransferred(File remoteBase, List<ArtifactsCollection> artifactsCollections) throws IOException {

        for (ArtifactsCollection artifactsCollection : artifactsCollections) {
            for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                final File source = fileStringEntry.getKey();
                final String targetPath = fileStringEntry.getValue() + File.separator + source.getName();
                final File target = new File(remoteBase, targetPath);
                assertTrue(target.exists(), "Destination file [" + targetPath + "] does not exist");
                assertEquals(FileUtil.readText(target), FileUtil.readText(source), "wrong content");
            }
        }
    }

}
