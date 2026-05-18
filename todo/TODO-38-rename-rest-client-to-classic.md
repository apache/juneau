# TODO-38: Rename `juneau-rest-client` → `juneau-rest-client-classic` and `juneau-ng-rest-client-*` → `juneau-rest-client-*`

Source: created on 2026-05-18.

## Goal

Promote the next-generation, transport-agnostic REST client (today living under the `juneau-ng-rest-client-*` modules and the `org.apache.juneau.ng.*` packages) to the canonical `juneau-rest-client` name, and demote the existing Apache HttpClient 4.5-coupled client to `juneau-rest-client-classic`. Same swap for the `juneau-rest-mock` content: the new transport-agnostic mock (`org.apache.juneau.ng.rest.mock.NgMockRestClient`) becomes the canonical mock; the legacy mock becomes the "classic" mock.

End state:

| Today | After |
|---|---|
| `juneau-rest/juneau-rest-client` (Apache HC 4.5 bound) | `juneau-rest/juneau-rest-client-classic` |
| `juneau-rest/juneau-ng-rest-client-apache-httpclient-45` | `juneau-rest/juneau-rest-client-apache-httpclient-45` |
| `juneau-rest/juneau-ng-rest-client-apache-httpclient-50` | `juneau-rest/juneau-rest-client-apache-httpclient-50` |
| `juneau-rest/juneau-ng-rest-client-java-httpclient` | `juneau-rest/juneau-rest-client-java-httpclient` |
| `juneau-rest/juneau-ng-rest-client-okhttp` | `juneau-rest/juneau-rest-client-okhttp` |
| `juneau-rest/juneau-ng-rest-client-jetty` | `juneau-rest/juneau-rest-client-jetty` |
| `org.apache.juneau.ng.rest.client.*` (in `juneau-rest-client`) | `org.apache.juneau.rest.client.*` (in **new** `juneau-rest-client`) |
| `org.apache.juneau.rest.client.*` (legacy, in `juneau-rest-client`) | `org.apache.juneau.rest.client.classic.*` (in `juneau-rest-client-classic`) |
| `org.apache.juneau.ng.http.*` (in `juneau-rest-common`) | `org.apache.juneau.http.*` (canonical) |
| `org.apache.juneau.http.*` (legacy, in `juneau-rest-common`) | `org.apache.juneau.http.classic.*` |
| `org.apache.juneau.ng.rest.mock.*` (in `juneau-rest-mock`) | `org.apache.juneau.rest.mock.*` (canonical) |
| `org.apache.juneau.rest.mock.*` (legacy) | `org.apache.juneau.rest.mock.classic.*` |
| Type `NgRestClient` | Type `RestClient` |
| Type `NgMockRestClient` | Type `MockRestClient` |

Net effect: from a user's point of view, `<artifactId>juneau-rest-client</artifactId>` in their `pom.xml` and `import org.apache.juneau.rest.client.RestClient;` resolve to the new transport-abstracted client. The Apache HC 4.5 client remains available, but explicitly opted into via `<artifactId>juneau-rest-client-classic</artifactId>`.

## Why now

The next-generation client design plan (`todo/FINISHED-11a-restclient-ng-design-plan.md`) and the coverage closeout (`todo/FINISHED-11b-restclient-ng-coverage-closeout.md`) are landed. The `.ng.` prefix on packages and the `-ng-` infix on artifact ids were always intended to be **temporary scaffolding** so the old and new clients could coexist during the migration. With the new client feature-complete and tested, the scaffolding can come off and the new client can take the canonical name.

## Scope (in-tree)

### Maven artifacts to rename
- `juneau-rest/juneau-rest-client` → `juneau-rest/juneau-rest-client-classic`
- `juneau-rest/juneau-ng-rest-client-apache-httpclient-45` → `juneau-rest/juneau-rest-client-apache-httpclient-45`
- `juneau-rest/juneau-ng-rest-client-apache-httpclient-50` → `juneau-rest/juneau-rest-client-apache-httpclient-50`
- `juneau-rest/juneau-ng-rest-client-java-httpclient` → `juneau-rest/juneau-rest-client-java-httpclient`
- `juneau-rest/juneau-ng-rest-client-okhttp` → `juneau-rest/juneau-rest-client-okhttp`
- `juneau-rest/juneau-ng-rest-client-jetty` → `juneau-rest/juneau-rest-client-jetty`
- **Create new** `juneau-rest/juneau-rest-client` aggregator-style module that holds the (renamed) `org.apache.juneau.rest.client.*` source. Concretely this is the split of today's `juneau-rest-client` (which carries both legacy and `.ng.` packages) into **two** modules: `juneau-rest-client` (was `.ng.`) and `juneau-rest-client-classic` (legacy).

### Packages to rename

