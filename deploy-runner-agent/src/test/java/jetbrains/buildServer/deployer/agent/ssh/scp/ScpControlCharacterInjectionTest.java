package jetbrains.buildServer.deployer.agent.ssh.scp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import jetbrains.buildServer.deployer.agent.BaseDeployerTest;
import jetbrains.buildServer.deployer.agent.UnsafeRemoteNameException;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * A crafted VCS file or directory name carrying CR/LF must not reach the line-oriented SCP protocol,
 * where its tail would become extra authenticated commands on the control connection (TW-104639).
 */
@Test
public class ScpControlCharacterInjectionTest extends BaseDeployerTest {

  public void file_operation_rejects_crlf_name_and_writes_nothing() throws Exception {
    // Back the operation with a real file so the constructor's permission read (file.toPath()) works on
    // every OS; only the reported name carries the CR/LF, which Windows would reject as an actual path.
    final File realFile = createTempFile();
    final File malicious = new File(realFile.getPath()) {
      @Override
      public String getName() {
        return "authorized_keys\r\nMKD stolen";
      }
    };
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    thenThrownBy(() -> new FileScpOperation(malicious).execute(out, ackStream()))
        .isInstanceOf(UnsafeRemoteNameException.class);
    then(out.toByteArray()).as("nothing must be sent to the remote scp sink").isEmpty();
  }

  public void directory_operation_rejects_crlf_name_and_writes_nothing() {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    thenThrownBy(() -> new DirScpOperation("incoming\r\nRNFR incoming\r\nRNTO .ssh").execute(out, ackStream()))
        .isInstanceOf(UnsafeRemoteNameException.class);
    then(out.toByteArray()).as("nothing must be sent to the remote scp sink").isEmpty();
  }

  private static ByteArrayInputStream ackStream() {
    // a stream of success acks, in case execution were (wrongly) to proceed to read one
    return new ByteArrayInputStream(new byte[]{0, 0, 0, 0});
  }
}
