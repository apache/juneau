# Start Juneau REST Examples (Jetty) Script

## Overview

`start-examples-rest-jetty.py` starts the Juneau REST examples application using the embedded Jetty server. This provides a live demonstration of Juneau's REST capabilities.

## What It Does

The script performs these operations:

1. **Locates Project** - Finds the Juneau project root directory
2. **Checks Build** - Verifies that classes are compiled
3. **Auto-Builds** - Runs `mvn clean compile` if classes are missing
4. **Starts Server** - Launches the Jetty-based REST examples application
5. **Handles Shutdown** - Gracefully stops the server on Ctrl+C

## Usage

### Start the Server

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/start-examples-rest-jetty.py
```

The server will start at `http://localhost:10000/`

Press `Ctrl+C` to stop the server.

## Output Example

```
🚀 Starting Juneau REST Examples (Jetty)...
📁 Working directory: /Users/james.bognar/git/juneau/juneau-examples/juneau-examples-rest-jetty
🚀 Starting Jetty microservice...
Main class: org.apache.juneau.examples.rest.jetty.App
Default URL: http://localhost:10000

Press Ctrl+C to stop the server

================================================================================
[INFO] Scanning for projects...
[INFO] Building Apache Juneau REST Examples Jetty 9.2.0-SNAPSHOT
...
INFO: Jetty server started on port 10000
INFO: Open your browser to http://localhost:10000
```

## What You Can Do

Once the server is running, visit `http://localhost:10000/` to:

- **Browse REST Resources** - Explore the example REST APIs
- **Test Serialization** - See JSON, XML, HTML, and other formats
- **View Documentation** - Auto-generated API documentation
- **Try Examples** - Interactive REST interface
- **Learn Patterns** - See best practices in action

### Example Endpoints

- `http://localhost:10000/` - Root resources with navigation
- `http://localhost:10000/helloWorld` - Simple hello world example
- `http://localhost:10000/addressBook` - CRUD operations example
- `http://localhost:10000/petStore` - Pet store REST API example
- `http://localhost:10000/photos` - Photo gallery example

## When to Use

Use this script when:
- **Testing REST Features** - Trying out Juneau REST capabilities
- **Learning Juneau** - Exploring examples and patterns
- **Debugging** - Testing changes to REST examples
- **Demonstrations** - Showing Juneau to others
- **Development** - Working on REST examples code

## Auto-Build Feature

If the project hasn't been compiled, the script will automatically build it:

```
⚠️  Classes not found. Building project first...
🔨 Running: mvn clean compile

[INFO] Scanning for projects...
[INFO] Building Apache Juneau REST Examples Jetty 9.2.0-SNAPSHOT
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

### Force Kill (if needed)

If the server doesn't stop gracefully:

**macOS/Linux:**
```bash
lsof -ti:10000 | xargs kill -9
```

**Windows:**
```bash
netstat -ano | findstr :10000
taskkill /PID <pid> /F
```

## Troubleshooting

### Port 10000 Already in Use

If you see an error about port 10000 being in use:

1. **Find the process:**
   ```bash
   lsof -ti:10000  # macOS/Linux
   ```

2. **Kill it:**
   ```bash
   lsof -ti:10000 | xargs kill -9
   ```

3. **Or change the port** - Edit `juneau-examples-rest-jetty.cfg` to use a different port

### Build Failures

If the auto-build fails:

```bash
# Build from project root
cd /Users/james.bognar/git/juneau
mvn clean install -DskipTests

# Or build just the examples
cd juneau-examples/juneau-examples-rest-jetty
mvn clean compile
```

### Maven Not Found

Make sure Maven is installed and in your PATH:

```bash
mvn -version
```

If not installed, download from: https://maven.apache.org/

### Java Version Issues

The examples require Java 11 or higher:

```bash
java -version
```

If using an older version, update Java or set `JAVA_HOME`:

```bash
export JAVA_HOME=/path/to/jdk11
```

### Class Not Found Errors

If you see `ClassNotFoundException`:

1. Clean and rebuild:
   ```bash
   cd juneau-examples/juneau-examples-rest-jetty
   mvn clean compile
   ```

2. Make sure parent project is installed:
   ```bash
   cd /Users/james.bognar/git/juneau
   mvn clean install -DskipTests
   ```

### Server Starts But Can't Connect

1. **Check the port** - Look for "Jetty server started on port XXXX" in the output
2. **Check firewall** - Make sure port 10000 isn't blocked
3. **Wait a moment** - Server may take a few seconds to fully initialize
4. **Check logs** - Look for errors in the console output

## Configuration

The Jetty server is configured via:

```
juneau-examples/juneau-examples-rest-jetty/juneau-examples-rest-jetty.cfg
```

You can modify:
- **Port** - `Jetty/port`
- **Context Path** - `Jetty/contextPath`
- **Resources** - REST resource classes to load
- **Logging** - Log levels and output

Example:
```ini
[Jetty]
port = 10000
contextPath = /

[REST]
allowBodyParam = true
```

## Development Workflow

### Typical usage during development:

```bash
# 1. Start the server
python3 scripts/start-examples-rest-jetty.py

# 2. Open browser to http://localhost:10000

# 3. Make changes to example code

# 4. Stop server (Ctrl+C)

# 5. Rebuild
cd juneau-examples/juneau-examples-rest-jetty
mvn compile

# 6. Restart
python3 scripts/start-examples-rest-jetty.py
```

### Hot Reload (Advanced)

For faster development, you can use Maven's exec plugin directly with a class reloader, but this script uses the standard approach for simplicity.

## Requirements

- **Python**: 3.6 or higher
- **Maven**: 3.6 or higher
- **Java**: JDK 11 or higher
- **Built Project**: Script will auto-build if needed

## Replacing Launch Files

This Python script replaces the Eclipse `.launch` file with:
- ✅ Cross-platform compatibility (not Eclipse-specific)
- ✅ Auto-build feature
- ✅ Better error messages
- ✅ Clearer console output
- ✅ Can run from anywhere in the project

## Related Examples

- **Spring Boot Version**: Use `start-examples-rest-springboot.py` instead
- **Pet Store**: Standalone application with more features
- **Microservices**: See `juneau-microservice` modules

## Performance Notes

- **Startup Time**: 5-15 seconds depending on system
- **Memory**: ~200-500 MB heap
- **Port**: Default 10000 (configurable)
- **Auto-reload**: Not enabled by default (requires restart)

## Notes

- The script automatically detects the project root
- Uses Maven's exec plugin for proper classpath handling
- Configuration is read from `juneau-examples-rest-jetty.cfg`
- The server supports hot-swappable REST resources
- All REST resources are documented at the root URL
- Examples demonstrate various serialization formats

