package jetbrains.buildServer.deployer.agent.ftp;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.UploadInterruptedException;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.WaitFor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static jetbrains.buildServer.deployer.agent.DeployerAgentUtils.logBuildProblem;


class FtpBuildProcessAdapter extends SyncBuildProcessAdapter {
  private static final String FTP_PROTOCOL = "ftp://";

  private static final Logger LOG = Logger.getInstance(FtpBuildProcessAdapter.class.getName());
  private static final int STREAM_BUFFER_SIZE = 5 * 1024 * 1024; // 5 Mb
  private static final int SOCKET_BUFFER_SIZE = 1024 * 1024; // 1 Mb
  private static final int DEFAULT_FTP_CONNECT_TIMEOUT = 30 * 1000 * 60; // 30 Min
  private static final int DEFAULT_FTP_CONTROL_KEEP_ALIVE_TIMEOUT = 60; // 60 sec

  private final String myTarget;
  private final String myUsername;
  private final String myPassword;
  private final List<ArtifactsCollection> myArtifacts;
  private final String myTransferMode;
  private final String mySecureMode;
  private final boolean myIsActive;
  private FtpConnectTimeout myFtpConnectTimeout;
  private int myControlKeepAliveTimeout;

  public FtpBuildProcessAdapter(@NotNull final BuildRunnerContext context,
                                @NotNull final String target,
                                @NotNull final String username,
                                @NotNull final String password,
                                @NotNull final List<ArtifactsCollection> artifactsCollections) {
    super(context.getBuild().getBuildLogger());
    myIsActive = "ACTIVE".equals(context.getRunnerParameters().get(FTPRunnerConstants.PARAM_FTP_MODE));
    myTarget = target.toLowerCase().startsWith(FTP_PROTOCOL) ? target : FTP_PROTOCOL + target;
    myUsername = username;
    myPassword = password;
    myArtifacts = artifactsCollections;
    myTransferMode = context.getRunnerParameters().get(FTPRunnerConstants.PARAM_TRANSFER_MODE);
    mySecureMode = context.getRunnerParameters().get(FTPRunnerConstants.PARAM_SSL_MODE);
    myFtpConnectTimeout = getConnectTimeout(context);
    myControlKeepAliveTimeout = getControlKeepAliveTimeout(context);
  }

  private int getControlKeepAliveTimeout(BuildRunnerContext context) {
    String timeout = context.getBuild().getSharedConfigParameters()
            .get(FTPRunnerConstants.PARAM_FTP_CONTROL_KEEP_ALIVE_TIMEOUT);
    if (timeout == null || timeout.isEmpty()) {
      return DEFAULT_FTP_CONTROL_KEEP_ALIVE_TIMEOUT;
    }

    try {
      return Integer.parseInt(timeout);
    } catch (NumberFormatException err) {
      LOG.warn("Incorrect format of controlKeepAliveTimeout '" + timeout + "'. " +
                       "Expecting single integer value. " +
                       "Default value " + DEFAULT_FTP_CONTROL_KEEP_ALIVE_TIMEOUT  + "sec was used");
      return DEFAULT_FTP_CONTROL_KEEP_ALIVE_TIMEOUT;
    }
  }

  private FtpConnectTimeout getConnectTimeout(BuildRunnerContext context) {
    String timeout = context.getBuild().getSharedConfigParameters().get(FTPRunnerConstants.PARAM_FTP_CONNECT_TIMEOUT);
    if (timeout == null || timeout.isEmpty()) {
      return new FtpConnectTimeout();
    }

    String[] timeouts = timeout.split(" ");
    try {
      if (timeouts.length == 1) {
        return new FtpConnectTimeout(getTimeoutFromString(timeout));
      } else if (timeouts.length == 3) {
        return new FtpConnectTimeout(getTimeoutFromString(timeouts[0]),
                                     getTimeoutFromString(timeouts[1]),
                                     getTimeoutFromString(timeouts[2]));
      }
    } catch (NumberFormatException err) {
      //
    }

    LOG.warn("Incorrect format of ftp connect timeout '" + timeout + "'. " +
                     "Expecting either single value integer either three integers for " +
                     "1. socketTimeout  2. connectTimeout  3. dataTimeout. " +
                     "Default value " + DEFAULT_FTP_CONNECT_TIMEOUT  + "ms was used for 2. and 3.");
    return new FtpConnectTimeout();
  }

  private int getTimeoutFromString(String timeout) {
    int timeoutAsInteger = Integer.parseInt(timeout);
    return (timeoutAsInteger > 0) ? timeoutAsInteger : DEFAULT_FTP_CONNECT_TIMEOUT;
  }

