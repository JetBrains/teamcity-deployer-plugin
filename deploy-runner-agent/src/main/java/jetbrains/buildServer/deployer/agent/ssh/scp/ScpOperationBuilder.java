/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package jetbrains.buildServer.deployer.agent.ssh.scp;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class ScpOperationBuilder {

  /**
   * Build chain of scp operations to copy source file to destination path
   *
   * @param sourceFile      source file to copy. Must be a file, not a directory
   * @param destinationPath relative path to destination
   * @return top of resulting operations chain
   */
  public static ScpOperation getCopyFileOperation(@NotNull final File sourceFile,
                                                  @NotNull final String destinationPath) throws IOException {

    if (!sourceFile.exists()) {
      throw new IOException("Source [" + sourceFile.getAbsolutePath() + "] does not exists");
    }

    if (sourceFile.isDirectory()) {
      throw new IOException("Source [" + sourceFile.getAbsolutePath() + "] is a directory, but a file is expected");
    }

    ScpOperation fileOperation = new FileScpOperation(sourceFile);

    return doCreatePathOperation(destinationPath, fileOperation);
  }


  /**
   * Build chain of scp opertaions to create empty directory.
   *
   * @param remotePath path to create
   * @return top of resulting operations chain
   */

  public static ScpOperation getCreatePathOperation(@NotNull final String remotePath) {
    return doCreatePathOperation(remotePath, null);
  }


  private static ScpOperation doCreatePathOperation(@NotNull final String remotePath,
                                                    @Nullable final ScpOperation chainTailOperation) {
    final String normalisedPath = remotePath.replaceAll("\\\\", "/");
    File remoteDir = new File(normalisedPath);
    DirScpOperation childOperation = new DirScpOperation(remoteDir.getName());
    if (null != chainTailOperation) {
      childOperation.add(chainTailOperation);
    }
    remoteDir = remoteDir.getParentFile();
    String name = remoteDir != null ? remoteDir.getName() : "";
    while (remoteDir != null && !StringUtil.isEmpty(name)) {

      final DirScpOperation directoryOperation = new DirScpOperation(name);
      directoryOperation.add(childOperation);

      childOperation = directoryOperation;
      remoteDir = remoteDir.getParentFile();
      if (remoteDir != null) {
        name = remoteDir.getName();
      }
    }
    return childOperation;
  }
}
