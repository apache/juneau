# TODO-21: Bean / inject annotation rename and commons.inject surface

Source: promoted from `TODO.md` on 2026-04-19.

## Goal

Reduce confusion between Juneau and Spring naming, align vocabulary with what each annotation actually does, and place **resource / store contribution** annotations in **`org.apache.juneau.commons.inject`** so they are not tied to REST-only packages and do not compete for names with REST-specific annotations.

## Naming decisions (proposed)

| Current | Proposed | Module / package |
|--------|----------|-------------------|
| `@org.apache.juneau.annotation.Bean` | `@BeanClass` | `juneau-marshall` — `org.apache.juneau.annotation` |
| `@Beanp` | `@BeanProp` | `juneau-marshall` (and `*Apply` siblings → `@BeanPropApply`, etc.) |
| `@Beanc` | `@BeanCtor` | `juneau-marshall` (and `*Apply` → `@BeanCtorApply`, etc.) |
| `@RestInject` | `@Bean` | **`org.apache.juneau.commons.inject`** (not `juneau.rest.annotation`) |

**Rationale**

- **`BeanClass`** describes type-level “how this class participates as a Java bean / in BeanMap and related machinery,” without implying Spring-style bean registration.
- **`BeanProp` / `BeanCtor`** replace cryptic abbreviations while staying in the same family as `BeanClass`.
- **`RestInject` → `@Bean`** matches the mental model of “this field or method contributes an object to the bean graph,” analogous in *role* (not necessarily semantics) to Spring `@Bean`. REST stacks discover these via **`BeanStore`** / resource wiring, not only servlet code.

## New / moved annotations in `org.apache.juneau.commons.inject`

1. **`@Bean`** (successor to `@RestInject`)  
   - Defined in **`org.apache.juneau.commons.inject`**.  
   - Used on fields and methods that supply or override values consumed by the framework (e.g. REST resource customization today).  
   - **Runtime disambiguation from Spring:** always compare **`annotation.annotationType()`** (or equivalent), never **`getSimpleName()`** alone — both could be named `Bean` but are different types.  
   - **Source caveat:** a single compilation unit cannot `import` both Spring’s `Bean` and Juneau’s `Bean` and use the short name `@Bean` for both; one side may need a fully qualified annotation.

2. **`@Configuration` (optional follow-on)**  
   - Same package could host a Juneau **`@Configuration`** for grouping or scanning rules **if** you want symmetry with common DI vocabulary.  
   - **Open design work:** document precisely what Juneau `@Configuration` means (marker for types that host `@Bean` methods? inheritance rules? interaction with `BeanStore` builders?). Avoid overlapping semantics with Spring unless intentionally bridged.

## Steps

1. **Marshall rename pass**  
   - Rename `@Bean` → `@BeanClass` and update all references, Javadoc, tests, and docs links (`BeanAnnotation` topics, etc.).  
   - Rename `@Beanp` / `@Beanc` and `@BeanpApply` / `@BeancApply` to the `BeanProp*` / `BeanCtor*` names (or chosen final spellings).  
   - Search for string literals, reflection by class name, and generated / serialized metadata that might hard-code old names.

2. **Introduce `org.apache.juneau.commons.inject.Bean` annotation**  
   - Port semantics from `RestInject` (attributes: name/value, `methodScope`, etc.).  
   - Ensure `juneau-commons` (or appropriate module) declares the annotation with correct **`@Target` / `@Retention`** matching today’s `RestInject`.

3. **Wire REST and other consumers to the new type**  
   - Replace `RestInject` usage in `juneau-rest-server` (and any other modules) with `org.apache.juneau.commons.inject.Bean`.  
   - Keep **`RestInject`** as **deprecated alias** for one release if policy requires, delegating to the same annotation processor path, **or** remove in a major with migration notes only — pick per release policy.

4. **Annotation discovery**  
   - Update any code that looks for `RestInject.class` to look for `org.apache.juneau.commons.inject.Bean.class`.  
   - Audit for **`getSimpleName()`** checks on `"Bean"` or `"RestInject"` and fix to type-safe checks.

5. **`@Configuration` (if pursued)**  
   - Specify behavior, add types, add tests, document difference from Spring `Configuration`.

6. **Docs and migration**  
   - `juneau-docs`: migration guide rows (coordinate with **TODO-17**).  
   - Release notes: breaking renames for marshall; REST inject rename.

7. **Related TODO**  
   - **TODO-5** (“Synonym for @Bean…”) is subsumed by this plan; remove **TODO-5** from `TODO.md` when implementation is underway or complete to avoid duplicate tracking.

## Notes

- **Module boundaries:** confirm `juneau-rest-server` already depends on the artifact that will host `org.apache.juneau.commons.inject.Bean`; adjust POMs if the annotation must live in a module every REST user already pulls in.  
- **Binary compatibility:** renames are breaking; batch with other 9.x breaking changes if desired.  
- **IDE / import ergonomics:** document recommended imports for apps that use both Spring and Juneau (`Bean` vs `BeanClass` vs Spring `Bean`).
