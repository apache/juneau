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
--><@page tab="users" toolkit="views">
<@card type="datatables" id="users" title="Users">
{
  rowType: 'User',
  dataMode: 'server',
  dataUrl: '/console/users/query',
  defaultOrder: [ { key: 'username', dir: 'asc' } ],
  columns: [
    { key: 'username', label: 'Username' },
    { key: 'firstName', label: 'First name' },
    { key: 'lastName', label: 'Last name' },
    { key: 'userStatus', label: 'Status', render: { id: 'pill' } }
  ],
  ribbon: [
    { type: 'optionGroup', id: 'userStatus', title: 'User status', deselectable: true, options: [
      { id: 'active', title: 'Active', column: 'userStatus', value: '$eq(ACTIVE)' },
      { id: 'inactive', title: 'Inactive', column: 'userStatus', value: '$eq(INACTIVE)' }
    ] },
    { type: 'dialog', id: 'add-user', title: '+ Add user', symbol: 'plus',
      form: '/console/users/form', endpoint: '/console/users', method: 'POST', onSuccess: 'redraw' },
    { type: 'refresh' }
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
