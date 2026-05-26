# TODO-999 — Final pre-release cleanup pass (9.5.0)

> **Note on the ID choice.** This TODO uses the marker ID `999` (not the next sequential integer) to signal it's the **always-last** item in the 9.5.0 cycle. Per AGENTS.md the TODO-ID convention is "next free integer" for sequential work; the `999` marker is a deliberate exception so this entry sorts last in any ID-ordered listing and never gets confused for in-flight work.


Stable-state quality pass executed **only when the 9.5.0 release branch is being cut** — not before. All in-flight feature work above this TODO in `TODO.md` must have landed first. Most tasks are codebase-wide sweeps that need the rest of 9.5.0 feature work to be stable so the cleanup doesn't fight ongoing refactors.

This is a living plan file — add items as they surface during regular development. The user's pattern is to drop a `// TODO - <hint>` comment in code as they spot opportunities, then collect them here for batch processing when the release branch opens.

## Trigger condition

Start when **both** of the following are true:

1. All TODOs in `todo/TODO.md` execution-order Phases A–F have landed (i.e. all live feature work is complete).
2. The user explicitly invokes "start TODO-999" or equivalent.

Do NOT start opportunistically. Mid-cycle cleanup risks fighting in-flight feature refactors and creating merge conflicts.

## Cleanup task inventory

### 1. Utility-method migration sweep

**Goal:** find places where ad-hoc utility logic should be using shared helpers from `org.apache.juneau.commons.utils.*` (`Utils`, `CollectionUtils`, `StringUtils`, `IOUtils`, `FileUtils`, `ClassUtils`, etc.) and migrate the call sites.

**Canonical hint comment** the user drops in code: `// TODO - Use <Helper>.<method>()` (e.g. `// TODO - Use Utils.list()`).

**Confirmed instances (concrete starting list — extend as user spots more):**

| File | Line | Current pattern | Proposed replacement |
|---|---|---|---|
| `juneau-rest/juneau-rest-server/.../convention/BasicFaviconResource.java` | 162 | `var hdrs = new ArrayList<HttpHeader>(); hdrs.add(...); hdrs.add(...);` | `var hdrs = CollectionUtils.list(ContentType.of(...), CacheControl.of(...));` |

**Codebase-wide discovery instructions:**

1. **Grep for the hint comments:**
   ```
   rg -n 'TODO\s*[-:]\s*Use\s+(Utils|CollectionUtils|StringUtils|IOUtils|FileUtils|ClassUtils)' \
      --type java
   ```

2. **Grep for common anti-patterns** the helpers exist to replace:
   ```
   # ad-hoc empty-then-add lists (candidates for CollectionUtils.list(...) factory)
   rg -n 'new ArrayList<\w+>\(\);' --type java | head -100

   # ad-hoc empty-then-put maps (candidates for CollectionUtils.map(...) factory)
   rg -n 'new (Linked)?HashMap<\w+,\s*\w+>\(\);' --type java | head -100

   # Arrays.asList(...) followed by ArrayList wrap (candidates for CollectionUtils.list(...))
   rg -n 'new ArrayList<>\(Arrays\.asList\(' --type java | head -50

   # String concat (candidates for StringUtils.f / StringUtils.concat helpers)
   rg -n '"\s*\+\s*\w+\s*\+\s*"' --type java | head -50
   ```

3. **For each hit, evaluate** whether the helper is appropriate. NOT every `new ArrayList<>()` should become `CollectionUtils.list()` — only the cases where:
   - The list is populated immediately at construction with a known number of values.
   - The current code is verbose (3+ lines of `add(...)` calls that collapse to a single varargs literal).
   - No specific list type (`LinkedList`, `CopyOnWriteArrayList`) is required.

