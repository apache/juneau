# Code conventions

These conventions derive from Apache Juneau's `juneau-code-conventions` skill
(in `apache/juneau` at `.claude/skills/juneau-code-conventions/SKILL.md`), adapted for this
**standalone Spring Boot + Apache Juneau application**. They are the checklist for a "READY-0
review" of this repo: a future session can run the review against this file without re-deriving
the rules from the upstream skill.

This project depends only on Apache Juneau's **public API** (Juneau `10.0.0-SNAPSHOT`) plus Spring
Boot; it is **not** part of `apache/juneau`. Some upstream conventions rely on Juneau's *internal*
test/build machinery that isn't published as a consumable dependency, so those are marked **N/A**
with a rationale below.

Each item: a one-line rule, then **Applied** or **N/A** (with rationale).

## Java source

- **Imports over FQCN** — Reference types by simple name + `import`; no inline fully-qualified class
  names (fields, params, returns, locals, generics, casts, `instanceof`, annotation values, Javadoc
  `{@link}`). **Applied.** Swept all `java.*`, `javax.*`, `org.xml.*`, and `org.apache.juneau.*` inline
  FQCNs. Verify per file with `rg '\bjava\.[a-z][a-zA-Z]+\.[A-Z]' <file>` (should be empty outside
  `import`/`package` lines).
- **FQCN allowed exceptions** — Name collisions, string literals (`Class.forName`, `@SuppressWarnings`),
  `java.lang.*` (always simple). **Applied.** The one deliberate FQCN kept:
  `freemarker.template.Configuration` in `AppConfiguration` — its simple name collides with Spring's
  `@Configuration` used on the same class.
- **`var` for obvious locals** — Use `var` when the RHS makes the type obvious; keep explicit types where
  readability needs it (interface-typed locals, loop `String line;`, etc.). **Applied** (codebase was
  already idiomatic; no remaining candidates).
- **Terse exception factories (`Shorts`)** — Use `iaex`/`rex`/`isex`/`ioex`/`uoex`/… with printf `%s`
  placeholders and `import static org.apache.juneau.commons.utils.Shorts.*;` instead of
  `new IllegalArgumentException(...)` etc. **Applied.** `org.apache.juneau.commons.utils.Shorts` is on the
  compile classpath (transitive via `juneau-commons 10.0.0-SNAPSHOT`); migrated 32 throw sites across 11
  files. Note: `Shorts` routes through `StringUtils.format`, which is **pure printf** — single quotes are
  literal, so do **not** double them (`''` is only for the MessageFormat `mf()`/`mfs()` path).
- **No process/planning vocabulary in code** — No work-item IDs, phase/wave/session markers, dated
  "revision" notes, decision numbers, or internal design-spec section citations (`spec §N`) in comments or
  Javadoc; rewrite as neutral standalone English. **Applied.** Removed all `Task N`, `spec §N`, `decision
  #N`, `CONFIRM-AT-BUILD`, `Slice N`, and `2026-08-15 revision` markers. (Dates that are genuine test
  **data** — e.g. `voteDeadline`, JSON `created`, `rcHistory` timestamps — are left untouched.)
- **`final` fields** — Declare instance fields `final` wherever possible; use `find`-prefixed helpers for
  memoized suppliers needing constructor params. **Applied.** All service-class fields are already `final`;
  the only non-final fields are intentionally mutable (public JSON-bean fields Juneau marshals, and the
  `ChangelogEntry.Builder` accumulators).
- **`instanceof` pattern-variable naming (`o` → `o2`)** — **N/A.** No `instanceof` pattern matches in the
  codebase.
- **`@SuppressWarnings` format** — Always the multiline brace/array form with a `//` rationale on every
  token, even a single token (except the inline catch-parameter `"unused"` case). **Applied.** Converted
  the four `@SuppressWarnings("unchecked")` (parsed-JSON casts) to the multiline form with rationale.
- **Delete dead code** — Remove unreachable branches / unused fields & imports rather than leaving them.
  **Applied** (none found).
