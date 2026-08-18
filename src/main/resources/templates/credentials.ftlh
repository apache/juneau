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
    <div class="jc-card">
        <#list credentials as c>
        <div class="cred" data-name="${c.name}">
            <#--
              Four states, four treatments. "Stored but never tested" used to share the red `invalid` class, which
              claimed the credential was bad when the app had no idea and sent the user off to re-enter a password
              that was probably fine. It must not read as a pass either -- hence amber rather than green.

              lastValid is a boxed Boolean whose null means "never validated", so the untested branch tests for
              absence (!c.lastValid??) and must come before the branches that read the value. Validation results
              are held in memory only, so a restart correctly returns every credential here.
            -->
            <#if !c.present>
                <#assign pillClass = 'unset'><#assign pillLabel = 'Not set'>
            <#elseif !c.lastValid??>
                <#assign pillClass = 'untested'><#assign pillLabel = 'Stored — not tested'>
            <#elseif c.lastValid>
                <#assign pillClass = 'valid'><#assign pillLabel = 'Valid'>
            <#else>
                <#assign pillClass = 'invalid'><#assign pillLabel = 'Invalid'>
            </#if>
            <h3>${c.label}
                <span class="pill ${pillClass}">${pillLabel}</span>
            </h3>
            <div class="row">
                <#if c.name != 'github'>
                    <input type="text" class="acct" placeholder="<#if c.name=='apache'>Apache availid<#else>GPG key ID</#if>">
                </#if>
                <input type="password" class="secret" placeholder="<#if c.name=='apache'>password<#elseif c.name=='gpg'>passphrase<#else>token</#if>">
                <button class="jc-btn jc-btn-primary" onclick="rmSet('${c.name}', this)">Save</button>
                <button class="jc-btn jc-btn-outline" onclick="rmValidate('${c.name}', this)">Validate</button>
            </div>
            <div class="msg">${c.lastMessage!''}</div>
        </div>
        </#list>
    </div>
</#macro>
<#include "base.ftlh">
