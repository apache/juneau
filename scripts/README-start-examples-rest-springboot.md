# Start Juneau REST Examples (Spring Boot) Script

## Overview

`start-examples-rest-springboot.py` starts the Juneau REST examples application using Spring Boot. This demonstrates how Juneau integrates with the Spring Boot framework.

## What It Does

The script performs these operations:

1. **Locates Project** - Finds the Juneau project root directory
2. **Checks Build** - Verifies that classes are compiled
3. **Auto-Builds** - Runs `mvn clean compile` if classes are missing
4. **Starts Server** - Launches the Spring Boot REST examples application
5. **Handles Shutdown** - Gracefully stops the server on Ctrl+C

## Usage

### Start the Server

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/start-examples-rest-springboot.py
```

The server will start at `http://localhost:5000/`

Press `Ctrl+C` to stop the server.

## Output Example

```
🚀 Starting Juneau REST Examples (Spring Boot)...
📁 Working directory: /Users/james.bognar/git/juneau/juneau-examples/juneau-examples-rest-springboot
🚀 Starting Spring Boot application...
Main class: org.apache.juneau.examples.rest.springboot.App
Default URL: http://localhost:5000

Press Ctrl+C to stop the server

================================================================================
[INFO] Scanning for projects...
[INFO] Building Apache Juneau REST Examples Spring Boot 9.2.0-SNAPSHOT
...
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.0)

INFO: Started App in 3.456 seconds (JVM running for 4.123)
Initialized.  App available on http://localhost:5000
```

## What You Can Do

Once the server is running, visit `http://localhost:5000/` to:

- **Browse REST Resources** - Explore the example REST APIs
- **Test Serialization** - See JSON, XML, HTML, and other formats
- **View Documentation** - Auto-generated API documentation
- **Try Examples** - Interactive REST interface
- **See Spring Integration** - How Juneau works with Spring Boot
- **Test Dependency Injection** - Spring beans in Juneau resources

### Example Endpoints

- `http://localhost:5000/` - Root resources with navigation
- `http://localhost:5000/helloWorld` - Hello world with Spring injection
- `http://localhost:5000/addressBook` - CRUD operations example
- `http://localhost:5000/petStore` - Pet store REST API example
- `http://localhost:5000/photos` - Photo gallery example

## When to Use

Use this script when:
- **Learning Spring Boot Integration** - See how Juneau works with Spring
- **Testing REST Features** - Trying out Juneau REST capabilities
- **Debugging** - Testing changes to Spring Boot examples
- **Comparing** - See differences from Jetty version
- **Development** - Working on Spring Boot integration code

## Differences from Jetty Version

| Feature | Jetty | Spring Boot |
|---------|-------|-------------|
| **Port** | 10000 | 5000 |
| **Startup** | Faster | Slower (Spring init) |
| **DI** | Manual | Spring autowiring |
| **Config** | `.cfg` file | `application.properties` |
| **Use Case** | Microservices | Enterprise apps |

## Auto-Build Feature

If the project hasn't been compiled, the script will automatically build it:

```
⚠️  Classes not found. Building project first...
🔨 Running: mvn clean compile

[INFO] Scanning for projects...
[INFO] Building Apache Juneau REST Examples Spring Boot 9.2.0-SNAPSHOT
...
[INFO] BUILD SUCCESS

✓ Build completed successfully
```

This ensures the server can always start, even on a fresh clone.

## Stopping the Server

### Graceful Shutdown

Press `Ctrl+C` in the terminal:

```
^C
🛑 Shutting down server...
Server stopped
```

Spring Boot will perform a graceful shutdown, closing resources properly.

### Force Kill (if needed)

If the server doesn't stop gracefully:

**macOS/Linux:**
```bash
lsof -ti:5000 | xargs kill -9
```

**Windows:**
```bash
netstat -ano | findstr :5000
taskkill /PID <pid> /F
```

## Troubleshooting

### Port 5000 Already in Use

If you see an error about port 5000 being in use:

1. **Find the process:**
   ```bash
   lsof -ti:5000  # macOS/Linux
   ```

2. **Kill it:**
   ```bash
   lsof -ti:5000 | xargs kill -9
   ```

3. **Or change the port** - Set environment variable:
   ```bash
   export SERVER_PORT=8080
   python3 scripts/start-examples-rest-springboot.py
   ```

   Or edit `application.properties`:
   ```properties
   server.port=8080
   ```

### Build Failures

If the auto-build fails:

```bash
# Build from project root
cd /Users/james.bognar/git/juneau
mvn clean install -DskipTests

# Or build just the examples
cd juneau-examples/juneau-examples-rest-springboot
mvn clean compile
```

