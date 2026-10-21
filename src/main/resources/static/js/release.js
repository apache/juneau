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
  const $ = (id) => document.getElementById(id);
  const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };
  let state = { history: [] };
  let unmount = null;
  let shownAttempt = null;
  let openPromptId = null;
  // Prompt ids already answered or withdrawn. A /current fetched before the answer can arrive after it; without
  // this, its stale "pending" would reopen a dialog for a prompt that is gone.
  const resolved = new Set();

  let lastStatus = 0;

  async function post(path, body) {
    const r = await fetch('/rest/runs' + path, { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(body || {}) });
    lastStatus = r.status;
    if (!r.ok) {
      // Only the server's own error text is shown; the request body (which may hold a secret) is never echoed.
      const text = await r.text();
      alert(text || r.statusText);
      return null;
    }
    return r.json();
  }

  // Responses can arrive out of order: only the newest request issued so far is applied.
  let requestSeq = 0, appliedSeq = 0, openedSeq = 0;

  async function refresh() {
    const seq = ++requestSeq;
    const r = await fetch('/rest/runs/current', { headers: { Accept: 'application/json' } });
    if (!r.ok) return;
    const next = await r.json();
    if (seq < appliedSeq) return;
    appliedSeq = seq;
    state = next;
    if (!state.history) state.history = [];
    // A dialog whose prompt is gone (answered from the CLI, cancelled, process ended) is closed; only a /current
    // requested after the dialog opened can say so, since an older one may predate the prompt.
    if (openPromptId && seq > openedSeq && !(state.pending && state.pending.id === openPromptId)) closeDialog(openPromptId);
    render();
  }

  const PHASES = { rc: 'RC', vote: 'Vote', promote: 'Promote', released: 'Released' };

  function countdown(deadline) {
    const ms = Date.parse(deadline) - Date.now();
    if (isNaN(ms)) return '';
    if (ms <= 0) return ' (closed)';
    const h = Math.floor(ms / 3600000);
    return ' (in ' + Math.floor(h / 24) + 'd ' + (h % 24) + 'h)';
  }

  function render(tick) {
    const a = state.active;
    // A newest attempt still recorded as running counts as busy, so Start does not flicker in between events.
    const newest = state.history[0];
    const live = state.live || (newest && newest.outcome === 'running' ? { attemptId: newest.id } : null);
    $('rl-summary').textContent = a
      ? a.version + ' RC' + a.rc + ' · phase ' + (PHASES[a.phase] || a.phase) + (a.last ? ' · ' + a.last : '')
        + (a.voteDeadline ? ' · closes ' + a.voteDeadline + countdown(a.voteDeadline) : '')
      : live ? 'Starting a release…' : 'No release in progress.';
    // No run -> Start; live process -> only Cancel; otherwise Resume and Drop RC (hidden from Promote on).
    const promoteOrLater = a && (a.phase === 'promote' || a.phase === 'released');
    $('rl-start').hidden = !!a || !!live;
    $('rl-resume').hidden = !a || !!live;
    $('rl-drop').hidden = !a || !!live || promoteOrLater;
    if (a) $('rl-drop').textContent = 'Drop RC' + a.rc;
    $('rl-cancel').hidden = !state.live;
    $('rl-dry-run').disabled = !!live;
    if (!tick) renderHistory();
    if (state.live && shownAttempt !== state.live.attemptId) bindTerminal(state.live.attemptId, 'Live: ' + state.live.command);
    else if (!shownAttempt && state.history.length) bindTerminal(state.history[0].id, titleOf(state.history[0]));
    if (state.pending && state.pending.id !== openPromptId && !resolved.has(state.pending.id)) openDialog(state.pending);
  }

  function titleOf(h) {
    return h.label + ' ' + h.command + ' (' + h.origin + ')' + (h.dryRun ? ' · dry run' : '');
  }

  function renderHistory() {
    const tbody = $('rl-history').querySelector('tbody');
    tbody.replaceChildren(...state.history.map((h) => {
      const tr = document.createElement('tr');
      tr.dataset.attempt = h.id;
      tr.classList.toggle('selected', h.id === shownAttempt);
      const outcome = h.outcome + (h.step ? ' at ' + h.step : '') + (h.exit != null && h.exit !== 0 ? ' · exit ' + h.exit : '');
      for (const text of [h.label, h.command, h.origin, h.started || '', outcome]) {
        const td = document.createElement('td');
        td.textContent = text;
        tr.append(td);
      }
      if (h.dryRun) {
        const tag = document.createElement('span');
        tag.className = 'tag rl-dry-tag';
        tag.textContent = 'dry run';
        tr.children[1].append(' ', tag);
      }
      const td = document.createElement('td');
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'jc-btn jc-btn-sm';
      b.textContent = h.origin === 'ui' ? 'Replay' : 'Log';
      b.disabled = !h.hasTerminalLog && h.origin === 'ui';
      b.onclick = () => bindTerminal(h.id, titleOf(h));
      td.append(b);
      tr.append(td);
      return tr;
    }));
  }

  function bindTerminal(attemptId, title) {
    if (unmount) unmount();
    shownAttempt = attemptId;
    $('rl-terminal-title').textContent = title;
    $('rl-terminal').replaceChildren();
    unmount = JuneauTerminal.mount($('rl-terminal'), {
      id: 'rl-term',
      bytesUrl: '/rest/runs/juneau-terminal/' + encodeURIComponent(attemptId) + '/bytes',
      title: title
    });
    $('rl-history').querySelectorAll('tbody tr').forEach((tr) => tr.classList.toggle('selected', tr.dataset.attempt === attemptId));
  }

  // ---- Dialogs: one renderer per prompt kind -----------------------------------------------------------------

  const el = (tag, props, ...kids) => {
    const e = Object.assign(document.createElement(tag), props || {});
    e.append(...kids);
    return e;
  };

  function input(kind, value) {
    // A secret is only ever held in a password input: never prefilled, never copied elsewhere in the DOM.
    const i = el('input', { type: kind === 'secret' ? 'password' : 'text', value: kind === 'secret' ? '' : (value ?? ''), className: 'rl-input' });
    if (kind === 'secret') i.autocomplete = 'off';
    return i;
  }

  function copyRow(label, value, multiline) {
    const field = multiline ? el('textarea', { readOnly: true, value: value, rows: 12 }) : el('input', { readOnly: true, value: value });
    field.className = 'rl-email-field';
    const copy = el('button', { type: 'button', className: 'jc-btn', textContent: 'Copy' });
    copy.onclick = () => {
      const fallback = () => { field.focus(); field.select(); copy.textContent = 'Selected: press Ctrl/Cmd+C'; };
      if (!navigator.clipboard) return fallback();
      navigator.clipboard.writeText(value).then(() => { copy.textContent = 'Copied'; }).catch(fallback);
    };
    return el('div', { className: 'rl-email-row' }, el('span', { className: 'rl-email-label', textContent: label }), field, copy);
  }

  function openDialog(p) {
    if (openPromptId === p.id || resolved.has(p.id)) return; // the SSE event and /current can both deliver it
    openPromptId = p.id;
    openedSeq = requestSeq;
    const form = $('rl-dialog-form');
    const buttons = el('div', { className: 'rl-dialog-buttons' });
    let read = () => null;
    const ok = el('button', { type: 'button', className: 'jc-btn jc-btn-primary', textContent: 'OK' });
    const cancel = el('button', { type: 'button', className: 'jc-btn', textContent: 'Cancel' });
    cancel.onclick = () => answer(p.id, { cancel: true });
    const body = [el('h2', { id: 'rl-dialog-title', textContent: p.title || p.message || p.key })];
    if (p.error) body.push(el('p', { className: 'rl-error', textContent: p.error }));
    if (p.title && p.message) body.push(el('p', { textContent: p.message }));

    if (p.kind === 'text' || p.kind === 'secret') {
      const i = input(p.kind, p.default);
      body.push(i);
      read = () => ({ value: i.value });
    } else if (p.kind === 'confirm' && p.expect != null) {
      const i = input('text', '');
      ok.disabled = true;
      i.oninput = () => { ok.disabled = i.value !== p.expect; };
      body.push(i);
      read = () => ({ value: i.value });
    } else if (p.kind === 'confirm') {
      ok.textContent = 'Yes';
      const no = el('button', { type: 'button', className: 'jc-btn', textContent: 'No' });
      no.onclick = () => answer(p.id, { value: false });
      buttons.append(no);
      read = () => ({ value: true });
    } else if (p.kind === 'choice') {
      const list = el('div', { className: 'rl-choices' });
      for (const o of p.options || []) {
        const r = el('input', { type: 'radio', name: 'rl-choice', value: o.value, checked: o.value === p.default });
        list.append(el('label', {}, r, ' ' + (o.label || o.value)));
      }
      body.push(list);
      ok.disabled = !list.querySelector('input:checked');
      list.onchange = () => { ok.disabled = !list.querySelector('input:checked'); };
      read = () => {
        const c = list.querySelector('input:checked');
        return c ? { value: c.value } : null;
      };
    } else if (p.kind === 'form') {
      const inputs = {};
      for (const f of p.fields || []) {
        const i = f.kind === 'multiline' || f.kind === 'textarea' ? el('textarea', { value: f.default ?? '', rows: 4 }) : input(f.kind, f.default);
        i.name = f.name;
        i.required = !!f.required;
        inputs[f.name] = i;
        body.push(el('label', { className: 'rl-field' }, el('span', { textContent: f.label || f.name }), i));
      }
      read = () => ({ value: Object.fromEntries(Object.entries(inputs).map(([k, i]) => [k, i.value])) });
    } else if (p.kind === 'email') {
      const m = p.email || {};
      body.push(copyRow('To', (m.to || []).join(', ')));
      if (m.cc && m.cc.length) body.push(copyRow('Cc', m.cc.join(', ')));
      body.push(copyRow('Subject', m.subject || ''));
      body.push(copyRow('Body', m.body || '', true));
      if (m.mailto && /^mailto:/i.test(m.mailto)) body.push(el('p', {}, el('a', { href: m.mailto, textContent: 'Open in mail client' })));
      body.push(el('p', { textContent: 'Send it from your apache.org address, then:' }));
      ok.textContent = 'Sent';
      cancel.textContent = 'Not yet';
      cancel.onclick = () => answer(p.id, { value: 'not-yet' });
      read = () => ({ value: 'sent' });
    } else {
      body.push(el('p', { textContent: 'This prompt kind (' + p.kind + ') is not supported here; answer it from the CLI.' }));
      ok.disabled = true;
    }
    ok.onclick = () => {
      const v = read();
      if (v) answer(p.id, v);
    };
    buttons.append(cancel, ok);
    form.replaceChildren(...body, buttons);
    $('rl-dialog').setAttribute('aria-labelledby', 'rl-dialog-title');
    form.dataset.kind = p.kind;
    form.dataset.promptId = p.id;
    form.onsubmit = (e) => {
      // Enter in a field must answer, not let method="dialog" close the dialog without answering.
      e.preventDefault();
      if (!ok.disabled) ok.click();
    };
    if (!$('rl-dialog').open) $('rl-dialog').showModal();
  }

  function closeDialog(id) {
    resolved.add(id);
    if (openPromptId !== id) return;
    openPromptId = null;
    $('rl-dialog-form').replaceChildren();
    if ($('rl-dialog').open) $('rl-dialog').close();
  }

  async function answer(id, body) {
    const r = await post('/prompts/' + encodeURIComponent(id), body);
    // 409: the prompt is no longer pending (answered elsewhere or the process ended); drop the stale dialog.
    if (r || lastStatus === 409) closeDialog(id);
    if (!r) refresh();
  }

  // Esc closes a <dialog> natively; treat it as Cancel so the script is never left waiting on a hidden prompt.
  $('rl-dialog').addEventListener('cancel', (e) => {
    e.preventDefault();
    // On the email prompt a cancel would abort the step; "Not yet" is its way out, so Esc does nothing there.
    if (openPromptId && $('rl-dialog-form').dataset.kind !== 'email') answer(openPromptId, { cancel: true });
  });

  // ---- Controls ----------------------------------------------------------------------------------------------

  const dryRun = () => $('rl-dry-run').checked;
  $('rl-start').onclick = () => post('/start', { dryRun: dryRun(), idempotencyKey: $('rl-head').dataset.startKey }).then(refresh);
  $('rl-resume').onclick = () => post('/resume', { dryRun: dryRun() }).then(refresh);
  $('rl-cancel').onclick = () => {
    if (confirm('Cancel the running release.py? The attempt is recorded as cancelled and can be resumed.')) post('/cancel').then(refresh);
  };
  $('rl-drop').onclick = () => {
    const want = 'RC' + state.active.rc;
    const typed = prompt('Drop ' + state.active.version + ' ' + want + ' from Nexus and dist SVN? Type ' + want + ' to confirm.');
    if (typed === want) post('/drop-rc', { confirm: typed, dryRun: dryRun() }).then(refresh);
  };

  // ---- Events ------------------------------------------------------------------------------------------------

  const es = new EventSource('/events/runs');
  es.onmessage = (m) => {
    const e = JSON.parse(m.data);
    if (e.type === 'prompt') {
      // Ordering against `process {started}` is not guaranteed: render the prompt now, whatever attempt it belongs
      // to, then re-fetch so the page learns the live attempt (and its terminal) that goes with it.
      openDialog(e);
      refresh();
    } else if (e.type === 'prompt-resolved') {
      closeDialog(e.id);
    } else {
      refresh(); // process, run
    }
  };
  // After a dropped connection, catch up on whatever was missed.
  es.onopen = () => refresh();

  refresh();
  setInterval(() => { if (state.active && state.active.voteDeadline) render(true); }, 60000);
})();
