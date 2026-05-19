# FINISHED-42: Split `juneau-rest-common` into `juneau-rest-common` + `juneau-rest-common-classic`

**Status:** Completed 2026-05-19.

**Outcome:**

- New module `juneau-rest/juneau-rest-common-classic` created with explicit `httpcore:4.4.16` dependency and a `compile` dep on the pure `juneau-rest-common`.
- 171 source files moved from `juneau-rest-common/src/main/java/org/apache/juneau/http/classic/` to the new module via `git mv`.
- `juneau-rest-common`'s `pom.xml` no longer declares `httpcore`. `dependency:tree` confirms zero `org.apache.httpcomponents:*` entries.
- 7 transport-neutral files that used to import from `org.apache.juneau.http.classic.header.*` were untangled. `EntityTag` / `EntityTags` (pure value types) moved out of `classic/header/` into `org.apache.juneau.http.header`.
- `@Remote.headerList()` widened from `Class<? extends HeaderList>` to `Class<?>` (default `Void.class`) so the annotation no longer references classic. Classic `RemoteMeta` does an `instanceof HeaderList` runtime check.
- Consumer POMs updated: `juneau-rest-server`, `juneau-rest-client-classic`, the shaded artifacts (`-rest-server`, `-rest-client`, `-all`), and `juneau-distrib` now declare the new module. Other consumers (`juneau-rest-mock`, `juneau-microservice`, `juneau-rest-server-springboot`, `juneau-rest-client-apache-httpclient-*`) pick it up transitively.
- Documentation updated in `juneau-docs`: release notes (`9.5.0.md`), migration guide (`23.01.V9.5-migration-guide.md`), and topic page (`09.01.JuneauRestCommonBasics.md`).
- Verification gate: `mvn install` succeeds; `mvn test` runs 48,208 unit tests with 0 failures / 0 errors / 20 skipped.

---

Source: created on 2026-05-19 as a follow-on to **TODO-38** (which split the REST client into `juneau-rest-client` + `juneau-rest-client-classic`) and **TODO-40** (which tracks rewriting `juneau-rest-server` / `juneau-rest-common` consumers to transport-neutral types).

## Goal

Physically split today's `juneau-rest-common` module into two artifacts:

1. **`juneau-rest-common`** — transport-neutral, contains only `org.apache.juneau.http.*` (excluding `.classic.*`), `org.apache.juneau.http.annotation.*`, `org.apache.juneau.http.entity.*`, `org.apache.juneau.http.header.*`, `org.apache.juneau.http.part.*`, `org.apache.juneau.http.remote.*`, `org.apache.juneau.http.resource.*`, `org.apache.juneau.http.response.*`. **No** `org.apache.httpcomponents:*` in its `dependency:tree`.
2. **`juneau-rest-common-classic`** — contains the entire `org.apache.juneau.http.classic.*` subtree (171 source files). Declares `httpcore` 4.4.16 as a `compile` dependency and depends on `juneau-rest-common` for the transport-neutral siblings.

This TODO is a **strict structural split** — it does **not** rewrite consumers (`juneau-rest-server`, `juneau-rest-mock`, examples, etc.). Consumers that need the classic facades pick up an explicit dependency on `juneau-rest-common-classic`. The longer-term work of changing those consumers to use transport-neutral types lives in **TODO-40** and the eventual "all `juneau-rest-*` modules depend only on the pure `juneau-rest-common`" follow-up.

End state:

- `mvn -pl juneau-rest/juneau-rest-common -am dependency:tree | grep -i 'httpcomponents'` returns **nothing**.
- `mvn -pl juneau-rest/juneau-rest-common-classic -am dependency:tree` shows `org.apache.httpcomponents:httpcore:4.4.16` exactly as today's `juneau-rest-common` does.
- `git grep -nE '^import\s+org\.apache\.http\.' juneau-rest/juneau-rest-common/src` returns **nothing** (it currently returns ~140 hits, all under `classic/`).
- All existing tests pass — split is invisible to working code.
- `juneau-distrib`, `juneau-shaded-rest-server`, `juneau-shaded-rest-client`, `juneau-shaded-all` ship the new `juneau-rest-common-classic` artifact alongside `juneau-rest-common`.

