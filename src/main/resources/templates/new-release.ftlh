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

<#assign _navTab = navTab!'new/input'>
<#assign _subtab = subtab!'input'>
<#assign hasRun = run??>
<@page tab=_navTab css="/css/new-release.css" init="/js/new-release.js">
    <div class="jc-page-header">
        <h1>New Release</h1>
        <p class="jc-page-sub">Cut an Apache Juneau release candidate, from input through vote and publish.</p>
    </div>

    <#if _subtab == "exec">
    <section class="rm-subtab-panel" id="nr-panel-exec" data-subtab="exec">
    <#if !hasRun>
        <div class="jc-card">
            <p class="hint">No active run yet. Fill in the Input tab and click Start to begin.</p>
        </div>
    <#else>
        <div class="rm-run-header">
            <div class="rm-run-header-top">
                <div class="rm-run-title-group">
                    <h2 class="rm-run-title" id="nr-run-title">${run.version} &middot; RC${run.rc}</h2>
                    <span class="tag status ${run.status?lower_case?replace('_','-')}" id="nr-run-status">${run.status}</span>
                </div>
                <div class="rm-run-actions">
                    <button class="jc-btn jc-btn-danger" onclick="nrDropRc()">Drop RC</button>
                </div>
            </div>
            <div class="rm-run-meta" style="margin-top:10px;">
                <span class="rm-meta-chip">Branch <b>${run.branch}</b></span>
            </div>
        </div>

        <div class="rm-rail-layout" data-version="${run.version}" data-rc="${run.rc}">
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
            <div class="rm-detail-col" id="nr-detail">
                <p class="hint">Select a step to run it.</p>
            </div>
        </div>

        <script id="nr-step-meta" type="application/json">${stepMeta?no_esc}</script>
    </#if>
    </section>
    <#else>
    <section class="rm-subtab-panel" id="nr-panel-input" data-subtab="input">
        <div class="jc-card rm-form-card"<#if hasRun> data-run-version="${run.version}"</#if>>
            <#if !hasRun>
            <h2>Start a new release</h2>
            <#else>
            <h2>Release details</h2>
            <p class="hint">These fields can be edited any time before each email is composed. Blank narrative fields are omitted from the emails.</p>
            </#if>
            <div class="rm-form-grid">
                <div class="rm-field">
                    <label for="nr-version">Version</label>
                    <input id="nr-version" type="text" placeholder="e.g. 9.2.1" value="<#if hasRun>${run.version}</#if>">
                </div>
                <div class="rm-field">
                    <label for="nr-devversion">Development version</label>
                    <input id="nr-devversion" type="text" placeholder="required if x.y.0 (e.g. 9.3.0-SNAPSHOT)" value="<#if hasRun && run.developmentVersion??>${run.developmentVersion}</#if>">
                </div>
                <div class="rm-field">
                    <label for="nr-rc">Release candidate</label>
                    <input id="nr-rc" type="number" min="1" step="1" placeholder="1" value="<#if hasRun>${run.rc}<#else>1</#if>">
                </div>
            </div>

            <div class="rm-field">
                <label for="nr-releaseSummary">Release summary <span class="rm-field-opt">optional</span></label>
                <input id="nr-releaseSummary" type="text" placeholder="Short &lsquo;why we&rsquo;re releasing&rsquo; line &mdash; used in the PROPOSE and ANNOUNCEMENT emails" value="<#if hasRun && run.releaseSummary??>${run.releaseSummary}</#if>">
            </div>
            <div class="rm-field">
                <label for="nr-highlights">Highlights <span class="rm-field-opt">optional</span></label>
                <textarea id="nr-highlights" rows="4" placeholder="What&rsquo;s added &mdash; one bullet per line; used in the VOTE and ANNOUNCEMENT emails"><#if hasRun && run.highlights??>${run.highlights}</#if></textarea>
            </div>
            <div class="rm-field">
                <label for="nr-knownIssues">Known issues <span class="rm-field-opt">optional</span></label>
                <textarea id="nr-knownIssues" rows="3" placeholder="Known issues &mdash; one per line; used in the VOTE and ANNOUNCEMENT emails"><#if hasRun && run.knownIssues??>${run.knownIssues}</#if></textarea>
            </div>
            <div class="rm-field">
                <label for="nr-acknowledgements">Acknowledgements <span class="rm-field-opt">optional</span></label>
                <textarea id="nr-acknowledgements" rows="3" placeholder="Thanks / acknowledgements &mdash; used in the RESULT and ANNOUNCEMENT emails"><#if hasRun && run.acknowledgements??>${run.acknowledgements}</#if></textarea>
            </div>

            <div class="rm-form-actions">
                <#if !hasRun>
                <button class="jc-btn jc-btn-primary" onclick="nrStart()">Start</button>
                <#else>
                <button class="jc-btn jc-btn-primary" onclick="nrSaveDetails()">Save details</button>
                <span id="nr-details-msg" class="rm-step-note"></span>
                </#if>
            </div>
        </div>
    </section>
    </#if>
</@page>
