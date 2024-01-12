

package jetbrains.buildServer.deployer.agent.base;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by Nikita.Skvortsov
 * Date: 10/1/12, 10:44 AM
 */
public abstract class BaseDeployerRunner implements AgentBuildRunner {
  protected final ExtensionHolder myExtensionHolder;

  public BaseDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
    myExtensionHolder = extensionHolder;
  }

  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild,
                                         @NotNull final BuildRunnerContext context) throws RunBuildException {

    final Map<String, String> runnerParameters = context.getRunnerParameters();
    final String username = StringUtil.emptyIfNull(runnerParameters.get(DeployerRunnerConstants.PARAM_USERNAME));
    final String password = StringUtil.emptyIfNull(runnerParameters.get(DeployerRunnerConstants.PARAM_PASSWORD));
    final String target = StringUtil.emptyIfNull(runnerParameters.get(DeployerRunnerConstants.PARAM_TARGET_URL));
    final String sourcePaths = runnerParameters.get(DeployerRunnerConstants.PARAM_SOURCE_PATH);

    final Collection<ArtifactsPreprocessor> preprocessors = myExtensionHolder.getExtensions(ArtifactsPreprocessor.class);

    final ArtifactsBuilder builder = new ArtifactsBuilder();
    builder.setPreprocessors(preprocessors);
    builder.setBaseDir(runningBuild.getCheckoutDirectory());
    builder.setArtifactsPaths(sourcePaths);

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