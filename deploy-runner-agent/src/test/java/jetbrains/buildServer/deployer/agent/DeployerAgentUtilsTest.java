package jetbrains.buildServer.deployer.agent;

import org.testng.annotations.Test;

import static jetbrains.buildServer.deployer.agent.DeployerAgentUtils.checkHasNoControlCharacters;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the control-character guard that stops a crafted VCS file or directory name from injecting
 * extra commands into the line-oriented FTP and SCP protocols (TW-104639).
 */
@Test
public class DeployerAgentUtilsTest extends BaseDeployerTest {

  public void accepts_ordinary_names() {
    assertThatCode(() -> {
      checkHasNoControlCharacters("index.html", "file name");
      checkHasNoControlCharacters("a/b/c", "target path");
      checkHasNoControlCharacters("name with spaces.txt", "file name");
      checkHasNoControlCharacters("café-déjà.txt", "file name");
      checkHasNoControlCharacters("", "target path");
    }).doesNotThrowAnyException();
  }

  public void rejects_line_feed() {
    assertThatThrownBy(() -> checkHasNoControlCharacters("authorized_keys\nMKD stolen", "file name"))
        .isInstanceOf(UnsafeRemoteNameException.class);
  }

  public void rejects_carriage_return() {
    assertThatThrownBy(() -> checkHasNoControlCharacters("authorized_keys\rCWD ..", "file name"))
        .isInstanceOf(UnsafeRemoteNameException.class);
  }

  public void rejects_crlf_sequence() {
    assertThatThrownBy(() -> checkHasNoControlCharacters("a\r\nDELE secret\r\nNOOP", "file name"))
        .isInstanceOf(UnsafeRemoteNameException.class);
  }

  public void rejects_other_control_characters() {
    assertThatThrownBy(() -> checkHasNoControlCharacters("a\u0000b", "file name"))
        .isInstanceOf(UnsafeRemoteNameException.class);
    assertThatThrownBy(() -> checkHasNoControlCharacters("a\tb", "file name"))
        .isInstanceOf(UnsafeRemoteNameException.class);
    assertThatThrownBy(() -> checkHasNoControlCharacters("a\u007fb", "file name"))
        .isInstanceOf(UnsafeRemoteNameException.class);
  }

  public void message_names_the_description_and_never_the_raw_name() {
    assertThatThrownBy(() -> checkHasNoControlCharacters("secret\r\nMKD stolen", "file name"))
        .isInstanceOf(UnsafeRemoteNameException.class)
        .hasMessageContaining("file name")
        .matches(e -> !e.getMessage().contains("stolen"), "message must not echo the rejected name")
        .matches(e -> e.getMessage().indexOf('\n') < 0 && e.getMessage().indexOf('\r') < 0,
                 "message must not contain raw CR or LF");
  }
}
