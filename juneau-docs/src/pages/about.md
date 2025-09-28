# About Apache Juneau™

Apache Juneau™ is a single cohesive Java ecosystem for marshalling Java objects to a wide variety of language types and 
creating annotation-based REST end-to-end server and client APIs.

## Common Use Cases

Juneau excels in the following scenarios:

* **Marshalling Java beans to a variety of languages using zero dependencies** - Serialize POJOs to JSON, XML, HTML, URL-Encoding, UON, OpenAPI, PlainText, CSV, SOAP, MessagePack, and RDF formats with minimal setup
* **Creation of self-documenting Bean-based REST APIs for SpringBoot and Jetty applications** - Build REST services with automatic Swagger documentation, content negotiation, and POJO-based request/response handling
* **Creation of Java interface proxies on top of existing REST APIs** - Generate type-safe client proxies that make REST calls feel like local method invocations
* **Powerful INI-based configuration files** - Manage application configuration with support for POJOs, arrays, collections, binary data, and real-time file watching
* **Serverless unit testing of REST APIs** - Test REST services without servlet containers using MockRestClient for fast, comprehensive testing
* **Microservice development** - Build lightweight microservices with embedded Jetty or Spring Boot integration
* **Data transformation and mapping** - Convert between different data formats and handle complex object hierarchies with swap mechanisms
* **Bean-Centric Testing and fluent-style assertions** - Write readable test assertions with comprehensive validation capabilities using juneau-bct and juneau-assertions
* **Content negotiation and HTTP/2 support** - Handle multiple content types automatically with modern HTTP features

## When to Choose Juneau

Not sure if Juneau is right for your project? Check out our detailed [Why Choose Juneau?](/docs/topics/WhyJuneau) page for a comprehensive comparison with alternatives, or see our [Framework Comparisons](/docs/topics/FrameworkComparisons) page for detailed technical comparisons.

**Quick summary:** Choose Juneau when you need multi-format serialization, REST APIs with automatic content negotiation, or want an integrated solution with minimal dependencies.

## Ecosystem

The Juneau ecosystem consists of the following parts:

| Group | Component | Description |
|-------|-----------|-------------|
| **juneau-core** | [juneau-marshall](/docs/topics/JuneauMarshallBasics) | POJO marshalling support for JSON, JSON5, XML, HTML, URL-encoding, UON, MessagePack, and CSV using no external module dependencies. |
| | [juneau-marshall-rdf](/docs/topics/Module-juneau-marshall-rdf) | Extended marshalling support for RDF/XML, N3, N-Tuple, and Turtle. |
| | [juneau-bean](/docs/topics/JuneauBeanBasics) | Data Transfer Objects for HTML5, ATOM, JSON Schema, and OpenAPI |
| | [juneau-config](/docs/topics/JuneauConfigBasics) | A sophisticated configuration file API. |
| | [juneau-assertions](/docs/topics/JuneauAssertionBasics) | A fluent assertions API. |
| | [juneau-bct](/docs/topics/JuneauBctBasics) | Bean-Centric Testing framework that extends JUnit with streamlined assertion methods for Java objects. |
| **juneau-rest** | [juneau-rest-server](/docs/topics/JuneauRestServerBasics) | A universal REST server API for creating Swagger-based self-documenting REST interfaces using POJOs, simply deployed as one or more top-level servlets in any Servlet 3.1.0+ container. |
| | [juneau-rest-server-springboot](/docs/topics/JuneauRestServerSpringbootBasics) | Spring Boot integration. |
| | [juneau-rest-client](/docs/topics/JuneauRestClientBasics) | A universal REST client API for interacting with Juneau or 3rd-party REST interfaces using POJOs and proxy interfaces. |

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
