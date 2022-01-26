/*
 * Copyright 2000-2022 JetBrains s.r.o.
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