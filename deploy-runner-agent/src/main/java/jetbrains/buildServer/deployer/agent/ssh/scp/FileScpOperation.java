package jetbrains.buildServer.deployer.agent.ssh.scp;

import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
* Created by Kit
* Date: 21.04.12 - 22:15
*/
class FileScpOperation implements ScpOperation {
    final File myFile;

    public FileScpOperation(File file) {
        assert file.isFile();
        myFile = file;
    }

    @Override
    public void execute(@NotNull final OutputStream out,
                        @NotNull final InputStream in) throws IOException {
        final String command = "C0755 " + myFile.length() + " " + myFile.getName() + "\n";
        out.write(command.getBytes()); out.flush();
        ScpExecUtil.checkScpAck(in);

        // send the content
        FileInputStream fis = null;
        byte[] buf=new byte[1024];
        try {
            fis = new FileInputStream(myFile);
            while(true){
                int len=fis.read(buf, 0, buf.length);
                if(len<=0) break;
                out.write(buf, 0, len); //out.flush();
            }
        } finally {
            FileUtil.close(fis);
        }

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
        ScpExecUtil.checkScpAck(in);
    }
}
