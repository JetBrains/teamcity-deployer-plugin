<%@ page import="my.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<l:settingsGroup title="Deployment Target">
    <tr>
        <th><label for="my.buildServer.deployer.targetUrl">Target host: </label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter hostname or IP address</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Deployment Credentials">
    <tr>
        <th><label for="my.buildServer.deployer.myUsername">Username:</label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_USERNAME%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter username</span>
        </td>
    </tr>
    <tr>
        <th><label for="my.buildServer.deployer.myPassword">Password:</label></th>
        <td><props:passwordProperty name="<%=DeployerRunnerConstants.PARAM_PASSWORD%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter password</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Deployment source">
    <tr>
        <th><label for="my.buildServer.deployer.sourcePath">Artifacts path: </label></th>
        <td>
            <props:multilineProperty name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>" className="longField" cols="30" rows="4" expanded="true" linkTitle="Enter artifacts paths"/>
            <span class="smallNote">New line or comma separated paths to build artifacts. Ant-style wildcards like dir/**/*.zip and target directories like *.zip => winFiles,unix/distro.tgz => linuxFiles, where winFiles and linuxFiles are target directories are supported.</span>
        </td>
    </tr>
</l:settingsGroup>
