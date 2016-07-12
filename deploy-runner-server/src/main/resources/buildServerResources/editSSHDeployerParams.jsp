<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ page import="jetbrains.buildServer.deployer.common.SSHRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="runnerConst" scope="request" class="jetbrains.buildServer.deployer.common.SSHRunnerConstants"/>

<l:settingsGroup title="Deployment Target">
    <tr>
        <th><label for="jetbrains.buildServer.deployer.targetUrl">Target: <l:star/></label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>" className="longField"
                                maxlength="256"/>
            <span class="smallNote">Enter target url in form {hostname|ip_address}[:path/to/target/folder]</span><span
                    class="error" id="error_jetbrains.buildServer.deployer.targetUrl"></span>
        </td>
    </tr>

    <tr class="advancedSetting">
        <th><label for="jetbrains.buildServer.deployer.ssh.transport">Transport protocol: </label></th>
        <td>
            <props:selectProperty name="<%=SSHRunnerConstants.PARAM_TRANSPORT%>">
                <c:forEach var="type" items="${runnerConst.transportTypeValues}">
                    <props:option value="${type.key}"><c:out value="${type.value}"/></props:option>
                </c:forEach>
            </props:selectProperty>
            <span class="smallNote">Select SSH transfer protocol to use</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="jetbrains.buildServer.sshexec.port">Port: </label></th>
        <td><props:textProperty name="<%=SSHRunnerConstants.PARAM_PORT%>" className="longField" maxlength="256"/>
            <span class="smallNote">Optional. Default value: 22</span>
        </td>
    </tr>
</l:settingsGroup>

<%@include file="sshCredentials.jspf" %>

<l:settingsGroup title="Deployment Source">
    <tr>
        <th><label for="jetbrains.buildServer.deployer.sourcePath">Paths to sources: <l:star/></label></th>
        <td>
            <props:multilineProperty name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>" className="longField"
                                     cols="30" rows="4" expanded="true"
                                     linkTitle="Enter paths to sources for deployment"/>
            <span class="smallNote">Newline- or comma-separated paths to files/directories to be deployed. Ant-style wildcards like dir/**/*.zip and target directories like *.zip => winFiles,unix/distro.tgz => linuxFiles, where winFiles and linuxFiles are target directories, are supported.
            <bs:help file="Configuring+General+Settings" anchor="artifactPaths"/></span><span
                class="error" id="error_jetbrains.buildServer.deployer.sourcePath"></span>
        </td>
    </tr>
</l:settingsGroup>
