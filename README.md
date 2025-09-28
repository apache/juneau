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

## 📚 Documentation & Resources

### 🌐 Official Resources
* **[🏠 Homepage](https://juneau.staged.apache.org/)** - Official Apache Juneau website
* **[📖 Wiki](https://github.com/apache/juneau/wiki)** - Community documentation and guides
* **[🎯 Pet Store App](https://github.com/apache/juneau-petstore)** - Complete example application

### 📋 Documentation
* **[📚 Javadocs](https://juneau.staged.apache.org/site/apidocs/)** - Complete API documentation
* **[📖 User Guide](https://juneau.staged.apache.org/docs/topics/JuneauEcosystemOverview)** - Comprehensive framework documentation
* **[⚖️ Framework Comparisons](https://juneau.staged.apache.org/docs/topics/FrameworkComparisons)** - Compare Juneau with Jackson, Spring Boot, and JAX-RS
* **[🔧 Examples](https://juneau.staged.apache.org/docs/topics/JuneauExamplesCore)** - Code examples and tutorials
  * [juneau-examples-core](https://juneau.staged.apache.org/docs/topics/JuneauExamplesCore) - Core serialization examples
  * [juneau-examples-rest](https://juneau.staged.apache.org/docs/topics/JuneauExamplesRest) - REST API examples
  * [juneau-examples-rest-jetty](https://juneau.staged.apache.org/docs/topics/JuneauExamplesRestJetty) - Jetty microservice examples
  * [juneau-examples-rest-springboot](https://juneau.staged.apache.org/docs/topics/JuneauExamplesRestSpringboot) - Spring Boot examples
* **[📊 Test Reports](https://juneau.staged.apache.org/site/surefire.html)** - JUnit test execution results
* **[📦 Dependencies](https://juneau.staged.apache.org/site/dependency-info.html)** - Project dependency analysis
* **[📋 Project Reports](https://juneau.staged.apache.org/site/project-reports.html)** - Complete Maven site reports

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
* **Fluent-style assertions and testing** - Write readable test assertions with comprehensive validation capabilities
* **Content negotiation and HTTP/2 support** - Handle multiple content types automatically with modern HTTP features

## Description

Apache Juneau™ is a single cohesive Java ecosystem consisting of the following parts:

* **juneau-marshall**	- A universal toolkit for marshalling POJOs to a variety of content types using a common framework with no external library dependencies.
* **juneau-marshall-rdf**	- Additional support for various RDF languages.
* **juneau-bean-atom, juneau-bean-common, juneau-bean-html5, juneau-bean-jsonschema, juneau-bean-openapi3**	- A variety of predefined serializable beans such as HTML5, Swagger and ATOM.
* **juneau-config**	- A sophisticated configuration file API.
* **juneau-rest-server**	- A universal REST server API for creating Swagger-based self-documenting REST interfaces using POJOs, simply deployed as one or more top-level servlets in any Servlet 3.1.0+ container. Includes Spring Boot and JAX-RS integration support.
* **juneau-rest-client** - A universal REST client API for interacting with Juneau or 3rd-party REST interfaces using POJOs and proxy interfaces.
* **juneau-rest-springboot** - Spring boot integration for juneau-rest-servlet. 

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
