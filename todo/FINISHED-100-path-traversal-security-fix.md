# FINISHED-100 — Security: CWE-22 path traversal in `DirectoryResource` (and audit-discovered `LogsResource`)

> **PRIVATELY REPORTED — DISCLOSURE TIMING PENDING CVE COORDINATION. DO NOT PUBLISH OR PUSH WITHOUT USER DIRECTION.**
>
> Same-session security fix, no plan file. Filed and landed in one Phase C2 worker run alongside the TODO-78 family cleanup. No `TODO-100-*.md` plan file was created — the work was scoped narrowly enough to capture entirely in this archive.

## Disclosure metadata (internal — for project owner reference only)

- **Reporter:** LTSHFWJT (privately disclosed; reporter handle kept ONLY in this archive — never in source comments, commit messages, release notes, or `todo/FINISHED.md` until disclosure is coordinated).
- **Reported against:** Apache Juneau 9.2.0. Other versions not verified by the reporter; we confirmed the bug is still present in 9.5.0 (in-flight) before fixing.
- **CVE assignment:** pending. User will coordinate with the Apache Security Team and request a CVE before any public disclosure.
- **Original report file:** `/Users/james.bognar/Downloads/SecurityIssue.txt` (local-only; not committed).
- **Audit performed:** repo-wide grep for the same vulnerability shape (unbounded `getAbsolutePath() + path` / `new File(root + userInput)` / `Path.resolve(userInput)` without canonical boundary check). One additional confirmed-vulnerable site found and fixed (`LogsResource`); one deferred site logged as an open item (`BasicJspResource`, owned by the in-flight TODO-78 worker — not in scope for this fix worker).

## Summary

Fixes a CWE-22 path-traversal vulnerability in `org.apache.juneau.microservice.resources.DirectoryResource#getFile(String)` in which a user-supplied request path was concatenated onto `rootDir.getAbsolutePath() + '/' + path` and then passed to `new File(...)` with no canonical-path boundary check. As a result, request paths containing `..` segments could escape the configured root directory and read (or, when uploads/deletes are enabled, write/delete) any file readable by the JVM process. The same vulnerability shape was discovered in `LogsResource#getFile(String)` during the codebase-wide audit and fixed identically. The fix funnels every user-supplied path through `java.nio.file.Path#toRealPath()`/`#normalize()` and rejects paths that don't `startsWith(root)` with a 403 (Forbidden), with a defensive post-existence symlink check so symlinks-out-of-root are also rejected. 25 new regression tests across 2 test classes assert the post-fix behavior on every public operation surface (view / parse / download / upload / delete / info).

## Files changed

### Production code (2 files — both confirmed vulnerable, both fixed)

- **`juneau-microservice/juneau-microservice/src/main/java/org/apache/juneau/microservice/resources/DirectoryResource.java`**
  - Replaced `getFile(String path)` body with a canonical-path resolution + boundary check.
  - Pre-fix shape (vulnerable):
    ```java
    var f = new File(rootDir.getAbsolutePath() + '/' + path);
    if (!f.exists()) throw new NotFound(...);
    return f;
    ```
  - Post-fix shape:
    ```java
    java.nio.file.Path root = rootDir.toPath().toRealPath();   // with IOException fallback to absolute+normalize
    java.nio.file.Path target = root.resolve(path).normalize();
    if (!target.startsWith(root)) throw new Forbidden(...);    // pre-existence boundary check
    if (!target.toFile().exists()) throw new NotFound(...);
    if (!target.toRealPath().startsWith(root)) throw new Forbidden(...);  // post-existence symlink check
    return target.toFile();
    ```
  - The fully-qualified `java.nio.file.Path` is used because the file already imports `org.apache.juneau.http.annotation.Path` and the simple name is shadowed.
  - The two `Forbidden` branches use the same message ("Path escapes configured root directory.") so attackers can't distinguish "rejected by pre-existence boundary check" from "rejected by post-existence symlink check" — both are a hard denial.
  - All public operations on this resource (`viewFile`, `downloadFile`, `getDir`, `deleteFile`, `uploadFile`) already funnel through `getFile(...)`, so the single fix protects all of them. Verified by reading every caller in the file.