- **Indentation: tabs** — **Applied (tabs)** — full Juneau parity; enforced by the checked-in Eclipse
  formatter + save-action profile in `.settings/`. The whole tree (98 Java files) was reformatted with the
  real Eclipse formatter using Juneau's tab profile (`org.eclipse.jdt.core.formatter.tabulation.char=tab`),
  so indentation, line-wrapping, and spacing all match upstream Juneau. The Eclipse convention metadata is
  now tracked in the repo: `.settings/org.eclipse.jdt.core.prefs` (compiler settings + inlined tab formatter
  profile), `.settings/org.eclipse.jdt.ui.prefs` (import order + cleanup/save-action profile), and
  `.settings/org.eclipse.core.resources.prefs` (UTF-8 encoding). These are sourced from Juneau's canonical
  `scripts/eclipse-preferences/` baseline; upstream keeps the formatter/cleanup as workspace-imported XML,
  but here they're inlined into the project `.settings/` so the profile is enforced project-scoped and
  travels with the repo. Only `.classpath` and `.project` remain git-ignored.

## Javadoc

- **Javadoc on public types & methods** — Brief description; `@param`/`@return`/`@throws` as applicable.
  **Applied.** Public types and non-trivial public methods are documented; trivial accessors/REST handlers
  are self-descriptive.
- **Juneau syntax-highlight tags (`<jc>`/`<jk>`/`<jv>`/…)** — **N/A (style choice).** This app does not use
  Juneau's Javadoc syntax-highlight tags (it doesn't ship the Juneau doclet/CSS); plain Javadoc with
  `{@code ...}` and `{@link ...}` is used consistently.

## Tests

- **Method naming `LNN_testName` + `@Nested L_category`** — **Partially applied / project style.** Test
  methods use descriptive camelCase names (e.g. `discoversMostRecentOpenRepoForProfile`) consistently
  across the suite; the `LNN_`/`@Nested L_category` scheme is not retrofitted here to avoid churning a
  green ~98-test suite for a purely cosmetic naming change. New tests should follow the existing
  descriptive-name style for consistency within this repo.
- **SSLLC test-data naming (`a`,`a1`,`b`…)** — **Project style.** Applied loosely; not enforced.
- **`TestBase` base class** — **N/A.** Juneau's `TestBase` lives in Juneau's internal test sources and is
  not published as a consumable test-jar dependency. Tests use plain JUnit 5 (`spring-boot-starter-test`).
- **`assertBean` / `assertMap` / DPPAP assertions** — **N/A.** Same reason as `TestBase` — Juneau's
  assertion helpers are not on this app's test classpath. Tests use JUnit 5 `Assertions.*`
  (`assertEquals`, `assertTrue`, `assertThrows`, `assertDoesNotThrow`).
- **`assertThrowsWithMessage`** — **N/A** (not available). Use JUnit 5 `assertThrows(...)` plus a
  `getMessage()`/`contains` check where message content matters.
- **`Flag` / `IntegerValue` for lambda state capture** — **N/A.** Not on the test classpath; use local
  mutable holders (e.g. `ArrayList`, `AtomicInteger`) where state capture is needed.
- **Exception factories in tests** — **Applied** where a test stub throws (e.g. `throw uoex()`), since
  `Shorts` is on the test classpath too.

## SonarQube findings policy

How to triage SonarQube/SonarLint findings in this repo, mirroring Apache Juneau's Sonar Suppression
Policy (upstream `juneau-code-conventions` skill → "Sonar Suppression Policy").

**Default: prefer a real fix.** Refactor the code so the finding no longer applies. Only suppress when
the finding matches one of Juneau's *standing* suppression rules (below).

**Suppression format (required).** When you do suppress, ALWAYS use the multiline brace/array form with a
`//` rationale comment on **every** token — even a single token. The inline single-string form
(`@SuppressWarnings("java:Sxxx") // ...`) is **not** allowed. Scope to the smallest practical target
(class-level for the class-shape rules). Example:

```java
@SuppressWarnings({
    "java:S6539" // Spring @Configuration legitimately aggregates cohesive bean wiring; splitting would fragment it.
})
```

The one exception is the inline `@SuppressWarnings("unused")` on a catch parameter, which cannot carry a
trailing comment.

**Suppress (Juneau standing policy — do not refactor):**

