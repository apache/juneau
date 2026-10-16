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
--><@page tab="orders" toolkit="views">
<@card type="datatables" id="orders" title="Orders">
{
  rowType: 'Order',
  dataMode: 'server',
  dataUrl: '/console/orders/query',
  columnConfig: { tabs: [ 'columns', 'sort' ] },
  urlState: { day: {}, window: {} },
  defaultOrder: [ { key: 'id', dir: 'asc' } ],
  columns: [
    { key: 'id', label: 'Id' },
    { key: 'petId', label: 'Pet', href: '/console/pets/{petId}/summary' },
    { key: 'shipDate', label: 'Ship date', render: { id: 'date' } },
    { key: 'status', label: 'Status', render: { id: 'pill' } }
  ],
  ribbon: [
    { type: 'optionGroup', id: 'status', title: 'Order status', deselectable: true, options: [
      { id: 'placed', title: 'Placed', column: 'status', value: '$eq(PLACED)' },
      { id: 'approved', title: 'Approved', column: 'status', value: '$eq(APPROVED)' },
      { id: 'delivered', title: 'Delivered', column: 'status', value: '$eq(DELIVERED)' }
    ] },
    { type: 'dialog', id: 'new-order', title: '+ New order', symbol: 'plus',
      form: '/console/orders/new-form', endpoint: '/console/orders', method: 'POST', onSuccess: 'redraw' },
    { type: 'export', buttons: [ 'copy', 'csv' ], optional: [ 'excel', 'pdf' ] },
    { type: 'refresh' }
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
