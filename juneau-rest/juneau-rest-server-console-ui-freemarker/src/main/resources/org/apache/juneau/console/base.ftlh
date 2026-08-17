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
 Reserved admin-console chrome template (org/apache/juneau/console/base.ftlh).

 Shipped on the classpath ROOT by juneau-rest-server-console-ui-freemarker and made resolvable by
 ConsoleFreemarkerMixin's augmented template loader. Consumer templates pull it in with a loader-root-absolute
 include (NOT a bare relative path -- FreeMarker resolves a relative <#include> against the INCLUDING template's
 own directory, so a relative "org/apache/juneau/console/base.ftlh" from e.g. /templates/admin/mypage.ftlh would
 miss, looking for admin/org/apache/juneau/console/base.ftlh instead):

   <#include "/org/apache/juneau/console/base.ftlh">

 v1 scope: this file's only job is to define the <@tag domain=... value=.../> macro (the design's
 serialize-then-mark-trusted Trusted-HTML insertion contract for enum pill/badge markup). <#include> shares the
 including template's namespace, so the macro becomes directly callable as <@tag .../> with no import prefix.
 Page-level chrome layout (nav/header shell) is out of v1 scope -- see the design's P3/fast-follow list.
-->
<#macro tag domain value>${jcTagHtml(domain, value)}</#macro>
