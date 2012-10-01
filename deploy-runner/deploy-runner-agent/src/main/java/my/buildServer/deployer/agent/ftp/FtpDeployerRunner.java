package my.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import my.buildServer.deployer.agent.base.BaseDeployerRunner;
import my.buildServer.deployer.common.DeployerRunnerConstants;
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
        return new FtpBuildProcessAdapter(target, username, password, context, artifactsCollections);
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new FtpDeployerRunnerInfo();
    }


}
