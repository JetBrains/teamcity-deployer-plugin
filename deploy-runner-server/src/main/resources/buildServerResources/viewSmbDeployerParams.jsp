<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Target share: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>"
                                              emptyValue="default"/></strong>
</div>

<div class="parameter">
    Domain: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_DOMAIN%>" emptyValue="empty"/></strong>
</div>

<div class="parameter">
    Username: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_USERNAME%>"
                                          emptyValue="none"/></strong>
</div>

<div class="parameter">
    Source: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>"
                                        emptyValue="none"/></strong>
</div>