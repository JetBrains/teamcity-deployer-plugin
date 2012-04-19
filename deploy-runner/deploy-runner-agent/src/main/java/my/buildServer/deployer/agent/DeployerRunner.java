package my.buildServer.deployer.agent;

import com.jcraft.jsch.*;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.WaitFor;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:26
 */
public class DeployerRunner implements AgentBuildRunner {

    private static final String SMB = "smb://";
    private static final String SCP = "spc://";

    @NotNull
    @Override
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {

        final String username = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
        final String password = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);
        final String target = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
        final String sourcePath = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_SOURCE_PATH);

        return new BuildProcessAdapter() {

            private volatile boolean hasFinished = false;

            @NotNull
            @Override
            public BuildFinishedStatus waitFor() throws RunBuildException {
                new WaitFor(15000, 100) {
                    @Override
                    protected boolean condition() {
                        return hasFinished;
                    }
                };
                return hasFinished ? BuildFinishedStatus.FINISHED_SUCCESS :
                        BuildFinishedStatus.FINISHED_FAILED;
            }

            @Override
            public void start() throws RunBuildException {
                String host = target.substring(0, target.indexOf(':'));
                String remotePath = target.substring(target.indexOf(':')+1);
                File localFile = new File(context.getWorkingDirectory(), sourcePath);
                assert localFile.exists();
                try {
                    copy(localFile, remotePath + "/" + localFile.getName(), host);
                } catch (Exception e) {
                    throw new RunBuildException(e);
                }
                hasFinished = true;
            }

            private void copy(File localFile, String remoteFile, String host) throws Exception {
                FileInputStream fis=null;
                try{
                    String[] normRemoteFile = remoteFile.replaceAll("\\\\", "/").split("/");
                    JSch jsch=new JSch();
                    JSch.setConfig("StrictHostKeyChecking", "no");
                    Session session=jsch.getSession(username, host, 22);

                    // username and password will be given via UserInfo interface.
                    session.setPassword(password);
                    session.connect();

                    // exec 'scp -t rfile' remotely
                    String command= "scp -rt .";
                    Channel channel=session.openChannel("exec");
                    ((ChannelExec)channel).setCommand(command);

                    // get I/O streams for remote scp
                    OutputStream out=channel.getOutputStream();
                    InputStream in=channel.getInputStream();

                    channel.connect();
                    checkAck(in);

                    int depth;
                    for (depth = 0; depth < normRemoteFile.length; depth++) {
                        command = "D0755 0 " + normRemoteFile[depth] + "\n";
                        out.write(command.getBytes()); out.flush();
                        checkAck(in);
                    }

                    // send "C0644 filesize filename", where filename should not include '/'
                    command = "C0644 " + localFile.length() + " " + localFile.getName() + "\n";
                    out.write(command.getBytes()); out.flush();
                    checkAck(in);

                    // send a content of lfile
                    fis=new FileInputStream(localFile);
                    byte[] buf=new byte[1024];
                    while(true){
                        int len=fis.read(buf, 0, buf.length);
                        if(len<=0) break;
                        out.write(buf, 0, len); //out.flush();
                    }
                    fis.close();
                    fis=null;
                    // send '\0'
                    buf[0]=0; out.write(buf, 0, 1); out.flush();
                    checkAck(in);

                    for (; depth > 0; depth--) {
                        command = "E\n";
                        out.write(command.getBytes()); out.flush();
                        checkAck(in);
                    }

                    out.close();

                    channel.disconnect();
                    session.disconnect();
                } finally {
                    FileUtil.close(fis);
                }
            }

            private int checkAck(InputStream in) throws IOException{
                int b=in.read();
                // b may be 0 for success,
                //          1 for error,
                //          2 for fatal error,
                //          -1
                if(b==0) return b;
                if(b==-1) return b;

                if(b==1 || b==2){
                    final StringBuilder sb=new StringBuilder();
                    int c;
                    do {
                        c=in.read();
                        sb.append((char)c);
                    }
                    while(c!='\n');
                    throw new IOException(sb.toString());
                }
                return b;
            }
        };
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new DeployerRunnerInfo();
    }

    private static class SMBBuildProcessAdapter extends BuildProcessAdapter {
        private final String target;
        private final String username;
        private final String password;
        private final BuildRunnerContext context;
        private final String sourcePath;

        public SMBBuildProcessAdapter(String target, String username, String password, BuildRunnerContext context, String sourcePath) {
            this.target = target;
            this.username = username;
            this.password = password;
            this.context = context;
            this.sourcePath = sourcePath;
        }

        @Override
        public void start() throws RunBuildException {
            final String targetWithProtocol;
            if (!target.startsWith(SMB)) {
                targetWithProtocol = SMB + target;
            } else {
                targetWithProtocol = target;
            }
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", username, password);
            final String settingsString = "Trying to connect with following parameters:\n" +
                    "username=[" + username + "]\n" +
                    "password=[" + password + "]\n" +
                    "target=[" + targetWithProtocol + "]";
            try {
                Loggers.AGENT.debug(settingsString);
                SmbFile destinationDir = new SmbFile(targetWithProtocol, auth);
                File source = new File(context.getWorkingDirectory(), sourcePath);
                if (source.isDirectory()) {
                    File[] files = source.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (!file.isDirectory()) {
                                upload(file, destinationDir);
                            }
                        }
                    }
                }  else {
                    upload(source, destinationDir);
                }
            } catch (Exception e) {
                Loggers.AGENT.error(settingsString, e);
                throw new RunBuildException(e);
            }
        }

        private void upload(File source, SmbFile destinationDir) throws IOException {
            SmbFile destFile = new SmbFile(destinationDir, source.getName());
            Loggers.AGENT.debug("Uploading source=[" + source.getAbsolutePath() + "] to \n" +
                    "target=[" + destFile.getCanonicalPath() + "]");
            FileInputStream inputStream = new FileInputStream(source);
            OutputStream outputStream = destFile.getOutputStream();
            try {
                FileUtil.copy(inputStream, outputStream);
                outputStream.flush();
            } finally {
                FileUtil.close(inputStream);
                FileUtil.close(outputStream);
            }
        }
    }
}
