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
--><@page toolkit="views">
<@card type="datatables" id="sold" title="Sold pets">
{
  rowType: 'Pet',
  dataMode: 'client',
  dataUrl: '/console/pets/sold/rows',
  columns: [
    { key: 'id', label: 'Id' },
    { key: 'name', label: 'Name' },
    { key: 'species', label: 'Species' },
    { key: 'price', label: 'Price (USD)', render: { id: 'decimal', meta: { places: 2 } } }
  ],
  ribbon: [
    { type: 'optionGroup', id: 'species', title: 'Species', options: [
      { id: 'dogs', title: 'Dogs', column: 'species', value: '$eq(DOG)' },
      { id: 'cats', title: 'Cats', column: 'species', value: '$eq(CAT)' }
    ] },
    { type: 'refresh' }
  ]
}
</@card>
</@page>
