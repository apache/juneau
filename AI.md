# AI Assistant Rules for Apache Juneau

This document outlines the rules, guidelines, and best practices that AI assistants follow when working on the Apache Juneau project.

**Note**: This file is referred to as "my rules" and serves as the definitive reference for all guidelines and conventions I follow when working on the Apache Juneau project.

**Important**: Also read `AI_SESSION.md` to understand the current session's work context, what we're currently working on, and any recent changes or patterns established in this session.

**Documentation Separation Rule**: `AI_SESSION.md` should only contain session-specific information that is not already covered in `AI.md`. General rules, permanent conventions, and best practices belong in `AI.md`. Session-specific work progress, current tasks, and temporary patterns belong in `AI_SESSION.md`.

## User Commands

### Shorthand Commands
- **"c"** means **"continue"** - When the user sends just "c", continue with the current task or work
- **"s"** means **"status"** - When the user sends just "s", give a status update on what you're currently working on
- **"TODO-x"** means **"work on this TODO"** - When the user sends just "TODO-3", "TODO-67", etc., start working on that specific TODO item from the TODO.md file

### Script Shortcut Commands
- **"start docs"** or **"start docusaurus"** - Runs `scripts/start-docusaurus.py`
- **"revert staged"** - Runs `scripts/revert-staged.py`
- **"revert unstaged"** - Runs `scripts/revert-unstaged.py`
- **"start jetty"** - Runs `scripts/start-examples-rest-jetty.py`
- **"start springboot"** - Runs `scripts/start-examples-springboot.py`
- **"push [commit message]"** - Runs `scripts/build-and-push.py` with the commit message. Example: "push Added Algolia search"
- **"test"** - Runs `scripts/build-and-test.py`

### Documentation Commands
- **"save a rule"** or **"save this rule"** - Add the rule/information to `AI.md` (permanent/general)
- **"store this rule in the session"** - Add the rule/information to `AI_SESSION.md` (session-specific)
- **"store this rule in the context"** - Add the rule/information to `AI.md` (permanent/general)

## Core Working Principles

### 1. Code Quality and Consistency
- Follow existing code patterns and conventions
- Maintain consistency with the existing codebase
- Use established naming conventions and formatting
- Preserve existing functionality while making improvements

#### Indentation Rules
When adding or modifying Java code, **ALWAYS** preserve the exact indentation of the surrounding code:

**Critical Requirements:**
1. **Use tabs, not spaces** - Java files in this project use tab characters for indentation
2. **Match surrounding code exactly** - Copy the leading whitespace character-for-character from the `old_string`
3. **Preserve indentation levels** - Count tabs in nearby lines and use the same number

**Common Patterns:**
```java
<TAB>/**
<TAB> * Javadoc line 1
<TAB> * Javadoc line 2
<TAB> */
<TAB>@Annotation
<TAB>public ReturnType methodName() {
<TAB><TAB>return statement;
<TAB>}
```

Where `<TAB>` represents an actual tab character (`\t`), not spaces.

**Why This Matters:**
- Incorrect indentation (offset by one tab to the left) is a common error
- Mixed tabs/spaces cause inconsistent formatting
- IDE auto-formatting relies on consistent indentation
- Project standards mandate tab-based indentation

**Verification Steps:**
1. Before creating `new_string`, examine the indentation in `old_string`
2. Count the tab characters at the start of each line
3. Copy the exact indentation pattern (including all tabs)
4. For new Javadoc, match the indentation of nearby methods

**Example Correction:**
```java
// WRONG - Missing leading tab
/**
 * Method description
 */
public void method() {

// CORRECT - Includes leading tab
<TAB>/**
<TAB> * Method description
<TAB> */
<TAB>public void method() {
```

#### Exception Handling
When throwing exceptions in Java code:

1. **Use `ThrowableUtils` methods** instead of direct exception constructors:
   - Use `illegalArg(String message, Object... args)` for `IllegalArgumentException`
   - Use `runtimeException(String message, Object... args)` for generic `RuntimeException`
   - Add static import: `import static org.apache.juneau.common.utils.ThrowableUtils.*;`

2. **Use MessageFormat-style placeholders** with `ThrowableUtils` methods:
   - Use `{0}`, `{1}`, etc. for parameter placeholders
   - Escape single quotes with `''` (e.g., `"Value ''{0}'' is invalid"`)
   - Pass arguments as varargs after the message string

**Examples:**
```java
// WRONG - Direct exception constructor
throw new IllegalArgumentException("Value '" + value + "' is invalid");

// CORRECT - ThrowableUtils with MessageFormat
throw illegalArg("Value ''{0}'' is invalid", value);

// WRONG - String concatenation
throw new RuntimeException("Failed to process " + name + " with id " + id);

// CORRECT - ThrowableUtils with multiple arguments
throw runtimeException("Failed to process {0} with id {1}", name, id);
```

#### Local Variable Type Inference
When declaring local variables in Java code:

1. **Use the `var` keyword** whenever the type is obvious from the right-hand side:
   - Initializers with constructor calls: `var map = new HashMap<String,Integer>();`
   - Method calls with clear return types: `var list = getList();`
   - Stream operations: `var result = stream.collect(toList());`
   - Enhanced for loops: `for (var entry : map.entrySet())`

