package jetbrains.buildServer.deployer.agent.ftp;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
import jetbrains.buildServer.deployer.common.FTPRunnerConstants;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class FtpDeployerRunner extends BaseDeployerRunner {

    public FtpDeployerRunner(@NotNull final ExtensionHolder extensionHolder) {
        super(extensionHolder);
    }


    @Override
    protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                              @NotNull final String username,
                                              @NotNull final String password,
                                              @NotNull final String target,
                                              @NotNull final List<ArtifactsCollection> artifactsCollections) throws RunBuildException {
        final Map<String,String> runnerParameters = context.getRunnerParameters();
        final String authMethod = runnerParameters.get(FTPRunnerConstants.PARAM_AUTH_METHOD);

        if ("USER_PWD".equals(authMethod)) {
            return new FtpBuildProcessAdapter(context, target, username, password, artifactsCollections);
        } else if ("ANONYMOUS".equals(authMethod)) {
            return new FtpBuildProcessAdapter(context, target, "anonymous", " ", artifactsCollections);
        } else {
            throw new RunBuildException("Unknown FTP authentication method: [" + authMethod + "]");
        }

    }

    @NotNull
    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new FtpDeployerRunnerInfo();
    }


}
