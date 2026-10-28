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

<@page tab=navTab!'release' toolkit="terminal" css=assetUrl("/css/release.css") init=assetUrl("/js/release.js")>
    <div class="jc-page-header">
        <h1>Release</h1>
        <p class="jc-page-sub">Run <code>scripts/release.py</code>: cut a release candidate, hold the vote, promote.</p>
    </div>

    <div class="jc-card rl-head" id="rl-head" data-start-key="${startKey}">
        <div class="rl-summary" id="rl-summary">No release in progress.</div>
        <div class="rl-actions">
            <button type="button" class="jc-btn jc-btn-primary" id="rl-start" hidden>Start</button>
            <button type="button" class="jc-btn jc-btn-primary" id="rl-resume" hidden>Resume</button>
            <button type="button" class="jc-btn" id="rl-cancel" hidden>Cancel</button>
            <button type="button" class="jc-btn jc-btn-danger" id="rl-drop" hidden>Drop RC</button>
            <label class="rl-dry"><input type="checkbox" id="rl-dry-run"> Dry run</label>
        </div>
    </div>

    <div class="jc-card rl-pause" id="rl-pause" role="status" hidden></div>

    <div class="jc-card rl-terminal-card">
        <div class="rl-terminal-title" id="rl-terminal-title">No attempt selected</div>
        <div id="rl-terminal"></div>
    </div>

    <div class="jc-card">
        <h2 class="rl-history-title">History</h2>
        <table class="jc-table rl-history" id="rl-history">
            <thead><tr><th>#</th><th>Command</th><th>Origin</th><th>Started</th><th>Outcome</th><th></th></tr></thead>
            <tbody></tbody>
        </table>
    </div>

</@page>
