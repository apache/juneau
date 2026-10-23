# About Apache Juneau™

Apache Juneau™ is a single cohesive Java ecosystem for marshalling Java objects to a wide variety of language types and 
creating annotation-based REST end-to-end server and client APIs. Release 10.0 extends the REST server with server-rendered
admin consoles and rich table views, adds Model Context Protocol (MCP) server and client support, and ships a runnable
Petstore demo that shows them together.

[![The Juneau Petstore admin console](/topics/JuneauPetstore.Tour.1.png)](/docs/topics/JuneauPetstore)

## Common Use Cases

Juneau excels in the following scenarios:

* **Marshalling Java beans to a broad set of data languages and wire formats with zero required dependencies** - Serialize and parse POJOs as JSON, JSON5, JSONL, XML, HTML, URL-Encoding, UON, OpenAPI, PlainText, CSV, SOAP XML, MessagePack, BSON, CBOR, Protobuf, Prototext, Parquet, Markdown, TOML, HOCON, HJSON, INI, YAML, SSE, RDF/XML, RDF/XML-ABBREV, N-Triple, N-Quads, N3, Turtle, TriG, TriX, JSON-LD, RDF/JSON, RDF/Proto, and RDF/Thrift
* **Creation of self-documenting Bean-based REST microservices for Spring Boot, Jetty, and Tomcat** - Build REST services with automatic OpenAPI/Swagger documentation, content negotiation, and POJO-based request/response handling
* **Creation of Java interface proxies on top of existing REST APIs** - Generate type-safe client proxies that make REST calls feel like local method invocations
* **Powerful configuration files supporting multiple formats including INI and YAML** - Manage application configuration with POJO support, embedded SVL variable resolution, real-time file watching, and pluggable config stores
* **Serverless unit testing of REST APIs** - Test REST services without servlet containers using MockRestClient for fast, comprehensive testing
* **Microservice development with production-focused building blocks** - Start quickly with `juneau-microservice` and deploy on Jetty or Tomcat while reusing REST APIs, auth filters (JWT, SAML, OAuth/OIDC), reactive endpoints, observability hooks (Micrometer/OpenTelemetry), and configuration APIs across services
* **Using one ecosystem across service, client, and DTO layers** - Keep service contracts and model objects aligned with reusable DTO modules (OpenAPI, JSON Schema, RFC 7807, MCP, Atom/HAL/JSON:API) and transport adapters for Apache HttpClient 4.5/5.x, OkHttp, or Jetty clients
* **Universal Java marshalling** - The framework handles the full breadth of Java language features automatically during marshalling — beans, generics, collections, enums, records, optionals, and more — and is extensible via swaps and surrogate classes so that virtually any Java artifact can be serialized and deserialized
* **Bean-Centric Testing and fluent-style assertions** - Write compact, expressive unit tests that are considerably smaller than equivalent JUnit-alone tests; juneau-test provides deep bean-aware assertions and fluent validation chains that eliminate boilerplate while improving readability
* **Content negotiation and HTTP/2 support** - Handle multiple content types automatically with modern HTTP features
* **Server-rendered admin consoles** - Build a themeable console on `juneau-rest-server` with the [console chrome](/docs/topics/AdminConsoleChrome): page tabs and subtabs, FreeMarker `<@console>`, `<@page>` and `<@card>` directives, and a CSS theme token allowlist. [Building a Console Page](/docs/topics/BuildingAConsolePage) is the end-to-end tutorial.
* **Rich table views** - Describe a table with plain beans or a JSON5 catalog and get search, sort, paging, a column chooser, saved View Settings, row actions, bulk mutation, cell popovers, row-detail regions and shareable links from a first-party JavaScript runtime. See [Rich Table Views](/docs/topics/RichTableViews).
* **Chart cards, run and terminal regions** - Draw line, bar and donut [chart cards](/docs/topics/ChartCards) as inline SVG with no third-party chart library, show multi-step test and build runs in the [Run View Region](/docs/topics/RunViewRegion), and render a command's raw output in the [Terminal Region](/docs/topics/TerminalRegion) with xterm.js.
* **Async jobs and live updates** - Start long-running work from a [job-log card](/docs/topics/JobLogCard) or a table row ([Row Jobs](/docs/topics/RowJobs)) and stream its progress over Server-Sent Events ([Async Jobs](/docs/topics/RichTableViewsAsyncJobs)). The [console message bus](/docs/topics/ConsoleMessageBus) pushes server state to pages without polling.
* **Declarative widgets and server-side views** - Describe a dialog, form, calendar, row of stat tiles or badge as data-only beans with [Declarative Widgets](/docs/topics/DeclarativeWidgets), and render Markdown to HTML with [Markdown View Rendering](/docs/topics/MarkdownViewRendering). Template-based views remain available for [JSP](/docs/topics/JspViewSupport), [Thymeleaf](/docs/topics/ThymeleafViewSupport), [Mustache](/docs/topics/MustacheViewSupport) and [FreeMarker](/docs/topics/FreemarkerViewSupport).
* **Model Context Protocol (MCP) servers and clients** - Expose tools, prompts and resources to AI assistants from a `juneau-rest-server` resource with [juneau-rest-server-mcp](/docs/topics/JuneauRestServerMcp), backed by the [MCP wire beans](/docs/topics/JuneauBeanMcp) for revisions `2025-06-18` and `2026-07-28`. New to MCP in Juneau? Start with the [MCP Quickstart](/docs/topics/JuneauMcpQuickstart).
* **A worked example you can run** - The [juneau-petstore](/docs/topics/JuneauPetstore) sample deploys on Jetty and Spring Boot with the same REST surface and an admin console that exercises the pieces above, page by page.
* **Board build output** - [juneau-run](/docs/topics/JuneauRun) wraps Maven, pytest, Playwright, Gradle, npm and Bazel runs, parses their output into steps and test counts, and writes the run-view events the console regions read.

