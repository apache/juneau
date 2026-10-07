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
--><@page tab="dev/secure" init=["/console/dev/secure/secure.js"]>
<@card id="about" title="Bearer-token guard">
<div class="jc-prose">
<p>This page is open. The API under <code>/console/dev/secure/api</code> is not: every call needs an
<code>Authorization: Bearer &lt;token&gt;</code> header, checked by a fail-closed guard.</p>
<p>Demo tokens: <code>petstore-user</code> (alice) and <code>petstore-admin</code> (admin). Anything else is a 401.</p>
<pre><code>curl -H 'Authorization: Bearer petstore-user' http://localhost:10000/console/dev/secure/api/pets</code></pre>
</div>
</@card>
<@card id="try" title="Try it">
<div class="jc-prose">
<p>
<button type="button" data-secure-call="none" data-secure-url="/console/dev/secure/api/whoami">Without a token</button>
<button type="button" data-secure-call="petstore-user" data-secure-url="/console/dev/secure/api/whoami">With the demo token</button>
<button type="button" data-secure-call="wrong-token" data-secure-url="/console/dev/secure/api/whoami">With a wrong token</button>
</p>
<pre><output id="secure-result" aria-live="polite"></output></pre>
</div>
</@card>
</@page>
