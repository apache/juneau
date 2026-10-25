# Downloads

## Current Release

**9.2.0 (Jan 5, 2026)**

- [Release Notes](/docs/release-notes/9.2.0)
- [Binaries](https://www.apache.org/dyn/closer.cgi/juneau/binaries/juneau-9.2.0/apache-juneau-9.2.0-bin.zip) ([ASC](https://www.apache.org/dist/juneau/binaries/juneau-9.2.0/apache-juneau-9.2.0-bin.zip.asc), [SHA512](https://www.apache.org/dist/juneau/binaries/juneau-9.2.0/apache-juneau-9.2.0-bin.zip.sha512))
- [Source](https://www.apache.org/dyn/closer.cgi/juneau/source/juneau-9.2.0/apache-juneau-9.2.0-src.zip) ([ASC](https://www.apache.org/dist/juneau/source/juneau-9.2.0/apache-juneau-9.2.0-src.zip.asc), [SHA512](https://www.apache.org/dist/juneau/source/juneau-9.2.0/apache-juneau-9.2.0-src.zip.sha512))

### What's Included

#### Binaries
- **`/shaded`** - Shaded (uber) JARs for simplified dependency management
  - `juneau-shaded-core.jar` - Core marshalling and configuration (2.1 MB)
  - `juneau-shaded-rest-client.jar` - Core + REST client functionality (3.8 MB)
  - `juneau-shaded-rest-server.jar` - Core + REST server functionality (3.8 MB)
  - `juneau-shaded-rest-server-springboot.jar` - REST server + Spring Boot integration (3.8 MB)
  - `juneau-shaded-all.jar` - Complete framework in one JAR (4.0 MB)
- **`/lib`** - Individual Jars
  - `juneau-commons.jar` - Bean metadata framework, SVL string-variable resolution, and shared utilities
  - `juneau-marshall.jar` - Marshalling
  - `juneau-marshall-rdf.jar` - Marshalling RDF extension (requires Apache Jena)
  - `juneau-config.jar` - Config File
  - `juneau-test.jar` - Fluent assertions, Bean-Centric Testing, and JUnit 5 extensions
  - `juneau-bean-atom.jar` - ATOM Data Transfer Object Beans
  - `juneau-bean-common.jar` - Common Data Transfer Object Beans
  - `juneau-bean-html5.jar` - HTML5 Data Transfer Object Beans
  - `juneau-bean-jsonschema.jar` - JSON Schema Data Transfer Object Beans
  - `juneau-bean-openapi-v3.jar` - OpenAPI 3 Data Transfer Object Beans
  - `juneau-bean-swagger-v2.jar` - Swagger 2 Data Transfer Object Beans
  - `juneau-rest-server.jar` - REST Servlet
  - `juneau-rest-server-springboot.jar` - REST Spring Boot integration
  - `juneau-rest-client.jar` - Recommended, transport-agnostic REST client (generally available in 10.0.0)
  - `juneau-rest-client-classic.jar` - Apache HttpClient 4.5 REST client, supported for existing users (not deprecated)
  - `juneau-rest-mock.jar` - REST mock testing API

- **`/osgi`** - OSGi Libraries
  - `org.apache.juneau.commons.jar` - Bean metadata framework, SVL, and shared utilities
  - `org.apache.juneau.marshall.jar` - Marshalling
  - `org.apache.juneau.marshall.rdf.jar` - Marshalling RDF extension
  - `org.apache.juneau.config.jar` - Config File
  - `org.apache.juneau.test.jar` - Fluent assertions and Bean-Centric Testing
  - `org.apache.juneau.bean.atom.jar` - ATOM Data Transfer Object Beans
  - `org.apache.juneau.bean.common.jar` - Common Data Transfer Object Beans
  - `org.apache.juneau.bean.html5.jar` - HTML5 Data Transfer Object Beans
  - `org.apache.juneau.bean.jsonschema.jar` - JSON Schema Data Transfer Object Beans
  - `org.apache.juneau.bean.openapi3.jar` - OpenAPI 3 Data Transfer Object Beans
  - `org.apache.juneau.rest.server.jar` - REST Servlet
  - `org.apache.juneau.rest.server.springboot.jar` - REST Spring Boot integration
  - `org.apache.juneau.rest.client.jar` - REST Client
  - `org.apache.juneau.rest.mock.jar` - REST mock testing API

- **`/projects`** - Eclipse Projects
  - `juneau-examples-core.zip` - Core libraries examples
  - `juneau-petstore-jetty.zip` - Canonical petstore showcase application starter (Jetty/Microservice deployment)

### Maven and Gradle

Add the module you need from Maven Central. The artifact ids are the ones published for 9.2.0; the full list is in the "What's Included" section above. These docs describe 10.0 (`10.0.0-SNAPSHOT`), which is not on Maven Central, so to use 10.0 snapshots see [Install Juneau](/docs/topics/GettingStartedInstall#set-the-version).

```xml
<dependency>
	<groupId>org.apache.juneau</groupId>
	<artifactId>juneau-marshall</artifactId>
	<version>9.2.0</version>
</dependency>
```

```groovy
implementation 'org.apache.juneau:juneau-marshall:9.2.0'
```

For a single JAR that bundles everything, use `juneau-shaded-all`. The 9.2.0 Maven artifacts do not include `juneau-marshall-rdf`; for the 10.0 module list, see the [Juneau Ecosystem](/docs/topics/JuneauEcosystemOverview) overview.

## Verifying File Integrity

How to [verify downloaded files](https://www.apache.org/info/verification.html).

[Download KEYS](https://www.apache.org/dist/juneau/KEYS) file.

## Older Releases
### 9.1.0 (June 19, 2025)
- [Release Notes](/docs/release-notes/9.1.0)
- [Binaries](https://archive.apache.org/dyn/closer.cgi/juneau/binaries/juneau-9.1.0/apache-juneau-9.1.0-bin.zip) ([ASC](https://archive.apache.org/dist/juneau/binaries/juneau-9.1.0/apache-juneau-9.1.0-bin.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/binaries/juneau-9.1.0/apache-juneau-9.1.0-bin.zip.sha512))
- [Source](https://archive.apache.org/dyn/closer.cgi/juneau/source/juneau-9.1.0/apache-juneau-9.1.0-src.zip) ([ASC](https://archive.apache.org/dist/juneau/source/juneau-9.1.0/apache-juneau-9.1.0-src.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/source/juneau-9.1.0/apache-juneau-9.1.0-src.zip.sha512))

### 9.0.1 (Sept 6, 2023)
- [Release Notes](/docs/release-notes/9.0.1)
- [Binaries](https://archive.apache.org/dyn/closer.cgi/juneau/binaries/juneau-9.0.1/apache-juneau-9.0.1-bin.zip) ([ASC](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.1/apache-juneau-9.0.1-bin.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.1/apache-juneau-9.0.1-bin.zip.sha512))
- [Source](https://archive.apache.org/dyn/closer.cgi/juneau/source/juneau-9.0.1/apache-juneau-9.0.1-src.zip) ([ASC](https://archive.apache.org/dist/juneau/source/juneau-9.0.1/apache-juneau-9.0.1-src.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/source/juneau-9.0.1/apache-juneau-9.0.1-src.zip.sha512))

### 9.0.0 (Feb 27, 2023)
- [Release Notes](/docs/release-notes/9.0.0)
- [Binaries](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.0/apache-juneau-9.0.0-bin.zip) ([ASC](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.0/apache-juneau-9.0.0-bin.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/binaries/juneau-9.0.0/apache-juneau-9.0.0-bin.zip.sha512))
- [Source](https://archive.apache.org/dist/juneau/source/juneau-9.0.0/apache-juneau-9.0.0-src.zip) ([ASC](https://archive.apache.org/dist/juneau/source/juneau-9.0.0/apache-juneau-9.0.0-src.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/source/juneau-9.0.0/apache-juneau-9.0.0-src.zip.sha512))

### 8.2.0 (Oct 14, 2020)
- [Release Notes](/docs/release-notes/8.x#release-8-2-0)
- [Binaries](https://archive.apache.org/dist/juneau/binaries/juneau-8.2.0/apache-juneau-8.2.0-bin.zip) ([ASC](https://archive.apache.org/dist/juneau/binaries/juneau-8.2.0/apache-juneau-8.2.0-bin.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/binaries/juneau-8.2.0/apache-juneau-8.2.0-bin.zip.sha512))
- [Source](https://archive.apache.org/dist/juneau/source/juneau-8.2.0/apache-juneau-8.2.0-src.zip) ([ASC](https://archive.apache.org/dist/juneau/source/juneau-8.2.0/apache-juneau-8.2.0-src.zip.asc), [SHA512](https://archive.apache.org/dist/juneau/source/juneau-8.2.0/apache-juneau-8.2.0-src.zip.sha512))

**Note:** Additional older releases are available in the [Apache Archive](https://archive.apache.org/dist/juneau/).
