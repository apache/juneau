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
--><@page tab="ops/audit" toolkit="views">
<@card type="datatables" id="audit">{
  dataMode: 'server',
  dataUrl: '/console/ops/audit/query',
  defaultOrder: [{ key: 'at', dir: 'desc' }],
  columns: [
    { key: 'at', label: 'When', render: { id: 'timestamp' }, searchType: 'timestamp' },
    { key: 'actor', label: 'Actor', searchType: 'text' },
    { key: 'action', label: 'Action', searchType: 'text' },
    { key: 'entity', label: 'Entity', searchType: 'text' },
    { key: 'entityId', label: 'Id', searchType: 'text' },
    { key: 'detail', label: 'Detail', searchType: 'text' }
  ],
  ribbon: [
    { type: 'export', id: 'export' },
    { type: 'refresh', id: 'refresh' }
  ]
}</@card>
</@page>
