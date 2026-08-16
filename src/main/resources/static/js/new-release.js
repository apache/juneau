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
  const val = (id) => document.getElementById(id)?.value ?? null;

  // App-local Input/Execution subtab toggle. Only shows/hides panels — the Execution panel (and its two
  // EventSources, wired further down) is never removed from the DOM, so switching tabs never reconnects SSE.
  globalThis.nrSubtab = function (name) {
    document.querySelectorAll('.rm-subtab').forEach((b) => b.classList.toggle('active', b.dataset.subtab === name));
    document.querySelectorAll('.rm-subtab-panel').forEach((p) => { p.hidden = p.dataset.subtab !== name; });
  };

  // Defined unconditionally so the start form (rendered when `run` is null, before `.rm-rail-layout`
  // exists) always has it available. Captures the release metadata plus the four optional narrative fields.
  globalThis.nrStart = async function () {
    const msEl = document.getElementById('nr-milestone');
    const ms = msEl?.value ? Number(msEl.value) : null;
    await fetch('/rest/runs', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        version: val('nr-version'), developmentVersion: val('nr-devversion'), milestoneNumber: ms,
        mode: document.querySelector('input[name="nr-mode"]:checked')?.value || 'SAFE',
        releaseSummary: val('nr-releaseSummary'), highlights: val('nr-highlights'),
        knownIssues: val('nr-knownIssues'), acknowledgements: val('nr-acknowledgements')
      })
    });
    location.reload();
  };

  // Persist edits to the four narrative fields on the active run (POST /{version}/details) without leaving
  // the page, so they can be revised before each email is composed. Surfaces a terse inline result.
  globalThis.nrSaveDetails = async function () {
    const ver = document.querySelector('.rm-rail-layout')?.dataset.version;
    const msg = document.getElementById('nr-details-msg');
    if (!ver) return;
    try {
      const r = await fetch('/rest/runs/' + encodeURIComponent(ver) + '/details', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          releaseSummary: val('nr-releaseSummary'), highlights: val('nr-highlights'),
          knownIssues: val('nr-knownIssues'), acknowledgements: val('nr-acknowledgements')
        })
      });
      if (msg) { msg.textContent = r.ok ? 'Saved.' : 'Save failed.'; msg.style.color = r.ok ? '' : 'var(--rm-danger)'; }
    } catch (e) {
      if (msg) { msg.textContent = 'Save failed.'; msg.style.color = 'var(--rm-danger)'; }
    }
  };

  // §8.1: auto-resolve the milestone number by version-title match as soon as the operator finishes typing
  // the version, so the New-Release form field arrives pre-filled (still user-overridable before Start).
  const nrVersionInput = document.getElementById('nr-version');
  if (nrVersionInput) {
    nrVersionInput.addEventListener('blur', async () => {
      const v = nrVersionInput.value.trim();
      const msEl = document.getElementById('nr-milestone');
      if (!v || !msEl || msEl.value) return; // don't clobber an already-filled/overridden value
      try {
        const r = await fetch('/rest/milestones/' + encodeURIComponent(v) + '/resolve');
        const data = await r.json();
        if (data && data.milestoneNumber != null) msEl.value = data.milestoneNumber;
      } catch (e) {
        // Best-effort pre-fill only; the operator can still type the milestone number manually.
      }
    });
  }

  const layout = document.querySelector('.rm-rail-layout');
  if (!layout) return;  // start form only; no active run yet

  const version = layout.dataset.version;
  const rc = layout.dataset.rc;
  const mode = layout.dataset.mode || 'SAFE';
  const metaEl = document.getElementById('nr-step-meta');
  const STEP_META = metaEl ? JSON.parse(metaEl.textContent) : {};

  // The apply affordance is mode-derived: SAFE simulates (command-log, no side effects), LIVE mutates.
  const runLabel = mode === 'LIVE' ? 'Run (LIVE)' : 'Simulate (SAFE)';

  // Exactly one EventSource at a time (spec §4/§7: one console visible at a time). Switching the selected
  // step closes the old connection and opens a new one against that step's own /events/{version}/{stepId}.
  let es = null;

  function statusOf(stepId) {
    const el = layout.querySelector('.rm-rail-item[data-step="' + stepId + '"]');
    if (!el) return 'pending';
    for (const c of el.classList) {
      if (c !== 'rm-rail-item' && c !== 'selected') return c;
    }
    return 'pending';
  }

  // Mirrors the mockup's renderActions(step): button set is entirely a function of current status
  // (spec §3 decisions #11/#12 — every button below is the SAME preview/apply call regardless of status).
  function renderActions(stepId, status) {
    const dryRun = '<button class="btn btn-outline btn-sm" onclick="nrDryRun(\'' + stepId + '\')">Dry-run</button>';
    if (status === 'running')
      return '<button class="btn btn-outline btn-sm" disabled>Dry-run</button> <button class="btn btn-primary btn-sm" disabled>Running&hellip;</button>';
    if (status === 'failed')
      return dryRun + ' <button class="btn btn-primary btn-sm" onclick="nrApply(\'' + stepId + '\')">Resume</button>';
    if (status === 'awaiting-vote')
      return renderVoteGateActions();
    if (status === 'awaiting-review')
      return dryRun + ' <button class="btn btn-primary btn-sm" onclick="nrConfirmReview(\'' + stepId + '\')">Confirm review</button>';
    if (status === 'succeeded')
      return dryRun + ' <button class="btn btn-outline btn-sm" onclick="nrApply(\'' + stepId + '\')">Re-run</button>';
    if (status === 'skipped')
      return dryRun + ' <button class="btn btn-outline btn-sm" onclick="nrApply(\'' + stepId + '\')">Run anyway</button>';
    return dryRun + ' <button class="btn btn-primary btn-sm" onclick="nrApply(\'' + stepId + '\')">' + runLabel + '</button>';
  }

  // §5.15/§5.16: entering vote-gate only opens the vote (sets AWAITING_VOTE) — it is NOT itself the
  // advance action. The gate is only passed by recording a vote result (POST .../vote-result, which the
  // engine applies as the separate "tally-vote-result" step). SAFE has no real 72h wait or email tally to
  // read, so it gets a one-click "Simulate (SAFE)" that records a passing result outright; LIVE still
  // requires the operator to pick a real outcome and type the tally summary read off the vote thread.
  function renderVoteGateActions() {
    if (mode !== 'LIVE')
      return '<button class="btn btn-primary btn-sm" onclick="nrVoteResult(\'passed\')">' + runLabel + '</button>'
        + '<span class="rm-step-note">Records a simulated passing vote (no real 72h wait or tally) and advances the run.</span>';
    return '<div class="rm-vote-form">'
      + '<label>Vote outcome<select id="nr-vote-outcome"><option value="passed">Passed</option><option value="rejected">Rejected</option></select></label>'
      + '<label>Tally summary<textarea id="nr-vote-tally" rows="3" placeholder="+1/0/-1 counts, binding voters, read off the vote thread"></textarea></label>'
      + '<button class="btn btn-primary btn-sm" onclick="nrVoteResult()">Submit vote result</button>'
      + '</div>';
  }

  globalThis.nrVoteResult = async function (safeOutcome) {
    var outcome = safeOutcome;
    var tally = 'SAFE-mode simulated passing vote (no real tally read).';
    if (!outcome) {
      outcome = document.getElementById('nr-vote-outcome').value;
      tally = document.getElementById('nr-vote-tally').value;
    }
    const res = await post('/vote-result', { outcome, tally });
    if (res && res.success === false) { alert(res.message); return; }
    if (outcome === 'rejected') await nrDropRc(); // §8: a rejected vote forks to Drop-RC.
    location.reload();
  };

  // The selected step's title/status/mutating row — shared by nrSelect() (full detail-pane render) and
  // the live state-push patch (status-only refresh, below) so the two never drift apart.
  function topRowHtml(stepId, status) {
    const meta = STEP_META[stepId] || { title: stepId, mutating: false };
    return '<h3>' + meta.title + '</h3>' +
      '<span class="tag status ' + status + '">' + status + '</span>' +
      (meta.mutating ? ' <span class="tag mutating">mutating</span>' : '');
  }

  function connectConsole(stepId) {
    if (es) es.close();
    const consoleEl = document.getElementById('nr-console');
    if (!consoleEl) return;
    consoleEl.textContent = '';
    // Replay-then-tail, scoped to this one step (SseLogServlet, Task 8).
    es = new EventSource('/events/' + encodeURIComponent(version) + '/' + encodeURIComponent(stepId));
    es.onmessage = (e) => { consoleEl.textContent += e.data + '\n'; consoleEl.scrollTop = consoleEl.scrollHeight; };
  }

  globalThis.nrSelect = function (stepId) {
    layout.querySelectorAll('.rm-rail-item').forEach((el) => el.classList.toggle('selected', el.dataset.step === stepId));
    const status = statusOf(stepId);
    document.getElementById('nr-detail').innerHTML =
      '<div class="rm-step-title-row" id="nr-detail-title-row">' + topRowHtml(stepId, status) + '</div>' +
      '<div class="rm-actions-row" id="nr-actions">' + renderActions(stepId, status) + '</div>' +
      '<div class="rm-console-label"><span>Console output &mdash; this step only</span></div>' +
      '<pre id="nr-console" class="rm-console"></pre>';
    connectConsole(stepId);
  };

  async function post(path, body) {
    const r = await fetch('/rest/runs/' + version + path, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body || {})
    });
    if (!r.ok) {
      const t = await r.text().catch(() => '');
      return { success: false, message: 'HTTP ' + r.status + (t ? ': ' + t.slice(0, 300) : '') };
    }
    try { return await r.json(); }
    catch (e) { return { success: false, message: 'Unexpected non-JSON response from server.' }; }
  }

  globalThis.nrDryRun = async function (stepId) {
    const p = await post('/steps/' + stepId + '/preview', {});
    // MVP surfacing of preview text; a richer inline preview panel (vs. this alert) is a fast follow, not
    // required for the pipeline itself to work correctly.
    alert((p.lines || []).join('\n'));
  };

  globalThis.nrApply = async function (stepId) {
    // Steps needing inputs (developmentVersion, confirmVersion, voteOutcome, repoIdOverride, checklist) read here.
    const form = {};
    const dv = document.getElementById('nr-devversion');
    if (dv?.value) form.developmentVersion = dv.value;
    if (stepId === 'nexus-release') form.confirmVersion = prompt('Type the version to confirm release');
    // Same call whether the button said Run / Resume / Re-run (spec decision #11/#12) — hits /apply, not
    // /resume, for all three; /resume (Task 19) remains available as an equivalent alias.
    const res = await post('/steps/' + stepId + '/apply', form);
    // A forward-apply guard refusal (unsatisfied required predecessor) or a finalize-run refusal (some
    // required step still not terminal) comes back as success:false — surface it instead of silently
    // reloading into a run that looks unchanged and leaving the operator to guess why nothing happened.
    if (res && res.success === false) { alert(res.message); return; }
    location.reload();
  };

  globalThis.nrArm = async function () {
    const confirm = prompt('Type "' + version + ' LIVE" to arm this run for live mutation');
    if (!confirm) return;
    const res = await post('/arm', { confirm });
    if (res && res.message) alert(res.message);
    location.reload();
  };

  globalThis.nrConfirmReview = async function (stepId) {
    await post('/steps/' + stepId + '/confirm-review', {});
    location.reload();
  };

  globalThis.nrDropRc = async function () {
    const preview = await post('/drop-rc/preview', {});
    if (!confirm((preview.lines || []).join('\n'))) return;
    const confirmRc = prompt('Type the RC identifier to confirm (e.g. RC' + rc + ')');
    await post('/drop-rc/apply', { reason: 'vote rejected', confirmRc });
    location.reload();
  };

  // Live rail updates: a second EventSource, independent of the per-step console one above, so every
  // connected New-Release tab — including a passive second browser that never clicks anything — tracks
  // run/step status as it changes, with no polling or page reload. Opened once for the page's lifetime
  // (not reopened on step reselection); never touches #nr-console or its own EventSource.
  const STATUS_CLASS = (s) => s.toLowerCase().replace(/_/g, '-');

  // Swaps el's single status class for statusClass, leaving its other (non-status) classes alone. Used on
  // both .rm-rail-item (base classes rm-rail-item/selected) and .tag.status spans (base classes tag/status).
  function applyStatusClass(el, statusClass) {
    const keep = new Set(['rm-rail-item', 'selected', 'tag', 'status']);
    Array.from(el.classList).forEach((c) => { if (!keep.has(c)) el.classList.remove(c); });
    el.classList.add(statusClass);
  }

  function applySnapshot(snap) {
    if (!snap || snap.version !== version) return; // scoped by version already; ignore anything stray

    const runStatusEl = document.getElementById('nr-run-status');
    if (runStatusEl) { applyStatusClass(runStatusEl, STATUS_CLASS(snap.status)); runStatusEl.textContent = snap.status; }
    const titleEl = document.getElementById('nr-run-title');
    if (titleEl) titleEl.textContent = snap.version + ' \u00b7 RC' + snap.rc;
    const armArea = document.getElementById('nr-arm-area'); // only rendered server-side in LIVE mode
    if (armArea) {
      armArea.innerHTML = snap.armed
        ? '<span class="tag armed" title="This run is armed for live mutation">ARMED</span>'
        : '<button class="btn btn-warning" onclick="nrArm()">Arm this run</button>';
    }

    let selected = null;
    (snap.steps || []).forEach((s) => {
      const item = layout.querySelector('.rm-rail-item[data-step="' + s.stepId + '"]');
      if (!item) return;
      if (item.classList.contains('selected')) selected = s;
      applyStatusClass(item, STATUS_CLASS(s.status));
      const tagEl = item.querySelector('.tag.status');
      if (tagEl) { applyStatusClass(tagEl, STATUS_CLASS(s.status)); tagEl.textContent = s.status; }
    });

    // Keep the currently-selected step's own status tag + action buttons correct (e.g. running ->
    // succeeded unlocks Re-run; pending -> awaiting-vote swaps in the vote form) — but leave the console
    // <pre> and its EventSource alone; only reselecting a different step touches those (nrSelect above).
    if (selected) {
      const status = STATUS_CLASS(selected.status);
      const titleRow = document.getElementById('nr-detail-title-row');
      if (titleRow) titleRow.innerHTML = topRowHtml(selected.stepId, status);
      const actionsEl = document.getElementById('nr-actions');
      if (actionsEl) actionsEl.innerHTML = renderActions(selected.stepId, status);
    }
  }

  function connectState() {
    const stateEs = new EventSource('/events/' + encodeURIComponent(version) + '/state');
    stateEs.onmessage = (e) => {
      let snap;
      try {
        snap = JSON.parse(e.data);
      } catch (err) {
        return; // the servlet's "(no active run...)" placeholder line isn't JSON; ignore it quietly
      }
      applySnapshot(snap);
    };
  }
  connectState();

  // Default selection on load: a step awaiting human input (review or vote) is the most urgent — without
  // this, a step that just flipped to AWAITING_REVIEW/AWAITING_VOTE is invisible behind whichever later
  // PENDING step the old running/failed/pending-only chain fell through to, hiding its Confirm review /
  // vote-status controls. Then: the running step (mirrors option-a-rail.html's mockup default); else the
  // first failed step; else the first pending step; else the last (terminal) step.
  const initial = layout.querySelector('.rm-rail-item.awaiting-review')
    || layout.querySelector('.rm-rail-item.awaiting-vote')
    || layout.querySelector('.rm-rail-item.running')
    || layout.querySelector('.rm-rail-item.failed')
    || layout.querySelector('.rm-rail-item.pending');
  const all = layout.querySelectorAll('.rm-rail-item');
  const fallback = all.length ? all[all.length - 1] : null;
  const toSelect = initial || fallback;
  if (toSelect) nrSelect(toSelect.dataset.step);
})();
