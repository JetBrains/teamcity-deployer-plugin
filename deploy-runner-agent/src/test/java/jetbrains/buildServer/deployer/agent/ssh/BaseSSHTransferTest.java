/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.BaseTestCase;
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
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1", "dest2"));
    final BuildProcess process = getProcess(HOST_ADDR);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(myRemoteDir, myArtifactsCollections);
  }

  @Test
  public void testTransferToRelativePath() throws Exception {
    final String subPath = "test_path/subdir";
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1", "dest2"));
    final BuildProcess process = getProcess(HOST_ADDR + ":" + subPath);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
  }

  @Test
  public void testTransferToRelativeTargetAndEmptyPath() throws Exception {
    final String subPath = "test_path/subdir";
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), ""));
    final BuildProcess process = getProcess(HOST_ADDR + ":" + subPath);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(new File(myRemoteDir, subPath), myArtifactsCollections);
  }

  @Test
  public void testTransferAbsoluteBasePath() throws Exception {
    final File absDestination = new File(myRemoteDir, "sub/path");
    final String absPath = absDestination.getCanonicalPath();
    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), "dest1", "dest2"));
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

    myArtifactsCollections.add(DeployTestUtils.buildArtifactsCollection(createTempFilesFactory(), artifactDestination, "dest2"));
    final BuildProcess process = getProcess(HOST_ADDR + ":" + uploadDestination);
    DeployTestUtils.runProcess(process, 5000);
    DeployTestUtils.assertCollectionsTransferred(existingPath, myArtifactsCollections);
  }


  protected abstract BuildProcess getProcess(String targetBasePath) throws Exception;
}
