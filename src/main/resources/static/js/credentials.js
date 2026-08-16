/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

async function rmSet(name, btn) {
    const card = btn.closest('.cred');
    const acctEl = card.querySelector('.acct');
    const body = { account: acctEl ? acctEl.value : null, secret: card.querySelector('.secret').value };
    const r = await fetch('/rest/credentials/' + name, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    });
    if (r.ok) { card.querySelector('.secret').value = ''; location.reload(); }
    else { card.querySelector('.msg').textContent = 'Save failed'; }
}

async function rmValidate(name, btn) {
    const card = btn.closest('.cred');
    card.querySelector('.msg').textContent = 'Validating…';
    const r = await fetch('/rest/credentials/' + name + '/validate', { method: 'POST' });
    const res = await r.json();
    card.querySelector('.msg').textContent = res.message;
    const pill = card.querySelector('.pill');
    pill.className = 'pill ' + (res.valid ? 'valid' : 'invalid');
    pill.textContent = res.valid ? 'Valid' : 'Invalid';
}
