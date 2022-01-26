/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.deployer.agent.util;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.WaitFor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 5:51 PM
 */
public class DeployTestUtils {
  public static void runProcess(final BuildProcess process, final int timeout) throws RunBuildException {
    process.start();
    new WaitFor(timeout) {
      @Override
      protected boolean condition() {
        return process.isFinished();
      }
    };
    assertThat(process.isFinished()).describedAs("Failed to finish test in time").isTrue();
    assertThat(process.waitFor()).isEqualTo(BuildFinishedStatus.FINISHED_SUCCESS);
  }

  public static ArtifactsCollection buildArtifactsCollection(final TempFilesFactory tempFiles, String... destinationDirs) throws IOException {

    final Map<File, String> filePathMap = new HashMap<File, String>();
    String dirTo = "dirTo";
    for (String destinationDir : destinationDirs) {
      dirTo = destinationDir;
      final File content = tempFiles.createTempFile(100);
      filePathMap.put(content, destinationDir);
    }
    return new ArtifactsCollection("dirFrom/**", dirTo, filePathMap);
  }

  public static void assertCollectionsTransferred(File remoteBase, List<ArtifactsCollection> artifactsCollections) throws IOException {

    for (ArtifactsCollection artifactsCollection : artifactsCollections) {
      for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
        final File source = fileStringEntry.getKey();
        final String relativePath = fileStringEntry.getValue();
        final String targetPath = relativePath + (StringUtil.isNotEmpty(relativePath) ? File.separator : "") + source.getName();
        final File target;
        if (new File(targetPath).isAbsolute()) {
          target = new File(targetPath);
        } else {
          target = new File(remoteBase, targetPath);
        }
        assertTrue(target.exists(), "Destination file [" + targetPath + "] does not exist");
        assertEquals(FileUtil.readText(target), FileUtil.readText(source), "wrong content");
      }
    }
  }

  public interface TempFilesFactory {
    File createTempFile(int size) throws IOException;
  }
}
