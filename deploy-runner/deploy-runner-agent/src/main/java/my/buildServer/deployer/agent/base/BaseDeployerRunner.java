package my.buildServer.deployer.agent.base;

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

/**
 * Created by Nikita.Skvortsov
 * Date: 10/1/12, 10:44 AM
 */
public abstract class BaseDeployerRunner implements AgentBuildRunner {
    protected final ExtensionHolder myExtentionHolder;

    public BaseDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
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

        return getDeployerProcess(context, username, password, target, artifactsCollections);
    }

    protected abstract BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                                       @NotNull final String username,
                                                       @NotNull final String password,
                                                       @NotNull final String target,
                                                       @NotNull final List<ArtifactsCollection> artifactsCollections) throws RunBuildException;

    @NotNull
    @Override
    public abstract AgentBuildRunnerInfo getRunnerInfo();
}
