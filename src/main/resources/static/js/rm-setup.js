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

    // Juneau owns probe SELECTION (radiogroup / ring / keyboard).  enhanceProbeGroup lives on JuneauViews.init
    // (not JuneauViews).  A second call is idempotent and ignores opts — Details listens for juneau:probe-select
    // (click and keyboard) instead of relying on onSelect surviving the page-wide initProbeGroups pass.
    const I = window.JuneauViews && window.JuneauViews.init;
    const ctl = I && I.enhanceProbeGroup ? I.enhanceProbeGroup(row) : null;

    let probes = [];
    let selectedId = ctl && ctl.getSelected() ? ctl.getSelected().getAttribute('data-juneau-probe') : null;

    const PROBE_STATUS = ['jc-probe-success', 'jc-probe-error', 'jc-probe-warning', 'jc-probe-neutral'];

    // pass|fail|warn|pending verdict -> the Juneau probe status class (success|error|warning|neutral).
    function statusClass(status) {
        if (status === 'pass') return 'jc-probe-success';
        if (status === 'fail') return 'jc-probe-error';
        if (status === 'warn') return 'jc-probe-warning';
        return 'jc-probe-neutral';
    }

    function escapeHtml(s) {
        return String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    function onSelect(id) {
        selectedId = id;
        renderDetails();
    }

    row.addEventListener('juneau:probe-select', (e) => {
        const id = e && e.detail && e.detail.id;
        if (id != null)
            onSelect(id);
    });

    // Repaints only the status color; selection (aria-checked/tabindex) is the helper's, kept across the repaint.
    function renderProbes() {
        row.querySelectorAll('[data-juneau-probe]').forEach((el) => {
            const p = probes.find((x) => x.id === el.getAttribute('data-juneau-probe'));
            el.classList.remove(...PROBE_STATUS);
            el.classList.add(statusClass(p ? p.status : 'pending'));
        });
        if (ctl) ctl.repaint();
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
        renderProbes();
        // Init does not emit juneau:probe-select; pick up the helper's initial selection (or its aria-checked
        // stamp if enhance raced after our first ctl read) so Details is not stuck on the placeholder.
        if (!selectedId) {
            const checked = row.querySelector('[data-juneau-probe][aria-checked="true"]');
            if (checked)
                selectedId = checked.getAttribute('data-juneau-probe');
        }
        if (selectedId)
            renderDetails();
    }

    loadData();
})();
