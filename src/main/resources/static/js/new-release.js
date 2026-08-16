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
  // Defined unconditionally so the start form (rendered when `run` is null, before `.rm-rail-layout`
  // exists) always has it available.
  globalThis.nrStart = async function () {
    const v = document.getElementById('nr-version').value;
    const dev = document.getElementById('nr-devversion').value;
    const msEl = document.getElementById('nr-milestone');
    const ms = msEl && msEl.value ? Number(msEl.value) : null;
    await fetch('/rest/runs', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ version: v, developmentVersion: dev, milestoneNumber: ms })
    });
    location.reload();
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
      return '<span class="rm-step-note">Pipeline is paused pending the vote outcome. Use any step\u2019s own controls to re-check it individually while you wait.</span>';
    if (status === 'awaiting-review')
      return dryRun + ' <button class="btn btn-primary btn-sm" onclick="nrConfirmReview(\'' + stepId + '\')">Confirm review</button>';
    if (status === 'succeeded')
      return dryRun + ' <button class="btn btn-outline btn-sm" onclick="nrApply(\'' + stepId + '\')">Re-run</button>';
    if (status === 'skipped')
      return dryRun + ' <button class="btn btn-outline btn-sm" onclick="nrApply(\'' + stepId + '\')">Run anyway</button>';
    return dryRun + ' <button class="btn btn-primary btn-sm" onclick="nrApply(\'' + stepId + '\')">' + runLabel + '</button>';
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
    const meta = STEP_META[stepId] || { title: stepId, mutating: false };
    const status = statusOf(stepId);
    document.getElementById('nr-detail').innerHTML =
      '<div class="rm-step-title-row"><h3>' + meta.title + '</h3>' +
      '<span class="tag status ' + status + '">' + status + '</span>' +
      (meta.mutating ? ' <span class="tag mutating">mutating</span>' : '') + '</div>' +
      '<div class="rm-actions-row" id="nr-actions">' + renderActions(stepId, status) + '</div>' +
      '<div class="rm-console-label"><span>Console output &mdash; this step only</span></div>' +
      '<pre id="nr-console" class="rm-console"></pre>';
    connectConsole(stepId);
  };

  async function post(path, body) {
    const r = await fetch('/rest/runs/' + version + path, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body || {})
    });
    return r.json();
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
    await post('/steps/' + stepId + '/apply', form);
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

  // Default selection on load: the running step is most informative (mirrors option-a-rail.html's mockup
  // default); else the first failed step; else the first pending step; else the last (terminal) step.
  const initial = layout.querySelector('.rm-rail-item.running')
    || layout.querySelector('.rm-rail-item.failed')
    || layout.querySelector('.rm-rail-item.pending');
  const all = layout.querySelectorAll('.rm-rail-item');
  const fallback = all.length ? all[all.length - 1] : null;
  const toSelect = initial || fallback;
  if (toSelect) nrSelect(toSelect.dataset.step);
})();
