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
--><@page tab="orders" toolkit="views,datatables-buttons,datatables-export">
<@card type="datatables" id="orders" title="Orders">
{
  rowType: 'Order',
  dataMode: 'server',
  dataUrl: '/console/orders/query',
  columnConfig: { tabs: [ 'columns', 'sort' ] },
  urlState: { day: {}, window: {} },
  defaultOrder: [ { key: 'id', dir: 'asc' } ],
  detail: {"mode":"bean","endpoint":"/console/orders/row-detail/order/{id}","title":"Order {id}","columns":2,"labels":{"petId":"Pet","id":"Id","shipDate":"Ship date","status":"Status"},"tabs":[{"id":"details","kind":"fields","title":"Details","fields":["id","petId","shipDate","status"]},{"id":"pet","kind":"table","title":"Pet","nested":{"contractVersion":"2","parentScopeParam":"orderId","view":{"id":"order-pet","columns":[{"data":"id","title":"Id"},{"data":"name","title":"Name","render":{"id":"linked"},"href":"/console/pets/{id}/summary"},{"data":"species","title":"Species"},{"data":"status","title":"Status"},{"data":"price","title":"Price (USD)","render":{"id":"decimal","meta":{"places":2}}}]},"meta":{"contractVersion":"5","id":"order-pet","dataUrl":"/console/orders/pet/rows","dataMode":"client","rowType":"Pet","columns":[{"data":"id","title":"Id"},{"data":"name","title":"Name","render":{"id":"linked"},"href":"/console/pets/{id}/summary"},{"data":"species","title":"Species"},{"data":"status","title":"Status"},{"data":"price","title":"Price (USD)","render":{"id":"decimal","meta":{"places":2}}}]}},"count":"rows"}]},
  columns: [
    { key: 'id', label: 'Id', searchType: 'numeric' },
    { key: 'petId', label: 'Pet', href: '/console/pets/{petId}/summary', searchType: 'numeric' },
    { key: 'shipDate', label: 'Ship date', render: { id: 'date' }, searchType: 'text' },
    { key: 'status', label: 'Status', render: { id: 'pill', meta: { tones: { PLACED: 'info', APPROVED: 'warning', DELIVERED: 'success' } } }, searchType: 'enum' }
  ],
  ribbon: [
    { type: 'search', id: 'search', group: 'search' },
    { type: 'dialog', id: 'new-order', title: '+ New order', symbol: 'plus', group: 'entity',
      form: '/console/orders/new-form', endpoint: '/console/orders', method: 'POST', onSuccess: 'redraw' },
    { type: 'optionGroup', id: 'status', title: 'Order status', group: 'filter', deselectable: true, options: [
      { id: 'placed', title: 'Placed', symbol: 'shopping_cart', column: 'status', value: '$eq(PLACED)' },
      { id: 'approved', title: 'Approved', symbol: 'badge_check', column: 'status', value: '$eq(APPROVED)' },
      { id: 'delivered', title: 'Delivered', symbol: 'truck', column: 'status', value: '$eq(DELIVERED)' }
    ] },
    { type: 'export', id: 'export', group: 'export', buttons: [ 'copy', 'csv' ], optional: [ 'excel', 'pdf' ] },
    { type: 'linkButton', id: 'link', group: 'export' },
    { type: 'collapseAll', id: 'collapse', group: 'viewing' },
    { type: 'columnSettings', id: 'columns', group: 'viewing' },
    { type: 'resetDefaults', id: 'reset', group: 'viewing' },
    { type: 'refresh', id: 'refresh', group: 'page' }
  ],
  rowActions: [
    { id: 'approve', label: 'Approve', endpoint: '/console/orders/{id}/approve', method: 'POST', onSuccess: 'redraw',
      visibleWhen: [ { field: 'status', op: 'eq', value: 'PLACED' } ] },
    { id: 'deliver', label: 'Deliver', endpoint: '/console/orders/{id}/deliver', method: 'POST', onSuccess: 'redraw',
      visibleWhen: [ { field: 'status', op: 'eq', value: 'APPROVED' } ] },
    { id: 'cancel', label: 'Cancel', form: '/console/orders/{id}/cancel-form', present: 'dialog',
      endpoint: '/console/orders/{id}', method: 'DELETE', onSuccess: 'redraw' },
    { id: 'delete', label: 'Delete', endpoint: '/console/orders/{id}', method: 'DELETE',
      confirm: "Delete order '{id}'?", confirmLabel: 'Delete', tone: 'danger', confirmRenderer: 'typed', onSuccess: 'redraw' }
  ]
}
</@card>
</@page>
