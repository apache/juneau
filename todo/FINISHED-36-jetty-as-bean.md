# TODO-36 — Collapse `juneau-microservice-jetty` into a predefined bean on top of a single `Microservice`

Source: promoted from `TODO.md` on 2026-05-18.

---

## Goal

Make `Microservice` (in [juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/Microservice.java](../juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/Microservice.java)) the single, non-subclassed microservice class. `juneau-microservice-jetty` becomes a thin module that contributes **one** `@Configuration` class (`JettyConfiguration`) plus a bean that drives the Jetty server lifecycle through standard `MicroserviceListener` hooks. No more `JettyMicroservice extends Microservice`. Module `juneau-microservice-core` is renamed to `juneau-microservice` since it is no longer "the core of a hierarchy".

End-state usage:

```java
Microservice
    .create()
    .args(args)
    .configurations(JettyConfiguration.class, AppConfig.class)
    .build()
    .start()
    .startConsole()
    .join();
```

where `AppConfig` is the user's own `@Configuration` class contributing `@Bean Servlet`s (auto-mounted via `@Rest(path=...)`) and optionally a `@Bean JettySettings` for programmatic knobs (ports, jetty.xml, factory).

---

## Why this is feasible now

The 9.5 inject-aware bootstrap already does most of the heavy lifting:

- `Microservice` owns a `WritableBeanStore` populated from `@Configuration` classes, with builder > `@Bean` > default field resolution and `@PreDestroy` on `stop()`.
- `JettyMicroservice.createServer()` already auto-discovers `@Bean Server` and `@Bean Servlet` beans, mounts `@Rest`-annotated servlets at `@Rest(path=...)`, and hard-fails on path collisions — see [JettyMicroservice.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyMicroservice.java) lines 593–690.
- The test fixture in [MicroserviceTestFixture.java](../juneau-utest/src/test/java/org/apache/juneau/microservice/MicroserviceTestFixture.java) (via `EphemeralJettyServerConfig`) already demonstrates booting Jetty end-to-end with a single `@Bean Server` + `@Bean Servlet`s and no programmatic Builder knobs.

So the migration is: peel off the remaining "must be a subclass" pieces of `JettyMicroservice` (the start/stop overrides, the Jetty-specific listener, the static `getInstance()`) into a single bean that participates in `Microservice` lifecycle through the existing listener interface.

---

## Resolved design decisions

1. **API shape: pure `@Configuration`.** The user registers `JettyConfiguration.class` via `Microservice.Builder.configurations(...)`. No Jetty-specific methods leak into `Microservice.Builder`. Programmatic knobs (ports, jetty.xml content, factory) come from an optional `@Bean JettySettings` value bean.
2. **Compatibility: hard break.** Delete `JettyMicroservice`, `JettyMicroserviceListener`, and `BasicJettyMicroserviceListener` outright; rewrite all in-tree callers; document Old→New in the v9.5 migration guide.
3. **Lifecycle plumbing: reuse `MicroserviceListener`, fan it out.** Today `Microservice.start()`/`stop()` only invoke the single resolved primary listener (see [Microservice.java lines 1217 and 1250](../juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/Microservice.java)). Change them to iterate over **every** `MicroserviceListener` bean in the store (registration order for `onStart`, reversed for `onStop`). The Jetty bean implements `MicroserviceListener` and gets driven for free. No new SPI surface.

---

## Architecture

```mermaid
flowchart TD
    User[User main]
    User -->|configurations JettyConfiguration AppConfig| Builder[Microservice.Builder]
    Builder -->|build| MS[Microservice]
    MS --> Store[(WritableBeanStore)]
    JC[JettyConfiguration] -->|@Bean| Store
    AC[AppConfig] -->|@Bean Servlet| Store
    Store --> Settings[JettySettings]
    Store --> Factory[JettyServerFactory]
    Store --> Server[org.eclipse.jetty.server.Server]
    Store --> Component[JettyServerComponent : MicroserviceListener]
    MS -->|start fan-out| Component
    Component -->|mount @Rest servlets + server.start| Server
    MS -->|stop fan-out reverse| Component
    Component -->|server.stop| Server
```

