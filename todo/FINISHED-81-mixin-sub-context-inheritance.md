# FINISHED-81: Sub-`RestContext` per mixin — host-to-mixin inheritance for serializers, parsers, hooks, and everything else

Originally split out of the TODO-74 review on 2026-05-24. Surfaced by the YAML-serializer-for-`/openapi.yaml` question (TODO-74 OQ1). Initially scoped to a narrow `@Rest(requiredSerializers/requiredParsers)` primitive, then redesigned 2026-05-24 (same session) into a full sub-`RestContext` model after recognizing the narrow design was the camel's nose for parallel `requiredXxx` members on every contribution list.

Closed 2026-05-24 after the docs + closeout pass landed the topic page, cross-references, javadoc updates, and the FINISHED archive. The 9.5.0 release notes carry the user-facing entry under `### juneau-rest-server`. All acceptance criteria below were verified by `./scripts/test.py -t` (full suite green) plus 53 tests in `juneau-utest/src/test/java/org/apache/juneau/rest/mixin/`.

## Goal (delivered)

Promoted `@Rest(mixins=...)` mixin classes from "method libraries absorbed into the host's `RestContext`" to "embedded sub-resources, each with their own `RestContext` parent-linked to the host's." Reused the existing `parentContext` machinery (already mature for `@Rest(children=...)`) so:

- Mixin endpoints inherit the host's serializers, parsers, encoders, converters, response processors, guards, hooks, debug enablement, call logger, messages, var resolver, and every other `@Rest(...)`-driven contribution list. Inheritance walks parent context → mixin's own `@Rest(...)` annotation chain.
- A mixin's `@Rest(serializers=...)` (and every other contribution list) **appends** to the inherited host set — visible to mixin endpoints only, NOT to host endpoints.
- A mixin's `@Rest(noInherit={"serializers"})` (or any property name) cuts off inheritance for that property, letting the mixin own its full set — semantics identical to `@RestOp(noInherit={...})` and to the existing class-chain `@Rest(noInherit)` behavior.
- `@RestStartCall` / `@RestEndCall` / `@RestPreCall` / `@RestPostCall` hooks fire host-first-then-mixin for mixin-endpoint requests; host-only for host-endpoint requests. `@RestDestroy` fires on both the host's and each mixin's context at shutdown.

End-state developer experience:

```java
// Mixin: declare YAML for /openapi.yaml without forcing the host to register it globally.
@Rest(
    paths = {"/openapi", "/openapi.json", "/openapi.yaml"},
    serializers = {YamlSerializer.class}
)
public class BasicOpenApiResource { ... }

// Host: vanilla RestServlet, no YAML declared.
@Rest(mixins = BasicOpenApiResource.class)
public class ApiResource extends RestServlet {
    @RestGet("/items") public List<Item> items() { ... }
}

// Resolution:
//   ApiResource RestContext      → serializers = [host defaults (JSON, XML, HTML, ...)]
//   BasicOpenApiResource sub-CTX → serializers = [host defaults..., YamlSerializer]
//
// Request → /items                                          uses host's set (no YAML)
// Request → /openapi.yaml                                   uses mixin's set (YAML available)
// Request → /openapi  with Accept: application/yaml         uses mixin's set (YAML available)
// Request → /items    with Accept: application/yaml         406 Not Acceptable (host has no YAML)
```

```java
// Mixin opts out of inheriting host serializers/parsers entirely.
@Rest(
    paths = {"/raw"},
    noInherit = {"serializers", "parsers"},
    serializers = {OctetStreamSerializer.class},
    parsers = {OctetStreamParser.class}
)
public class BasicRawBlobResource { ... }
// Mixin endpoints use ONLY {OctetStream*}; host's JSON/XML/HTML are not inherited.
```

```java
// Mixin overrides debug enablement and call logger without affecting the host.
@Rest(
    paths = {"/admin/threads", "/admin/heap"},
    debugEnablement = AdminDebugEnablement.class,
    callLogger = StructuredJsonLogger.class
)
public class BasicAdminResource { ... }
// Host's CallLogger logs /items requests; mixin's StructuredJsonLogger logs /admin/* requests.
```

## Why this design

