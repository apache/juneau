<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<#assign activeTab = "setup">
<#macro content>
    <div class="rm-setup">
        <div class="jc-card rm-setup-probes">
            <div class="jc-card-header">
                <h2>Probes</h2>
            </div>
            <div class="rm-probe-row" id="rm-probe-row">
                <#list inventory as p>
                <button type="button" class="rm-probe-pill rm-probe-pill-pending" data-probe-id="${p.id}" data-kind="${p.kind}">${p.label}</button>
                </#list>
            </div>
        </div>
        <div class="jc-card rm-setup-details">
            <div class="jc-card-header">
                <h2>Details</h2>
            </div>
            <div id="rm-probe-details">
                <p class="rm-probe-placeholder">Select a probe.</p>
            </div>
        </div>
    </div>
</#macro>
<#include "base.ftlh">
