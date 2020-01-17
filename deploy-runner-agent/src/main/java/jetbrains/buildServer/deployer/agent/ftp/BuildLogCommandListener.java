/*
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

package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
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
    logInternalMessage(sb.toString());
  }

  public void protocolReplyReceived(ProtocolCommandEvent event) {
    logInternalMessage("< " + event.getMessage());
  }

  private void logInternalMessage(@NotNull final String msg) {
    myLogger.logMessage(DefaultMessagesInfo.internalize(DefaultMessagesInfo.createTextMessage(msg)));
  }
}
