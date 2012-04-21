package my.buildServer.deployer.agent.scp;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
* Created by Kit
* Date: 21.04.12 - 21:59
*/
public class ScpOperationChainBuilder {

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
                public void execute(OutputStream out, InputStream in) throws IOException{
                    for (ScpOperation op : ops) {
                        op.execute(out, in);
                    }
                }
            };
        }
    }
}
