# FINISHED-102 — Scripting support in `VarResolver` (`#{...}` syntax)

Joint landing with TODO-103 (`VarTemplate` compilation + `resolveSupplier()`). Shared
infrastructure (unified tokenizer/compiler + segment classes) was built once and used by both
features. See also [`FINISHED-103-varresolver-template-compilation.md`](FINISHED-103-varresolver-template-compilation.md).

## What landed

A new `#{name(args)}` function-call syntax in Juneau's SVL `VarResolver`, with a ~68-function
built-in catalog and an extensible `VarFunction` SPI. The 11 single-purpose transformation
`Var` classes were removed (hard break, no `@Deprecated` shims); their behaviour is recovered
via the new function syntax.

The `#{...}` form composes naturally with `${...}` and `$X{...}` forms — a single
recursive-descent compiler handles all three — and is recognised by `VarResolver.DEFAULT`,
`RestContext.varResolver`, and any user-built resolver that calls `.defaultFunctions()`.

## Why

Before this change, each transformation/conditional behaviour (upper-case, ternary, switch,
regex match, regex replace, regex extract, substring, length, coalesce, not-empty, lower-case)
required its own dedicated `Var` class with its own prefix and parsing rules. Adding a new
transformation meant adding a new `Var` class + reserving a new prefix; composing two
transformations required nesting two `Var` invocations (`$UC{$LC{...}}`). The function-call
syntax flattens that into a single extensible call form with reflection-derived arity + arg
coercion. It also drops the 11-class maintenance surface and lets the catalog grow without
prefix collisions.

## Design decisions

All OQA-resolved 2026-05-25 in the plan file. Recap:

1. **Syntax shape — paren call form.** `#{name(arg1, arg2, ...)}`. Distinct from `$X{...}` so
   unambiguous. Args may themselves be `${...}` lookups, `$X{...}` invocations, or nested
   `#{...}` calls. Recursive-descent parser on the function-call body.

2. **Migration model — HARD BREAK in 9.5.0.** All 11 transformation/conditional `Var` classes
   deleted outright. No `@Deprecated` shims. Mirrors the FINISHED-20 hard-break pattern.

3. **Function discovery — BOTH `BeanStore` AND `ServiceLoader`.** Built-in catalog auto-registers;
   third-party functions via `META-INF/services/org.apache.juneau.commons.svl.VarFunction`.
   Resolution priority chain (later wins on name collision): explicit `functions(...)` >
   `BeanStore` beans > `ServiceLoader` entries > built-in catalog.

4. **Unknown-function failure — LAZY (resolve-time exception).** Templates referencing an
   unregistered function name don't fail at `VarResolver.Builder.build()`; they throw
   `IllegalArgumentException("No such function 'foo'")` from
   `VarResolverSession.resolve(...)` when the unknown reference is actually evaluated.

5. **Escape syntax — `\#{` literal, matching existing `\$` convention.**

6. **Built-in catalog scope — ~68 v1 functions, no v2 deferral.** Beyond the 11 migration
   replacements, ship string-manipulation extensions, type-conversion, arithmetic, boolean
   logic, regex, encoding, date/time, random/UUID, and JSON navigation.

7. **Type-coercion strategy — TYPED via reflection (Option 7B).** `TypedFunction` declares typed
   `invoke(...)` signatures; `ArgCoercer` reflects on the method signature and coerces string
   args. Boolean coercion rules: `"true"`, `"yes"`, `"on"`, `"1"` truthy; everything else
   (including `null` and empty string) falsy.

8. **Truthiness semantics — EXPLICIT.** No implicit "empty string = false" shortcut; functions
   that want "is non-empty" semantics use the explicit `notEmpty(s)` function.

9. **Plan-file timing — written after OQA.**

## API surface (as shipped)

```java
// SPI — implement to publish a new function.
package org.apache.juneau.commons.svl;
public interface VarFunction {
    String name();
    int minArity();
    int maxArity();
    String invoke(VarResolverSession session, List<Object> args);
}

// Reflection-based base class — derives arity + arg types from invoke(...).
public abstract class TypedFunction implements VarFunction { ... }

// Builder extensions.
public class VarResolver.Builder {
    public Builder functions(VarFunction... values);
    public Builder functions(Class<? extends VarFunction>... values);
    public Builder defaultFunctions();   // built-in catalog + ServiceLoader discovery
}

// VarResolver.DEFAULT picks up .defaultVars().defaultFunctions().build() by default.
```

## Built-in catalog (as shipped)

~68 functions across 10 categories, each as its own `TypedFunction` subclass in
`org.apache.juneau.commons.svl.functions`:

- **String** — `upper`, `lower`, `len` (1-arg char count + 2-arg part count), `substring`,
  `trim`, `stripLeading`, `stripTrailing`, `stripSlashes`, `pathToken`, `replace` (literal),
  `contains`, `startsWith`, `endsWith`, `concat`, `repeat`, `reverse`, `format`, `split`,
  `join`.
- **Type conversion** — `toInt`, `toLong`, `toDouble`, `toBool`.
- **Arithmetic** — `add`, `subtract`, `multiply`, `divide`, `modulo`, `min`, `max`, `abs`,
  `round`, `floor`, `ceil`.
- **Boolean** — `and`, `or`, `not`, `xor`, `eq`, `neq`, `lt`, `lte`, `gt`, `gte`.
- **Conditional** — `if`, `switch` (glob matching), `coalesce`, `notEmpty`, `case`.
- **Regex** — `match`, `extract`, `replaceRegex`.
- **Encoding / escaping** — `base64Encode`, `base64Decode`, `urlEncode`, `urlDecode`,
  `htmlEscape`, `htmlUnescape`.
- **Date / time** — `now`, `today`, `formatDate`, `parseDate`, `parseDate(s, format)`.
- **Random / UUID** — `rand`, `randInt`, `randLong`, `randString`, `randChoice`, `uuid`.
- **JSON navigation** — `jsonPath`, `get`, `keys`, `values`, `size`.

See <a href="/site/apidocs/org/apache/juneau/commons/svl/functions/package-summary.html" target="_blank">org.apache.juneau.commons.svl.functions</a>
javadoc for full per-function signatures.

## Hard-break Var migration (11 classes removed)

| Removed `Var` | Removed prefix | `#{...}` replacement |
|---|---|---|
| `IfVar` | `$IF{...}` | `#{if(cond, then, else)}` |
| `SwitchVar` | `$SW{...}` | `#{switch(value, pat1:result1, ..., *:default)}` (glob) |
| `CoalesceVar` | `$CO{...}` | `#{coalesce(a, b, c, ...)}` |
| `NotEmptyVar` | `$NE{...}` | `#{notEmpty(arg)}` |
| `PatternMatchVar` | `$PM{...}` | `#{match(value, pattern)}` |
| `PatternReplaceVar` | `$PR{...}` | `#{replaceRegex(value, pattern, replacement)}` |
| `PatternExtractVar` | `$PE{...}` | `#{extract(value, pattern[, groupIndex])}` |
| `UpperCaseVar` | `$UC{...}` | `#{upper(arg)}` |
| `LowerCaseVar` | `$LC{...}` | `#{lower(arg)}` |
| `LenVar` | `$LN{...}` | `#{len(arg)}` (1-arg) or `#{len(arg, delim)}` (2-arg) |
| `SubstringVar` | `$ST{...}` | `#{substring(arg, start)}` or `#{substring(arg, start, end)}` |

Source-data Vars (`$E{}`, `$S{}`, `$MF{}`, `$A{}`, `$P{}`, `${...}` shortcut, `$C{}` config var)
are **unchanged**. Only the 11 transformation/conditional Vars were removed.

Before/after migration examples:

```text
Before: "$UC{$S{user.name}}"
After:  "#{upper(${user.name})}"

Before: "$IF{$S{prod},prod.key,dev.key}"
After:  "#{if(${prod}, prod.key, dev.key)}"

Before: "$SW{$S{os},*win*:Windows,*nix*:Linux,*:Unix}"
After:  "#{switch(${os}, *win*:Windows, *nix*:Linux, *:Unix)}"

Before: "$PR{$S{name},[aeiou],*}"
After:  "#{replaceRegex(${name}, [aeiou], *)}"

Before: "$LN{a,b,c,,}"        (delimiter-based part count)
After:  "#{len(a,b,c, ,)}"

Before: "$ST{hello,2,4}"
After:  "#{substring(hello, 2, 4)}"
```

Codebase scans found 12 in-source usage sites total (mostly in examples + tests); all were
migrated as part of Phase F. Zero deleted-prefix references remain in the codebase.

### Switch glob matching parity

The legacy `SwitchVar` matched cases as glob patterns (`*` = any sequence, `?` = single char).
`#{switch(...)}` preserves that behaviour — patterns are compiled to a regex equivalent
(`*` → `.*`, `?` → `.`) and matched against the value. Exact-string matches still work as
glob-without-wildcards.

### `len(s, delimiter)` two-arg parity

The legacy `LenVar` had a two-argument form that split the string by `delimiter` and returned
the part count (not character length). `#{len(arg)}` (1-arg) returns character length;
`#{len(arg, delim)}` (2-arg) returns part count. Both forms ship.

## Tests as shipped

