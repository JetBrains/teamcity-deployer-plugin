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

package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.BuildMessage1;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;

public class SSHExecProcessAdapterTest {

  private static final String DEFAULT_COMMAND = "echo hello";

  private Mockery myContext;
  private SSHSessionProvider mySessionProvider;
  private Session mySession;
  private ChannelExec myChannel;
  private SSHExecProcessAdapter myAdapter;
  private BuildProgressLogger myLogger;

  @BeforeMethod
  public void setup() {
    myContext = new Mockery();
    myContext.setImposteriser(ClassImposteriser.INSTANCE);

    mySessionProvider = myContext.mock(SSHSessionProvider.class);
    mySession = myContext.mock(Session.class);
    myChannel = myContext.mock(ChannelExec.class);
    myLogger = myContext.mock(BuildProgressLogger.class);
    myAdapter = newAdapter(myLogger);
    commonExpectations();
  }

  @Test
  public void stdoutAndStderrShouldBeLogged() throws Exception {
    myContext.checking(new Expectations() {{
      allowing(myChannel).connect(with(any(Integer.class)));

      oneOf(myChannel).getInputStream();
      will(returnValue(new ByteArrayInputStream("standard output\n".getBytes())));

      oneOf(myChannel).getErrStream();
      will(returnValue(new ByteArrayInputStream("standard error\n".getBytes())));

      oneOf(myChannel).isConnected();
      will(returnValue(true));
      oneOf(myChannel).isEOF();
      will(returnValue(false));

      oneOf(myLogger).message("Executing commands:\n" + DEFAULT_COMMAND + "\non host []");
      oneOf(myLogger).message("standard output");
      oneOf(myLogger).message("standard error");
      oneOf(myLogger).message("SSH exit-code [0]");
    }});

    myAdapter.runProcess();

    myContext.assertIsSatisfied();
  }

  @Test
  public void stderrShouldBeLoggedIfStdOutIsEmpty() throws Exception {
    myContext.checking(new Expectations() {{

      allowing(myChannel).connect(with(any(Integer.class)));

      oneOf(myChannel).getInputStream();
      will(returnValue(new ByteArrayInputStream(new byte[]{11}) {
        @Override
        public synchronized int read(byte[] b, int off, int len) {
          return -1;
        }
      }));

      oneOf(myChannel).getErrStream();
      will(returnValue(new ByteArrayInputStream("standard error\n".getBytes())));

      oneOf(myChannel).isConnected();
      will(returnValue(true));
      oneOf(myChannel).isEOF();
      will(returnValue(false));

      oneOf(myLogger).message("Executing commands:\n" + DEFAULT_COMMAND + "\non host []");
      oneOf(myLogger).message("standard error");
      oneOf(myLogger).message("SSH exit-code [0]");
    }});

    myAdapter.runProcess();

    myContext.assertIsSatisfied();
  }

  private void commonExpectations() {
    myContext.checking(new Expectations() {{
      try {
        allowing(mySessionProvider).getSession();
        will(returnValue(mySession));

        allowing(mySession).getHost();
        allowing(mySession).openChannel("exec");
        will(returnValue(myChannel));
        allowing(mySession).disconnect();

        allowing(myChannel).setCommand(DEFAULT_COMMAND);
        allowing(myChannel).connect();
        allowing(myChannel).isClosed();
        will(returnValue(true));
        allowing(myChannel).disconnect();
        allowing(myChannel).getExitStatus();
        allowing(myLogger).logMessage(with(any(BuildMessage1.class)));
      } catch (JSchException e) {
        Assert.fail("Unexpected exception in jmock expectations list.", e);
      }
    }});

  }

  private SSHExecProcessAdapter newAdapter(BuildProgressLogger logger) {
    SSHProcessAdapterOptions options = new SSHProcessAdapterOptions(true, false);
    return new SSHExecProcessAdapter(mySessionProvider, DEFAULT_COMMAND, null, logger, options);
  }
}
