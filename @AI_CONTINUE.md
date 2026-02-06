# SonarLint System.err/out Replacement - Continuation Guide

## Current Status

We are working on resolving SonarLint naming convention and code quality issues, primarily by replacing `System.err` and `System.out` calls with `org.apache.juneau.commons.logging.Logger` instances, or by adding `@SuppressWarnings` annotations where direct console output is intentional.

## What We've Done

### Completed Replacements (System.err/out → Logger)

1. **BasicSwaggerProviderSession.java**: Replaced `System.err.println` with `LOG.warning()`
2. **Microservice.java**: Replaced `System.err.println` with `getLogger().warning()`
3. **RestContext.java**: 
   - Replaced `System.err.println` with `LOG.warning()`
   - Ensured `createLogger` returns `org.apache.juneau.commons.logging.Logger`
   - Registered logger under both `Logger.class` (Juneau) and `java.util.logging.Logger.class` in bean store for compatibility
4. **ThrowableUtils.java**: Added `LOG.warning()` alongside existing `printStackTrace` call
5. **Utils.java**: Replaced `System.out.println` with `Logger.getLogger(Utils.class).info()`
6. **WriterSerializer.java**: Replaced `System.out.println` with `Logger.getLogger(WriterSerializer.class).info()`
7. **SystemUtils.java**: Replaced `System.out.println` in shutdown hook with `LOG.info()`
8. **Example Files**: Replaced `System.out.println`/`System.err.println` with `Logger.getLogger(ClassName.class).info()` in:
   - `App.java`, `AtomHtmlExample.java`, `HtmlComplexExample.java`, `HtmlSimpleExample.java`
   - `JsonComplexExample.java`, `JsonConfigurationExample.java`, `JsonSimpleExample.java`
   - `OapiExample.java`, `SvlExample.java`, `XmlComplexExample.java`, `XmlConfigurationExample.java`, `XmlSimpleExample.java`
   - `UonComplexExample.java`, `UonExample.java`, `GitControl.java`

### Completed Suppressions (Intentional Console Output)

1. **Assertion.java**: 
   - Reverted Logger changes (direct console output is intentional for assertion error output)
   - Added class-level `@SuppressWarnings({"java:S106", "java:S108"})`
2. **Console.java**: 
   - Reverted Logger changes (direct console output is intentional for console utility)
   - Added class-level `@SuppressWarnings({"java:S106", "java:S108"})`
3. **PredicateUtils.java**: 
   - Reverted Logger changes (tests capture `System.err` output)
   - Added class-level `@SuppressWarnings("java:S106")`

### Files Left Unchanged (Intentional)

- **RestClient.java**: Assignment of `System.err` to `console` field is intentional for direct console output, not logging

## Key Technical Details

### Logger Type Consistency
- **Primary Logger**: `org.apache.juneau.commons.logging.Logger` (extends `java.util.logging.Logger`)
- **Bean Store Registration**: In `RestContext`, logger must be registered under both:
  - `Logger.class` (Juneau logger)
  - `java.util.logging.Logger.class` (for `CallLogger` compatibility)

### SonarLint Rules Addressed
- **java:S106**: Standard outputs should not be used directly for logging
- **java:S108**: Nested blocks of code should not be left empty

### Pattern for Logger Usage
```java
import org.apache.juneau.commons.logging.Logger;

private static final Logger LOG = Logger.getLogger(ClassName.class);

// For warnings:
LOG.warning("Message: {}", arg);
LOG.warning(throwable, "Message");

// For info:
LOG.info("Message: {}", arg);
Logger.getLogger(ClassName.class).info("Message");
```

## Issues Resolved

1. **Type Mismatch in RestContext**: Fixed by ensuring `createLogger` returns `org.apache.juneau.commons.logging.Logger` and registering under both logger types
2. **Test Failure in PredicateUtils_Test**: Fixed by reverting to `System.err.println()` (tests capture `System.err` output) and adding suppression
3. **Import Issues**: Added `import org.apache.juneau.commons.logging.Logger;` to `RestObject.java` and `RestServlet.java`

## Current State

All explicit requests to replace `System.out`/`err` with loggers or revert/suppress have been completed. All compilation errors have been fixed. The codebase now uses structured logging (`org.apache.juneau.commons.logging.Logger`) for logging purposes, while preserving direct console output where it's intentional (utility classes, tests, etc.).

## Files Modified

### Core Files
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/swagger/BasicSwaggerProviderSession.java`
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java`
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/RestObject.java`
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/RestServlet.java`
- `juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/Microservice.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/ThrowableUtils.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/Utils.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/serializer/WriterSerializer.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/SystemUtils.java`

### Suppressed Files (Intentional Console Output)
- `juneau-core/juneau-assertions/src/main/java/org/apache/juneau/assertions/Assertion.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/io/Console.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/PredicateUtils.java`

### Example Files
- Multiple example files in `juneau-examples/` directories

## Next Steps

1. **Verify All Changes**: Run tests to ensure all changes work correctly
2. **Check SonarLint**: Verify that SonarLint warnings are resolved
3. **Continue with Other SonarLint Issues**: If there are other SonarLint issues to address, proceed with those

## Notes

- Always use `org.apache.juneau.commons.logging.Logger` for logging (not `java.util.logging.Logger`)
- When direct console output is intentional (utilities, tests), use `@SuppressWarnings("java:S106")` at class level
- Tests that capture `System.err` output require `System.err.println()` to remain (use suppression instead of replacement)
- Logger registration in bean stores may need both Juneau and standard Java logger types for compatibility
