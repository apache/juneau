# Plan: Convert Juneau System Properties to Settings Class

## Overview

This plan documents all locations where Juneau defines or uses system properties and provides a migration strategy to convert them to use the `org.apache.juneau.commons.settings.Settings` class. The Settings class provides:

- **Thread-safe access** with support for global and per-thread overrides (critical for unit tests)
- **Unified lookup** through system properties, environment variables, and custom sources
- **Type conversion** via `asBoolean()`, `asInteger()`, etc.
- **Testability** - tests can use `Settings.get().setLocal()` without affecting other tests

## Reference: Settings API

**Getting values:**
```java
// String with default
String value = Settings.get().get("property.name").orElse("default");
// Or with typed default:
String value = Settings.get().get("property.name", "default");

// Typed access
Boolean flag = Settings.get().get("property.name").asBoolean().orElse(false);
Integer count = Settings.get().get("property.name").asInteger().orElse(100);

// Via Utils.env() - already delegates to Settings
T value = Utils.env("property.name", defaultValue);
```

**For unit tests:**
```java
Settings.get().setLocal("property.name", "test-value");
try {
    // test code
} finally {
    Settings.get().clearLocal();
}
```

## Current State Analysis

### Already Using Settings (No Conversion Needed)

| Component | Method | Notes |
|-----------|--------|-------|
| **ThrowableUtils** | `Settings.get().get("juneau.enableVerboseExceptions").asBoolean()` | Reference implementation |
| **Cache** (Cache, Cache2, Cache3, Cache4, Cache5) | `Utils.env("juneau.cache.mode", ...)` | Utils.env delegates to Settings |
| **CallLogger** | `Utils.env(SP_*, ...)` | Uses SP_ constants, env() delegates to Settings |
| **BeanContext** | `BeanContext.locale`, `mediaType`, `timeZone` from Settings | BeanContext_Test verifies |
| **Utils.env()** | `Settings.get().get(name)` | Central entry point |

### Direct System.getProperty() Usage - Conversion Candidates

#### Priority 1: juneau.* Properties (Production Code)

| # | File | Property | Current Usage | Conversion Approach |
|---|------|----------|---------------|---------------------|
| 1 | **Microservice.java** | `juneau.workingDir` | `System.getProperty("juneau.workingDir")` | `Settings.get().get("juneau.workingDir").orElse(null)` or `Settings.get().get("juneau.workingDir", (File)null)` |
| 2 | **JettyMicroservice.java** | `availablePort` | `System.getProperty("availablePort")` | `Settings.get().get("availablePort")` - Note: set by test framework |
| 3 | **JettyMicroservice.java** | `juneau.serverPort` | `System.getProperty("juneau.serverPort")` | `Settings.get().get("juneau.serverPort")` |
| 4 | **StringFormat.java** | `juneau.StringFormat.caching` | `CacheMode.parse(System.getProperty("juneau.StringFormat.caching", "FULL"))` | `Settings.get().get("juneau.StringFormat.caching", "FULL")` then parse, or `Utils.env("juneau.StringFormat.caching", CacheMode.FULL)` (if CacheMode has fromString) |
| 5 | **XorEncodeMod.java** | `org.apache.juneau.config.XorEncoder.key` | `System.getProperty(..., "nuy7og796Vh6G9O6bG230SHK0cc8QYkH")` | `Settings.get().get("org.apache.juneau.config.XorEncoder.key", "nuy7og796Vh6G9O6bG230SHK0cc8QYkH")` |
| 6 | **Config.java** | `juneau.configFile` | `System.getProperty("juneau.configFile")` | `Settings.get().get("juneau.configFile")` |
| 7 | **Config.java** | `sun.java.command` | `System.getProperty("sun.java.command", "not_found")` | **EXCLUDE** - JVM standard property; Settings includes system props so will work, but consider leaving as-is for clarity |
| 8 | **AnnotationProvider.java** | `juneau.annotationProvider.caching` | `CacheMode.parse(System.getProperty(..., "FULL"))` | `Utils.env("juneau.annotationProvider.caching", CacheMode.FULL)` |
| 9 | **AnnotationProvider.java** | `juneau.annotationProvider.caching.logOnExit` | `bool(System.getProperty(...))` | `Utils.env("juneau.annotationProvider.caching.logOnExit", false)` |
| 10 | **ArgsVar.java** | `sun.java.command` | `System.getProperty("sun.java.command")` | **EXCLUDE** - JVM standard |
| 11 | **ArgsVar.java** | `juneau.args` | `System.getProperty("juneau.args", "")` | `Settings.get().get("juneau.args", "")` |
| 12 | **ParameterInfo.java** | `juneau.disableParamNameDetection` | `Boolean.getBoolean("juneau.disableParamNameDetection")` via ResettableSupplier | Convert to `Settings.get().get("juneau.disableParamNameDetection").asBoolean().orElse(false)` - requires ResettableSupplier replacement with Setting reset |
| 13 | **SystemUtils.java** | `juneau.shutdown.quiet` | `Boolean.getBoolean("juneau.shutdown.quiet")` in static block | `Settings.get().get("juneau.shutdown.quiet").asBoolean().orElse(false)` - **CAUTION**: static init, Settings must be initialized first |

