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
    <meta name="csrf-token" content="${csrfToken}">
    <script src="/js/csrf.js"></script>
    <link rel="icon" type="image/svg+xml" href="/img/oakleaf.svg">
    <@theme name="light-red"/>
    <link rel="stylesheet" href="${pageThemeCss}">
    <link rel="stylesheet" href="/juneau-console/chrome.css">
    <#list pageCss as href><link rel="stylesheet" href="${href}"></#list>
    <#list pageToolkitCss as href><link rel="stylesheet" href="${href}" data-toolkit-css></#list>
    <link rel="stylesheet" href="/css/chrome.css">
</head>
<body data-juneau-csrf="${csrfToken}" data-juneau-csrf-header="${csrfHeader}">
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

    <@navigation>
        <@node id="setup" label="Setup" href="/rest/setup"/>
        <@node id="releases" label="Releases" href="/rest/releases"/>
        <@node id="new" label="New Release" href="/rest/runs"/>
    </@navigation>

    <main class="jc-main">${pageBody}</main>

    <#list pageToolkitJs as src><script src="${src}" data-toolkit-js></script></#list>
    <#list pageInit as src><script src="${src}"></script></#list>
    <script src="/js/renderers.js"></script>
</body>
</html>
