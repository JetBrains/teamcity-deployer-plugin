package my.buildServer.deployer.agent.sftp;

import com.intellij.util.WaitFor;
import com.jcraft.jsch.*;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;


class SftpBuildProcessAdapter extends BuildProcessAdapter {

    private final String target;
    private final String username;
    private final String password;
    private final BuildRunnerContext context;
    private final String sourcePath;

    private volatile boolean hasFinished;

    public SftpBuildProcessAdapter(String target, String username, String password, BuildRunnerContext context, String sourcePath) {
        this.target = target;
        this.username = username;
        this.password = password;
        this.context = context;
        this.sourcePath = sourcePath;
        hasFinished = false;
    }

    @NotNull
    @Override
    public BuildFinishedStatus waitFor() throws RunBuildException {
        while (!isInterrupted() && !hasFinished) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RunBuildException(e);
            }
        }
        return hasFinished ? BuildFinishedStatus.FINISHED_SUCCESS :
                BuildFinishedStatus.INTERRUPTED;
    }

    @Override
    public void start() throws RunBuildException {
        final String host = target.substring(0, target.indexOf(':'));
        final String remotePath = target.substring(target.indexOf(':')+1);
        final String escapedRemotePath = remotePath.trim().replaceAll("\\\\", "/");

        JSch jsch=new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session session = null;

        try {
            session = jsch.getSession(username, host, 22);
            session.setPassword(password);
            session.connect();

            createRemotePath(session, escapedRemotePath);
            if (isInterrupted()) return;

            Channel channel=session.openChannel("sftp");
            channel.connect();
            ChannelSftp c=(ChannelSftp)channel;

            c.put(new File(context.getWorkingDirectory(), sourcePath).getAbsolutePath(), remotePath);
            c.disconnect();

        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
        hasFinished = true;
    }

    private void createRemotePath(final @NotNull Session session,
                                     final @NotNull String escapedRemoteBase) throws JSchException, IOException {

           if (StringUtil.isEmptyOrSpaces(escapedRemoteBase)) {
               return;
           }

           assert session.isConnected();

           final String command= "mkdir -p " + escapedRemoteBase;
           final ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
           final ChannelExec execChannel = (ChannelExec) session.openChannel("exec");

           execChannel.setCommand(command);
           execChannel.setExtOutputStream(os);
           execChannel.connect();

           WaitFor waitFor = new WaitFor(5000) {
               @Override
               protected boolean condition() {
                   return execChannel.isClosed();
               }
           };

           if (!waitFor.isConditionRealized()) {
               throw new IOException("Timed out waiting for remote command [" + command + "] to execute");
           }

           if(execChannel.getExitStatus() != 0) {
               throw new IOException(os.toString());
           }
       }

}
