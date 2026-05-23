# FINISHED-72 - `@Rest(mixins=...)` composition + `@Rest(paths=...)` multi-mount

Completed 2026-05-23. Landed new `@Rest(mixins=...)` operation composition and `@Rest(paths=...)` top-level multi-mount support, refactored `BasicHealthResource` to explicit probe paths (`/healthz`, `/readyz`, `/livez`), and eliminated root `/*` collisions for health probe deployment while preserving an explicit standalone-mount escape hatch.

## Goal

Resolve `BasicHealthResource` root-path collisions (`/*`) while adding a reusable resource-composition primitive for REST operations.

## Phase 0 findings

- `RestContext.restOperations` is the exact method-discovery seam (`RestContext.java`, `restOperations` memoizer).
- `RestOpInvoker` always invoked on `RestSession.getResource()`, so mixins required an explicit invocation target supplier.
- `JettyServerComponent` auto-mount path logic was single-path (`restPathFor` + `mountWithCollisionCheck`), so multi-mount required a path array abstraction plus one-holder/multi-pattern registration.

## Implementation checklist

- [x] Add `@Rest(mixins=Class<?>[])`.
- [x] Add `@Rest(paths=String[])`.
- [x] Wire both through `RestAnnotation` and constants.
- [x] Update `JettyServerComponent` auto-discovery to honor `paths`.
- [x] Add mixin operation discovery in `RestContext`.
- [x] Add mixin invocation target support in `RestOpContext` / `RestOpInvoker` (including RRPC variant).
- [x] Refactor `BasicHealthResource` to `@Rest(paths={"/healthz","/readyz","/livez"})`.
- [x] Add and update tests for paths, mixins, conflicts, and health-as-mixin.
- [x] Update release notes + health topic + migration guide docs.
