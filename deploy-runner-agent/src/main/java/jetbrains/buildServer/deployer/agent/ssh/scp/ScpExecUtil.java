package jetbrains.buildServer.deployer.agent.ssh.scp;

import java.io.IOException;
import java.io.InputStream;

public class ScpExecUtil {

  public static int checkScpAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for warning,
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
      } while (c != '\n');
      throw new IOException("Remote system responded with error: " + sb.toString());
    } else {
      final int available = in.available();
      byte[] content = new byte[available + 1];
      content[0] = (byte) b;
      final int read = in.read(content, 1, available);
      final String message = new String(content, 0, read + 1, "UTF-8");
      throw new IOException("Unexpected response from remote system: " + message);
    }
  }
}