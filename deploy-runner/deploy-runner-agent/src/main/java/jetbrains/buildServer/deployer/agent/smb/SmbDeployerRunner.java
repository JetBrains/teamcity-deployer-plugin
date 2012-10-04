package jetbrains.buildServer.deployer.agent.smb;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmbDeployerRunner extends BaseDeployerRunner {

    public SmbDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
        super(extensionHolder);
    }


    @Override
    protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                              @NotNull final String username,
                                              @NotNull final String password,
                                              @NotNull final String target,
                                              @NotNull final List<ArtifactsCollection> artifactsCollections) {
        return new SMBBuildProcessAdapter(username, password, target, artifactsCollections);
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new SmbDeployerRunnerInfo();
    }


}
