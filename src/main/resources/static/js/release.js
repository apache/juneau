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

  async function post(path, body) {
    const r = await fetch('/rest/runs' + path, { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(body || {}) });
    if (!r.ok) {
      // Only the server's own error text is shown; the request body (which may hold a secret) is never echoed.
      const text = await r.text();
      alert(text || r.statusText);
      return null;
    }
    return r.json();
  }

  // Responses can arrive out of order: only the newest request issued so far is applied.
  let requestSeq = 0, appliedSeq = 0;

  async function refresh() {
    const seq = ++requestSeq;
    const r = await fetch('/rest/runs/current', { headers: { Accept: 'application/json' } });
    if (!r.ok) return;
    const next = await r.json();
    if (seq < appliedSeq) return;
    appliedSeq = seq;
    state = next;
    if (!state.history) state.history = [];
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
    const pauseEl = $('rl-pause');
    // A pending mail counts as paused even if the pause block is missing (a kill between send() and the recorded pause).
    const text = a && a.pause && a.pause.message ? a.pause.message
      : (a && a.emailPending ? 'The ' + a.emailPending + ' email is waiting to be sent; send it (see the run directory), then press Resume.' : '');
    const paused = !!text && !live;
    pauseEl.hidden = !paused;
    pauseEl.textContent = paused ? text : '';
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
  es.onmessage = () => refresh(); // process, run
  // After a dropped connection, catch up on whatever was missed.
  es.onopen = () => refresh();

  refresh();
  setInterval(() => { if (state.active && state.active.voteDeadline) render(true); }, 60000);
})();
