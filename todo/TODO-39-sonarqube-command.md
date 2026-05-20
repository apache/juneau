# TODO-39: `/sonarqube` Cursor command — per-file/package/module SonarQube findings triage

Source: promoted from `TODO.md` on 2026-05-20.

## Goal

Add a `/sonarqube` Cursor command (and matching `scripts/sonarqube.py` helper) that prints a concise per-file summary of SonarQube findings (rule id, severity, line, message) for a given source file, package directory, or Maven module — the same way `/coverage` + `scripts/coverage.py` does for JaCoCo. The goal is to let the user triage Sonar findings from the terminal/chat in seconds, mirroring the existing JaCoCo flow.

This is a tooling/workflow TODO; it does not change any Java source or `pom.xml`.

## Why now

- Apache Juneau already publishes Sonar analyses to **SonarCloud** under project key `apache_juneau` (org `apache`) via the `.github/workflows/sonarcloud.yml` CI job that runs on every push to `master` and every PR, so server-side findings are always reasonably fresh (typically same-day).
- The existing local workflow is **manual** and **stale**: a developer downloads `SonarQubeIssues.txt` (TSV export) from sonarcloud.io into `~/Downloads/`, then runs `scripts/view-sonar-category.py` against `~/Downloads/SonarQubeIssues.categorized.json`. The categorize step itself (`scripts/categorize-sonar-issues.py`) is **referenced from `scripts/README_SONARQUBE.md` but no longer present in `scripts/`** — git log shows only legacy commits (e.g. `1585c82792 SonarQube bug fixes`) that touched these files, and the README points at a file path that does not exist on disk. The current path is broken for any developer who didn't already have the JSON cached locally.
- AGENTS.md "suppress warnings" workflow already assumes Sonar findings get surfaced **inside the IDE** via the SonarLint plugin connected to SonarCloud (see `.sonarlint/connectedMode.json` — `apache_juneau` / `apache` / EU region). There is **no terminal-side equivalent** today, which is the gap this command fills.
- Coverage and Sonar are the two "quality dashboards" the project cares about. Coverage already has `/coverage` + `scripts/coverage.py`; Sonar should too, and the two helpers should feel symmetric.

## Data source: Eclipse MCP bridge

The `/sonarqube` command needs to surface Sonar findings on **uncommitted local changes** so a developer can triage before delivery. Earlier candidate data sources were ruled out:

- **SonarCloud Web API** — only knows about code CI has already analyzed (master push or PR build). Doesn't see the working tree.
- **SonarLint CLI** — the JAR (`org.sonarsource.sonarlint.core:sonarlint-backend-cli`) has no `main()`; it's a JSON-RPC backend driven by IDE plugins, not a shell entrypoint.
- **Local SonarQube via Docker** — heavy infra to stand up per developer.
- **Cached `SonarQubeIssues.categorized.json`** — stale by definition, depends on a manual TSV export, and the `categorize-sonar-issues.py` half of the legacy flow is missing from `scripts/` today.

What we do have: every developer on this project already runs Eclipse with the **SonarLint Eclipse plugin** connected to SonarCloud (`.sonarlint/connectedMode.json`). SonarLint analyzes files on open / save / autosave and writes results into Eclipse's **Problems** view as `IMarker`s of subtype `IMarker.PROBLEM`. The proposed data source is to read those markers from Eclipse via an MCP bridge plugin.

### Surveyed Eclipse MCP plugins

