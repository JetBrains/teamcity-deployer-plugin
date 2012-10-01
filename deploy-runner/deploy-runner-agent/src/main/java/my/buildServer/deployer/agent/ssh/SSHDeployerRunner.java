package my.buildServer.deployer.agent.ssh;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import my.buildServer.deployer.agent.base.BaseDeployerRunner;
import my.buildServer.deployer.agent.ssh.scp.ScpProcessAdapter;
import my.buildServer.deployer.agent.ssh.sftp.SftpBuildProcessAdapter;
import my.buildServer.deployer.common.DeployerRunnerConstants;
import my.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:26
 */
public class SSHDeployerRunner extends BaseDeployerRunner {

    public SSHDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
        super(extensionHolder);
    }

    @Override
    protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                              @NotNull final String username,
                                              @NotNull final String password,
                                              @NotNull final String target,
                                              @NotNull final List<ArtifactsCollection> artifactsCollections) throws RunBuildException {
        final String transport = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_TRANSPORT);

        if (SSHRunnerConstants.TRANSPORT_SCP.equals(transport)) {
            return new ScpProcessAdapter(context, username, password, target, artifactsCollections);
        } else if (SSHRunnerConstants.TRANSPORT_SFTP.equals(transport)) {
            return new SftpBuildProcessAdapter(target, username, password, context, artifactsCollections);
        } else {
            throw new RunBuildException("Unknown ssh transport [" + transport + "]");
        }
    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new SSHDeployerRunnerInfo();
    }


}