## Background — current state (as of 2026-05-19, post-TODO-38)

`juneau-rest-common/src/main/java` contains **365 source files**. The HC 4.5 footprint is **already isolated** under `org/apache/juneau/http/classic/`:

| Package | Files | HC 4.5? |
|---|---|---|
| `org.apache.juneau.http` (top-level facades) | 10 | partial (see "Cross-references" below) |
| `org.apache.juneau.http.annotation` | 22 | no |
| `org.apache.juneau.http.entity` | 7 | no |
| `org.apache.juneau.http.header` | 67 | no (see "Cross-references") |
| `org.apache.juneau.http.part` | 3 | no |
| `org.apache.juneau.http.remote` | 12 | no (see "Cross-references") |
| `org.apache.juneau.http.resource` | 2 | no |
| `org.apache.juneau.http.response` | 62 | no |
| `org.apache.juneau.http.classic` (root + 6 subpackages: `header`, `response`, `entity`, `part`, `resource`, `remote`) | **171** | **yes — every file** |

`rg '^import org\.apache\.http\.' juneau-rest/juneau-rest-common/src/main/java` finds matches **only** inside `org/apache/juneau/http/classic/`. TODO-38 Option 1 already did the heavy lifting of partitioning the source tree by HC 4.5 footprint.

### Cross-references that block a naive split

There are **6 non-classic files that import classic types** today. These are the only barrier to a clean module split, and they must be untangled first:

| File | Classic type imported | Disposition |
|---|---|---|
| `org/apache/juneau/http/HttpHeaders.java` | `classic.header.EntityTag`, `classic.header.EntityTags` | Move the two `entityTag(...)` / `entityTags(...)` static factory overloads that return `classic.*` into a parallel `classic/HttpHeaders.java` facade (or remove if redundant with existing classic facade). |
| `org/apache/juneau/http/header/HttpEntityTagHeader.java` | `classic.header.EntityTag` | Pure type-narrowing — replace with the transport-neutral `org.apache.juneau.http.header.EntityTag` (already exists). |
| `org/apache/juneau/http/header/HttpEntityTagsHeader.java` | `classic.header.EntityTags` | Same as above. |
| `org/apache/juneau/http/header/IfMatch.java` | `classic.header.EntityTags` | Same as above. |
| `org/apache/juneau/http/header/IfNoneMatch.java` | `classic.header.EntityTags` | Same as above. |
| `org/apache/juneau/http/remote/Remote.java` | `classic.header.*` (wildcard, used in `{@link}` only) | Re-resolve javadoc links — either FQCN-link to the moved classes or drop the `{@link}` to plain text. |

Each one is a small fix; none requires a behavior change.

### Tests

`juneau-rest-common/src/test/java` is empty (`find ... | wc -l` → 0). All `juneau-rest-common` tests live under `juneau-utest`. Tests already split by package: `juneau-utest/src/test/java/org/apache/juneau/http/classic/**` (HC 4.5–typed) vs `juneau-utest/src/test/java/org/apache/juneau/http/**` (transport-neutral). The split needs `juneau-utest`'s POM to depend on **both** new modules; otherwise no per-test changes are needed.

### Module consumers (today)

`<artifactId>juneau-rest-common</artifactId>` is referenced from:

