package my.buildServer.deployer.agent.ssh.scp;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.util.FileUtil;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Created by Kit
 * Date: 21.04.12 - 22:27
 */
public class ScpProcessAdapterTest {

    final String myUsername = "testuser";
    final String myPassword = "testpassword";
    final String myHost = "192.168.56.102";

    File myWorkingDir = null;

    @BeforeMethod
    public void setUp() throws Exception {
        myWorkingDir = FileUtil.createTempDirectory("test", "workingDir");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        FileUtil.delete(myWorkingDir);
    }

    @Test
    public void testSimpleTransfer() throws Exception {
        final File f = new File(myWorkingDir, "file.txt");
        FileUtil.writeFile(f, "Some sample text");
        String remotePath = "";
        ScpProcessAdapter scpProcess = createScpProcAdapter(f.getName(), remotePath);
        scpProcess.start();
    }

    @Test
    public void testTransferToRelativePath() throws Exception {
        final File f = new File(myWorkingDir, "file.txt");
        FileUtil.writeFile(f, "Some sample text\n");
        String remotePath = "subdir/sub/sub";
        ScpProcessAdapter scpProcess = createScpProcAdapter(f.getName(), remotePath);
        scpProcess.start();
    }

    @Test
    public void testTransferAbsPath() throws Exception {
        final File f = new File(myWorkingDir, "file.txt");
        FileUtil.writeFile(f, "Some sample text\n");
        String remotePath = "/home/testuser/subdir/sub/sub";
        ScpProcessAdapter scpProcess = createScpProcAdapter(f.getName(), remotePath);

        scpProcess.start();
    }

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
        return null; // new ScpProcessAdapter(srcPath, myHost + ":" + remotePath, myUsername, myPassword, myWorkingDir);
    }
}
