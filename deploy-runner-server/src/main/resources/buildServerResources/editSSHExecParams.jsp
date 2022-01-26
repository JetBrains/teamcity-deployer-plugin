<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ page import="jetbrains.buildServer.deployer.common.SSHRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%--
  ~ Copyright 2000-2022 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<l:settingsGroup title="Deployment Target">
    <tr>
        <th><label for="jetbrains.buildServer.sshexec.host">Target: <l:star/></label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>" className="longField"
                                maxlength="256"/>
            <span class="smallNote">Enter hostname or IP address</span><span class="error"
                                                                             id="error_jetbrains.buildServer.deployer.targetUrl"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="jetbrains.buildServer.sshexec.port">Port: </label></th>
        <td><props:textProperty name="<%=SSHRunnerConstants.PARAM_PORT%>" className="longField" maxlength="256"/>
            <span class="smallNote">Optional. Default value: 22</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="jetbrains.buildServer.sshexec.pty">Use pty: </label></th>
        <td><props:textProperty name="<%=SSHRunnerConstants.PARAM_PTY%>" className="stringField" maxlength="256"/>
            <span class="smallNote">Optional. By default a pty will not be allocated</span>
        </td>
    </tr>
</l:settingsGroup>

<%@include file="sshCredentials.jspf" %>

<l:settingsGroup title="SSH Commands">
    <tr>
        <th><label for="jetbrains.buildServer.sshexec.command">Commands: <l:star/></label></th>
        <td>
            <props:multilineProperty name="<%=SSHRunnerConstants.PARAM_COMMAND%>" className="longField" rows="4"
                                     cols="30" expanded="true" linkTitle="Enter remote commands"/>
            <span class="smallNote">Enter newline delimited set of commands to run</span>
        </td>
    </tr>
</l:settingsGroup>
