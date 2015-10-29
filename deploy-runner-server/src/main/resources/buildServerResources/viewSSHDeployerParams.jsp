<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ page import="jetbrains.buildServer.deployer.common.SSHRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="runnerConst" scope="request" class="jetbrains.buildServer.deployer.common.SSHRunnerConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Target host: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>"
                                             emptyValue="default"/></strong>
</div>

<div class="parameter">
    Target port: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_PORT%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
    Username: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_USERNAME%>"
                                          emptyValue="none"/></strong>
</div>

<div class="parameter">
    Transport: <strong><c:forEach var="type" items="${runnerConst.transportTypeValues}"><c:if
        test="${type.key == propertiesBean.properties[runnerConst.transportType]}"><c:out value="${type.value}"/></c:if></c:forEach></strong>
</div>

<div class="parameter">
    Source: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>"
                                        emptyValue="none"/></strong>
</div>