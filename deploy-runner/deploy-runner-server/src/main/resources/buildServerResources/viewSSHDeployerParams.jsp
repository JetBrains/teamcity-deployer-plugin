<%@ page import="my.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Target url: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
  Username: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_USERNAME%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
  Password: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_PASSWORD%>" emptyValue="default"/></strong>
</div>

<div class="parameter">
  Source: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>" emptyValue="default"/></strong>
</div>