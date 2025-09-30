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
> This README has been updated to reflect our new Docusaurus-based documentation site. For the most current documentation, please visit the [official Apache Juneau website](https://juneau.staged.apache.org/).

# Apache Juneau

[![Java CI](https://github.com/apache/juneau/actions/workflows/maven.yml/badge.svg)](https://github.com/apache/juneau/actions/workflows/maven.yml)
[![CodeQL](https://github.com/apache/juneau/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/apache/juneau/actions/workflows/codeql-analysis.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=apache_juneau&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=apache_juneau)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=apache_juneau&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=apache_juneau)

## Documentation & Resources

### Official Resources
* **[Homepage](https://juneau.staged.apache.org/)** - Official Apache Juneau website
* **[Wiki](https://github.com/apache/juneau/wiki)** - Community documentation and guides
* **[Pet Store App](https://github.com/apache/juneau-petstore)** - Complete example application

### Documentation
* **[Javadocs](https://juneau.staged.apache.org/site/apidocs/)** - Complete API documentation
* **[User Guide](https://juneau.staged.apache.org/docs/topics/JuneauEcosystemOverview)** - Comprehensive framework documentation
* **[Why Choose Juneau?](https://juneau.staged.apache.org/docs/topics/WhyJuneau)** - Benefits and comparisons with alternatives
* **[Framework Comparisons](https://juneau.staged.apache.org/docs/topics/FrameworkComparisons)** - Compare Juneau with Jackson, Spring Boot, and JAX-RS
* **[Examples](https://juneau.staged.apache.org/docs/topics/JuneauExamplesCore)** - Code examples and tutorials
  * [juneau-examples-core](https://juneau.staged.apache.org/docs/topics/JuneauExamplesCore) - Core serialization examples
  * [juneau-examples-rest](https://juneau.staged.apache.org/docs/topics/JuneauExamplesRest) - REST API examples
  * [juneau-examples-rest-jetty](https://juneau.staged.apache.org/docs/topics/JuneauExamplesRestJetty) - Jetty microservice examples
  * [juneau-examples-rest-springboot](https://juneau.staged.apache.org/docs/topics/JuneauExamplesRestSpringboot) - Spring Boot examples
* **[Test Reports](https://juneau.staged.apache.org/site/surefire.html)** - JUnit test execution results
* **[Dependencies](https://juneau.staged.apache.org/site/dependency-info.html)** - Project dependency analysis
* **[Project Reports](https://juneau.staged.apache.org/site/project-reports.html)** - Complete Maven site reports

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
* **Bean-Centric Testing and fluent-style assertions** - Write readable test assertions with comprehensive validation capabilities using juneau-bct and juneau-assertions
* **Content negotiation and HTTP/2 support** - Handle multiple content types automatically with modern HTTP features

## Getting Started in 5 Minutes

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-all</artifactId>
    <version>9.1.0</version>
</dependency>
```

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
import org.apache.juneau.rest.annotation.*;
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
import org.apache.juneau.http.annotation.*;

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

Apache Juneau™ is a single cohesive Java ecosystem consisting of the following parts:

* **juneau-marshall**	- A universal toolkit for marshalling POJOs to a variety of content types using a common framework with no external library dependencies.
* **juneau-marshall-rdf**	- Additional support for various RDF languages.
* **juneau-bean-atom, juneau-bean-common, juneau-bean-html5, juneau-bean-jsonschema, juneau-bean-openapi3**	- A variety of predefined serializable beans such as HTML5, Swagger and ATOM.
* **juneau-config**	- A sophisticated configuration file API.
* **juneau-assertions** - Fluent-style assertions API.
* **juneau-bct** - Bean-Centric Testing framework that extends JUnit with streamlined assertion methods for Java objects.
* **juneau-svl** - Simple Variable Language for dynamic string processing.
* **juneau-rest-common** - REST APIs common to client and server side.
* **juneau-rest-server**	- A universal REST server API for creating Swagger-based self-documenting REST interfaces using POJOs, simply deployed as one or more top-level servlets in any Servlet 3.1.0+ container. Includes Spring Boot and JAX-RS integration support.
* **juneau-rest-client** - A universal REST client API for interacting with Juneau or 3rd-party REST interfaces using POJOs and proxy interfaces.
* **juneau-rest-server-springboot** - Spring boot integration for juneau-rest-servlet.
* **juneau-rest-mock** - REST testing API.
* **juneau-microservice-core** - Core microservice API.
* **juneau-microservice-jetty** - Jetty microservice API.
* **juneau-examples-core** - Core code examples.
* **juneau-examples-rest** - REST code examples.
* **juneau-examples-rest-jetty** - Jetty microservice examples.
* **juneau-examples-rest-springboot** - Spring Boot examples.
* **juneau-petstore** - Complete REST application example.
* **juneau-all** - Convenience dependency combining all core Juneau modules. 

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
   * juneau-marshall, juneau-bean-atom, juneau-bean-common, juneau-bean-html5, juneau-bean-jsonschema, juneau-bean-openapi3, juneau-svl, juneau-config - No external dependencies. Entirely self-contained.
   * juneau-marshall-rdf - Optional RDF support. Requires Apache Jena 2.7.1+.
   * juneau-rest-server - Any Servlet 3.1.0+ container.
   * juneau-rest-client - Apache HttpClient 4.5+.
* Built on top of Servlet and Apache HttpClient APIs that allow you to use the newest HTTP/2 features such as request/response multiplexing and server push.

## Building
Building requires:
* [Apache Maven](https://maven.apache.org/)
* Java 17 is required to build and run.