### Maven Not Found

Make sure Maven is installed and in your PATH:

```bash
mvn -version
```

If not installed, download from: https://maven.apache.org/

### Spring Boot Startup Fails

Common issues:

1. **Dependency conflicts** - Clean and rebuild:
   ```bash
   mvn clean compile
   ```

2. **Port conflicts** - Change the port (see above)

3. **Memory issues** - Increase heap size:
   ```bash
   export MAVEN_OPTS="-Xmx2g"
   python3 scripts/start-examples-rest-springboot.py
   ```

### Java Version Issues

The Spring Boot examples require Java 17 or higher:

```bash
java -version
```

If using an older version, update Java or set `JAVA_HOME`:

```bash
export JAVA_HOME=/path/to/jdk17
```

### Slow Startup

Spring Boot can take 5-15 seconds to start. This is normal due to:
- Spring context initialization
- Component scanning
- Dependency injection setup
- Bean creation

For faster development iterations, consider:
- Using Spring DevTools (hot reload)
- The Jetty version (faster startup)
- Disabling unused Spring features

### Server Starts But Can't Connect

1. **Check the port** - Look for "Started App in X.XXX seconds" in the output
2. **Wait for initialization** - Spring Boot may take a moment after "Started"
3. **Check firewall** - Make sure port 5000 isn't blocked
4. **Check logs** - Look for errors in the console output
5. **Verify URL** - The app prints "App available on http://localhost:5000"

## Configuration

The Spring Boot application is configured via:

```
juneau-examples/juneau-examples-rest-springboot/src/main/resources/application.properties
```

You can modify:
- **Server Port** - `server.port=5000`
- **Context Path** - `server.servlet.context-path=/`
- **Logging** - `logging.level.org.apache.juneau=DEBUG`
- **Spring Settings** - Any Spring Boot property

Example:
```properties
server.port=5000
server.servlet.context-path=/
logging.level.org.apache.juneau=INFO
spring.main.banner-mode=console
```

You can also override properties via environment variables:
```bash
export SERVER_PORT=8080
python3 scripts/start-examples-rest-springboot.py
```

## Development Workflow

### Typical usage during development:

```bash
# 1. Start the server
python3 scripts/start-examples-rest-springboot.py

# 2. Open browser to http://localhost:5000

# 3. Make changes to example code

# 4. Stop server (Ctrl+C)

# 5. Rebuild
cd juneau-examples/juneau-examples-rest-springboot
mvn compile

# 6. Restart
python3 scripts/start-examples-rest-springboot.py
```

### Hot Reload (Spring DevTools)

For faster development, add Spring DevTools to the POM:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

Then code changes will auto-reload without restarting the server.

## Spring Integration Features

This example demonstrates:

- **@SpringBootApplication** - Spring Boot entry point
- **@Bean** - Exposing Juneau resources as Spring beans
- **Dependency Injection** - Autowiring services into REST resources
- **ServletRegistrationBean** - Registering Juneau servlet
- **Spring Configuration** - Using `application.properties`

Example from the code:
```java
@Bean
public HelloWorldMessageProvider getHelloWorldMessageProvider() {
    return new HelloWorldMessageProvider("Hello Spring injection user!");
}
```

## Requirements

- **Python**: 3.6 or higher
- **Maven**: 3.6 or higher
- **Java**: JDK 17 or higher (Spring Boot 3.x requirement)
- **Built Project**: Script will auto-build if needed

## Replacing Launch Files

This Python script replaces the Eclipse `.launch` file with:
- ✅ Cross-platform compatibility (not Eclipse-specific)
- ✅ Auto-build feature
- ✅ Better error messages
- ✅ Clearer console output
- ✅ Can run from anywhere in the project
- ✅ Uses Spring Boot Maven plugin properly

## Related Examples

- **Jetty Version**: Use `start-examples-rest-jetty.py` for faster startup
- **Pet Store**: Standalone application with more features
- **Microservices**: See `juneau-microservice` modules

## Performance Notes

- **Startup Time**: 5-20 seconds (Spring Boot overhead)
- **Memory**: ~300-600 MB heap (Spring context)
- **Port**: Default 5000 (configurable)
- **Hot Reload**: Available via Spring DevTools

## Notes

- The script automatically detects the project root
- Uses Maven's Spring Boot plugin for proper execution
- Configuration is read from `application.properties`
- Spring context includes all `@Bean` annotated methods
- Supports full Spring Boot ecosystem (Actuator, Security, etc.)
- Examples demonstrate Spring dependency injection
- Slower startup than Jetty but more enterprise features

