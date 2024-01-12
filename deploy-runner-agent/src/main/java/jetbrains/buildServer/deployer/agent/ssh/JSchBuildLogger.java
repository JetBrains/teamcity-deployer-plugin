

package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.Logger;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.BuildMessage1;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.messages.DefaultMessagesInfo.createTextMessage;
import static jetbrains.buildServer.messages.DefaultMessagesInfo.internalize;

/**
 * Created by Nikita.Skvortsov
 * date: 23.06.2017.
 */
public class JSchBuildLogger implements Logger {

  private final BuildProgressLogger myBuildLogger;

  public JSchBuildLogger(@NotNull BuildProgressLogger buildLogger) {
    myBuildLogger = buildLogger;
  }

  @Override
  public boolean isEnabled(int level) {
    return true;
  }

  @Override
  public void log(int level, String message) {
    BuildMessage1 buildMessage = createTextMessage(message);
    if (level < WARN) {
      buildMessage = internalize(buildMessage);
    }
    myBuildLogger.logMessage(buildMessage);
  }
}