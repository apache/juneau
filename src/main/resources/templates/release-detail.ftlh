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

<#assign activeTab = "releases">
<#macro content>
    <div class="jc-card">
        <div class="jc-card-header">
            <h2>Release ${release.version!'—'}</h2>
            <a href="/rest/releases">&larr; All releases</a>
        </div>

        <dl class="rm-detail-list">
            <dt>Version</dt><dd>${release.version!'—'}</dd>
            <dt>RC</dt><dd>${release.rc!'—'}</dd>
            <dt>Status</dt><dd><span class="tag status ${(release.status!'UNKNOWN')?lower_case}">${release.status!'—'}</span></dd>
            <dt>Stage</dt>
            <dd>
                <#if !(release.stage??) || release.stage == '—'>
                    &mdash;
                <#else>
                    <span class="tag stage ${release.stage?lower_case?replace(' ', '-')}">${release.stage}</span>
                </#if>
            </dd>
            <dt>Vote closes</dt><dd><@ts value=release.voteCloses/></dd>
            <dt>Released</dt><dd>${release.released!'—'}</dd>
            <dt>Links</dt>
            <dd>
                <#if release.githubReleaseUrl??><a href="${release.githubReleaseUrl}">GitHub</a></#if>
                <#if release.milestoneUrl??><a href="${release.milestoneUrl}">Milestone</a></#if>
                <#if !(release.githubReleaseUrl??) && !(release.milestoneUrl??)>&mdash;</#if>
            </dd>
        </dl>
    </div>
</#macro>
<#include "base.ftlh">
