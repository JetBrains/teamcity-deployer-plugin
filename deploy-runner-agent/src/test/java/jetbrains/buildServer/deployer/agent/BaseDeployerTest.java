package jetbrains.buildServer.deployer.agent;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.TestLogger;
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
