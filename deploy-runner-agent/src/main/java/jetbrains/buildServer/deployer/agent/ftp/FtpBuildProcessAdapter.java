package jetbrains.buildServer.deployer.agent.ftp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.UploadInterruptedException;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.WaitFor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static jetbrains.buildServer.util.FileUtil.getExtension;


class FtpBuildProcessAdapter extends SyncBuildProcessAdapter {
  private static final String FTP_PROTOCOL = "ftp://";

  private static final Logger myInternalLog = Logger.getInstance(FtpBuildProcessAdapter.class.getName());
  private static final int STREAM_BUFFER_SIZE = 5 * 1024 * 1024; // 5 Mb
  private static final int SOCKET_BUFFER_SIZE = 1024 * 1024; // 1 Mb

  private final String myTarget;
  private final String myUsername;
  private final String myPassword;
  private final List<ArtifactsCollection> myArtifacts;
  private final String myTransferMode;
  private final String mySecureMode;
  private final static Set<String> ourKnownAsciiExts = new HashSet<String>();

  static {
    ourKnownAsciiExts.add("abc");
    ourKnownAsciiExts.add("acgi");
    ourKnownAsciiExts.add("aip");
    ourKnownAsciiExts.add("asm");
    ourKnownAsciiExts.add("asp");
    ourKnownAsciiExts.add("c");
    ourKnownAsciiExts.add("c");
    ourKnownAsciiExts.add("cc");
    ourKnownAsciiExts.add("cc");
    ourKnownAsciiExts.add("com");
    ourKnownAsciiExts.add("conf");
    ourKnownAsciiExts.add("cpp");
    ourKnownAsciiExts.add("csh");
    ourKnownAsciiExts.add("css");
    ourKnownAsciiExts.add("cxx");
    ourKnownAsciiExts.add("def");
    ourKnownAsciiExts.add("el");
    ourKnownAsciiExts.add("etx");
    ourKnownAsciiExts.add("f");
    ourKnownAsciiExts.add("f");
    ourKnownAsciiExts.add("f77");
    ourKnownAsciiExts.add("f90");
    ourKnownAsciiExts.add("f90");
    ourKnownAsciiExts.add("flx");
    ourKnownAsciiExts.add("for");
    ourKnownAsciiExts.add("for");
    ourKnownAsciiExts.add("g");
    ourKnownAsciiExts.add("h");
    ourKnownAsciiExts.add("h");
    ourKnownAsciiExts.add("hh");
    ourKnownAsciiExts.add("hh");
    ourKnownAsciiExts.add("hlb");
    ourKnownAsciiExts.add("htc");
    ourKnownAsciiExts.add("htm");
    ourKnownAsciiExts.add("html");
    ourKnownAsciiExts.add("htmls");
    ourKnownAsciiExts.add("htt");
    ourKnownAsciiExts.add("htx");
    ourKnownAsciiExts.add("idc");
    ourKnownAsciiExts.add("jav");
    ourKnownAsciiExts.add("jav");
    ourKnownAsciiExts.add("java");
    ourKnownAsciiExts.add("java");
    ourKnownAsciiExts.add("js");
    ourKnownAsciiExts.add("ksh");
    ourKnownAsciiExts.add("list");
    ourKnownAsciiExts.add("log");
    ourKnownAsciiExts.add("lsp");
    ourKnownAsciiExts.add("lst");
    ourKnownAsciiExts.add("lsx");
    ourKnownAsciiExts.add("m");
    ourKnownAsciiExts.add("m");
    ourKnownAsciiExts.add("mar");
    ourKnownAsciiExts.add("mcf");
    ourKnownAsciiExts.add("p");
    ourKnownAsciiExts.add("pas");
    ourKnownAsciiExts.add("php");
    ourKnownAsciiExts.add("pl");
    ourKnownAsciiExts.add("pl");
    ourKnownAsciiExts.add("pm");
    ourKnownAsciiExts.add("py");
    ourKnownAsciiExts.add("rexx");
    ourKnownAsciiExts.add("rt");
    ourKnownAsciiExts.add("rt");
    ourKnownAsciiExts.add("rtf");
    ourKnownAsciiExts.add("rtx");
    ourKnownAsciiExts.add("s");
    ourKnownAsciiExts.add("scm");
    ourKnownAsciiExts.add("scm");
    ourKnownAsciiExts.add("sdml");
    ourKnownAsciiExts.add("sgm");
    ourKnownAsciiExts.add("sgm");
    ourKnownAsciiExts.add("sgml");
    ourKnownAsciiExts.add("sgml");
    ourKnownAsciiExts.add("sh");
    ourKnownAsciiExts.add("shtml");
    ourKnownAsciiExts.add("shtml");
    ourKnownAsciiExts.add("spc");
    ourKnownAsciiExts.add("ssi");
    ourKnownAsciiExts.add("talk");
    ourKnownAsciiExts.add("tcl");
    ourKnownAsciiExts.add("tcsh");
    ourKnownAsciiExts.add("text");
    ourKnownAsciiExts.add("tsv");
    ourKnownAsciiExts.add("txt");
    ourKnownAsciiExts.add("uil");
    ourKnownAsciiExts.add("uni");
    ourKnownAsciiExts.add("unis");
    ourKnownAsciiExts.add("uri");
    ourKnownAsciiExts.add("uris");
    ourKnownAsciiExts.add("uu");
    ourKnownAsciiExts.add("uue");
    ourKnownAsciiExts.add("vcs");
    ourKnownAsciiExts.add("wml");
    ourKnownAsciiExts.add("wmls");
    ourKnownAsciiExts.add("wsc");
    ourKnownAsciiExts.add("xml");
    ourKnownAsciiExts.add("zsh");

  }

