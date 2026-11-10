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
<@card id="t" type="datatables">
{
  "dataUrl": "/rest/t/data",
  "columns": [
    {
      "key": "id",
      "label": "Id"
    }
  ],
  "ribbon": [
    {
      "type": "refresh",
      "id": "refresh"
    },
    {
      "type": "export",
      "id": "export",
      "buttons": [
        "copy",
        "csv",
        "print"
      ]
    },
    {
      "type": "collapseAll",
      "id": "collapseAll"
    },
    {
      "type": "pausePolling",
      "id": "pausePolling"
    },
    {
      "type": "resetDefaults",
      "id": "resetDefaults"
    },
    {
      "type": "search",
      "id": "search"
    },
    {
      "type": "linkButton",
      "id": "link"
    },
    {
      "type": "columnSettings",
      "id": "columns"
    },
    {
      "type": "dialog",
      "id": "add",
      "title": "Add repo",
      "form": "/rest/t/add-modal",
      "endpoint": "/rest/t/new",
      "method": "POST",
      "onSuccess": "redraw"
    }
  ]
}
</@card>
</@page>