4. **Helpers to migrate toward** (canonical list — confirm against current `CollectionUtils` / `Utils` / `StringUtils`):
   - `CollectionUtils.list(T...)` — replaces `new ArrayList<>()` + N `add(...)` calls.
   - `CollectionUtils.map(K, V, K, V, ...)` — replaces `new HashMap<>()` + N `put(...)` calls.
   - `CollectionUtils.set(T...)` — replaces `new HashSet<>()` + N `add(...)` calls.
   - `StringUtils.f(format, args...)` — replaces `String.format(...)` (shorter).
   - `StringUtils.isBlank(CharSequence)` — replaces `(s == null || s.isBlank())` (null-safe blank check, single call). Hot codebase-wide pattern — see grep recipe below.
   - `StringUtils.isEmpty(CharSequence)` — replaces `(s == null || s.isEmpty())` (null-safe empty check; distinct from `isBlank` which also strips whitespace).
   - `Utils.eq(a, b)` — replaces `Objects.equals(a, b)` (existing convention in Juneau).
   - `Utils.nn(o)` — replaces `o != null`.
   - `Utils.n(o)` — replaces `o == null`.

5. **Anti-pattern grep recipes** for the high-frequency migrations:

   ```
   # (s == null || s.isBlank()) and variants — candidates for StringUtils.isBlank(s)
   rg -n '\(\s*\w+\s*==\s*null\s*\|\|\s*\w+\.isBlank\(\)\s*\)' --type java
   rg -n '\(\s*\w+\s*==\s*null\s*\|\|\s*\w+\.trim\(\)\.isEmpty\(\)\s*\)' --type java

   # (s == null || s.isEmpty()) — candidates for StringUtils.isEmpty(s)
   rg -n '\(\s*\w+\s*==\s*null\s*\|\|\s*\w+\.isEmpty\(\)\s*\)' --type java

   # Objects.equals(a, b) — candidates for Utils.eq(a, b)
   rg -n '\bObjects\.equals\(' --type java
   ```

5. **Add an inventory section to this file** before starting migration — list each hit with file + line + proposed replacement, so reviewers can scan the change scope before the PR lands.

### 2. FQCN cleanup

**Goal:** replace fully-qualified class names with regular imports where the FQCN was not required for disambiguation.

**Discovery:**

```
# Fully-qualified class references in source (excluding javadoc references and reflection lookups)
rg -n '\b(org\.apache\.juneau\.[a-z0-9_]+\.)+[A-Z][A-Za-z0-9_]*\b' \
   --type java \
   -g '!**/test/**' \
   | head -200
```

**Skip false positives:**

- FQCNs inside Javadoc `{@link ...}` / `{@linkplain ...}` (those are fine).
- `Class.forName("...")` / reflection-based lookups (necessary).
- FQCNs needed because of same-simple-name collision (e.g. both `org.apache.juneau.X.Foo` and `org.apache.juneau.Y.Foo` referenced in the same file).
- Annotation `value=ClassName.class` references where the class is in a non-obvious package and the reader benefits from seeing it inline (judgment call — usually NOT a candidate).

**Process:** per-file, top-down. Add the `import` statement; replace the FQCN. Let the IDE's organize-imports pass run after.

### 3. Full SonarQube scan + fixes

**Goal:** clean up Sonar findings codebase-wide; suppress the ones that match AGENTS.md policies.

**Process:**

1. Run `scripts/sonarqube.py --run` (forces fresh fetch from SonarCloud, overwrites local cache).
2. Run `scripts/sonarqube.py` (no args) at the repo root for the totals + per-rule breakdown.
3. For each rule, decide: fix or suppress.

**Suppress (per AGENTS.md policies — do NOT try to fix):**

- `java:S6541` (Brain Method) on parser state-machine methods — suppress at method scope with rationale comment.
- `java:S110` (deep inheritance) on parser-session classes — suppress at class scope.
- `java:S3776` (Cognitive Complexity) on dispatch-heavy methods — suppress at method scope.
- `java:S6539` (Monster Class) on coordination classes — suppress at class scope.
- `java:S115` (constant naming) on `UPPER_camelCase` / external-protocol-literal-mirroring constants — suppress at field/class scope.