---

## Steps

### 1. Rename `juneau-microservice-core` → `juneau-microservice`

- The aggregator pom currently has `artifactId=juneau-microservice` (see [juneau-microservice/pom.xml](../juneau-microservice/pom.xml)). Rename aggregator to `juneau-microservice-parent` (still `<packaging>pom</packaging>`) so the name is free for the core jar.
- Rename module folder `juneau-microservice/juneau-microservice-core` → `juneau-microservice/juneau-microservice` and its `artifactId` to `juneau-microservice`.
- Update `<module>` entries in [juneau-microservice/pom.xml](../juneau-microservice/pom.xml).
- Update every `<artifactId>juneau-microservice-core</artifactId>` reference in the repo (at minimum: `juneau-microservice-jetty/pom.xml`, `juneau-my-jetty-microservice/pom.xml`, any aggregator BOMs / docs).
- Java package `org.apache.juneau.microservice` is **unchanged**. No class moves on the core side.

### 2. Fan out `MicroserviceListener` (small core change)

In [Microservice.start()](../juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/Microservice.java) and [Microservice.stop()](../juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/Microservice.java) replace the single `listener.onStart(this)` / `listener.onStop(this)` call with iteration over **all** `MicroserviceListener` beans in the store:

- Start order = bean-store registration order; stop order = reverse.
- The builder-supplied / default-resolved primary listener (current `this.listener` field) is registered into the store like today and falls naturally into the fan-out.
- Recommend doing the same for `onConfigChange` (consistency); call out as part of the diff.

### 3. New Jetty-side classes

In `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/`:

- **`JettyConfiguration`** (new, `@Configuration`) — the single class the user adds to `Microservice.Builder.configurations(...)`. Contributes:
  - `@Bean @ConditionalOnMissingBean JettyServerFactory` → `BasicJettyServerFactory`.
  - `@Bean @ConditionalOnMissingBean JettySettings` → resolved from `Config`/`ManifestFile`/defaults (ports, jetty.xml location, resolveVars). Plain value/record bean.
  - `@Bean @ConditionalOnMissingBean Server` → built from `JettyServerFactory` + `JettySettings` + `Config` + `VarResolver` (port selection, jetty.xml load, var resolution). User-supplied `@Bean Server` wins — same behavior `JettyMicroservice.createServer()` has today.
  - `@Bean JettyServerComponent` → the lifecycle-participating bean.
