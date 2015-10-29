package jetbrains.buildServer.deployer.agent.ftp;

import com.intellij.openapi.diagnostic.Logger;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.UploadInterruptedException;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.WaitFor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.*;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;


class FtpBuildProcessAdapter extends SyncBuildProcessAdapter {
  private static final String FTP_PROTOCOL = "ftp://";

  private static final Logger myInternalLog = Logger.getInstance(FtpBuildProcessAdapter.class.getName());

  private final String myTarget;
  private final String myUsername;
  private final String myPassword;
  private final List<ArtifactsCollection> myArtifacts;
  private final String myTransferMode;
  private final String mySecureMode;
  private final List<Integer> knownMods = Arrays.asList(FTPClient.SECURITY_FTP,
      FTPClient.SECURITY_FTPS, FTPClient.SECURITY_FTPES);
  private final SSLSocketFactory myTrustfulSocketFactory = createTrustfulSocketFactory();

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

    final FTPClient client = new FTPClient();
    try {
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

            final int secureMode = determineSecureMode(mySecureMode);

            client.setSSLSocketFactory(myTrustfulSocketFactory);
            client.setSecurity(secureMode);

            if (port > 0) {
              client.connect(host, port);
            } else {
              client.connect(host);
            }

            client.login(myUsername, myPassword);

            if (FTPRunnerConstants.TRANSFER_MODE_BINARY.equals(myTransferMode)) {
              client.setType(FTPClient.TYPE_BINARY);
            } else if (FTPRunnerConstants.TRANSFER_MODE_ASCII.equals(myTransferMode)) {
              client.setType(FTPClient.TYPE_TEXTUAL);
            }

            if (!StringUtil.isEmpty(path)) {
              createPath(client, path);
              client.changeDirectory(path);
            }

            final String remoteRoot = client.currentDirectory();

            myLogger.message("Starting upload via " + (secureMode == FTPClient.SECURITY_FTP ? "FTP" :
                (secureMode == FTPClient.SECURITY_FTPS ? "FTPS" : "FTPES")) + " to " + myTarget);

            for (ArtifactsCollection artifactsCollection : myArtifacts) {
              int count = 0;
              for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                final File source = fileStringEntry.getKey();
                final String destinationDir = fileStringEntry.getValue();

                if (StringUtil.isNotEmpty(destinationDir)) {
                  createPath(client, destinationDir);
                  client.changeDirectory(destinationDir);
                }
                myInternalLog.debug("Transferring [" + source.getAbsolutePath() + "] to [" + destinationDir + "] under [" + remoteRoot + "]");
                checkIsInterrupted();
                client.upload(source);
                client.changeDirectory(remoteRoot);
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
              client.abortCurrentDataTransfer(true);
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
        if (client.isConnected()) {
          client.disconnect(true);
        }
      } catch (Exception e) {
        Loggers.AGENT.error(e.getMessage(), e);
      }
    }
  }

  private SSLSocketFactory createTrustfulSocketFactory() {
    TrustManager[] trustManager = new TrustManager[]{new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    }};
    SSLContext sslContext = null;
    try {
      sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustManager, new SecureRandom());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
    return sslContext.getSocketFactory();
  }

  private int determineSecureMode(@Nullable String modeString) throws RunBuildException {
    if (StringUtil.isEmpty(modeString)) {
      return FTPClient.SECURITY_FTP;
    } else {
      try {
        final int i = Integer.parseInt(modeString);
        if (knownMods.contains(i)) {
          return i;
        }
      } catch (NumberFormatException e) {
        myInternalLog.warn("Failed to parse FTP security mode. Unknown mode [" + modeString + "]. Will fallback to insecure connection.", e);
      }
      final String message = "Incorrect FTP security mode provided: [" + modeString + "]. Aborting";
      myLogger.error(message);
      throw new RunBuildException(message);
    }
  }

  private void createPath(@NotNull final FTPClient client,
                          @NotNull final String path) throws Exception {
    final String root = client.currentDirectory();
    final String normalisedPath = path.trim().replaceAll("\\\\", "/");
    final StringTokenizer pathTokenizer = new StringTokenizer(normalisedPath, "/");
    if (path.startsWith("/")) {
      client.changeDirectory("/"); // support absolute paths
    }
    boolean prevDirExisted = true;
    while (pathTokenizer.hasMoreTokens()) {
      checkIsInterrupted();
      final String nextDir = pathTokenizer.nextToken();
      if (prevDirExisted && dirExists(nextDir, client)) {
        client.changeDirectory(nextDir);
      } else {
        Exception createException = null;
        try {
          client.createDirectory(nextDir);
          prevDirExisted = false;
        } catch (FTPException e) {
          createException = e;
        }
        try {
          client.changeDirectory(nextDir);
        } catch (FTPException f) {
          String message = "Failed to change current dir to [" + nextDir + "]";
          if (createException != null) {
            message += "\n also failed to create this dir: [" + createException.getMessage() + "]";
          }
          throw new RunBuildException(message, f);
        }
      }
    }

    client.changeDirectory(root);
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
