package my.buildServer.deployer.agent.scp;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
* Created by Kit
* Date: 21.04.12 - 21:59
*/
public interface ScpOperation {
    void execute(@NotNull final OutputStream out,
                 @NotNull final InputStream in) throws IOException;
}
