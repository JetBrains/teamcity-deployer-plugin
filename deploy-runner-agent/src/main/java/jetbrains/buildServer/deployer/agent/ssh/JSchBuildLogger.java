/*
 * Copyright 2000-2021 JetBrains s.r.o.
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
