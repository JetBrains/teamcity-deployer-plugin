/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package jetbrains.buildServer.deployer.server;

import jetbrains.buildServer.deployer.common.DeployerRunnerConstants;
import jetbrains.buildServer.deployer.common.SSHRunnerConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by Nikita.Skvortsov
 * Date: 12/18/12, 4:15 PM
 */
@Test
public class SSHExecRunTypeTest extends DeployerRunTypeTest {

  SSHExecRunType myRunType;

  @Override
  protected void createRunType(RunTypeRegistry registry, PluginDescriptor descriptor) {
    myRunType = new SSHExecRunType(registry, descriptor);
  }

  public void testGetDescription() throws Exception {
    assertEquals(myRunType.getDescription(), "Runner able to execute commands over SSH");
  }

  public void testDescribeEmptyParameters() throws Exception {
    //noinspection unchecked
    myRunType.describeParameters(Collections.EMPTY_MAP);
  }

  public void testValidParameters() throws Exception {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(DeployerRunnerConstants.PARAM_TARGET_URL, "targethost");
    parameters.put(SSHRunnerConstants.PARAM_AUTH_METHOD, SSHRunnerConstants.AUTH_METHOD_USERNAME_PWD);
    parameters.put(DeployerRunnerConstants.PARAM_USERNAME, "user");
    parameters.put(DeployerRunnerConstants.PARAM_PASSWORD, "password");
    parameters.put(SSHRunnerConstants.PARAM_COMMAND, "ls -la");

    final Collection<InvalidProperty> process = myRunType.getRunnerPropertiesProcessor().process(parameters);

    assertTrue(process.isEmpty());
  }
}
