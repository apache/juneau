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
--><@page tab="ops/jobs" toolkit="views">
<@card id="restock-help" title="Restocking">
<div class="jc-prose"><p>Restock adds new available pets to the store as a background job; progress shows here until it finishes.
Submitting twice from the same dialog runs one job.</p></div>
</@card>
<@card type="datatables" id="jobs">{
  dataMode: 'client',
  dataUrl: '/console/ops/jobs/rows',
  pollIntervalMs: 2000,
  columns: [
    { key: 'jobId', label: 'Job' },
    { key: 'submitted', label: 'Submitted', render: { id: 'timestamp' } },
    { key: 'state', label: 'State', render: { id: 'pill' } },
    { key: 'result.message', label: 'Result' }
  ],
  ribbon: [
    { type: 'dialog', id: 'restock', title: 'Restock', endpoint: '/console/ops/jobs/restock', method: 'POST', form: '/console/ops/jobs/restock-form', present: 'dialog' },
    { type: 'refresh', id: 'refresh' }
  ]
}</@card>
</@page>
