package jetbrains.buildServer.deployer.agent.smb;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb.SMB1NotSupportedException;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.Share;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.deployer.agent.UploadInterruptedException;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OVERWRITE_IF;
import static jetbrains.buildServer.deployer.agent.DeployerAgentUtils.logBuildProblem;


@SuppressWarnings("unused") // used via reflection
public class SMBJBuildProcessAdapter extends SyncBuildProcessAdapter {

  private static final Logger LOG = Logger.getInstance(SMBJBuildProcessAdapter.class.getName());
  private static final int STREAM_BUFFER_SIZE = 1024 * 1024; // use 1 Mb buffer

  private final String myTarget;
  private final String myUsername;
  private final String myPassword;
  private final List<ArtifactsCollection> myArtifactsCollections;
  private final String myDomain;


  @SuppressWarnings("unused") // used via reflection
  public SMBJBuildProcessAdapter(@NotNull final BuildRunnerContext context,
                                 @NotNull final String username,
                                 @NotNull final String password,
                                 @Nullable final String domain,
                                 @NotNull final String target,
                                 @NotNull final List<ArtifactsCollection> artifactsCollections) {
    super(context.getBuild().getBuildLogger());
    myTarget = target;
    myUsername = username;
    myPassword = password;
    myDomain = domain;
    myArtifactsCollections = artifactsCollections;
  }

  @Override
  public BuildFinishedStatus runProcess() {
    String target;
    if (myTarget.startsWith("\\\\")) {
      target = myTarget.substring(2);
    } else {
      target = myTarget;
    }

    target = target.replaceAll("/", "\\\\");

    final String settingsString = "Trying to connect with following parameters:\n" +
            "username=[" + myUsername + "]\n" +
            "domain=[" + (myDomain == null ? "" : myDomain) + "]\n" +
            "target=[" + target + "]";

    Loggers.AGENT.debug(settingsString);
    myLogger.message("Starting upload via SMB to " + myTarget);

    final List<String> components = StringUtil.split(target, "\\");
    final String host = components.remove(0);
    final String shareName = components.remove(0);
    final String pathInShare = StringUtil.join(components, "\\");

    try {
      SmbConfig config = SmbConfig
              .builder()
              .withMultiProtocolNegotiate(true)
              .withSigningRequired(true).build();

      SMBClient client = new SMBClient(config);

      Connection connection = client.connect(host);
      Session session = connection.authenticate(new AuthenticationContext(myUsername, myPassword.toCharArray(), myDomain));
      Share share = session.connectShare(shareName);

      if (share instanceof DiskShare) {
        DiskShare diskShare = (DiskShare)share;
        for (ArtifactsCollection artifactsCollection : myArtifactsCollections) {
          final int numOfUploadedFiles = upload(artifactsCollection.getFilePathMap(), diskShare, pathInShare);
          myLogger.message("Uploaded [" + numOfUploadedFiles + "] files for [" + artifactsCollection.getSourcePath() + "] pattern");
        }

      } else {
        logBuildProblem(myLogger, "Shared resource [" + shareName + "] is not a folder, can not upload files.");
        return BuildFinishedStatus.FINISHED_FAILED;
      }

      return BuildFinishedStatus.FINISHED_SUCCESS;
    } catch (TransportException e) {

      final String message;
      if (hasCauseOfType(SMB1NotSupportedException.class, e)) {
        message = "The remote host [" + host + "] does not support SMBv2 or support was explicitly disabled. Please, check the remote host configuration";
      } else {
        message = e.getMessage();
      }
      logBuildProblem(myLogger, message);

      LOG.warnAndDebugDetails("Error executing SMB command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    } catch (UploadInterruptedException e) {
      myLogger.warning("SMB upload interrupted.");
      return BuildFinishedStatus.FINISHED_FAILED;
    } catch (IOException | SMBRuntimeException e) {
      logBuildProblem(myLogger, e.getMessage());
      LOG.warnAndDebugDetails("Error executing SMB command", e);
      return BuildFinishedStatus.FINISHED_FAILED;
    }
  }

  private boolean hasCauseOfType(@NotNull Class<? extends Throwable> exceptionClass, @NotNull Throwable e) {
    Throwable current = e;
    if (exceptionClass.isAssignableFrom(e.getClass())) {
      return true;
    }

    while (current != null && current.getCause() != current) {
      if (exceptionClass.isAssignableFrom(current.getClass())) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private void maybeCreate(@NotNull final DiskShare diskShare, @NotNull final String pathInShare) {
    String existingPrefix = FileUtil.normalizeRelativePath(pathInShare).replace('/', '\\');
    final Stack<String> toCreate = new Stack<>();

    while (existingPrefix.length() > 0 && !diskShare.folderExists(existingPrefix)) {
      final int endIndex = existingPrefix.lastIndexOf('\\');
      if (endIndex > -1) {
        toCreate.push(existingPrefix.substring(endIndex + 1));
        existingPrefix = existingPrefix.substring(0, endIndex);
      } else {
        toCreate.push(existingPrefix);
        existingPrefix = "";
      }
    }

    while (!toCreate.empty()) {
      existingPrefix = (existingPrefix.length() > 0 ? existingPrefix + "\\" : "") + toCreate.pop();
      diskShare.mkdir(existingPrefix);
    }
  }

  private int upload(Map<File, String> filePathMap, DiskShare share, String prefixPath) throws IOException {
    int count = 0;

    Map<File, String> fileFullPathMap = new HashMap<>();

    if (prefixPath.length() > 0) {
      for (Map.Entry<File, String> entry : filePathMap.entrySet()) {
        fileFullPathMap.put(entry.getKey(), prefixPath + "\\" + entry.getValue());
      }
    } else {
      fileFullPathMap.putAll(filePathMap);
    }

    for (Map.Entry<File, String> fileDestEntry : fileFullPathMap.entrySet()) {
      checkIsInterrupted();

      final File source = fileDestEntry.getKey();
      final String targetPath = fileDestEntry.getValue().replace('/', '\\');

      maybeCreate(share, targetPath);

      final String targetName = (targetPath.length() > 0 ? targetPath + "\\" : "") + source.getName();
      final com.hierynomus.smbj.share.File targetFile = share.openFile(targetName,
              EnumSet.of(AccessMask.GENERIC_WRITE),
              null,
              SMB2ShareAccess.ALL,
              FILE_OVERWRITE_IF,
              null);

      Loggers.AGENT.debug("Uploading source=[" + source.getAbsolutePath() + "] to \n" +
          " destFile=[" + targetName + "]");

      FileInputStream inputStream = null;
      OutputStream outputStream = null;

      try {
        inputStream = new FileInputStream(source);
        outputStream = targetFile.getOutputStream();
        copyInterruptibly(inputStream, outputStream);
        outputStream.flush();
      } finally {
        FileUtil.close(inputStream);
        FileUtil.close(outputStream);
        targetFile.close();
      }
      LOG.debug("Done transferring [" + source.getAbsolutePath() + "]");
      count++;
    }
    return count;
  }

  private void copyInterruptibly(@NotNull FileInputStream inputStream, @NotNull OutputStream outputStream) throws IOException {
    byte[] buf = new byte[STREAM_BUFFER_SIZE];
    int read;
    while ((read = inputStream.read(buf)) > -1) {
      checkIsInterrupted();
      outputStream.write(buf, 0, read);
    }
  }

}