2. **Keep explicit types** when:
   - The type is not obvious from the initializer: `InputStream stream = getStream();`
   - Readability would suffer: `boolean hasNext = iterator.hasNext();` (better than `var hasNext`)
   - Generic type parameters need to be preserved on left side: `List<String> list = new ArrayList<>();`

**Examples:**
```java
// WRONG - Redundant type declaration
Map<String,Integer> map = new HashMap<String,Integer>();
List<String> keys = map.keySet();

// CORRECT - Use var
var map = new HashMap<String,Integer>();
var keys = map.keySet();
```

#### Final Fields and Memoization Pattern
When declaring class fields, always use `final` to ensure true immutability:

1. **Always use `final` for fields**:
   - All instance fields should be declared `final` whenever possible
   - This provides compile-time immutability guarantees
   - Prevents accidental modification after construction

2. **Use `find` helper methods for memoized fields**:
   - When memoized `Supplier` fields need constructor parameters, use helper methods
   - Name helper methods with the `find` prefix (e.g., `findGenericInterfaces()`)
   - Helper methods are called during field initialization and can access constructor parameters
   - This allows final fields to depend on constructor parameters

3. **Why this matters:**
   - Provides stronger immutability guarantees than "effectively final" comments
   - Compiler enforces immutability rather than relying on conventions
   - Makes the code more maintainable and less error-prone
   - Documents thread-safety guarantees explicitly

**Examples:**
```java
// WRONG - Non-final field with comment
Class<?> c;  // Effectively final

// CORRECT - Final field with helper method pattern
public class ClassInfo {
	private final Class<?> c;
	
	// Final supplier initialized using helper method that accesses constructor parameter
	private final Supplier<List<Type>> genericInterfacesCache = memoize(this::findGenericInterfaces);
	
	public ClassInfo(Class<?> c) {
		this.c = c;
	}
	
	// Helper method called during field initialization
	private List<Type> findGenericInterfaces() {
		return c == null ? Collections.emptyList() : u(l(c.getGenericInterfaces()));
	}
}
```

**Pattern Summary:**
1. Declare constructor parameters as `final` fields
2. Create `find` helper methods that use those fields
3. Initialize memoized `Supplier` fields with method references to the helper methods
4. This allows all fields to be truly `final` while still supporting lazy initialization

#### Git Operations and Reverts
When working with git operations, especially reverting changes:

1. **Use helper scripts for reverting** - Always use the provided Python scripts instead of git commands directly:
   - ✅ CORRECT: `./scripts/revert-unstaged.py path/to/specific/File.java` (reverts unstaged to staged)
   - ✅ CORRECT: `./scripts/revert-staged.py path/to/specific/File.java` (reverts to HEAD, discards all changes)
   - ❌ WRONG: Using `git restore`, `git checkout`, or any git commands directly

2. **Revert Unstaged Changes Script** - `./scripts/revert-unstaged.py`
   - Reverts working directory changes back to the staged (INDEX) version
   - Preserves staged changes that have been tested
   - Use this when you have staged changes you want to keep
   - Command: `git restore --source=INDEX <file>`

3. **Revert Staged Changes Script** - `./scripts/revert-staged.py`
   - Reverts both staged AND unstaged changes back to HEAD (last commit)
   - ⚠️  WARNING: Discards all changes (staged and unstaged)
   - Use this when you want to completely discard all changes to a file
   - Command: `git restore --source=HEAD <file>`

4. **Always revert one file at a time** - Never use broad wildcards or multiple file paths:
   - ✅ CORRECT: `./scripts/revert-unstaged.py path/to/specific/File.java`
   - ❌ WRONG: Reverting multiple files or using wildcards

5. **Why this matters:**
   - **Preserves staged changes**: Staged changes have been tested and should not be lost
   - **Surgical precision**: Only reverts the specific problematic file's changes
   - **Safe recovery**: If a file is staged, it means it was working at that point
   - **Prevents data loss**: Won't accidentally revert good changes along with bad ones
   - **User-friendly**: Scripts provide clear feedback and prevent common mistakes

6. **Proper workflow when compilation fails:**
   - Identify the specific file(s) causing the error
   - Revert only that file's unstaged changes: `./scripts/revert-unstaged.py path/to/ProblematicFile.java`
   - Verify the revert fixed the issue (back to staged/tested version)
   - Then address that specific file's changes separately

**Examples:**
```bash
# CORRECT - Revert unstaged changes only (preserves staged)
./scripts/revert-unstaged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/objecttools/ObjectSearcher.java

# CORRECT - Revert all changes back to HEAD (discards everything)
./scripts/revert-staged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/objecttools/ObjectSearcher.java

# WRONG - Don't use git commands directly
git restore --source=INDEX path/to/file
git checkout -- path/to/file
```

**Understanding the scripts:**
- `revert-unstaged.py`: Uses `git restore --source=INDEX` to revert to staged version
- `revert-staged.py`: Uses `git restore --source=HEAD` to revert to last commit
- Both scripts handle one file at a time for safety
- Both provide clear feedback about what they're doing

### 2. Testing Standards
- Ensure comprehensive test coverage for all changes
- Follow the established unit testing patterns
- Use the Sequential Single-Letter Label Convention (SSLLC)
- Implement proper assertion patterns using `assertBean()`

