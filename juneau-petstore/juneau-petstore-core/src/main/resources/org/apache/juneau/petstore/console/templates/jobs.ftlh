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
Submitting twice from the same dialog runs one job.  Give the card below a wait to see a cancel before anything changes, or a longer hold to see one after.</p></div>
</@card>
<@card type="job-log" id="restock" title="Restock inventory">{
  submit: {
    endpoint: '/console/ops/jobs/restock',
    submitLabel: 'Restock',
    fields: [
      { name: 'DOG', label: 'Dogs (0-50)', type: 'number', default: 1 },
      { name: 'slowMs', label: 'Wait before restocking (ms, 0-10000)', type: 'number', default: 0 },
      { name: 'holdMs', label: 'Hold after restocking (ms, 0-10000)', type: 'number', default: 1000 }
    ]
  },
  urlParam: 'job'
}</@card>
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
    { type: 'dialog', id: 'restock', title: 'Restock', endpoint: '/console/ops/jobs/restock', method: 'POST', form: '/console/ops/jobs/restock-form' },
    { type: 'refresh', id: 'refresh' }
  ]
}</@card>
<@card type="datatables" id="species">{
  dataMode: 'client',
  dataUrl: '/console/ops/jobs/species',
  pollIntervalMs: 2000,
  columns: [
    { key: 'species', label: 'Species' },
    { key: 'available', label: 'Available' }
  ],
  detail: {
    endpoint: '/console/ops/jobs/species/{id}',
    region: {
      id: 'restock-species',
      populate: 'job-log',
      config: {
        submit: {
          endpoint: '/console/ops/jobs/species/{id}/restock',
          envelope: 'action',
          action: 'restock-species',
          keyUrl: '/console/ops/jobs/species/{id}/key',
          submitLabel: 'Restock',
          fields: [
            { name: 'count', label: 'How many (1-50)', type: 'number', default: 1, required: true },
            { name: 'species', type: 'hidden', source: 'row.species' }
          ]
        },
        detail: { renderer: 'fieldGrid', fields: [ { data: 'available', label: 'Available' } ] },
        output: { target: { field: 'available' }, from: 'row.available' }
      }
    }
  }
}</@card>
<@card type="terminal" id="groom-output" title="Groom run">
{
  contractVersion: '1',
  terminal: {
    bytesUrl: '${groomBytesUrl?json_string?no_esc}',
    title: 'Groom log'
  }
}
</@card>
<@card type="terminal" id="groom-file" title="Groom transcript (file)">
{
  contractVersion: '1',
  terminal: {
    bytesUrl: '${groomFileBytesUrl?json_string?no_esc}',
    title: 'Transcript file'
  }
}
</@card>
<@card type="datatables" id="groom-runs">{
  dataMode: 'client',
  dataUrl: '/console/ops/jobs/groom-rows',
  columns: [
    { key: 'pet', label: 'Pet' },
    { key: 'submitted', label: 'Submitted', render: { id: 'timestamp' } },
    { key: 'state', label: 'State', render: { id: 'pill' } },
    { key: 'lines', label: 'Lines' }
  ],
  detail: {
    endpoint: '/console/ops/jobs/groom-runs/{id}',
    region: { id: 'regroom', populate: 'petstore-regroom' }
  },
  ribbon: [
    { type: 'dialog', id: 'groom', title: 'Groom', endpoint: '/console/ops/jobs/groom', method: 'POST', form: '/console/ops/jobs/groom-form' },
    { type: 'refresh', id: 'groom-refresh' }
  ]
}</@card>
</@page>
