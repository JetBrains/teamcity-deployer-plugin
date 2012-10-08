package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FtpDeployerRunner extends BaseDeployerRunner {

    public FtpDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
        super(extensionHolder);
    }


    @Override
    protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                              @NotNull final String username,
                                              @NotNull final String password,
                                              @NotNull final String target,
                                              @NotNull final List<ArtifactsCollection> artifactsCollections) {
        return new FtpBuildProcessAdapter(context, target, username, password, artifactsCollections);
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new FtpDeployerRunnerInfo();
    }


}
