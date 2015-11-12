package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.agent.BuildProgressLogger;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Nikita.Skvortsov
 * date: 12.11.2015.
 */
public class BuildLogCommandListener implements ProtocolCommandListener {
  private final BuildProgressLogger myLogger;

  public BuildLogCommandListener(@NotNull  BuildProgressLogger logger) {
    this.myLogger = logger;
  }

  public void protocolCommandSent(ProtocolCommandEvent event) {
    final StringBuilder sb = new StringBuilder("> ");
    String cmd = event.getCommand();
    if(!"PASS".equalsIgnoreCase(cmd) && !"USER".equalsIgnoreCase(cmd)) {
      if("LOGIN".equalsIgnoreCase(cmd)) {
        String msg = event.getMessage();
        msg = msg.substring(0, msg.indexOf("LOGIN") + "LOGIN".length());
        sb.append(msg).append(" *******");
      } else {
        sb.append(event.getMessage());
      }
    } else {
      sb.append(cmd).append(" *******");
    }
    myLogger.message(sb.toString());
  }

  public void protocolReplyReceived(ProtocolCommandEvent event) {
    myLogger.message("< " + event.getMessage());
  }
}
