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
--><@page tab="store" toolkit="views,datatables-buttons,datatables-export" init=["/console/dev/cards/petstore-cards.js"]>
<@card type="html" id="welcome" title="Welcome to the petstore">
<div class="jc-prose"><p>Browse pets, place orders and review what changed. Every page here is built from the same
Juneau console pieces your own app can use.</p>
<p data-slot="welcome-note">Signed in as a guest.</p></div>
</@card>
<@card id="revenue" type="kpi" title="Revenue">{label:'Revenue today', dataUrl:'/console/store/kpi/revenue'}</@card>
<@card type="switcher" id="store-sw" style="slider">
{ initialView: 'recent',
  views: [
    { id: 'recent', label: 'Recent orders', card: { type: 'datatables', id: 'stats',
        rowType: 'Order',
        dataMode: 'client',
        dataUrl: '/console/store/recent-orders',
        columnConfig: true,
        ribbon: [
          { type: 'search', id: 'search', group: 'search' },
          { type: 'dialog', id: 'new-order', title: '+ New order', symbol: 'plus', group: 'entity',
            form: '/console/orders/new-form', endpoint: '/console/orders', method: 'POST', onSuccess: 'redraw' },
          { type: 'export', id: 'export', group: 'export', buttons: [ 'copy', 'csv' ], optional: [ 'excel', 'pdf' ] },
          { type: 'linkButton', id: 'link', group: 'export' },
          { type: 'collapseAll', id: 'collapse', group: 'viewing' },
          { type: 'columnSettings', id: 'columns', group: 'viewing' },
          { type: 'resetDefaults', id: 'reset', group: 'viewing' },
          { type: 'refresh', id: 'refresh', group: 'page' }
        ],
        detail: {"mode":"bean","endpoint":"/console/orders/row-detail/order/{id}","title":"Order {id}","labels":{"petId":"Pet","id":"Order","shipDate":"Ship date","status":"Status"},"tabs":[{"id":"details","kind":"fields","title":"Details","fields":["id","petId","shipDate","status"]},{"id":"pet","kind":"table","title":"Pet","nested":{"contractVersion":"2","parentScopeParam":"orderId","view":{"id":"order-pet","columns":[{"data":"id","title":"Id"},{"data":"name","title":"Name","render":{"id":"linked"},"href":"/console/pets/{id}/summary"},{"data":"species","title":"Species"},{"data":"status","title":"Status"},{"data":"price","title":"Price (USD)","render":{"id":"decimal","meta":{"places":2}}}]},"meta":{"contractVersion":"5","id":"order-pet","dataUrl":"/console/orders/pet/rows","dataMode":"client","rowType":"Pet","columns":[{"data":"id","title":"Id"},{"data":"name","title":"Name","render":{"id":"linked"},"href":"/console/pets/{id}/summary"},{"data":"species","title":"Species"},{"data":"status","title":"Status"},{"data":"price","title":"Price (USD)","render":{"id":"decimal","meta":{"places":2}}}]}},"count":"rows"}]},
        columns: [
          { key: 'id', label: 'Order', href: '/console/pets/{petId}/orders/{id}' },
          { key: 'petId', label: 'Pet', href: '/console/pets/{petId}/summary' },
          { key: 'shipDate', label: 'Ship date', render: { id: 'date' } },
          { key: 'status', label: 'Status', render: { id: 'pill', meta: { tones: { PLACED: 'info', APPROVED: 'warning', DELIVERED: 'success' } } } }
        ],
        quickStats: { id: 'store', items: [
          { id: 'available', label: 'Available', value: ${qsAvailable?c} },
          { id: 'pending', label: 'Pending', value: ${qsPending?c} },
          { id: 'sold', label: 'Sold', value: ${qsSold?c} },
          { id: 'orders', type: 'segmented', label: 'Orders', segments: [
            { label: 'Placed', count: ${qsOrdersPlaced?c} },
            { label: 'Approved', count: ${qsOrdersApproved?c}, tone: 'info' },
            { label: 'Delivered', count: ${qsOrdersDelivered?c}, tone: 'success' } ] },
<#if qsPetTotal gt 0>
          { id: 'inventory', type: 'meter', label: 'Inventory', value: ${qsAvailable?c}, max: ${qsPetTotal?c}, text: '${qsAvailable?c} of ${qsPetTotal?c}' }
<#else>
          { id: 'inventory', label: 'Inventory', value: '0 of 0' }
</#if>
        ] } } },
    { id: 'revenue', label: 'Revenue', initialSection: 'chart', sections: [
        { id: 'chart', label: 'Chart', card: { type: 'chart', id: 'revenue-chart', src: '/console/store/stats/revenue',
            kind: 'line', x: { field: 'day', label: 'Day' }, series: [ { field: 'revenue', label: 'Revenue' } ] } },
        { id: 'data', label: 'Data', card: { type: 'datatables', id: 'revenue-data', dataMode: 'client', dataUrl: '/console/store/stats/revenue',
            columns: [ { key: 'day', label: 'Day' }, { key: 'revenue', label: 'Revenue' } ] } } ] },
    { id: 'orders', label: 'Orders', initialSection: 'chart', sections: [
        { id: 'chart', label: 'Chart', card: { type: 'chart', id: 'orders-trend', src: '/console/store/stats/daily',
            kind: 'line', x: { field: 'day', label: 'Day' },
            series: [ { field: 'placed', label: 'Placed' }, { field: 'delivered', label: 'Delivered', tone: 'ok' } ],
            onClick: { href: '/console/orders?state=day({day})' } } },
        { id: 'data', label: 'Data', card: { type: 'datatables', id: 'orders-data', dataMode: 'client', dataUrl: '/console/store/stats/daily',
            columns: [ { key: 'day', label: 'Day' }, { key: 'placed', label: 'Placed' }, { key: 'delivered', label: 'Delivered' } ] } } ] },
    { id: 'pets', label: 'Pets', initialSection: 'species', sections: [
        { id: 'species', label: 'Species', card: { type: 'chart', id: 'by-species', src: '/console/store/stats/species', dataTab: true,
            kind: 'donut', label: 'species', value: 'count', center: 'total' } },
        { id: 'status', label: 'Status', card: { type: 'chart', id: 'by-status', src: '/console/store/stats/status', dataTab: true,
            kind: 'bar', x: { field: 'status' }, series: [ { field: 'count', label: 'Pets' } ] } },
        { id: 'treemap', label: 'Treemap', card: { type: 'chart', id: 'pets-map', src: '/console/store/stats/pets', dataTab: true,
            publishes: [ { topic: 'pets-map.click', retain: false } ], on: { click: 'pets-map.click' },
            helper: { kind: 'treemap', groupBy: ['species', 'status'], label: 'name',
              color: { field: 'status', legend: [
                { value: 'AVAILABLE', label: 'Available', tone: 'ok' },
                { value: 'PENDING', label: 'Pending', tone: 'warning' },
                { value: 'SOLD', label: 'Sold' } ] },
              tileFields: ['id', 'name', 'species', 'status'] },
            onClick: { action: { id: 'edit', label: 'Edit', form: '/console/pets/form?id={id}', present: 'dialog',
              endpoint: '/console/pets/{id}', method: 'PUT', onSuccess: 'redraw' } } } } ] },
    { id: 'hours', label: 'Store hours', card: { type: 'html', id: 'hours', title: 'Store hours', src: '/console/store/fragments/hours' } } ] }
</@card>
</@page>
