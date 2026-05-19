# FINISHED-41: Merged `juneau-rest-client-java-httpclient` into `juneau-rest-client`; JDK HttpClient is now the built-in default transport

## Outcome

The unreleased `juneau-rest-client-java-httpclient` module has been folded into the canonical `juneau-rest-client` artifact.  `JavaHttpTransport`, `JavaHttpTransportBuilder`, and `JavaHttpTransportProvider` were promoted to the root `org.apache.juneau.rest.client` package and ship inside `juneau-rest-client` itself.

End-user effect:

- `RestClient.create()` now works out of the box on Java 11+ with **zero** third-party dependencies — no transport module required.
- Pulling in one of the optional transport siblings (`juneau-rest-client-apache-httpclient-45`, `-apache-httpclient-50`, `-okhttp`, `-jetty`) registers a higher-priority `HttpTransportProvider` via `ServiceLoader` and automatically takes over discovery.
- `RestClient.builder().transport(...)` always wins over auto-discovery.
- `RestClient.discoverTransport()` now defensively falls back to `JavaHttpTransport.create()` when `ServiceLoader` returns no providers (covers heavily-shaded uber-jars where `META-INF/services` files can be stripped).

## What changed

### Sources

- `juneau-rest/juneau-rest-client-java-httpclient/src/main/java/org/apache/juneau/rest/client/javahttpclient/JavaHttpTransport.java` → `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/JavaHttpTransport.java`
- `…/JavaHttpTransportBuilder.java` → `…/rest/client/JavaHttpTransportBuilder.java`
- `…/JavaHttpTransportProvider.java` → `…/rest/client/JavaHttpTransportProvider.java`
- `…/src/main/resources/META-INF/services/org.apache.juneau.rest.client.HttpTransportProvider` → `juneau-rest/juneau-rest-client/src/main/resources/META-INF/services/org.apache.juneau.rest.client.HttpTransportProvider` (content updated to `org.apache.juneau.rest.client.JavaHttpTransportProvider`)
- The stale "Beta — part of `org.apache.juneau.ng.*`" boilerplate was removed from each class's javadoc since the NG promotion is already complete.

### `RestClient`

- Class-level javadoc updated to describe the JDK HttpClient transport as the built-in default and to document the override paths.
- `discoverTransport()` now falls through to `JavaHttpTransport.create()` when `ServiceLoader` finds no providers (was returning `null`, which then failed `assertArgNotNull`).
- `Builder.transport(...)` javadoc now references the JDK default.

### Module removal

- `juneau-rest/juneau-rest-client-java-httpclient/` directory deleted entirely.
- `<module>juneau-rest-client-java-httpclient</module>` removed from `juneau-rest/pom.xml`.
- `<dependency>juneau-rest-client-java-httpclient</dependency>` removed from `juneau-utest/pom.xml`.

### Tests

- `juneau-utest/src/test/java/org/apache/juneau/rest/client/JavaHttpTransport_Test.java` — dropped the now-redundant `import org.apache.juneau.rest.client.javahttpclient.*;`.
- `juneau-utest/src/test/java/org/apache/juneau/rest/client/RemoteInterfaceTransport_Test.java` — same.
- `RestClient_Test.h01_create_autoDiscoversTransport` continues to pass with no source change — it asserted `client.getTransport() != null`, which is now satisfied by the new fallback as well as by the in-jar SPI registration.

### Documentation (`juneau-docs`)

- `pages/topics/12.15.NextGenRestClient.md` — removed the `juneau-rest-client-java-httpclient` row from the transports table, added a one-paragraph callout above the table explaining the JDK transport is built-in / default, and updated the logging example so it no longer manually wires `JavaHttpTransport.create()`.
- `pages/topics/20.03.JuneauShadedRestClient.md` — reworded the dependencies section to call out the built-in JDK transport and clarified the role of the optional transport modules.
- `pages/release-notes/9.5.0.md` — refreshed the TODO-38 transports table to delete the JDK row, then added a new "JDK `HttpClient` is now the built-in default transport (TODO-41)" section with usage examples.

No migration-guide update was needed — the deleted module was never released, so no external consumer can have referenced it.

## Verification

- `mvn -pl juneau-rest/juneau-rest-client dependency:tree` — `juneau-rest-client` carries only Juneau-internal deps (`juneau-rest-common` → `juneau-commons`, `juneau-marshall`, `juneau-assertions`).  No `org.apache.httpcomponents:*` or any third-party HTTP client lib.
- `python3 scripts/test.py --full` — clean build of all 52 modules + full test suite green.  ~50,700 surefire tests, 0 failures, 0 errors.
- Cross-transport scenarios (`RemoteInterfaceTransport_Test`) all 60 cases pass against the five transports (JDK HttpClient + HC 4.5 + HC 5 + OkHttp + Jetty).

## Risk / rollback

Very low.  Three self-contained source files moved within the reactor, one SPI string updated, one one-line fallback added in `RestClient.discoverTransport()`.  Rollback would be a single `git revert` of the merge commit.

## Out of scope (not changed by this TODO)

- `MockHttpTransport` continues to be opt-in via explicit `transport(...)`; it does not register a `ServiceLoader` provider.
- The `HttpTransportProvider` SPI itself was not changed.
- The classic REST client (`juneau-rest-client-classic`) was not touched.
- No changes to provider priority numbers.
