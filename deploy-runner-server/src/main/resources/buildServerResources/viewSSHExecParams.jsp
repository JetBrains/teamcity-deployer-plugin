<%@ page import="jetbrains.buildServer.deployer.common.SSHRunnerConstants" %>
<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Target host: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>"
                                             emptyValue="default"/></strong>
</div>

<div class="parameter">
    Target port: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_PORT%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
    Use pty: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_PTY%>" emptyValue="none"/></strong>
</div>

<div class="parameter">
    Username: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_USERNAME%>"
                                          emptyValue="none"/></strong>
</div>

<div class="parameter">
    Commands: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_COMMAND%>" emptyValue="none"/></strong>
</div>