In `juneau-rest-common` (no module rename — the module is shared by both clients):

- `org.apache.juneau.ng.http` → `org.apache.juneau.http`
- `org.apache.juneau.ng.http.entity` → `org.apache.juneau.http.entity`
- `org.apache.juneau.ng.http.header` → `org.apache.juneau.http.header`
- `org.apache.juneau.ng.http.part` → `org.apache.juneau.http.part`
- `org.apache.juneau.ng.http.response` → `org.apache.juneau.http.response`
- The **existing** `org.apache.juneau.http.*` packages in `juneau-rest-common` (Apache HC 4.5-typed `BasicHeader implements org.apache.http.Header`, etc.) move to `org.apache.juneau.http.classic.*` and ship in `juneau-rest-client-classic` instead of `juneau-rest-common` — see "Common-module split" below.

In the (renamed) `juneau-rest-client`:

- `org.apache.juneau.ng.rest.client` → `org.apache.juneau.rest.client`
- `org.apache.juneau.ng.rest.client.assertion` → `org.apache.juneau.rest.client.assertion`
- `org.apache.juneau.ng.rest.client.remote` → `org.apache.juneau.rest.client.remote`

In (the new) `juneau-rest-client-classic`:

- The legacy `org.apache.juneau.rest.client.*` source from today's `juneau-rest-client` moves here under the same package name **with `.classic` inserted** (`org.apache.juneau.rest.client.classic.*`). Keeping the package name distinct avoids type-name collisions when both jars are on the classpath (intentional during migration; some users will need both).

In `juneau-rest-mock`:

- `org.apache.juneau.ng.rest.mock` → `org.apache.juneau.rest.mock` (canonical mock).
- Legacy `org.apache.juneau.rest.mock.*` (built on Apache HC 4.5) → `org.apache.juneau.rest.mock.classic`.

### Types to rename

- `NgRestClient` → `RestClient` (the new canonical name).
- `NgMockRestClient` → `MockRestClient`.
- The legacy `RestClient` and `MockRestClient` stay named the same, but live in the `.classic` package, so the FQN changes (`org.apache.juneau.rest.client.RestClient` → `org.apache.juneau.rest.client.classic.RestClient`).

## Steps

### 1. Common-module split (decision before code moves)

Today, `juneau-rest-common` contains **both** the new transport-neutral `org.apache.juneau.ng.http.*` types **and** the Apache HC 4.5-typed `org.apache.juneau.http.*` types. After the rename, `juneau-rest-common` should hold only the transport-neutral types (which become `org.apache.juneau.http.*`). The legacy HC 4.5-typed classes need to leave `juneau-rest-common` because pulling `juneau-rest-common` should no longer drag in `org.apache.httpcomponents:httpcore`.

Move the legacy HC 4.5-typed types out of `juneau-rest-common` into `juneau-rest-client-classic` under `org.apache.juneau.http.classic.*` (~186 classes per the inventory in `FINISHED-11a`). The legacy `juneau-rest-server`'s Apache HC 4.5 dependency goes with them — server-side request/response wrapping that today uses `org.apache.http.Header` etc. stays on the legacy stack but resolves through `juneau-rest-client-classic`'s relocated types (or, more cleanly, through a new `juneau-rest-server-classic` if one is needed — out of scope for this TODO; flag for follow-up).

### 2. Maven module renames + new `juneau-rest-client` module

1. Rename folders:
   - `juneau-rest/juneau-rest-client` → `juneau-rest/juneau-rest-client-classic`
   - `juneau-rest/juneau-ng-rest-client-*` → `juneau-rest/juneau-rest-client-*` (five modules)
2. Create new folder `juneau-rest/juneau-rest-client` that contains:
   - The (renamed) `org.apache.juneau.rest.client.*` source from the legacy module (the new client).
   - A trimmed `pom.xml` whose dependencies are limited to `juneau-rest-common` (no Apache HC 4.5).
3. Update every `<artifactId>` and folder-relative `<module>` reference:
   - `juneau-rest/pom.xml` aggregator.
   - `juneau-shaded/juneau-shaded-all/pom.xml`, `juneau-shaded/juneau-shaded-rest-client/pom.xml`.
   - `juneau-distrib/pom.xml` (lines 486-506 reference `juneau-rest-client` — these need to fan out to `juneau-rest-client-classic` for the legacy distrib bundle, plus the new `juneau-rest-client` for the new distrib bundle, and pick up each renamed transport module).
   - `juneau-microservice/juneau-microservice-core/pom.xml`.
   - `juneau-rest/juneau-rest-mock/pom.xml`.
   - Test module `juneau-utest/pom.xml`.
4. Update `<artifactId>` inside each renamed pom.xml itself.

### 3. Package renames

Use a single pass per module. For each rename, update:

