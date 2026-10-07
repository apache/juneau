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
--><@page tab="about">
<@card id="overview" title="About the petstore">
<div class="jc-prose">
<p>The petstore is Apache Juneau's sample application and its console regression suite. Every page is built from
the same console pieces your own app can use: navigation, cards, data tables over BeanQuery, async jobs, themes
and a bearer-token guard.</p>
<p>The data is seeded deterministically, so the same pets, orders and users appear on every start.</p>
</div>
</@card>
<@card id="links" title="Elsewhere">
<div class="jc-prose">
<ul>
<li><a href="/petstore/api">The REST API</a>, with its generated Swagger page.</li>
<li><a href="/petstore-ui">The React app</a>, a standalone single-page client of the same API.</li>
<li><a href="https://juneau.apache.org/docs/topics/JuneauPetstore">The petstore docs</a>.</li>
</ul>
</div>
</@card>
<@card id="beans" title="REST utility beans">
<div class="jc-prose">
<p>Three small beans for REST responses, shown against the <code>Pet</code> bean:</p>
<ul>
<li><a href="/console/about/beans/BeanDescription">BeanDescription</a>: the properties of a bean class.</li>
<li><a href="/console/about/beans/Hyperlink">Hyperlink</a>: a navigational link.</li>
<li><a href="/console/about/beans/SeeOtherRoot">SeeOtherRoot</a>: a redirect to the servlet root.</li>
</ul>
</div>
</@card>
</@page>
