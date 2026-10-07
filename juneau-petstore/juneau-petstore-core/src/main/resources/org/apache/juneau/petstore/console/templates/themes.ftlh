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
--><@page tab="dev/themes">
<@card id="swatches" title="This theme">
<div class="jc-prose">
<p>The page is drawn in <strong>${petstoreTheme!"open"}</strong><#if petstoreCustomTheme!false>, with two custom tokens</#if>.</p>
<p>
<span class="tag stage released">Available</span>
<span class="tag stage pending">Pending</span>
<span class="tag stage failed">Sold</span>
</p>
</div>
</@card>
<@card id="picker" title="Pick a theme">
<div class="jc-prose">
<ul>
<#list themeChoices as t><li><a href="/console/dev/themes?theme=${t}">${t}</a></li></#list>
</ul>
</div>
</@card>
<@card id="authoring" title="Writing a custom theme">
<div class="jc-prose">
<p>Seed from a stock theme and override tokens. A <code>value</code> sets a token; an <code>alias</code> makes it
follow another token.</p>
<pre><code>&lt;@theme name="light-brown"&gt;
&lt;@token name="--jc-pill-red-bg" value="#fdeceb"/&gt;
&lt;@token name="--jc-tab-bar-bg" alias="var(--jc-card-bg)"/&gt;
&lt;/@theme&gt;</code></pre>
</div>
</@card>
</@page>
