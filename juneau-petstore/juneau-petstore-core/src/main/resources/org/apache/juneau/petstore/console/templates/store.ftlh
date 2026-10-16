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
--><@page tab="store" toolkit="views" init=["/console/dev/cards/petstore-cards.js"]>
<@card type="html" id="welcome" title="Welcome to the petstore">
<div class="jc-prose"><p>Browse pets, place orders and review what changed. Every page here is built from the same
Juneau console pieces your own app can use.</p>
<p data-slot="welcome-note">Signed in as a guest.</p></div>
</@card>
<@card type="datatables" id="stats" title="Recent orders">
{
  rowType: 'Order',
  dataMode: 'client',
  dataUrl: '/console/store/recent-orders',
  columns: [
    { key: 'id', label: 'Order', href: '/console/pets/{petId}/orders/{id}' },
    { key: 'petId', label: 'Pet', href: '/console/pets/{petId}/summary' },
    { key: 'shipDate', label: 'Ship date', render: { id: 'date' } },
    { key: 'status', label: 'Status', render: { id: 'pill' } }
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
  ] }
}
</@card>
<@card id="revenue" type="kpi" title="Revenue">{label:'Revenue today', dataUrl:'/console/store/kpi/revenue'}</@card>
<@card type="chart" id="orders-trend" title="Orders, last 30 days" src="/console/store/stats/daily">
{ kind: 'line', x: { field: 'day', label: 'Day' },
  series: [ { field: 'placed', label: 'Placed' }, { field: 'delivered', label: 'Delivered', tone: 'ok' } ],
  onClick: { href: '/console/orders?state=day({day})' } }
</@card>
<@card type="chart" id="by-species" title="Pets by species" src="/console/store/stats/species">
{ kind: 'donut', label: 'species', value: 'count', center: 'total' }
</@card>
<@card type="chart" id="by-status" title="Pets by status" src="/console/store/stats/status">
{ kind: 'bar', x: { field: 'status' }, series: [ { field: 'count', label: 'Pets' } ] }
</@card>
<@card type="chart" id="pets-map" title="Pets by species and status" src="/console/store/stats/pets" publishes="pets-map.click">
{ helper: { kind: 'treemap', groupBy: ['species', 'status'], label: 'name',
    color: { field: 'status', legend: [
      { value: 'AVAILABLE', label: 'Available', tone: 'ok' },
      { value: 'PENDING', label: 'Pending', tone: 'warning' },
      { value: 'SOLD', label: 'Sold' } ] },
    tileFields: ['id', 'name', 'species', 'status'] },
  on: { click: 'pets-map.click' },
  onClick: { action: { id: 'edit', label: 'Edit', form: '/console/pets/form?id={id}', present: 'dialog',
    endpoint: '/console/pets/{id}', method: 'PUT', onSuccess: 'redraw' } } }
</@card>
<@card type="html" id="hours" title="Store hours" src="/console/store/fragments/hours"/>
</@page>
