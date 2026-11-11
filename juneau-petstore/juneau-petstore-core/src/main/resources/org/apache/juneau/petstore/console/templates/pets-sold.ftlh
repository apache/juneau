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
--><@page toolkit="views,datatables-buttons">
<@card type="datatables" id="sold" title="Sold pets">
{
  rowType: 'Pet',
  dataMode: 'client',
  dataUrl: '/console/pets/sold/rows',
  columnConfig: true,
  detail: {"mode":"bean","endpoint":"/console/pets/row-detail/soldPet/{id}","title":"{name}","labels":{"price":"Price (USD)","id":"Id","name":"Name","species":"Species"},"tabs":[{"id":"details","kind":"fields","title":"Details","fields":["id","name","species","price"]},{"id":"orders","kind":"table","title":"Orders","nested":{"contractVersion":"2","parentScopeParam":"petId","view":{"id":"sold-orders","columns":[{"data":"id","title":"Order","render":{"id":"linked"},"href":"/console/pets/{petId}/orders/{id}"},{"data":"shipDate","title":"Ship date","render":{"id":"date"}},{"data":"status","title":"Status","render":{"id":"pill","meta":{"tones":{"PLACED":"info","APPROVED":"warning","DELIVERED":"success"}}}}]},"meta":{"contractVersion":"5","id":"sold-orders","dataUrl":"/console/pets/orders/rows","dataMode":"client","rowType":"Order","columns":[{"data":"id","title":"Order","render":{"id":"linked"},"href":"/console/pets/{petId}/orders/{id}"},{"data":"shipDate","title":"Ship date","render":{"id":"date"}},{"data":"status","title":"Status","render":{"id":"pill","meta":{"tones":{"PLACED":"info","APPROVED":"warning","DELIVERED":"success"}}}}]}},"count":"rows"}]},
  columns: [
    { key: 'id', label: 'Id' },
    { key: 'name', label: 'Name' },
    { key: 'species', label: 'Species', render: { id: 'pill', meta: { categorical: true, categories: { DOG: 1, CAT: 2, BIRD: 4, FISH: 6, SNAKE: 7, RABBIT: 9, MOUSE: 10 } } } },
    { key: 'price', label: 'Price (USD)', render: { id: 'decimal', meta: { places: 2 } } }
  ],
  ribbon: [
    { type: 'search', id: 'search', group: 'search' },
    { type: 'optionGroup', id: 'species', title: 'Species', group: 'filter', options: [
      { id: 'dogs', title: 'Dogs', symbol: 'paw_print', column: 'species', value: '$eq(DOG)' },
      { id: 'cats', title: 'Cats', symbol: 'shapes', column: 'species', value: '$eq(CAT)' }
    ] },
    { type: 'export', id: 'export', group: 'export', buttons: [ 'copy', 'csv' ] },
    { type: 'linkButton', id: 'link', group: 'export' },
    { type: 'collapseAll', id: 'collapse', group: 'viewing' },
    { type: 'columnSettings', id: 'columns', group: 'viewing' },
    { type: 'resetDefaults', id: 'reset', group: 'viewing' },
    { type: 'refresh', id: 'refresh', group: 'page' }
  ]
}
</@card>
</@page>