## When to Choose Juneau

Not sure if Juneau is right for your project? Check out our detailed [Why Choose Juneau?](/docs/topics/WhyJuneau) page for a comprehensive comparison with alternatives, or see our [Framework Comparisons](/docs/topics/FrameworkComparisons) page for detailed technical comparisons.

**Quick summary:** Choose Juneau when you need multi-format serialization, REST APIs with automatic content negotiation, or want an integrated solution with minimal dependencies. Also consider it when the same Java service needs a server-rendered admin console ([console chrome](/docs/topics/AdminConsoleChrome) and [rich table views](/docs/topics/RichTableViews)) or an [MCP](/docs/topics/JuneauRestServerMcp) endpoint, without adding a separate front-end stack. Upgrading from 9.x? See the [V10.0 Migration Guide](/docs/topics/V10MigrationGuide).

## Ecosystem

The Juneau ecosystem consists of the following parts. A few rows are **grouped families** rather than fully independent modules — a shared, revision- or spec-neutral core plus multiple thin sibling modules built on it (the JSON-RPC/MCP core-plus-adapter rows below are one example, alongside the pre-existing OpenAPI/Swagger spec-version pair); each row's description says how it relates to its siblings, and [Choosing your MCP setup](/docs/topics/JuneauMcpChoosingSetup#module-shape-3-shared-cores-6-per-revision-adapters) has the full MCP family tree.