**Fix (do NOT suppress without strong justification):**

- `BLOCKER` / `CRITICAL` severity (everything else).
- `MAJOR` severity in `juneau-marshall` / `juneau-rest-server` / `juneau-commons` (the high-traffic modules).
- Any rule not in the suppress list above.

**Output:** per-module fix-vs-suppress table in the FINISHED-999 archive so reviewers can audit the decisions.

### 4. TODO-execution-vocabulary cleanup in code comments

**Goal:** strip any lingering TODO-execution vocabulary from Java source / comments / javadocs. Two distinct leakage categories:

**4a. `TODO-<n>` ID tokens** — per AGENTS.md's "No `TODO-x` IDs in Java code" rule.

**4b. Phase markers and process vocabulary** — TODO-execution shorthand that crept into code comments during implementation. Includes:
- `Phase A` / `Phase B` / `Phase C` / etc. references
- `per Phase N` / `from Phase N` / `defer to Phase N` style notes
- `FINISHED-<n>` / `FINISHED-<id>-<slug>` references
- `OQ #N` / `RD #N` / `OQA` references
- `per <plan-file-name>.md` / `see todo/<file>.md` references
- General implementation-process narrative that documents HOW the code came to exist rather than WHAT the code does or WHY it works

The bar: a code comment should explain non-obvious intent, trade-offs, or constraints. It should NOT carry meta-vocabulary about which planning document, phase, or work-item drove the change. That context belongs in the FINISHED-<n> archive, not in the source.

**Discovery:**

```
# 4a: explicit TODO-N tokens
rg -n 'TODO-\d+' --type java
rg -n 'TODO-\d+' juneau-docs/pages/

# 4b: phase markers
rg -n -i '\b[Pp]hase\s+[A-Z]\b' --type java
rg -n -i '\b[Pp]hase\s+\d\b' --type java

# 4b: FINISHED archive references
rg -n 'FINISHED-\d+' --type java

# 4b: OQA / RD / planning-process vocabulary
rg -n '\b(OQ|RD|OQA)\s*#?\d+' --type java
rg -n -i '\b(per|from|see|defer(?:red)? to)\s+(TODO|FINISHED|todo/)' --type java

# 4b: direct plan-file references
rg -n 'todo/TODO-\d+' --type java
rg -n 'todo/FINISHED-\d+' --type java
```

**Replacement style** (per AGENTS.md):

- For category 4a (TODO IDs): use neutral wording — "work item N", or plain-English description.
- For category 4b (phase / process vocabulary): rewrite to describe the intent or trade-off being captured, NOT the planning process. If the comment doesn't add intent value once the process vocabulary is stripped, just delete it.
  - Example BAD: `// Per TODO-79 Phase 6, this stays user-facing only.`
  - Example BETTER (if intent matters): `// Intentionally narrow scope — see release notes for rationale.`
  - Example BETTER (if intent is obvious from code): just delete the comment.

**Exceptions** — do NOT touch:

- `todo/*.md` plan files themselves and `todo/FINISHED.md` (those legitimately use the identifiers as primary keys).
- Release notes in `juneau-docs/pages/release-notes/` (per-version archives; `TODO-N` / `FINISHED-N` references are intentional pointers for users tracking the work).
- Test class names / test method names that legitimately encode the work-item reference (e.g. `BasicJspResource_PathTraversal_Test`). Naming-level references are acceptable; in-method comments are not.

### 5. In-code `TODO` / `FIXME` / `XXX` marker resolution

**Goal:** every `TODO` / `FIXME` / `XXX` comment in the codebase is either (a) resolved in place, or (b) promoted to a fresh `[TODO-n]` bullet in `todo/TODO.md`.

**Discovery:**

```
rg -n '\b(TODO|FIXME|XXX)\b' --type java -g '!**/test/**' | head -200
rg -n '\b(TODO|FIXME|XXX)\b' --type java -g '**/test/**' | head -200
```