### 3. Documentation Standards
- Maintain comprehensive javadoc documentation
- Follow established documentation formatting rules
- Include practical examples in documentation
- Link to relevant specifications and resources

### 4. Build and Compilation Issues

**IMPORTANT: Maven vs IDE Compilation Divergence**

When compiling code using Maven and the user compiles through their IDE (Eclipse), the compiled code can diverge, leading to unexpected behavior such as:
- Code changes not being reflected in test runs
- Old code behavior persisting despite source changes
- Stale class files causing incorrect results

**Eclipse "Build Automatically" Setting:**
The user typically has "Refresh using native hooks or polling" enabled in Eclipse, and may or may not have "Build Automatically" enabled.

**If code changes don't appear to be taking effect or are getting overwritten:**
1. **Ask the user to check if "Build Automatically" is enabled** (Project → Build Automatically menu)
2. **If enabled, politely ask them to disable it**: "Could you please disable 'Build Automatically' in Eclipse (Project → Build Automatically)?"
3. **Why this matters:**
   - When "Build Automatically" is enabled, Eclipse may rebuild files immediately after saving
   - This can conflict with Maven builds, causing files to be overwritten
   - Disabling it gives you more control over when builds occur
   - The user can manually build when needed using Ctrl+B or Project → Build Project

**Resolution Strategy:**
If you encounter situations where your code changes don't appear to be taking effect:
1. **First, ask about Eclipse "Build Automatically"** and request it be disabled if enabled
2. **Then run** `mvn clean install` to clear all compiled artifacts and rebuild fresh
3. This ensures Maven and IDE compiled code are synchronized
4. Rerun tests after the clean build to verify changes are properly reflected

**When to suspect this issue:**
- Tests fail in unexpected ways after code changes
- Behavior doesn't match recent code modifications
- Test results seem to reflect old code despite edits
- Compilation succeeds but runtime behavior is wrong
- Code changes appear to be getting overwritten or not taking effect

**Java Runtime Location:**
If you can't find Java on the file system using standard commands, look for it in the `~/jdk` folder. For example:
- Java 17 can be found at: `~/jdk/openjdk_17.0.14.0.101_17.57.18_aarch64/bin/java`
- Use this path when you need to run Java commands directly

### 5. Helper Scripts

**Build and Test Script:**
A reusable Python script is available at `scripts/build-and-test.py` for common Maven operations.

**Usage:**
```bash
# Default: Clean build + run tests
./scripts/build-and-test.py

# Build only (skip tests)
./scripts/build-and-test.py --build-only
./scripts/build-and-test.py -b

# Test only (no build)
./scripts/build-and-test.py --test-only
./scripts/build-and-test.py -t

# Full build and test (explicit)
./scripts/build-and-test.py --full
./scripts/build-and-test.py -f

# Verbose output (show full Maven output)
./scripts/build-and-test.py --verbose
./scripts/build-and-test.py -v

# Help
./scripts/build-and-test.py --help
./scripts/build-and-test.py -h
```

**What it does:**
- `--build-only/-b`: Runs `mvn clean install -q -DskipTests`
- `--test-only/-t`: Runs `mvn test -q -Drat.skip=true`
- `--full/-f` (default): Runs both build and test in sequence
- By default, shows only the last 50 lines of output
- Use `--verbose/-v` to see full Maven output

**When to use:**
- Instead of manually typing out Maven commands repeatedly
- When you need to verify both build and tests pass
- During iterative development to quickly test changes

### 6. Error Handling and Validation
- Use `assertThrowsWithMessage` for exception testing
- Test both valid and invalid scenarios
- Include proper error messages in assertions
- Test null parameter validation where applicable

### 7. Code Coverage Guidelines
- Aim for 100% instruction coverage on bean classes
- Use JaCoCo reports to identify missing coverage
- Focus on methods with 0% coverage first
- Add comprehensive tests for all code paths
- **Hard-to-Test Code Marking**: When a line of code is found to not be fully testable (e.g., requires complex setup, compiler-generated code, or unreachable branches), add a comment `// HTT` (Hard To Test) on that line to document why it's difficult to test

### 8. File Organization and Naming
- Follow established file naming conventions
- Maintain proper package structure
- Use consistent import organization
- Follow established class and method naming patterns

### 9. Documentation Links and References
- Use hardcoded links to `https://juneau.apache.org/docs/topics/`
- Include specification links for external standards where applicable
- Use proper cross-references with `{@link}` tags
- Maintain consistent link formatting

### 10. Systematic Approach
- Work through tasks systematically and alphabetically when specified
- Complete tasks without breaks when requested
- Verify all changes work correctly
- Maintain consistency across similar files

### 11. TODO List Management
- When the user says "add to TODO" or "add to the TODO list", this refers to the `TODO.md` file in the project root
- Do NOT use the in-memory todo list tool for user-requested TODO items
- Add items directly to the `TODO.md` file using the write or search_replace tools
- Follow the existing format and structure of the `TODO.md` file
- **TODO Identifiers**: When adding new TODO items, assign them a unique "TODO-#" identifier (e.g., "TODO-1", "TODO-2", etc.)
- **TODO References**: When the user asks to "fix TODO-X" or "work on TODO-X", they are referring to the specific identifier in the TODO.md file
- **TODO Completion**: When TODOs are completed, remove them from the TODO.md list entirely

