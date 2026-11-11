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
--><@page tab="pets/changes" toolkit="views,datatables-buttons">
<@card type="datatables" id="changes" title="Pending changes">
{
  rowType: 'PendingChange',
  dataMode: 'server',
  dataUrl: '/console/pets/changes/query',
  columnConfig: true,
  defaultOrder: [ { key: 'id', dir: 'desc' } ],
  detail: {"mode":"inline","title":"Change {id}","labels":{"id":"Change","summary":"Change summary","author":"Author","state":"State","message":"Message"},"tabs":[{"id":"details","kind":"fields","title":"Details","fields":["id","summary","author","state","message"]}]},
  columns: [
    { key: 'id', label: 'Change', searchType: 'numeric' },
    { key: 'summary', label: 'Change summary', searchType: 'text' },
    { key: 'author', label: 'Author', searchType: 'text' },
    { key: 'state', label: 'State', render: { id: 'pill', meta: { tones: { PENDING: 'warning', APPLIED: 'success', FAILED: 'error' } } }, searchType: 'enum' },
    { key: 'message', label: 'Message', searchType: 'text', defaultVisible: false }
  ],
  ribbon: [
    { type: 'search', id: 'search', group: 'search' },
    { type: 'option', id: 'pending-only', title: 'Pending only', symbol: 'hourglass', column: 'state', value: '$eq(PENDING)', default: true, group: 'filter' },
    { type: 'export', id: 'export', group: 'export', buttons: [ 'copy', 'csv' ] },
    { type: 'linkButton', id: 'link', group: 'export' },
    { type: 'collapseAll', id: 'collapse', group: 'viewing' },
    { type: 'columnSettings', id: 'columns', group: 'viewing' },
    { type: 'resetDefaults', id: 'reset', group: 'viewing' },
    { type: 'refresh', id: 'refresh', group: 'page' }
  ],
  rowActions: [
    { id: 'retry', label: 'Retry', endpoint: '/console/pets/changes/{id}/retry', method: 'POST', onSuccess: 'redraw',
      visibleWhen: [ { field: 'state', op: 'eq', value: 'FAILED' } ] }
  ],
  selection: { rowIdField: 'id', selectAll: true, scope: 'persistent', labelField: 'summary',
    selectableWhen: [ { field: 'state', op: 'eq', value: 'PENDING', reason: 'Only pending changes can be discarded or applied.' } ] },
  bulk: { actions: [
    { id: 'discard', label: 'Discard', endpoint: '/console/pets/changes/discard', method: 'POST', bulkMode: 'aggregate',
      confirmTitle: 'Discard changes', confirm: 'Discard the selected changes?', tone: 'danger', confirmRenderer: 'typed' },
    { id: 'apply', label: 'Apply', endpoint: '/console/pets/changes/{id}/apply', method: 'POST', bulkMode: 'perRow' }
  ] }
}
</@card>
</@page>
