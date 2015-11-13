package jetbrains.buildServer.deployer.agent.ftp;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static jetbrains.buildServer.util.FileUtil.getExtension;

/**
 * Created by Nikita.Skvortsov
 * date: 06.11.2015.
 */
abstract class InterruptibleUploadProcess implements Runnable {

  private static final Logger myInternalLog = Logger.getInstance(InterruptibleUploadProcess.class.getName());

  private final FTPClient myClient;
  private final AtomicReference<Exception> myException;
  private BuildProgressLogger myLogger;
  private List<ArtifactsCollection> myArtifacts;
  private boolean myIsAutoType;
  private String myPath;

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
                                    @NotNull final AtomicReference<Exception> exception,
                                    @NotNull final BuildProgressLogger logger,
                                    @NotNull final List<ArtifactsCollection> artifacts,
                                    final boolean isAutoType,
                                    @NotNull final String path) {
    this.myClient = client;
    this.myException = exception;
    this.myLogger = logger;
    this.myArtifacts = artifacts;
    this.myIsAutoType = isAutoType;
    this.myPath = path;
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
          myInternalLog.debug("Transferring [" + source.getAbsolutePath() + "] to [" + destinationDir + "] under [" + remoteRoot + "]");
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
          myInternalLog.debug("done transferring [" + source.getAbsolutePath() + "]");
          count++;
        }
        myLogger.message("Uploaded [" + count + "] files for [" + artifactsCollection.getSourcePath() + "] pattern");
      }
    } catch (Exception t) {
      final String message = "Exception while uploading files: " + t.getMessage();
      myLogger.error(message);
      myInternalLog.debug(message, t);
      myException.set(t);
    }
  }

  private void createPath(@NotNull final String path) throws Exception {
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
          throw new RunBuildException(message);
        }
      }
    }

    checkResult(myClient.changeWorkingDirectory(root));
  }

  private void checkResult(boolean flag) throws Exception {
    if (!flag) {
      throw new RunBuildException("Failed to upload artifacts via FTP. Reply was: " + myClient.getReplyString());
    }
  }

  private boolean dirExists(@NotNull final String nextDir) throws Exception {
    // these directories always exist
    if ("..".equals(nextDir) || ".".equals(nextDir)) {
      return true;
    }
    final String[] strings = myClient.listNames();
    if (strings == null) {
      throw new RunBuildException("Failed to upload artifacts via FTP. Reply was: " + myClient.getReplyString());
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
}
