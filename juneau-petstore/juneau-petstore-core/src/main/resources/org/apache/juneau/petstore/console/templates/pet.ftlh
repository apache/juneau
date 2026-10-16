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
--><#-- P4: one pet.  The page appends its own nodes under "pets" (spec 3.2): the pet (p<id>), its four tabs, and the
     selected order (o<id>) at depth 4.  Node ids must start with a letter, so the numeric ids are prefixed. -->
<@page tab=activeNav toolkit="views">
<@node under="pets" id="p${pet.id?c}" label=pet.name href="/console/pets/${pet.id?c}/summary">
<@node id="summary" label="Summary" href="/console/pets/${pet.id?c}/summary"/>
<@node id="photo" label="Photo" href="/console/pets/${pet.id?c}/photo"/>
<@node id="orders" label="Orders" href="/console/pets/${pet.id?c}/orders">
<#if order??><@node id="o${order.id?c}" label="Order ${order.id?c}" href="/console/pets/${pet.id?c}/orders/${order.id?c}"/></#if>
</@node>
<@node id="history" label="History" href="/console/pets/${pet.id?c}/history"/>
</@node>
<#if tab == "summary">
<@card type="datatables" id="pet" title=pet.name>
{
  rowType: 'Pet',
  dataMode: 'client',
  dataUrl: '/console/pets/${pet.id?c}/row',
  columns: [
    { key: 'id', label: 'Id' },
    { key: 'name', label: 'Name', render: { id: 'edit', meta: { action: 'rename', column: 'name', input: 'text' } } },
    { key: 'species', label: 'Species' },
    { key: 'status', label: 'Status', render: { id: 'pill' } },
    { key: 'price', label: 'Price (USD)', render: { id: 'edit', meta: { action: 'reprice', column: 'price', input: 'number' } } }
  ],
  rowActions: [
    <#-- The pencil edit cell commits these; present: 'inline' keeps them out of the row menu. -->
    { id: 'rename', label: 'Rename', endpoint: '/console/pets/{id}/stage', method: 'POST', confirm: false,
      present: 'inline',
      visibleWhen: [ { field: 'status', op: 'ne', value: 'SOLD' } ] },
    { id: 'reprice', label: 'Reprice', endpoint: '/console/pets/{id}/stage', method: 'POST', confirm: false,
      present: 'inline',
      visibleWhen: [ { field: 'status', op: 'ne', value: 'SOLD' } ] },
    { id: 'sell', label: 'Mark sold', endpoint: '/console/pets/{id}/sell', method: 'POST', onSuccess: 'redraw',
      confirm: "Mark '{name}' sold?", visibleWhen: [ { field: 'status', op: 'ne', value: 'SOLD' } ] }
  ],
  ribbon: [ { type: 'refresh' } ]
}
</@card>
<#elseif tab == "photo">
<@card type="html" id="photo" title="Photo">
<#include "fragments/pet-photo.ftlh">
</@card>
<#-- Server-side omission for non-admins; the client-side card visibleWhen rule is builder-only (CardSpec.visibleWhen). -->
<#if roles?seq_contains("admin")>
<@card type="html" id="photo-upload" title="Upload photo">
<form class="jc-form" data-petstore-upload="/petstore/pets/${pet.id?c}/photo"<#if apiCsrf??> data-petstore-csrf="${apiCsrf}"</#if>>
<input type="file" name="photo" accept="image/png,image/jpeg" aria-label="Photo file">
<button type="submit" class="jc-button">Upload</button>
</form>
</@card>
</#if>
<#elseif tab == "orders">
<@card type="datatables" id="pet-orders" title="Orders">
{
  rowType: 'Order',
  dataMode: 'client',
  dataUrl: '/console/pets/${pet.id?c}/orders/rows',
  columns: [
    { key: 'id', label: 'Order', href: '/console/pets/${pet.id?c}/orders/{id}' },
    { key: 'shipDate', label: 'Ship date' },
    { key: 'status', label: 'Status', render: { id: 'pill' } }
  ],
  detail: { endpoint: '/console/pets/${pet.id?c}/orders/{id}/detail', title: 'Order {id}' }
}
</@card>
<#if order??>
<@card type="html" id="order" title="Order ${order.id?c}">
<dl class="jc-details"><dt>Status</dt><dd>${order.status}</dd><dt>Ship date</dt><dd>${order.shipDate!""}</dd><dt>Pet</dt><dd>${pet.name}</dd></dl>
</@card>
</#if>
<#else>
<@card type="datatables" id="history" title="History">
{
  rowType: 'AuditEntry',
  dataMode: 'client',
  dataUrl: '/console/pets/${pet.id?c}/history/rows',
  defaultOrder: [ { key: 'at', dir: 'desc' } ],
  columns: [
    { key: 'at', label: 'When', render: { id: 'timestamp' } },
    { key: 'actor', label: 'Actor' },
    { key: 'action', label: 'Action' },
    { key: 'detail', label: 'Detail' }
  ]
}
</@card>
</#if>
</@page>
