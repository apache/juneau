# TODO


- [TODO-7] Decouple `juneau-rest-common` from `juneau-marshall` by breaking the compile dependency so REST annotations and beans can be used without pulling in the full serialization stack. See `todo/TODO-7-decouple-rest-common-from-marshall.md`.

- [TODO-14] Close the latent map-key coercion gap in `BeanPropertyMeta.setPropertyValue` (`needsConversion` predicate inspects only entry values, not entry keys; key-side `convertToType` call missing). Currently unreachable from any tested parser thanks to the per-parser Bug #7b fixes, but a defense-in-depth backstop. See `todo/TODO-14-beanpropertymeta-map-key-coercion.md`.

- [TODO-17] Audit 9.2.x changes (juneau-docs release notes 9.2.0 / 9.5.0 + git history since 9.1.0) for breaking changes and populate the v9.5 Migration Guide at juneau-docs/pages/topics/23.01.V9.5-migration-guide.md with Old→New rows for each. Focus on removed APIs, renamed annotations/classes/methods, changed default behaviors, and any annotation-attribute semantics changes.

- [TODO-18] Investigate possible useful features to add to juneau-rest-server.

- [TODO-30] Investigate moving `ClassMeta` and related non-marshalling type metadata from `juneau-marshall` into `juneau-commons` (analysis/feasibility pass). See `todo/TODO-30-classmeta-to-commons.md`.

- [TODO-58] Fix silent element-drop when `BeanMap.put` converts `List<String>` → `Set<EnumType>` against an `@Beanp(type=TreeSet.class, params=EnumType.class)`-annotated property. Surfaced downstream in `central-routing/irs` PR #1806, which had to add a per-bean `parse()` override to work around it. See `todo/TODO-58-beanmap-typed-set-element-coercion.md`.

