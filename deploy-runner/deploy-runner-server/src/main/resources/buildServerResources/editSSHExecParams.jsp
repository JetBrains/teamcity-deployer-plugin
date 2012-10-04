<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ page import="jetbrains.buildServer.deployer.common.SSHRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<l:settingsGroup title="Deployment Target">
    <tr>
        <th><label for="my.buildServer.sshexec.host">Hostname: </label></th>
        <td><props:textProperty name="<%=SSHRunnerConstants.PARAM_HOST%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter hostname</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Deployment Credentials">
    <tr>
        <th><label for="my.buildServer.sshexec.username">Username:</label></th>
        <td><props:textProperty name="<%=SSHRunnerConstants.PARAM_USERNAME%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter username</span>
        </td>
    </tr>
    <tr>
        <th><label for="my.buildServer.sshexec.password">Password:</label></th>
        <td><props:passwordProperty name="<%=SSHRunnerConstants.PARAM_PASSWORD%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter password</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Deployment source">
    <tr>
        <th><label for="my.buildServer.sshexec.command">Commands: </label></th>
        <td>
            <props:textProperty name="<%=SSHRunnerConstants.PARAM_COMMAND%>"  className="longField" expandable="true"/>
            <span class="smallNote">Enter newline delimited set of commands to run</span>
        </td>
    </tr>
</l:settingsGroup>
