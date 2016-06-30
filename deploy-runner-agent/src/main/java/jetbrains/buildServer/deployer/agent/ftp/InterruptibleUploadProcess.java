package jetbrains.buildServer.deployer.agent.ftp;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static jetbrains.buildServer.util.FileUtil.getExtension;

/**
 * Created by Nikita.Skvortsov
 * date: 06.11.2015.
 */
abstract class InterruptibleUploadProcess implements Runnable {

  private static final Logger LOG = Logger.getInstance(InterruptibleUploadProcess.class.getName());

  private final FTPClient myClient;
  private BuildProgressLogger myLogger;
  private List<ArtifactsCollection> myArtifacts;
  private boolean myIsAutoType;
  private String myPath;
  @NotNull
  private final AtomicBoolean myIsFinishedSuccessfully;

  private final static Set<String> ourKnownAsciiExts = new HashSet<String>();

  static {
    ourKnownAsciiExts.addAll(Arrays.asList("abc", "acgi", "aip", "asm", "asp", "c", "cc", "com", "conf",
        "cpp", "csh", "css", "cxx", "def", "el", "etx", "f", "f77", "f90", "flx", "for", "g", "h",
        "hh", "hh", "hlb", "htc", "htm", "html", "htmls", "htt", "htx", "idc", "jav", "java", "js",
        "ksh", "list", "log", "lsp", "lst", "lsx", "m", "mar", "mcf", "p", "pas", "php", "pl", "pm", "py",
        "rexx", "rt", "rtf", "rtx", "s", "scm", "sdml", "sgm", "sgml", "sh", "shtml",
        "spc", "ssi", "talk", "tcl", "tcsh", "text", "tsv", "txt", "uil", "uni", "unis", "uri", "uris", "uu",
        "uue", "vcs", "wml", "wmls", "wsc", "xml", "zsh"));
  }


  public InterruptibleUploadProcess(@NotNull final FTPClient client,
                                    @NotNull final BuildProgressLogger logger,
                                    @NotNull final List<ArtifactsCollection> artifacts,
                                    final boolean isAutoType,
                                    @NotNull final String path,
                                    @NotNull AtomicBoolean isFinishedSuccessfully) {
    this.myClient = client;
    this.myLogger = logger;
    this.myArtifacts = artifacts;
    this.myIsAutoType = isAutoType;
    this.myPath = path;
    myIsFinishedSuccessfully = isFinishedSuccessfully;
  }

  public void run() {
    try {
      if (!StringUtil.isEmpty(myPath)) {
        createPath(myPath);
        checkResult(myClient.changeWorkingDirectory(myPath));
      }

      final String remoteRoot = myClient.printWorkingDirectory();

      for (ArtifactsCollection artifactsCollection : myArtifacts) {
        int count = 0;
        for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
          final File source = fileStringEntry.getKey();
          final String destinationDir = fileStringEntry.getValue();

          if (StringUtil.isNotEmpty(destinationDir)) {
            createPath(destinationDir);
            checkResult(myClient.changeWorkingDirectory(destinationDir));
          }
          LOG.debug("Transferring [" + source.getAbsolutePath() + "] to [" + destinationDir + "] under [" + remoteRoot + "]");
          checkIsInterrupted();
          InputStream inputStream = null;
          try {
            if (myIsAutoType) {
              checkResult(myClient.setFileType(detectType(source.getName())));
            }
            inputStream = new FileInputStream(source);
            checkResult(myClient.storeFile(source.getName(), inputStream));
          } finally {
            if (inputStream != null) {
              inputStream.close();
            }
          }
          checkResult(myClient.changeWorkingDirectory(remoteRoot));
          checkIsInterrupted();
          LOG.debug("done transferring [" + source.getAbsolutePath() + "]");
          count++;
        }
        myLogger.message("Uploaded [" + count + "] files for [" + artifactsCollection.getSourcePath() + "] pattern");
      }
      myIsFinishedSuccessfully.set(true);
    } catch (FailureDetectedException t) {
      myLogger.error(t.getMessage());
      LOG.debug(t.getMessage(), t);
    } catch (IOException t) {
      myLogger.error(t.getMessage());
      LOG.debug(t.getMessage(), t);
    }
  }

  private void createPath(@NotNull final String path) throws IOException, FailureDetectedException {
    final String root = myClient.printWorkingDirectory();
    final String normalisedPath = path.trim().replaceAll("\\\\", "/");
    final StringTokenizer pathTokenizer = new StringTokenizer(normalisedPath, "/");
    if (path.startsWith("/")) {
      checkResult(myClient.changeWorkingDirectory("/")); // support absolute paths
    }
    boolean prevDirExisted = true;
    while (pathTokenizer.hasMoreTokens()) {
      checkIsInterrupted();
      final String nextDir = pathTokenizer.nextToken();
      if (prevDirExisted && dirExists(nextDir)) {
        checkResult(myClient.changeWorkingDirectory(nextDir));
      } else {
        String mkdirFailureMsg = null;

        if (!myClient.makeDirectory(nextDir)) {
          mkdirFailureMsg = myClient.getReplyString();
        } else {
          prevDirExisted = false;
        }

        if (!myClient.changeWorkingDirectory(nextDir)) {
          String cwdFailureMsg = myClient.getReplyString();
          String message = "Failed to change current dir to [" + nextDir + "]: " + cwdFailureMsg +
              (mkdirFailureMsg != null ? "\n Also failed to create this dir: " + mkdirFailureMsg   : "");
          throw new FailureDetectedException(message);
        }
      }
    }

    checkResult(myClient.changeWorkingDirectory(root));
  }

  private void checkResult(boolean flag) throws FailureDetectedException {
    if (!flag) {
      throw new FailureDetectedException("Failed to upload artifacts via FTP. Reply was: " + myClient.getReplyString());
    }
  }

  private boolean dirExists(@NotNull final String nextDir) throws IOException, FailureDetectedException {
    // these directories always exist
    if ("..".equals(nextDir) || ".".equals(nextDir)) {
      return true;
    }
    final String[] strings = myClient.listNames();
    if (strings == null) {
      throw new FailureDetectedException("Failed to upload artifacts via FTP. Reply was: " + myClient.getReplyString());
    }
    for (String string : strings) {
      if (string.equals(nextDir)) {
        return true;
      }
    }
    return false;
  }


  private int detectType(String name) {
    if (ourKnownAsciiExts.contains(getExtension(name))) {
      return FTP.ASCII_FILE_TYPE;
    } else {
      return FTP.BINARY_FILE_TYPE;
    }
  }

  abstract boolean checkIsInterrupted();

  private class FailureDetectedException extends Exception {
    FailureDetectedException(@NotNull final String message) {
      super(message);
    }
  }
}
