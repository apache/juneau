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
            <h2>All Releases</h2>
        </div>

        <#-- Rich-view toolkit table + JSON sidecar, pre-serialized server-side (ReleaseRest.page via
             ViewTable.of) and inserted as trusted markup: ?no_esc suppresses this .ftlh's HTML auto-escaping
             so the emitted <table data-juneau-view>/<script> markup is not double-escaped. juneau-views.js
             hydrates it into a server-side DataTable + ribbon on DOMContentLoaded.
             TODO-445n: the toolkit now owns table-only horizontal scroll (juneau-views.css / the flex
             .dt-layout-cell), so the old .rm-table-scroll wrapper is removed — an outer overflow-x:auto here
             would clip the paging-pill menu (INV-2) and can produce two horizontal scrollbars. The
             ViewTable wrapper <div> now sits as a direct child of .jc-card and its data-juneau-layout="wide"
             stamp widens the card to full content width. -->
        ${viewTable?no_esc}
    </div>
</#macro>
<#include "base.ftlh">
