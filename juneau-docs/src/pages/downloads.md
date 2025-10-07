# Downloads

## Current Release

**9.1.0 (June 19, 2025)**

- [Release Notes](http://juneau.apache.org/site/apidocs-9.1.0/overview-summary.html#9.1.0)
- [Binaries](https://www.apache.org/dyn/closer.cgi/juneau/binaries/juneau-9.1.0/apache-juneau-9.1.0-bin.zip) ([ASC](https://www.apache.org/dist/juneau/binaries/juneau-9.1.0/apache-juneau-9.1.0-bin.zip.asc), [SHA512](https://www.apache.org/dist/juneau/binaries/juneau-9.1.0/apache-juneau-9.1.0-bin.zip.sha512))
- [Source](https://www.apache.org/dyn/closer.cgi/juneau/source/juneau-9.1.0/apache-juneau-9.1.0-src.zip) ([ASC](https://www.apache.org/dist/juneau/source/juneau-9.1.0/apache-juneau-9.1.0-src.zip.asc), [SHA512](https://www.apache.org/dist/juneau/source/juneau-9.1.0/apache-juneau-9.1.0-src.zip.sha512))

### What's Included

#### Binaries
- **`juneau-all.jar`** - Everything (except RDF, JAX/RS, and Microservice)
- **`/lib`** - Individual Jars
  - `juneau-marshall.jar` - Marshalling
  - `juneau-marshall-rdf.jar` - Marshalling RDF extension (requires Apache Jena 2.7.1+)
  - `juneau-bean-atom.jar` - ATOM Data Transfer Object Beans
  - `juneau-bean-common.jar` - Common Data Transfer Object Beans
  - `juneau-bean-html5.jar` - HTML5 Data Transfer Object Beans
  - `juneau-bean-jsonschema.jar` - JSON Schema Data Transfer Object Beans
  - `juneau-bean-openapi-v3.jar` - OpenAPI 3 Data Transfer Object Beans
  - `juneau-bean-swagger-v2.jar` - Swagger 2 Data Transfer Object Beans
  - `juneau-svl.jar` - Simple Variable Language
  - `juneau-config.jar` - Config File
  - `juneau-rest-server.jar` - REST Servlet
  - `juneau-rest-server-jaxrs.jar` - REST Servlet JAX/RS extension
  - `juneau-rest-client.jar` - REST Client (requires Apache HttpClient 4.5+)
  - `juneau-rest-mock.jar` - REST mock testing API

- **`/osgi`** - OSGi Libraries
  - `org.apache.juneau.marshall.jar` - Marshalling
  - `org.apache.juneau.marshall.rdf.jar` - Marshalling RDF extension
  - `org.apache.juneau.bean.atom.jar` - ATOM Data Transfer Object Beans
  - `org.apache.juneau.bean.common.jar` - Common Data Transfer Object Beans
  - `org.apache.juneau.bean.html5.jar` - HTML5 Data Transfer Object Beans
  - `org.apache.juneau.bean.jsonschema.jar` - JSON Schema Data Transfer Object Beans
  - `org.apache.juneau.bean.openapi3.jar` - OpenAPI 3 Data Transfer Object Beans
  - `org.apache.juneau.svl.jar` - Simple Variable Language
  - `org.apache.juneau.config.jar` - Config File
  - `org.apache.juneau.rest.server.jar` - REST Servlet
  - `org.apache.juneau.rest.server.jaxrs.jar` - REST Servlet JAX/RS extension
  - `org.apache.juneau.rest.server.springboot` - REST Spring Boot integration
  - `org.apache.juneau.rest.client.jar` - REST Client
  - `org.apache.juneau.rest.mock.jar` - REST mock testing API

- **`/projects`** - Eclipse Projects
  - `my-springboot-microservice.zip` - Microservice starter project using Spring Boot
  - `juneau-examples-core.zip` - Core libraries examples
  - `juneau-examples-rest-jetty.zip` - REST libraries examples using Jetty
  - `juneau-examples-rest-springboot.zip` - REST libraries examples using Spring Boot

### Maven

```xml
<!-- Use the following dependency... -->
<dependency>
	<groupId>org.apache.juneau</groupId>
	<artifactId>juneau-XXX</artifactId>
	<version>9.1.0</version>
</dependency>

<!-- ...where the artifactId is typically... -->
<artifactId>juneau-all</artifactId>

<!-- ...but can also be any of the following... -->
<artifactId>juneau-marshall</artifactId>
<artifactId>juneau-marshall-rdf</artifactId>
<artifactId>juneau-bean-atom</artifactId>
<artifactId>juneau-bean-common</artifactId>
<artifactId>juneau-bean-html5</artifactId>
<artifactId>juneau-bean-jsonschema</artifactId>
<artifactId>juneau-bean-openapi-v3</artifactId>
<artifactId>juneau-bean-swagger-v2</artifactId>
<artifactId>juneau-svl</artifactId>
<artifactId>juneau-config</artifactId>
<artifactId>juneau-rest-server</artifactId>
<artifactId>juneau-rest-server-jaxrs</artifactId>
<artifactId>juneau-rest-client</artifactId>
<artifactId>juneau-microservice-server</artifactId>
```

## Verifying File Integrity

How to [verify downloaded files](https://www.apache.org/info/verification.html).

[Download KEYS](https://www.apache.org/dist/juneau/KEYS) file.

## Older Releases

### 9.0.1 (Sept 6, 2023)
- [Release Notes](http://juneau.apache.org/site/apidocs-9.1.0/overview-summary.html#9.0.1)
- [Binaries](https://archive.apache.org/dyn/closer.cgi/juneau/binaries/juneau-9.0.1/apache-juneau-9.0.1-bin.zip) ([ASC](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.1/apache-juneau-9.0.1-bin.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.1/apache-juneau-9.0.1-bin.zip.sha512))
- [Source](https://archive.apache.org/dyn/closer.cgi/juneau/source/juneau-9.0.1/apache-juneau-9.0.1-src.zip) ([ASC](https://archive.apache.org/dist/juneau/source/juneau-9.0.1/apache-juneau-9.0.1-src.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/source/juneau-9.0.1/apache-juneau-9.0.1-src.zip.sha512))

### 9.0.0 (Feb 27, 2023)
- [Release Notes](http://juneau.apache.org/site/apidocs-9.1.0/overview-summary.html#9.0.0)
- [Binaries](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.0/apache-juneau-9.0.0-bin.zip) ([ASC](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.0/apache-juneau-9.0.0-bin.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.0/apache-juneau-9.0.0-bin.zip.sha512))
- [Source](https://archive.apache.org/dist/juneau/source/juneau-9.0.0/apache-juneau-9.0.0-src.zip) ([ASC](https://archive.apache.org/dist/juneau/source/juneau-9.0.0/apache-juneau-9.0.0-src.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/source/juneau-9.0.0/apache-juneau-9.0.0-src.zip.sha512))

### 8.2.0 (Oct 14, 2020)
- [Release Notes](http://juneau.apache.org/site/apidocs-9.1.0/overview-summary.html#8.2.0)
- [Binaries](https://archive.apache.org/dist/juneau/binaries/juneau-8.2.0/apache-juneau-8.2.0-bin.zip) ([ASC](https://archive.apache.org/dist/juneau/binaries/juneau-8.2.0/apache-juneau-8.2.0-bin.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/binaries/juneau-8.2.0/apache-juneau-8.2.0-bin.zip.sha512))
- [Source](https://archive.apache.org/dist/juneau/source/juneau-8.2.0/apache-juneau-8.2.0-src.zip) ([ASC](https://archive.apache.org/dist/juneau/source/juneau-8.2.0/apache-juneau-8.2.0-src.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/source/juneau-8.2.0/apache-juneau-8.2.0-src.zip.sha512))

**Note:** Additional older releases are available in the [Apache Archive](https://archive.apache.org/dist/juneau/).
