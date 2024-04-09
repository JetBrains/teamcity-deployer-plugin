

package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.BaseDeployerTest;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DockerFtpBuildProcessAdapterTest extends BaseDeployerTest {

  private File myRemoteDir;
  private int testPort;

  private final String myUsername = "guest";
  private final String myPassword = "guest";
  private List<ArtifactsCollection> myArtifactsCollections;
  private BuildRunnerContext myContext;
  private final Map<String, String> myRunnerParameters = new HashMap<String, String>();
  private final Map<String, String> mySharedConfigParameters = new HashMap<String, String>();
  private List<String> myResultingLog;
  private FixedHostPortGenericContainer ftp = new FixedHostPortGenericContainer("loicmathieu/vsftpd")
          .withFixedExposedPort(2020, 20)
          .withFixedExposedPort(2021, 21)
          .withFixedExposedPort(21100, 21100)
          .withFixedExposedPort(21101, 21101)
          .withFixedExposedPort(21102, 21102)
          .withFixedExposedPort(21103, 21103)
          .withFixedExposedPort(21104, 21104)
          .withFixedExposedPort(21105, 21105)
          .withFixedExposedPort(21106, 21106)
          .withFixedExposedPort(21107, 21107)
          .withFixedExposedPort(21108, 21108)
          .withFixedExposedPort(21109, 21109)
          .withFixedExposedPort(21110, 21110)
          ;

  @BeforeClass
  public void setupClass() throws IOException {
    myRemoteDir = createTempDir();
    ftp.setCommand("ftps");
    ftp.withExposedPorts(21);
    try {
      ftp.start();
    } catch (IllegalStateException e) {
      throw new SkipException(e.getMessage());
    }
  }

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    myResultingLog = new LinkedList<>();
    myRunnerParameters.put(FTPRunnerConstants.PARAM_FTP_MODE, "PASSIVE");
    myArtifactsCollections = new ArrayList<>();
    testPort = 2021;

    Mockery mockeryCtx = new Mockery();
    myContext = mockeryCtx.mock(BuildRunnerContext.class);
    final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
    final BuildProgressLogger logger = new NullBuildProgressLogger() {
      @Override
      public void message(String message) {
        myResultingLog.add(message);
      }
    };
    final File workingDir = createTempDir();

    mockeryCtx.checking(new Expectations() {{
      allowing(myContext).getWorkingDirectory();
      will(returnValue(workingDir));
      allowing(myContext).getBuild();
      will(returnValue(build));
      allowing(myContext).getRunnerParameters();
      will(returnValue(myRunnerParameters));
      allowing(build).getBuildLogger();
      will(returnValue(logger));
      allowing(build).getSharedConfigParameters();
      will(returnValue(mySharedConfigParameters));
    }});
  }

  @AfterClass
  public void tearDownClass() {
    ftp.stop();
  }

  @AfterMethod
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void testSimpleTransfer() throws Exception {
    myRunnerParameters.put(FTPRunnerConstants.PARAM_SSL_MODE, "2");
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(),
        "dest1",
        "dest2",
        "dest3",
        "dest4",
        "dest5",
        "dest6",
        "dest7",
        "dest8",
        "dest9",
        "dest10",
        "dest11",
        "dest12",
        "dest13",
        "dest14",
        "dest15",
        "dest16",
        "dest17",
        "dest18",
        "dest19",
        "dest20",
        "dest21"));
    final BuildProcess process = getProcess("127.0.0.1:" + testPort);
    DeployTestUtils.runProcess(process, 50000);
    moveDirFromContainer(ftp, "/home/guest", myRemoteDir);
    DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
    assertTrue(myResultingLog.contains("< and continued >"));
  }

  private void moveDirFromContainer(GenericContainer container, String sourcePath, File targetDir) throws IOException, InterruptedException {
    String tmpArchiveName = "testResults.tgz";
    String tmpArchive = "/tmp/" + tmpArchiveName;
    container.execInContainer("tar",  "-czvf", tmpArchive, "-C", sourcePath, ".");
    container.copyFileFromContainer(tmpArchive, targetDir.getPath() + "/" + tmpArchiveName);
    File gzipArchive = new File(targetDir, tmpArchiveName);
    unTarFile(gzipArchive.getPath(), targetDir);
  }

  private static void unTarFile(String tarFile, File destFile) {
    TarArchiveInputStream tis = null;
    try {
      FileInputStream fis = new FileInputStream(tarFile);
      // .gz
      GZIPInputStream gzipInputStream = new GZIPInputStream(new BufferedInputStream(fis));
      //.tar.gz
      tis = new TarArchiveInputStream(gzipInputStream);
      TarArchiveEntry tarEntry = null;
      while ((tarEntry = tis.getNextTarEntry()) != null) {
        System.out.println(" tar entry- " + tarEntry.getName());
        if(tarEntry.isDirectory()){
          continue;
        }else {
          // In case entry is for file ensure parent directory is in place
          // and write file content to Output Stream
          File outputFile = new File(destFile, tarEntry.getName());
          outputFile.getParentFile().mkdirs();
          IOUtils.copy(tis, new FileOutputStream(outputFile));
        }
      }
    }catch(IOException ex) {
      System.out.println("Error while untarring a file- " + ex.getMessage());
    }finally {
      if(tis != null) {
        try {
          tis.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  private BuildProcess getProcess(String target) {
    return new FtpBuildProcessAdapter(myContext, target, myUsername, myPassword, myArtifactsCollections);
  }
}