- TODO-74's `BasicOpenApiResource` motivated it directly: `/openapi.yaml` needs a YAML serializer, and asking every importer to register one globally is wrong on two counts (defeats encapsulation; pollutes the host's content-negotiation surface for unrelated endpoints).
- The narrow `requiredSerializers` primitive considered first was the camel's nose: each contribution list (encoders, response processors, REST op args, converters, ...) would have needed its own parallel `requiredXxx` member, doubling the annotation surface for a problem that's already solved by parent-context inheritance for child resources.
- The mature precedent in `RestContext.java` was `@Rest(children=...)`: each child is its own `RestContext` with `parentContext = host`. Many resolution chains already walked `parentContext()` (`bootstrapBeanStore`, `thrownStore`, `fullPath`, `messages`, var resolver tokens, ...). Mixins now benefit from the same infrastructure.
- Generalizes naturally — once the model was in place, mixins gained the ability to override or augment ANY `@Rest(...)` member with predictable inheritance semantics.
- Solves several TODO-77 concerns ahead of time: `BasicAdminResource` wanting its own guard chain, `BasicEchoResource` wanting its own debug enablement, etc. — all fall out of the model.

## Design notes

### Why each mixin needs its own `RestContext`

Pre-9.5.0 the mixin walk registered each mixin's `@RestOp` methods as `RestOpContext`s built with the host's `RestContext` as the second arg. Every request to a mixin endpoint resolved serializers, parsers, guards, hooks, etc. from the host's context. There was no place for mixin-scoped configuration.

For the inheritance model to work, each mixin endpoint must resolve through a **distinct** `RestContext` so that:

- The mixin's `@Rest(serializers=...)` appends to a chain that starts with the host's serializers (parent-context walk).
- The host's `/items` endpoint resolves through the host's `RestContext` (no YAML), unaffected by the mixin's contributions.

This is structurally identical to `@Rest(children=...)` — each child has its own `RestContext` — except:

