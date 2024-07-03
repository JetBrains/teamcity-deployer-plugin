package jetbrains.buildServer.deployer.agent;

/**

import com.google.common.io.ByteStreams;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.OpenSSHPrivateKeyFile;
import com.sshtools.common.ssh.components.SshKeyPair;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Scanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.assertj.core.api.Assertions.assertThat;
@Test
public class DecryptSSHKeyTest {

  @Test(dataProvider="listOfKeys")
  public void testDecryptRSAKey(String resourceKeyFile) throws IOException, InvalidPassphraseException {
    byte[] rsaKey = ByteStreams.toByteArray(getResourceAsStream(resourceKeyFile));
    OpenSSHPrivateKeyFile kf1 = new OpenSSHPrivateKeyFile(rsaKey);
    assertThat(kf1.isPassphraseProtected()).isTrue();
    kf1.changePassphrase("1234", "");
    SshKeyPair kp = kf1.toKeyPair("");
    String alg = kp.getPrivateKey().getAlgorithm();
    String unencryptedRsaKey = convertContentIntoString(readResourceIntoByteArray(resourceKeyFile), alg);
    String hexResult = convertContentIntoString(kf1.getFormattedKey(), alg);
    assertThat(hexResult).isEqualTo(unencryptedRsaKey);
  }

  @Nullable
  private InputStream getResourceAsStream(String resourceKeyFile) {
    return getClass().getClassLoader().getResourceAsStream(resourceKeyFile);
  }

  @NotNull
  private String convertContentIntoString(byte[] contents, String alg) throws IOException {
    String s =  encodeHexString(getPrivateKeyContent(new String(contents)));
    String[] split = s.split("(?<=\\G.{4})");
    switch (alg) {
      case "ssh-dss":
        setRandomTo(split, 240, 4);
        break;
      case "ecdsa-sha2-nistp256":
        setRandomTo(split, 75, 5);
        break;
      case "ssh-ed25519":
        setRandomTo(split, 49, 4);
        break;
      case "ssh-rsa":
        setRandomTo(split, 227, 4);
        break;
    }
    return String.join("\n", split);
  }

  private static void setRandomTo(String[] split, int beginIndex, int size) {
    assertThat(split.length).isGreaterThanOrEqualTo(beginIndex+size);
    for (int i = 0; i < size; i++) {
      split[beginIndex+i] = "<RANDOM>";
    }
  }

  private byte[] readResourceIntoByteArray(String resourceKeyFile) throws IOException {
    return ByteStreams.toByteArray(getResourceAsStream(resourceKeyFile + "_unencrypted"));
  }

  @DataProvider
  public static String[][] listOfKeys() {
    return new String[][]{
      { "keys/encrypted/id_dsa" },
      { "keys/encrypted/id_ecdsa" },
      { "keys/encrypted/id_ed25519" },
      { "keys/encrypted/id_rsa" }
    };
  }

  @NotNull
  private byte[] getPrivateKeyContent(String keyContent) {
    StringBuilder sb = new StringBuilder();
    Scanner s = new Scanner(keyContent);
    while (s.hasNextLine()) {
      String l = s.nextLine();
      if (l == null || l.isEmpty() || l.startsWith("-----")) {
        continue;
      }
      sb.append(l);
    }
    String privateKeyContent = sb.toString();
    return Base64.getDecoder().decode(privateKeyContent);
  }
}
*/
