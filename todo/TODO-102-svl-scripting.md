# TODO-102 — Scripting support in `VarResolver`

Introduce a new `#{...}` script-evaluation syntax in Juneau's SVL `VarResolver` that lets template strings call functions composed with existing `${...}` variable lookups. Replaces 11 existing transformation/conditional `Var` implementations with a unified function library; lookup `Var`s stay in the `${...}` namespace.

## Resolved decisions (OQA 2026-05-25)

1. **Syntax shape — Paren call form.** `#{name(arg1, arg2, ...)}`. Distinct from `$X{...}` so unambiguous. Args may themselves be `${...}` lookups, `$X{...}` Var invocations, or nested `#{...}` calls. Recursive-descent parser on the function-call body.

2. **Migration model — HARD BREAK in 9.5.0.** The 11 transformation/conditional `Var` classes are deleted outright. All call sites in the Juneau codebase migrate to `#{...}` form as part of Phase 5. Release notes flag the breaking change for downstream users; no deprecation cycle. Mirrors the FINISHED-20 hard-break pattern.

3. **Function discovery — BOTH `BeanStore` AND `ServiceLoader`.** Built-in catalog auto-registers via `BeanStore` chain (matches existing `Var` resolution pattern). Third-party functions also discoverable via `META-INF/services/org.apache.juneau.commons.svl.Function` for plug-in distribution. Resolution priority: explicit `VarResolver.Builder.functions(...)` > `BeanStore` beans > `ServiceLoader` entries > built-in catalog (later wins on name collision so users can override built-ins).

4. **Unknown-function failure — LAZY (resolve-time exception).** Templates that reference an unregistered function name don't fail at `VarResolver.Builder.build()`; they throw `IllegalArgumentException("No such function 'foo'")` from `VarResolverSession.resolve(...)` when the unknown reference is actually evaluated.

5. **Escape syntax — `\#{` literal, matching existing `\$` convention.** Phase 1 task includes verifying the existing `\$` escape behavior in `VarResolverSession` before implementing the symmetric `\#{` escape.

6. **Built-in catalog scope — LARGER (~40 v1 functions).** Beyond the 11 migration replacements, ship string-manipulation extensions, type-conversion, arithmetic, boolean logic, regex, encoding, and date/time. Full catalog in Phase 3 below.

7. **Type-coercion strategy — TYPED via reflection (Option 7B).** Function signatures declare typed args (`int`, `long`, `double`, `boolean`, `String`, `String[]`). The runtime coerces resolved string args to declared types via a centralized coercion table at the function-invocation boundary. Boolean coercion rules:
   - TRUE: `"true"`, `"1"`, `"yes"`, `"on"` (case-insensitive)
   - FALSE: `"false"`, `"0"`, `"no"`, `"off"`, `""` (case-insensitive)
   - Anything else: `IllegalArgumentException`

   Numeric coercion uses `Integer.parseInt` / `Long.parseLong` / `Double.parseDouble`; non-numeric input throws `IllegalArgumentException` wrapped with function context. `String[]` args accept the JSON-array shortcut form `["a","b","c"]` (parsed via Juneau JSON parser).

8. **Truthiness semantics — EXPLICIT.** `if`/`switch`/etc. boolean-typed args follow the OQ #7 coercion table verbatim — no implicit truthiness rules (no "empty string = false, non-empty = true" shortcut). Functions that want "is non-empty" semantics use the explicit `notEmpty(s)` function.

9. **Plan file timing — written after OQA (this file).**

## Hard-break removal inventory

The following Var classes will be **deleted** in 9.5.0:

| Class | Existing prefix (to verify) | `#{...}` replacement |
|---|---|---|
| `SubstringVar` | `$SS{...}` | `#{substring(s, start, end)}` |
| `SwitchVar` | `$SW{...}` | `#{switch(value, case1, val1, ...)}` |
| `IfVar` | `$IF{...}` | `#{if(cond, then, else)}` |
| `UpperCaseVar` | `$UC{...}` | `#{upper(s)}` |
| `LowerCaseVar` | `$LC{...}` | `#{lower(s)}` |
| `LenVar` | `$LN{...}` | `#{len(s)}` |
| `CoalesceVar` | `$CO{...}` | `#{coalesce(a, b, ...)}` |
| `NotEmptyVar` | `$NE{...}` | `#{notEmpty(s)}` |
| `PatternMatchVar` | `$PM{...}` | `#{match(s, regex)}` |
| `PatternReplaceVar` | `$PR{...}` | `#{replace(s, regex, replacement)}` |
| `PatternExtractVar` | `$PE{...}` | `#{extract(s, regex[, group])}` |