| Rule | Where | Rationale |
|------|-------|-----------|
| `java:S110` (inheritance depth) | `RootRest` | Depth is imposed by the Juneau REST servlet base-class hierarchy; flattening isn't appropriate. Class-level. |
| `java:S6539` (Monster Class / too many deps) | `AppConfiguration` | A Spring `@Configuration` legitimately aggregates bean wiring; splitting fragments cohesive wiring. Juneau suppresses S6539 unconditionally. Class-level. |
| `java:S107` (too many params) | `ReleaseEngine` constructor | Constructor-injected collaborators; a parameter object would obscure the DI wiring. Suppress at the constructor only when a holder/record refactor would read worse than plain constructor injection (document the choice). |
| `java:S1192` (duplicate literals) | — | Suppress at class level **only** when the repeated literal is a protocol-wire value, annotation attribute, or config key where a constant would obscure meaning. Ordinary duplicated strings → extract a constant instead (see below). |
| `java:S115` (constant naming) | — | Suppress at **class** level (never on the field) for `UPPER_camelCase` constants or constants mirroring an external-protocol literal. |
| `unchecked` (parsed-JSON casts) | JSON parse sites | Assigning a raw `Json.DEFAULT.read(…, List.class/Map.class)` result to its known parameterized shape is an unchecked conversion; keep the multiline-form suppression with a rationale. |

**Fix, don't suppress (rules handled by real changes in this repo):**

- `java:S1192` — extract a constant for ordinary duplicated string literals (not protocol/annotation/config keys). E.g. `EmailTemplate.DEV` (dev-list address), `ProcessRunner.Default.MSG_INTERRUPTED`/`MSG_ERROR`.
- `java:S125` — delete genuinely commented-out code; if it's explanatory prose that Sonar's recognizer misclassifies (e.g. a comment containing `Type.method()`), reword it so it isn't code-shaped rather than deleting the explanation.
- `java:S1845` — rename to remove a method/field name clash (prefer renaming the field): `CredentialSpec.name`→`id`, `StepResult.ok` field→`success` (keeping the `ok(…)`/`fail(…)` factory pair). Update all references + tests; keep behavior identical.
- `java:S1172` — remove unused parameters and simplify callers/tests.
- `java:S1905` — drop redundant casts; where a `var` + cast only supplied the generic type, switch to an explicit typed declaration.
- `java:S3400` — replace a method that always returns a constant with a declared constant (e.g. `SseLogServlet.HEARTBEAT`).
- `java:S2589` / `java:S1125` — remove always-true expressions and unnecessary boolean literals (`while (x || true)` → a real loop condition).
- `java:S135` — reduce a loop to ≤1 `break`/`continue` (combine conditions; use a boolean loop flag).
- `java:S1989` — never let a servlet method leak a checked `IOException`; catch and handle it (a broken SSE pipe just ends the stream).
- `java:S1210` — a class implementing `compareTo` gets `equals`/`hashCode` consistent with it.
- `java:S1130` — drop a `throws` clause that can't actually be thrown.
- `java:S2065` — drop `transient` on fields that are never serialized (e.g. REST-resource collaborators).
- `java:S5778` — in `assertThrows` lambdas, keep exactly one throwing invocation (hoist helper calls like `Map.of()` / builders out of the lambda).
- `java:S1659` — one variable declaration per line.
- `java:S1640` — use `EnumMap` for enum-keyed maps.
- `java:S1186` — give an empty method/constructor a nested comment explaining why it's empty (e.g. no-arg constructor required for JSON deserialization), or implement/throw.
- `java:S2142` — on a caught `InterruptedException`, call `Thread.currentThread().interrupt()` before proceeding.
- `java:S6126` — use a text block instead of multi-line string concatenation.
- **Web** (`Web:MetaRefreshCheck`, `Web:PageWithoutTitleCheck`, `Web:S5254`) — HTML needs `<html lang=…>`, a `<title>`, and an accessible redirect (inline `location.replace(...)` + a `<noscript>` link) rather than `<meta http-equiv="refresh">`.
- **JavaScript** (`javascript:S7764`, `javascript:S6582`) — prefer `globalThis` over `window`; use optional chaining (`a?.b`) instead of `a && a.b`.

## General rule

Any upstream convention that cannot be cleanly applied to this standalone app is documented as
**N/A-with-rationale** above rather than forced via a fragile/experimental dependency. Do not add a
Juneau internal test-jar or any new dependency solely to satisfy a convention.