**Triage:**

- **Resolvable in place** (cheap fix, clear path): fix and remove the marker.
- **Substantive enough to be its own work item**: promote to `[TODO-n]` in `todo/TODO.md` with a brief description and remove the in-code marker, replacing it with a comment that points at the new TODO if context helps the next reader.
- **Stale / no longer relevant**: just remove the marker.

**Exception:** the `// TODO - Use Utils.list()` hint comments (task 1 above) are intentional bookkeeping for THIS plan file — they get resolved by task 1, not task 5.

### 6. `@SuppressWarnings` format normalization

**Goal:** every `@SuppressWarnings` in the codebase uses one canonical multi-line, array-literal form with inline per-rule rationale comments — even for single-rule suppressions. This makes diff review trivial (one rule = one line), keeps rationale physically adjacent to the rule ID it explains, and lets future suppressions append in one-line additions instead of reformatting the whole annotation.

**Canonical format:**

```java
@SuppressWarnings({
    "java:S1166"  // Exception swallowed intentionally — fold-or-skip contract
})
```

Rules:

- **Always use the array form** (`{ ... }`), even for a single suppression. Future-proofs against adding a second rule later without reformatting.
- **One rule ID per line.** Even if all suppressions share a single rationale, list them on separate lines.
- **Inline `// rationale` trailing comment per rule ID** — short (one line), explains WHY the suppression is legitimate. The rationale must justify the suppression against AGENTS.md's allowlist (Brain Method, Cognitive Complexity, Monster Class, deep inheritance, constant naming, resource analysis) OR explain the local intent for any other rule.
- **No trailing comma** after the last rule ID (Juneau style — match the rest of the codebase).
- **Indent the contents** by one tab/4-space level inside the braces; align the `})` closer with the `@SuppressWarnings` column.
- **No bare `@SuppressWarnings("...")` single-string form** — even if it fits on one line.
- **No grouped/shared trailing comment** at the end of the array — one rationale per ID, even when repetitive. (A reviewer skimming a diff should never have to scroll up to find the rationale for an ID.)

**Anti-pattern examples** (all candidates for normalization):

```java
// Bad: bare single-string form, no rationale
@SuppressWarnings("rawtypes")

// Bad: bare single-string form, trailing-only rationale
@SuppressWarnings("java:S1166")  // Exception swallowed intentionally

// Bad: array form, single trailing rationale for two unrelated rules
@SuppressWarnings({"unchecked", "rawtypes"})  // generics noise

// Bad: array form, no rationale
@SuppressWarnings({"unchecked", "rawtypes"})
```

**Normalized form** (target after migration):

```java
@SuppressWarnings({
    "unchecked",  // Type erasure on legacy reflective bean accessor
    "rawtypes"    // Companion to "unchecked" for the same accessor
})

@SuppressWarnings({
    "java:S1166"  // Exception swallowed intentionally — fold-or-skip contract
})
```

**Discovery:**

```
# All @SuppressWarnings usages, with line numbers
rg -n '@SuppressWarnings' --type java

# Specifically bare single-string forms (no braces) — primary normalization candidates
rg -n '@SuppressWarnings\("[^"]+"\)' --type java

# Array forms without inline per-rule comments — secondary normalization candidates
# (matches @SuppressWarnings({"a","b"}) or @SuppressWarnings({"a", "b"}) on one line)
rg -n '@SuppressWarnings\(\{\s*"[^"]+"\s*(,\s*"[^"]+"\s*)*\}\)' --type java
```

**Exceptions** — leave alone:

- Generated source (anything under `target/`, `generated-sources/`, etc.) — never touched.
- Test classes where the suppression is on a synthetic `// CHECKSTYLE:OFF` style comment block — out of scope.

**Process:** per-file, top-down. Normalize all `@SuppressWarnings` in the file before moving to the next. Where rationale is missing, supply one — checking AGENTS.md's suppression policies to ensure the suppression is legitimate. If a suppression cannot be justified, **delete it and fix the underlying issue** instead of inventing a rationale.

