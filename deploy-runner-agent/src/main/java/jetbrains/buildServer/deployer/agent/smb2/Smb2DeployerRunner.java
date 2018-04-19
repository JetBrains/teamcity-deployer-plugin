package jetbrains.buildServer.deployer.agent.smb2;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.plugins.beans.PluginDescriptor;
import jetbrains.buildServer.deployer.agent.base.BaseDeployerRunner;
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

public class Smb2DeployerRunner extends BaseDeployerRunner {

  private final File root;

  public Smb2DeployerRunner(@NotNull final ExtensionHolder extensionHolder,
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
      final File[] files = new File(root, "smb2Lib").listFiles();
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

      final ClassLoader smbjCL = new URLClassLoader(urls, getClass().getClassLoader());
      final Class smbBuildProcessClass = smbjCL.loadClass("jetbrains.buildServer.deployer.agent.smb.SMBJBuildProcessAdapter");

      final String domain;
      final String actualUsername;

      if (username.indexOf('\\') > -1) {
        domain = username.substring(0, username.indexOf('\\'));
        actualUsername = username.substring(username.indexOf('\\') + 1);
      } else {
        domain = "";
        actualUsername = username;
      }


      @SuppressWarnings("unchecked")
      final Constructor constructor = smbBuildProcessClass.getConstructor(BuildRunnerContext.class,
          String.class, String.class, String.class, String.class,
          List.class);
      return (BuildProcess) constructor.newInstance(context, actualUsername, password, domain, target, artifactsCollections);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new Smb2DeployerRunnerInfo();
  }


}
