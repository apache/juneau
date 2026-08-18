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

<#-- Shared timestamp renderer: an ISO value renders as a compact Zulu span (progressively enhanced by
     renderers.js into "YYYY-MM-DD HH:mmZ" with a native title tooltip showing local time), falling back
     to the raw ISO string if JS never runs. In scope for every child template that <#include>s this file. -->
<#macro ts value>
    <#if !value??>&mdash;<#else><span data-render="ts-zulu" data-value="${value}">${value}</span></#if>
</#macro>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Apache Juneau · Release Manager</title>
    <#-- The server-held CSRF token for this boot, required back on every state-changing request by the loopback
         write boundary (LoopbackBoundary). Supplied by ConsolePage.of(...) from the request attribute the
         boundary's filter sets; referenced with no FreeMarker default on purpose, so a page that forgets to go
         through ConsolePage fails to render instead of quietly shipping a UI whose writes all 403.

         This is a synchronizer token, NOT a double-submit cookie. Do not "simplify" it into one: cookies ignore
         the port, so any page on any other localhost:* could plant a value this app would read back and accept
         as its own. csrf.js reads it from here; nothing writes it to document.cookie. -->
    <meta name="csrf-token" content="${csrfToken}">
    <#-- Must load before any other script: it wraps window.fetch so every non-GET request carries the token
         above, rather than leaving each call site to remember. -->
    <script src="/js/csrf.js"></script>
    <link rel="icon" type="image/svg+xml" href="/img/oakleaf.svg">
    <#if activeTab?? && (activeTab == 'releases' || activeTab == 'admin')>
    <link rel="stylesheet" href="/datatables/dataTables.dataTables.min.css">
    <#-- DataTables Buttons extension (copy/csv export) — caller-provided per ASF category-A discipline; CDN
         coordinates per the toolkit design §6.11. Only the copy/csv cluster needs this; excel/pdf (JSZip/pdfmake)
         stay unloaded and are feature-detected off by juneau-ribbon.js. -->
    <link rel="stylesheet" href="https://cdn.datatables.net/buttons/3.2.0/css/buttons.dataTables.min.css">
    <#-- First-party toolkit base .tag chip stylesheet (neutral); the app's console-ui palette themes the same
         .tag.<domain>.<value> classes. This same stylesheet also carries the TODO-399 Phase C .jc-tab/.jc-panel
         page-shell base rules the Admin tab's juneau-pages.js runtime relies on. -->
    <link rel="stylesheet" href="${viewsCssUrl!'/rest/releases/juneau-views.css'}">
    </#if>
    <link rel="stylesheet" href="/juneau-console/chrome.css">
    <link rel="stylesheet" href="/css/chrome.css">
</head>
<body>
    <header class="jc-header">
        <div class="jc-brand">
            <div class="jc-logo" role="img" aria-label="Apache Juneau"></div>
            <div class="jc-brand-title">Apache Juneau <span class="jc-brand-sep">&middot;</span> Release Manager</div>
        </div>
        <div class="jc-header-actions">
            <button class="jc-icon-btn" title="Help" aria-label="Help">
                <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><text x="12" y="16.5" text-anchor="middle" font-size="11" font-weight="700" fill="currentColor">?</text></svg>
            </button>
            <div class="jc-avatar" title="Release Manager">RM</div>
        </div>
    </header>

    <#-- Primary nav: real tabs, each pointing at its actual /rest/* route.
         Active tab is driven by the `activeTab` attribute each page template
         assigns before including this layout — no hardcoded per-page string
         compares here. -->
    <nav class="jc-nav">
        <a class="jc-nav-tab<#if activeTab?? && activeTab == 'home'> active</#if>" href="/rest/home">Home</a>
        <a class="jc-nav-tab<#if activeTab?? && activeTab == 'credentials'> active</#if>" href="/rest/credentials">Credentials</a>
        <a class="jc-nav-tab<#if activeTab?? && activeTab == 'releases'> active</#if>" href="/rest/releases">Releases</a>
        <a class="jc-nav-tab<#if activeTab?? && activeTab == 'new'> active</#if>" href="/rest/runs">New Release</a>
        <a class="jc-nav-tab<#if activeTab?? && activeTab == 'admin'> active</#if>" href="/rest/admin">Admin</a>
    </nav>

    <main class="jc-main">
        <#-- child templates define the "content" macro, then <#include> this layout -->
        <@content/>
    </main>

    <#if activeTab?? && (activeTab == 'releases' || activeTab == 'admin')>
    <#-- Caller-provided libs: self-hosted jQuery + DataTables, plus the DataTables Buttons extension (copy/csv)
         from CDN. Then the first-party toolkit runtime in load order renders -> icons -> ribbon -> views;
         juneau-views.js scans table[data-juneau-view] on DOMContentLoaded and hydrates the server-side table +
         ribbon. juneau-icons.js MUST load before juneau-ribbon.js: the ribbon/paging-pill buttons resolve their
         glyphs from the icon registry (window.JuneauViews.icons) at build time, so the registry must already
         exist when juneau-ribbon.js/juneau-views.js render them. The old hand-rolled /js/releases.js DataTable
         init is retired — the toolkit owns initialization now. -->
    <script src="/datatables/jquery.min.js"></script>
    <script src="/datatables/dataTables.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/3.2.0/js/dataTables.buttons.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/3.2.0/js/buttons.html5.min.js"></script>
    <script src="${rendersJsUrl!'/rest/releases/juneau-renders.js'}"></script>
    <script src="${iconsJsUrl!'/rest/releases/juneau-icons.js'}"></script>
    <script src="${ribbonJsUrl!'/rest/releases/juneau-ribbon.js'}"></script>
    <script src="${viewsJsUrl!'/rest/releases/juneau-views.js'}"></script>
    </#if>
    <#if activeTab?? && activeTab == 'admin'>
    <#-- TODO-399 Phase C: the opt-in tabs/sub-tabs page runtime — only the Admin tab's composed PageDef page loads
         this; the single-view Releases tab above does not. Must load after juneau-views.js (it calls the
         now-public NS.init.initTable to lazy-init a panel's DataTable on first activation). -->
    <script src="${pagesJsUrl!'/rest/admin/juneau-pages.js'}"></script>
    </#if>
    <#if activeTab?? && activeTab == 'credentials'>
    <script src="/js/credentials.js"></script>
    </#if>
    <#if activeTab?? && activeTab == 'new'>
    <link rel="stylesheet" href="/css/new-release.css">
    <script src="/js/new-release.js"></script>
    </#if>
    <script src="/js/renderers.js"></script>
</body>
</html>