#### Priority 2: Third-Party / JVM Properties (Evaluate)

| # | File | Property | Notes |
|---|------|----------|-------|
| 14 | **MockLogger.java** | `java.util.logging.SimpleFormatter.format` | Standard Java logging - temporarily set/restored. Consider: Settings doesn't support setProperty for Java logging; may need to stay as System.getProperty/setProperty for Java logging integration. |
| 15 | **Config.java**, **ArgsVar.java** | `sun.java.command` | JVM launcher property - document as "pass-through" even if using Settings |

#### Priority 3: Test Code

| # | File | Property | Notes |
|---|------|----------|-------|
| 16 | **ParameterInfo_Test.java** | `juneau.disableParamNameDetection` | Save/restore for test isolation - **convert to** `Settings.get().setLocal()`/`clearLocal()` |
| 17 | **Config_Test.java** | `a`, `S/b` | Assertions on Config behavior - Config sets these via Config API; verify Config uses Settings after conversion |
| 18 | **ConfigBuilder_Test.java** | `java.io.tmpdir`, `line.separator` | JVM standards - **EXCLUDE** |
| 19 | **IgnoredClasses_Test.java** | `os.name` | JVM standard - **EXCLUDE** |
| 20 | **_TestSuite.java** | `java.specification.version` | JVM standard - **EXCLUDE** |

### Properties Documented in Comments/Javadoc Only

These are already implemented via Utils.env() or Settings but worth cataloging:

- `juneau.restLogger.enabled`, `.level`, `.logger`, `.requestDetail`, `.responseDetail` - CallLogger (uses env)
- `juneau.restCallLogger.enabled` - CallLoggerRule (inherits from CallLogger)
- `juneau.cache.mode`, `juneau.cache.maxSize`, `juneau.cache.logOnExit` - Cache classes (uses env)
- `juneau.settings.disableGlobal` - Settings itself

---

## Conversion Strategy

### Phase 1: Low-Risk Conversions (Recommended First)

Convert straightforward `System.getProperty(name, default)` patterns:

1. **Microservice.java** - `juneau.workingDir`
2. **JettyMicroservice.java** - `availablePort`, `juneau.serverPort`
3. **Config.java** - `juneau.configFile`
4. **ArgsVar.java** - `juneau.args`
5. **XorEncodeMod.java** - `org.apache.juneau.config.XorEncoder.key`

### Phase 2: Caching / Enum Conversions

Requires `CacheMode` and similar to support `Settings.get().get(name, EnumValue)`:

6. **StringFormat.java** - `juneau.StringFormat.caching`
7. **AnnotationProvider.java** - `juneau.annotationProvider.caching`, `juneau.annotationProvider.caching.logOnExit`

Verify `Utils.env(name, CacheMode.FULL)` works (Settings supports Enum via valueOf).

