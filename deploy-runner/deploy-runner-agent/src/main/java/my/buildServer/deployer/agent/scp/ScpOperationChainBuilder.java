package my.buildServer.deployer.agent.scp;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class ScpOperationChainBuilder {

    /**
     * Build chain of scp operations to copy source file to destination path
     * @param sourceFile source file to copy. Must be a file, not a directory
     * @param destinationPath relative path to destination
     * @return top of resulting operations chain
     */
    public static ScpOperation buildChain(@NotNull final File sourceFile,
                                          @NotNull final String destinationPath) throws IOException {

        if (!sourceFile.exists()) {
            throw new IOException("Source [" + sourceFile.getAbsolutePath() + "] does not exists");
        }

        if (sourceFile.isDirectory()) {
                    throw new IOException("Source [" + sourceFile.getAbsolutePath() + "] is a directory, but a file is expected");
        }

        ScpOperation childOperation = new FileScpOperation(sourceFile);
        File destinationDir = new File(destinationPath);
        while (destinationDir != null) {
            DirScpOperation directoryOperation = new DirScpOperation(destinationDir.getName());
            directoryOperation.add(childOperation);
            childOperation = directoryOperation;
            destinationDir = destinationDir.getParentFile();
        }

        return childOperation;
    }


    /**
     * Build chain of scp operations to copy source file. If sourceFile is a directory, it will be copied recursively
     * @param sourceFile source file to copy.
     * @return top of resulting operations chain
     */
    public static ScpOperation buildChain(File sourceFile) {
        if (sourceFile.isFile()) {
            return new FileScpOperation(sourceFile);
        } else {
            File[] files = sourceFile.listFiles();
            assert files != null;
            final List<ScpOperation> ops = new LinkedList<ScpOperation>();
            for (File file : files) {
                if(file.isFile()) {
                    ops.add(new FileScpOperation(file));
                } else {
                    ops.add(new DirScpOperation(file));
                }
            }
            return new ScpOperation() {
                @Override
                public void execute(@NotNull OutputStream out, @NotNull InputStream in) throws IOException{
                    for (ScpOperation op : ops) {
                        op.execute(out, in);
                    }
                }
            };
        }
    }
}
