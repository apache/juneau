# FINISHED-17: 9.2.x → 9.5.0 Migration Guide Audit

## Scope

Audited every breaking change between Apache Juneau 9.1.0 (Jakarta EE jump) and 9.5.0
to ensure the v9.5 Migration Guide
(`juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`) contains an actionable
Old → New row for each end-user-visible change. Coverage axes: removed APIs, renamed
public types/members, changed default behaviors, annotation-attribute semantics
changes.

## Sources read

- `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` — existing baseline.
- `juneau-docs/pages/release-notes/9.1.0.md` — start point (Jakarta EE migration).
- `juneau-docs/pages/release-notes/9.2.0.md` — covered multi-key cache rename,
  `juneau-all` → `juneau-shaded-all`, `@Schema` Draft 2020-12.
- `juneau-docs/pages/release-notes/9.5.0.md` — the bulk of the audit (read in
  chunks; ~200K characters).
- `git log juneau-9.1.0..HEAD --no-merges --first-parent --pretty=format:'%h %s'`
  in `/Users/james.bognar/git/apache/juneau`, filtered for
  `BREAKING|remove|delete|rename|move|feat` subjects.
- Targeted reads of `todo/FINISHED-*.md` archives for context:
  TODO-4, TODO-10, TODO-13, TODO-21, TODO-22, TODO-26, TODO-29, TODO-34, TODO-49,
  TODO-50, TODO-51, TODO-52, TODO-54, TODO-57.

No 9.2.x sibling release-notes files exist (the next file after `9.2.0.md` is
`9.5.0.md`), so the audit window is effectively `9.1.0 → 9.2.0 → 9.5.0`.

## Existing-coverage baseline (before this run)

The guide already documented (15 H2 sections):

- REST builder + injection removals (initial unlabeled table).
- SVL + runtime input types moved to `juneau-commons` (TODO-14).
- `MarshalledMap` / `MarshalledList` + `Json5Map` / `Json5List` (TODO-34).
- Bean → Marshalled annotation/engine renames.
- Bean-modeling layer split (package + type/method renames).
- JSR-330 alignment + Spring-lite (TODO-24).
- Microservice + Jetty refactor (TODO-36).
- REST client + HTTP stack promotion (TODO-38).
- `juneau-rest-common` module split (TODO-42).
- `juneau-rest-server` decoupled from HC 4.5 (TODO-40).
- Format-control round-trip hardening (TODO-57): `TemporalAccessor` widening,
  Parquet/RdfThrift/RdfProto byte[] wire-shape change, `Float` → `Double`.

## New rows added (16 new H2 sections + 2 H3 sub-sections inside the existing
   Bean → Marshalled section; 74 new Old → New rows total)

### Format-control program (4 sections — fills gaps the existing
TODO-57 section did not cover)

- `## Format-Control: Duration / Period Defaults (TODO-4)` — 3 rows.
  Default `Duration` flips to `ISO_8601_WITH_DAYS`; new `DurationFormat` /
  `PeriodFormat` enums; binary serializers can emit native numeric wire types.
- `## Format-Control: Calendar / Date / Temporal / TimeZone / Locale Swap
  Deletions (TODO-51)` — 6 rows. Removes the `Temporal*Swap` family +
  `LocaleSwap` / `TimeZoneSwap` / `ZoneIdSwap` direct usage; introduces
  `CalendarFormat`, `DateFormat`, `TemporalFormat`, `TimeZoneFormat`,
  `LocaleFormat`.
- `## Format-Control: Binary + Enum Swap and Builder Deletions (TODO-52)` — 7
  rows. Removes `ByteArraySwap`, the `OutputStreamSerializer.Builder` /
  `InputStreamParser.Builder` `binaryFormat(...)` setters, and
  `useEnumNames(boolean)`. Renames `csv.ByteArrayFormat` to
  `CsvByteArrayCellFormat`.
- `## Format-Control: ClassSwap Deletion + Boolean / Float / Currency / Class
  Settings (TODO-54)` — 3 rows. `ClassSwap` deleted; new `BooleanFormat`,
  `FloatFormat`, `CurrencyFormat`, `ClassFormat` enums and setters; default
  `Currency` round-trip support.

### Marshalling defaults / parser behavior

- `## JSON Strict-Mode Separation` — 6 rows. `JsonParser` / `JsonSerializer`
  enforce strict RFC 8259; JSON5 input/output requires `Json5Parser` /
  `Json5Serializer`; `JsonSerializer.json5()` builder method removed;
  `JsonParser.Strict` removed.
- `## Sorted Bean Properties by Default` — 7 rows. `sortProperties()` API gone
  across the builder hierarchy (~55 overrides); `@Bean(sort=true)` →
  `@Bean(unsorted=true)` with inverted semantics; default is now sorted.
- `## Native Iterator / Iterable / Stream / Enumeration Serialization` — 5 rows.
  `IteratorSwap` / `EnumerationSwap` removed; lazy native serialization;
  `Supplier<T>` transparently unwrapped; new `ClassMeta` predicates and
  `Category` enum values.

### REST / HTTP layer

- `## HTTP Annotation Moves and Attribute Removals` — 6 rows. HTTP parameter
  annotations moved from `juneau-marshall` into `juneau-rest-common` (package
  unchanged); `serializer()` / `parser()` attributes replaced by
  `@HttpPartMarshalling`; `@Repeatable` / `on()` / `onClass()` /
  `@ContextApply` gone; 10 `XAnnotation` companions deleted; NG snapshot
  annotations deleted.
- `## Request Attributes vs Session Properties Separation` — 3 rows.
  `defaultRequestAttributes` no longer auto-merge into session properties; new
  `RestRequest.setSerializerSessionProperty(...)` / `setParserSessionProperty(...)`
  API.
