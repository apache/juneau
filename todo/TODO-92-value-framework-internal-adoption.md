# TODO-92 — `@Value` framework-internal adoption pass

Source: filed during TODO-79 Phase 6 discovery on 2026-05-25.

## Goal

Route Juneau's framework-internal config readers — places that today hand-roll `System.getProperty(...)` / `System.getenv(...)` / `env(...)` lookups inside static initializers, builder constructors, or non-injectable lifecycles — through a `BeanInstantiator`-resolved seam so they can carry `@Value("${...}")`. Picks up where TODO-79 Phase 6 deliberately stopped.

## Why this is a separate TODO

TODO-79's Phase 6 OQA was explicitly resolved as "stay user-facing": migrate only sites that are already on a `BeanInstantiator`-managed lifecycle. Framework-internal config readers need a non-trivial refactor (route through a bean-store seam first), which is out of scope for the `@Value` introduction PR. Splitting it out keeps both PRs reviewable.

## Inventory (carried over from TODO-79 Phase 6 discovery report)

Each entry lists the file and the specific lookup that should ultimately become `@Value`-driven. The bucket "Defer-Framework" means "framework-internal lifecycle, not directly injectable today".

### juneau-commons

- **`org.apache.juneau.commons.settings.DotenvPropertySource`** (lines 55-57) — reads `juneau.dotenv.path` from `System.getProperty(...)` then `System.getenv(...)` in a builder constructor. Currently bootstrap code; needs a seam to be made injectable.
- **`org.apache.juneau.commons.settings.ArgsPropertySource`** (lines 51, 56) — reads `sun.java.command` and `juneau.args` in a static helper. Bootstrap-level; defer.

### juneau-config

- **`org.apache.juneau.config.Config.getCandidateSystemDefaultConfigNames()`** (line 533) — reads `sun.java.command` to derive the system-default config file name. This is the bootstrap path for `Config.getSystemDefault()` itself; cannot be `@Value`-driven without breaking the cycle.

### juneau-marshall

- **`org.apache.juneau.parquet.ParquetParserSession`** (lines 61, 78) — `juneau.parquet.debug` and `java.io.tmpdir` reads in static field initializers. Class-load-time only; defer.

### juneau-rest-server

- **`org.apache.juneau.rest.logger.CallLogger.Builder`** (lines 134-139) — `juneau.restLogger.{logger,enabled,requestDetail,responseDetail,level}` lookups via `env(...)` in the no-arg builder constructor. Already goes through `Settings.get(...)` but isn't `@Value`-annotated because the builder is reflectively instantiated without a bean store. Route through `BeanInstantiator` and add `@Value` slots.
- **`org.apache.juneau.rest.convention.BasicVersionResource.Builder.fromJavaVersion()`** (line 334) — reads `java.version` via `System.getProperty(...)`. Inside a builder method called by user code; could become `@Value("${java.version:?}")` on a field if the builder were converted to `BeanInstantiator`-managed.
- **`org.apache.juneau.rest.RestContext.Builder`** defaults — the resource-scoped `Config` is now bridged to `Settings` by TODO-79, but `RestContext.Builder`'s own defaulting code (encoders, serializers, logging levels) still hand-rolls system-property reads. TODO-92 should sweep these.

### juneau-rest-server-jwt (FINISHED-69)

- **`org.apache.juneau.rest.auth.jwt.JwtTokenValidator.Builder.jwksCacheTtl`** (line 119) — defaults to `Duration.ofMinutes(5)` in the builder; ideally `@Value("${juneau.jwt.jwksCacheTtl:PT5M}")`. Requires routing the builder through `BeanInstantiator`.

### juneau-microservice (TODO-79 Phase 3 already partially addressed)

- **`org.apache.juneau.microservice.Microservice`** bootstrap — TODO-79 wires the microservice's `Config` into `Settings`, so `@Value` works for any bean *built by the microservice*. The microservice's own bootstrap fields (logging level, port, etc.) are still set via builder methods, not `@Value`. TODO-92 should route those through `BeanInstantiator`.

### juneau-bct

- **`org.apache.juneau.junit.bct.BctConfiguration.BCT_SORT_MAPS` / `BCT_SORT_COLLECTIONS`** (lines 111, 125) — already reads from system properties via `env(...)`; not currently injectable but could be after the BCT config layer is routed through `BeanInstantiator`.

## Approach (sketch)

1. Identify the "seam class" for each entry — the smallest unit that can be migrated from "reflectively instantiated, no bean store" to "constructed via `BeanInstantiator` with a bean store".
2. For each seam class, add a `BeanInstantiator.of(...).run()` constructor path (typically a static factory) and a `@Value`-annotated field/parameter.
3. Migrate existing call sites to the new factory.
4. Delete the hand-rolled `env(...)` / `System.getProperty(...)` reads, leave the `SP_xxx` string constants alone (they remain the canonical property names — `@Value` references them indirectly through the `${...}` expression).
5. Update tests.

## Sequencing

- Land after TODO-79.
- Each module's pass can ship independently; recommend `juneau-commons` first (smallest seam), then `juneau-rest-server` (largest payoff).

## Out of scope

- Anything in `Config.<clinit>` itself — the bootstrap path can't depend on `@Value` without cycles.
- Static `<clinit>`-time field initializers — those run before any bean store exists; they stay as `env(...)` reads.
