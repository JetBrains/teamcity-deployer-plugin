package my.buildServer.deployer.agent.scp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
* Created by Kit
* Date: 21.04.12 - 22:15
*/
class DirScpOperation implements ScpOperation {
    private final String myDirName;
    private final List<ScpOperation> myOps = new LinkedList<ScpOperation>();

    public DirScpOperation(File root) {
        File[] dirContent = root.listFiles();
        assert root.isDirectory() && dirContent != null;
        myDirName = root.getName();
        for (File file : dirContent) {
            if (file.isDirectory()) {
                myOps.add(new DirScpOperation(file));
            } else {
                myOps.add(new FileScpOperation(file));
            }
        }
    }

    @Override
    public void execute(OutputStream out, InputStream in) throws IOException {
        final String command = "D0755 0 " + myDirName + "\n";
        out.write(command.getBytes()); out.flush();
        ScpExecUtil.checkScpAck(in);

        for (ScpOperation myOp : myOps) {
            myOp.execute(out, in);
        }

        final String endDir = "E\n";
        out.write(endDir.getBytes()); out.flush();
        ScpExecUtil.checkScpAck(in);
    }
}
