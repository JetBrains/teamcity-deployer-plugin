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

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.jcraft.jsch.JSch;
import jetbrains.buildServer.NetworkUtil;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.deployer.agent.BaseDeployerTest;
import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.filesystem.NativeFileSystemFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.security.PublicKey;
import java.util.*;

/**
 * Created by User
 * date: 29.07.13.
 */
public class BaseSSHTest extends BaseDeployerTest {

  private static final int SSH_DEFAULT_PORT = 15655;
  static final String HOST_ADDR = "127.0.0.1";
  final Map<String, String> myRunnerParams = new HashMap<String, String>();
  final Map<String, String> myInternalProperties = new HashMap<String, String>();

  final InternalPropertiesHolder myInternalPropertiesHolder = new InternalPropertiesHolder() {
    @Nullable
    @Override
    public String getInternalProperty(@NotNull String s, String s2) {
      return myInternalProperties.get(s) != null ? myInternalProperties.get(s) : s2;
    }
  };

  File myWorkingDir;
  private String myUsername = "testuser";
  private String myPassword = "testpassword";
  List<ArtifactsCollection> myArtifactsCollections;
  BuildRunnerContext myContext;
  File myPassphraselessKey;
  File myPrivateKey;
  File myRemoteDir = null;
  private String oldUserDir = null;
  private SshServer myServer;
  int testPort;

  AgentRunningBuildSshKeyManager mySshKeyManager;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    myRemoteDir = createTempDir();

    myServer = SshServer.setUpDefaultServer();
    testPort = NetworkUtil.getFreePort(SSH_DEFAULT_PORT);
    myServer.setPort(testPort);
    myServer.setCommandFactory(new ScpCommandFactory());
    myServer.setShellFactory(new ProcessShellFactory(new String[]{SystemInfo.isWindows ? "cmd" : "sh"}));
    myServer.setPasswordAuthenticator(new PasswordAuthenticator() {
      @Override
      public boolean authenticate(String username, String password, ServerSession session) {
        return myUsername.equals(username) && myPassword.equals(password);
      }
    });

    final File keyFile = getTestResource("hostkey.pem");
    myPrivateKey = getTestResource("tmp_rsa").getAbsoluteFile();
    myPassphraselessKey = getTestResource("passphraseless").getAbsoluteFile();

    myServer.setPublickeyAuthenticator(new PublickeyAuthenticator() {
      @Override
      public boolean authenticate(String username, PublicKey key, ServerSession session) {
        return true;
      }
    });
    myServer.setKeyPairProvider(new FileKeyPairProvider(new String[]{keyFile.getCanonicalPath()}));
    myServer.setFileSystemFactory(new NativeFileSystemFactory());
    myServer.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));

    myServer.start();

    Mockery mockeryCtx = new Mockery();
    myContext = mockeryCtx.mock(BuildRunnerContext.class);
    final AgentRunningBuild build = mockeryCtx.mock(AgentRunningBuild.class);
    final BuildProgressLogger logger = new NullBuildProgressLogger();
    myWorkingDir = createTempDir();

    mySshKeyManager = mockeryCtx.mock(AgentRunningBuildSshKeyManager.class);
    final TeamCitySshKey sshKey = new TeamCitySshKey("Name", FileUtil.loadFileBytes(myPrivateKey), false);

    mockeryCtx.checking(new Expectations() {{
      allowing(myContext).getWorkingDirectory();
      will(returnValue(myWorkingDir));
      allowing(myContext).getBuild();
      will(returnValue(build));
      allowing(myContext).getRunnerParameters();
      will(returnValue(myRunnerParams));
      allowing(build).getBuildLogger();
      will(returnValue(logger));
      allowing(build).getCheckoutDirectory();
      will(returnValue(myWorkingDir));
      allowing(mySshKeyManager).getKey("key_id_value");
      will(returnValue(sshKey));
    }});

    // need to change user.dir, so that NativeFileSystemFactory works inside temp directory
    oldUserDir = System.getProperty("user.dir");
    System.setProperty("user.dir", myRemoteDir.getAbsolutePath());

    myArtifactsCollections = new ArrayList<ArtifactsCollection>();


    myRunnerParams.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_USERNAME_PWD);
    myRunnerParams.put(DeployerRunnerConstants.PARAM_USERNAME, myUsername);
    myRunnerParams.put(DeployerRunnerConstants.PARAM_PASSWORD, myPassword);

    myRunnerParams.put(DeployerRunnerConstants.PARAM_TARGET_URL, HOST_ADDR);
    myRunnerParams.put(SSHRunnerConstants.PARAM_PORT, String.valueOf(testPort));

    // newer version of JSch has disabled some of the deprecated algorithms
    // but our SSH server in this test requires them, this is why we have to change JSch config so the JSch client could connect to the server
    String kex = JSch.getConfig("kex");
    JSch.setConfig("kex", kex + ",diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group1-sha1");

    for (String k: Arrays.asList("cipher.s2c","cipher.c2s")) {
      String val = JSch.getConfig(k);
      JSch.setConfig(k, val + ",aes128-cbc,aes192-cbc,aes256-cbc,3des-ctr,3des-cbc,blowfish-cbc");
    }

    String pubKeyVal = JSch.getConfig("PubkeyAcceptedKeyTypes");
    JSch.setConfig("PubkeyAcceptedKeyTypes", "ssh-rsa,rsa-sha2-256,rsa-sha2-512," + pubKeyVal);

    /* Uncomment to enable JSch debug logging
    JSch.setLogger(new Logger() {
      @Override
      public boolean isEnabled(int level) {
        return true;
      }

      @Override
      public void log(int level, String message) {
        System.out.println(message);
      }
    });
    */
  }

  @AfterMethod
  @Override
  public void tearDown() throws Exception {
    myServer.stop(true);
    System.setProperty("user.dir", oldUserDir);
    super.tearDown();
  }
}