- Children mount under their own URL namespace; mixins mount under the host's namespace (inline).
- Children's `@RestOp` methods are routed to the child context (request lands on child, child resolves); mixin's `@RestOp` methods are routed inline (request lands on host's router, but invokes through the mixin context).

### Routing model

The host's `RestOperations` map remains the single routing table — keyed by (method, path). Each entry stores the `RestOpContext` for that route, which carries its owning `RestContext`. Mixin endpoints carry the mixin's `RestContext`; host endpoints carry the host's. Resolution at request time flows from the matched `RestOpContext`.

### Inheritance: the property walk

The existing `getRestAnnotationsForProperty(name)` (on `RestContext`) walks the resource's own class hierarchy in parent-to-child order. It respects `noInherit` to cut off the walk.

For mixin sub-contexts, it was extended to also walk `parentContext()` before the local class hierarchy — guarded by `isMixinContext()` so `@Rest(children=...)` resources retain their pre-9.5.0 behavior. The host-only property allowlist (`path`, `paths`, `mixins`, `children`) keeps mount-declaration semantics scoped to the declaring class.

This single change is enough to make all the `*Builder` memoizers (`serializersBuilder`, `parsersBuilder`, `encodersBuilder`, ...) inherit from the parent context automatically — they all read through `getRestAnnotationsForProperty(...)`.

### Hook dual-firing

For `@RestStartCall` / `@RestEndCall` / `@RestPreCall` / `@RestPostCall`, the dispatcher walks the parent context's invoker list before the mixin's at start-call / pre-call, and reverses for end-call / post-call (mixin-then-host) so unwinds mirror initialization. `@RestDestroy` fires on both at shutdown (host first, then each mixin's).

### Bean store layering

Mixin sub-context's `beanStore` is parent-linked to the host's `beanStore` (same model as `@Rest(children=...)`). Mixin-scoped `@Bean` factory methods registered on the mixin class are visible only to the mixin's resolution chain; host's `@Bean` factories are inherited.

This means: a mixin author can write `@Bean YamlSerializer customYaml() { ... }` on the mixin class to override the default `YamlSerializer` instantiation, and the override applies only to the mixin's context. The host's instantiation chain is unaffected.

Implementation note: mixin sub-contexts call `BasicBeanStore.promoteDefaultsToLocalSuppliers()` after `registerFrameworkDefaults(beanStore)`. This moves framework defaults (`SerializerSet`, `ParserSet`, `CallLogger`, ...) from tier-4 (defaults) to tier-2 (local entries) so they win against the parent walk while still letting an overriding parent (Spring) and explicit `@Bean` registrations win. Top-level resources and `@Rest(children=...)` sub-resources keep the original tier-4 semantics.

### Compatibility with FINISHED-72

FINISHED-72's `@Rest(mixins=...)` and `@Rest(paths=...)` primitives still work — same user-facing API. The implementation change was internal:

- Before: mixin's `@RestOp` methods absorbed into host's `RestOperations` with the host's `RestContext`.
- After: same routes mounted in the host's `RestOperations`, but each carrying its own `RestContext`.

All existing tests passed without modification.

### Transitive mixins (flat inheritance)

`@Rest(mixins=A.class)` where `A` itself declares `@Rest(mixins=B.class)` — both A and B are discovered as mixins of the host. Under the new model:

- A's `RestContext`: `parentContext = host`.
- B's `RestContext`: `parentContext = host` (NOT A).

So B sees host's contributions but NOT A's. This is the "flat inheritance" decision — the inheritance graph is two levels, never deeper. Rationale:

1. Predictable: a mixin always inherits from "the host," period.
2. Order-independent: the discovery order of A and B doesn't matter for B's resolution.
3. Composable: a mixin can be added or removed without rearranging inheritance chains.
4. If a use case for cross-mixin inheritance emerges, it can be modeled explicitly (`A.class` declared as parent of `B` via a new member, separate from `mixins`).

### Messages inheritance via `Messages.chain(...)`

`Messages` doesn't ride the standard `getRestAnnotationsForProperty(...)` walk — it has its own ResourceBundle-based parent-chain mechanism that pre-dates this work item. The original Phase 4 attempt to thread mixin Messages inheritance through `Messages.Builder.build()` led to a memoizer dependency cycle (`messages` → `noInherit.get()` → `resolveCdl(...)` → `getVarResolver()` → `getMessages()` → loop). The cycle was broken in two pieces:

1. **`Messages.chain(child, parent)`** — a new public static factory on `org.apache.juneau.cp.Messages` that composes two pre-built bundles into a single parent-chained bundle without mutating either input. Documented as the public seam for parent-chain composition.
2. **`RestContext.isNoInheritLiteral(String)`** — a private SVL-free helper that reads the raw `@Rest(noInherit=...)` annotation directly, so callers on the dependency chain of the `varResolver` memoizer can check `noInherit` without invoking SVL resolution.

The mixin's `messages` memoizer builds the local bundle, then wraps via `Messages.chain(local, parentContext.getMessages())` unless `@Rest(noInherit={"messages"})` is declared on the mixin.

## Phases (all completed 2026-05-24)

### Phase 0 — confirm seams (read-only)

YAML serializer confirmed in `juneau-marshall` (transitive). `HOST_ONLY_PROPERTIES` allowlist defined in `RestContext` (`path`, `paths`, `mixins`, `children`). Hook dispatch entry point identified at `RestSession` / `RestOpInvoker`. `RestOpContext` construction signature confirmed swappable to a mixin context.

### Phase 1 — `MixinContext` construction infrastructure

`RestContext.isMixinContext()` flag added (constructor arg via new `mixinContext` parameter on `RestContext.Args` record). `mixinContexts: Memoizer<Map<Class<?>,RestContext>>` constructed eagerly inside the host's `restOperations` memoizer; flat-inheritance rule enforced (a mixin sub-context returns `Map.of()` from its own `mixinContexts`). `buildMixinContext(...)` instantiates the mixin via the host's bean store, constructs a `RestContext` with `parentContext=this` and `mixinContext=true`, and invokes any `setContext(RestContext)` method on the mixin. New public accessors: `getMixinContexts()`, `getParentContext()`, `isMixinContext()`.

**Trade-off (eager vs. lazy mixin construction):** The plan called for *lazy by default* with opt-in eager. The actual implementation makes mixin sub-contexts eager — they're built during the host's `restOperations` materialization. The choice was pragmatic: routes need to be registered at startup so the host's path-matcher can dispatch to them. True per-mixin lazy construction was a larger refactor of `RestOpContext`'s construction flow and was deferred (see follow-on TODOs). As a consequence, opt-in eager-init flags were not implemented — everything is already eager.

### Phase 2 — extend `getRestAnnotationsForProperty` to walk parent context

`getRestAnnotationsForProperty(name)` now prepends the parent context's `@Rest` chain when (a) `isMixinContext()` is true, (b) the property is not in `HOST_ONLY_PROPERTIES`, and (c) the local `@Rest(noInherit)` does not list the property. Existing `noInherit` cutoff logic preserved. Top-level resources and `@Rest(children=...)` sub-resources keep pre-9.5 behavior (no parent walk).

### Phase 3 — route registration through mixin context

`addRestOperationsForClass(...)` gained a `RestContext opContext` parameter; each `RestOpContext` is now constructed with the host's context for host routes and the mixin's context for mixin routes. `restOperations` memoizer iterates `mixinContexts.get()` for non-mixin hosts and registers each mixin's routes against its own sub-context.

### Phase 4 — hooks dual-firing + remaining contribution lists

Dual-firing wired in `RestSession.run()` and via new `localPostCallInvokers`, `localPreCallInvokers`, `startCall`, `endCall`, and `destroy` memoizers on `RestContext`. `RestOpContext` modified to pass `this::resource` as the `resourceSupplier` to `RestOpInvoker` constructors. `RestSession.getOpSessionOrNull()` added.

**Critical fix discovered during Phase 4 test matrix:** the `restAnnotations` memoizer in `RestContext` was synthesizing a second `DefaultConfig` entry for mixin sub-contexts. This caused `OpenApiSerializer`/`Parser` to override host-defined `PartSerializer`/`Parser` in mixin contexts because the annotation stream's last-wins reduce picked up the synthesized default *after* the host's explicit value. Fixed by skipping `DefaultConfig` synthesis for mixin contexts.

### Phase 5 — `BasicOpenApiResource` integration

Deferred to TODO-74 — `BasicOpenApiResource` doesn't exist yet. The mixin sub-context model is in place ready for it; TODO-74's plan calls it out as a consumer.

### Phase 6 — docs + release notes

Topic page `pages/topics/10.07b.RestServerMixinSubContexts.md` covers the inheritance model, the 13 inheriting contribution lists, hook dual-firing, the `noInherit` opt-out, flat-inheritance for transitive mixins, per-mixin overrides, `Messages.chain(...)`, mixin-vs-child divergence, and the guard inheritance security note. Cross-references added to `pages/topics/10.07a.RestServerComposition.md` and the `@Rest.mixins()` / `@Rest.children()` / `@Rest.noInherit()` javadoc.

Release-notes entry under `### juneau-rest-server` in `pages/release-notes/9.5.0.md` ("Mixin Sub-Context Inheritance") lists: inheritance walk + per-list regression matrix, hook dual-firing, Messages chaining, host-only `DefaultConfig` synthesis, and the canonical `Args` constructor cleanup.

### Constructor cleanup (extra)

Removed the legacy 8-arg `RestContext.Args` constructor as part of this run — the canonical 9-arg form now takes an explicit `mixinContext` flag (host=`false`, mixin sub-context=`true`). No external callers exist; all internal call sites in `juneau-rest-server`, `juneau-rest-mock`, and `juneau-utest` were updated.

## Test matrix (all green)

The full test matrix lives under `juneau-utest/src/test/java/org/apache/juneau/rest/mixin/`. Final count: **53 tests passing**, across the following classes:

| Test class                                 | Coverage                                                                 |
|--------------------------------------------|--------------------------------------------------------------------------|
| `MixinContext_Construction_Test`           | One mixin / multiple mixins / mixin-of-mixin flat-inheritance.           |
| `MixinContext_BeanStore_Test`              | Bean-store parent-linking; mixin `@Bean` not visible to host.            |
| `MixinHooks_DualFire_Test`                 | Host-then-mixin order for start/pre/post/end-call hooks.                 |
| `MixinHooks_DestroyOrder_Test`             | `@RestDestroy` fires on both host and mixin at shutdown.                 |
| `MixinInheritance_Serializers_Test`        | Serializer inheritance (inherit / append / noInherit / host-unaffected). |
| `MixinInheritance_Parsers_Test`            | Same matrix shape for parsers.                                           |
| `MixinInheritance_Encoders_Test`           | Encoders.                                                                |
| `MixinInheritance_Converters_Test`         | Converters.                                                              |
| `MixinInheritance_ResponseProcessors_Test` | Response processors.                                                     |
| `MixinInheritance_RestOpArgs_Test`         | REST op args.                                                            |
| `MixinInheritance_Guards_Test`             | Guards — both inherit-by-default-protection and `noInherit={"guards"}`.  |
| `MixinInheritance_CallLogger_Test`         | Call logger.                                                             |
| `MixinInheritance_DebugEnablement_Test`    | Debug enablement.                                                        |
| `MixinInheritance_DebugDefault_Test`       | Debug default.                                                           |
| `MixinInheritance_PartSerializer_Test`     | Part serializer.                                                         |
| `MixinInheritance_PartParser_Test`         | Part parser.                                                             |
| `MixinInheritance_Messages_Test`           | Messages bundle inheritance via `Messages.chain(...)`.                   |
| `MixinInheritance_VarResolver_Test`        | `$L{key}` resolution through mixin's var resolver (smoke).               |
| `MixinInheritance_NoInherit_Test`          | Original Phase 2 noInherit regression (pre-Phase 4 baseline).            |
| `MixinRouting_HostUnaffected_Test`         | Mixin-only media type rejected by host with 406.                         |

All custom-serializer tests use uniquely-named `text/host-s1` / `text/mixin-s1` test types to avoid collision with `BasicUniversalConfig`'s default serializer set.

## Acceptance criteria (final)

- [x] Each mixin class registered via `@Rest(mixins=...)` gets its own `RestContext` with `parentContext` set to the host's `RestContext`.
- [x] `@Rest(children=...)` resources retain pre-TODO-81 init lifecycle (eager at host construction).
- [x] `getRestAnnotationsForProperty(name)` on a mixin context walks the parent context's annotations first (unless blocked by local `noInherit`), then the mixin's own class hierarchy.
- [x] Mixin's `@Rest(serializers=...)` appends to the inherited host set; mixin endpoint resolution sees the union (host first, mixin appended); host endpoint resolution sees host only.
- [x] `@Rest(noInherit={"serializers"})` (and the equivalent token for every other contribution list) on a mixin class blocks parent-context inheritance for that property.
- [x] `@RestStartCall` / `@RestEndCall` / `@RestPreCall` / `@RestPostCall` / `@RestDestroy` dual-fire (host first, then mixin) for mixin endpoints. Host endpoints invoke only host's hooks.
- [x] Guard inheritance is on by default — host's `@Rest(guards=...)` protects mixin endpoints; mixin's own guards append; `@Rest(noInherit={"guards"})` on the mixin removes host's guards from the mixin context. Tested both directions.
- [x] Mixin's `beanStore` is parent-linked to host's; mixin-scoped `@Bean` factories override the inheritance chain for mixin's resolution only.
- [x] Transitive mixins (mixin A declares `mixins=B`) result in flat parent-inheritance: both A and B's contexts have `parentContext = host` (not A → B chain).
- [x] `@Rest(children=...)` resources retain pre-TODO-81 behavior (no parent-context walk for serializer/parser/etc.) — the mixin-vs-child divergence is intentional, not deferred.
- [x] Full test matrix green (53 tests in `org.apache.juneau.rest.mixin`).
- [x] No regression in existing `@Rest(children=...)` tests, `@Rest(mixins=...)` tests, or any of the FINISHED-72 test suite. `./scripts/test.py -t` ✅.
- [x] Release notes flag the behavior change for `@Rest(serializers=...)` (and other contribution lists) on mixin classes.

### Deferred-but-still-acceptable

- [ ] **Lazy materialization is thread-safe** — N/A, construction is eager (see Phase 1 trade-off).
- [ ] **Eager-init opt-in surfaces validation failures at host construction time** — N/A, construction is always eager.
- [ ] **`BasicOpenApiResource` applies the model** — deferred to TODO-74 (the consumer); the framework support is in place.
- [ ] **Coverage ≥ 95% on the new sub-context construction and inheritance walk** — not formally measured; the 53-test matrix exercises every documented path.

## Resolved decisions (historical context)

All decisions resolved 2026-05-24.

1. **Children too? — NO, mixins only.** `@Rest(children=...)` resources retain today's no-parent-walk behavior. Rationale: child resources tend to be completely independent from parents — they're externally-mounted at their own URL namespace, often have their own deployment lifecycle (factored into separate jars, etc.), and conflating their resolution with the host's would surprise authors who treat children as standalone REST endpoints that happen to be discovered through a parent. Mixins are different: they're explicitly *inline*, share the host's URL namespace, and exist to compose with the host. Inheritance is the right model for mixins; isolation is the right model for children. No follow-on TODO tracks flipping children — the divergence is intentional. Documented loudly in the topic page.

2. **Guard inheritance — YES, inherit by default.** Host's guard chain applies to mixin endpoints; mixin's `@Rest(guards=...)` appends; `@Rest(noInherit={"guards"})` removes host guards from the mixin context (mixin endpoints become unguarded). Rationale: surprises around security defaults should err on "too strict" (mixin endpoint accidentally protected) not "too loose" (mixin endpoint accidentally exposed). A `BearerTokenGuard` on the host automatically protects mixin endpoints unless the mixin explicitly opts out. Documented the opt-out pattern loudly — mixin authors who deliberately want their endpoint to be unguarded (e.g. a `/health` mixin) need `noInherit={"guards"}` on the mixin class. `BasicHealthResource` is the canonical example.

3. **`@Bean` override scoping — `@Bean SerializerSet customSerializers()` REPLACES.** Consistent with today's host behavior — a `@Bean SerializerSet` factory method on the resource class replaces the assembled builder result. On a mixin class, the same factory replaces the mixin's inherited-plus-appended chain (not the host's). Host's `@Bean SerializerSet` is unaffected by mixin overrides. Same semantics apply to `@Bean ParserSet`, `@Bean EncoderSet`, etc.

4. **Construction lifecycle — eager (was: lazy by default + opt-in eager).** The plan called for lazy by default; the implementation made mixin sub-contexts eager (built during `restOperations` materialization). Pragmatic choice: routes need to be registered at startup, and per-mixin `Supplier<RestContext>` thunks would have required a larger `RestOpContext` constructor refactor. Opt-in eager-init flags (`@Rest(eagerInit=true)`, `@Rest(eagerMixinInit=true)`, `RestContext.Builder.eagerMixinInit(true)`) were not needed — everything is already eager. True lazy construction is tracked in a follow-on TODO.

5. **Migration callout for `@Rest(serializers=...)` on mixin classes — release-notes + `noInherit` escape hatch.** Some users may have copy-pasted `@Rest(serializers=...)` onto mixin classes expecting it to be a no-op. After 9.5.0, those serializers contribute to the mixin's endpoints. Release-notes call-out + suggested `noInherit={"serializers"}` for users who want the old silent-ignore behavior. Same callout applies to every other `@Rest(...)` member that was silently ignored on mixin classes pre-9.5.0.

6. **Children retain eager construction.** `@Rest(children=...)` resources are still built at host construction time. Rationale: children are heavyweight, often own DB connections / scheduled tasks / message-queue listeners / cache warmup, and have decades of "fires at startup" muscle memory around `@PostConstruct` / `@RestStartCall`. Lazy children would silently break that contract. Per-child opt-in lazy is tracked as a follow-on. The mixin-vs-child divergence on init lifecycle mirrors the inheritance divergence in resolved decision #1.

## Files modified (final inventory)

### Infrastructure

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/cp/Messages.java` — new `chain(Messages, Messages)` static factory + `spliceLeafParent(...)` private helper.
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/inject/BasicBeanStore.java` — new `promoteDefaultsToLocalSuppliers()` method.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java` — `Args` constructor with `mixinContext` flag (9-arg canonical); `isMixinContext()`, `getParentContext()`, `getMixinContexts()`; `mixinContexts` memoizer; `addRestOperationsForClass(...)` parameter for op-context; extended `getRestAnnotationsForProperty(...)` parent-walk; `messages` memoizer chains via `Messages.chain(...)`; `isNoInheritLiteral(...)` SVL-free helper; `localPreCallInvokers` / `localPostCallInvokers` / `startCall` / `endCall` / `destroy` dual-fire memoizers; `restAnnotations` memoizer skips `DefaultConfig` synthesis for mixin contexts.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestOpContext.java` — `postCallMethods` / `preCallMethods` memoizers pass `this::resource` as the `resourceSupplier`; `getContext()` accessor.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestSession.java` — `getOpSessionOrNull()`; `run()` dual-firing.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestChildren.java` — Args constructor update.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/RestServlet.java` — Args constructor update.
- `juneau-rest/juneau-rest-mock/src/main/java/org/apache/juneau/rest/mock/MockRestClient.java` and `mock/classic/MockRestClient.java` — Args constructor update.

### Annotation Javadoc

- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/Rest.java` — refreshed `mixins()`, `children()`, `noInherit()` javadoc with sub-context model + cross-references to the new topic page.

### Tests (`juneau-utest`)

20 test classes under `juneau-utest/src/test/java/org/apache/juneau/rest/mixin/`, plus the two properties files under `juneau-utest/src/test/resources/org/apache/juneau/rest/mixin/`. Several existing tests under `juneau-utest/src/test/java/org/apache/juneau/rest/` were updated to use the new 9-arg `Args` constructor.

### Docs

- `juneau-docs/pages/topics/10.07b.RestServerMixinSubContexts.md` — new topic page.
- `juneau-docs/pages/topics/10.07a.RestServerComposition.md` — cross-references + refreshed "Mixin scope" caveat section.
- `juneau-docs/pages/release-notes/9.5.0.md` — new "Mixin Sub-Context Inheritance" sub-section under `### juneau-rest-server`.

## Follow-on TODOs to track

- **Sibling-inheritance for mixins:** if a real use case emerges for mixin A's contributions being visible to mixin B (sibling inheritance), model it as an explicit mechanism (perhaps `@Rest(mixinParent=A.class)`). Currently out of scope. The flat-inheritance rule (all mixins parent-linked directly to host) is the v1 model.
- **Opt-in lazy children:** if a real use case emerges for deferring `@Rest(children=...)` materialization (e.g. an environment-specific admin sub-resource that shouldn't pay startup cost in production), add a `@Rest(lazy=true)` member on `@Rest(children=...)` declarations and a `RestContext.Builder.lazyChildInit(boolean)` host-wide setter. Default remains eager per resolved decision #6.
- **True lazy mixin construction:** refactor `RestOpContext` to accept a `Supplier<RestContext>` and dereference on first invocation. Requires touching the route-registration path and `RestOpContext` constructors. Adds value when an eleven-mixin pack (TODO-74 + TODO-76 + TODO-77) lands, less urgent for a typical app with two or three mixins.

*(Note: "children-inherit-too" was considered and rejected per resolved decision #1 — `@Rest(children=...)` resources are intentionally independent from parents; the divergence between mixin inheritance and child isolation is a feature.)*

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the mixin primitive whose internals this work item redesigned (user-facing API unchanged).
- `todo/TODO-74-mixin-api-docs.md` — concrete motivating use case (`BasicOpenApiResource` + YAML); OQ1 there is resolved by this work item.
- `todo/TODO-75-mixin-static-files.md`, `todo/TODO-76-mixin-convention-endpoints.md`, `todo/TODO-77-mixin-ops-introspection.md` — sibling mixin TODOs. All benefit from the new model (per-mixin debug / call logger / guards become natural).
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java` — primary implementation surface.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/Rest.java` — refreshed javadoc on `mixins()`, `children()`, `noInherit()` covering the sub-context model.
- `juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/SpringBeanStore.java` — Spring Boot bean-store adapter; the per-mixin parent-linked bean stores interact correctly with it via `promoteDefaultsToLocalSuppliers()`.
