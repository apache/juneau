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
--><@page tab="pets" toolkit="views,datatables-buttons">
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
  columns: [
    { key: 'id', label: 'Id' },
    { key: 'name', label: 'Name', href: '/console/pets/{id}/summary' },
    { key: 'species', label: 'Species' },
    { key: 'status', label: 'Status', render: { id: 'pill' } },
    { key: 'price', label: 'Price (USD)', render: { id: 'decimal', meta: { places: 2 } } },
    { key: 'tags', label: 'Tags', defaultVisible: false }
  ],
  rowClassRules: [ { field: 'status', op: 'eq', value: 'SOLD', class: 'is-muted' } ],
  ribbon: [
    { type: 'optionGroup', id: 'status', title: 'Pet status', deselectable: true, default: 'available', options: [
      { id: 'available', title: 'Available', column: 'status', value: '$eq(AVAILABLE)' },
      { id: 'pending', title: 'Pending', column: 'status', value: '$eq(PENDING)' }
    ] },
    { type: 'optionGroup', id: 'species', title: 'Species', deselectable: true, default: 'mammals', options: [
      { id: 'mammals', title: 'Mammals', column: 'species', value: '$in(DOG,CAT,MOUSE,RABBIT)' },
      { id: 'others', title: 'Others', column: 'species', value: '$in(BIRD,FISH,SNAKE)' }
    ] },
    { type: 'dialog', id: 'add-pet', title: '+ Add pet', symbol: 'plus',
      form: '/console/pets/form', endpoint: '/console/pets', method: 'POST', onSuccess: 'redraw' },
    { type: 'export', buttons: [ 'copy', 'csv' ], optional: [ 'excel', 'pdf' ] },
    { type: 'refresh' }
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