- **`JettyServerComponent`** (new, implements `MicroserviceListener`) — owns the start/stop wiring that lives in `JettyMicroservice.start()`/`stop()` today:
  - `onStart(Microservice)` — mount config-driven servlets (`Jetty/servlets`, `Jetty/servletMap`, `Jetty/servletAttributes`), mount `@Bean Servlet`s by `@Rest(path=...)` (port the `mountWithCollisionCheck` logic from today's `JettyMicroservice.createServer()`), call `server.start()`, set `availablePort` / `juneau.serverPort` sys props.
  - `onStop(Microservice)` — mirror of today's `JettyMicroservice.stop()` (graceful server stop, log, join in a worker thread).
  - Public getters `getServer()`, `getPort()`, `getProtocol()`, `getURI()`, `getContextPath()`, `getServletContextHandler()`, `addServlet(...)`, `addServletAttribute(...)` migrated 1:1 from `JettyMicroservice`. Callers reach this bean via `Microservice.getInstance().getBeanStore().getBean(JettyServerComponent.class).orElseThrow()`.
- **`JettySettings`** (new) — immutable record-style bean: `ports`, `jettyXml`, `jettyXmlResolveVars`. Builder pattern. Users who need programmatic config supply `@Bean JettySettings`; everyone else gets the defaults that today's builder fields fall back to.
- **Keep**: [JettyServerFactory.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyServerFactory.java), [BasicJettyServerFactory.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/BasicJettyServerFactory.java), [JettyLogger.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyLogger.java), [resources/DebugResource.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/resources/DebugResource.java) (rewritten to look up `JettyServerComponent` via the bean store), and `JettyMicroservice.properties` (rename to `JettyServerComponent.properties` for the messages bundle).
- **Delete** (hard break):
  - [JettyMicroservice.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyMicroservice.java) — all logic migrated into `JettyConfiguration` + `JettyServerComponent`.
  - [JettyMicroserviceListener.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyMicroserviceListener.java) — the Jetty-specific `onCreateServer`/`onStartServer`/`onStopServer` hooks fold into fanned-out `MicroserviceListener` beans, or are dropped (the events are easily observable by adding another `@Bean MicroserviceListener`).
  - [BasicJettyMicroserviceListener.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/BasicJettyMicroserviceListener.java).

### 4. Caller migration (hard break)

- [juneau-my-jetty-microservice/.../template/App.java](../juneau-microservice/juneau-my-jetty-microservice/src/main/java/org/apache/juneau/microservice/jetty/template/App.java) — rewrite to `Microservice.create().configurations(JettyConfiguration.class, MyAppConfig.class)...`. Add a small `MyAppConfig` with `@Bean Servlet rootResources() { return new RootResources(); }`.
- [juneau-examples-rest-jetty/.../App.java](../juneau-examples/juneau-examples-rest-jetty/src/main/java/org/apache/juneau/examples/rest/jetty/App.java) — same pattern.
- [juneau-sc-server/.../App.java](../juneau-sc/juneau-sc-server/src/main/java/org/apache/juneau/server/config/App.java) — same pattern.
- [SamplesMicroservice.java](../juneau-examples/juneau-examples-rest-jetty-ftest/src/test/java/org/apache/juneau/examples/rest/SamplesMicroservice.java) — holds a `Microservice` reference; pulls `URI` from `JettyServerComponent` via the bean store.
- [DebugResource.java](../juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/resources/DebugResource.java) — `JettyMicroservice.getInstance().getServer()` → `Microservice.getInstance().getBeanStore().getBean(Server.class).orElseThrow()`.
- [MicroserviceTestFixture.java](../juneau-utest/src/test/java/org/apache/juneau/microservice/MicroserviceTestFixture.java) — drop the `JettyMicroservice` field, use `Microservice`. `EphemeralJettyServerConfig` already contributes `@Bean Server`; just add `JettyConfiguration.class` to the configurations list and pull the bound port from `JettyServerComponent` for `rootUrl`.

### 5. Tests

- Delete [JettyMicroservice_Inject_Test.java](../juneau-utest/src/test/java/org/apache/juneau/microservice/jetty/JettyMicroservice_Inject_Test.java), [JettyMicroservice_Builder_Test.java](../juneau-utest/src/test/java/org/apache/juneau/microservice/jetty/JettyMicroservice_Builder_Test.java), and [BasicJettyMicroserviceListener_Test.java](../juneau-utest/src/test/java/org/apache/juneau/microservice/jetty/BasicJettyMicroserviceListener_Test.java).
- Add:
  - `JettyConfiguration_Test` — default beans present; `@ConditionalOnMissingBean` overrides work; `JettySettings` defaults pull from `Config`/`ManifestFile`.
  - `JettyServerComponent_Test` — lifecycle (`onStart` mounts servlets and starts the server, `onStop` stops it); `@Rest` auto-mount; path-collision hard-fail; `getServer()` / `getURI()`.
  - `Microservice_Listener_Fanout_Test` (under [juneau-utest/.../microservice/](../juneau-utest/src/test/java/org/apache/juneau/microservice/)) — multiple `@Bean MicroserviceListener` beans all see `onStart` / `onStop` in the correct order.
- Keep [JettyLogger_Test.java](../juneau-utest/src/test/java/org/apache/juneau/microservice/jetty/JettyLogger_Test.java) unchanged.

### 6. Docs (`juneau-docs`)

- **Sidebar** [juneau-docs/sidebars.ts](../../juneau-docs/sidebars.ts) — rename section "14. juneau-microservice-core" → "14. juneau-microservice". Section 15 stays "juneau-microservice-jetty" but its sub-pages get rewritten.
- **14.01** [JuneauMicroserviceCoreBasics.md](../../juneau-docs/pages/topics/14.01.JuneauMicroserviceCoreBasics.md) → rename to `JuneauMicroserviceBasics.md` (slug + title + artifactId block).
- **14.09** [InjectAwareMicroservice.md](../../juneau-docs/pages/topics/14.09.InjectAwareMicroservice.md) — update the "what lands in the bean store" table (drop `JettyMicroservice`-specific row, replace with `Server` / `JettyServerComponent` rows under a `JettyConfiguration` heading).
- **15.01 / 15.02 / 15.07 / 15.09** [JuneauMicroserviceJettyBasics.md](../../juneau-docs/pages/topics/15.01.JuneauMicroserviceJettyBasics.md), [MicroserviceJettyOverview.md](../../juneau-docs/pages/topics/15.02.MicroserviceJettyOverview.md), [JettyXml.md](../../juneau-docs/pages/topics/15.07.JettyXml.md), [Extending.md](../../juneau-docs/pages/topics/15.09.Extending.md) — rewrite examples to the new `configurations(JettyConfiguration.class)` model. `15.09 Extending` shrinks dramatically since "extending the microservice" is no longer the path — replace with "Customizing via `@Bean`".
- **Release notes** [9.5.0.md](../../juneau-docs/pages/release-notes/9.5.0.md) — new "Breaking changes" entries under `juneau-microservice` and `juneau-microservice-jetty`.
- **Migration guide** (the v9.5 file tracked under TODO-17 at `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`) — Old → New rows:
  - `JettyMicroservice.create()...` → `Microservice.create().configurations(JettyConfiguration.class)...`
  - `extends JettyMicroservice` → `@Bean MicroserviceListener`
  - `JettyMicroserviceListener` → `MicroserviceListener` (or new `@Bean`)
  - `JettyMicroservice.getInstance().getServer()` → `Microservice.getInstance().getBeanStore().getBean(Server.class).orElseThrow()`
  - Maven `juneau-microservice-core` → `juneau-microservice`

### 7. Verification

Per `AGENTS.md`, after substantive edits run `./scripts/test.py` (full clean build + tests) to catch dependency, OSGi-bundle, and Surefire fallout from the rename and the deleted classes.

---

## Notes

- The `JettyMicroservice.getInstance()` singleton has a small handful of in-tree consumers (`DebugResource`, `SamplesMicroservice`); they all migrate to `Microservice.getInstance().getBeanStore().getBean(...)`. External users on the deprecated API will need the migration guide.
- `JettyServerComponent` lookups should consistently go through `Microservice.getInstance().getBeanStore().getBean(JettyServerComponent.class)` rather than via a new static singleton, to avoid re-introducing the `JettyMicroservice.getInstance()` smell.
- `BeanStore.getBeansOfType(Class)` is **exact-type** (see the note in [MicroserviceTestFixture.java](../juneau-utest/src/test/java/org/apache/juneau/microservice/MicroserviceTestFixture.java)). The fan-out iteration in step 2 must declare the lookup type as `MicroserviceListener.class` and expect `@Bean` methods to declare their return type accordingly (already the convention).
- Keep `JettyConfiguration` minimal — anything Jetty-specific that today lives on `JettyMicroservice.Builder` (programmatic `.servlet(...)`, `.servletAttribute(...)`) is intentionally **dropped** in favor of `@Bean Servlet` + the existing `Jetty/servletAttributes` config section. This is the "single configurable bean" the TODO is asking for.