  public FtpBuildProcessAdapter(@NotNull final BuildRunnerContext context,
                                @NotNull final String target,
                                @NotNull final String username,
                                @NotNull final String password,
                                @NotNull final List<ArtifactsCollection> artifactsCollections) {
    super(context.getBuild().getBuildLogger());
    myTarget = target.toLowerCase().startsWith(FTP_PROTOCOL) ? target : FTP_PROTOCOL + target;
    myUsername = username;
    myPassword = password;
    myArtifacts = artifactsCollections;
    myTransferMode = context.getRunnerParameters().get(FTPRunnerConstants.PARAM_TRANSFER_MODE);
    mySecureMode = context.getRunnerParameters().get(FTPRunnerConstants.PARAM_SSL_MODE);
  }

  @Override
  public void runProcess() throws RunBuildException {

    FTPClient clientToDisconnect = null;
    try {
      final FTPClient client;
      if (isNone(mySecureMode)) {
        client = new FTPClient();
      } else {
        if (isImplicit(mySecureMode)) {
          client = new FTPSClient(true);
        } else {
          client = new FTPSClient(false);
        }
        ((FTPSClient) client).setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
      }

      clientToDisconnect = client;

      final AtomicReference<Exception> innerException = new AtomicReference<Exception>();
      final Runnable interruptibleBody = new Runnable() {
        public void run() {
          try {
            final URL targetUrl = new URL(myTarget);
            final String host = targetUrl.getHost();
            final int port = targetUrl.getPort();
            final String encodedPath = targetUrl.getPath();

            final String path;
            if (encodedPath.length() > 0) {
              path = URLDecoder.decode(encodedPath.substring(1), "UTF-8");
            } else {
              path = "";
            }

            client.setBufferSize(STREAM_BUFFER_SIZE);
            client.setSendBufferSize(SOCKET_BUFFER_SIZE);

            if (port > 0) {
              client.connect(host, port);
            } else {
              client.connect(host);
            }

            client.login(myUsername, myPassword);

            boolean isAutoType = false;
            if (FTPRunnerConstants.TRANSFER_MODE_BINARY.equals(myTransferMode)) {
              client.setFileType(FTP.BINARY_FILE_TYPE);
            } else if (FTPRunnerConstants.TRANSFER_MODE_ASCII.equals(myTransferMode)) {
              client.setFileType(FTP.ASCII_FILE_TYPE);
            } else {
              isAutoType = true;
            }

            client.setControlKeepAliveTimeout(60); // seconds

            if (!StringUtil.isEmpty(path)) {
              createPath(client, path);
              client.changeWorkingDirectory(path);
            }

            final String remoteRoot = client.printWorkingDirectory();

            myLogger.message("Starting upload via " + (isNone(mySecureMode) ? "FTP" :
                (isImplicit(mySecureMode) ? "FTPS" : "FTPES")) + " to " + myTarget);

            for (ArtifactsCollection artifactsCollection : myArtifacts) {
              int count = 0;
              for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                final File source = fileStringEntry.getKey();
                final String destinationDir = fileStringEntry.getValue();

                if (StringUtil.isNotEmpty(destinationDir)) {
                  createPath(client, destinationDir);
                  client.changeWorkingDirectory(destinationDir);
                }
                myInternalLog.debug("Transferring [" + source.getAbsolutePath() + "] to [" + destinationDir + "] under [" + remoteRoot + "]");
                checkIsInterrupted();
                InputStream inputStream = null;
                try {
                  if (isAutoType) {
                    client.setFileType(detectType(source.getName()));
                  }
                  inputStream = new FileInputStream(source);
                  client.storeFile(source.getName(), inputStream);
                } finally {
                  if (inputStream != null) {
                    inputStream.close();
                  }
                }
                client.changeWorkingDirectory(remoteRoot);
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
            innerException.set(t);
          }
        }
      };


      final Thread uploadThread = new Thread(interruptibleBody);

      uploadThread.start();

      new WaitFor(Long.MAX_VALUE, 1000) {
        @Override
        protected boolean condition() {
          if (uploadThread.getState() == Thread.State.TERMINATED) {
            return true;
          }
          try {
            if (isInterrupted()) {
              client.abort();
              uploadThread.join();
              return true;
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          return false;
        }
      };

      if (uploadThread.getState() != Thread.State.TERMINATED) {
        myInternalLog.warn("Ftp upload thread did not reach termination state after wait operation, trying to join");
        uploadThread.join();
        myInternalLog.warn("thread joined.");
      }

      final Exception exception = innerException.get();
      if (exception != null) {
        throw exception;
      }

    } catch (UploadInterruptedException e) {
      myLogger.warning("Ftp upload interrupted.");
    } catch (SSLException e) {
      if (e.getMessage().contains("unable to find valid certification path to requested target")) {
        myLogger.warning("Failed to setup SSL connection. Looks like target's certificate is not trusted.\n" +
            "See Oracle's documentation on how to import the certificate as a Trusted Certificate.");
      }
      throw new RunBuildException(e);
    } catch (Exception e) {
      if (e instanceof RunBuildException) {
        throw (RunBuildException) e;
      } else {
        throw new RunBuildException(e);
      }
    } finally {
      try {
        if (clientToDisconnect != null && clientToDisconnect.isConnected()) {
          clientToDisconnect.disconnect();
        }
      } catch (Exception e) {
        Loggers.AGENT.error(e.getMessage(), e);
      }
    }
  }

  private int detectType(String name) {
    if (ourKnownAsciiExts.contains(getExtension(name))) {
      return FTP.ASCII_FILE_TYPE;
    } else {
      return FTP.BINARY_FILE_TYPE;
    }
  }

  private boolean isImplicit(String secureMode) {
    return "1".equals(secureMode);
  }

  private boolean isNone(String secureMode) {
    return StringUtil.isEmpty(secureMode) || "0".equals(secureMode);
  }

  private void createPath(@NotNull final FTPClient client,
                          @NotNull final String path) throws Exception {
    final String root = client.printWorkingDirectory();
    final String normalisedPath = path.trim().replaceAll("\\\\", "/");
    final StringTokenizer pathTokenizer = new StringTokenizer(normalisedPath, "/");
    if (path.startsWith("/")) {
      client.changeWorkingDirectory("/"); // support absolute paths
    }
    boolean prevDirExisted = true;
    while (pathTokenizer.hasMoreTokens()) {
      checkIsInterrupted();
      final String nextDir = pathTokenizer.nextToken();
      if (prevDirExisted && dirExists(nextDir, client)) {
        client.changeWorkingDirectory(nextDir);
      } else {
        Exception createException = null;
        try {
          client.makeDirectory(nextDir);
          prevDirExisted = false;
        } catch (IOException e) {
          createException = e;
        }
        try {
          client.changeWorkingDirectory(nextDir);
        } catch (IOException f) {
          String message = "Failed to change current dir to [" + nextDir + "]";
          if (createException != null) {
            message += "\n also failed to create this dir: [" + createException.getMessage() + "]";
          }
          throw new RunBuildException(message, f);
        }
      }
    }

    client.changeWorkingDirectory(root);
  }

  private boolean dirExists(@NotNull final String nextDir,
                            @NotNull final FTPClient client) throws Exception {
    final String[] strings = client.listNames();
    for (String string : strings) {
      if (string.equals(nextDir)) {
        return true;
      }
    }
    return false;
  }
}
