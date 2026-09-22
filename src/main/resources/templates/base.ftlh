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

  Thin document shell: <@console> emits the whole <!DOCTYPE html> document, its head cascade (including the CSRF
  meta and body attributes when the LoopbackBoundaryFilter published a token), and its body scaffold.  This chrome
  authors only the release-manager-specific pieces: the oakleaf mark (icon=) / tab icon (favicon=), the danger-pill
  token overrides on the stock light-red pack, the app-only head/tail assets, the header actions, the nav tree, the
  main slot, and the footer.  Child templates stay <@page> calls.
--><@console theme="light-red" icon="/img/oakleaf.svg" favicon="/img/oakleaf.svg" brand="Apache Juneau · Release Manager">
<@theme name="light-red">
    <#-- The app's pre-existing danger palette (the same colors .rm-mode-banner.live / .tag.armed / .pill.invalid
         already used).  Overrides the stock light-red pack's Bootstrap-maroon red so the FAILED status/stage pill
         resolves through the token system without changing how it looks.  --jc-tag-red-* is kept as an alias-free
         mirror for the .jc-badge[danger] / busy-avatar-status consumers that still read that triad. -->
    <@token name="--jc-pill-red-bg" value="#fdeceb"/>
    <@token name="--jc-pill-red-text" value="#c23934"/>
    <@token name="--jc-pill-red-border" value="#f3c6c2"/>
    <@token name="--jc-tag-red-bg" value="#fdeceb"/>
    <@token name="--jc-tag-red-text" value="#c23934"/>
    <@token name="--jc-tag-red-border" value="#f3c6c2"/>
</@theme>
<@head>
    <script src="/js/csrf.js"></script>
    <link rel="stylesheet" href="/css/chrome.css">
</@head>
<@actions>
    <button class="jc-icon-btn" title="Help" aria-label="Help">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><text x="12" y="16.5" text-anchor="middle" font-size="11" font-weight="700" fill="currentColor">?</text></svg>
    </button>
    <div class="jc-avatar" title="Release Manager">RM</div>
</@actions>
<@navigation>
    <@node id="setup" label="Setup" href="/rest/setup"/>
    <@node id="releases" label="Releases" href="/rest/releases"/>
    <@node id="new" label="New Release" href="/rest/runs"/>
</@navigation>
<@main/>
<@footer>Apache Juneau Release Manager — loopback tool for cutting Apache Juneau releases.</@footer>
<@scripts>
    <script src="/js/renderers.js"></script>
</@scripts>
</@console>
