package jetbrains.buildServer.deployer.agent.ssh.scp;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

class DirScpOperation implements ScpOperation {
    private final String myDirName;
    private final List<ScpOperation> myOps = new LinkedList<ScpOperation>();

    /**
     * Create a recursive copy of a directory
     * @param root - start of directory tree
     */
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

    /**
     * Create a single directory operation
     * @param name directory name
     */
    public DirScpOperation(@NotNull final String name) {
        myDirName = name;
    }

    /**
     * Add additional operation to be executed inside created directory
     * @param operation operation to run inside directory
     */
    public void add(@NotNull final ScpOperation operation) {
        myOps.add(operation);
    }

    @Override
    public void execute(@NotNull final OutputStream out,
                        @NotNull final  InputStream in) throws IOException {
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
