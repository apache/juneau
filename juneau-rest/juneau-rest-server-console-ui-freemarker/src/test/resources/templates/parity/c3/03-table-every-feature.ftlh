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
<@card id="slo" type="datatables">
{
  "dataUrl": "/rest/slo/data",
  "columns": [
    {
      "key": "pod",
      "label": "Pod",
      "className": "ssc-id-cell"
    },
    {
      "key": "incidentKey",
      "label": "Incident",
      "render": "linked",
      "href": "{incidentUrl}"
    },
    {
      "key": "rootCause",
      "label": "Root cause",
      "defaultVisible": false
    }
  ],
  "defaultOrder": [
    {
      "key": "pod",
      "dir": "asc"
    }
  ],
  "ribbon": [
    {
      "type": "refresh"
    },
    {
      "type": "export",
      "buttons": [
        "copy",
        "csv"
      ]
    }
  ],
  "rowClassRules": [
    {
      "field": "analysisState",
      "op": "eq",
      "value": "degraded",
      "class": "row-degraded"
    }
  ],
  "rowActions": [
    {
      "id": "analyze",
      "label": "Analyze pod",
      "endpoint": "/rest/slo/analyze",
      "method": "POST"
    }
  ],
  "detail": {
    "endpoint": "/rest/slo/data/{id}",
    "title": "{pod}",
    "region": {
      "id": "slo-detail",
      "type": "row-detail",
      "populate": "slo-detail",
      "dataUrl": "/rest/slo/data/{id}",
      "lazy": false
    }
  }
}
</@card>
</@page>
