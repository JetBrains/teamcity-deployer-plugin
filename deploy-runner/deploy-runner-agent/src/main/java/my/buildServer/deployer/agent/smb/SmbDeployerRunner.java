package my.buildServer.deployer.agent.smb;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilderAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import my.buildServer.deployer.agent.base.BaseDeployerRunner;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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
