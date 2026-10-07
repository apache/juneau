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
--><#-- Chrome-less FreeMarker fragment for the Rendering flavors page.  No <@page>: an html card loads it with src=. -->
<table class="petstore-flavor" data-flavor="freemarker">
<tr><th>Name</th><th>Species</th><th>Price</th><th>Status</th></tr>
<#list pets as p>
<tr><td>${p.name}</td><td>${p.species}</td><td>${p.price}</td><td>${p.status}</td></tr>
</#list>
</table>
