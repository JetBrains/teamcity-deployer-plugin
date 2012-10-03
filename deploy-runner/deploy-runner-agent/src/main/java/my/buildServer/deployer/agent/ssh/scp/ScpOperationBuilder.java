package my.buildServer.deployer.agent.ssh.scp;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public class ScpOperationBuilder {

    /**
     * Build chain of scp operations to copy source file to destination path
     * @param sourceFile source file to copy. Must be a file, not a directory
     * @param destinationPath relative path to destination
     * @return top of resulting operations chain
     */
    public static ScpOperation getCopyFileOperation(@NotNull final File sourceFile,
                                                    @NotNull final String destinationPath) throws IOException {

        if (!sourceFile.exists()) {
            throw new IOException("Source [" + sourceFile.getAbsolutePath() + "] does not exists");
        }

        if (sourceFile.isDirectory()) {
            throw new IOException("Source [" + sourceFile.getAbsolutePath() + "] is a directory, but a file is expected");
        }

        ScpOperation fileOperation = new FileScpOperation(sourceFile);

        return doCreatePathOperation(destinationPath, fileOperation);
    }


    /**
     * Build chain of scp opertaions to create empty directory.
     * @param remotePath path to create
     * @return top of resulting operations chain
     */

    public static ScpOperation getCreatePathOperation(@NotNull final String remotePath) {
        return doCreatePathOperation(remotePath, null);
    }


    private static ScpOperation doCreatePathOperation(@NotNull final String remotePath,
                                                      @Nullable final ScpOperation chainTailOperation) {
        final String normalisedPath = remotePath.replaceAll("\\\\","/");
        File remoteDir = new File(normalisedPath);
        DirScpOperation childOperation = new DirScpOperation(remoteDir.getName());
        if (null != chainTailOperation) {
            childOperation.add(chainTailOperation);
        }
        remoteDir = remoteDir.getParentFile();

        while (remoteDir != null) {
            final String name = remoteDir.getName();
            final DirScpOperation directoryOperation;
            if (StringUtil.isEmpty(name) && remoteDir.isAbsolute()) {
                directoryOperation = new DirScpOperation("/"+remoteDir.getAbsolutePath());
            } else {
                directoryOperation = new DirScpOperation(name);
            }

            directoryOperation.add(childOperation);

            childOperation = directoryOperation;
            remoteDir = remoteDir.getParentFile();
        }
        return childOperation;
    }
}
