package jetbrains.buildServer.deployer.agent.ssh.scp;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

public class ScpOperationBuilderTest {
    @DataProvider(name = "paths")
    public Object[][] getPaths() {
        Object[][] paths = {
                new Object[] { "/E://", Arrays.asList("E:") },
                new Object[] { "/a/b/c/d", Arrays.asList("a","b","c","d") },
                new Object[] { "/a/b/", Arrays.asList("a","b") },
                new Object[] { "E:/a/b", Arrays.asList("E:","a","b") },
        };
        return paths;
    }

    @Test(dataProvider = "paths")
    public void testWinRemotePath(String destination, List<String> expected) throws IOException {
        List<String> result = new ArrayList<>();
        ScpOperation operation = ScpOperationBuilder.doCreatePathOperation(destination, null);
        DirScpOperation currentOperation = (DirScpOperation) operation;
        while (currentOperation != null) {
            if (currentOperation instanceof DirScpOperation) {
                result.add(currentOperation.getName());
                List<ScpOperation> ops = currentOperation.getChildOperations();
                if (!ops.isEmpty() && ops.get(0) instanceof DirScpOperation)
                    currentOperation = (DirScpOperation) ops.get(0);
                else
                    currentOperation = null;
            }
        }
        Assert.assertThat(result, Matchers.is(expected));
    }
}