- Folder structure under `src/main/java/` and `src/test/java/`.
- `package …;` declaration in every file.
- `import …;` declarations everywhere that references the renamed package (cross-module — `juneau-rest-server`, `juneau-rest-mock`, `juneau-utest`, examples, microservice).
- `META-INF/services/` SPI files (e.g. `org.apache.juneau.ng.rest.client.HttpTransportProvider` → `org.apache.juneau.rest.client.HttpTransportProvider`).

Order:
1. Rename packages inside the (renamed) `juneau-rest-client` first (`org.apache.juneau.ng.rest.client.*` → `org.apache.juneau.rest.client.*`). This forces a temporary collision with the legacy `org.apache.juneau.rest.client.*` source which lives in the same physical module today — resolve by moving the legacy source into `juneau-rest-client-classic` and renaming its packages to `.classic.*` in the same commit.
2. Rename `org.apache.juneau.ng.http.*` → `org.apache.juneau.http.*` in `juneau-rest-common`. Move the legacy `org.apache.juneau.http.*` types into `juneau-rest-client-classic` under `org.apache.juneau.http.classic.*` (step 1 of the plan handles this) — they leave `juneau-rest-common` entirely.
3. Rename `org.apache.juneau.ng.rest.mock.*` → `org.apache.juneau.rest.mock.*` in `juneau-rest-mock`; move the legacy `org.apache.juneau.rest.mock.*` types to `org.apache.juneau.rest.mock.classic.*`.
4. Repeat for the five transport-adapter modules.

### 4. Type renames

- `NgRestClient` → `RestClient` (FQN: `org.apache.juneau.rest.client.RestClient`).
- `NgMockRestClient` → `MockRestClient` (FQN: `org.apache.juneau.rest.mock.MockRestClient`).
- Legacy `RestClient` keeps its class name but its FQN changes to `org.apache.juneau.rest.client.classic.RestClient`.
- Legacy `MockRestClient` keeps its class name but its FQN changes to `org.apache.juneau.rest.mock.classic.MockRestClient`.

Cross-references in javadoc (`{@link …}`, `{@code …}`, `<c>…</c>`) and in non-Java files (`.md` docs, sidebar entries, knowledge JSONL) need the same rename — single search-and-replace per FQN pattern.

### 5. Shaded / distrib renames