- `## REST Session-Option Allowlist Refactor` — 8 rows. `@NoInherit` removed in
  favor of `noInherit` attribute on `@Rest` / `@RestOp` siblings; legacy
  programmatic builder allowlist setters gone; helper-class renames
  (`RestSessionOptionWire` → `RestSharedConstants`).

### Bean-store / DI consolidation

- `## Legacy cp Bean-Store Classes Removed (TODO-26)` — 9 rows. `BasicBeanStore`,
  `BeanCreator`, `BeanBuilder`, `BeanCreateMethodFinder` removed from
  `org.apache.juneau.cp`; canonical replacements live in
  `org.apache.juneau.commons.inject` (`BeanInstantiator` is the new name for
  `BeanCreator`). `CreatableBeanStore` removed.
- `## Bean-Store Convenience Renames` — 3 rows. `BasicBeanStore2` /
  `SpringBeanStore2` promoted to canonical names; `WritableBeanStore` narrowed
  to `BeanStore` on several builders (source-compatible).

### Misc renames + cleanups

- `## Swagger v2: ParamInfo → ParameterInfo` — 1 row.
- `## Multi-Key Cache and ConcurrentHashMapXKey Refactor (9.2.0)` — 6 rows.
  `Concurrent[2-5]KeyHashMap` → `ConcurrentHashMap[2-5]Key`; constructors no
  longer carry caching state; new `CacheX` family for caching; null keys throw.
- `## Module Rename: juneau-all → juneau-shaded-all (9.2.0)` — 1 row.
- `## Miscellaneous Utility Removals` — 5 rows. `TupleXFunction` → `FunctionX`;
  `Console.format` → `Utils.f`; `BasicRuntimeException` retired;
  `ArrayUtils` deprecated in favor of `CollectionUtils`; `ResettableSupplier`
  → `OptionalSupplier`.
- `## BCT: AssertionArgs Removed` — 1 row. Replaced by thread-local converter
  + leading `Supplier<String>` message parameter on assertion methods.

### Bean → Marshalled section additions (2 new H3 sub-sections inside
existing H2)

- `### Removed @MarshalledProp(properties) Attribute` — 3 rows. Plus removed
  internal `BeanMetaFiltered`, `BeanPropertyMeta.getProperties()`,
  `applyChildPropertiesFilter(...)`.
- `### @Beanp("*") on Non-Map Fields` — 1 row. `*` is now a no-op on non-Map
  fields and the namer-derived field name wins.

## Intentionally skipped

- **TODO-13 (system-property → `Settings` conversion)**: internal refactor;
  consumer-visible property names + behavior preserved.
- **TODO-22 (SVL DotenvVar / EnvFileVar additions)**: additive only.
- **TODO-49 (SchemaUtils empty-collection returns)**: per the FINISHED archive,
  caller analysis confirms no external code observes the change.
- **TODO-9 (MarkdownParserSession map-branch outer pass-through)**: behavior-
  preserving internal refactor.
- **TODO-56 (dispatch-chain reorder)**: behavior-preserving internal refactor.
- **TODO-10 (HTTP annotations moved to rest-common)**: only the *Maven
  coordinate* shifted — package names are unchanged. Covered briefly in the
  new "HTTP Annotation Moves and Attribute Removals" section as a one-line
  note (no separate row per annotation since import statements do not need
  updating).
- 9.5.x feature additions that are purely additive (Hjson / TOML / JSONL / SSE
  / BSON / CBOR / Protobuf Text / JCS marshallers, dynamic child REST
  resources, YAML format strategy, AI-friendly summary field, schema-
  validation mode, dotenv / env-file SVL variables, etc.): no migration step
  needed.

## TODO file state

- `[TODO-17]` removed from `/Users/james.bognar/git/apache/juneau/todo/TODO.md`.
- Archive file created: this file
  (`todo/FINISHED-17-9.2.x-9.5.0-migration-guide-audit.md`) — captures the
  audit narrative since it was a multi-day cross-repo sweep and the
  per-section breakdown is useful institutional memory for the 9.6 audit.

## Working tree state

- `juneau` repo: `M todo/TODO.md`, `?? todo/FINISHED-17-9.2.x-9.5.0-migration-guide-audit.md`.
  **No Java files modified.**
- `juneau-docs` repo: `M pages/topics/23.01.V9.5-migration-guide.md` only.
  +219 / −2 lines, 74 new Old → New rows across 16 new H2 sections and 2 new
  H3 sub-sections.

Neither repo has been committed or pushed.

## Anything unexpected

- **No 9.2.x point-release notes exist** — the next file after `9.2.0.md` is
  `9.5.0.md`. So "9.2.x changes" effectively means "everything between 9.2.0
  and 9.5.0 GA," all rolled into the `9.5.0.md` release notes. The audit
  scope was widened accordingly.
- **All breaking changes I identified are already documented in the release
  notes** — the migration guide was simply missing the actionable Old → New
  rows. No new release-notes entries were needed.
- The existing migration-guide rows for the format-control program (TODO-57)
  cover only the *round-trip hardening* fixes (`TemporalAccessor` widening,
  Parquet / RDF byte[] wire-shape change, `Float` → `Double`). The four
  upstream format-control TODOs (TODO-4 / 51 / 52 / 54) that introduced the
  new enums and deleted the swap families were missing from the guide entirely
  — they account for ~20 of the 74 new rows.
- The `JsonParser.Strict` removal + JSON5 separation is a *hard runtime
  break* for any caller passing single-quoted / unquoted-key JSON to
  `JsonParser.DEFAULT` — flagged prominently in the new "JSON Strict-Mode
  Separation" section.
