package jetbrains.buildServer.deployer.agent;

import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.TestLogger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Created by Nikita.Skvortsov
 * date: 08.07.2014.
 */
public class BaseDeployerTest {

    protected TestLogger myLogger = new TestLogger();
    protected TempFiles myTempFiles = new TempFiles();


    @BeforeClass
    public void setUpClass() throws Exception {
        myLogger.onSuiteStart();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        myLogger.onTestStart();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        myTempFiles.cleanup();
        myLogger.onTestFinish(true);
    }
}
