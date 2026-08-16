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
    <div class="rm-card">
        <div class="rm-card-header">
            <h2>All Releases</h2>
        </div>

        <div class="rm-table-scroll">
            <table id="releases" class="rm-table">
                <thead>
                    <tr>
                        <th>Version</th><th>RC</th><th>Status</th><th>Stage</th>
                        <th>Vote closes</th><th>Released</th><th>Links</th>
                    </tr>
                </thead>
                <tbody>
                    <#list releases as r>
                    <tr>
                        <td class="version-cell">${r.version!'—'}</td>
                        <td class="rc-cell">${r.rc!'—'}</td>
                        <td><span class="tag status ${(r.status!'UNKNOWN')?lower_case}">${r.status!'—'}</span></td>
                        <td>
                            <#if !(r.stage??) || r.stage == '—'>
                                &mdash;
                            <#else>
                                <span class="tag stage ${r.stage?lower_case?replace(' ', '-')}">${r.stage}</span>
                            </#if>
                        </td>
                        <td>${r.voteCloses!'—'}</td>
                        <td>${r.released!'—'}</td>
                        <td class="links-cell">
                            <#if r.githubReleaseUrl??><a href="${r.githubReleaseUrl}">GitHub</a></#if>
                            <#if r.milestoneUrl??><a href="${r.milestoneUrl}">Milestone</a></#if>
                        </td>
                    </tr>
                    </#list>
                </tbody>
            </table>
        </div>
    </div>
</#macro>
<#include "base.ftlh">
