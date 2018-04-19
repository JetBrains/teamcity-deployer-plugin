package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

public class Smb2DeployerRunType extends SmbDeployerRunType {

  public Smb2DeployerRunType(@NotNull final RunTypeRegistry registry,
                             @NotNull final PluginDescriptor descriptor) {
    super(registry, descriptor);
  }

  @NotNull
  @Override
  public String getType() {
    return DeployerRunnerConstants.SMB2_RUN_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "SMB v2 Upload";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Deploys files/directories via SMB v2 (Windows share)";
  }
}