| Component | Description |
|-----------|-------------|
| **juneau-core** | |
| [juneau-commons](/docs/topics/JuneauCommons) | Bean metadata framework, SVL string-variable resolution, and shared utilities (I/O, reflection, HTTP, collections) used across Juneau modules. |
| [juneau-beanquery-sql](/docs/topics/JuneauCommonsBeanQuery#sql-dialects) | Dialect-agnostic JDBC `BeanQuery` context that runs bean queries against a SQL database. |
| [juneau-beanquery-postgres](/docs/topics/JuneauCommonsBeanQuery#postgresdialect-behaviour) | PostgreSQL dialect for juneau-beanquery-sql above. |
| [juneau-marshall](/docs/topics/JuneauMarshall) | POJO marshalling for 26+ wire formats — JSON/JSON5/JSONL/JSON5L/JCS, XML/SOAP, HTML, YAML/TOML/HOCON/HJSON/INI, CBOR/BSON/MessagePack, Protobuf/Prototext, UON/URL-encoding, CSV, Parquet, Markdown, SSE, OpenAPI, and PlainText — plus a token-streaming API; no external dependencies required. |
| [juneau-marshall-rdf](/docs/topics/JuneauMarshallRdf) | Extended marshalling support for 12 RDF variants — RDF/XML, RDF/XML-Abbrev, N3, N-Triple, N-Quads, Turtle, TriG, TriX, JSON-LD, RDF/JSON, RDF/Proto, and RDF/Thrift (requires Apache Jena). |
| [juneau-config](/docs/topics/JuneauConfig) | Multi-format (INI and YAML) configuration file API with POJO support, variable resolution, and real-time file watching. |
| [juneau-test](/docs/topics/JuneauTest) | Fluent assertions, Bean-Centric Testing, and JUnit 5 extensions for readable, expressive unit tests. |
| [juneau-test-utils](https://github.com/apache/juneau/tree/master/juneau-core/juneau-test-utils) | Shared test infrastructure for Juneau modules (commons-level; no juneau-marshall dependency). |
| [juneau-test-marshall-utils](https://github.com/apache/juneau/tree/master/juneau-core/juneau-test-marshall-utils) | Marshall-level shared test fixtures (bean tester, marshall-annotated POJOs, round-trip harness), used at test scope by downstream modules. |
| **juneau-bean** | |
| [juneau-bean-atom](/docs/topics/JuneauBeanAtom) | DTOs for the Atom Syndication Format (RFC 4287). |
| [juneau-bean-common](/docs/topics/JuneauBeanCommon) | Shared DTO base classes and utilities used across juneau-bean modules. |
| [juneau-bean-hal](/docs/topics/JuneauBeanHal) | DTOs for Hypertext Application Language (HAL). |
| [juneau-bean-html5](/docs/topics/JuneauBeanHtml5) | DTOs for HTML5 elements. |
| [juneau-bean-jsonapi](/docs/topics/JuneauBeanJsonApi) | DTOs for the JSON:API specification. |
| [juneau-bean-jsonpatch](/docs/topics/JuneauBeanJsonPatch) | DTOs for JSON Patch (RFC 6902). |
| [juneau-bean-jsonrpc](/docs/topics/JuneauBeanJsonRpc) | Revision-neutral JSON-RPC 2.0 envelope DTOs (`JsonRpcRequest`/`JsonRpcResponse`/`JsonRpcError`/`McpException`) — the core the two `juneau-bean-mcp-v2*` adapter rows below are built on. |
| [juneau-bean-jsonschema](/docs/topics/JuneauBeanJsonSchema) | DTOs for JSON Schema. |
| [juneau-bean-mcp-v20250618](/docs/topics/JuneauBeanMcp) | Adapter on `juneau-bean-jsonrpc` above: DTOs for the MCP `2025-06-18` wire format — tool/prompt/resource descriptors, capability beans, and polymorphic content types. |
| [juneau-bean-mcp-v20260728](/docs/topics/JuneauBeanMcp#mcp-2026-07-28-cache-hints-resource-templates) | Adapter on `juneau-bean-jsonrpc` above: DTOs for the MCP `2026-07-28` wire format — adds cache hints and resource templates on top of the 2025-06-18 baseline. |
| [juneau-bean-openapi-v3](/docs/topics/JuneauBeanOpenApi3) | DTOs for the OpenAPI v3 specification. |
| [juneau-bean-rfc7807](/docs/topics/JuneauBeanRfc7807) | DTOs for RFC 7807 Problem Details. |
| [juneau-bean-swagger-v2](/docs/topics/JuneauBeanSwagger2) | DTOs for the Swagger/OpenAPI v2 specification. |
| **juneau-rest** | |
| [juneau-rest-common](/docs/topics/JuneauRestCommon) | Shared types and utilities for the REST server and client stacks. |
| [juneau-rest-common-classic](/docs/topics/JuneauRestCommon) | Apache HttpClient 4.5–compatible HTTP type facades (`org.apache.juneau.http.classic.*`) for the classic REST client/server stack. |
| [juneau-rest-server](/docs/topics/JuneauRestServer) | Annotation-driven REST server with automatic OpenAPI/Swagger docs and content negotiation. |
| [juneau-rest-server-springboot](/docs/topics/JuneauRestServerSpringboot) | Spring Boot auto-configuration for juneau-rest-server. |
| [juneau-rest-server-mcp](/docs/topics/JuneauRestServerMcp) | Revision-neutral Model Context Protocol (MCP) server dispatch core — the two rows below are its per-revision adapters. |
| [juneau-rest-server-mcp-v20250618](/docs/topics/JuneauRestServerMcp#getting-started) | MCP `2025-06-18` protocol adapter for juneau-rest-server-mcp above. |
| [juneau-rest-server-mcp-v20260728](/docs/topics/JuneauRestServerMcp#configuring-the-endpoint-mcpoptions-mcp-2026-07-28) | MCP `2026-07-28` protocol adapter for juneau-rest-server-mcp above — adds OAuth 2.1 resource-server support, cache hints, and multi-round-trip requests. |
| [juneau-rest-server-auth-jwt](/docs/topics/AuthFilterFramework) | JWT authentication filter. |
| [juneau-rest-server-auth-saml](/docs/topics/SamlAuthSupport) | SAML 2.0 authentication filter. |
| [juneau-rest-server-auth-oauth](/docs/topics/OAuthAuthSupport) | OAuth 2.0 / OIDC authentication and token introspection filter. |
| [juneau-rest-server-auth-oidc-rp](/docs/topics/OidcRelyingParty) | OIDC relying-party support for delegating auth to an external identity provider. |
| [juneau-rest-auth-oauth-flow](/docs/topics/OAuthAuthSupport#client-side-acquire-tokens) | Role-neutral OAuth 2.0 grant-flow helpers (client-credentials, authorization-code+PKCE, refresh-token) shared by the REST server auth modules and the MCP client auth layer. |
| [juneau-rest-server-metrics-micrometer](/docs/topics/RestServerMicrometerMetrics) | Micrometer integration for REST server metrics. |
| [juneau-rest-server-tracing-otel](/docs/topics/RestServerOtelTracing) | OpenTelemetry tracing integration for REST server. |
| [juneau-rest-server-management-logging](/docs/topics/RestServerManagementLogging) | Management and logging surface — dynamic log-level control, log tail, and audit logging. |
| [juneau-rest-server-datatables](/docs/topics/Converters#datatables-server-side-processing) | DataTables server-side-processing query protocol adapter (opt-in). |
| [juneau-rest-server-views](/docs/topics/RichTableViews) | Rich table-view toolkit: declarative view definitions plus the browser runtime that renders them (opt-in). |
| [juneau-rest-server-views-markdown](/docs/topics/MarkdownViewRendering) | Optional Markdown-to-HTML rendering for juneau-rest-server-views, with a pluggable parser SPI and a commonmark-java default. |
| [juneau-rest-server-widgets](/docs/topics/DeclarativeWidgets) | Reusable widget primitives (`ActionBar` and the shared widget contract) used by the views and console modules. |
| [juneau-rest-server-console-ui](/docs/topics/AdminConsoleChrome) | Themeable, engine-agnostic admin-console chrome — themes, tags, `chrome.css`, and the console chrome mixin (opt-in). |
| [juneau-rest-server-console-ui-freemarker](/docs/topics/AdminConsoleChrome#freemarker-directive-reference) | FreeMarker delivery for juneau-rest-server-console-ui above — base template, `<@tag>` macro, and shared page and card directives. |
| [juneau-rest-server-console-ui-freemarker-tests](https://github.com/apache/juneau/tree/master/juneau-rest/juneau-rest-server-console-ui-freemarker-tests) | Unit tests for juneau-rest-server-console-ui-freemarker above (build-only; not published). |
| [juneau-run-report](/docs/topics/RunViewRegion) | Run-view events and test-report readers (Surefire and JUnit XML, Jest JSON, Playwright JSON); no dependency on the REST server. |
| [juneau-rest-server-terminal](/docs/topics/TerminalRegion) | Terminal region: a command's raw output rendered by xterm.js, fed by a byte-offset endpoint (opt-in). |
| [juneau-rest-server-terminal-bus](/docs/topics/ConsoleMessageBus#terminals-on-the-bus) | Bridge that connects terminal input to the server message bus (opt-in). |
| [juneau-rest-server-bus](/docs/topics/ConsoleMessageBus) | Server half of the console message bus — `ServerBus`, `BusPolicy`, and the session POST and SSE stream (opt-in). |
| [juneau-rest-server-bus-websocket](/docs/topics/ConsoleMessageBus#the-websocket-transport) | Jakarta WebSocket transport for juneau-rest-server-bus above; the servlet container supplies the WebSocket implementation. |
| [juneau-rest-server-rdf](/docs/topics/RestServerRdf) | Predefined REST resource config adding RDF (Jena) marshalling support. |
| [juneau-rest-server-reactive](/docs/topics/RestServerReactive) | Reactive (Project Reactor / RxJava) endpoint support. |
| [juneau-rest-server-reactive-reactor](/docs/topics/RestServerReactive) | Project Reactor / RxJava 3 / Reactive-Streams `Publisher` adapters layered on juneau-rest-server-reactive. |
| [juneau-rest-server-view-jsp](/docs/topics/JspViewSupport) | JSP template view support. |
| [juneau-rest-server-view-thymeleaf](/docs/topics/ThymeleafViewSupport) | Thymeleaf template view support. |
| [juneau-rest-server-view-mustache](/docs/topics/MustacheViewSupport) | Mustache template view support. |
| [juneau-rest-server-view-freemarker](/docs/topics/FreemarkerViewSupport) | Freemarker template view support. |
| [juneau-rest-client](/docs/topics/NextGenRestClient) | Canonical, transport-agnostic REST client with POJO mapping and type-safe proxy interface generation (beta). |
| [juneau-rest-client-classic](/docs/topics/JuneauRestClient) | Apache HttpClient 4.5–based classic REST client — the pre-10.0 `RestClient` implementation, retained for binary stability. |
| [juneau-rest-client-apache-httpclient-45](/docs/topics/RestClientApacheHttpClient45) | Adapter for Apache HttpClient 4.5.x. |
| [juneau-rest-client-apache-httpclient-50](/docs/topics/RestClientApacheHttpClient50) | Adapter for Apache HttpClient 5.x. |
| [juneau-rest-client-okhttp](/docs/topics/RestClientOkHttp) | Adapter for OkHttp. |
| [juneau-rest-client-jetty](/docs/topics/RestClientJetty) | Adapter for Jetty HTTP client. |
| [juneau-rest-client-mcp](/docs/topics/JuneauRestClientMcp) | Revision-neutral Model Context Protocol (MCP) client core — the two rows below are its per-revision adapters. |
| [juneau-rest-client-mcp-v20250618](/docs/topics/JuneauRestClientMcp#revision-binding) | MCP `2025-06-18` protocol adapter for juneau-rest-client-mcp above. |
| [juneau-rest-client-mcp-v20260728](/docs/topics/JuneauRestClientMcp#elicitation-mcp-2026-07-28-sep-2322) | MCP `2026-07-28` protocol adapter for juneau-rest-client-mcp above — adds elicitation, OAuth 2.1 auth support, and response caching. |
| [juneau-rest-client-mcp-auth](/docs/topics/JuneauRestClientMcp#oauth-21-authorization-juneau-rest-client-mcp-auth) | OAuth 2.1 client-side authorization support for juneau-rest-client-mcp (DCR, PKCE, protected-resource metadata discovery). |
| [juneau-rest-mock](/docs/topics/JuneauRestMock) | Serverless REST mock client for unit testing without a servlet container. |
| **juneau-microservice** | |
| [juneau-microservice](/docs/topics/JuneauMicroservice) | Core microservice framework with lifecycle management and REST API wiring. |
| [juneau-microservice-jetty](/docs/topics/JuneauMicroserviceJetty) | Jetty-embedded microservice launcher. |
| [juneau-microservice-tomcat](/docs/topics/JuneauMicroserviceTomcat) | Tomcat-embedded microservice launcher. |
| [juneau-microservice-test](/docs/topics/MicroserviceTesting) | JUnit 5 `@MicroserviceTest` extension that boots a whole microservice on an ephemeral port for integration tests, with `@TestBean` mock injection. |
| [juneau-microservice-examples](https://github.com/apache/juneau/tree/master/juneau-microservice/juneau-microservice-examples) | Example and demo REST resources for the microservice (not shipped as a framework artifact). |
| **juneau-sc** | |
| [juneau-sc-server](/docs/topics/ScServerOverview) | Centralized configuration server exposing juneau-config files over REST. |
| **juneau-secret-macos-keychain** | |
| [juneau-secret-macos-keychain](/docs/topics/JuneauCommonsSettings#os-keychain-backed-store) | Opt-in module providing a macOS `security`-CLI-backed `KeychainSecretStore` implementation of the `SecretStore` SPI. |
| **juneau-run** | |
| [juneau-run](/docs/topics/JuneauRun) | Build tooling: the `juneau_run.py` wrapper that adds run markers and board output to Maven, pytest, Playwright, Gradle, and npm runs. Attached as `.py` and `.zip` artifacts; not a library dependency. |
| **juneau-shaded** | |
| [juneau-shaded-core](/docs/topics/JuneauShadedCore) | Shaded uber-jar of the juneau-core modules for zero-conflict dependency management. |
| [juneau-shaded-rest-client](/docs/topics/JuneauShadedRestClient) | Shaded uber-jar of juneau-rest-client and its dependencies. |
| [juneau-shaded-rest-server](/docs/topics/JuneauShadedRestServer) | Shaded uber-jar of juneau-rest-server and its dependencies. |
| [juneau-shaded-rest-server-springboot](/docs/topics/JuneauShadedRestServerSpringboot) | Shaded uber-jar of juneau-rest-server-springboot and its dependencies. |
| [juneau-shaded-all](/docs/topics/JuneauShadedAll) | All-in-one shaded uber-jar of the full Juneau ecosystem. |
| **juneau-bundles** | |
| [juneau-microservice-jetty-bundle](/docs/topics/DependencyManagement#juneau-microservice-jetty-bundle) | Ready-to-run Jetty microservice deployment bundle. |
| [juneau-microservice-tomcat-bundle](/docs/topics/DependencyManagement#juneau-microservice-tomcat-bundle) | Ready-to-run Tomcat microservice deployment bundle. |
| [juneau-springboot-bundle](/docs/topics/DependencyManagement#juneau-springboot-bundle) | Spring Boot application bundle with REST server included. |
| [juneau-observability-otlp-bundle](/docs/topics/DependencyManagement#juneau-observability-otlp-bundle) | Bundle combining OpenTelemetry and Micrometer observability modules. |
| **juneau-bom** | |
| [juneau-bom](/docs/topics/DependencyManagement#the-bom) | Bill of Materials POM: import it and drop explicit versions from your Juneau dependencies. |
| **juneau-examples** and **juneau-petstore** | |
| [juneau-examples-core](/docs/topics/JuneauExamples) | Standalone examples demonstrating core marshalling and configuration APIs. |
| [juneau-examples-mcp](/docs/topics/JuneauMcpRecipes#full-example-juneau-examples-mcp) | Runnable MCP example app pairing a server and client walkthrough, including a fully offline OAuth 2.1–secured variant. |
| [juneau-petstore](/docs/topics/JuneauPetstore) | Sample Petstore REST application available as Jetty and Spring Boot variants (aggregator for the three rows below). |
| [juneau-petstore-core](/docs/topics/JuneauPetstore#module-layout) | Petstore shared domain model and REST resources. |
| [juneau-petstore-jetty](/docs/topics/JuneauPetstore#module-layout) | Petstore Microservice (Jetty) deployment. |
| [juneau-petstore-springboot](/docs/topics/JuneauPetstore#module-layout) | Petstore Spring Boot deployment. |
| **Build-only** | |
| [juneau-integration-tests](https://github.com/apache/juneau/tree/master/juneau-integration-tests) | Cross-module and `MockRestClient`-driven integration test suite. |
| [juneau-distrib](https://github.com/apache/juneau/tree/master/juneau-distrib) | Distribution artifacts and packaging. |

Questions via email to <a href="mailto:dev@juneau.apache.org?Subject=Apache%20Juneau%20question" target="_blank">dev@juneau.apache.org</a> are always welcome.

Juneau is packed with features that may not be obvious at first. Users are encouraged to ask for code reviews by providing links to specific source files such as through GitHub. Not only can we help you with feedback, but it helps us understand usage patterns to further improve the product.


---

## Quick Links

### Documentation & Reports
- <a href="/site/apidocs/" target="_blank">Javadocs</a> - API documentation
- <a href="/site/" target="_blank">Maven Site</a> - Complete project reports
- <a href="/site/xref/" target="_blank">Source Cross-Reference</a> - Browsable source code
- <a href="/site/surefire.html" target="_blank">Test Reports</a> - Unit test results
- <a href="/site/jacoco-aggregate/" target="_blank">Code Coverage</a> - Test coverage reports
