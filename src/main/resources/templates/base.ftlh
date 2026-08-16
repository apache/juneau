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

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Apache Juneau · Release Manager</title>
    <#if activeTab?? && activeTab == 'releases'>
    <link rel="stylesheet" href="/datatables/dataTables.dataTables.min.css">
    </#if>
    <link rel="stylesheet" href="/css/chrome.css">
</head>
<body>
    <header class="rm-header">
        <div class="rm-brand">
            <div class="rm-brand-mark" role="img" aria-label="Apache Juneau"></div>
            <div class="rm-brand-title">Apache Juneau <span class="rm-brand-sep">&middot;</span> Release Manager</div>
        </div>
        <div class="rm-header-actions">
            <button class="rm-icon-btn" title="Help" aria-label="Help">
                <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><text x="12" y="16.5" text-anchor="middle" font-size="11" font-weight="700" fill="currentColor">?</text></svg>
            </button>
            <div class="rm-avatar" title="Release Manager">RM</div>
        </div>
    </header>

    <#-- Primary nav: real tabs, each pointing at its actual /rest/* route.
         Active tab is driven by the `activeTab` attribute each page template
         assigns before including this layout — no hardcoded per-page string
         compares here. -->
    <nav class="nav">
        <a class="nav-tab<#if activeTab?? && activeTab == 'home'> active</#if>" href="/rest/home">Home</a>
        <a class="nav-tab<#if activeTab?? && activeTab == 'credentials'> active</#if>" href="/rest/credentials">Credentials</a>
        <a class="nav-tab<#if activeTab?? && activeTab == 'releases'> active</#if>" href="/rest/releases">Releases</a>
        <a class="nav-tab<#if activeTab?? && activeTab == 'new'> active</#if>" href="/rest/runs">New Release</a>
    </nav>

    <main class="rm-main">
        <#-- child templates define the "content" macro, then <#include> this layout -->
        <@content/>
    </main>

    <#if activeTab?? && activeTab == 'releases'>
    <script src="/datatables/jquery.min.js"></script>
    <script src="/datatables/dataTables.min.js"></script>
    <script src="/js/releases.js"></script>
    </#if>
    <#if activeTab?? && activeTab == 'credentials'>
    <script src="/js/credentials.js"></script>
    </#if>
    <#if activeTab?? && activeTab == 'new'>
    <link rel="stylesheet" href="/css/new-release.css">
    <script src="/js/new-release.js"></script>
    </#if>
</body>
</html>