### 12. Release Notes Management
- When the user says "add to release notes" or "add this to the release notes", this refers to the release notes in the `/juneau-docs` directory
- Do NOT add to the root `RELEASE-NOTES.txt` file
- For the current release (9.2.0), add entries to `/juneau-docs/9.2.0.md`
- Follow the existing format and structure of the release notes in juneau-docs
- Use the same formatting style as other entries in the release notes file

### 13. Fluent Setter Override Formatting
When adding fluent setter overrides to classes:
- Include blank lines between each method
- Each override method should be separated by exactly one blank line
- Example pattern:
  ```java
  @Override /* Overridden from ParentClass */
  public ChildClass setProperty1(Type value) {
      super.setProperty1(value);
      return this;
  }

  @Override /* Overridden from ParentClass */
  public ChildClass setProperty2(Type value) {
      super.setProperty2(value);
      return this;
  }
  ```
- **Rationale**: Maintains consistency with existing code style and improves readability

---

# Documentation Guidelines for Apache Juneau

This document outlines the documentation conventions, formatting rules, and best practices for the Apache Juneau project.

## Javadoc Formatting Standards

### HTML Tags and Formatting

**Java Variables in Examples**:
- Wrap local Java variables in `<jv>` tags
- Example: `<jv>json</jv>`, `<jv>swagger</jv>`, `<jv>result</jv>`

**Static Method References**:
- Use `<jsm>` tags for static method names
- Example: `Json.<jsm>from</jsm>(<jv>x</jv>)`

**Static Field References**:
- Use `<jsf>` tags for static field names
- Example: `JsonSerializer.<jsf>DEFAULT</jsf>`

**Code Comments**:
- Use `<jc>` tags for code comments
- Example: `<jc>// Serialize using JsonSerializer.</jc>`

**Class References**:
- Use `<jk>` tags for class names in text
- Example: `<jk>null</jk>`, `<jk>String</jk>`

### Syntax Highlighting Tags (from juneau-code.css)

**Java Code Tags**:
- `<jc>` - Java comment (green)
- `<jd>` - Javadoc comment (blue)
- `<jt>` - Javadoc tag (blue, bold)
- `<jk>` - Java keyword (purple, bold)
- `<js>` - Java string (blue)
- `<jf>` - Java field (dark blue)
- `<jsf>` - Java static field (dark blue, italic)
- `<jsm>` - Java static method (italic)
- `<ja>` - Java annotation (grey)
- `<jp>` - Java parameter (brown)
- `<jv>` - Java local variable (brown)

**XML Code Tags**:
- `<xt>` - XML tag (dark cyan)
- `<xa>` - XML attribute (purple)
- `<xc>` - XML comment (medium blue)
- `<xs>` - XML string (blue, italic)
- `<xv>` - XML value (black)

**JSON Code Tags**:
- `<joc>` - JSON comment (green)
- `<jok>` - JSON key (purple)
- `<jov>` - JSON value (blue)

**URL Encoding/UON Tags**:
- `<ua>` - Attribute name (black)
- `<uk>` - true/false/null (purple, bold)
- `<un>` - Number value (dark blue)
- `<us>` - String value (blue)

**Manifest File Tags**:
- `<mc>` - Manifest comment (green)
- `<mk>` - Manifest key (dark red, bold)
- `<mv>` - Manifest value (dark blue)
- `<mi>` - Manifest import (dark blue, italic)

**Config File Tags**:
- `<cc>` - Config comment (green)
- `<cs>` - Config section (dark red, bold)
- `<ck>` - Config key (dark red)
- `<cv>` - Config value (dark blue)
- `<ci>` - Config import (dark red, bold, italic)

**Special Tags**:
- `<c>` - Synonym for `<code>`
- `<dc>` - Deleted code (strikethrough)
- `<bc>` - Bold code (bold)

**Code Block Classes**:
- `bcode` - Bordered code block
- `bjava` - Bordered Java code block
- `bjson` - Bordered JSON code block
- `bxml` - Bordered XML code block
- `bini` - Bordered INI code block
- `buon` - Bordered UON code block
- `burlenc` - Bordered URL encoding code block
- `bconsole` - Bordered console output (black background, yellow text)
- `bschema` - Bordered schema code block
- `code` - Unbordered code block

### Javadoc Structure

**Standard Method Javadoc**:
```java
/**
 * Brief description of what the method does.
 *
 * <p>
 * Longer description if needed, explaining the purpose and behavior.
 * </p>
 *
 * @param paramName Description of the parameter.
 * @return Description of what is returned.
 * @throws ExceptionType Description of when this exception is thrown.
 */
```

**Property Documentation**:
```java
/**
 * The property name.
 *
 * @param value The new value for this property.
 * @return This object.
 */
```

### Parameter Documentation

**Standard Parameters**:
- Use `value` as the parameter name for fluent setters
- Document parameter types and constraints
- Specify when parameters can be null

**Builder Method Parameter Naming**:
- For single-value setter methods in builder classes, use `value` as the parameter name
- This allows field assignment without the `this.` modifier (e.g., `field = value` instead of `this.field = value`)
- Improves code readability and reduces redundancy

**Null Parameter Handling**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Can be <jk>null</jk> to unset the property.
 * @return This object.
 */
