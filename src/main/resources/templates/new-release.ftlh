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

<#assign activeTab = "new">
<#macro content>
    <#assign modeLc = (mode!'SAFE')?lower_case>
    <div class="rm-mode-banner ${modeLc}" data-mode="${mode!'SAFE'}">
        <span class="rm-mode-chip ${modeLc}">${mode!'SAFE'}</span>
        <#if (mode!'SAFE') == 'SAFE'>
            Simulate mode &mdash; every mutation is command-logged with zero side effects, and Nexus staging is routed to the in-app loopback mock.
        <#else>
            Live mode &mdash; mutations hit real endpoints. Each run must be individually armed before it can mutate.
        </#if>
    </div>
    <#if !(run??)>
        <div class="rm-card">
            <h2>Start a run</h2>
            <div class="row">
                <input id="nr-version" type="text" placeholder="version (e.g. 9.2.1)">
                <input id="nr-devversion" type="text" placeholder="developmentVersion (required if x.y.0)">
                <input id="nr-milestone" type="number" placeholder="milestone # (auto-resolved from version)">
                <button class="btn btn-primary" onclick="nrStart()">Start</button>
            </div>
        </div>
    <#else>
        <div class="rm-run-header">
            <div class="rm-run-header-top">
                <div class="rm-run-title-group">
                    <h2 class="rm-run-title">${run.version} &middot; RC${run.rc}</h2>
                    <span class="tag status ${run.status?lower_case?replace('_','-')}">${run.status}</span>
                </div>
                <div class="rm-run-actions">
                    <#if (mode!'SAFE') == 'LIVE'>
                        <#if armed!false>
                            <span class="tag armed" title="This run is armed for live mutation">ARMED</span>
                        <#else>
                            <button class="btn btn-warning" onclick="nrArm()">Arm this run</button>
                        </#if>
                    </#if>
                    <button class="btn btn-danger" onclick="nrDropRc()">Drop RC</button>
                </div>
            </div>
            <div class="rm-run-meta" style="margin-top:10px;">
                <span class="rm-meta-chip">Branch <b>${run.branch}</b></span>
                <#if run.milestoneNumber??><span class="rm-meta-chip">Milestone <b>#${run.milestoneNumber}</b></span></#if>
            </div>
        </div>

        <!-- Left column is the step list; right column shows the selected step's detail and console. -->
        <div class="rm-rail-layout" data-version="${run.version}" data-rc="${run.rc}" data-mode="${mode!'SAFE'}" data-armed="${(armed!false)?c}">
            <div class="rm-rail-col" id="nr-rail">
                <#list run.steps as s>
                <div class="rm-rail-item ${s.status?lower_case?replace('_','-')}" data-step="${s.id}"
                     onclick="nrSelect('${s.id}')">
                    <span class="rm-rail-num">${s?index + 1}</span>
                    <span class="rm-rail-name">${s.id}</span>
                    <span class="tag status ${s.status?lower_case?replace('_','-')}">${s.status}</span>
                </div>
                </#list>
            </div>
            <!-- Populated by nrSelect() in new-release.js — shows exactly one step's detail and console at a time. -->
            <div class="rm-detail-col" id="nr-detail">
                <p class="hint">Select a step to preview and run it.</p>
            </div>
        </div>

        <!-- Step id -> {title, mutating} from the StepRegistry (page()'s "steps" attr, Task 19), for the
             detail pane's title/mutating-tag rendering. Not RunState — that's already inline above. -->
        <script id="nr-step-meta" type="application/json">
        {
            <#list steps as reg>"${reg.id()}": {"title": "${reg.title()}", "mutating": ${reg.mutating()?c}}<#if reg?has_next>,</#if>
            </#list>
        }
        </script>
    </#if>
</#macro>
<#include "base.ftlh">
