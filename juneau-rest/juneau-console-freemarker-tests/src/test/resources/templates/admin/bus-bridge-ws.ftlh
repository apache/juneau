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
<@topic name="ops.jobs" retain=true publisher="server"/>
<@topic name="ops.cancel-all" retain=false publisher="ribbon"/>
<@bridge id="ops" transport="websocket" session="/rest/ops/juneau-bus/session"
  downstream=["ops.jobs"] upstream="ops.cancel-all" maxAttempts=5/>
<@card type="html" id="jobs" title="Jobs" subscribes="ops.jobs"><p>Jobs</p></@card>
</@page>
