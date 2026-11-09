package jetbrains.buildServer.deployer.agent;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a remote file or directory name carries a control character that could inject extra
 * commands into the line-oriented FTP or SCP protocol. It is a dedicated checked type so the deployment
 * fails explicitly and the signal is never swallowed by generic I/O error handling.
 */
public class UnsafeRemoteNameException extends Exception {
  public UnsafeRemoteNameException(@NotNull final String message) {
    super(message);
  }
}
