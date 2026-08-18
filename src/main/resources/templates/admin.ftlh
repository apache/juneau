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

        <#-- TODO-399 Phase C: the composed tabs/sub-tabs page shell (PageTable.of(...) via AdminRest.page()),
             pre-serialized server-side and inserted as trusted markup: ?no_esc suppresses this .ftlh's HTML
             auto-escaping so the emitted [data-juneau-page]/<table data-juneau-view>/<script> markup is not
             double-escaped. juneau-pages.js hash-routes the tabs and lazy-inits each panel's DataTable on first
             activation via juneau-views.js's initTable. -->
        <div class="rm-table-scroll">
            ${pageTable?no_esc}
        </div>
    </div>
</#macro>
<#include "base.ftlh">
