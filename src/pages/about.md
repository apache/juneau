# About Apache Juneau™

Apache Juneau™ turns Java objects into JSON, XML, HTML, YAML and many other formats, and parses them back, through one
API. REST servers and clients are built on the same model, so the beans you serialize are the beans you serve and
call over HTTP.

## What you can do

### Marshalling

Serialize and parse Java beans as JSON, XML, HTML, YAML, CSV, MessagePack, RDF and more, with no required
dependencies. See [juneau-marshall](/docs/topics/JuneauMarshall).

### REST servers and microservices

Build self-documenting REST APIs from annotated Java classes with [juneau-rest-server](/docs/topics/JuneauRestServer),
and run them as a [microservice](/docs/topics/JuneauMicroservice) on [Jetty](/docs/topics/JuneauMicroserviceJetty) or
[Tomcat](/docs/topics/JuneauMicroserviceTomcat), or inside [Spring Boot](/docs/topics/JuneauRestServerSpringboot).

### REST clients

Call REST APIs with POJO mapping using the [REST client](/docs/topics/NextGenRestClient) or the
[classic client](/docs/topics/JuneauRestClient), and turn a Java interface into type-safe calls with
[remote proxies](/docs/topics/RestProxies).

### Testing

Write compact, bean-aware assertions with [juneau-test](/docs/topics/JuneauTest), and test REST APIs without a servlet
container using [juneau-rest-mock](/docs/topics/JuneauRestMock).

### Configuration

Read INI and YAML configuration files as POJOs, with variable resolution and live file watching, using
[juneau-config](/docs/topics/JuneauConfig).

### Admin consoles and UI

Build a themeable, server-rendered admin console on `juneau-rest-server` with the
[console chrome](/docs/topics/AdminConsoleChrome); [Building a Console Page](/docs/topics/BuildingAConsolePage) is the
end-to-end tutorial. [Rich table views](/docs/topics/RichTableViews) give plain beans search, sort, paging and row
actions. Pages can draw [chart cards](/docs/topics/ChartCards), show multi-step runs in the
[Run View Region](/docs/topics/RunViewRegion) (fed by [juneau-run](/docs/topics/JuneauRun)) and raw command output in the
[Terminal Region](/docs/topics/TerminalRegion), and start long-running work from a
[job-log card](/docs/topics/JobLogCard), [row jobs](/docs/topics/RowJobs) or [async jobs](/docs/topics/RichTableViewsAsyncJobs),
with live updates over the [console message bus](/docs/topics/ConsoleMessageBus).
[Declarative widgets](/docs/topics/DeclarativeWidgets) and [Markdown rendering](/docs/topics/MarkdownViewRendering)
cover forms and text, and template views are available for [JSP](/docs/topics/JspViewSupport),
[Thymeleaf](/docs/topics/ThymeleafViewSupport), [Mustache](/docs/topics/MustacheViewSupport) and
[FreeMarker](/docs/topics/FreemarkerViewSupport).

### AI and MCP

Expose tools, prompts and resources to AI assistants from a REST resource with
[juneau-rest-server-mcp](/docs/topics/JuneauRestServerMcp), built on the [MCP wire beans](/docs/topics/JuneauBeanMcp).
Start with the [MCP Quickstart](/docs/topics/JuneauMcpQuickstart), then
[choose your MCP setup](/docs/topics/JuneauMcpChoosingSetup).

## See it running

The [juneau-petstore](/docs/topics/JuneauPetstore) sample runs on Jetty and Spring Boot with an admin console that uses
the pieces above, and [juneau-run](/docs/topics/JuneauRun) turns Maven, npm and other build output into the run views
the console shows.

<a href="/docs/topics/JuneauPetstore"><img src="/topics/JuneauPetstore.Tour.1.webp" alt="The Juneau Petstore admin console" width="2560" height="1800" loading="lazy" style={{height: "auto", maxWidth: "100%"}} /></a>

## When to Choose Juneau

Not sure if Juneau is right for your project? Check out our detailed [Why Choose Juneau?](/docs/topics/WhyJuneau) page for a comprehensive comparison with alternatives, or see our [Framework Comparisons](/docs/topics/FrameworkComparisons) page for detailed technical comparisons.

**Quick summary:** Choose Juneau when you need multi-format serialization, REST APIs with automatic content negotiation, or want an integrated solution with minimal dependencies. Also consider it when the same Java service needs a server-rendered admin console ([console chrome](/docs/topics/AdminConsoleChrome) and [rich table views](/docs/topics/RichTableViews)) or an [MCP](/docs/topics/JuneauRestServerMcp) endpoint, without adding a separate front-end stack. Upgrading from 9.x? See the [V10.0 Migration Guide](/docs/topics/V10MigrationGuide).

## Pick your module

| Module | Use it for |
|--------|------------|
| [juneau-marshall](/docs/topics/JuneauMarshall) | Serializers and parsers for JSON, XML, HTML, YAML and more. |
| [juneau-config](/docs/topics/JuneauConfig) | INI and YAML configuration files as POJOs. |
| [juneau-rest-server](/docs/topics/JuneauRestServer) | Annotation-based REST servers with content negotiation and OpenAPI docs. |
| [juneau-rest-client](/docs/topics/NextGenRestClient) | REST clients with POJO mapping and remote proxies. |
| [juneau-microservice](/docs/topics/JuneauMicroservice) | A runnable REST microservice on Jetty or Tomcat. |
| [juneau-test](/docs/topics/JuneauTest) / [juneau-rest-mock](/docs/topics/JuneauRestMock) | Bean-aware assertions and serverless REST tests. |
| [juneau-rest-server-console-ui](/docs/topics/AdminConsoleChrome) | Server-rendered admin console chrome. |
| [juneau-rest-server-mcp](/docs/topics/JuneauRestServerMcp) | MCP servers for AI assistants. |

For every module, including the bean DTOs, shaded jars and bundles, see the
[Juneau Ecosystem](/docs/topics/JuneauEcosystemOverview) overview.

Questions via email to [dev@juneau.apache.org](mailto:dev@juneau.apache.org?Subject=Apache%20Juneau%20question) are always welcome.

Juneau is packed with features that may not be obvious at first. Users are encouraged to ask for code reviews by providing links to specific source files such as through GitHub. Not only can we help you with feedback, but it helps us understand usage patterns to further improve the product.


---

## Quick Links

### Documentation & Reports
- <a href="/site/apidocs/" target="_blank">Javadocs</a> - API documentation
- <a href="/site/" target="_blank">Maven Site</a> - Complete project reports
- <a href="/site/xref/" target="_blank">Source Cross-Reference</a> - Browsable source code
- <a href="/site/surefire.html" target="_blank">Test Reports</a> - Unit test results