- **`juneau-microservice/juneau-microservice/src/main/java/org/apache/juneau/microservice/resources/LogsResource.java`**
  - Identical pre-fix vulnerability shape (`new File(logDir.getAbsolutePath() + '/' + path)`), found by the codebase-wide audit.
  - Same fix applied to the static `getFile(String path)` method.
  - All public operations on this resource (`viewFile`, `viewParsedEntries`, `downloadFile`, `getFile`-info, `deleteFile`) funnel through `getFile(String)`, so the single fix protects all of them.

### Tests (2 new files, 25 test methods total)

- **`juneau-utest/src/test/java/org/apache/juneau/microservice/resources/DirectoryResource_PathTraversal_Test.java`** — 13 `@Test` methods.
- **`juneau-utest/src/test/java/org/apache/juneau/microservice/resources/LogsResource_PathTraversal_Test.java`** — 12 `@Test` methods.

Both test classes share the same matrix shape (baseline + traversal across each operation surface + absolute-path + URL-encoded + symlink), differing only in the resource and configuration plumbing. See "Test coverage" below.

### Plan + index files

- **`todo/FINISHED-100-path-traversal-security-fix.md`** (this file).
- **`todo/FINISHED.md`** — sanitized "Recent completions" entry pointing here. The `FINISHED.md` entry deliberately does NOT include the reporter handle or CVE coordination details (those live only in this archive until disclosure is coordinated).