### Phase 3: Special Cases

8. **ParameterInfo.java** - Uses `ResettableSupplier` for testability. Options:
   - Replace with `Settings.get().get("juneau.disableParamNameDetection").asBoolean()` 
   - Use `Setting.reset()` when test needs to re-read - check if Setting has reset
   - Keep ResettableSupplier but have it delegate to Settings

9. **SystemUtils.java** - Static block runs at class load. Ensure Settings is initialized (it's in same module). May have circular dependency risk if SystemUtils is used by Settings.

10. **MockLogger.java** - Uses `System.setProperty`/`clearProperty` to temporarily override Java logging format. Settings supports overrides via `setLocal`/`unsetLocal` but Java's `SimpleFormatter` reads directly from `System.getProperty`. **Recommendation**: Document as out-of-scope; Java logging integration requires System properties.

### Phase 4: Test Updates

- **ParameterInfo_Test.java**: Replace `System.setProperty`/`System.clearProperty` with `Settings.get().setLocal()`/`unsetLocal()`/`clearLocal()`
- **Config_Test.java**: After Config conversion, tests may need to use Settings overrides instead of System properties

### Phase 5: JVM Standard Properties

**Decision**: Leave `sun.java.command`, `java.io.tmpdir`, `line.separator`, `os.name`, `java.specification.version` as `System.getProperty()` - these are not Juneau-specific and Settings will return them when queried, but using System.getProperty makes the "this is a JVM property" intent clear.

---

## Implementation Checklist

### Per-File Conversion Template

For each conversion:

1. [ ] Add import: `import org.apache.juneau.commons.settings.Settings;` (or `Utils` for env)
2. [ ] Replace `System.getProperty(name)` with `Settings.get().get(name).orElse(null)` or `Utils.env(name)`
3. [ ] Replace `System.getProperty(name, default)` with `Settings.get().get(name, default)` or `Utils.env(name, default)`
4. [ ] For booleans: `Boolean.getBoolean(name)` → `Settings.get().get(name).asBoolean().orElse(false)` or `Utils.env(name, false)`
5. [ ] Update Javadoc to reference Settings/environment variables
6. [ ] Run existing tests
7. [ ] Add/update unit test using `Settings.get().setLocal()` for override scenarios

### Dependency Order

- **juneau-commons** (Settings, Utils) has no Juneau dependencies
- **juneau-config** depends on commons - Config, XorEncodeMod can use Settings
- **juneau-marshall** depends on commons - StringFormat, AnnotationProvider, ArgsVar can use Settings
- **juneau-microservice** depends on config/marshall - Microservice, JettyMicroservice can use Settings
- **SystemUtils** is in commons - ensure no circular dependency with Settings

---

## Documentation Updates

After conversion:

1. **docs/pages/topics/01.02.Marshalling.md** - Update references to "BeanContext.sortProperties" system property / env var
2. **docs/pages/topics/...** - Ensure all documented system properties list Settings as the mechanism
3. **RELEASE-NOTES.txt** - Document the migration (e.g., "juneau.shutdown.quiet" → "juneau.shutdown.verbose" flip noted in existing notes)
4. Create **SYSTEM_PROPERTIES.md** (or add to existing docs) - Comprehensive list of all Juneau properties with:
   - Property name
   - Environment variable equivalent (JUNEAU_* convention)
   - Default value
   - Description

---

## Rollback Plan

Each conversion should be a separate commit. If issues arise:

- Revert the specific commit
- Settings is backward-compatible: it reads System properties as a source, so `-Dproperty=value` continues to work
- Tests using `Settings.setLocal()` will need to be reverted with the conversion

---

## Success Criteria

- [ ] No direct `System.getProperty()` calls for Juneau-defined properties (`juneau.*`, `org.apache.juneau.*`) in production code
- [ ] All such properties accessible via `Settings.get()` or `Utils.env()`
- [ ] Unit tests can override properties via `Settings.get().setLocal()` without cross-test pollution
- [ ] Documentation updated
- [ ] All existing tests pass
