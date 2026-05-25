# TODO-71: Doc-site local build script + Docusaurus search swap

Source: TODO.md headline bullet; expanded 2026-05-24.

## Goal

Two distinct sub-deliverables in one TODO. Both target the `juneau-docs` repo and together move the doc site from a push-driven GitHub Action build into a deliberate, locally-driven publish flow with a self-hosted search index:

1. **Replace the push-triggered GitHub Action with a local Python script.** Today the doc site was rebuilt + redeployed by `.github/workflows/deploy-docs.yml` on every push that touched `juneau-docs/**` / `juneau-core/**` / `juneau-rest/**` / etc. on `master`. That coupling means a typo in a markdown commit triggers a full Maven site build + Docusaurus rebuild + force-push to `asf-staging` — wasted CI minutes and zero human control over when a publish happens. Move the build to a local Python script (`scripts/build-docs.py` already exists) and the publish to a local Python script (`scripts/release-docs-stage.py` / `scripts/release-docs.py` already exist); humans run the script when they actually want to publish.
2. **Swap the Docusaurus search plugin to `@easyops-cn/docusaurus-search-local`.** Today the site uses Algolia DocSearch (configured in `docusaurus.config.ts`). Algolia adds an external-service dependency, requires API-key management, and crawls the deployed site rather than the source — local-build-driven search avoids all three.

End-state developer experience:

```bash
# From juneau-docs/ root — build locally, no GitHub Action involved.
python3 scripts/build-docs.py            # full build (npm + Maven site + Docusaurus)
python3 scripts/build-docs.py --dry-run  # report what would be built; do nothing
python3 scripts/release-docs-stage.py    # publish to asf-staging when ready
```

```ts
// docusaurus.config.ts — old (Algolia) → new (local-search) shape
themes: [
  ['@easyops-cn/docusaurus-search-local', {
    hashed: true,
    language: ['en'],
    docsRouteBasePath: '/docs',
    indexBlog: true,
    indexPages: true,
  }],
],
// (delete the entire baseThemeConfig.algolia { ... } block)
```

## Why now

- **GitHub-Action coupling has zero human control.** A push to `master` that touches anything under `juneau-docs/**` (including this TODO file in `todo/` if added there in the future, or any markdown comment fix) triggers a full Maven `site` build (~5 min) + Docusaurus build (~1 min) + force-push to `asf-staging`. Operators have repeatedly wanted to batch doc changes into a single publish; the GitHub Action makes that impossible.
- **The script already exists** — `juneau-docs/scripts/build-docs.py` is a full-featured replacement (npm install, Maven compile + site, versioned-javadoc copy, Docusaurus build, link checking). The `/docs build` Cursor command already invokes it. What's missing is the explicit "this is the new flow" cutover: removing the disabled GitHub Action, refreshing docs/READMEs, and adding `--dry-run` / `--verbose` for ergonomics. **See "Research findings" below.**
- **Algolia DocSearch indexes the deployed site** — which on a force-push-driven publish flow means the index is always lagging behind master. Local-search builds the index from the source at build time, so search is always in sync with the docs that were just deployed.
- **No external service dependency on Algolia.** One less secret to rotate (the API key in `docusaurus.config.ts` is the public crawler-issued key, but the project still has an admin-issued key registered at Algolia that someone has to own); one less third-party SLA to track.

## Research findings (verified 2026-05-24)

Significant state to flag before the plan's phases run:

