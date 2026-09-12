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

<#assign activeTab = "admin">
<#macro content>
    <div class="jc-card">
        <div class="jc-card-header">
            <h2>Admin</h2>
        </div>

        <nav class="juneau-page-nav" aria-label="Admin">
            <div class="juneau-page-nav-sections">
                <a class="juneau-page-nav-section" href="/rest/admin/releases" aria-current="page">Admin</a>
            </div>
            <div class="juneau-page-nav-children">
                <a class="juneau-page-nav-child" href="/rest/admin/releases"<#if selectedChild == 'releases'> aria-current="page"</#if>>Releases</a>
                <a class="juneau-page-nav-child" href="/rest/admin/credentials"<#if selectedChild == 'credentials'> aria-current="page"</#if>>Credentials</a>
            </div>
        </nav>
        <div id="${slotId}"></div>
        <script type="application/json" id="rm-table-slot">${tableSlotJson?no_esc}</script>
    </div>
</#macro>
<#include "base.ftlh">