```

**Required Parameters**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Must not be <jk>null</jk>.
 * @return This object.
 */
```

## Link and Reference Standards

### Documentation Links

**Juneau Documentation Site**:
- Use hardcoded links to `https://juneau.apache.org/docs/topics/`
- Use slug names as topic names
- Use page titles for anchor text

**Examples**:
```java
/**
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ModuleName">module-name</a>
 * </ul>
 */
```

**External Specification Links**:
```java
/**
 * <a class="doclink" href="https://example.com/specification/#property">property</a> description.
 */
```

### Cross-References

**Internal Class References**:
- Use `{@link ClassName}` for internal class references
- Use `{@link ClassName#methodName}` for method references

**External References**:
- Use `<a class="doclink" href="URL">text</a>` for external links
- Include specification links for external standards where applicable

## Code Examples

### JSON Serialization Examples

**Standard Pattern**:
```java
/**
 * <jc>// Serialize using JsonSerializer.</jc>
 * String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * <jc>// Or just use toString() which does the same as above.</jc>
 * String <jv>json</jv> = <jv>x</jv>.toString();
 */
```

**Consistency Requirements**:
- Always include both `Json.from()` and `toString()` examples
- Use consistent variable names (`<jv>json</jv>`, `<jv>x</jv>`)
- Include explanatory comments

### Method Usage Examples

**Fluent Setter Examples**:
```java
/**
 * <jc>// Create a link element</jc>
 * A <jv>link</jv> = a().href("https://example.com").target("_blank");
 */
```

**Builder Pattern Examples**:
```java
/**
 * <jc>// Create a Swagger document</jc>
 * Swagger <jv>swagger</jv> = swagger()
 *     .info(info().title("My API").version("1.0"))
 *     .path("/users", pathItem().get(operation().summary("Get users")));
 */
```

## Property Documentation

### Bean Properties

**Standard Property Documentation**:
```java
/**
 * The property description.
 *
 * @param value The new value for this property.
 * @return This object.
 */
```

**Property with External Link Documentation**:
```java
/**
 * The <a class="doclink" href="https://example.com/spec#property">property</a> description.
 *
 * @param value A description of the parameter.
 * @return This object.
 */
```

## Class Documentation

### Class-Level Javadoc

**Standard Structure**:
```java
/**
 * Brief description of the class purpose.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ModuleName">module-name</a>
 * </ul>
 */
```

**Builder Classes**:
```java
/**
 * Builder class for creating {@link ClassName} objects.
 *
 * <p>
 * This class provides fluent methods for building complex objects.
 * </p>
 */
```

## Validation and Error Documentation

### Parameter Validation

**Null Parameter Validation**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Must not be <jk>null</jk>.
 * @throws IllegalArgumentException If value is <jk>null</jk>.
 * @return This object.
 */
```

**String Validation**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Must not be <jk>null</jk> or blank.
 * @throws IllegalArgumentException If value is <jk>null</jk> or blank.
 * @return This object.
 */
```

### Strict Mode Documentation

**Strict Mode Behavior**:
```java
/**
 * Sets the property value.
 *
 * <p>
 * In strict mode, invalid values will throw {@link RuntimeException}.
 * In non-strict mode, invalid values are ignored.
 * </p>
 *
 * @param value The new value.
 * @return This object.
 */
```

## Special Cases

### Constructor Documentation

**Standard Constructor**:
```java
/**
 * Constructor.
 *
 * @param children The child nodes.
 */
public ClassName(Object...children) {
```

**Parameterized Constructor**:
```java
/**
 * Constructor.
 *
 * @param param1 Description of first parameter.
 * @param param2 Description of second parameter.
 */
public ClassName(Type1 param1, Type2 param2) {
```

### Override Documentation

**Method Overrides**:
```java
@Override /* <SimpleClassName> */
public ClassName methodName(Type value) {
```

**Interface Implementations**:
```java
@Override /* <SimpleClassName> */
public ClassName methodName(Type value) {
```

## Best Practices

### Consistency
- Use consistent parameter names (`value` for fluent setters)
- Use consistent variable names in examples
- Use consistent link formatting

### Completeness
- Document all public methods and constructors
- Include parameter types and constraints
- Include return value descriptions
- Include exception documentation

### Clarity
- Use clear, concise descriptions
- Include practical examples
- Link to relevant specifications
- Explain complex behavior

### Maintenance
- Keep documentation up-to-date with code changes
- Use consistent formatting throughout
- Include cross-references where helpful
- Document edge cases and special behavior

## Common Patterns

### Fluent Setter Documentation
```java
/**
 * Sets the property value.
 *
 * @param value The new value for this property.
 * @return This object.
 */
public ClassName property(Type value) {
```

### Collection Setter Documentation
```java
/**
 * Sets the collection of items.
 *
 * @param value The new collection. Can be <jk>null</jk> to unset the property.
 * @return This object.
 */
public ClassName items(Collection<Item> value) {
```

### Builder Method Documentation
```java
/**
 * Creates a new instance.
 *
 * @return A new instance.
 */
public static ClassName create() {
```

This document serves as the definitive guide for documentation in the Apache Juneau project, ensuring consistency, completeness, and clarity across all documentation.

---

# Unit Testing Guidelines for Apache Juneau

This document outlines the testing conventions, methodologies, and best practices for unit testing in the Apache Juneau project.

