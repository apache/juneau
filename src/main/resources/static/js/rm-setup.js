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

(function () {
    const row = document.getElementById('rm-probe-row');
    const details = document.getElementById('rm-probe-details');
    if (!row || !details) return;

    let probes = [];
    let selectedId = null;

    function statusClass(status) {
        if (status === 'pass') return 'rm-probe-pill-pass';
        if (status === 'fail') return 'rm-probe-pill-fail';
        if (status === 'warn') return 'rm-probe-pill-warn';
        return 'rm-probe-pill-pending';
    }

    function escapeHtml(s) {
        return String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    function renderPills() {
        row.querySelectorAll('[data-probe-id]').forEach((el) => {
            const p = probes.find((x) => x.id === el.getAttribute('data-probe-id'));
            el.className = 'rm-probe-pill ' + statusClass(p ? p.status : 'pending');
            if (el.getAttribute('data-probe-id') === selectedId)
                el.classList.add('rm-probe-pill-selected');
        });
    }

    function credentialForm(p) {
        const name = p.credentialName;
        let acct = '';
        if (name !== 'github') {
            const ph = name === 'apache' ? 'Apache availid' : 'GPG key ID';
            acct = '<input type="text" class="acct" placeholder="' + ph + '">';
        }
        const sph = name === 'apache' ? 'password' : name === 'gpg' ? 'passphrase' : 'token';
        return '<div class="cred" data-name="' + escapeHtml(name) + '">'
            + '<div class="row">'
            + acct
            + '<input type="password" class="secret" placeholder="' + sph + '">'
            + '<button type="button" class="jc-btn jc-btn-primary" onclick="rmSet(\'' + name + '\', this)">Save</button>'
            + '<button type="button" class="jc-btn jc-btn-outline" onclick="rmValidate(\'' + name + '\', this)">Validate</button>'
            + '</div>'
            + '<div class="msg"></div></div>';
    }

    function renderDetails() {
        const p = probes.find((x) => x.id === selectedId);
        if (!p) {
            details.innerHTML = '<p class="rm-probe-placeholder">Select a probe.</p>';
            return;
        }
        const bits = [];
        bits.push('<h3>' + escapeHtml(p.label) + '</h3>');
        bits.push('<dl class="rm-detail-list">');
        bits.push('<dt>Status</dt><dd>' + escapeHtml(p.status || 'pending') + '</dd>');
        bits.push('<dt>Message</dt><dd>' + escapeHtml(p.message || '') + '</dd>');
        bits.push('<dt>Next step</dt><dd>' + escapeHtml(p.nextStep || '') + '</dd>');
        bits.push('</dl>');
        if (p.copyCommand) {
            bits.push('<pre class="rm-probe-copy" id="rm-probe-copy-text">' + escapeHtml(p.copyCommand) + '</pre>');
            bits.push('<button type="button" class="jc-btn jc-btn-outline jc-btn-sm" id="rm-probe-copy-btn">Copy command</button>');
        }
        if (p.installable) {
            bits.push('<button type="button" class="jc-btn jc-btn-primary" id="rm-probe-install-btn">Install</button>');
            bits.push('<pre class="rm-probe-install-out" id="rm-probe-install-out" hidden></pre>');
        }
        if (p.kind === 'credential')
            bits.push(credentialForm(p));
        details.innerHTML = bits.join('');
        const copyBtn = document.getElementById('rm-probe-copy-btn');
        if (copyBtn)
            copyBtn.addEventListener('click', () => navigator.clipboard.writeText(p.copyCommand));
        const installBtn = document.getElementById('rm-probe-install-btn');
        if (installBtn)
            installBtn.addEventListener('click', () => install(p.id));
    }

    async function install(id) {
        const out = document.getElementById('rm-probe-install-out');
        const btn = document.getElementById('rm-probe-install-btn');
        if (btn) btn.disabled = true;
        if (out) {
            out.hidden = false;
            out.textContent = 'Installing…';
        }
        try {
            const r = await fetch('/rest/setup/install/' + encodeURIComponent(id), { method: 'POST' });
            const body = await r.json();
            if (out)
                out.textContent = body.output || (r.ok ? 'Done' : 'Install failed');
            await loadData();
            renderDetails();
        } catch (e) {
            if (out)
                out.textContent = String(e);
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    async function loadData() {
        const r = await fetch('/rest/setup/data');
        const body = await r.json();
        probes = body.probes || [];
        renderPills();
        if (selectedId)
            renderDetails();
    }

    row.addEventListener('click', (ev) => {
        const btn = ev.target.closest('[data-probe-id]');
        if (!btn) return;
        selectedId = btn.getAttribute('data-probe-id');
        renderPills();
        renderDetails();
    });

    loadData();
})();
