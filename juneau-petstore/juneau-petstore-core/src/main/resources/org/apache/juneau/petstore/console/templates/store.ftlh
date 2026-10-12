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
--><@page tab="store" toolkit="views">
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
    { id: 'orders', label: 'Orders', value: '${qsOrders?js_string}' },
    { id: 'inventory', label: 'Inventory', value: '${qsInventory?js_string}' }
  ] }
}
</@card>
<#-- Blocked: ChartDef. The three chart cards (orders-trend, by-species, by-status) go here, in this order, once the
     chart type lands; they read /console/store/stats/daily, /species and /status. -->
<@card type="html" id="hours" title="Store hours" src="/console/store/fragments/hours"/>
</@page>