## Core Testing Conventions

### Sequential Single-Letter Label Convention (SSLLC)

**Purpose**: Provides consistent, readable naming for test data that improves test maintainability and readability.

**Rules**:
1. **Single values**: Use single letters (`a`, `b`, `c`, etc.)
2. **Multiple values on same line**: Append numbers (`a1`, `a2`, `b1`, `b2`, etc.)
3. **New bean instances**: Reset labels to 'a' when creating new bean instances
4. **Consistent prefixes**: Use consistent prefixes for multiple values on the same line

**Examples**:
```java
// Single values
var a = swagger();
var b = info();

// Multiple values on same line
var a1 = operation("get", "/users");
var a2 = operation("post", "/users");
var b1 = response("200");
var b2 = response("400");

// New bean instance - reset to 'a'
var a = new Swagger();
var b = a.info();
```

### Deep Property Path Assertion Pattern (DPPAP)

**Purpose**: Assert on actual nested property values using deep property paths rather than just collection sizes.

**Usage**: Use `assertBean()` with deep property paths to verify actual values, not just collection sizes.

**Examples**:
```java
// Instead of just checking size
assertNotNull(swagger.getPaths());
assertEquals(2, swagger.getPaths().size());

// Use deep property path assertions
assertBean(swagger, "paths{get:/users{summary,operationId},post:/users{summary,operationId}}");
```

### TestUtils Convenience Methods

**Purpose**: Use TestUtils methods instead of direct serializer/parser calls for consistency and readability.

**Methods**:
- `TestUtils.json(Object)` - Serialize object to JSON string
- `TestUtils.json(String, Class)` - Deserialize JSON string to object
- `TestUtils.jsonRoundTrip(Object, Class)` - Round-trip serialization/deserialization testing

**Examples**:
```java
// Instead of direct serializer calls
String json = Json5Serializer.DEFAULT.toString(swagger);
Swagger parsed = Json5Parser.DEFAULT.parse(json, Swagger.class);

// Use TestUtils convenience methods
String json = json(swagger);
Swagger parsed = json(json, Swagger.class);
Swagger roundTrip = jsonRoundTrip(swagger, Swagger.class);
```

## Test Structure Patterns

### Test Class Naming Convention

All test methods follow the pattern: `LNN_testName` where:
- **L** is a letter from 'a'-'z' representing the test category
- **NN** is a number from "00"-"99" for ordering tests within a category
- **testName** is a descriptive name for what the test does

**Examples:**
- `a01_basicTest()` - First test in category 'a'
- `b05_serializationTest()` - Fifth test in category 'b'
- `c12_validationTest()` - Twelfth test in category 'c'

### Simple Test Class Structure

For test classes with a small number of tests:

```java
public class BeanName_Test extends TestBase {
    
    @Test void a01_basicPropertyTest() {
        // Test basic properties
    }
    
    @Test void a02_fluentSetters() {
        // Test fluent setter methods
    }
    
    @Test void b01_serialization() {
        // Test JSON serialization
    }
    
    @Test void b02_deserialization() {
        // Test JSON deserialization
    }
}
```

**Key Points:**
- Tests are grouped by letter prefix (a, b, c, etc.)
- Each group represents a general test category
- Tests within a group are numbered sequentially

### Complex Test Class Structure

For test classes with large numbers of tests that can be grouped into major categories:

```java
public class BeanName_Test extends TestBase {
    
    @Nested class A_basicTests extends TestBase {
        @Test void a01_properties() {
            // Test bean properties using varargs setters
            // Use SSLLC naming convention
            // Use DPPAP for assertions
        }
        
        @Test void a02_fluentSetters() {
            // Test fluent setter chaining
        }
    }
    
    @Nested class B_serialization extends TestBase {
        @Test void b01_toJson() {
            // Test JSON serialization
            // Use TestUtils convenience methods
        }
        
        @Test void b02_fromJson() {
            // Test JSON deserialization
        }
    }
    
    @Nested class C_extraProperties extends TestBase {
        @Test void c01_dynamicProperties() {
            // Test set(String, Object) method
            // Use set(String, Object) for ALL the same properties as A_basicTests
            // Match values from A_basicTests exactly
        }
    }
    
    @Nested class D_additionalMethods extends TestBase {
        @Test void d01_collectionSetters() {
            // Test setX(Collection<X>) methods
        }
        
        @Test void d02_varargAdders() {
            // Test addX(X...) methods
        }
    }
}
```

**Key Points:**
- Use `@Nested` inner classes for major test categories
- Each nested class name follows the pattern: `L_categoryName`
- Tests within nested classes still use `LNN_testName` pattern
- The letter in the nested class name matches the letter in the test method names

### Common Test Categories

While not prescriptive, common test category prefixes include:
- **a**: Basic tests (properties, constructors, basic functionality)
- **b**: Serialization/deserialization tests
- **c**: Extra/dynamic properties tests
- **d**: Additional methods tests
- **e**: Validation/strict mode tests
- **f**: Reference resolution tests (where applicable)

Choose the structure (Simple vs Complex) based on:
- **Simple**: < 20 tests, or tests don't naturally group into major categories
- **Complex**: > 20 tests, especially when tests group into distinct functional areas

## Assertion Patterns

### assertBean Usage

