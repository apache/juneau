# Apache Juneau Shaded Artifacts

This module contains shaded (uber) JAR artifacts that bundle multiple Juneau modules together. These artifacts are useful for:

1. **Bazel Build System**: Bazel requires explicit declaration of all dependencies. Shaded JARs simplify this by bundling transitive dependencies.
2. **Simplified Dependency Management**: Projects can include one shaded JAR instead of managing multiple Juneau dependencies.
3. **Reduced Build Configuration**: Less configuration needed in build files.

## Available Shaded Artifacts

### juneau-shaded-core
**Artifact ID**: `juneau-shaded-core`  
**Description**: All core Juneau modules for general marshalling and configuration work.

**Includes**:
- juneau-commons
- juneau-assertions
- juneau-bct (Bean-Centric Testing)
- juneau-config
- juneau-marshall

**Maven Usage**:
```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-shaded-core</artifactId>
    <version>9.2.0-SNAPSHOT</version>
</dependency>
```

**Bazel Usage**:
```python
maven_jar(
    name = "juneau_core",
    artifact = "org.apache.juneau:juneau-shaded-core:9.2.0-SNAPSHOT",
)
```

---

### juneau-shaded-rest-client
**Artifact ID**: `juneau-shaded-rest-client`  
**Description**: Everything needed for REST client development.

**Includes**:
- All modules from juneau-shaded-core
- juneau-rest-common
- juneau-rest-client
- juneau-rest-mock

**Maven Usage**:
```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-shaded-rest-client</artifactId>
    <version>9.2.0-SNAPSHOT</version>
</dependency>
```

**Bazel Usage**:
```python
maven_jar(
    name = "juneau_rest_client",
    artifact = "org.apache.juneau:juneau-shaded-rest-client:9.2.0-SNAPSHOT",
)
```

---

### juneau-shaded-rest-server
**Artifact ID**: `juneau-shaded-rest-server`  
**Description**: Everything needed for REST server development.

**Includes**:
- All modules from juneau-shaded-core
- juneau-rest-common
- juneau-rest-server
- juneau-rest-server-rdf
- juneau-rest-mock

**Maven Usage**:
```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-shaded-rest-server</artifactId>
    <version>9.2.0-SNAPSHOT</version>
</dependency>
```

**Bazel Usage**:
```python
maven_jar(
    name = "juneau_rest_server",
    artifact = "org.apache.juneau:juneau-shaded-rest-server:9.2.0-SNAPSHOT",
)
```

---

### juneau-shaded-rest-server-springboot
**Artifact ID**: `juneau-shaded-rest-server-springboot`  
**Description**: Everything needed for REST server development with Spring Boot integration.

**Includes**:
- All modules from juneau-shaded-rest-server
- juneau-rest-server-springboot

**Maven Usage**:
```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-shaded-rest-server-springboot</artifactId>
    <version>9.2.0-SNAPSHOT</version>
</dependency>
```

**Bazel Usage**:
```python
maven_jar(
    name = "juneau_rest_server_springboot",
    artifact = "org.apache.juneau:juneau-shaded-rest-server-springboot:9.2.0-SNAPSHOT",
)
```

---

### juneau-shaded-all
**Artifact ID**: `juneau-shaded-all`  
**Description**: The complete Juneau framework in a single JAR (excludes unit tests, examples, and distribution artifacts).

**Includes**:
- All modules from juneau-shaded-rest-server-springboot
- juneau-rest-client
- All juneau-bean modules (atom, html5, jsonschema, openapi-v3, swagger-v2)
- All juneau-microservice modules (core, jetty)

**Maven Usage**:
```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-shaded-all</artifactId>
    <version>9.2.0-SNAPSHOT</version>
</dependency>
```

**Bazel Usage**:
```python
maven_jar(
    name = "juneau_all",
    artifact = "org.apache.juneau:juneau-shaded-all:9.2.0-SNAPSHOT",
)
```

---

## Important Notes

### External Dependencies
These shaded JARs **only** bundle Juneau modules. External dependencies (like Apache HttpClient, Jakarta Servlet API, Spring Boot, etc.) are still declared as transitive dependencies and must be managed separately.

### Maven Dependency Resolution
When using shaded artifacts in Maven, transitive dependencies are automatically resolved. The generated `dependency-reduced-pom.xml` in each shaded module shows the actual external dependencies that will be pulled in.

### Bazel Dependency Resolution
When using shaded artifacts in Bazel, you **must** explicitly declare all external dependencies. To find out what external dependencies are needed:

1. Check the `dependency-reduced-pom.xml` in the shaded module's target directory after building
2. Or use `mvn dependency:tree` on the shaded module
3. Or refer to the individual Juneau module POMs to see their dependencies

Example Bazel configuration with external dependencies:
```python
maven_jar(
    name = "juneau_rest_client",
    artifact = "org.apache.juneau:juneau-shaded-rest-client:9.2.0-SNAPSHOT",
)

maven_jar(
    name = "httpclient",
    artifact = "org.apache.httpcomponents.client5:httpclient5:5.2.1",
)

maven_jar(
    name = "httpcore",
    artifact = "org.apache.httpcomponents.core5:httpcore5:5.2.1",
)

# ... other external dependencies
```

### Service Files
The Maven Shade Plugin automatically merges `META-INF/services` files from all included modules, ensuring that service providers are properly registered.

### Signatures
JAR signature files (`*.SF`, `*.DSA`, `*.RSA`) are excluded from shaded JARs to prevent signature validation errors.

---

## Building

To build all shaded artifacts:

```bash
cd juneau-shaded
mvn clean install
```

To build a specific shaded artifact:

```bash
cd juneau-shaded/juneau-shaded-core
mvn clean install
```

---

## Module Dependencies

The shaded modules form a dependency hierarchy:

```
juneau-shaded-core
    ↓
juneau-shaded-rest-client
    ↓ (separate branch)
juneau-shaded-rest-server
    ↓
juneau-shaded-rest-server-springboot
    ↓
juneau-shaded-all (also includes rest-client + bean modules + microservice)
```

This hierarchy means:
- `juneau-shaded-rest-client` depends on `juneau-shaded-core`
- `juneau-shaded-rest-server` depends on `juneau-shaded-core`
- `juneau-shaded-rest-server-springboot` depends on `juneau-shaded-rest-server`
- `juneau-shaded-all` depends on `juneau-shaded-rest-server-springboot` plus additional modules

---

## When to Use

### Use Shaded JARs When:
- Using Bazel or another build system with strict dependency requirements
- You want simplified dependency management
- You're building a standalone application
- You want to minimize build configuration

### Use Individual Modules When:
- You need fine-grained control over dependencies
- You're building a library that will be used by others
- You want to minimize JAR size by excluding unused modules
- You need to avoid potential classpath conflicts in complex applications

---

## License

All shaded artifacts are licensed under the Apache License 2.0, same as the rest of Apache Juneau.

