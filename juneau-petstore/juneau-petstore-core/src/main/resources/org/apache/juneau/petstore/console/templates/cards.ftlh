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
--><@page tab="dev/cards" init=["/console/dev/cards/petstore-cards.js"]>
<@card type="switcher" id="cards-sw" style="slider">
{ initialView: 'bands',
  views: [
    { id: 'available', label: 'Pets available', card: { type: 'kpi', id: 'available', title: 'Pets available', label: 'Pets available', dataUrl: '/console/dev/cards/stats/available' } },
    { id: 'gauge', label: 'Dog inventory', card: { type: 'inventory-gauge', id: 'gauge', title: 'Dog inventory', species: 'DOG', low: 20, high: 120 } },
    { id: 'bands', label: 'Stock by species', card: { type: 'chart', id: 'bands', title: 'Stock by species', src: '/console/dev/cards/inventory/all',
        publishes: [ { topic: 'app.band-picked', retain: false } ], kind: 'bar', x: { field: 'species' }, dataTab: true,
        series: [ { field: 'available', label: 'Available' }, { field: 'pending', label: 'Pending' }, { field: 'sold', label: 'Sold', tone: 'ok' } ],
        stacked: true, orientation: 'horizontal', decorators: [ { use: 'petstore.stockTotals' } ], on: { click: 'app.band-picked' } } },
    { id: 'prices', label: 'Prices by pet', card: { type: 'chart', id: 'prices', title: 'Prices by pet', src: '/console/dev/cards/prices',
        publishes: [ { topic: 'zoom:prices', retain: true } ],
        helper: { kind: 'scatter', x: { field: 'id', type: 'number', label: 'Pet' }, series: [ { field: 'price', label: 'Price' } ] },
        option: { dataZoom: [ { type: 'inside' }, { type: 'slider' } ] } } },
    { id: 'toolbar', label: 'Card API', card: { type: 'html', id: 'toolbar', title: 'Card API', src: '/console/dev/cards/fragments/toolbar' } },
    { id: 'how', label: 'Writing a card type', card: { type: 'html', id: 'how', title: 'Writing a card type', src: '/console/dev/cards/fragments/how' } } ] }
</@card>
</@page>
