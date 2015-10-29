package jetbrains.buildServer.deployer.agent.smb;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.plugins.beans.PluginDescriptor;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SMBRunnerConstants;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public class SmbDeployerRunner extends BaseDeployerRunner {

  private final File root;

  public SmbDeployerRunner(@NotNull final ExtensionHolder extensionHolder,
                           @NotNull final PluginDescriptor pluginDescriptor) {
    super(extensionHolder);
    root = pluginDescriptor.getPluginRoot();
  }


  @Override
  protected BuildProcess getDeployerProcess(@NotNull final BuildRunnerContext context,
                                            @NotNull final String username,
                                            @NotNull final String password,
                                            @NotNull final String target,
                                            @NotNull final List<ArtifactsCollection> artifactsCollections) {

    try {
      final File[] files = new File(root, "smbLib").listFiles();
      final URL[] urls = CollectionsUtil.convertCollection(Arrays.asList(files), new Converter<URL, File>() {
        @Override
        public URL createFrom(@NotNull File file) {
          try {
            return file.toURI().toURL();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }).toArray(new URL[files.length]);

      final ClassLoader jcifsCL = new URLClassLoader(urls, getClass().getClassLoader());
      final Class smbBuildProcessClass = jcifsCL.loadClass("jetbrains.buildServer.deployer.agent.smb.SMBBuildProcessAdapter");

      final String domain = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_DOMAIN);
      final boolean dnsOnly = Boolean.valueOf(context.getRunnerParameters().get(SMBRunnerConstants.DNS_ONLY_NAME_RESOLUTION));

      final Constructor constructor = smbBuildProcessClass.getConstructor(BuildRunnerContext.class,
          String.class, String.class, String.class, String.class,
          List.class, boolean.class);
      return (BuildProcess) constructor.newInstance(context, username, password, domain, target, artifactsCollections, dnsOnly);

      // return new SMBBuildProcessAdapter(context, username, password, domain, target, artifactsCollections, dnsOnly);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new SmbDeployerRunnerInfo();
  }


}
