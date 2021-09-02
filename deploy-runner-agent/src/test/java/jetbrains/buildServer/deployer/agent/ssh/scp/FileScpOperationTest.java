package jetbrains.buildServer.deployer.agent.ssh.scp;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.attribute.PosixFilePermission.*;

public class FileScpOperationTest {
    @DataProvider(name = "permissions")
    public Object[][] getPermissions() {
        Object[][] paths = {
                new Object[] { Collections.EMPTY_SET, "000" },
                new Object[] { Collections.singleton(OWNER_READ), "400" },
                new Object[] { Collections.singleton(OWNER_WRITE), "200" },
                new Object[] { Collections.singleton(OWNER_EXECUTE), "100" },
                new Object[] { Collections.singleton(GROUP_READ), "040" },
                new Object[] { Collections.singleton(GROUP_WRITE), "020" },
                new Object[] { Collections.singleton(GROUP_EXECUTE), "010" },
                new Object[] { Collections.singleton(OTHERS_READ), "004" },
                new Object[] { Collections.singleton(OTHERS_WRITE), "002" },
                new Object[] { Collections.singleton(OTHERS_EXECUTE), "001" },
                new Object[] { Stream.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ).collect(Collectors.toSet()), "644" },
                new Object[] { Stream.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE).collect(Collectors.toSet()), "755" }
        };
        return paths;
    }

    @Test(dataProvider = "permissions")
    public void test_if_file_permissions_saved(Set<PosixFilePermission> permissions, String expected) throws IOException {
            File tmpFile = File.createTempFile("someprefix", "");
            PosixFileAttributeView view = Files.getFileAttributeView(tmpFile.toPath(), PosixFileAttributeView.class);
            if (view != null) { // only for posix-compatible file systems
                view.setPermissions(permissions);
                String actual = new FileScpOperation(tmpFile).getPermission();
                Assert.assertThat(actual, Matchers.is(expected));
            }
            Files.delete(tmpFile.toPath());
    }
}
