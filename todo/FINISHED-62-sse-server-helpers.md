# FINISHED-62: Server-side SSE helpers (broadcaster, per-event flush, heartbeat)

Completed on 2026-05-22.

## Delivered

- Added `RestResponse.sse()` and `org.apache.juneau.rest.sse.SseResponseSupport` for fluent SSE response writes.
- Added `SseBroadcaster`, `SseSubscription`, and `SseHeartbeat` in `juneau-rest-server`.
- Added `SseBroadcasterArg` and `SseSubscriptionArg` and wired them into default `restOpArgs`.
- Added SSE helper/broadcaster tests in `juneau-utest`.
- Updated `SseDemoResource` with a broadcaster endpoint example.
- Added docs page `pages/topics/10.08.RestServerSse.md`, sidebar entry, and a `9.5.0` release note entry.

## TODO updates

- Archived this plan as finished.
- Removed `[TODO-62]` from `todo/TODO.md`.
