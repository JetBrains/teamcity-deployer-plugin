<%@ page import="jetbrains.buildServer.deployer.common.SSHRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Target url: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_HOST%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
  Username: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_USERNAME%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
  Password: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_PASSWORD%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
  Commands: <strong><props:displayValue name="<%=SSHRunnerConstants.PARAM_COMMAND%>" emptyValue="default"/></strong>
</div>