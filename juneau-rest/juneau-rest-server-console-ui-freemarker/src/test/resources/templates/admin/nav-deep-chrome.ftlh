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
--><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<#if pageTab?has_content><meta name="page-tab" content="${pageTab}"></#if>
</head>
<body>
<@navigation>
<@node id="home" label="Home" href="/home">
<@node id="skill-repos" label="Skill Repos" href="/home/skill-repos">
<@node id="alpha" label="Alpha" href="/home/skill-repos/alpha"/>
<@node id="beta" label="Beta" href="/home/skill-repos/beta"/>
</@node>
<@node id="settings" label="Settings" href="/home/settings"/>
</@node>
<@node id="pagerduty" label="PagerDuty" href="/pd"/>
</@navigation>
<main class="jc-main">${pageBody}</main>
</body>
</html>
