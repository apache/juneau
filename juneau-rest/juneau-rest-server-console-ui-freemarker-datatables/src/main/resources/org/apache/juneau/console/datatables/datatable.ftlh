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
<#--
 Reserved <@datatable> macro template (org/apache/juneau/console/datatables/datatable.ftlh).

 Shipped on the classpath ROOT by juneau-rest-server-console-ui-freemarker-datatables and made resolvable by
 ConsoleDataTablesFreemarkerMixin's augmented template loader (the SAME classpath-root ClassTemplateLoader
 ConsoleFreemarkerMixin already splices in for base.ftlh -- see that class's javadoc). Consumer templates pull it in
 with a loader-root-absolute include:

   <#include "/org/apache/juneau/console/datatables/datatable.ftlh">

 v1 scope: this file's only job is to define the <@datatable id=... rows=... rowType=.../> macro, which renders a
 DataTablesTable.of(...)-built <table class="jc-table" data-juneau-datatable ...> (additively honoring each row
 property's @Html(render=...), per TODO-361 Phase 6) through the same serialize-then-mark-trusted adapter <@tag>
 uses. rowType is the row bean's fully-qualified class name as a plain string (e.g. rowType="com.example.Release"),
 not a FTL type reference -- FreeMarker templates have no way to express a Java Class literal directly.
-->
<#macro datatable id rows rowType>${jcDataTableHtml(id, rows, rowType)}</#macro>
