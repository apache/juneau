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

# Apache Juneau

* [Homepage](https://juneau.apache.org/)
* [Wiki](https://github.com/apache/juneau/wiki)
* [Javadocs](https://juneau.apache.org/site/apidocs-9.0.0/index.html)
* [Documentation](https://juneau.apache.org/site/apidocs-9.0.0/overview-summary.html#overview.description)
* [Pet Store App](https://github.com/apache/juneau-petstore)

## Description

Apache Juneau™ is a single cohesive Java ecosystem consisting of the following parts:

* **juneau-marshall**	- A universal toolkit for marshalling POJOs to a variety of content types using a common framework with no external library dependencies.
* **juneau-marshall-rdf**	- Additional support for various RDF languages.
* **juneau-dto**	- A variety of predefined DTOs for serializing and parsing languages such as HTML5, Swagger and ATOM.
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
   * juneau-marshall, juneau-dto, juneau-svl, juneau-config - No external dependencies. Entirely self-contained.
   * juneau-marshall-rdf - Optional RDF support. Requires Apache Jena 2.7.1+.
   * juneau-rest-server - Any Servlet 3.1.0+ container.
   * juneau-rest-client - Apache HttpClient 4.5+.
* Built on top of Servlet and Apache HttpClient APIs that allow you to use the newest HTTP/2 features such as request/response multiplexing and server push.
