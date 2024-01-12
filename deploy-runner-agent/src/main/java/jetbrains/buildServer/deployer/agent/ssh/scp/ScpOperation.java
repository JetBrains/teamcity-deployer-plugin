

package jetbrains.buildServer.deployer.agent.ssh.scp;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ScpOperation {
  void execute(@NotNull final OutputStream out,
               @NotNull final InputStream in) throws IOException;
}