package jetbrains.buildServer.deployer.agent.ssh.scp;

import java.io.IOException;
import java.io.InputStream;

public class ScpExecUtil {

    public static int checkScpAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            final StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            throw new IOException(sb.toString());
        }
        return b;
    }
}