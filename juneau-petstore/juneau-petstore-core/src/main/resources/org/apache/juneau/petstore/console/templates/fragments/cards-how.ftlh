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
-->
<div class="jc-prose">
<p>A card type is a JS class extending the console's Card, and optionally a Java class that validates the body and shapes what the handler
receives. Push classes onto <code>window.JuneauConsoleCards</code>; the queue works whether your script loads before
or after the console.</p>
<pre><code>class KpiCard extends JuneauConsole.Card {
  render() { ... }
}
(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(['kpi', KpiCard]);</code></pre>
<p>The gauge's Java side is <code>InventoryGaugeCardType</code>, registered with
<code>ConsoleFreemarkerMixin.create().cardType(new InventoryGaugeCardType())</code>.</p>
</div>
