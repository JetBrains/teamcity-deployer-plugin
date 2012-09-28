package my.buildServer.deployer.agent.smb;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilderAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class SmbDeployerRunner implements AgentBuildRunner {


    private final ExtensionHolder myExtentionHolder;

    public SmbDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
        myExtentionHolder = extensionHolder;
    }


    @NotNull
    @Override
    public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild,
                                           @NotNull final BuildRunnerContext context) throws RunBuildException {

        final String username = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
        final String password = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);
        final String target = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
        final String sourcePaths = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_SOURCE_PATH);

        final Collection<ArtifactsPreprocessor> preprocessors = myExtentionHolder.getExtensions(ArtifactsPreprocessor.class);

        final ArtifactsBuilder builder = new ArtifactsBuilder();
        builder.setPreprocessors(preprocessors);
        builder.setBaseDir(runningBuild.getCheckoutDirectory());
        builder.setCaseInsensitivePatterns(SystemInfo.isWindows);
        builder.setArtifactsPaths(sourcePaths);

        builder.addListener(new ArtifactsBuilderAdapter());

        final List<ArtifactsCollection> artifactsCollections = builder.build();

        return new SMBBuildProcessAdapter(target, username, password, context, artifactsCollections);
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new SmbDeployerRunnerInfo();
    }


}
