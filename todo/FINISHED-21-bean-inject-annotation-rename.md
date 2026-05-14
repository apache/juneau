# FINISHED-21: Bean / inject annotation rename and commons.inject surface (complete)

This file is the closeout record for TODO-21.

---

## Scope

TODO-21 covered the annotation-surface cleanup around bean/inject naming and the migration from REST-only
injection annotation usage to the commons.inject surface.

---

## Final outcomes

- Adopted `org.apache.juneau.commons.inject.Bean` as the replacement for `@RestInject`.
- Migrated REST-side consumers and annotation discovery paths to the commons.inject annotation type.
- Completed the bean/marshalling annotation rename work for the 9.5 line and documented the breaking changes in:
  - `juneau-docs/pages/release-notes/9.5.0.md`
  - `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`

---

## Follow-on moved out of scope

The optional Juneau `@Configuration` support idea was intentionally split out to:

- `todo/TODO-23-commons-inject-framework-roadmap.md`

TODO-23 now owns design and implementation of `org.apache.juneau.commons.inject.Configuration`.

