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

package jetbrains.buildServer.deployer.agent;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.deployer.agent.util.DeployTestUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Nikita.Skvortsov
 * date: 08.07.2014.
 */
public class BaseDeployerTest extends BaseTestCase {
  protected static final int DEPLOYER_DEFAULT_PORT = 55369;

  protected File getTestResource(String fileName) {
    final String pathInBuildAgentModule = "src/test/resources/" + fileName;
    File file = new File(pathInBuildAgentModule);
    if (!file.exists()) {
      file = new File("deploy-runner-agent/" + pathInBuildAgentModule);
    }
    return file;
  }

  protected DeployTestUtils.TempFilesFactory createTempFilesFactory() {
    return new DeployTestUtils.TempFilesFactory() {
      @Override
      public File createTempFile(int size) throws IOException {
        return BaseDeployerTest.this.createTempFile(size);
      }
    };
  }
}
