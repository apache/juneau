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

    <#assign hasRun = run??>
    <#-- App-local subtabs. Both panels stay in the DOM at all times; new-release.js just shows/hides them,
         so switching to Input never tears down or reconnects the Execution panel's two EventSources. When a
         run is active the Execution tab opens first (that's the working view); otherwise Input does. -->
    <div class="rm-subtabs" role="tablist">
        <button type="button" class="rm-subtab<#if !hasRun> active</#if>" role="tab" data-subtab="input" onclick="nrSubtab('input')">Input</button>
        <button type="button" class="rm-subtab<#if hasRun> active</#if>" role="tab" data-subtab="exec" onclick="nrSubtab('exec')">Execution</button>
    </div>

    <#-- ===================== INPUT SUBTAB: release metadata + narrative ===================== -->
    <section class="rm-subtab-panel" id="nr-panel-input" data-subtab="input"<#if hasRun> hidden</#if>>
        <div class="jc-card rm-form-card">
            <#if !hasRun>
            <h2>Start a new release</h2>
            <div class="rm-form-grid">
                <div class="rm-field">
                    <label for="nr-version">Version</label>
                    <input id="nr-version" type="text" placeholder="e.g. 9.2.1">
                </div>
                <div class="rm-field">
                    <label for="nr-devversion">Development version</label>
                    <input id="nr-devversion" type="text" placeholder="required if x.y.0 (e.g. 9.3.0-SNAPSHOT)">
                </div>
                <div class="rm-field">
                    <label for="nr-milestone">Milestone #</label>
                    <input id="nr-milestone" type="number" placeholder="auto-resolved from version">
                </div>
            </div>
            <div class="rm-field rm-mode-picker">
                <span class="rm-mode-picker-label">Mode</span>
                <label class="rm-mode-option" for="nr-mode-safe">
                    <input type="radio" name="nr-mode" value="SAFE" id="nr-mode-safe" checked>
                    Dry-run (SAFE)
                </label>
                <label class="rm-mode-option<#if !(liveCapable!false)> disabled</#if>" for="nr-mode-live">
                    <input type="radio" name="nr-mode" value="LIVE" id="nr-mode-live"<#if !(liveCapable!false)> disabled</#if>>
                    Actual (LIVE)
                </label>
                <#if !(liveCapable!false)>
                <p class="hint">Actual (LIVE) is disabled because this box was started in SAFE mode. Relaunch with <code>rm.mode=live</code> to enable it. LIVE runs still require a typed &lsquo;<em>version</em> LIVE&rsquo; arming phrase.</p>
                <#else>
                <p class="hint">Dry-run rehearses against the in-app Nexus mock. Actual (LIVE) hits real endpoints and still requires a typed &lsquo;<em>version</em> LIVE&rsquo; arming phrase per run.</p>
                </#if>
            </div>
            <#else>
            <h2>Release details</h2>
            <p class="hint">Narrative fields are optional and can be edited any time before each email is composed. Blank fields are omitted from the emails.</p>
            <div class="rm-form-grid">
                <div class="rm-field">
                    <label>Version</label>
                    <input type="text" value="${run.version}" readonly>
                </div>
                <div class="rm-field">
                    <label>Development version</label>
                    <input type="text" placeholder="&mdash;" value="<#if run.developmentVersion??>${run.developmentVersion}</#if>" readonly>
                </div>
                <div class="rm-field">
                    <label>Milestone #</label>
                    <input type="text" placeholder="&mdash;" value="<#if run.milestoneNumber??>${run.milestoneNumber}</#if>" readonly>
                </div>
                <div class="rm-field">
                    <label>Mode</label>
                    <input type="text" value="${(run.mode!'SAFE')}" readonly>
                </div>
            </div>
            </#if>

            <#-- The four optional narrative fields (shared IDs — only one of the two forms above ever renders). -->
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

    <#-- ===================== EXECUTION SUBTAB: step rail + detail/console ===================== -->
    <section class="rm-subtab-panel" id="nr-panel-exec" data-subtab="exec"<#if !hasRun> hidden</#if>>
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
                    <#if (mode!'SAFE') == 'LIVE'>
                        <span id="nr-arm-area">
                        <#if armed!false>
                            <span class="tag armed" title="This run is armed for live mutation">ARMED</span>
                        <#else>
                            <button class="jc-btn jc-btn-warning" onclick="nrArm()">Arm this run</button>
                        </#if>
                        </span>
                    </#if>
                    <button class="jc-btn jc-btn-danger" onclick="nrDropRc()">Drop RC</button>
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

        <!-- Step id -> {title, mutating} from the StepRegistry (page()'s "steps" attr), for the
             detail pane's title/mutating-tag rendering. Not RunState — that's already inline above. -->
        <script id="nr-step-meta" type="application/json">
        {
            <#list steps as reg>"${reg.id()}": {"title": "${reg.title()}", "mutating": ${reg.mutating()?c}}<#if reg?has_next>,</#if>
            </#list>
        }
        </script>
    </#if>
    </section>
</#macro>
<#include "base.ftlh">