1. **The GitHub Action is already disabled.** `.github/workflows/deploy-docs.yml.disabled` exists at the renamed path — someone preemptively disabled it without removing it. The plan still needs to decide between **delete** (resolved-decision recommendation, see below) and **keep-as-disabled-archive**.
2. **`build-docs.py` already exists** at `juneau-docs/scripts/build-docs.py` with a full Maven-site + Docusaurus + javadoc-version pipeline. The disabled workflow even called `python3 scripts/build-docs.py` directly, so the GitHub Action was already a thin wrapper around the script. Sub-deliverable A is therefore mostly a documentation + cleanup change, not a script-authoring change.
3. **The publish step is `release-docs-stage.py` / `release-docs.py`**, not part of `build-docs.py`. `build-docs.py` builds locally into `juneau-docs/build/`; the release scripts check out the `asf-staging` / `asf-site` branch in a temp dir, copy the build output in, commit, and push. This split matters for the plan's `--dry-run` behavior — `build-docs.py --dry-run` should print the build it would run; `release-docs-stage.py --no-push` already exists for the publish side (verified at line 162 of that script).
4. **The `/docs` Cursor command already documents the new flow.** `.cursor/commands/docs.md` lines 48-58 already point users at `python3 scripts/build-docs.py`. The plan's Phase 1.4 (update `/docs` command) is a no-op unless we want to add `--dry-run` documentation.
5. **`juneau-docs/README.md` does NOT mention the script flow.** This is a real doc gap — `README.md` only points at the official website and "automatic updates"; it doesn't explain how a contributor builds + publishes the site. Phase 1.3 must fill this gap.
6. **No `juneau-docs/.github/workflows/` directory exists.** The disabled workflow lives in `juneau/.github/workflows/`, not in `juneau-docs/`. Sub-deliverable A's "disable strategy" therefore concerns the `juneau` repo, not `juneau-docs`.

## Scope

**Sub-deliverable A — local build/publish flow (in scope):**

- Delete `juneau/.github/workflows/deploy-docs.yml.disabled`.
- Add `--dry-run` and `--verbose` flags to `juneau-docs/scripts/build-docs.py`. Current flags: `--skip-npm`, `--skip-maven`, `--skip-copy`, `--staging`. `--dry-run` prints the steps it would execute; `--verbose` enables stage-by-stage banner logging.
- Add a "now run `python3 scripts/release-docs-stage.py` to publish" reminder at the end of a successful `build-docs.py` run (only when no `--skip-*` flag was passed and the build succeeded).
- Document the new flow in `juneau-docs/README.md` under a new "Building the docs site" section.
- Document the script + the disabled-Action cutover in `juneau/AGENTS.md` under "Script Shortcut Commands" alongside the existing `/docs` Cursor shortcut. Two new shortcut entries: **"build docs"** → `juneau-docs/scripts/build-docs.py`, **"stage docs"** → `juneau-docs/scripts/release-docs-stage.py`.
- No change to `.cursor/commands/docs.md` (already points at the script); optionally add the new `--dry-run` flag to the documented examples.

**Sub-deliverable B — Docusaurus search swap (in scope):**

- Add `@easyops-cn/docusaurus-search-local` to `juneau-docs/package.json` `devDependencies` (the plugin is a build-time theme, not a runtime dep).
- Remove the `algolia: { ... }` block from `baseThemeConfig` in `juneau-docs/docusaurus.config.ts` (lines 47-76).
- Add the new `themes: [ ... ]` block to the top-level `Config` in `docusaurus.config.ts` per the plugin docs.
- Verify `npm run build` succeeds with the new plugin; measure the size of the produced search index (`build/search-index-*.json`) and capture it in the PR description.
- Revoke the existing Algolia DocSearch crawler API key at the Algolia console (operator step, captured in the plan checklist).

**Explicitly out of scope (v1):**