`todo/TODO.md` is **not** touched: TODO-100 was never an in-flight TODO bullet (it's a same-session security fix triaged in via private disclosure).

`/Users/james.bognar/git/apache/juneau-docs/pages/release-notes/9.5.0.md` is **deliberately not modified** by this worker. The release-notes entry is sanitized and disclosed by the user after CVE coordination — see "Release-notes draft" below for a copy-paste-ready blurb.

## The vulnerability — what attackers can do (pre-fix)

Pre-fix `DirectoryResource.getFile(String path)`:

```java
private File getFile(String path) {
    if (path == null) return rootDir;
    var f = new File(rootDir.getAbsolutePath() + '/' + path);
    if (! f.exists()) throw new NotFound("File not found.");
    return f;
}
```

`rootDir.getAbsolutePath() + '/' + path` is just string concatenation — there's no canonical-path normalization, no boundary check, no symlink resolution. So:

- `GET /files/../outside-secret.txt?method=VIEW` resolves to `<rootDir>/../outside-secret.txt` which `File` happily resolves up out of `rootDir`. If the file exists and the JVM can read it, the contents are returned in the response body.
- `GET /files/etc/passwd` (on Linux, where `/etc/passwd` is an absolute path): `new File(rootDir + "/" + "/etc/passwd")` — Java's `File` constructor treats the second argument as absolute when it starts with `/` and replaces the rootDir, so this resolves to `/etc/passwd` directly. Pre-fix, this returns the file contents.
- `PUT /files/../outside-uploaded.txt` body=...: the same resolution flow plus the upload path writes the body to the resolved File, so an attacker can drop a payload anywhere the JVM can write.
- `DELETE /files/../inside-some-other-dir/file.log`: the same resolution flow plus the delete path can `delete()` any file the JVM can delete.
- A symlink at `<rootDir>/escape -> /etc/passwd` resolves through `File` and the boundary check would never catch it (because there is no boundary check).

Result: any unauthenticated remote attacker who can reach a deployed `DirectoryResource` (or `LogsResource`, which has the same shape) can read/write/delete files outside the configured root, bounded only by the JVM's filesystem permissions. CWE-22 (Improper Limitation of a Pathname to a Restricted Directory).

## The fix — what attackers can no longer do (post-fix)

The post-fix `getFile(String path)` performs four checks in order:

1. **`root = rootDir.toPath().toRealPath()`** (with `IOException` fallback to `toAbsolutePath().normalize()`) — resolves the configured root once per call. `toRealPath()` is preferred because it normalizes any symlinks at the root itself; the fallback handles the rare case where the configured root doesn't exist at the moment of the call (legacy contract tolerated this).
2. **Pre-existence boundary check** — `root.resolve(path).normalize()` (catching `InvalidPathException` → 403). `Path.resolve(other)` treats an absolute `other` as the new root, so requests like `/etc/passwd` land outside `root` and `target.startsWith(root)` is `false` → 403. `..`-segment traversal lands outside `root` after `.normalize()` → 403.
3. **Existence check** — `target.toFile().exists()` is the legacy 404 contract; preserved.
4. **Post-existence symlink check** — `target.toRealPath().startsWith(root)` (catching `NoSuchFileException` → 404 to handle the TOCTOU race between `exists()` and `toRealPath()`; catching other `IOException` → 500). This unwraps any symlinks under the root and re-checks the boundary, so a symlink at `<rootDir>/escape -> /etc/passwd` is rejected with 403.

The two boundary-check sites use the same `Forbidden("Path escapes configured root directory.")` message so attackers cannot distinguish pre-existence rejection from post-existence rejection.

The fix is applied at the **single funnel** that every public op calls. Both `DirectoryResource` and `LogsResource` have a single private `getFile(String)` method that every user-supplied path flows through, so the fix is applied once per resource and protects every operation surface.

## Audit findings (codebase-wide)

Search patterns used (via Cursor's `Grep` tool, not raw `rg`):

- `new File\(.*\+`
- `getAbsolutePath\(\)\s*\+`
- `getRequestDispatcher\(`
- `Paths\.get\(.*\+` / `Path\.of\(.*\+`
- `\.resolve\(.*(\?:request|path|userInput|param)`
- `getResourceAsStream\(`

Audit results, classified per the user's three-bucket taxonomy:

### CONFIRMED VULNERABLE — fixed in this worker run

- **`DirectoryResource.getFile(String)`** — primary disclosure target. Fixed in the same file. Regression tests added.
- **`LogsResource.getFile(String)`** — same vulnerability shape; discovered in audit. Fixed in the same file. Regression tests added.

### CONFIRMED SAFE — verified by reading the code

- **`org.apache.juneau.cp.FileFinder`** — explicit `isInvalidPath(String)` check that rejects any path containing `..` or `%`. Returns `false` (not found) rather than throwing, but the practical effect is identical for security purposes — no traversal-shaped path can reach the underlying filesystem read.
- **`org.apache.juneau.rest.staticfile.BasicStaticFiles`** — delegates to `FileFinder`. Inherits its path validation. Confirmed safe.
- **`org.apache.juneau.commons.io.LocalDir`** — explicitly documents in its Javadoc that it does not perform path validation and relies on the caller. The known caller (`FileFinder`) does validate before calling. **Defensive-in-depth:** no caller adds explicit boundary checks today; if a future caller invokes `LocalDir` without going through `FileFinder`, the vulnerability could re-surface. Flagged below.
- **`org.apache.juneau.rest.vars.FileVar`** — uses `req.getStaticFiles().getString(key, null)`. The `key` flows through `BasicStaticFiles` → `FileFinder`, so the path validation is shared. Confirmed safe.
- **`org.apache.juneau.rest.convention.BasicFaviconResource`** — favicon path is configured at deploy time, not user input. Confirmed safe.
- **`org.apache.juneau.rest.convention.BasicVersionResource`** — version-properties path is configured at deploy time, not user input. Confirmed safe.
- **`org.apache.juneau.config.store.ClasspathStore.read(String name)`** — `name` is a configuration key, not a request-path remainder. The configuration system's own threat model treats config keys as trusted (configured at deploy time, not from request input). Confirmed safe.
- **`org.apache.juneau.rest.RestContext` SVL `paths` resolution** — operates on URL-path strings (routing), not filesystem paths. Out of scope for CWE-22.
- **`org.apache.juneau.http.classic.entity.FileEntity`** — only uses `getAbsolutePath()` for error-message text; no path resolution. Confirmed safe.

### DEFENSIVE-IN-DEPTH RECOMMENDED — not fixed in this worker run

- **`org.apache.juneau.commons.io.LocalDir`** — currently safe in context (only known caller validates), but the class itself is permissive. A small belt-and-suspenders boundary check would harden against future refactors. Tracking suggestion: file as a follow-up TODO during the next inner-loop pass; not pulled in here to keep the security fix tight and reviewable.

### CONFIRMED VULNERABLE — FIXED (BasicJspResource) — Phase C2 follow-up (2026-05-25)

- **`org.apache.juneau.rest.view.jsp.BasicJspResource` / `JspViewRenderer`** (TODO-78 working tree). **The deferred audit finding above was addressed in the Phase C2 cleanup pass on 2026-05-25.** The fix funnels both `BasicJspResource.render(...)` and `JspViewRenderer.joinPath(...)` through a new sibling helper `FileUtils.resolveVirtualPathSafely(String basePath, String userPath)` — the virtual-path counterpart of `FileUtils.resolveSafely(File, String)` extracted from this archive's `DirectoryResource`/`LogsResource` fixes. Boundary semantics match: any `..` segment that escapes `basePath` raises `IllegalArgumentException`, which `BasicJspResource.render` catches and maps to HTTP 403 (Forbidden) with the same generic "Path escapes configured base path." message. `JspViewRenderer.process` (which sees template names from application code, not user input) maps the same IAE to HTTP 500 since a template-name escape attempt is a server-side bug, not a request-side attack. Regression tests live in `juneau-utest/src/test/java/org/apache/juneau/rest/view/jsp/BasicJspResource_PathTraversal_Test.java` (5 `@Test` methods covering baseline / direct traversal / nested traversal / traversal to sibling base-path prefix / URL-encoded traversal). The pre-existing `BasicJspResource_MockRest_Test` and `JspViewRenderer_Test` continue to pass unmodified — the new helper preserves every legitimate `joinPath(...)` case from the original implementation.

## Test coverage

`DirectoryResource_PathTraversal_Test.java` — 13 `@Test` methods (matrix shape):

| # | Test | Asserts |
|---|------|---------|
| t01 | `normalAccess_view` | Baseline: `VIEW /inside.txt` returns 200 with the file content. Confirms the fix doesn't break legitimate use. |
| t02 | `directTraversal_GET_returns403` | `GET /../outside-secret.txt` → 403. |
| t03 | `methodVIEW_traversal_returns403` | `GET /../outside-secret.txt?method=VIEW` → 403, body does not contain the outside-root secret marker. |
| t04 | `methodDOWNLOAD_traversal_returns403` | `GET /../outside-secret.txt?method=DOWNLOAD` → 403, body does not contain the outside-root secret marker. |
| t05 | `verbVIEW_traversal_returns403` | `VIEW /../outside-secret.txt` → 403. |
| t06 | `verbDOWNLOAD_traversal_returns403` | `DOWNLOAD /../outside-secret.txt` → 403. |
| t07 | `nestedTraversal_returns403` | `GET /a/b/../../../outside-secret.txt` → 403. |
| t08 | `absolutePathInRequest_isNotTreatedAsFilesystemAbsolute` | `GET /etc/passwd` → 403 or 404 (platform-dependent, both are non-leak outcomes). |
| t09 | `urlEncodedTraversal_doesNotLeak` | `GET /%2e%2e/outside-secret.txt` → not 200, body does not contain the secret marker. |
| t10 | `uploadTraversal_does_not_create_outside_root` | `PUT /../outside-uploaded-by-test.txt` → 403/404, no file is created outside the root. |
| t11 | `deleteTraversal_does_not_delete_outside_root` | `DELETE /../outside-secret.txt` → 403, the outside-root file is unchanged. |
| t12 | `symlinkInsideRoot_is_followed` | Symlink `<root>/link-to-inside.txt -> <root>/inside.txt` → 200 with content. |
| t13 | `symlinkEscapesRoot_is_rejected` | Symlink `<root>/link-to-outside -> <outside>/outside-secret.txt` → 403, body does not contain the secret marker. |

`LogsResource_PathTraversal_Test.java` — 12 `@Test` methods (same shape, minus the upload test because `LogsResource` doesn't expose uploads):

| # | Test | Asserts |
|---|------|---------|
| t01 | `normalAccess_view` | Baseline: `VIEW /inside.log` returns 200. |
| t02 | `directTraversal_GET_returns403` | Direct `..` traversal → 403. |
| t03–t06 | `method=VIEW`/`method=DOWNLOAD`, verb `VIEW`/`DOWNLOAD` traversal | All → 403, no leak. |
| t07 | `nestedTraversal_returns403` | Nested `..` chain → 403. |
| t08 | `absolutePathInRequest_isNotTreatedAsFilesystemAbsolute` | Request-absolute path → 403 or 404. |
| t09 | `urlEncodedTraversal_doesNotLeak` | URL-encoded `..` → no leak. |
| t10 | `deleteTraversal_does_not_delete_outside_root` | DELETE traversal → 403, outside file unchanged. |
| t11–t12 | Symlink-inside / symlink-escape | Symlink-inside followed; symlink-escape rejected. |

Symlink tests are conditional on the filesystem supporting symbolic links (`assumeTrue(...)`) so the matrix still runs on platforms / filesystems that don't support them (e.g. some Windows configurations).

Both test classes use a `@TempDir` JUnit 5 fixture for the test root + outside file. Each test gets a fresh `MockRestClient` from a `buildClient()` factory, and the in-memory `MemoryStore`-backed `Config` is reconstructed per call so test ordering doesn't matter.

## Verification

`./scripts/test.py` — full repo build + tests — was run from the `juneau` repo root. See "Verification log" at the bottom of this archive for the wall-clock and the per-class summary.

Targeted runs of just the new test classes (used during development, also passing in the full-suite run):

```
./mvnw -pl juneau-utest test -Dtest=DirectoryResource_PathTraversal_Test,LogsResource_PathTraversal_Test \
  -Drat.skip=true -Dsurefire.failIfNoSpecifiedTests=false
# → Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
# → DirectoryResource_PathTraversal_Test: 13 tests, 0.853 s
# → LogsResource_PathTraversal_Test:      12 tests, 1.128 s
```

No pre-existing tests were updated as part of this fix. (The fix tightens the contract — what was previously a silent traversal-success now returns 403 — so any pre-existing test that depended on the vulnerable behavior would have failed. None did, suggesting no production code intentionally relied on the bug.)

## Follow-up: BasicJspResource fix + helper extraction (2026-05-25)

Phase C2 of this work cycle landed two related changes that close out the deferred audit finding and consolidate the canonical fix into a reusable helper:

1. **Helper extraction to `FileUtils` (`juneau-core/juneau-commons`).** The duplicated `Path.toRealPath() + startsWith(root)` logic from `DirectoryResource.getFile` and `LogsResource.getFile` was extracted into:
    - `FileUtils.resolveSafely(File rootDir, String userPath) → Optional<File>` — filesystem variant (canonicalizes via `toRealPath()`, handles symlinks, returns `Optional.empty()` for non-existent files, throws `IllegalArgumentException` on traversal escape).
    - `FileUtils.resolveVirtualPathSafely(String basePath, String userPath) → String` — virtual-path variant for servlet-context / classpath resolvers (uses `Path.normalize()` + `Path.startsWith()` without touching the filesystem; throws IAE on traversal escape).
    
    Both `DirectoryResource.getFile` and `LogsResource.getFile` were refactored to delegate to `FileUtils.resolveSafely`; the post-refactor regression tests landed in this archive's original pass still pass without modification — that's the win of the helper extraction.
2. **`BasicJspResource` fix landed via the virtual-path helper.** See the "CONFIRMED VULNERABLE — FIXED (BasicJspResource)" entry above. The deferred TODO-78 working-tree fix uses the new `FileUtils.resolveVirtualPathSafely` helper, with `BasicJspResource.render(...)` mapping the IAE to HTTP 403 (request-side path) and `JspViewRenderer.process(...)` mapping the same IAE to HTTP 500 (application-side template name). 5 new regression tests in `BasicJspResource_PathTraversal_Test`.

The Phase C2 release-notes draft for the helper extraction (separately from the security disclosure) is left to user discretion — the helper is a non-security-sensitive utility addition and could ship in the same 9.5.0 cut without coordination, but co-disclosing with the CVE keeps the narrative cleaner. **No release-notes lines for either change were published by this worker** per the parent-prompt disclosure constraint.

## Open items

1. **Disclosure coordination (user task)** — file with the Apache Security Team, request CVE assignment, coordinate disclosure timing. Until then: do **not** push, do **not** publish release notes, do **not** mention in public commits. **The `BasicJspResource` fix is part of the same disclosure-sensitive group** — same vulnerability class (CWE-22), same fix shape (boundary check via `Path.startsWith()`), same 403 surface — so include it under the same CVE umbrella.
2. **`LocalDir` defensive-in-depth (low priority)** — see "DEFENSIVE-IN-DEPTH RECOMMENDED" above. Add an explicit boundary check inside `LocalDir` so it's not just-as-safe-as-its-callers. File as a follow-up TODO when the inner-loop next visits the `juneau-commons` IO layer. (`FileUtils.resolveSafely` is now the obvious helper to call from `LocalDir`.)
3. **Examples / petstore / starter modules** — the audit covered the production microservice resources but did not exhaustively walk every example app's custom file-serving handlers (the examples are not on the public-attack-surface critical path the way the canonical microservice resources are). If the user wants belt-and-suspenders, file a follow-up TODO to do an examples-only audit pass.
4. **View-module siblings (TODO-82/83/84)** — the Thymeleaf, Mustache, and FreeMarker view-module plans were updated on 2026-05-25 to require `FileUtils.resolveVirtualPathSafely` hardening in their bridge implementations. When those modules are eventually built, the helper is the canonical call site for the same boundary check.

## Release-notes draft (sanitized — for user to publish AFTER disclosure is coordinated)

This is intentionally vague enough to land before CVE assignment without telegraphing a PoC. The user adds CVE-NNNN-NNNN once it's assigned.

```markdown
### Security

- **(CVE-NNNN-NNNN) Path traversal in `DirectoryResource` and `LogsResource`** —
  `org.apache.juneau.microservice.resources.DirectoryResource#getFile` and
  `org.apache.juneau.microservice.resources.LogsResource#getFile` previously
  resolved a request-supplied path against the configured root with simple
  string concatenation, allowing `..` segments and absolute paths to escape
  the configured root and read (or, when uploads/deletes were enabled,
  modify/delete) files outside it. Both methods now resolve through
  `java.nio.file.Path#toRealPath()` and reject any target that does not
  remain inside the configured root with HTTP 403 (Forbidden). Symlinks
  inside the root that resolve to outside-root targets are also rejected.
  Reported privately; thanks to the reporter for responsible disclosure.
```

The reporter's handle (LTSHFWJT) is intentionally omitted from the public release-notes draft. If the reporter requests acknowledgement at disclosure time, the user can add a credit line then.

## Suggested commit message (sanitized — for user to use AFTER disclosure is coordinated)

```
Security: harden DirectoryResource and LogsResource against path traversal

Both resources resolved request-supplied paths against their configured
root with simple string concatenation, allowing .. segments and absolute
paths to escape the configured root and read (or, when uploads/deletes
were enabled, modify/delete) files outside it.

Both getFile(String) methods now resolve via java.nio.file.Path,
canonicalize via toRealPath(), and reject any target that does not
remain inside the configured root with HTTP 403. A post-existence
symlink check additionally rejects in-root symlinks whose targets
resolve outside the root.

25 new regression tests (DirectoryResource_PathTraversal_Test: 13,
LogsResource_PathTraversal_Test: 12) cover direct traversal, nested
traversal, request-absolute paths, URL-encoded variants, upload and
delete traversal, and symlink handling on every public operation
surface.
```

CVE number is intentionally omitted until assignment.

## Verification log

`./scripts/test.py` run from the `juneau` repo root on 2026-05-25:

- **Result:** `✅ Tests passed!`
- **Wall-clock:** 113 seconds (113146 ms — single full build + full test run)
- **Targeted run of the new test classes** (run separately during development, also passing in the full-suite run): 25 tests across `DirectoryResource_PathTraversal_Test` (13) and `LogsResource_PathTraversal_Test` (12); 0 failures, 0 errors, 0 skipped.

No pre-existing tests were modified. No new third-party dependencies were added. License headers present on both new test files.
