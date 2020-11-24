package jetbrains.buildServer.deployer.agent.ssh.scp;/*
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