  @Override
  public BuildFinishedStatus runProcess() {

    FTPClient clientToDisconnect = null;
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

      final FTPClient client = createClient();
      clientToDisconnect = client;

      if (port > 0) {
        client.connect(host, port);
      } else {
        client.connect(host);
      }

      client.addProtocolCommandListener(new BuildLogCommandListener(myLogger));

      if (myIsActive) {
        client.enterLocalActiveMode();
      } else {
        client.enterLocalPassiveMode();
      }

      final boolean loginSuccessful = client.login(myUsername, myPassword);
      if (!loginSuccessful) {
        logBuildProblem(myLogger, "Failed to login. Reply was: " + client.getReplyString());
        return BuildFinishedStatus.FINISHED_FAILED;
      }

      boolean isAutoType = false;
      if (FTPRunnerConstants.TRANSFER_MODE_BINARY.equals(myTransferMode)) {
        client.setFileType(FTP.BINARY_FILE_TYPE);
      } else if (FTPRunnerConstants.TRANSFER_MODE_ASCII.equals(myTransferMode)) {
        client.setFileType(FTP.ASCII_FILE_TYPE);
      } else {
        isAutoType = true;
      }

      client.setControlKeepAliveTimeout(myControlKeepAliveTimeout); // seconds
      AtomicReference<BuildFinishedStatus> processResult = new AtomicReference<BuildFinishedStatus>(BuildFinishedStatus.FINISHED_SUCCESS);
      final Runnable interruptibleBody = new InterruptibleUploadProcess(client, myLogger, myArtifacts, isAutoType, path, processResult) {
        public boolean checkIsInterrupted() {
          return FtpBuildProcessAdapter.this.isInterrupted();
        }
      };
      final Thread uploadThread = new Thread(interruptibleBody);

      myLogger.message("Starting upload via " + (isNone(mySecureMode) ? "FTP" :
          (isImplicit(mySecureMode) ? "FTPS" : "FTPES")) + " to " + myTarget);
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
        LOG.warn("Ftp upload thread did not reach termination state after wait operation, trying to join");
        try {
          uploadThread.join();
        } catch (InterruptedException e) {
          LOG.warnAndDebugDetails("Interrupted while waiting for FTP upload thread to join.", e);
        }
        LOG.warn("thread joined.");
      }

      return processResult.get();
    } catch (UploadInterruptedException e) {
      myLogger.warning("Ftp upload interrupted.");
      return BuildFinishedStatus.FINISHED_FAILED;
    } catch (SSLException e) {
      if (e.getMessage().contains("unable to find valid certification path to requested target")) {
        logBuildProblem(myLogger,"Failed to setup SSL connection. Looks like target's certificate is not trusted.\n" +
            "See Oracle's documentation on how to import the certificate as a Trusted Certificate.");
      }
      LOG.warnAndDebugDetails("SSL error executing FTP command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } catch (IOException e) {
      logBuildProblem(myLogger, e.getMessage());
      LOG.warnAndDebugDetails("Error executing FTP command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } finally {
      try {
        if (clientToDisconnect != null && clientToDisconnect.isConnected()) {
          clientToDisconnect.disconnect();
        }
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  @NotNull
  private FTPClient createClient() throws SocketException {
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

    client.setBufferSize(FtpBuildProcessAdapter.STREAM_BUFFER_SIZE);
    client.setSendBufferSize(FtpBuildProcessAdapter.SOCKET_BUFFER_SIZE);
    client.setConnectTimeout(myFtpConnectTimeout.connectTimeout);
    client.setDataTimeout(myFtpConnectTimeout.dataTimeout);
    if (myFtpConnectTimeout.socketTimeout > 0) {
      client.setDefaultTimeout(myFtpConnectTimeout.socketTimeout);
    }
    return client;
  }

  private boolean isImplicit(String secureMode) {
    return "1".equals(secureMode);
  }

  private boolean isNone(String secureMode) {
    return StringUtil.isEmpty(secureMode) || "0".equals(secureMode);
  }

  private static class FtpConnectTimeout {
    final int connectTimeout;
    final int dataTimeout;
    final int socketTimeout;

    private FtpConnectTimeout() {
      socketTimeout = -1;
      connectTimeout = DEFAULT_FTP_CONNECT_TIMEOUT;
      dataTimeout = DEFAULT_FTP_CONNECT_TIMEOUT;
    }

    private FtpConnectTimeout(int timeout) {
      socketTimeout = timeout;
      connectTimeout = timeout;
      dataTimeout = timeout;
    }

    private FtpConnectTimeout(int socketTimeout,
                              int connectTimeout,
                              int dataTimeout) {
      this.socketTimeout = socketTimeout;
      this.connectTimeout = connectTimeout;
      this.dataTimeout = dataTimeout;
    }
  }
}
