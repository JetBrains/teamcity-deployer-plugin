package jetbrains.buildServer.deployer.agent.util;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.WaitFor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/3/12, 5:51 PM
 */
public class DeployTestUtils {
    public static void runProcess(final BuildProcess process, final int timeout) throws RunBuildException {
        process.start();
        new WaitFor(timeout) {
            @Override
            protected boolean condition() {
                return process.isFinished();
            }
        };
        assertTrue(process.isFinished(), "Failed to finish test in time");
    }

    public static ArtifactsCollection buildArtifactsCollection(final TempFiles tempFiles, String... destinationDirs) throws IOException {

        final Map<File, String> filePathMap = new HashMap<File, String>();
        for (String destinationDir : destinationDirs) {
            final File content = tempFiles.createTempFile(100);
            filePathMap.put(content, destinationDir);
        }
        return new ArtifactsCollection("dirFrom/**", "dirTo", filePathMap);
    }

    public static void assertCollectionsTransferred(File remoteBase, List<ArtifactsCollection> artifactsCollections) throws IOException {

        for (ArtifactsCollection artifactsCollection : artifactsCollections) {
            for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                final File source = fileStringEntry.getKey();
                final String targetPath = fileStringEntry.getValue() + File.separator + source.getName();
                final File target;
                if (new File(targetPath).isAbsolute()) {
                    target = new File(targetPath);
                } else {
                    target = new File(remoteBase, targetPath);
                }
                assertTrue(target.exists(), "Destination file [" + targetPath + "] does not exist");
                assertEquals(FileUtil.readText(target), FileUtil.readText(source), "wrong content");
            }
        }
    }
}
