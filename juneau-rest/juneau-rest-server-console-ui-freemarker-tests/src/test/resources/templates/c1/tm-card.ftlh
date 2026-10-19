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
--><@page>
<@topic name="term.input-status" retain=false publisher="server"/>
<@bridge id="term" transport="websocket" session="servlet:/juneau-bus/session"
  downstream=["term.input-status"] upstream=["term.input"]/>
<@card type="terminal" id="build" title="Build" publishes=[{"topic":"term.input","retain":false}]
  subscribes=[{"topic":"term.input-status","as":"input-status"}]>
{
    contractVersion: '1',
    terminal: {
        bytesUrl: '/runs/juneau-terminal/${"r-7"?json_string?no_esc}/bytes',
        eventsUrl: '/runs/juneau-run-view/r-7/events',
        refreshMs: 500,
        title: 'mvn test',
        tailBytes: 1048576,
        input: true,
        inputId: 'r-7'
    }
}
</@card>
</@page>
