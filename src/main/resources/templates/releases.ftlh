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

<@page tab="releases" toolkit="views"
    css="/datatables/dataTables.dataTables.min.css,https://cdn.datatables.net/buttons/3.2.0/css/buttons.dataTables.min.css"
    init="/datatables/jquery.min.js,/datatables/dataTables.min.js,https://cdn.datatables.net/buttons/3.2.0/js/dataTables.buttons.min.js,https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js,https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/pdfmake.min.js,https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/vfs_fonts.min.js,https://cdn.datatables.net/buttons/3.2.0/js/buttons.html5.min.js,/js/releases-detail.js">
    <@card>
        <div class="jc-page-header">
            <h1>All Releases</h1>
            <p class="jc-page-sub">Every Apache Juneau release, from the current vote candidate back through GA history.</p>
        </div>
    </@card>
    <@card type="datatables" id="releases">
    {
        contractVersion: '1',
        layout: 'wide',
        view: {
            contractVersion: '4',
            id: 'releases',
            rowType: 'Release',
            dataMode: 'server',
            dataUrl: '/rest/releases/data',
            defaultOrder: [ { data: 'version', dir: 'desc' } ],
            columns: [
                { data: 'version', title: 'Version', orderable: true, searchable: true, className: 'version-cell', pinned: true },
                { data: 'rc', title: 'RC', orderable: true, searchable: true },
                { data: 'status', title: 'Status', orderable: true, searchable: true, render: { id: 'pill', meta: { field: 'status' } } },
                { data: 'stage', title: 'Stage', orderable: true, searchable: true, render: { id: 'pill', meta: { field: 'stage' } } },
                { data: 'voteCloses', title: 'Vote closes', orderable: true, searchable: true, render: { id: 'ts-zulu' }, formats: [ 'ts-zulu', 'datetime', 'date' ] },
                { data: 'released', title: 'Released', orderable: true, searchable: true, render: { id: 'date' }, formats: [ 'date', 'datetime', 'ts-zulu' ] },
                { data: 'githubReleaseUrl', title: 'GitHub', orderable: false, searchable: true, render: { id: 'linked' }, href: '{githubReleaseUrl}', defaultVisible: false },
                { data: 'milestoneUrl', title: 'Milestone', orderable: false, searchable: true, render: { id: 'linked' }, href: '{milestoneUrl}', defaultVisible: false }
            ],
            ribbon: [
                { type: 'columnSearchToggle', group: 'filters' },
                { type: 'option', id: 'dropped-only', title: 'Dropped only', group: 'filters', column: 'status', value: 'DROPPED', persist: true, symbol: 'filter_alt' },
                { type: 'export', buttons: [ 'copy', 'csv' ], optional: [ 'excel', 'pdf' ] },
                { type: 'refresh' }
            ],
            columnConfig: {}
        },
        detail: {
            contractVersion: '1',
            endpoint: '/rest/releases/expand/{id}',
            title: '{version}',
            region: { id: 'detail', type: 'row-detail', populate: 'releases-detail', dataUrl: '/rest/releases/expand/{id}', titleFields: [ 'version' ] }
        }
    }
    </@card>
</@page>
