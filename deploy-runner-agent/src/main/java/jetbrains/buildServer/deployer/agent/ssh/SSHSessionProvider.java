/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.*;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.InternalPropertiesHolder;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.parameters.ProcessingResult;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class SSHSessionProvider {

  public static final String TEAMCITY_DEPLOYER_SSH_CONFIG_PATH = "teamcity.deployer.ssh.config.path";
  public static final String TEAMCITY_DEPLOYER_SSH_DEFAULT_KEY = "teamcity.deployer.ssh.default.key";

  private final Logger myLog = Logger.getLogger(this.getClass());
  @NotNull
  private final AgentRunningBuildSshKeyManager mySshKeyManager;
  private final BuildRunnerContext myContext;
  private final InternalPropertiesHolder myHolder;

  private Session mySession;
  private String myHost;
  private int myPort;
  private String myRemotePath;
  private final String myDefaultKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa";


  public SSHSessionProvider(@NotNull final BuildRunnerContext context,
                            @NotNull final InternalPropertiesHolder holder,
                            @NotNull final AgentRunningBuildSshKeyManager sshKeyManager) {
    mySshKeyManager = sshKeyManager;

    final String target = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_TARGET_URL);
    final String portStr = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_PORT);
    try {
      myPort = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      myPort = 22;
    }

    final int delimiterIndex = target.indexOf(':');
    if (delimiterIndex > 0) {
      myHost = target.substring(0, delimiterIndex);
      final String remotePath = target.substring(delimiterIndex + 1);

      myRemotePath = remotePath.trim().replaceAll("\\\\", "/");
      if (new File(myRemotePath).isAbsolute() && !myRemotePath.startsWith("/")) {
        myRemotePath = "/" + myRemotePath;
      }
    } else {
      myHost = target;
      myRemotePath = "";
    }

    myContext = context;
    myHolder = holder;
  }

  private Session createSession(@NotNull BuildRunnerContext context, @NotNull InternalPropertiesHolder holder) throws JSchException {
    final String username = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_USERNAME);
    final String password = context.getRunnerParameters().get(DeployerRunnerConstants.PARAM_PASSWORD);
    final String authMethod = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_AUTH_METHOD);

    JSch jsch = new JSch();
    JSch.setConfig("StrictHostKeyChecking", "no");

    myLog.debug("Initializing ssh session.");
    if (SSHRunnerConstants.AUTH_METHOD_DEFAULT_KEY.equals(authMethod)) {
      final String configPath = holder.getInternalProperty(TEAMCITY_DEPLOYER_SSH_CONFIG_PATH, System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "config");
      //noinspection ConstantConditions
      final File config = new File(configPath);
      if (config.exists()) {
        myLog.debug("Found config at [" + config.getAbsolutePath() + "], reading.");
        return initSessionSSHConfig(jsch, config);
      } else {
        final String keyPath = holder.getInternalProperty(TEAMCITY_DEPLOYER_SSH_DEFAULT_KEY, myDefaultKeyPath);
        //noinspection ConstantConditions
        final File keyFile = new File(keyPath);
        myLog.debug("Using keyfile at [" + keyFile.getAbsolutePath() + "], load.");
        return initSessionKeyFile(username, password, keyFile, jsch);
      }
    } else if (SSHRunnerConstants.AUTH_METHOD_CUSTOM_KEY.equals(authMethod)) {
      String keyFilePath = context.getRunnerParameters().get(SSHRunnerConstants.PARAM_KEYFILE);
      if (StringUtil.isEmpty(keyFilePath)) {
        keyFilePath = myDefaultKeyPath;
      }
      final File keyFile = FileUtil.resolvePath(context.getBuild().getCheckoutDirectory(), keyFilePath);
      myLog.debug("Using keyfile at [" + keyFile.getAbsolutePath() + "], load.");
      return initSessionKeyFile(username, password, keyFile, jsch);

    } else if (SSHRunnerConstants.AUTH_METHOD_SSH_AGENT.equals(authMethod)) {
      final ProcessingResult result = context.getParametersResolver().resolve("%env.SSH_AUTH_SOCK%");
      String socketPath = null;
      if (result.isFullyResolved()) {
        socketPath = result.getResult();
      }
      return initSessionSshAgent(username, socketPath, jsch);
    } else if (SSHRunnerConstants.AUTH_METHOD_UPLOADED_KEY.equals(authMethod)) {

      final String keyId = context.getRunnerParameters().get("teamcitySshKey");
      if (StringUtil.isEmptyOrSpaces(keyId)) {
        throw new JSchException("SSH key is not specified");
      }
      return initSessionUploadedKey(username, password, keyId, jsch);

    } else {
      myLog.debug("Using provided username/password");
      return initSessionUserPassword(username, password, jsch);
    }
  }

  private Session initSessionSSHConfig(JSch jsch, File config) throws JSchException {
    final String configPath = config.getAbsolutePath();
    try {
      final OpenSSHConfig sshConfig = OpenSSHConfig.parseFile(configPath);
      jsch.setConfigRepository(sshConfig);
      final Session session = jsch.getSession(myHost);
      session.setConfig("PreferredAuthentications", "publickey");
      return session;
    } catch (IOException e) {
      throw new JSchException("Error parsing ssh config file [" + configPath + "]", e);
    }
  }

  private Session initSessionUserPassword(String username, String password, JSch jsch) throws JSchException {
    final Session session = jsch.getSession(username, myHost, myPort);
    session.setPassword(password);
    session.setConfig("PreferredAuthentications", "password");
    return session;
  }

  private Session initSessionKeyFile(String username, String password, File keyFile, JSch jsch) throws JSchException {
    try {
      if (StringUtil.isNotEmpty(password)) {
        myLog.debug("Adding password");
        jsch.addIdentity(keyFile.getCanonicalPath(), password);
      } else {
        jsch.addIdentity(keyFile.getCanonicalPath());
      }
      final Session session = jsch.getSession(username, myHost, myPort);
      session.setConfig("PreferredAuthentications", "publickey");
      return session;
    } catch (IOException e) {
      throw new JSchException("Failed to use key file", e);
    }
  }

  private Session initSessionSshAgent(String username, String socketPath, JSch jsch) throws JSchException {
    final Session session = jsch.getSession(username, myHost, myPort);
    session.setConfig("PreferredAuthentications", "publickey");

    try {
      ConnectorFactory cf = ConnectorFactory.getDefault();
      cf.setUSocketPath(socketPath);
      Connector con = cf.createConnector();
      IdentityRepository irepo = new RemoteIdentityRepository(con);
      jsch.setIdentityRepository(irepo);
      return session;
    } catch (AgentProxyException e) {
      throw new JSchException("Failed to connect to ssh agent.", e);
    }
  }

  private Session initSessionUploadedKey(String username, String password, String keyId, JSch jsch) throws JSchException {
    final TeamCitySshKey key = mySshKeyManager.getKey(keyId);
    if (key == null) {
      throw new JSchException("Failed to load ssh key id=[" + keyId + "]");
    }

    try {
      jsch.addIdentity(key.getName(), key.getPrivateKey(), null, StringUtil.isNotEmpty(password) ? password.getBytes("UTF-8") : new byte[0]);
    } catch (UnsupportedEncodingException e) {
      myLog.error("Wrong encoding name", e);
    }
    final Session session = jsch.getSession(username, myHost, myPort);
    session.setConfig("PreferredAuthentications", "publickey");
    return session;
  }

  public String getRemotePath() {
    return myRemotePath;
  }

  public Session getSession() throws JSchException {
    if (mySession == null) {
      mySession = createSession(myContext, myHolder);
      mySession.connect();
    }
    return mySession;
  }

  public String getSessionString() {
    return (StringUtil.isNotEmpty(myRemotePath) ? "[" + myRemotePath + "] on " : "") + "host [" + myHost + ":" + myPort + "]";
  }

}
