<%@ page import="jetbrains.buildServer.deployer.common.CargoRunnerConstants" %>
<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<l:settingsGroup title="Deployment Target">
    <tr>
        <th><label for="jetbrains.buildServer.deployer.targetUrl">Target: <l:star/></label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>" className="longField"
                                maxlength="256"/>
            <span class="smallNote">Enter target container info. Use format: {hostname|IP}[:port]</span><span
                    class="error" id="error_jetbrains.buildServer.deployer.targetUrl"></span>
        </td>
    </tr>
    <tr>
        <th><label for="jetbrains.buildServer.deployer.container.type">Container type:</label></th>
        <td>
            <props:selectProperty name="<%=DeployerRunnerConstants.PARAM_CONTAINER_TYPE%>">
                <props:option value="tomcat8x"><c:out value="Tomcat 8.x"/></props:option>
                <props:option value="tomcat7x"><c:out value="Tomcat 7.x"/></props:option>
                <props:option value="tomcat6x"><c:out value="Tomcat 6.x"/></props:option>
                <props:option value="tomcat5x"><c:out value="Tomcat 5.x"/></props:option>
            </props:selectProperty>
            <span class="smallNote">
                Default "Manager" web app must be deployed to target Tomcat. User must have role "manager-script".
            </span>
        </td>
    </tr>
    <tr>
        <th><label for="jetbrains.buildServer.deployer.cargo.https">Secure connection:</label></th>
        <td>
            <props:checkboxProperty name="<%=CargoRunnerConstants.USE_HTTPS%>"/>Use HTTPS protocol
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Deployment Credentials">
    <tr>
        <th><label for="jetbrains.buildServer.deployer.username">Username:<l:star/></label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_USERNAME%>" className="longField"
                                maxlength="256"/>
            <span class="smallNote">Enter username. The user must have "manager-script" role assigned</span>
            <span class="error" id="error_jetbrains.buildServer.deployer.username"></span>
        </td>
    </tr>
    <tr>
        <th><label for="secure:jetbrains.buildServer.deployer.password">Password:<l:star/></label></th>
        <td><props:passwordProperty name="<%=DeployerRunnerConstants.PARAM_PASSWORD%>" className="longField"
                                    maxlength="256"/>
            <span class="smallNote">Enter password. Configuration parameters can be used</span>
            <span class="error" id="error_secure:jetbrains.buildServer.deployer.password"></span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Web Application Settings">
    <tr>
        <th><label for="jetbrains.buildServer.deployer.sourcePath">Path to WAR archive: <l:star/></label></th>
        <td><props:textProperty name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>" className="longField"
                                maxlength="256"/>
            <span class="smallNote">Path to WAR archive to deploy</span><span
                    class="error" id="error_jetbrains.buildServer.deployer.sourcePath"></span>
        </td>
    </tr>
</l:settingsGroup>
