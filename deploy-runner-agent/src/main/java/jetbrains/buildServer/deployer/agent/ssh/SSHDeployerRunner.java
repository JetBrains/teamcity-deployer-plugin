package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.JSchException;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.InternalPropertiesHolder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
import jetbrains.buildServer.deployer.agent.ssh.scp.ScpProcessAdapter;
import jetbrains.buildServer.deployer.agent.ssh.sftp.SftpBuildProcessAdapter;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Kit
 * Date: 24.03.12 - 17:26
 */
public class SSHDeployerRunner extends BaseDeployerRunner {


    @NotNull
    private final InternalPropertiesHolder myInternalProperties;

    public SSHDeployerRunner(@NotNull final ExtensionHolder extensionHolder,
                             @NotNull final InternalPropertiesHolder holder) {
        super(extensionHolder);
        myInternalProperties = holder;
    }

    @Override
    protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                              @NotNull final String username,
                                              @NotNull final String password,
                                              @NotNull final String target,
                                              @NotNull final List<ArtifactsCollection> artifactsCollections) throws RunBuildException {

        final SSHSessionProvider provider;
        try {
            provider = new SSHSessionProvider(context, myInternalProperties);
        } catch (JSchException e) {
            throw new RunBuildException(e);
        }

        final String transport = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_TRANSPORT);
        if (SSHRunnerConstants.TRANSPORT_SCP.equals(transport)) {
            return new ScpProcessAdapter(context, artifactsCollections, provider);
        } else if (SSHRunnerConstants.TRANSPORT_SFTP.equals(transport)) {
            return new SftpBuildProcessAdapter(context, artifactsCollections, provider);
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
