package my.buildServer.deployer.agent.scp;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import my.buildServer.deployer.agent.base.BaseDeployerRunner;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:26
 */
public class ScpDeployerRunner extends BaseDeployerRunner {

    public ScpDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
        super(extensionHolder);
    }

    @Override
    protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                              @NotNull final String username,
                                              @NotNull final String password,
                                              @NotNull final String target,
                                              @NotNull final List<ArtifactsCollection> artifactsCollections) {
        return new ScpProcessAdapter(context, username, password, target, artifactsCollections);
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new ScpDeployerRunnerInfo();
    }


}