**Basic Usage**:
```java
assertBean(bean, "property1,property2,property3");
```

**Deep Property Paths**:
```java
assertBean(bean, "paths{get:/users{summary,operationId}}");
```

**Collection Assertions**:
```java
// Use # notation for uniform collections
assertBean(bean, "parameters{#{in,name}}");

// Use explicit indexing for non-uniform collections
assertBean(bean, "parameters{0{in,name},1{in,name}}");
```

**Map Assertions**:
```java
// Use assertMap for map entries
assertMap(map, "key1=value1", "key2=value2");
```

### Exception Testing

**Use assertThrowsWithMessage**:
```java
assertThrowsWithMessage(IllegalArgumentException.class, 
    "Parameter 'name' cannot be null", 
    () -> bean.setName(null));
```

## Bean Testing Patterns

### General Bean Testing

**Property Coverage**: Ensure `A_basicTests` covers all bean properties and `C_extraProperties` covers the same properties using the `set()` method.

**Getter/Setter Variants**: Test both collection variants and varargs variants where applicable.

### Fluent Setter Testing

**Parameter Naming**: All fluent setters should use `value` as the parameter name for consistency.

**Method Chaining**: Test fluent setter chaining:
```java
var result = bean
    .property1("value1")
    .property2("value2")
    .property3("value3");
```

### Collection Property Method Consistency

**Rule**: All collection bean properties should have exactly 4 methods:
1. `setX(X...)` - varargs setter
2. `setX(Collection<X>)` - Collection setter  
3. `addX(X...)` - varargs adder
4. `addX(Collection<X>)` - Collection adder

**Examples**:
```java
// For a tags property of type Set<String>:
public Bean setTags(String...value) { ... }
public Bean setTags(Collection<String> value) { ... }
public Bean addTags(String...values) { ... }
public Bean addTags(Collection<String> values) { ... }
```

### Varargs vs Collection Setter Testing

**Rule**: When a bean has both varargs and Collection setter methods for the same property:
- **A_basicTests**: Use the varargs version (e.g., `setTags("tag1", "tag2")`)
- **D_additionalMethods**: Test the Collection version (e.g., `setTags(list("tag1", "tag2"))`)

**Examples**:
```java
// A_basicTests - use varargs
.setTags("tag1", "tag2")
.setConsumes(MediaType.of("application/json"))

// D_additionalMethods - test Collection version
.setTags(list("tag1", "tag2"))
.setConsumes(list(MediaType.of("application/json"), MediaType.of("application/xml")))
```

**D_additionalMethods Test Structure**: This test class should contain three tests:

1. **d01_collectionSetters**: Tests `setX(Collection<X>)` methods
   ```java
   @Test void d01_collectionSetters() {
       var x = bean()
           .setTags(list("tag1", "tag2"))
           .setConsumes(list(MediaType.of("application/json"), MediaType.of("application/xml")));
       
       assertBean(x,
           "tags,consumes",
           "[tag1,tag2],[application/json,application/xml]"
       );
   }
   ```

2. **d02_varargAdders**: Tests `addX(X...)` methods - each method should be called twice
   ```java
   @Test void d02_varargAdders() {
       var x = bean()
           .addTags("tag1")
           .addTags("tag2")
           .addConsumes(MediaType.of("application/json"))
           .addConsumes(MediaType.of("application/xml"));
       
       assertBean(x,
           "tags,consumes",
           "[tag1,tag2],[application/json,application/xml]"
       );
   }
   ```

3. **d03_collectionAdders**: Tests `addX(Collection<X>)` methods - each method should be called twice
   ```java
   @Test void d03_collectionAdders() {
       // Note: Collection versions of addX methods exist but are difficult to test
       // due to Java method resolution preferring varargs over Collection
       // For now, we test the basic functionality with varargs versions
       var x = bean();
       
       // Test that the addX methods work by calling them multiple times
       x.addTags("tag1");
       x.addTags("tag2");
       x.addConsumes(MediaType.of("application/json"));
       x.addConsumes(MediaType.of("application/xml"));
       
       assertBean(x,
           "tags,consumes",
           "[tag1,tag2],[application/json,application/xml]"
       );
   }
   ```

In all cases, `assertBean` should be used to validate results.

## Code Coverage Guidelines

### Target Coverage
- **Bean Classes**: Aim for 100% instruction coverage
- **UI Classes**: Can be excluded from coverage targets
- **Builder Classes**: Include comprehensive tests for all builder methods

### Coverage Analysis
- Use JaCoCo reports to identify missing coverage
- Focus on methods with 0% coverage first
- Add tests for uncovered code paths
- Verify coverage improvements after adding tests

## Best Practices

### Test Data Management
- Use SSLLC for consistent test data naming
- Reset labels when creating new bean instances
- Use meaningful test data that represents real-world scenarios

### Assertion Strategy
- Prefer `assertBean()` over individual property assertions
- Use deep property paths for comprehensive validation
- Test both positive and negative scenarios

### Code Organization
- Group related tests in logical test methods
- Use descriptive test method names
- Follow consistent test structure across all test classes

### Documentation
- Include javadoc for test methods explaining their purpose
- Document any special test scenarios or edge cases
- Keep test code readable and maintainable

## Common Patterns

