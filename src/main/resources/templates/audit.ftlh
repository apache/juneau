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

<@page tab="audit" toolkit="views">
    <div class="jc-page-header">
        <h1>Audit</h1>
        <p class="jc-page-sub">What the release engine has recorded: every run's events, and every dropped release candidate.</p>
    </div>
    <@card type="datatables" id="audit">
    {
        rowType: 'AuditEntry',
        dataMode: 'server',
        dataUrl: '/rest/audit/query',
        defaultOrder: [ { data: 'run', dir: 'desc' } ],
        columns: [
            { data: 'run', title: 'Run', orderable: true, searchable: true, className: 'version-cell', pinned: true },
            { data: 'seq', title: '#', orderable: true, searchable: true },
            { data: 'kind', title: 'Kind', orderable: true, searchable: true },
            { data: 'at', title: 'When', orderable: true, searchable: true, render: { id: 'ts-zulu' }, formats: [ 'ts-zulu', 'datetime', 'date' ] },
            { data: 'subject', title: 'Subject', orderable: true, searchable: true },
            { data: 'status', title: 'Status', orderable: true, searchable: true },
            { data: 'detail', title: 'Detail', orderable: false, searchable: true }
        ],
        ribbon: [
            { type: 'export', buttons: [ 'copy', 'csv' ] },
            { type: 'refresh' }
        ],
        columnConfig: true,
        primary: true
    }
    </@card>
</@page>
