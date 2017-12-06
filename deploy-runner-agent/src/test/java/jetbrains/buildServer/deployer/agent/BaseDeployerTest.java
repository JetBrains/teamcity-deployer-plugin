package jetbrains.buildServer.deployer.agent;

import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.TestLogger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.File;

/**
 * Created by Nikita.Skvortsov
 * date: 08.07.2014.
 */
public class BaseDeployerTest {

  protected static final int DEPLOYER_DEFAULT_PORT = 55369;

  private TestLogger myLogger = new TestLogger();
  protected TempFiles myTempFiles = new TempFiles();


  @BeforeClass
  public void setUpClass() {
    myLogger.onSuiteStart();
  }

  @BeforeMethod
  public void setUp() throws Exception {
    myLogger.onTestStart();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    myTempFiles.cleanup();
    myLogger.onTestFinish(false);
  }

  protected File getTestResource(String fileName) {
    final String pathInBuildAgentModule = "src/test/resources/" + fileName;
    File file = new File(pathInBuildAgentModule);
    if (!file.exists()) {
      file = new File("deploy-runner-agent/" + pathInBuildAgentModule);
    }
    return file;
  }
}