Phase 0 verifies the actual prefixes from the class source. The lookup Vars (`PropertyVar`, `ConfigVar`, `EnvVariablesVar`, `SystemPropertiesVar`, `ManifestFileVar`, `ArgsVar`, `DotenvVar`, `EnvFileVar`) stay in the `${...}` namespace — they're variable resolution, not transformation.

## Phases

### Phase 0 — Inventory + verification

1. Confirm actual prefix names for each of the 11 Var classes being removed.
2. Grep the entire Juneau codebase (Java source, `.cfg` test resources, Javadoc, `juneau-docs`, release notes) for every usage of `$SS{...}` / `$SW{...}` / `$IF{...}` / `$UC{...}` / `$LC{...}` / `$LN{...}` / `$CO{...}` / `$NE{...}` / `$PM{...}` / `$PR{...}` / `$PE{...}`.
3. Build a per-file migration table — file path + count + sample lines — and check it into the FINISHED archive when the work is done.
4. Verify the existing `\$` escape semantics in `VarResolverSession` so Phase 1 can mirror them for `\#{`.

**Acceptance:** complete migration table; documented escape semantics.

### Phase 1 — Tokenizer + recursive-descent parser

1. Extend `VarResolverSession`'s tokenizer to recognize `#{...}` as a new top-level token type alongside `${...}` and `$X{...}`.
2. Body collection respects nested `{` / `}` and ignores `}` inside quoted string literals.
3. New post-tokenization parser for the body: `name(arg, arg, ...)` where each arg is one of:
   - Quoted string literal (`"..."` or `'...'`, with `\"` / `\'` escape)
   - Bare identifier (treated as literal string, e.g. for `switch` case labels)
   - Numeric literal (integer or decimal)
   - Boolean literal (`true` / `false`)
   - Nested `${...}` / `$X{...}` / `#{...}` token
4. Implement `\#{` literal escape mirroring existing `\$` semantics.
5. Whitespace inside arg lists is tolerated.

**Acceptance:** parser unit tests covering each arg type + nesting + escape semantics; tokenizer tests covering coexistence with existing `${...}` and `$X{...}`.

### Phase 2 — `Function` SPI + reflection-based coercion

1. New SPI in `juneau-commons`:

   ```java
   public interface Function {
       String name();           // e.g. "substring"
       int minArity();          // -1 for unbounded
       int maxArity();
       String invoke(VarResolverSession session, List<Object> args);
   }
   ```

   `args` is `List<Object>` (already-coerced typed values), NOT `List<String>` — the reflection coercion layer happens before `invoke` is called.

2. New `TypedFunction` abstract class that uses Java reflection on a subclass's `invoke(...)` method signature to derive arity + arg types, eliminating per-function arity declaration:

   ```java
   public class SubstringFunction extends TypedFunction {
       @Override public String name() { return "substring"; }
       public String invoke(String s, int start, int end) { ... }
   }
   ```

   Reflection caches the `Method` + `Class<?>[]` arg types at function-registration time; per-call dispatch is one reflective invoke + N coercions.

