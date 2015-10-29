package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 3:13 PM
 */
public abstract class BaseSSHTransferTest extends BaseSSHTest {

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