### Round-trip Testing
```java
@Test void B_serialization() {
    var original = createTestBean();
    var roundTrip = jsonRoundTrip(original, BeanClass.class);
    assertBean(original, roundTrip);
}
```

### Strict Mode Testing
```java
@Test void E_strictMode() {
    // Test invalid value with strict mode
    assertThrowsWithMessage(RuntimeException.class, 
        "Invalid value", 
        () -> bean.setProperty("invalid"));
    
    // Test valid value with strict mode
    assertDoesNotThrow(() -> bean.setProperty("valid"));
}
```

### Collection Testing
```java
@Test void a08_otherGettersAndSetters() {
    var a = bean.addItems("item1", "item2");
    assertBean(a, "items{0=item1,1=item2}");
    
    var b = bean.setItems(Arrays.asList("item3", "item4"));
    assertBean(b, "items{0=item3,1=item4}");
}
```

## Serializer and Parser Implementation Rules

### Adding Settings to Serializers/Parsers

When adding a new setting (configuration property) to a serializer or parser, follow these steps:

#### 1. Add Field to Builder Class
```java
public static class Builder extends XmlSerializer.Builder {
    String textNodeDelimiter;  // Add the field
}
```

#### 2. Initialize in Constructor
```java
protected Builder() {
    textNodeDelimiter = env("XmlSerializer.textNodeDelimiter", "");  // Set default
}
```

#### 3. Add to Copy Constructors
```java
protected Builder(XmlSerializer copyFrom) {
    super(copyFrom);
    textNodeDelimiter = copyFrom.textNodeDelimiter;  // Copy from serializer
}

protected Builder(Builder copyFrom) {
    super(copyFrom);
    textNodeDelimiter = copyFrom.textNodeDelimiter;  // Copy from builder
}
```

#### 4. Add Setter Method
```java
public Builder textNodeDelimiter(String value) {
    textNodeDelimiter = value == null ? "" : value;
    return this;
}
```

#### 5. **CRITICAL: Update hashKey() Method**
This is essential to prevent caching issues where different configurations would incorrectly share the same cached instance:

```java
@Override /* Context.Builder */
public HashKey hashKey() {
    return HashKey.of(
        super.hashKey(),
        addBeanTypesXml,
        addNamespaceUrisToRoot,
        // ... other fields ...
        textNodeDelimiter  // ADD NEW SETTING HERE
    );
}
```

**Why this is critical**: Serializers and parsers use caching based on the hash key. If a new setting is not included in the hash key, two builders with different values for that setting will hash to the same key and incorrectly use the same cached instance, causing the second configuration to be ignored.

#### 6. Add Field to Main Class
```java
public class XmlSerializer extends WriterSerializer {
    final String textNodeDelimiter;  // Add to main class
    
    public XmlSerializer(Builder builder) {
        super(builder);
        textNodeDelimiter = builder.textNodeDelimiter;  // Initialize from builder
    }
}
```

#### 7. Pass to Session (if needed)
If the setting needs to be accessed during serialization:

```java
public class XmlSerializerSession extends WriterSerializerSession {
    private final String textNodeDelimiter;
    
    protected XmlSerializerSession(Builder builder) {
        super(builder);
        textNodeDelimiter = ctx.textNodeDelimiter;  // Get from context
    }
}
```

#### 8. Override in Subclasses (if needed)
If the serializer has subclasses (e.g., `HtmlSerializer` extends `XmlSerializer`), override the setter to maintain fluent API:

```java
// In HtmlSerializer.Builder
@Override
public Builder textNodeDelimiter(String value) {
    super.textNodeDelimiter(value);  // Call parent
    return this;  // Return correct type
}
```

### Common Pitfalls
- ❌ **Forgetting to add the setting to `hashKey()`** - This causes caching bugs where different configurations share the same cached instance
- ❌ Not copying the field in all copy constructors
- ❌ Not overriding setter methods in subclass builders
- ❌ Not passing the setting to the session if it's needed during serialization

This document serves as the definitive guide for unit testing in the Apache Juneau project, ensuring consistency, maintainability, and comprehensive test coverage.

---

## Release Notes Guidelines

### Location
When asked to "add to the release notes", this refers to the current release file located at:
- `/juneau-docs/docs/release-notes/<VERSION>.md`
- **Current version**: `9.2.0`
- **Current file**: `/juneau-docs/docs/release-notes/9.2.0.md`

### Structure
Release notes are organized into two main sections:

1. **Top-level major changes** - High-level overview at the beginning of the file listing significant changes
2. **Per-module updates** - Detailed changes organized by module (similar to 9.0.0.md structure):
   - `juneau-marshall`
   - `juneau-rest-common`
   - `juneau-rest-server`
   - `juneau-rest-client`
   - `juneau-dto`
   - `juneau-microservice`
   - `juneau-examples`
   - Other modules as applicable

### Format
Each section should include:
- New features
- Bug fixes
- Breaking changes
- Deprecations
- Performance improvements
- Documentation updates
- API changes

### Process
1. Read the current release notes file (9.2.0.md) to understand the existing structure
2. Determine if the change is a major change (top-level) or module-specific
3. Add new entries under the appropriate section and module
4. Use clear, concise descriptions with code examples where helpful
5. Include issue/PR references where applicable
6. Maintain consistent formatting with existing entries (see 9.0.0.md for reference)
