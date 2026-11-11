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
--><@page tab="pets" toolkit="views,datatables-buttons,datatables-export">
<@card type="datatables" id="pets" title="Pets">
{
  rowType: 'Pet',
  dataMode: 'server',
  dataUrl: '/console/pets/query',
  columnConfig: true,
  urlState: { ids: {} },
  defaultOrder: [ { key: 'id', dir: 'asc' } ],
  pollIntervalMs: 30000,
  pausePollingWhileEditing: true,
  detail: {"mode":"bean","endpoint":"/console/pets/row-detail/pet/{id}","title":"{name}","columns":3,"labels":{"price":"Price (USD)","id":"Id","name":"Name","species":"Species","status":"Status","tags":"Tags"},"tabs":[{"id":"details","kind":"fields","title":"Details","fields":["id","name","species","status","price","tags"]},{"id":"orders","kind":"table","title":"Orders","nested":{"contractVersion":"2","parentScopeParam":"petId","view":{"id":"pets-orders","columns":[{"data":"id","title":"Order","render":{"id":"linked"},"href":"/console/pets/{petId}/orders/{id}"},{"data":"shipDate","title":"Ship date","render":{"id":"date"}},{"data":"status","title":"Status","render":{"id":"pill","meta":{"tones":{"PLACED":"info","APPROVED":"warning","DELIVERED":"success"}}}}]},"meta":{"contractVersion":"5","id":"pets-orders","dataUrl":"/console/pets/orders/rows","dataMode":"client","rowType":"Order","columns":[{"data":"id","title":"Order","render":{"id":"linked"},"href":"/console/pets/{petId}/orders/{id}"},{"data":"shipDate","title":"Ship date","render":{"id":"date"}},{"data":"status","title":"Status","render":{"id":"pill","meta":{"tones":{"PLACED":"info","APPROVED":"warning","DELIVERED":"success"}}}}]}},"count":"rows"},{"id":"history","kind":"table","title":"History","nested":{"contractVersion":"2","parentScopeParam":"petId","view":{"id":"pets-history","columns":[{"data":"at","title":"When","render":{"id":"timestamp"}},{"data":"actor","title":"Actor"},{"data":"action","title":"Action","render":{"id":"pill","meta":{"tones":{"CREATE":"success","UPDATE":"info","DELETE":"error","DISCARD":"warning","RESTOCK":"info"}}}},{"data":"detail","title":"Detail"}]},"meta":{"contractVersion":"5","id":"pets-history","dataUrl":"/console/pets/history/rows","dataMode":"client","rowType":"AuditEntry","columns":[{"data":"at","title":"When","render":{"id":"timestamp"}},{"data":"actor","title":"Actor"},{"data":"action","title":"Action","render":{"id":"pill","meta":{"tones":{"CREATE":"success","UPDATE":"info","DELETE":"error","DISCARD":"warning","RESTOCK":"info"}}}},{"data":"detail","title":"Detail"}]}},"count":"rows"}]},
  columns: [
    { key: 'id', label: 'Id' },
    { key: 'name', label: 'Name', href: '/console/pets/{id}/summary' },
    { key: 'species', label: 'Species', render: { id: 'pill', meta: { categorical: true, categories: { DOG: 1, CAT: 2, BIRD: 4, FISH: 6, SNAKE: 7, RABBIT: 9, MOUSE: 10 } } } },
    { key: 'status', label: 'Status', render: { id: 'pill', meta: { tones: { AVAILABLE: 'success', PENDING: 'warning', SOLD: 'neutral' } } } },
    { key: 'price', label: 'Price (USD)', render: { id: 'decimal', meta: { places: 2 } } },
    { key: 'tags', label: 'Tags', defaultVisible: false }
  ],
  rowClassRules: [ { field: 'status', op: 'eq', value: 'SOLD', class: 'is-muted' } ],
  ribbon: [
    { type: 'search', id: 'search', group: 'search' },
    { type: 'dialog', id: 'add-pet', title: '+ Add pet', symbol: 'plus', group: 'entity',
      form: '/console/pets/form', endpoint: '/console/pets', method: 'POST', onSuccess: 'redraw' },
    { type: 'optionGroup', id: 'status', title: 'Pet status', group: 'filter', deselectable: true, default: 'available', options: [
      { id: 'available', title: 'Available', symbol: 'circle_check', column: 'status', value: '$eq(AVAILABLE)' },
      { id: 'pending', title: 'Pending', symbol: 'hourglass', column: 'status', value: '$eq(PENDING)' }
    ] },
    { type: 'optionGroup', id: 'species', title: 'Species', group: 'filter', deselectable: true, default: 'mammals', options: [
      { id: 'mammals', title: 'Mammals', symbol: 'paw_print', column: 'species', value: '$in(DOG,CAT,MOUSE,RABBIT)' },
      { id: 'others', title: 'Others', symbol: 'shapes', column: 'species', value: '$in(BIRD,FISH,SNAKE)' }
    ] },
    { type: 'export', id: 'export', group: 'export', buttons: [ 'copy', 'csv' ], optional: [ 'excel', 'pdf' ] },
    { type: 'linkButton', id: 'link', group: 'export' },
    { type: 'collapseAll', id: 'collapse', group: 'viewing' },
    { type: 'columnSettings', id: 'columns', group: 'viewing' },
    { type: 'resetDefaults', id: 'reset', group: 'viewing' },
    { type: 'refresh', id: 'refresh', group: 'page' }
  ],
  rowActions: [
    { id: 'edit', label: 'Edit', form: '/console/pets/form?id={id}', present: 'dialog',
      endpoint: '/console/pets/{id}', method: 'PUT', onSuccess: 'mergeRow' },
    { id: 'delete', label: 'Delete', endpoint: '/console/pets/{id}', method: 'DELETE',
      confirm: "Delete '{name}'?", confirmLabel: 'Delete', tone: 'danger', onSuccess: 'redraw' }
  ],
  selection: { rowIdField: 'id', selectAll: true, scope: 'persistent', labelField: 'name',
    selectableWhen: [ { field: 'status', op: 'ne', value: 'SOLD', reason: 'Already sold.' } ] },
  bulk: { actions: [
    { id: 'sell', label: 'Mark sold', endpoint: '/console/pets/{id}/sell', method: 'POST', bulkMode: 'perRow',
      confirmTitle: 'Mark pets sold', confirm: 'Mark the selected pets sold?', onSuccess: 'redraw' }
  ] }
}
</@card>
</@page>
