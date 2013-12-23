package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SSHExecProcessAdapterTest {

    private static final String DEFAULT_COMMAND = "echo hello";

    private Mockery context = new Mockery();
    private SSHSessionProvider sessionProvider;
    private Session session;
    private ChannelExec channel;
    private InputStream stdout;
    private InputStream stderr;

    @BeforeClass
    public void setup() {
        this.context.setImposteriser(ClassImposteriser.INSTANCE);

        this.sessionProvider = context.mock(SSHSessionProvider.class);
        this.session = context.mock(Session.class);
        this.channel = context.mock(ChannelExec.class);
        this.stdout = context.mock(InputStream.class, "stdout");
        this.stderr = context.mock(InputStream.class, "stderr");
    }

    @Test
    public void stdoutAndStderrShouldBeLogged() throws Exception {
        final BuildProgressLogger mockedLogger = context.mock(BuildProgressLogger.class);
        SSHExecProcessAdapter adapter = newAdapter(mockedLogger);

        commonExpectations();
        context.checking(new Expectations() {{
            oneOf(channel).getInputStream();
            will(returnValue(new ByteArrayInputStream("standard output\n".getBytes())));

            oneOf(channel).getErrStream();
            will(returnValue(new ByteArrayInputStream("standard error\n".getBytes())));

            oneOf(mockedLogger).message("Executing commands:\n" + DEFAULT_COMMAND + "\non host []");
            oneOf(mockedLogger).message("Exec output:\nstandard output\nstandard error\n");
            oneOf(mockedLogger).message("ssh exit-code: 0");
        }});

        adapter.runProcess();

        context.assertIsSatisfied();
    }

    @Test
    public void stderrShouldBeLoggedIfStdOutIsEmpty() throws Exception {
        final BuildProgressLogger mockedLogger = context.mock(BuildProgressLogger.class);
        SSHExecProcessAdapter adapter = newAdapter(mockedLogger);

        commonExpectations();
        context.checking(new Expectations() {{
            oneOf(channel).getInputStream();
            will(returnValue(new ByteArrayInputStream(new byte[]{11}) {
                @Override
                public synchronized int read(byte[] b, int off, int len) {
                    return -1;
                }
            }));

            oneOf(channel).getErrStream();
            will(returnValue(new ByteArrayInputStream("standard error\n".getBytes())));

            oneOf(mockedLogger).message("Executing commands:\n" + DEFAULT_COMMAND + "\non host []");
            oneOf(mockedLogger).message("Exec output:\nstandard error\n");
            oneOf(mockedLogger).message("ssh exit-code: 0");
        }});

        adapter.runProcess();

        context.assertIsSatisfied();
    }

    private void commonExpectations() throws Exception {
        context.checking(new Expectations() {{
            allowing(sessionProvider).getSession();
            will(returnValue(session));

            allowing(session).getHost();
            allowing(session).openChannel("exec");
            will(returnValue(channel));
            allowing(session).disconnect();

            allowing(channel).setCommand(DEFAULT_COMMAND);
            allowing(channel).connect();
            allowing(channel).isClosed();
            will(returnValue(true));
            allowing(channel).disconnect();
            allowing(channel).getExitStatus();
        }});
    }

    private SSHExecProcessAdapter newAdapter(BuildProgressLogger logger) throws Exception {
        return new SSHExecProcessAdapter(sessionProvider, DEFAULT_COMMAND, null, logger);
    }
}