## Acceptance criteria

- Per-task migration table filed in `FINISHED-999-final-prerelease-cleanup.md`.
- `./scripts/test.py` green after each task's batch lands.
- No `// TODO - Use <Helper>` hint comments remain in source (task 1 complete).
- Codebase-wide grep for `TODO-\d+` in Java source returns zero matches outside the `todo/` directory (task 4a complete).
- Codebase-wide grep for phase-vocabulary patterns (`Phase A`/`B`/`C`, `FINISHED-\d+`, `OQ #N`, `RD #N`, `OQA`, `todo/TODO-\d+`, `todo/FINISHED-\d+`) in Java source returns zero matches outside test class/method names (task 4b complete).
- Codebase-wide grep for `TODO|FIXME|XXX` in Java source returns only comments that are either (a) part of an unrelated in-flight TODO, or (b) intentional documentation references (e.g. "marked TODO in upstream library"); no orphan markers (task 5 complete).
- SonarQube dashboard for `apache_juneau` shows no `BLOCKER`/`CRITICAL` findings; `MAJOR` findings either resolved or annotated with rationale (task 3 complete).
- Per-file FQCN-to-import migration count summed in the archive (task 2 complete).
- Codebase-wide grep `rg -n '@SuppressWarnings\("[^"]+"\)' --type java` returns zero matches (task 6 complete — no bare single-string forms remain); every `@SuppressWarnings` uses the canonical multi-line array form with one inline `// rationale` per rule ID.

## Risks

- **Long-running PR.** This is a 1000+-line diff potentially touching every module. Stage as a sequence of focused commits (one per task), pushed individually so review can happen in parallel with the next task's work.
- **CI test sensitivity.** Bulk helper-method migrations (`new ArrayList<>()` → `CollectionUtils.list(...)`) sometimes change iteration order in ways tests depend on. Run `./scripts/test.py` per-batch, not just at the end.
- **Sonar suppression scope creep.** Easy to over-suppress to clear the dashboard; resist. The AGENTS.md policies define what's legitimately suppressible — anything outside that list deserves a real fix or a written justification in the FINISHED-999 archive.
- **TODO-x removal collateral.** Some `TODO-n` references in release notes or migration docs are intentional pointers for users tracking the work. Per-occurrence judgment, not blanket removal.

## Working notes (append items as they surface)

_Use this section to log hints as you spot them mid-development. Each entry: file + line + brief description. Batch-process during the release-branch cleanup pass._

- 2026-05-25 — `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/convention/BasicFaviconResource.java:162` — `var hdrs = new ArrayList<HttpHeader>();` followed by two `hdrs.add(...)` calls; collapse to `CollectionUtils.list(...)` factory.
- 2026-05-25 — `(value == null || value.isBlank()) ? DEFAULT_BASE_PATH : value` pattern surfaced in unspecified location; replace with `StringUtils.isBlank(value) ? DEFAULT_BASE_PATH : value`. Task-1 grep recipe added to catch all similar occurrences codebase-wide.
- 2026-05-25 — Bare single-string `@SuppressWarnings("java:S1166")  // Exception swallowed intentionally — fold-or-skip contract` form surfaced in unspecified location; normalize to canonical multi-line array form per task 6. Task-6 grep recipe (`rg -n '@SuppressWarnings\("[^"]+"\)' --type java`) catches every bare single-string usage codebase-wide.

## Related work

- **AGENTS.md** — defines the canonical Sonar suppression policies, the "No TODO-x IDs in Java code" rule, and the `/sonarqube` + `/coverage` script wrappers used during the SonarQube task.
- **`scripts/sonarqube.py`** — primary tool for task 3.
- **`.cursor/skills/eclipse-warnings/SKILL.md`** — IDE-side complement to the SonarQube CLI; can drive quick-fixes from the Problems view if useful during task 3.
