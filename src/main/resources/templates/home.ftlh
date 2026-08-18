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

<#assign activeTab = "home">
<#macro content>
    <div class="jc-card">
        <div class="jc-card-header">
            <h2>Workflow</h2>
        </div>
        <ol class="rm-home-steps">
            <li><strong><a href="/rest/credentials">Credentials</a></strong> &mdash; configure and validate your Apache LDAP password, GPG signing key, and GitHub token first. Everything else depends on these being set up correctly.</li>
            <li><strong><a href="/rest/releases">Releases</a></strong> &mdash; review past and existing releases and their current status.</li>
            <li><strong><a href="/rest/releases#new">New Release</a></strong> &mdash; start and drive a new release through its steps.</li>
        </ol>
    </div>

    <div class="jc-card">
        <div class="jc-card-header">
            <h2>Prerequisites</h2>
        </div>
        <ul class="rm-home-prereqs">
            <li>A local checkout of <code>apache/juneau</code>.</li>
            <li><code>mvn</code>, <code>git</code>, <code>gh</code>, and <code>gpg</code> available on your <code>PATH</code>.</li>
            <li>Valid <code>apache.releases.https</code> credentials in <code>~/.m2/settings.xml</code>.</li>
        </ul>
    </div>
</#macro>
<#include "base.ftlh">