| Plugin | Reads Problems markers (SonarLint findings) | Triggers analysis | Transport | Last commit | Verdict |
|---|---|---|---|---|---|
| **[eclipse-agents/eclipse-agents](https://github.com/eclipse-agents/eclipse-agents)** | Yes — `listProblems` tool returns `IMarker.PROBLEM` subtree (depth-infinite, includes SonarLint markers) | No | HTTP + SSE | 2026-01-30 | **Recommended** |
| [maxmart/eclipse-mcp-server](https://github.com/maxmart/eclipse-mcp-server) | No — exposes `list_projects`, `build_project`, `launch`, `get_console_output`, etc. but no marker tool | No (build triggers JDT compile, not SonarLint specifically) | HTTP | 2026-02-23 | Runner-up |
| [sunix/jdtls-mcp](https://github.com/sunix/jdtls-mcp) | No — proxies Eclipse JDT Language Server over LSP. SonarLint is a separate plugin layer that doesn't surface through LSP diagnostics | No | stdio | 2026-03-16 | Wrong layer |
| Eclipse ECF MCP (`org.eclipse.ecf.ai.mcp.tools` in [ECF 3.16.5+](https://download.eclipse.org/rt/ecf/3.16.5/)) | n/a — a framework for *contributing* MCP tools, no built-in marker tool | n/a | varies | Active | Useful primitive but no built-in surface |

### Recommendation: `eclipse-agents`

Pre-release plugin developed under the Eclipse Foundation umbrella (initial contributions by IBM, EPL-2.0). It boots an MCP server inside the Eclipse VM and exposes a built-in `org.eclipse.agents.contexts.platform` package whose `Tools.java` defines these `@McpTool`s today: `currentSelection`, `listEditors`, `listConsoles`, `listProjects`, `saveEditor`, `listProblems`, `listTasks`. The one we care about is `listProblems`, which calls `IResource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)` — the `true` flag includes subtypes, so SonarLint marker types (`org.sonarlint.eclipse.sonarlintOnTheFlyProblem` and siblings, all of which extend `IMarker.PROBLEM`) are returned alongside JDT compile markers.

It wins on the only criterion that matters: it can return SonarLint findings from the local workspace. The other surveyed plugins either don't read markers at all (maxmart, jdtls-mcp) or are a framework rather than a finished surface (ECF).

#### Install (macOS)

1. Ensure Eclipse is the **Eclipse IDE for Enterprise Java and Web Developers, 2025-09** package (the eclipse-agents docs pin this; the bundled dependencies match).
2. Download `eclipse-agents` from the project's IBM Box download link advertised in the [README](https://github.com/eclipse-agents/eclipse-agents/blob/main/README.md) — there is no Eclipse update site yet.
3. Unzip the archive.
4. In Eclipse: **Help → Install New Software… → Add… → Archive…**, point at the unzipped `org.eclipse.mcp.update` folder, uncheck "Group items by category", install the feature, accept the unsigned-content warning, restart.
5. **Window → Preferences → Coding Agents → Agent Contexts**, check **Enable MCP HTTP Server**, apply, and click **Copy to clipboard** to capture the SSE endpoint URL (e.g. `http://127.0.0.1:<port>/mcp/sse`).

#### Cursor MCP config entry

The repo's MCP config lives at `~/.cursor/mcp.json` (per-user, not per-workspace — checked: no in-repo `.cursor/mcp.json` exists, and `~/.cursor/mcp.json` is where the other servers in this project are registered). Add a new entry alongside the existing ones (do **not** invent a transport — eclipse-agents speaks HTTP+SSE):

```json
"eclipse": {
  "url": "http://127.0.0.1:PORT/mcp/sse"
}
```

Replace `PORT` with the value from the eclipse-agents preference page. The exact suffix (`/mcp`, `/mcp/sse`, etc.) is what the preference-page "Copy to clipboard" button emits — use that verbatim.

#### Tool call the `/sonarqube` command will make

`tools/call` on tool name **`listProblems`**, parameters:

```json
{ "resourceURI": "file:///Users/james.bognar/git/apache/juneau/juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/SseParserSession.java" }
```

Or omit `resourceURI` to get problems for the whole workspace.

Expected response shape (per `org.eclipse.agents.contexts.platform.resource.ResourceSchema`):

```json
{
  "markers": [
    {
      "type": "Problem",
      "message": "Define a constant instead of duplicating this literal \"data:\" 4 times.",
      "charStart": 3214,
      "charEnd": 3220,
      "lineNumber": 134,
      "done": false,
      "location": "",
      "resource_link": { "uri": "file:///…/SseParserSession.java", "name": "SseParserSession.java", "mimeType": "text/x-java" },
      "id": 18273,
      "creationTime": 1716190000000,
      "severity": "WARNING",
      "priority": "NORMAL"
    }
  ]
}
```

### Known limitations (must be planned around)

1. **Pre-release.** The README explicitly says "This code base is a work in progress and is a pre-release." Schema and tool names may shift. Pin the version of the unzipped feature in a note in `AGENTS.md` once we adopt it.
2. **Install is a manual ZIP from IBM Box, not an Eclipse update site.** Adoption friction for any new contributor; doc it in `AGENTS.md`.
3. **`listProblems` returns *all* Problems-view markers, not just SonarLint.** JDT compile errors, Maven warnings, validation markers, etc. all share `IMarker.PROBLEM`. The `/sonarqube` script will need to filter client-side — but the current `MarkerAdapter.toJson()` does **not** expose the marker type id (e.g. `org.sonarlint.eclipse.sonarlintOnTheFlyProblem`), so filter-by-origin is not directly possible. Workarounds, in order of preference: (a) contribute a 50-line MCP tool to eclipse-agents (or to a small in-repo plugin) that calls `findMarkers("org.sonarlint.eclipse.sonarlintProblem", true, DEPTH_INFINITE)` and includes SonarLint custom attributes; (b) filter heuristically by absence of a Java compile signature; (c) pass through everything and let the user grep.
4. **Rule id (`java:Sxxx`) is not in the default response.** SonarLint stores it as the custom marker attribute `rulekey` (verified against `SonarSource/sonarlint-eclipse:org.sonarlint.eclipse.core/.../MarkerUtils.java`), and `MarkerAdapter.processMarker()` only extracts the *standard* `IMarker` attributes (message, charStart/End, lineNumber, severity, priority, location, done). Without the rule id, per-rule filtering (`--rule java:S1192`) and the `@SuppressWarnings("java:Sxxx")` hint flag can't work. Same workaround as (3) — a small contributed tool that reads `marker.getAttribute("rulekey")` alongside `sonarseverity`, `issuetype`, `sonarattribute`, `sonarhighestimpact`. This is the single biggest gap and likely the first follow-up.
5. **Does not trigger SonarLint analysis.** Eclipse-agents only *reads* markers; it has no `analyze` tool. We rely on SonarLint's automatic analysis (on file open / save / autosave — default ON). To force-refresh, the user has to open or save the file in Eclipse, or right-click → **SonarQube → Analyze**. The `/sonarqube` command can call eclipse-agents' `saveEditor` tool as a soft refresh nudge for files that are already open and dirty, but it cannot make Eclipse re-analyze a file the user hasn't touched.
6. **Eclipse must be running with the Juneau workspace imported.** No headless mode. If the developer hasn't opened Eclipse on this project, `/sonarqube` has to fall back to SonarCloud Web API (server-side, last-CI-analysis data) and say so in its header line.

### Runner-up: `maxmart/eclipse-mcp-server`

Loses cleanly. Its tool set is `list_projects`, `list_launch_configs`, `list_launches`, `launch`, `terminate`, `refresh_project`, `build_project`, `get_console_output`, `run` — useful for orchestrating builds and launches from an external agent, but it does not expose `IMarker`/Problems-view contents at all. There is no path from this plugin to SonarLint findings.

### Open questions for James

1. **Confirm Eclipse 2025-09 J2EE package.** The eclipse-agents install steps assume that exact package because it ships the right transitive plugin dependencies. Is your current Eclipse install that one, or older?
2. **Pre-release tolerance.** eclipse-agents is explicitly pre-release and last-touched in late January 2026. OK to depend on it for an internal tooling command, or do you want a fallback path written from day one?
3. **Rule-id strategy.** The single biggest gap is that `MarkerAdapter` doesn't surface SonarLint's custom `rulekey` attribute. Three paths: (a) contribute a tool upstream to eclipse-agents (medium effort, helps everyone), (b) build a tiny in-repo Eclipse plugin under `juneau-tools/` or similar that adds a `listSonarProblems` MCP tool with the SonarLint attributes (small effort, project-owned), or (c) ship v1 without rule-id-level filtering and add it later. Recommendation: (b), because it's small and gives us exactly the schema we want.
4. **Trigger strategy.** Accept "results are as fresh as SonarLint has last analyzed" (relies on autoanalyze-on-save), or have `/sonarqube` proactively call `saveEditor` on the target file before reading markers (only works for files already open in Eclipse), or document a manual "right-click → SonarQube → Analyze changed files" pre-step in the trigger phrases?
5. **Fallback when Eclipse isn't running.** Fall through to the SonarCloud Web API (the original plan, captured in this file's downstream sections) with a header note that data is from last CI analysis and may miss local edits? Recommended: yes — having one command name (`/sonarqube`) with two data layers (Eclipse for local edits, SonarCloud for committed/pushed) is easier than two commands.
6. **MCP server discovery.** The eclipse-agents preference page picks a port number; do we want a `scripts/eclipse-mcp-port.py` helper that reads the port out of Eclipse's preferences file (`~/.eclipse/…/org.eclipse.agents.prefs`) so the Cursor MCP config doesn't need a hand-edit every Eclipse install, or is "paste the URL once" acceptable?

### Note on downstream sections

The "Spec essentials", "Deliverables", "Design notes", and "Verification" sections below this point were written against the original SonarCloud Web API plan. If James signs off on the Eclipse MCP bridge as the v1 path, those sections (cache layout, flag table, `directories=` filter logic, etc.) all need to be re-aligned. Holding off on that edit until the data-source decision is locked.

## Spec essentials

### Argument shape (mirrors `/coverage`)

```bash
python3 scripts/sonarqube.py <path> [options]
```

- `<path>` — a source file (`.java`), a package directory under `src/main/java` / `src/test/java`, or a Maven module root (any directory containing a `pom.xml`). Absolute or repo-relative.
- Module auto-detection walks up from `<path>` to the nearest directory containing a `pom.xml` (excluding the repo root itself — same logic as `scripts/coverage.py` `find_maven_module()`).
- Path → Sonar component mapping uses `<module-relative-path>` as the `directories=` / `files=` filter; the `componentKeys=apache_juneau` global filter scopes to this project.

### Flags

| Flag | Meaning |
|---|---|
| `--run` / `-r` | Re-fetch issues from the SonarCloud Web API and overwrite the local cache. Default: reuse the local cache if present. |
| `--severity {BLOCKER,CRITICAL,MAJOR,MINOR,INFO}` | Filter to one or more severities (comma-separated). Default: all. |
| `--rule <java:Sxxx>` | Filter to a specific rule id (repeatable). Example: `--rule java:S1192` to only show string-literal-duplication findings. |
| `--type {CODE_SMELL,BUG,VULNERABILITY,SECURITY_HOTSPOT}` | Filter to one or more issue types (comma-separated). Default: all. |
| `--branch <branch>` | Query a different SonarCloud branch (defaults to `master`). Useful for inspecting PR results. |
| `--with-suppress-hint` | Append a `→ @SuppressWarnings("java:Sxxx")` line to each finding's output. Default: off (verbose). |
| `--max <N>` | Cap the number of findings printed (default: 200; matches `/coverage` "don't dump huge results"). |
| `--help` / `-h` | Show help. |

**No `--no-cache` flag** — omitting `--run` already implies "reuse cache". Mirrors `/coverage`'s "there is no `--no-run`" convention.

### Output shape (mirrors `/coverage`)

Header summary, then one block per source file (sorted by path), terminated by a TOTAL block when more than one file is reported. Per-file block:

```
======================================================================
  juneau-core.juneau-marshall.org.apache.juneau.sse.SseParserSession
======================================================================
  Findings:  [######..............]  6 issues  (1 critical, 2 major, 3 minor)

  line  85:  java:S2095  CRITICAL  Use try-with-resources or close this "Reader" in a "finally" clause.
  line 112:  java:S3776  MAJOR     Cognitive Complexity of method is 16 (max 15).
  line 134:  java:S1192  MAJOR     Define a constant instead of duplicating this literal "data:" 4 times.
  ...
```

With `--with-suppress-hint`, each line is followed by a hint line indented two extra spaces, e.g.:

```
  line  85:  java:S2095  CRITICAL  Use try-with-resources or close this "Reader" in a "finally" clause.
                                   → @SuppressWarnings("java:S2095") // rationale here
```

- Progress-bar widget reused from `scripts/coverage.py` `bar()` (visual symmetry).
- Severity color codes are **optional** and gated on `sys.stdout.isatty()` — `BLOCKER` red, `CRITICAL` red, `MAJOR` yellow, `MINOR` cyan, `INFO` dim. **Do not** emit ANSI codes when piped to a file (mirrors how `scripts/coverage.py` doesn't colorize today; this is a soft enhancement, not a requirement).
- TOTAL block at the end when >1 file: total issues + breakdown by severity + breakdown by rule (top-5 rules with counts).

### Cache layout

- Cache file: `target/.sonar-issues.json` at **repo root** (`juneau/target/.sonar-issues.json`). The repo-root `target/` already exists because the reactor build generates the aggregate site there.
- File is `.gitignore`-ed via the existing `target/` rule (no `.gitignore` change needed; verify before merge).
- Cache structure: the raw concatenated SonarCloud Web API `issues[]` arrays plus a small `meta` block with `fetched_at`, `branch`, `total`. No transformation on write — keep it close to the wire so future scripts can reuse it.
- Cache invalidation: explicit via `--run`. No automatic TTL (matches `/coverage`'s "reuse `.exec` unless told to refresh" model). The header line of the script's output prints `Using cached issues (fetched <ISO timestamp>, branch=<branch>). Use --run to refresh.` so the user can spot a stale cache.

### Cursor command file (`.cursor/commands/sonarqube.md`)

Mirror `.cursor/commands/coverage.md`'s structure literally:

- `---` front matter with `description: Juneau — SonarQube findings for a file, package, or folder via scripts/sonarqube.py`.
- `# /sonarqube (Apache Juneau)` heading.
- **Data source** section — points at SonarCloud Web API + local `target/.sonar-issues.json` cache.
- **Invocation** section — argument mapping (`SimpleClassName`, FQCN, repo-relative path, module shorthand table).
- **Module / area shorthands** table mirroring `/coverage`'s (`juneau-commons`, `juneau-marshall`, `juneau-rest-common`).
- **Run** section — show the `python3 scripts/sonarqube.py …` invocation form and the flag table from "Spec essentials" above.
- **Trigger phrases** section (see "Trigger phrases" below).
- **Reporting back** section — lead with total findings + severity breakdown; for directory targets call out the **worst** files (most findings) and the **highest-severity** finding(s); use **code references** (`startLine:endLine:filepath`) for the worst findings if helpful; do not paste a 200-line dump into the chat.
- **Safety** — do not echo `SONAR_TOKEN` if it's set in the environment; do not paste full URLs containing tokens.

### Trigger phrases

Run the workflow when the user types `/sonarqube` or any of:

- "show sonar issues on …"
- "sonar findings for …"
- "run sonar on …"
- "sonarqube report"
- "lint issues on …" (when context implies Sonar, not Checkstyle/PMD)
- "what sonar finds in …"
- "sonar triage …"

These are **disjoint** from AGENTS.md's existing "suppress warnings" trigger (which produces a Java edit, not a report). The two workflows are complementary: `/sonarqube` lists findings; the user then says "suppress warnings on …" to actually edit the source per the `code-conventions` skill.

## Deliverables

Concrete files this TODO adds (paths repo-relative to `juneau/`):

1. **`scripts/sonarqube.py`** — Python 3 script implementing the spec above. Modeled on `scripts/coverage.py` (same shebang, license header, `REPO_ROOT = Path(__file__).resolve().parent.parent`, `find_maven_module()`, `bar()`, `pct()` helpers). Uses only the Python standard library (`urllib.request`, `json`, `pathlib`, `argparse`, `sys`) — no `requests` dependency. The Apache license header at the top matches the existing `scripts/*.py` style.
2. **`.cursor/commands/sonarqube.md`** — Cursor command file mirroring `.cursor/commands/coverage.md`.
3. **`AGENTS.md`** — add a new **"5.2. SonarQube Script"** subsection (between the existing **5.1. Coverage Script** and **5.3. Command Execution Best Practices**) documenting the script, flags, and trigger phrases. Keep the format identical to the Coverage Script subsection.
4. **Touch (single-line edit) `scripts/README_SONARQUBE.md`** — add a top-of-file pointer to the new `/sonarqube` flow: "For ad-hoc triage from the terminal, use `python3 scripts/sonarqube.py <path>` (or the `/sonarqube` Cursor command). The TSV-export workflow below remains for batch categorization." Do **not** delete the legacy workflow in v1 — the categorized-JSON path is still useful for the "fix 100 issues in this category" use case the legacy script supports.

Nothing else in `pom.xml`, no Java source changes, no `.sonarlint/` or `sonar-project.properties` changes.

## Design notes

- **Mirror `/coverage` shape religiously.** The single biggest UX win is muscle memory: a developer who already uses `/coverage path/to/file.java --run` should be able to type `/sonarqube path/to/file.java --run` and have it feel identical. Flag names (`--run`/`-r`), argument shape (single path, file-or-directory-or-module), output structure (per-file block, progress bar, TOTAL summary), and helper functions (`bar()`, `pct()`, `find_maven_module()`) all come from `scripts/coverage.py` verbatim where possible.
- **SonarCloud Web API, not the Maven plugin.** Recap from the data-source survey above: (A) is fast, server-side, and aligned with the ruleset CI actually uses. (B) and (C) would re-push to the public dashboard. (D) requires per-developer install of a separate tool. (E) is the legacy path and is kept as a fallback. The script does **not** silently fall back between sources — if the Web API is unreachable, print the error and tell the user to (a) check network / VPN, or (b) supply a path to a cached `SonarQubeIssues.categorized.json` via a future `--from-tsv <path>` flag (not implemented in v1).
- **`directories=` filter, not `componentKeys=` per-file.** SonarCloud's API treats every file as a "component" with its own opaque key (`apache_juneau:juneau-core/juneau-marshall/src/main/java/.../Foo.java`). For directory queries this is unwieldy; SonarCloud offers a `directories=` filter (slash-separated path under the project root) that handles the package-roll-up case cleanly. The script computes the correct `directories=` value from the resolved repo-relative path. For single-file targets, it adds the corresponding `files=<repo-relative-path>` filter on top.
- **Branches.** SonarCloud analyzes the long-lived `master` branch and short-lived PR branches. Default to `master`. Surface a `--branch` flag for the rare "look at the PR" case; do not auto-detect the current git branch (too magical; PR branches have generated names like `apache:pr-123` on SonarCloud that don't match the local checkout name).
- **Authentication.** `apache_juneau` is a **public** SonarCloud project, so the Web API is accessible without auth. The script still picks up `SONAR_TOKEN` from the environment if set (used as `Authorization: Bearer <token>`) — useful for raising the rate limit and for the rare day SonarCloud serves a 429. Do **not** require it. **Never echo the token** (mirrors the AGENTS.md "do not leak secrets" rule).
- **Severity vocabulary.** SonarCloud uses `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`, `INFO` (legacy severity model). The newer "MQR" severity model (`HIGH` / `MEDIUM` / `LOW` per software quality) is also available on the API but is not what the existing `@SuppressWarnings("java:Sxxx")` suppressions in the codebase are scoped against — keep the legacy model for now. Open question for the user (below).
- **Rule-id format.** The codebase already uses the canonical SonarLint id format (`java:S2095`, `java:S3776`, `java:S110`, `java:S6541`, `java:S1452`, `java:S1104`, `java:S115`). The SonarCloud API returns rule ids in the same `java:Sxxx` form for Java findings (other languages use prefixes like `python:S...`, `css:S...`, `xml:S...`). The script renders them as-is — no remapping.
- **Suppression hint as an opt-in flag.** Putting a `@SuppressWarnings("java:Sxxx")` snippet on every line by default would double the output volume and conflict with the AGENTS.md "Sonar Suppression Policy for Parser Internals" workflow, which is **deliberate** (the policy is: suppress only on the four documented rules, not as a quick-fix for every finding). `--with-suppress-hint` is the off-by-default knob for the developer who has decided "yes, I'm suppressing this batch" and wants the exact token to paste. A future enhancement could honor the AGENTS.md "global policy" suppressions (auto-hint only for `java:S110`/`java:S6541` on parser-session classes), but v1 keeps the flag dumb.
- **Caching `target/.sonar-issues.json` (not `juneau-utest/target/...`).** Coverage's `juneau-utest/target/jacoco.exec` lives where it does because JaCoCo writes it during `mvn test` in that module. Sonar data is **fetched**, not built, so the cache lives at repo root under `target/.sonar-issues.json` and is project-scoped. The `target/` directory at the repo root already exists (parent reactor `mvn install` creates it for the aggregate site) and is `.gitignore`-d.
- **Pagination.** SonarCloud caps `ps` (page size) at 500 and the API is paged. The script pages until `paging.total <= ps * p` or 5000 issues total (safety cap; the project has ~3,000 issues today per `scripts/README_SONARQUBE.md`). At 500 per page that's at most 10 requests — under a second on a normal connection.
- **Rendering output for the parent agent.** When the Cursor command reports back to the user, it should lead with the **headline number** ("X issues, Y critical/major/minor") and then either list per-file results inline (small targets) or summarize the top-5 hottest files + top-3 rules (large targets). Mirrors `/coverage`'s "do not paste a huge dump" rule.

## Out of scope

- **CI / pre-commit integration.** Wiring `scripts/sonarqube.py` into a Git pre-commit hook or `scripts/push.py` is **explicitly OOS for v1**. The script is for interactive triage. If it grows into a gate later, that's a separate TODO that needs to settle "what severities are commit-blocking".
- **Replacing the SonarLint IDE plugin.** The IDE-side `.sonarlint/connectedMode.json` workflow stays. `/sonarqube` is a terminal/chat complement, not a replacement.
- **Triggering a server-side re-analysis.** `--run` only refreshes the **local cache** from whatever SonarCloud has computed. Forcing SonarCloud to re-analyze is a CI concern (push to `master` or open a PR), not a script concern.
- **Auto-applying `@SuppressWarnings`.** The `--with-suppress-hint` flag prints the token; it does **not** edit any source files. The existing "suppress warnings" AGENTS.md workflow handles the edit.
- **Retiring `scripts/categorize-sonar-issues.py` and the legacy TSV workflow.** That cleanup is a separate, small follow-up TODO once `/sonarqube` is in use for a couple of weeks and we know whether the batch-categorize use case still pulls its weight.
- **SonarLint CLI integration (Option D).** Defer to a separate TODO if anyone needs offline operation without SonarCloud connectivity.
- **`--from-tsv <path>` fallback** to parse a manually-exported SonarCloud TSV. Sketched in "Design notes" but not in v1 deliverables.
- **MQR-severity output** (`HIGH`/`MEDIUM`/`LOW`). Open question below; defer to v2 if the user wants the legacy severity model for v1.

## Verification

- `python3 scripts/sonarqube.py --help` prints the help block with all documented flags.
- `python3 scripts/sonarqube.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/` returns findings for the SSE package only (i.e. respects the `directories=` filter) — exact count depends on the live SonarCloud state.
- `python3 scripts/sonarqube.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/SseParserSession.java` returns findings for a **single file**, and at minimum surfaces the existing `java:S2095` suppression on line 86 if the suppression has been picked up by Sonar (or surfaces no findings on that line if it has).
- `python3 scripts/sonarqube.py juneau-core/juneau-marshall --severity BLOCKER,CRITICAL` returns only blocker/critical findings.
- `python3 scripts/sonarqube.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/ --rule java:S3776` returns only the cognitive-complexity findings in that package.
- Second run without `--run` is **noticeably faster** (reads cached JSON instead of re-fetching).
- Running with `SONAR_TOKEN=…` in the environment **does not** echo the token anywhere in output or error paths.
- The new `.cursor/commands/sonarqube.md` is recognized as a `/sonarqube` slash-command by Cursor.
- AGENTS.md "5.2. SonarQube Script" section is reachable from the "5.1. Coverage Script" reference style.
- `scripts/README_SONARQUBE.md`'s top-of-file pointer references the new script.

## Open questions

These are intentionally surfaced for the user to decide before implementation starts:

1. **Severity model.** Use the **legacy** severity model (`BLOCKER`/`CRITICAL`/`MAJOR`/`MINOR`/`INFO`) since that matches the existing `@SuppressWarnings("java:Sxxx")` mental model in the codebase — or use SonarCloud's newer **MQR** model (`HIGH`/`MEDIUM`/`LOW` per software quality) which the SonarCloud dashboard now defaults to? Recommended: legacy for v1, MQR as a future `--mqr` flag.
2. **Authentication policy.** Should the script (a) work anonymously by default and only use `SONAR_TOKEN` if present in the env (recommended), (b) require `SONAR_TOKEN` to be set (matches CI), or (c) read a stored token from `~/.sonar-token` if present?
3. **`--run` semantics.** Confirm `--run` means "re-fetch from SonarCloud Web API and overwrite local cache" (no server-side re-analysis). The user might intuitively expect `--run` to do something like `mvn sonar:sonar` (analogy to `/coverage --run` which does run tests). The plan recommends the cache-refresh meaning for cost and freshness reasons, but it's worth confirming.
4. **Cache location.** `target/.sonar-issues.json` at repo root (recommended — mirrors how the reactor's `target/` already aggregates), `juneau-utest/target/.sonar-issues.json` (symmetric with `/coverage`'s `juneau-utest/target/jacoco.exec`), or somewhere else like `~/.cache/juneau-sonar/issues.json`?
5. **`--with-suppress-hint`** — off by default (recommended; keeps output compact and respects the AGENTS.md "suppress sparingly" policy), or on by default (faster copy-paste workflow)?
6. **Color output.** Add ANSI colors gated on `isatty()` (recommended; `/coverage` does not but Sonar findings are scannable by severity), or keep monochrome to match `/coverage`?
7. **Legacy `scripts/categorize-sonar-issues.py` and `scripts/README_SONARQUBE.md`.** Leave the README pointing at a missing script (clearly stale) until a separate cleanup TODO, or retire/update the README as part of this work? Recommended: update the README top-of-file with a pointer to the new script (deliverable 4), defer full retirement.
8. **Cursor command default target.** Should `/sonarqube` (no argument) default to `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/` (matching `/coverage`'s default), or always require an argument?

## References

- `scripts/coverage.py` — primary architectural reference; copy its shape.
- `.cursor/commands/coverage.md` — primary command-file template; copy its structure.
- `scripts/README_SONARQUBE.md` — context on the existing manual SonarCloud workflow (TSV export → categorize → `view-sonar-category.py`).
- `scripts/view-sonar-category.py` — the surviving half of the legacy workflow; useful as a reference for the per-file grouping output shape.
- `sonar-project.properties` — project root; defines `sonar.projectKey=apache_juneau`, `sonar.organization=apache`, `sonar.host.url=https://sonarcloud.io`, plus the multi-criteria CSS rule exclusion.
- `.sonarlint/connectedMode.json` — IDE-side connection (project key + EU region) confirming the SonarCloud instance.
- `.sonarlint/sonarlint.json` — IDE-side per-project SonarLint config (`serverId: sonarcloud`, `projectKey: apache_juneau`).
- `.github/workflows/sonarcloud.yml` — CI side; runs `mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` with `SONAR_TOKEN` on push to `master` and on PRs.
- `pom.xml` lines 51 (`<sonar.organization>apache</sonar.organization>`) and 267-271 (`sonar-maven-plugin` 3.11.0.3922 declared in `pluginManagement`).
- `AGENTS.md` "suppress warnings" workflow and "Sonar Suppression Policy" entries (Brain Method `java:S6541`, inheritance depth `java:S110`) — these define the project's existing Sonar workflow and shape what `/sonarqube`'s output should pair with.
- `.cursor/skills/code-conventions/SKILL.md` lines 25-34 — "Sonar Suppression Policy for Parser Internals" canonical text.
- SonarCloud Web API "Issues — Search" endpoint: <https://sonarcloud.io/web_api/api/issues/search> (filters, pagination, response shape).
- Sample existing `@SuppressWarnings("java:Sxxx")` usages in the codebase (confirm rule-id format and suppression-comment convention):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/SseEventReader.java` — `java:S115`, `java:S2095`, `java:S3776`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetSerializerSession.java` — `java:S1104`.
  - `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanInfo.java` — `java:S1452`.
  - `juneau-bean/juneau-bean-mcp/src/main/java/org/apache/juneau/bean/mcp/McpMethods.java` — `java:S115` at class scope.