- `juneau-rest/juneau-rest-server/pom.xml`
- `juneau-rest/juneau-rest-client/pom.xml`
- `juneau-rest/juneau-rest-client-classic/pom.xml`
- `juneau-shaded/juneau-shaded-rest-client/pom.xml`
- `juneau-shaded/juneau-shaded-rest-server/pom.xml`
- `juneau-distrib/pom.xml`
- (self-reference inside `juneau-rest-common/pom.xml` is the bundle's own coordinates)

Every one of these consumers will need an audit: do they use anything from `org.apache.juneau.http.classic.*`? If yes, they pull in `juneau-rest-common-classic`; if no, they stay on the pure module.

## Scope (in-tree)

### New module: `juneau-rest/juneau-rest-common-classic`

`pom.xml` (analogous to `juneau-rest-client-classic`):

```xml
<artifactId>juneau-rest-common-classic</artifactId>
<name>Apache Juneau REST Common (Classic)</name>
<description>Apache Juneau REST Common API — Apache HttpClient 4.5–compatible facades.</description>
<packaging>bundle</packaging>

<dependencies>
  <dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-rest-common</artifactId>
    <version>${project.version}</version>
  </dependency>
  <dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpcore</artifactId>
    <version>4.4.16</version>
  </dependency>
</dependencies>
```

Plus the standard `maven-bundle-plugin` / `maven-source-plugin` / `maven-jar-plugin` block matching the other modules. OSGi manifest will be auto-generated from the package layout.

### File moves

- Move `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/classic/**` → `juneau-rest/juneau-rest-common-classic/src/main/java/org/apache/juneau/http/classic/**` via `git mv` (171 files; package declarations are unchanged because the package path is preserved).
- No source content changes for moved files (besides untangling the 6 cross-references above, which happen **before** the move).

### `juneau-rest-common/pom.xml`

- Remove the `org.apache.httpcomponents:httpcore` dependency.
- No other changes needed; `juneau-commons`, `juneau-marshall`, `juneau-assertions` stay.

### `juneau-rest/pom.xml`

- Add `<module>juneau-rest-common-classic</module>` between `juneau-rest-common` and `juneau-rest-server` (immediately after `juneau-rest-common` so build order is correct).

### Consumer POMs

For each consumer module, decide whether it currently uses any `org.apache.juneau.http.classic.*` type. Audit via `rg '^import\s+org\.apache\.juneau\.http\.classic\.' <module>/src` per module.

Expected outcome (based on today's tree):

| Module | Uses `http.classic.*`? | Action |
|---|---|---|
| `juneau-rest-server` | yes (`RequestHeaders`, `RestRequest`, `RestResponse`, processors, static-files, assertions, …) | Add `juneau-rest-common-classic` dep; keep `juneau-rest-common`. |
| `juneau-rest-client` (canonical NG) | unknown — audit; expected **no** | Stay on `juneau-rest-common` only. |
| `juneau-rest-client-classic` | yes (whole module is HC 4.5–native) | Add `juneau-rest-common-classic` dep; keep `juneau-rest-common`. |
| `juneau-rest-mock` | yes (uses both classic mock and shared mock) | Add `juneau-rest-common-classic` dep. |
| `juneau-rest-client-apache-httpclient-45` | likely yes | Add `juneau-rest-common-classic` dep. |
| `juneau-rest-client-apache-httpclient-50` / `…-java-httpclient` / `…-okhttp` / `…-jetty` | expected **no** | Stay on `juneau-rest-common` only — verify with audit. |
| `juneau-shaded-rest-server` | yes (shades the server stack incl. classic types) | Add `juneau-rest-common-classic` dep + shade config entry. |
| `juneau-shaded-rest-client` | yes (shades both canonical + classic clients) | Add `juneau-rest-common-classic` dep + shade config entry. |
| `juneau-shaded-all` | yes (shades everything) | Add `juneau-rest-common-classic` dep + shade config entry. |
| `juneau-microservice/juneau-microservice` | yes (re-exports classic facades for the templates) | Add `juneau-rest-common-classic` dep. |
| `juneau-microservice/juneau-my-jetty-microservice` / `juneau-my-springboot-microservice` | inherits via microservice | No direct change expected. |
| `juneau-examples/juneau-examples-rest` / `juneau-examples-rest-jetty` / `juneau-examples-rest-springboot` | yes (uses classic facades) | Add `juneau-rest-common-classic` dep. |
| `juneau-distrib/pom.xml` | n/a — distribution-only | Add `artifactItem` entries to emit sources, jar, and OSGi bundle for `juneau-rest-common-classic`. |
| `juneau-utest/pom.xml` | yes (tests touch both halves) | Add `juneau-rest-common-classic` to test scope. |

### Documentation

After this TODO lands:

- `juneau-docs/pages/release-notes/9.5.0.md` — add a sub-bullet under the existing TODO-38 "Classic Module Split" section: "`juneau-rest-common` has been split into `juneau-rest-common` (transport-neutral) and `juneau-rest-common-classic` (Apache HttpClient 4.5–compatible facades). Existing imports unchanged; consumers that referenced `org.apache.juneau.http.classic.*` must add the new module as a dependency."
- `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` — add a Maven coordinate table row for `juneau-rest-common-classic` next to the existing `juneau-rest-client-classic` row.
- `juneau-docs/pages/topics/20.03.JuneauShadedRestClient.md`, `20.06.JuneauShadedAll.md`, and the equivalent server-shaded topic — list `juneau-rest-common-classic` under "What's Included".
- `juneau-docs/pages/topics/12.15.NextGenRestClient.md` — extend the "Package Layout" table with the `juneau-rest-common-classic` row.

## Risk / decisions

- **No behavior changes.** This is a Maven-coordinate split only; no source content moves between packages, no API renames. The split is invisible to any consumer that already correctly imports its types.
- **`org.apache.juneau.http.HttpHeaders` cross-reference.** The transport-neutral `HttpHeaders` facade has two static methods (`entityTag(...)` / `entityTags(...)`) that today return classic-typed values. They must move (to a sibling classic facade) before the file-tree split — otherwise the pure module will not compile. This is the only **content** change in the plan; the rest is pure `git mv` + POM edits.
- **`juneau-utest` is the integration test.** Once both modules build, the full unit test suite under `juneau-utest` must pass without any test changes (other than the pom dep). If it doesn't, the split is leaking. This is the primary gate.
- **OSGi exports.** `maven-bundle-plugin` auto-generates the `Export-Package` header from the package layout. Verify that:
  - The new `juneau-rest-common-classic-9.5.0-SNAPSHOT.jar` exports `org.apache.juneau.http.classic.*` (and nothing else from the legacy namespace).
  - The trimmed `juneau-rest-common-9.5.0-SNAPSHOT.jar` no longer exports `org.apache.juneau.http.classic.*`.
- **Eclipse `.classpath` / `.project` refresh.** Each consumer module's IDE files may need a refresh after the dep change. As with TODO-38, these files are mostly gitignored or auto-managed by m2e — no manual intervention expected.
- **Release-line compatibility.** Existing 9.4.x users upgrading to 9.5.0 already see a binary-compatible classic facade at the same FQCNs (`org.apache.juneau.http.classic.*` was introduced in TODO-38). This TODO does not change FQCNs further; it only changes Maven coordinates. Compatibility-by-FQCN is preserved.

## Steps

### Step 1 — Untangle the 6 cross-references

Before any `git mv`, edit the 6 non-classic files listed in "Cross-references" so that none of them imports `org.apache.juneau.http.classic.*`:

1. `HttpHeaders.java` (top-level) — relocate the two `entityTag(...)` / `entityTags(...)` overloads that return classic types into a new `classic/HttpHeadersClassic.java` (or fold into existing `classic/HttpHeaders.java` if there's an obvious home).
2. `HttpEntityTagHeader.java`, `HttpEntityTagsHeader.java`, `IfMatch.java`, `IfNoneMatch.java` — re-point the import to `org.apache.juneau.http.header.EntityTag` / `EntityTags` (the transport-neutral siblings that already exist).
3. `Remote.java` — replace the wildcard javadoc-link import with FQCN `{@link}`s (or convert the references to plain `<code>` blocks) so the file no longer imports `classic.*`.

Compile gate: `mvn -pl juneau-rest/juneau-rest-common -am test-compile`. Then run `rg '^import org\.apache\.juneau\.http\.classic\.' juneau-rest/juneau-rest-common/src/main/java | grep -v '/classic/'` — must be empty.

### Step 2 — Create the new module skeleton

1. `mkdir -p juneau-rest/juneau-rest-common-classic/src/main/java`.
2. Write `juneau-rest/juneau-rest-common-classic/pom.xml` (template above).
3. Copy `juneau-rest/juneau-rest-common-classic/.gitignore` from `juneau-rest-client-classic/` for parity.
4. Add `<module>juneau-rest-common-classic</module>` to `juneau-rest/pom.xml` immediately after `juneau-rest-common`.

Compile gate: `mvn -pl juneau-rest/juneau-rest-common-classic -am install` (empty module — should succeed).

### Step 3 — Move `http.classic.*` to the new module

```bash
git mv juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/classic \
       juneau-rest/juneau-rest-common-classic/src/main/java/org/apache/juneau/http/classic
```

(171 files in a single rename; package declarations require no edits.)

### Step 4 — Trim `juneau-rest-common/pom.xml`

Remove the `org.apache.httpcomponents:httpcore` dependency stanza. Run `mvn -pl juneau-rest/juneau-rest-common -am dependency:tree | grep -i 'httpcomponents'` — must be empty.

### Step 5 — Update consumer POMs

For each consumer in the "Module consumers" audit table, add the `juneau-rest-common-classic` dependency where the audit says it's needed.

Compile gate per module: `mvn -pl <module> -am test-compile`.

Full gate: `mvn -DskipTests install` from repo root.

### Step 6 — Shaded + distrib

1. `juneau-shaded-rest-server/pom.xml`, `juneau-shaded-rest-client/pom.xml`, `juneau-shaded-all/pom.xml` — add `juneau-rest-common-classic` to `<dependencies>` and to the shade-plugin's `<artifactSet><includes>` (or `<filters>`) so the classic classes are physically shaded into the uber-jar.
2. `juneau-distrib/pom.xml` — add `artifactItem` entries for `juneau-rest-common-classic-${project.version}.jar`, `…-sources.jar`, and the OSGi bundle.

Verify each shaded jar with `unzip -l` includes the classic classes.

### Step 7 — Tests

`juneau-utest/pom.xml`: add `juneau-rest-common-classic` as a test-scope dep so the classic tests can resolve their types.

Full gate: `python3 scripts/test.py --full`.

### Step 8 — Documentation

Update the docs listed in the "Documentation" section above. Keep wording consistent with TODO-38's existing migration narrative — same "classic vs transport-neutral" framing.

### Step 9 — IDE / build-tooling

Same as TODO-38 Step 9: no manual `.project` / `.classpath` / `.settings` changes expected; m2e regenerates from the POMs.

## Verification

1. `python3 scripts/test.py --full` — all tests pass.
2. `mvn -pl juneau-rest/juneau-rest-common -am dependency:tree | grep -i 'httpcomponents'` — empty.
3. `mvn -pl juneau-rest/juneau-rest-common-classic -am dependency:tree | grep -i 'httpcomponents'` — shows `httpcore:4.4.16`.
4. `rg '^import org\.apache\.http\.' juneau-rest/juneau-rest-common/src/main/java` — empty.
5. `unzip -l juneau-rest/juneau-rest-common/target/juneau-rest-common-9.5.0-SNAPSHOT.jar | grep '/classic/'` — empty.
6. `unzip -l juneau-rest/juneau-rest-common-classic/target/juneau-rest-common-classic-9.5.0-SNAPSHOT.jar | grep '\.class$' | wc -l` — approximately 171.
7. OSGi manifest of `juneau-rest-common-classic`'s bundle exports `org.apache.juneau.http.classic.*` (visible via `unzip -p … META-INF/MANIFEST.MF | grep Export-Package`).
8. Sample consumer: a downstream project that depends only on `juneau-rest-common` (no `-classic`) compiles and runs without seeing any classic types.

## Future work (out of scope)

- **TODO-40** — rewrite `juneau-rest-server` / `juneau-rest-common` consumers to use the transport-neutral `org.apache.juneau.http.*` types instead of the `classic.*` ones. Once that lands, every consumer table row currently saying "Add `juneau-rest-common-classic` dep" can drop back to `juneau-rest-common` only, and the long-term goal — "all `juneau-rest-*` modules depend only on the pure `juneau-rest-common`" — is achieved.
- A possible later "Drop `juneau-rest-common-classic`" TODO, after TODO-40 finishes and the classic module has no internal Juneau consumers (only external user code still on HC 4.5). At that point the module can stay as a stable compatibility artifact, or be archived to a maintenance line — that decision is deferred until TODO-40 closes.
