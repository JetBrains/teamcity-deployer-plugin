<%@ page import="jetbrains.buildServer.deployer.common.DeployerRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 2000-2021 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    FTP server: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_TARGET_URL%>"
                                            emptyValue="default"/></strong>
</div>

<div class="parameter">
    Username: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_USERNAME%>"
                                          emptyValue="none"/></strong>
</div>

<div class="parameter">
    Source: <strong><props:displayValue name="<%=DeployerRunnerConstants.PARAM_SOURCE_PATH%>"
                                        emptyValue="none"/></strong>
</div>