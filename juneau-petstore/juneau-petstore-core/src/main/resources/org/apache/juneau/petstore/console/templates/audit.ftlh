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
--><@page tab="ops/audit" toolkit="views,datatables-buttons">
<@card type="datatables" id="audit">{
  dataMode: 'server',
  dataUrl: '/console/ops/audit/query',
  columnConfig: true,
  defaultOrder: [{ key: 'at', dir: 'desc' }],
  columns: [
    { key: 'at', label: 'When', render: { id: 'timestamp' }, searchType: 'timestamp' },
    { key: 'actor', label: 'Actor', searchType: 'text' },
    { key: 'action', label: 'Action', render: { id: 'pill', meta: { tones: { CREATE: 'success', UPDATE: 'info', DELETE: 'error', DISCARD: 'warning', RESTOCK: 'info' } } }, searchType: 'text' },
    { key: 'entity', label: 'Entity', render: { id: 'pill', meta: { categorical: true, categories: { Pet: 1, Order: 2, User: 4, Change: 6, Job: 7 } } }, searchType: 'text' },
    { key: 'entityId', label: 'Id', searchType: 'text' },
    { key: 'detail', label: 'Detail', searchType: 'text' }
  ],
  ribbon: [
    { type: 'search', id: 'search', group: 'search' },
    { type: 'export', id: 'export', group: 'export', buttons: [ 'copy', 'csv' ] },
    { type: 'linkButton', id: 'link', group: 'export' },
    { type: 'collapseAll', id: 'collapse', group: 'viewing' },
    { type: 'columnSettings', id: 'columns', group: 'viewing' },
    { type: 'resetDefaults', id: 'reset', group: 'viewing' },
    { type: 'refresh', id: 'refresh', group: 'page' }
  ]
}</@card>
</@page>
