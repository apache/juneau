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
--><@console brand="Juneau Petstore" theme=(petstoreTheme!"open")>
<#if petstoreCustomTheme!false>
<@theme name="light-brown">
<@token name="--jc-pill-red-bg" value="#fdeceb"/>
<@token name="--jc-tab-bar-bg" alias="var(--jc-card-bg)"/>
</@theme>
</#if>
<#-- The views runtime binds datatables cards to jQuery and DataTables, which Juneau does not bundle; this app serves them from its WebJars. -->
<@scripts phase="after-toolkit"><script src="/console/vendor/jquery/3.7.1/jquery.min.js"></script>
<script src="/console/vendor/datatables.net/js/dataTables.min.js"></script></@scripts>
<@navigation>
<@node id="store" label="Store" href="/console/store"/>
<@node id="pets" label="Pets" href="/console/pets">
<@node id="sold" label="Sold" href="/console/pets/sold"/>
<@node id="changes" label="Pending changes" href="/console/pets/changes"/>
</@node>
<@node id="orders" label="Orders" href="/console/orders"/>
<@node id="users" label="Users" href="/console/users"/>
<@node id="ops" label="Operations" href="/console/ops/jobs">
<@node id="jobs" label="Jobs" href="/console/ops/jobs"/>
<@node id="audit" label="Audit" href="/console/ops/audit"/>
</@node>
<@node id="dev" label="Developer" href="/console/dev/cards">
<@node id="cards" label="Custom cards" href="/console/dev/cards"/>
<@node id="themes" label="Themes" href="/console/dev/themes"/>
<@node id="flavors" label="Rendering flavors" href="/console/dev/flavors/html">
<@node id="html" label="Html" href="/console/dev/flavors/html"/>
<@node id="freemarker" label="Freemarker" href="/console/dev/flavors/freemarker"/>
<@node id="mustache" label="Mustache" href="/console/dev/flavors/mustache"/>
<@node id="react" label="React" href="/console/dev/flavors/react"/>
</@node>
<@node id="secure" label="Secure" href="/console/dev/secure"/>
</@node>
<@node id="about" label="About" href="/console/about"/>
</@navigation>
<@main/>
<@footer text="Apache Juneau petstore sample"/>
</@console>
