

package jetbrains.buildServer.deployer.agent.ssh.scp;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.Hash;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.attribute.PosixFilePermission.*;

/**
 * Created by Kit
 * Date: 21.04.12 - 22:15
 */
class FileScpOperation implements ScpOperation {
  private static final Logger logger = Logger.getInstance(FileScpOperation.class.getCanonicalName());
  private final static String ZEROES = "000";
  final File myFile;
  final int myPermission;

  public FileScpOperation(File file) {
    int permissions1;
    assert file.isFile();
    myFile = file;
    try {
      permissions1 = getPermissionsFromFile(file);
    } catch (IOException e) {
//      "rwxr-xr-x";
      permissions1 = 256|128|64|32|8|4|1;
      logger.warnAndDebugDetails("Error while trying to get permissions of " + file.getAbsolutePath() + " using 0755 instead.", e);
    }
    myPermission = permissions1;
  }

  int getPermissionsFromFile(File file) throws IOException {
    Set<PosixFilePermission> permission;
    try {
      permission = Files.getPosixFilePermissions(file.toPath(), LinkOption.NOFOLLOW_LINKS);
    } catch (UnsupportedOperationException e) {
      logger.warn("Filesystem doesn't support POSIX file attributes, rolling back to generics.");
      permission = new HashSet<>();
      if (Files.isExecutable(file.toPath())) {
        permission.add(OWNER_EXECUTE);
        permission.add(GROUP_EXECUTE);
        permission.add(OTHERS_EXECUTE);
      }
      if (Files.isReadable(file.toPath())) {
        permission.add(OWNER_READ);
        permission.add(GROUP_READ);
        permission.add(OTHERS_READ);
      }
      if (Files.isWritable(file.toPath())) {
        permission.add(OWNER_WRITE);
        permission.add(GROUP_WRITE);
        permission.add(OTHERS_WRITE);
      }
    }
    AtomicInteger permissionInt = new AtomicInteger(0);
    permission.forEach(p -> {
      switch (p) {
        case OWNER_READ:
          permissionInt.updateAndGet(i -> i | 256);
          break;
        case OWNER_WRITE:
          permissionInt.updateAndGet(i -> i | 128);
          break;
        case OWNER_EXECUTE:
          permissionInt.updateAndGet(i -> i | 64);
          break;
        case GROUP_READ:
          permissionInt.updateAndGet(i -> i | 32);
          break;
        case GROUP_WRITE:
          permissionInt.updateAndGet(i -> i | 16);
          break;
        case GROUP_EXECUTE:
          permissionInt.updateAndGet(i -> i | 8);
          break;
        case OTHERS_READ:
          permissionInt.updateAndGet(i -> i | 4);
          break;
        case OTHERS_WRITE:
          permissionInt.updateAndGet(i -> i | 2);
          break;
        case OTHERS_EXECUTE:
          permissionInt.updateAndGet(i -> i | 1);
          break;
      }
    });
    return permissionInt.get();
  }

  String getPermission() {
    String s = Integer.toOctalString(myPermission);
    if (s.length() < ZEROES.length()) {
      s = ZEROES.substring(s.length()) + s;
    } else if (s.length() < ZEROES.length()) {
      logger.warn("Permissions for file " + myFile.getAbsolutePath() + " calculated as " + s + ". Rolling back permissions to 644");
      s = "644";
    }
    return s;
  }

  @Override
  public void execute(@NotNull final OutputStream out,
                      @NotNull final InputStream in) throws IOException {
    final String command = "C0"+ getPermission() + " " + myFile.length() + " " + myFile.getName() + "\n";
    out.write(command.getBytes());
    out.flush();
    ScpExecUtil.checkScpAck(in);

    // send the content
    FileInputStream fis = null;
    byte[] buf = new byte[1024];
    try {
      fis = new FileInputStream(myFile);
      while (true) {
        int len = fis.read(buf, 0, buf.length);
        if (len <= 0) break;
        out.write(buf, 0, len); //out.flush();
      }
    } finally {
      FileUtil.close(fis);
    }

    // send '\0'
    buf[0] = 0;
    out.write(buf, 0, 1);
    out.flush();
    ScpExecUtil.checkScpAck(in);
  }
}