3. Centralized `ArgCoercer` with the OQ #7 / OQ #8 coercion rules:
   - `String` → identity
   - `int`/`Integer` → `Integer.parseInt`
   - `long`/`Long` → `Long.parseLong`
   - `double`/`Double` → `Double.parseDouble`
   - `boolean`/`Boolean` → explicit truthiness table (see OQ #7)
   - `String[]` → JSON-array shortcut parse
   - `Object` → passthrough (for variadic functions)
   - Coercion failures wrap `NumberFormatException` / etc. with `IllegalArgumentException("Function 'X' arg N: cannot coerce 'Y' to int")` for debuggability.

4. Function registration on `VarResolver.Builder`:

   ```java
   VarResolver.Builder.functions(Function...)
   VarResolver.Builder.functions(Class<? extends Function>...)
   ```

5. Resolution priority chain (later wins on name collision):
   - Built-in catalog (auto-registered in `VarResolver.DEFAULT`)
   - `ServiceLoader<Function>` discovery
   - `BeanStore.getBeans(Function.class)`
   - Explicit `VarResolver.Builder.functions(...)` registrations

**Acceptance:** `Function` SPI + `TypedFunction` base + `ArgCoercer` + all four discovery channels working in tests.

### Phase 3 — Built-in function library (v1 catalog, ~40 functions)

Grouped by category. Each function is its own `TypedFunction` subclass in `org.apache.juneau.commons.svl.functions`.

**String manipulation (15):**
- `substring(s, start)` / `substring(s, start, end)`
- `upper(s)`, `lower(s)`
- `trim(s)`, `stripLeading(s)`, `stripTrailing(s)`, `stripSlashes(s)`
- `len(s)`
- `replace(s, target, replacement)` (literal — distinct from regex `replace` below)
- `contains(s, substr)`, `startsWith(s, prefix)`, `endsWith(s, suffix)`
- `concat(s1, s2, ...)`, `repeat(s, n)`, `reverse(s)`

**Type conversion (4):**
- `toInt(s)`, `toLong(s)`, `toDouble(s)`, `toBool(s)`

**Arithmetic (8):**
- `add(a, b)`, `subtract(a, b)`, `multiply(a, b)`, `divide(a, b)`, `modulo(a, b)`
- `min(a, b)`, `max(a, b)`, `abs(a)`

**Boolean logic (7):**
- `and(a, b, ...)`, `or(a, b, ...)`, `not(a)`, `xor(a, b)`
- `eq(a, b)`, `neq(a, b)`
- `lt(a, b)`, `lte(a, b)`, `gt(a, b)`, `gte(a, b)` — numeric comparison via OQ #7 numeric coercion

**Conditional (3):**
- `if(cond, then, else)` — replaces `IfVar`
- `switch(value, case1, val1, case2, val2, ..., default)` — replaces `SwitchVar`, last unmatched arg is the default
- `coalesce(a, b, ...)` — replaces `CoalesceVar`, returns first non-empty
- `notEmpty(s)` — replaces `NotEmptyVar`

**Regex (4):**
- `match(s, regex)` — replaces `PatternMatchVar`
- `extract(s, regex)` / `extract(s, regex, group)` — replaces `PatternExtractVar`
- `replaceRegex(s, regex, replacement)` — replaces `PatternReplaceVar` (note: `replace(...)` above is literal, `replaceRegex(...)` is regex)

**Encoding (4):**
- `base64Encode(s)`, `base64Decode(s)`
- `urlEncode(s)`, `urlDecode(s)`

**Stretch (deferred to v2 if 9.5.0 timeline doesn't allow):**
- `format(value, pattern)` — `String.format`-style
- `split(s, separator)`, `join(separator, parts...)`
- `htmlEscape(s)` / `htmlUnescape(s)`
- `jsonPath(json, path)` — JSON-path extraction with default value
- `now()`, `parseDate(s, format)`, `formatDate(epoch, format)`
- Map/list helpers (`get`, `keys`, `values`, `size`)

**Acceptance:** per-function unit tests in `juneau-utest`; each function's Javadoc shows the `#{...}` invocation form and (where applicable) the old-Var equivalent it replaces.

### Phase 4 — Discovery wiring + integration

1. `META-INF/services/org.apache.juneau.commons.svl.Function` file in `juneau-commons` listing every built-in function class.
2. `VarResolver.DEFAULT` includes the built-in catalog automatically.
3. `RestContext`'s default `VarResolver` build chain picks up `BeanStore`-registered `Function`s the same way it picks up `Var`s today.
4. `@Value` (FINISHED-79) test: `@Value("#{upper(${juneau.app.name:demo})}")` resolves correctly. New focused test in the `@Value` test class.

**Acceptance:** end-to-end integration test confirms each of the four discovery channels works; `@Value` regression test passes.

### Phase 5 — Hard-break migration

1. Delete the 11 transformation/conditional `Var` classes (per the inventory in Phase 0).
2. Migrate every call site identified in Phase 0 from `$XX{...}` form to `#{...}` form.
3. Update related Javadoc, topic docs, and release notes.
4. Run `./scripts/test.py` — all tests must stay green.

**Acceptance:** zero references to the deleted Var prefixes anywhere in the codebase; full test suite green.

### Phase 6 — Tests

New test classes in `juneau-utest`:

- `VarResolver_ScriptingTokenizer_Test` — `#{...}` recognition + coexistence with `${...}` / `$X{...}` + escape semantics.
- `VarResolver_ScriptingParser_Test` — recursive-descent parser cases (each arg type, nesting depth, malformed input).
- `Function_ArgCoercion_Test` — boolean/int/long/double/String[] coercion edge cases (including OQ #7 boolean rules verbatim).
- `Function_Discovery_Test` — all four channels (built-in / ServiceLoader / BeanStore / explicit) + priority-order resolution.
- One test class per function group: `StringFunctions_Test`, `ArithmeticFunctions_Test`, `BooleanFunctions_Test`, `ConditionalFunctions_Test`, `RegexFunctions_Test`, `EncodingFunctions_Test`, `TypeConversionFunctions_Test`.
- `VarResolver_NestedScripting_Test` — `#{...}` inside `${...}` (and vice versa) + deeply nested function composition.
- `VarResolver_UnknownFunction_Test` — lazy-fail behavior per OQ #4.
- `Value_Scripting_Test` (in the existing `@Value` test package) — `@Value("#{...}")` integration.

**Acceptance:** 100% branch coverage on the new `Function` SPI + `ArgCoercer`; ≥ 90% on each built-in function.

### Phase 7 — Docs + release notes

1. New topic page `juneau-docs/pages/topics/<n>.SvlScripting.md` documenting `#{...}` syntax, the function catalog, the coercion rules, and extension via `Function` SPI.
2. Update existing SVL topic page to cross-reference.
3. Release-notes entry in `9.5.0.md` flagging:
   - **Breaking change:** 11 transformation Vars removed; migration table from old prefixes to new `#{...}` form.
   - **New feature:** `#{...}` scripting syntax + ~40-function built-in catalog + `Function` SPI for extension.
   - **Migration guide:** worked examples for each of the 11 hard-break replacements + the `@Value`/`Config`/`RestOp(path)` integration points.

**Acceptance:** topic page + release-notes entry merged; migration guide reviewable as a standalone doc.

## Acceptance criteria (rollup)

- `#{...}` syntax parses + resolves per the spec across the four arg types + nesting + escape.
- All four discovery channels (built-in / ServiceLoader / BeanStore / explicit) work with the priority order above.
- ~40 built-in functions ship in v1; ≥ 11 of them are the migration replacements.
- The 11 old `Var` classes are deleted; zero call-site references remain.
- Tests green; full suite within ~5% of pre-change baseline wall-clock.
- `@Value("#{...}")` works end-to-end.
- Release notes + topic page published in `juneau-docs`.
- `todo/FINISHED-102-svl-scripting.md` archive captures the per-file migration table from Phase 0.

## Risks

- **Hard-break blast radius.** Phase 0 inventory may surface more usages than expected (especially in test `.cfg` files and Javadoc examples). Mitigation: complete Phase 0 inventory BEFORE starting Phase 5 migration so the scope is known.
- **Parser ambiguity at args boundary.** Commas inside quoted string args, nested function calls, and `${...}` lookups inside args all need careful tokenization. Mitigation: dedicated `VarResolver_ScriptingParser_Test` with adversarial inputs.
- **`ServiceLoader` startup cost.** Discovery scans all classpath jars at first `VarResolver.DEFAULT` use. Mitigation: lazy initialization + one-time cache.
- **Reflection coercion overhead.** Per-resolve cost is N reflective coercions + 1 reflective invoke. Mitigation: cache `Method` + arg-type array at function-registration time (one-time cost); benchmark the per-resolve dispatch against the existing `Var.resolve(...)` path to confirm no regression.
- **`@Value` integration is shared with TODO-79.** Need at least one regression test inside the `@Value` test class to prevent future `@Value` refactors from breaking the scripting path.

## Open items

_All OQA resolved 2026-05-25. None outstanding._

## Related work

- **FINISHED-99** — SVL resolution in `@RestOp(path)` / `@RestGet(path)`. The `#{...}` syntax composes seamlessly with the SVL-in-op-paths capability that landed in FINISHED-99.
- **FINISHED-79** — `@Value` annotation + `${...}` shortcut + Spring Boot config bridge. The new scripting syntax becomes immediately useful inside `@Value`, e.g. `@Value("#{coalesce(${app.name}, 'untitled')}")`.
- **TODO-92** — `@Value` framework-internal adoption pass. May benefit from the scripting layer when internal config readers need transformation (e.g. coercing config strings to typed defaults inline).
- **TODO-95** — Per-RestContext `@Value` resolution against `@Rest(config=...)`. Independent; the scripting layer doesn't change the resolution-source model, just what can be expressed inside a template.