- i18n / multi-language search. Index built with `language: ["en"]` for v1; future i18n introduction is a separate change.
- Server-side / pre-rendered search results. The plugin ships a client-side index; that's the trade-off being made vs. Algolia's server-side index.
- Re-platforming away from Docusaurus. Not on the table for this TODO.
- Replacing `release-docs-stage.py` / `release-docs.py` with a single combined script. They already work; combining them is a separate cleanup.
- Adding CI checks on `juneau-docs` PRs that run `build-docs.py --skip-maven` to verify Docusaurus still builds. Separate ergonomics improvement; flag as a follow-on.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Verify the `.disabled` workflow file is in fact disabled (no other workflow references `deploy-docs`). Already done — only `maven.yml`, `codeql-analysis.yml`, and `sonarcloud.yml` are active on `master`.
2. Verify `build-docs.py` runs to completion locally on a clean checkout (run as a one-off; do not commit any output).
3. Verify `release-docs-stage.py --no-push` produces a staged commit on a local branch and confirms the working tree is clean afterwards (do not push).
4. Verify the current Algolia index URL (`indexName: 'juneau_docsJuneau Website'`) actually points at a real index and capture the admin who owns it (probably an ASF infra ticket / committer with Algolia admin access).

### Phase 1 — local build/publish flow (sub-deliverable A)

1. **Delete the disabled workflow.** `git rm juneau/.github/workflows/deploy-docs.yml.disabled`. Resolved decision #3 — delete vs. rename — see "Resolved decisions" below.
2. **Add `--dry-run` and `--verbose` to `build-docs.py`.** When `--dry-run` is set, every `run_command(...)` invocation is replaced by a `print(f"[dry-run] would run: {cmd}")` line; no Maven, no npm, no shutil-tree operations. When `--verbose` is set, the existing banner-style step headers stay; without it, switch to a single concise line per stage.
3. **Add a "now run `release-docs-stage.py`" reminder.** After the existing `print("=== Documentation build complete ===")` line, conditionally print: `"To publish: python3 scripts/release-docs-stage.py [--no-push]"` (skipped under `--dry-run` and when any `--skip-*` flag was passed).
4. **Update `juneau-docs/README.md`.** New top-level "Building the docs site" section after "Documentation & Resources" with: prerequisites (Node 18+, Maven, Java 17); the `build-docs.py` / `release-docs-stage.py` / `release-docs.py` invocation matrix; a pointer at `/docs` Cursor command for AI-driven flows.
5. **Update `juneau/AGENTS.md`.** Under "Script Shortcut Commands", add two new entries between **"start docs"** and **"revert staged"**:
    - **"build docs"** — runs `juneau-docs/scripts/build-docs.py` (full local build).
    - **"stage docs"** — runs `juneau-docs/scripts/release-docs-stage.py` (publish to `asf-staging`).
6. **Smoke test the script.** Run `python3 scripts/build-docs.py --dry-run` to confirm the new flag wires through; run a real build with `--verbose` to confirm verbose output is sensible.

### Phase 2 — Docusaurus search swap (sub-deliverable B)

1. **Add the dep.** `cd juneau-docs && npm install --save-dev @easyops-cn/docusaurus-search-local`. Verify `package.json` + `package-lock.json` reflect the addition; commit both.
2. **Remove the Algolia block** from `docusaurus.config.ts` `baseThemeConfig` (lines 47-76 inclusive). Keep `navbar` / `footer` / `prism` untouched.
3. **Add the `themes` array** to the top-level `Config` object — alongside `presets`:
    ```ts
    themes: [
      ['@easyops-cn/docusaurus-search-local', {
        hashed: true,
        language: ['en'],
        indexBlog: true,
        indexPages: true,
        docsRouteBasePath: '/docs',
      }],
    ],
    ```
    Exact option set is the easyops docs' recommended defaults for a docs-heavy site; `hashed: true` reduces bundle bloat.
4. **Smoke test locally.**
    - `npm run start` — confirm the search bar renders, common queries (`BasicSwaggerResource`, `@Rest(mixins=...)`, "static files") return reasonable results.
    - `npm run build` — confirm a clean production build. Inspect `build/search-index-*.json` size; record in PR description.
5. **Revoke the Algolia crawler API key.** Operator step, after the swap lands. Capture in plan checklist; not gated by the PR itself.

### Phase 3 — docs cleanup + release notes

