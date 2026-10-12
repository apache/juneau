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
--><@page tab="dev/cards" init=["/console/dev/cards/petstore-cards.js"]>
<@card id="available" type="kpi" title="Pets available">{label:'Pets available', dataUrl:'/console/dev/cards/stats/available'}</@card>
<@card id="gauge" type="inventory-gauge" title="Dog inventory">{species:'DOG', low:20, high:120}</@card>
<@card id="toolbar" title="Card API">
<div class="jc-prose">
<p>
<button type="button" class="jc-btn jc-btn-outline" data-cards-action="refresh">Refresh the gauge</button>
<button type="button" class="jc-btn jc-btn-outline" data-cards-action="api">Read the gauge API</button>
</p>
<output id="cards-api-result" aria-live="polite"></output>
</div>
</@card>
<@card id="how" title="Writing a card type">
<div class="jc-prose">
<p>A card type is a JS handler, and optionally a Java class that validates the body and shapes what the handler
receives. Push handlers onto <code>window.JuneauConsoleCards</code>; the queue works whether your script loads before
or after the console.</p>
<pre><code>(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(['kpi', {
  render(card, el, ctx) { ... }
}]);</code></pre>
<p>The gauge's Java side is <code>InventoryGaugeCardType</code>, registered with
<code>ConsoleFreemarkerMixin.create().cardType(new InventoryGaugeCardType())</code>.</p>
</div>
</@card>
</@page>