New test classes in `juneau-utest`:

- `VarResolver_ScriptingTokenizer_Test` — `#{...}` recognition + coexistence with `${...}` /
  `$X{...}` + escape semantics.
- `VarResolver_ScriptingParser_Test` — recursive-descent parser cases (each arg type, nesting
  depth, malformed input, quoted strings).
- `VarFunction_Test` — `TypedFunction` reflection dispatch + `ArgCoercer` boolean/int/long/
  double/`String[]` coercion + lazy-fail for unknown functions.
- `VarFunctionDiscovery_Test` — all four discovery channels (built-in / ServiceLoader /
  BeanStore / explicit) + priority-order resolution.
- One per function group: `StringFunctions_Test`, `ArithmeticFunctions_Test`,
  `BooleanFunctions_Test`, `ConditionalFunctions_Test`, `RegexFunctions_Test`,
  `EncodingFunctions_Test`, `TypeConversionFunctions_Test`, `DateFunctions_Test`,
  `RandomFunctions_Test`, `JsonFunctions_Test`.
- `ScriptSegment_Precompiled_Test` (Phase G) — verifies `#{...}` dispatch caches function
  references at compile time and reuses them on every subsequent resolve.

Phase F deleted the 11 corresponding `*VarTest` classes alongside the Var classes themselves.

Full suite passes (~88s wall-clock; baseline ~84.5s — within JIT/CI noise).

## Composition with `@Value` + TODO-103

`#{...}` functions evaluate per-resolve and are never folded (stable-value folding is
`Var`-side only). Combined with TODO-103's `Supplier<String>` autodetect, this produces live
factory patterns from the field type alone:

```java
// Resolves ONCE at bean construction — stable per-instance random token.
@Value("#{randString(32)}") String oneShotToken;

// Fresh UUID on every .get() — factory pattern. No annotation flag required.
@Value("#{uuid()}") Supplier<String> requestId;

// Composed: tenant-prefixed idempotency key, fresh per call.
@Value("#{concat(${tenant}, -, #{uuid()})}") Supplier<String> idemKey;
```

See [`FINISHED-103-varresolver-template-compilation.md`](FINISHED-103-varresolver-template-compilation.md)
for the field-type autodetect mechanics and the full "patterns emerging from composition"
catalogue.

## Verification

```
$ ./scripts/test.py --test-only
…
✅ Tests passed!
```

Full-suite wall-clock: **88.5s** (Phase F baseline: 84.5s). The slight increase is within
normal JIT/CI noise and adds ~3s of new Phase G regression tests + ~1.2s of the optional
micro-benchmark in `VarResolver_Benchmark_Test`.

## Known follow-ups / explicit deferrals

- **TODO-104** — Mixin `pathToken()` retrofit. Was hard-dep blocked on this TODO's catalog;
  the `pathToken(s)` function is now in place. Worker can pick it up next.
- **Function-discovery startup cost.** `ServiceLoader.load(VarFunction.class, ...)` runs once at
  `VarResolver.DEFAULT` initialisation. Builder API supports lazy/explicit registration for
  callers that want to skip the scan; no further optimisation deemed necessary.
- **Random function determinism for tests.** `rand*` functions use `ThreadLocalRandom`; users
  needing deterministic test output can register a seeded `VarFunction` impl that shadows the
  built-in names via the discovery priority order (explicit > built-in).
- **JSON-navigation parse cost.** `jsonPath` / `get` / `keys` / `values` / `size` parse the
  input JSON on every call. Acceptable for v1; for high-frequency call sites the TODO-103
  compiled-form path amortises some compile cost but not the parse cost. Future opt
  (per-session `JsonNode` cache) is out of scope for this landing.
- **Plan file** — `todo/TODO-102-svl-scripting.md` removed per `/todo` workflow; content lives
  in this archive.

## Related

- **TODO-103** — `VarResolver` template compilation + `resolveSupplier()` API. Shared
  infrastructure; joint landing. See [`FINISHED-103-varresolver-template-compilation.md`](FINISHED-103-varresolver-template-compilation.md).
- **TODO-79** — `@Value` annotation + `${...}` shortcut. Functions compose naturally inside
  `@Value` expressions; the field-type-driven Supplier autodetect (TODO-103 Phase 3 retrofit)
  unlocks the live-factory patterns documented above.
- **FINISHED-99** — SVL resolution in `@RestOp(path)`. `#{...}` works inside op paths the same
  way `${...}` does — the same `VarResolver.resolve(...)` pipeline handles both.
- **TODO-104** — Mixin `pathToken()` retrofit. Unblocked by this TODO; the `pathToken(s)`
  function used to normalise user-supplied path tokens is now in the catalog.