- `juneau-shaded/juneau-shaded-rest-client` — today shades the legacy client; after the swap, decide whether the "rest client" shaded jar tracks the new or the classic client. Default: track the **new** client (since that's now the canonical name). Add `juneau-shaded-rest-client-classic` if shading the legacy one is still desired (likely yes, given external users on HC 4.5).
- `juneau-shaded/juneau-shaded-all` — pick up both new and classic dependencies.
- `juneau-distrib/pom.xml` — emit bundles for both: `apache-juneau-rest-client-<ver>.jar` (new) and `apache-juneau-rest-client-classic-<ver>.jar` (legacy).

### 6. Tests

- Move and rename test classes that mirror the package changes (test classes live alongside source under `juneau-utest`).
- `NgRestClient_*_Test` → `RestClient_*_Test` (replacing the legacy `RestClient_*_Test` which moves to a `classic/` subpackage if it's retained).
- Verify the SPI ServiceLoader test for `HttpTransportProvider` resolves under the new package.
- Per `AGENTS.md`, run `./scripts/test.py` (full clean build + tests) after the renames to catch fallout from `META-INF/services` paths, javadoc `@link` references, OSGi bundle headers (in `pom.xml` plugin config), and Eclipse `.project`/`.settings` files.

### 7. Docs (`juneau-docs`)

Affected files (from the grep above):

- `pages/topics/12.15.NextGenRestClient.md` — content is no longer "next-gen"; either rename the page slug to `12.15.RestClient.md` and inline its content into the main RestClient section, or fold it into `12.01.JuneauRestClientBasics.md` and delete the page.
- `pages/topics/12.01.JuneauRestClientBasics.md` — currently describes the legacy client; rewrite to describe the new transport-agnostic client, with a "Classic client" section pointing users at `juneau-rest-client-classic`.
- `pages/topics/01.05.RestClient.md`, `pages/topics/01.03.EndToEndRest.md` — overview pages; replace `NgRestClient` references with `RestClient` and add a one-paragraph note that the HC 4.5 client is now `juneau-rest-client-classic`.
- `pages/topics/19.02.JuneauExamplesRest.md` — example references.
- `pages/topics/20.01.JuneauShadedOverview.md`, `20.03.JuneauShadedRestClient.md`, `20.06.JuneauShadedAll.md` — update shaded jar artifact-id references.
- `sidebars.ts` — drop the dedicated "NextGenRestClient" sidebar entry; the new client is just "RestClient" now.
- `static/ai/juneau-knowledge.jsonl` — regenerate (or scripted edit) so the embedded references match the renamed FQNs.
- `README.md`, `src/pages/downloads.md`, `src/pages/about.md` — quick grep + replace for `juneau-ng-rest-client` artifact ids.
- **Release notes** `pages/release-notes/9.5.0.md` — new "Breaking changes" entries under `juneau-rest-client` and `juneau-rest-common` describing the rename, the new `juneau-rest-client-classic` artifact, and the package moves (`.ng.` removed everywhere; legacy moves to `.classic.`).
- **Migration guide** (TODO-17 v9.5 file at `pages/topics/23.01.V9.5-migration-guide.md`) — Old → New rows for every renamed artifact id, package, and type listed above.

### 8. Examples + microservice templates

- `juneau-examples/juneau-examples-rest-*` — update `pom.xml` deps to either `juneau-rest-client` (new) or `juneau-rest-client-classic` (legacy). Default: switch examples to the **new** client so the canonical examples advertise the canonical API.
- `juneau-microservice/juneau-my-jetty-microservice` template — same.
- `juneau-microservice/juneau-microservice-core/pom.xml` — adjust the `juneau-rest-client` dependency (likely keep `juneau-rest-client`, now pointing at the new module).

### 9. IDE / build-tooling files

- `.project`, `.settings/org.eclipse.jdt.core.prefs` and `org.eclipse.jdt.ui.prefs` in every renamed module (Eclipse module names typically follow folder names).
- OSGi `Bundle-SymbolicName` and `Export-Package` entries in each renamed module's `pom.xml` (`maven-bundle-plugin` config).
- Any `target/` checkout scripts, `scripts/*.py` shortcuts, or `.cursor/commands/*.md` that hardcode the old artifact ids.

## Verification

1. `./scripts/test.py` from repo root (full clean build + tests).
2. `mvn -pl juneau-rest/juneau-rest-client -am dependency:tree` to confirm the new client has **no** `org.apache.httpcomponents` on its classpath.
3. `mvn -pl juneau-rest/juneau-rest-client-classic -am dependency:tree` to confirm the legacy client still has its Apache HC 4.5 dependency and pulls only `.classic.*` source.
4. Sample import — write a 5-line scratch program against `org.apache.juneau.rest.client.RestClient` and confirm it compiles against the renamed jars.
5. `git grep -nE 'juneau-ng-rest-client|org\.apache\.juneau\.ng\.'` returns **zero** hits in source (only in archive plans like `FINISHED-11a-restclient-ng-design-plan.md`).
6. Eclipse: re-import the workspace, confirm no module name conflicts and `Build Automatically` still produces a green workspace.

## Open questions

- **Strict reading of the user's request.** The TODO text says "rename `juneau-ng-rest-client` to `juneau-rest-client`", but there is **no** module called `juneau-ng-rest-client` in the tree — there is a family `juneau-ng-rest-client-apache-httpclient-45`, `…-50`, `…-java-httpclient`, `…-okhttp`, `…-jetty`. This plan assumes the intent is the whole family. Confirm before starting.
- **`org.apache.juneau.ng.http.*` in `juneau-rest-common`.** This plan removes the `.ng.` prefix everywhere, including in the shared `juneau-rest-common`, and relocates the legacy HC 4.5-typed siblings into `juneau-rest-client-classic`. Alternative: keep the legacy `org.apache.juneau.http.*` types in `juneau-rest-common` and rename the new ones to `org.apache.juneau.http2.*` or similar. The plan picks the cleaner long-term outcome; confirm before starting.
- **Shaded "RestClient" jar.** Decide which client the canonical `juneau-shaded-rest-client` shades. Plan assumes the new client; if external users on HC 4.5 are the dominant audience, swap defaults.
- **`juneau-rest-server` Apache HC 4.5 dependency.** The legacy server uses `org.apache.http.Header` etc. Out of scope for this TODO, but the move of legacy HC 4.5-typed classes out of `juneau-rest-common` will surface this. Likely needs a follow-up `juneau-rest-server-classic` split, or the server takes a direct dependency on `juneau-rest-client-classic` for the relocated types.

## Notes

- Keep the rename a **single atomic commit** (or one commit per module) so the migration guide diff is easy to read; partial states are unbuildable.
- All cross-module javadoc `{@link}` references break on package rename — IDE-assisted rename refactoring is mandatory here, not manual search-and-replace.
- The existing v9.5 migration guide (TODO-17) already tracks 9.2 → 9.5 breaking changes; this rename is the largest single 9.5 break and should get a top-level call-out at the head of the migration guide.
- After this TODO lands, the FINISHED archive files (`FINISHED-11a-restclient-ng-design-plan.md`, `FINISHED-11b-restclient-ng-coverage-closeout.md`) stay verbatim — they are historical record of the `.ng.` scaffolding, not live docs.
