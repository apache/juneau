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
--><@page tab="users" toolkit="views,datatables-buttons,datatables-export">
<@card type="datatables" id="users" title="Users">
{
  rowType: 'User',
  dataMode: 'server',
  dataUrl: '/console/users/query',
  columnConfig: true,
  defaultOrder: [ { key: 'username', dir: 'asc' } ],
  columns: [
    { key: 'username', label: 'Username' },
    { key: 'firstName', label: 'First name' },
    { key: 'lastName', label: 'Last name' },
    { key: 'userStatus', label: 'Status', render: { id: 'pill', meta: { tones: { ACTIVE: 'success', INACTIVE: 'neutral' } } } }
  ],
  ribbon: [
    { type: 'search', id: 'search', group: 'search' },
    { type: 'dialog', id: 'add-user', title: '+ Add user', symbol: 'plus', group: 'entity',
      form: '/console/users/form', endpoint: '/console/users', method: 'POST', onSuccess: 'redraw' },
    { type: 'optionGroup', id: 'userStatus', title: 'User status', group: 'filter', deselectable: true, options: [
      { id: 'active', title: 'Active', symbol: 'user_check', column: 'userStatus', value: '$eq(ACTIVE)' },
      { id: 'inactive', title: 'Inactive', symbol: 'user_x', column: 'userStatus', value: '$eq(INACTIVE)' }
    ] },
    { type: 'export', id: 'export', group: 'export', buttons: [ 'copy', 'csv' ], optional: [ 'excel', 'pdf' ] },
    { type: 'linkButton', id: 'link', group: 'export' },
    { type: 'collapseAll', id: 'collapse', group: 'viewing' },
    { type: 'columnSettings', id: 'columns', group: 'viewing' },
    { type: 'resetDefaults', id: 'reset', group: 'viewing' },
    { type: 'refresh', id: 'refresh', group: 'page' }
  ],
  rowActions: [
    { id: 'edit', label: 'Edit', form: '/console/users/form?username={username}', present: 'dialog',
      endpoint: '/console/users/{username}', method: 'PUT', onSuccess: 'redraw' },
    { id: 'delete', label: 'Delete', endpoint: '/console/users/{username}', method: 'DELETE',
      confirm: "Delete user '{username}'?", confirmLabel: 'Delete', tone: 'danger', onSuccess: 'redraw' }
  ]
}
</@card>
</@page>
