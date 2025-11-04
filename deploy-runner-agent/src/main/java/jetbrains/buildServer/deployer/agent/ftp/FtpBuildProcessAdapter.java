

package jetbrains.buildServer.deployer.agent.ftp;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.UploadInterruptedException;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import jetbrains.buildServer.deployer.common.FtpSSLMode;
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
import static jetbrains.buildServer.deployer.common.FtpSSLMode.FTPES;


class FtpBuildProcessAdapter extends SyncBuildProcessAdapter {
  private static final String FTP_PROTOCOL = "ftp://";
  private static final String FTPS_PROTOCOL = "ftps://";
  private static final String FTPS_SECURITY_MODE_DEFAULT = "1";

  private static final Logger LOG = Logger.getInstance(FtpBuildProcessAdapter.class.getName());
  private static final int STREAM_BUFFER_SIZE = 5 * 1024 * 1024; // 5 Mb
  private static final int SOCKET_BUFFER_SIZE = 1024 * 1024; // 1 Mb
  private static final long PBSZ = 16384;

  private final String myTarget;
  private final String myUsername;
  private final String myPassword;
  private final List<ArtifactsCollection> myArtifacts;
  private final String myTransferMode;
  private final FtpSSLMode mySecureMode;
  private final boolean myIsActive; // FTP Active Mode
  private final DataChannelProtection myDataChannelProtection;
  private final FtpTimeout myTimeout;

  public FtpBuildProcessAdapter(@NotNull final BuildRunnerContext context,
                                @NotNull final String target,
                                @NotNull final String username,
                                @NotNull final String password,
                                @NotNull final List<ArtifactsCollection> artifactsCollections) {
    super(context.getBuild().getBuildLogger());
    myIsActive = "ACTIVE".equals(context.getRunnerParameters().get(FTPRunnerConstants.PARAM_FTP_MODE));
    myTarget = formatProtocol(target);
    myUsername = username;
    myPassword = password;
    myArtifacts = artifactsCollections;
    myTransferMode = context.getRunnerParameters().get(FTPRunnerConstants.PARAM_TRANSFER_MODE);
    mySecureMode = FtpSSLMode.getByCode(context.getRunnerParameters().get(FTPRunnerConstants.PARAM_SSL_MODE), FTPES);
    myDataChannelProtection = DataChannelProtection.getByCode(context.getRunnerParameters().get(FTPRunnerConstants.DATA_CHANNEL_PROTECTION), DataChannelProtection.PRIVATE);
    myTimeout = FtpTimeout.parseTimeout(context);
  }

  private static boolean startsWithFtpsProtocol(String target) {
    return target.toLowerCase().startsWith(FTPS_PROTOCOL);
  }

  private static String formatProtocol(String target) {
    if (target == null)
      throw new IllegalArgumentException("Target must not be null");
    // strip protocols
    if (target.toLowerCase().startsWith(FTP_PROTOCOL))
      target = target.substring(FTP_PROTOCOL.length());
    if (startsWithFtpsProtocol(target)) {
      target = target.substring(FTPS_PROTOCOL.length());
    }
    return FTP_PROTOCOL + target;
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

      AtomicReference<BuildFinishedStatus> processResult = new AtomicReference<BuildFinishedStatus>(BuildFinishedStatus.FINISHED_SUCCESS);
      final Runnable interruptibleBody = new InterruptibleUploadProcess(client, myLogger, myArtifacts, isAutoType, path, processResult) {
        public boolean checkIsInterrupted() {
          return FtpBuildProcessAdapter.this.isInterrupted();
        }
      };
      AtomicReference<IOException> exceptionAtomicReference = new AtomicReference<>();
      final Thread uploadThread = new Thread(() -> {
        try {
          interruptibleBody.run();
        } catch (RetryWithPrivateSettingsException e) {
          try {
            enableDataChannelProtection(client, DataChannelProtection.PRIVATE);
            interruptibleBody.run();
          } catch (IOException e1) {
            exceptionAtomicReference.set(e1);
          }
        }
      });

      if (exceptionAtomicReference.get() != null)
        throw exceptionAtomicReference.get();

      myLogger.message("Starting upload via " + mySecureMode.name() + " to " + myTarget);
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

  private void enableDataChannelProtection(FTPClient client, DataChannelProtection dcp) throws IOException {
    try {
      FTPSClient ftpsClient = (client instanceof FTPSClient) ? (FTPSClient)client : null;
      if (ftpsClient != null) {
        long bufferSize = ftpsClient.parsePBSZ(PBSZ);
        ftpsClient.execPROT(dcp.getCodeAsString());
        myLogger.message("Negotiated " + bufferSize + " PBSZ buffer size" + ftpsClient.getReplyString());
      }
    } catch (SSLException e) {
      if (e.getMessage().contains("536 PROT not supported")) {
        myLogger.warning("Failed to setup data channel protection. Looks like target's FTPS server does not support PROT command.");
      }
    }
  }

  @NotNull
  private FTPClient createClient() throws SocketException {
    final FTPClient client;
    if (mySecureMode.isNonSecure()) {
      client = new FTPClient();
    } else {
      client = new FTPSClient(mySecureMode.isImplicit());
      ((FTPSClient) client).setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
    }

    client.setBufferSize(FtpBuildProcessAdapter.STREAM_BUFFER_SIZE);
    client.setSendBufferSize(FtpBuildProcessAdapter.SOCKET_BUFFER_SIZE);
    if (myTimeout.controlKeepAliveTimeoutEnabled())
      client.setControlKeepAliveTimeout(myTimeout.getControlKeepAliveTimeoutInSecs());
    if (myTimeout.connectTimeoutEnabled())
      client.setConnectTimeout(myTimeout.getConnectTimeout());
    if (myTimeout.dataTimeoutEnabled())
      client.setDataTimeout(myTimeout.getDataTimeout());
    if (myTimeout.socketTimeoutEnabled())
      client.setDefaultTimeout(myTimeout.getSocketTimeout());

    return client;
  }
}