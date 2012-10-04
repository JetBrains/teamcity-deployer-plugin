<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<l:settingsGroup title="Deployment Target">
    <tr>
        <th><label for="my.buildServer.deployer.targetUrl">Target URL: </label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter target url</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Deployment Credentials">
    <tr>
        <th><label for="my.buildServer.deployer.myUsername">Username:</label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_USERNAME%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter myUsername</span>
        </td>
    </tr>
    <tr>
        <th><label for="my.buildServer.deployer.myPassword">Password:</label></th>
        <td><props:passwordProperty name="<%=DeployerRunnerConstants.PARAM_PASSWORD%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter myPassword</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Deployment source">
    <tr>
        <th><label for="my.buildServer.deployer.sourcePath">Source directory: </label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter path to source</span>
        </td>
    </tr>
</l:settingsGroup>