1. Add a release-notes entry under `juneau-docs/pages/release-notes/9.5.0.md` (or whichever version is current at land time) noting both changes:
    - "Local-build flow: doc-site builds are now run locally via `scripts/build-docs.py`; the push-triggered GitHub Action has been retired."
    - "Search: swapped Algolia DocSearch for `@easyops-cn/docusaurus-search-local` — search index is now built at site-build time, no external service dependency, no API-key management."
2. If `pages/topics/` has a doc-contribution page, cross-reference the new flow. (Skip if there isn't one — out of scope to author it here.)
3. Verify the `/docs` Cursor command still works end-to-end after the changes (`/docs build`, `/docs start`).

## Acceptance criteria

- [ ] `juneau/.github/workflows/deploy-docs.yml.disabled` deleted.
- [ ] `juneau-docs/scripts/build-docs.py` supports `--dry-run` and `--verbose` flags.
- [ ] On successful real-build (no `--skip-*` flags), `build-docs.py` prints a reminder pointing at `release-docs-stage.py`.
- [ ] `juneau-docs/README.md` has a "Building the docs site" section.
- [ ] `juneau/AGENTS.md` lists **"build docs"** and **"stage docs"** in "Script Shortcut Commands".
- [ ] `@easyops-cn/docusaurus-search-local` is in `juneau-docs/package.json` `devDependencies`.
- [ ] `juneau-docs/docusaurus.config.ts` no longer references Algolia (`appId` / `apiKey` / `indexName` removed).
- [ ] `npm run build` in `juneau-docs/` succeeds with the new plugin configured.
- [ ] Local-search returns reasonable results for `BasicSwaggerResource`, `@Rest(mixins=...)`, "static files" smoke queries.
- [ ] Index-size measurement recorded in the PR description.
- [ ] Algolia crawler API key revoked at the Algolia console (operator confirms post-land).
- [ ] Release-notes entry added under `juneau-docs/pages/release-notes/<version>.md`.

## Resolved decisions

All previously open questions resolved 2026-05-24.

1. **Script language — Python 3.** Matches existing tooling (`scripts/push.py`, `scripts/test.py`, `scripts/coverage.py`, `scripts/sonarqube.py`, `scripts/slack-notify.py`, and the existing `build-docs.py` / `release-docs-stage.py` / `release-docs.py` already in `juneau-docs/`).
2. **Script home — `juneau-docs/scripts/`.** Already exists there. The script operates on the docs repo and on a sibling `master` checkout for Maven site generation (per the script's `find_master_branch_sibling(...)` logic at lines 50-84); putting it in the docs repo is the natural home.
3. **Disable strategy — delete.** Rename-to-`.disabled` is the current state; deleting is safer than keeping a `.disabled` archive (nothing accidentally re-enables it, no confusion about whether the file is authoritative).
4. **`--dry-run` semantics — print the commands that would run.** Don't try to do a partial build; just enumerate the `subprocess.run(...)` calls with their arguments and exit 0.
5. **`--verbose` semantics — opt-in stage banners.** Default to one concise line per stage; `--verbose` keeps the existing multi-line banner style.
6. **Search plugin choice — `@easyops-cn/docusaurus-search-local`** (per user instruction). Single-package, well-maintained, Docusaurus 3-compatible, used by sibling Apache projects.
7. **Index language — `["en"]`** for v1. The docs site is English-only; no i18n yet. Future i18n introduction is a separate change.
8. **Clean swap, no fallback.** Don't try to keep the Algolia plugin alongside as a fallback — it would require dual configuration and double-publish overhead. Land the swap, monitor for two weeks, rip out Algolia at the registrar if all is well.
9. **Sub-deliverable A and B are independent** — no inter-sub-deliverable dependency. Can split into two PRs if desired; recommendation is one combined PR if both are ready, since both touch only `juneau-docs/*` + a small `juneau/AGENTS.md` edit.

## Open questions

1. **Search index size budget.** The local-search plugin ships the entire index in the deployed bundle. For Juneau's doc site (~100+ pages, including the auto-generated topic + release-notes content), what is the actual index size? Worth a quick measurement during Phase 2 step 4; if it exceeds, say, 5 MB, reconsider (compress, drop blog from index, etc.). Not a blocker; will surface a number in the PR description.
2. **Algolia crawler API-key ownership.** Who at ASF Infra holds the admin-level Algolia account for `juneau_docsJuneau Website`? The crawler-issued public key in `docusaurus.config.ts` is fine to commit, but the admin key (used to manage the index) is held by an ASF committer — verify before revoking so we don't lock someone out.
3. **CI smoke check.** Should a `juneau-docs` PR-time CI smoke run `python3 scripts/build-docs.py --skip-maven` (just the Docusaurus build, not the full Maven `site`) to catch markdown errors / broken plugin configs at PR time? Recommend yes as a follow-on TODO; out of scope for v1.

## Risks

- **Forgetting to publish.** Without the GitHub Action, a markdown change that lands on `master` will NOT automatically appear on `juneau.apache.org`. Mitigation: (a) the new `build-docs.py` end-of-run reminder mentions `release-docs-stage.py`; (b) AGENTS.md "Script Shortcut Commands" calls out "stage docs"; (c) the `/push` Cursor command (`.cursor/commands/push.md`) gets a note that doc-only commits to `juneau-docs` need an explicit stage step — verify and update during Phase 1.5.
- **Search-index bloat.** Local-search ships the entire index client-side. A ~100-page doc site is well within the plugin's comfortable range, but if the index grows pathologically (e.g. if all 100+ release notes get indexed at full text) the deployed bundle could balloon. Mitigation: measure post-build during Phase 2; configure `indexBlog: false` if needed; ultimately, swap back to Algolia is a one-PR rollback.
- **Algolia crawler key revocation timing.** Revoke too early and the Algolia-indexed search breaks before the local-search swap is live in production; revoke too late and we ship two search systems for a while. Mitigation: the resolved-decision sequence is — land the swap, wait for staged build to publish to `asf-staging`, verify search works there, promote to `asf-site`, THEN revoke the Algolia key.
- **`build-docs.py`'s sibling-checkout assumption.** The script expects a sibling `master` clone (per `find_master_branch_sibling(...)` at lines 50-84). Contributors who have a non-sibling layout will get a hard error. Mitigation: documented in `juneau-docs/README.md` Phase 1.4; the script's own error message already explains the expected layout.
- **Disabled-workflow archeology.** Once the `.disabled` file is gone, future contributors investigating "how did docs publish work before this?" have no on-disk reference. Mitigation: the release-notes entry plus this plan file capture the migration narrative; git history preserves the deleted file.

## Related work

- `juneau-docs/scripts/build-docs.py` — the existing local-build script (no authoring needed, just `--dry-run` / `--verbose` + reminder line).
- `juneau-docs/scripts/release-docs-stage.py` — existing staging-publish script (no authoring needed).
- `juneau-docs/scripts/release-docs.py` — existing production-promote script (no authoring needed).
- `juneau/.github/workflows/deploy-docs.yml.disabled` — to be deleted.
- `juneau/.cursor/commands/docs.md` — already documents the script-driven flow; light update for `--dry-run` / `--verbose` documentation if desired.
- `juneau/.cursor/commands/push.md` — verify it covers doc-only commits to `juneau-docs` and points at `release-docs-stage.py`.
- `juneau/scripts/push.py` — the authoring-style reference for the new flags (argparse layout, exit-code discipline).
- `juneau-docs/docusaurus.config.ts` lines 47-76 — the Algolia block to remove.
- `juneau-docs/package.json` — the deps file to update.
- TODO.md line 36 — the headline bullet this plan expands.
