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

<#assign activeTab = "credentials">
<#macro content>
    <div class="rm-card">
        <#list credentials as c>
        <div class="cred" data-name="${c.name}">
            <h3>${c.label}
                <span class="pill ${c.present?string(c.lastValid!false?string('valid','invalid'),'unset')}">
                    <#if !c.present>Not set<#elseif c.lastValid??>${c.lastValid?string('Valid','Invalid')}<#else>Set (unvalidated)</#if>
                </span>
            </h3>
            <div class="row">
                <#if c.name != 'github'>
                    <input type="text" class="acct" placeholder="<#if c.name=='apache'>Apache availid<#else>GPG key ID</#if>">
                </#if>
                <input type="password" class="secret" placeholder="<#if c.name=='apache'>password<#elseif c.name=='gpg'>passphrase<#else>token</#if>">
                <button class="btn btn-primary" onclick="rmSet('${c.name}', this)">Save</button>
                <button class="btn btn-outline" onclick="rmValidate('${c.name}', this)">Validate</button>
            </div>
            <div class="msg">${c.lastMessage!''}</div>
        </div>
        </#list>
    </div>
</#macro>
<#include "base.ftlh">
