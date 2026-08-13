<!--
 ***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
 * with the License.  You may obtain a copy of the License at                                                              *
 *                                                                                                                         *
 *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
 *                                                                                                                         *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
 * specific language governing permissions and limitations under the License.                                              *
 ***************************************************************************************************************************
-->

> **📢 Documentation Update**  
> This README has been updated to reflect our new Docusaurus-based documentation site. For the most current documentation, please visit the [official Apache Juneau website](https://juneau.apache.org/).

# Apache Juneau

[![Java CI](https://github.com/apache/juneau/actions/workflows/maven.yml/badge.svg)](https://github.com/apache/juneau/actions/workflows/maven.yml)
[![CodeQL](https://github.com/apache/juneau/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/apache/juneau/actions/workflows/codeql-analysis.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=apache_juneau&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=apache_juneau)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=apache_juneau&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=apache_juneau)

Apache Juneau™ is a single cohesive Java ecosystem consisting of a comprehensive toolkit for marshalling POJOs to a wide variety of content types using a common framework, along with universal REST server and client APIs for creating Swagger-based self-documenting REST interfaces.

> **⚠️ Upgrading from 9.x?** The project is currently developing the **10.0.0** release, which includes a number of breaking changes (the public `ObjectRest` class removed, `SerializerSet`/`ParserSet` lookups now returning `Optional`, the next-gen `RestClient` no longer implicitly defaulting to JSON, the `juneau-assertions`/`juneau-bct`/`juneau-junit5` modules merged into a single `juneau-test` artifact, and the legacy `juneau-my-jetty-microservice`/`juneau-examples-rest` modules removed, among others). See the **[10.0.0 Migration Guide](https://juneau.apache.org/docs/topics/V10MigrationGuide)** for the full list before upgrading.

## Key Features

* **Universal Serialization** - Marshal POJOs to JSON, XML, HTML, URL-Encoding, UON, MessagePack, CSV, and more
* **REST Services** - Create self-documenting REST interfaces with automatic Swagger/OpenAPI generation
* **Microservices** - Build lightweight, standalone microservices with embedded Jetty
* **Configuration Management** - Sophisticated configuration file API with variable resolution
* **Fluent Assertions** - Powerful testing framework with fluent-style assertions
* **Type Conversion** - Lightweight, MarshallingContext-free converter framework with caching and broad type support
* **Large-Dataset Streaming** - BeanSupplier/BeanConsumer/BeanChannel APIs for serializing and parsing large datasets without loading all elements into memory; supports direct database integration via lifecycle methods (begin/acceptThrows/onError/complete)
* **MCP (Model Context Protocol) Support** - First-party server and client support for exposing REST resources as MCP tools/prompts/resources, or consuming MCP servers from Java, for both the `2025-06-18` and `2026-07-28` protocol revisions
* **Zero Dependencies** - Core marshalling requires no external dependencies

## MCP (Model Context Protocol) Support

Apache Juneau ships first-party [MCP](https://modelcontextprotocol.io/) integration, built the same way as the rest of the framework: annotation-driven, POJO-based, no magic. The module family is split into revision-neutral **cores** plus thin **adapters** per protocol revision, so a `2025-06-18`-only deployment never pulls in `2026-07-28`-only dependencies (OAuth 2.1, JWT, reactive-streams SSE, etc.):

* **juneau-bean-jsonrpc** - revision-neutral JSON-RPC 2.0 envelope beans, with `juneau-bean-mcp-v20250618` / `juneau-bean-mcp-v20260728` adapters for each revision's wire beans.
* **juneau-rest-server-mcp** - revision-neutral server dispatch core, with `juneau-rest-server-mcp-v20250618` / `juneau-rest-server-mcp-v20260728` adapters for exposing tools, prompts, and resources (dedicated servlet or drop-in mixin, plain or Spring Boot).
* **juneau-rest-client-mcp** - revision-neutral client core, with `juneau-rest-client-mcp-v20250618` / `juneau-rest-client-mcp-v20260728` typed client facades, plus `juneau-rest-client-mcp-auth` for the client-side OAuth 2.1 acquisition flow.
* **juneau-examples-mcp** - a runnable first-party example (notes-service demo, with plain, Spring Boot, and OAuth-secured variants).

The `2026-07-28` revision is a strict superset of `2025-06-18` and is where new capability work (Multi-Round-Trip Requests/elicitation, subscriptions, cache hints, trace-context propagation) lands going forward. See **[MCP (Model Context Protocol)](https://juneau.apache.org/docs/topics/JuneauMcp)** for the quickstart, setup guide, and full API reference.

## Documentation & Resources

### Official Resources
* **[Homepage](https://juneau.apache.org/)** - Official Apache Juneau website
* **[Staged Website](https://juneau.staged.apache.org/)** - Preview of pending website changes
* **[Wiki](https://github.com/apache/juneau/wiki)** - Community documentation and guides
* **[Pet Store App](https://github.com/apache/juneau-petstore)** - Complete example application

### Documentation
* **[Javadocs](https://juneau.apache.org/site/apidocs/)** - Complete API documentation
* **[User Guide](https://juneau.apache.org/docs/topics/JuneauEcosystemOverview)** - Comprehensive framework documentation
* **[Why Choose Juneau?](https://juneau.apache.org/docs/topics/WhyJuneau)** - Benefits and comparisons with alternatives
* **[Framework Comparisons](https://juneau.apache.org/docs/topics/FrameworkComparisons)** - Compare Juneau with Jackson, Spring Boot, and JAX-RS
* **[Examples](https://juneau.apache.org/docs/topics/JuneauExamplesCore)** - Code examples and tutorials
  * [juneau-examples-core](https://juneau.apache.org/docs/topics/JuneauExamplesCore) - Core serialization examples
  * [juneau-petstore](https://juneau.apache.org/docs/topics/JuneauPetstoreOverview) - Canonical petstore showcase application (deployed under both Jetty/Microservice and Spring Boot)
* **[Test Reports](https://juneau.apache.org/site/surefire.html)** - JUnit test execution results
* **[Dependencies](https://juneau.apache.org/site/dependency-info.html)** - Project dependency analysis
* **[Project Reports](https://juneau.apache.org/site/project-reports.html)** - Complete Maven site reports

> **Note:** The documentation is automatically updated and provides the most current project information.

## Common Use Cases

Apache Juneau™ excels in the following scenarios:

* **Marshalling Java beans to a variety of languages using zero dependencies** - Serialize POJOs to JSON, XML, HTML, URL-Encoding, UON, OpenAPI, PlainText, CSV, SOAP, MessagePack, and RDF formats with minimal setup
* **Creation of self-documenting Bean-based REST APIs for SpringBoot and Jetty applications** - Build REST services with automatic Swagger documentation, content negotiation, and POJO-based request/response handling
* **Creation of Java interface proxies on top of existing REST APIs** - Generate type-safe client proxies that make REST calls feel like local method invocations
* **Powerful INI-based configuration files** - Manage application configuration with support for POJOs, arrays, collections, binary data, and real-time file watching
* **Serverless unit testing of REST APIs** - Test REST services without servlet containers using MockRestClient for fast, comprehensive testing
* **Microservice development** - Build lightweight microservices with embedded Jetty or Spring Boot integration
* **Data transformation and mapping** - Convert between different data formats and handle complex object hierarchies with swap mechanisms
* **Bean-Centric Testing and fluent-style assertions** - Write readable test assertions with comprehensive validation capabilities using juneau-test
* **Content negotiation and HTTP/2 support** - Handle multiple content types automatically with modern HTTP features

## Getting Started in 5 Minutes

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-shaded-all</artifactId>
    <version>10.0.0-SNAPSHOT</version>
</dependency>
```

> `10.0.0` is currently under development (tracking `-SNAPSHOT` builds) and has not yet been released. See the [Downloads](https://juneau.apache.org/downloads) page for the latest released version.

### 2. Serialize a POJO to JSON

```java
import org.apache.juneau.json.*;

public class QuickStart {
    public static void main(String[] args) {
        // Create a simple POJO
        Person person = new Person("John", 30);
        
        // Serialize to JSON
        String json = Json.of(person);
        System.out.println(json);
        // Output: {"name":"John","age":30}
    }
    
    public static class Person {
        public String name;
        public int age;
        
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
```

### 3. Parse JSON back to POJO

```java
// Parse JSON back to POJO
Person parsed = Json.to(json, Person.class);
System.out.println(parsed.name); // Output: John
```

### 4. Create a REST API

```java
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.servlet.*;

@Rest(
    title="Hello World API",
    description="Simple REST API example"
)
public class HelloWorldResource extends BasicRestServlet {
    
    @RestGet("/hello/{name}")
    public String sayHello(@Path String name) {
        return "Hello " + name + "!";
    }
    
    @RestGet("/person")
    public Person getPerson() {
        return new Person("Jane", 25);
    }
}
```

### 5. Test Your API

```java
import org.apache.juneau.rest.mock.*;

public class ApiTest {
    @Test
    public void testHello() throws Exception {
        String response = MockRestClient
            .create(HelloWorldResource.class)
            .json5()
            .build()
            .get("/hello/World")
            .run()
            .assertStatus().is(200)
            .getContent().asString();
        
        assertEquals("Hello World!", response);
    }
}
```

**That's it!** You now have:
- JSON serialization/parsing
- A working REST API with automatic content negotiation
- Built-in testing support
- Zero external dependencies

### Next Steps
- **Multi-format support**: Try XML, HTML, or other formats
- **Configuration files**: Use `juneau-config` for INI-style configs
- **Spring Boot integration**: Add `juneau-rest-server-springboot`
- **Examples**: Check out our [comprehensive examples](/docs/topics/JuneauExamplesCore)

## More Examples

### XML Serialization

```java
import org.apache.juneau.xml.*;

// Serialize to XML
String xml = Xml.of(person);
System.out.println(xml);
// Output: <object><name>John</name><age>30</age></object>

// Parse XML back to POJO
Person parsed = Xml.to(xml, Person.class);
```

### HTML Serialization

```java
import org.apache.juneau.html.*;

// Serialize to HTML table
String html = Html.of(person);
System.out.println(html);
// Output: <table><tr><th>name</th><td>John</td></tr><tr><th>age</th><td>30</td></tr></table>
```

### Configuration Files

```java
import org.apache.juneau.config.*;

// Create configuration
Config config = Config.create()
    .set("database.host", "localhost")
    .set("database.port", 5432)
    .set("features.enabled", true)
    .build();

// Read configuration
String host = config.get("database.host");
int port = config.get("database.port", Integer.class);
boolean enabled = config.get("features.enabled", Boolean.class);
```

### REST Client Proxy

```java
import org.apache.juneau.rest.client.*;
import org.apache.juneau.http.*;

// Define REST interface
@Remote("http://api.example.com")
public interface UserService {
    @Get("/users/{id}")
    User getUser(@Path String id);
    
    @Post("/users")
    User createUser(@Body User user);
}

// Use as regular Java interface
UserService service = RestClient.create().build().getRemote(UserService.class);
User user = service.getUser("123");
```

### Testing REST APIs

```java
import org.apache.juneau.rest.mock.*;

// Test without starting a server
@Test
public void testUserAPI() throws Exception {
    String response = MockRestClient
        .create(UserResource.class)
        .json5()
        .build()
        .get("/users/123")
        .run()
        .assertStatus().is(200)
        .getContent().asString();
    
    assertThat(response).contains("John");
}
```

### Microservice with Jetty

```java
import org.apache.juneau.microservice.*;

// Create microservice
Microservice microservice = Microservice.create()
    .servlet(UserResource.class)
    .port(8080)
    .build();

// Start server
microservice.start();
```

## Description

Apache Juneau™ is a single cohesive Java ecosystem consisting of the following parts, grouped by aggregator module. For the complete, always-current per-artifact list, see the [Juneau Ecosystem Overview](https://juneau.apache.org/docs/topics/JuneauEcosystemOverview).

* **juneau-core** - Core marshalling and support APIs, with no external dependencies unless noted:
  * **juneau-commons** - Shared low-level utilities used across the ecosystem, including the Simple Variable Language (SVL) for dynamic string processing.
  * **juneau-marshall**	- A universal toolkit for marshalling POJOs to a variety of content types using a common framework with no external library dependencies.
  * **juneau-marshall-rdf**	- Additional support for various RDF languages.
  * **juneau-config**	- A sophisticated configuration file API.
  * **juneau-test** - Unified test-support API combining fluent-style assertions, Bean-Centric Testing, and JUnit 5 extensions (replaces the former `juneau-assertions`/`juneau-bct`/`juneau-junit5` artifacts).
* **juneau-bean** - Predefined serializable beans: **juneau-bean-atom**, **juneau-bean-common**, **juneau-bean-hal**, **juneau-bean-html5**, **juneau-bean-jsonapi**, **juneau-bean-jsonpatch**, **juneau-bean-jsonrpc**, **juneau-bean-jsonschema**, **juneau-bean-openapi-v3**, **juneau-bean-rfc7807**, **juneau-bean-swagger-v2** - such as HTML5, Swagger/OpenAPI, ATOM, HAL, JSON:API, JSON Patch, and RFC 7807 Problem Details. (See [MCP Support](#mcp-model-context-protocol-support) above for the `juneau-bean-mcp-*` adapters.)
* **juneau-rest** - REST server and client APIs:
  * **juneau-rest-common** / **juneau-rest-common-classic** - REST APIs common to client and server side (next-gen vs. classic client stack).
  * **juneau-rest-server**	- A universal REST server API for creating Swagger-based self-documenting REST interfaces using POJOs, simply deployed as one or more top-level servlets in any Servlet 3.1.0+ container. Includes Spring Boot and JAX-RS integration support.
  * **juneau-rest-server-rdf** - RDF support for the REST server.
  * **juneau-rest-server-springboot** - Spring Boot integration for juneau-rest-server.
  * **juneau-rest-server-auth-jwt, -saml, -oauth, -oidc-rp** and **juneau-rest-auth-oauth-flow** - Authentication/authorization add-ons (JWT, SAML, OAuth, OpenID Connect RP).
  * **juneau-rest-server-metrics-micrometer, -tracing-otel, -management-logging** - Observability add-ons (Micrometer metrics, OpenTelemetry tracing, request/response logging).
  * **juneau-rest-server-datatables** - Server-side processing adapter (plus browser-side helpers) for [DataTables](https://datatables.net/).
  * **juneau-rest-server-reactive, -reactive-reactor** - Reactive-streams (SSE) response support.
  * **juneau-rest-server-view-jsp, -thymeleaf, -mustache, -freemarker** - View-engine add-ons.
  * **juneau-rest-client** / **juneau-rest-client-classic** - A universal REST client API for interacting with Juneau or 3rd-party REST interfaces using POJOs and proxy interfaces (next-gen vs. classic client stack), with **juneau-rest-client-apache-httpclient-45/-50**, **juneau-rest-client-jetty**, and **juneau-rest-client-okhttp** transport backends.
  * **juneau-rest-mock** - REST testing API.
  * MCP client/server modules - see [MCP Support](#mcp-model-context-protocol-support) above.
* **juneau-microservice** - **juneau-microservice**, **juneau-microservice-jetty**, **juneau-microservice-tomcat**, **juneau-microservice-test**, **juneau-microservice-examples** - Lightweight standalone microservice APIs (Jetty and Tomcat), plus a JUnit 5 test harness.
* **juneau-sc** - **juneau-sc-server** - Git-backed source-control configuration server integration (`GitControl`, etc.).
* **juneau-secret-keychain** - Opt-in `SecretStore` implementation backed by the macOS `security` keychain CLI (implements the `SecretStore` SPI in `juneau-commons`).
* **juneau-examples** - **juneau-examples-core** - Core code examples; **juneau-examples-mcp** - Runnable MCP example (notes-service demo).
* **juneau-petstore** - **juneau-petstore-core** - Shared petstore domain + REST resources; **juneau-petstore-jetty** - Jetty/Microservice deployment; **juneau-petstore-springboot** - Spring Boot deployment.
* **juneau-shaded** - Shaded (uber) JARs combining multiple Juneau modules for simplified dependency management, especially useful for Bazel builds: **juneau-shaded-core**, **juneau-shaded-rest-client**, **juneau-shaded-rest-server**, **juneau-shaded-rest-server-springboot**, and **juneau-shaded-all**.
* **juneau-bundles** - Curated dependency bundles pulling a coherent module set per deployment shape: **juneau-microservice-jetty-bundle**, **juneau-microservice-tomcat-bundle**, **juneau-springboot-bundle**, **juneau-observability-otlp-bundle**.
* **juneau-bom** - Published Maven Bill-of-Materials for version-aligning Juneau dependencies.
* **juneau-distrib** - Release distribution assembly.

Questions via email to dev@juneau.apache.org are always welcome.

Juneau is packed with features that may not be obvious at first. Users are encouraged to ask for code reviews by providing links to specific source files such as through GitHub. Not only can we help you with feedback, but it helps us understand usage patterns to further improve the product.

## Features
* Fast memory-efficient serialization.
* Fast, safe, memory-efficient parsing. Parsers are not susceptible to deserialization attacks.
* KISS is our mantra! No auto-wiring. No code generation. No dependency injection. Just add it to your classpath and use it. Extremely simple unit testing!
* Enjoyable to use
* Tiny - ~1MB
* Exhaustively tested
* Lots of up-to-date documentation and examples
* Minimal library dependencies:
   * juneau-commons, juneau-marshall, juneau-bean-atom, juneau-bean-common, juneau-bean-html5, juneau-bean-jsonschema, juneau-bean-openapi-v3, juneau-config - No external dependencies. Entirely self-contained.
   * juneau-marshall-rdf - Optional RDF support. Requires Apache Jena 5.6.0+.
   * juneau-test - Requires opentest4j (JUnit Jupiter is a provided-scope dependency, supplied by your test runtime).
   * juneau-rest-server - Any Servlet 3.1.0+ container.
   * juneau-rest-client - Apache HttpClient 4.5+.
* Built on top of Servlet and Apache HttpClient APIs that allow you to use the newest HTTP/2 features such as request/response multiplexing and server push.

## Repository Structure

This repository uses multiple branches to separate different concerns:

* **`master`** - Contains the main source code for Apache Juneau
* **`docs`** - Contains the Docusaurus-based documentation site
* **`asf-staging`** - Contains the staging/preview version of the website
* **`asf-site`** - Contains the production version of the website

When working with the repository, ensure you're on the correct branch for your task:
- For code changes, work in the `master` branch
- For documentation updates, work in the `docs` branch
- The `asf-staging` and `asf-site` branches are automatically updated during the release process

## Building
Building requires:
* [Apache Maven](https://maven.apache.org/)
* Java 17 is required to build and run.
