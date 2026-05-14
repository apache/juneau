# TODO-21: Bean / inject annotation rename and commons.inject surface

Source: promoted from `TODO.md` on 2026-04-19.

## Status

Core scope for TODO-21 is complete and shipped in the 9.5 line.

## Completed outcomes

- Introduced and adopted `org.apache.juneau.commons.inject.Bean` as the replacement for `@RestInject`.
- Migrated REST-side usage and annotation discovery to the commons.inject annotation surface.
- Completed the bean/marshalling annotation rename work and documented breaking changes in release notes and migration docs.

## Follow-on moved to TODO-23

The optional Juneau `@Configuration` concept is now explicitly tracked under:

- `todo/TODO-23-commons-inject-framework-roadmap.md`

That TODO now owns design and implementation of any `org.apache.juneau.commons.inject.Configuration` support.
