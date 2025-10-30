# Completed TODO Items

This file contains TODO items that have been completed and moved from TODO.md.

## Code Quality Improvements

- **TODO-13** ✅ Search for places in code that should be using new try-with-return syntax.
  - **Status**: COMPLETED
  - **Details**: Found and fixed 1 instance in `StringUtils.compress()`. Comprehensive search shows most code already uses this pattern correctly.

- **TODO-16** ✅ Search for calls to filteredMap() with the following pattern and alphabetize the lines.
  - **Status**: COMPLETED
  - **Details**: Alphabetized 11 `filteredMap()` instances across multiple files including `Serializer`, `Parser`, `HtmlSerializer`, `HtmlDocSerializer`, `XmlSerializer`, `XmlParser`, `RdfSerializer`, `RdfParser`, `JsonSchemaGenerator`, `BeanContext`, `RestContext`, `CallLogger`, `CallLoggerRule`.

- **TODO-18** ✅ Look for places where we concatenate strings across multiple lines and determine if they can use Java multiline strings.
  - **Status**: COMPLETED
  - **Details**: 18 text block conversions across 6 files. High-priority candidates converted. See `TODO-multiLineStrings.md` for remaining low-priority candidates.

- **TODO-28** ✅ Remove dependencies on jakarta.xml.bind-api.
  - **Status**: COMPLETED
  - **Details**: Replaced with modern Java time APIs in `StringUtils`. No dependencies remain. Updated documentation to reflect removal.

- **TODO-31** ✅ Cache should extend from ConcurrentHashMap.
  - **Status**: COMPLETED
  - **Details**: `Cache`/`Cache2`/`Cache3`/`Cache4`/`Cache5` now extend from their respective `ConcurrentHashMapXKey` classes with builder patterns and caching features (`disableCaching`, `maxSize`, `logOnExit`, default/override suppliers).

- **TODO-32** ✅ TupleXFunction classes are redundant. Replace them with FunctionX.
  - **Status**: COMPLETED
  - **Details**: Removed `Tuple2Function`, `Tuple3Function`, `Tuple4Function`, `Tuple5Function` classes. Updated all usages to use `Function2`, `Function3`, `Function4`, `Function5` instead. Tests updated and passing.

- **TODO-33** ✅ Figure out if BidiMap needs an unmodifiable mode or if it can just be wrapped in an unmodifiable wrapper.
  - **Status**: COMPLETED
  - **Details**: `BidiMap` already has built-in unmodifiable support via `builder.unmodifiable()` which uses `Collections.unmodifiableMap()` internally. No external wrapper needed.

- **TODO-34** ✅ CharValue should have the following methods: is(char), isAny(char...), isAny(String). Determine if there are other useful methods that can be added to the XValue classes.
  - **Status**: COMPLETED
  - **Details**: 
    - Added `StringValue` and `ByteValue` classes
    - Enhanced `IntegerValue`, `ShortValue`, `LongValue`, `CharValue`, `ByteValue` with: `increment()`, `decrement()`, `incrementAndGet()`, `decrementAndGet()`, `add(X)`, `addAndGet(X)`, `is(X)`, `isAny(X...)`
    - Added `isAny(String)` to `CharValue`
    - Added `isAny(X precision, X... values)` to `FloatValue` and `DoubleValue` for precision-based equality
    - Added `orElse(X)` to all `ValueX` classes
    - Added `setIf(boolean, value)` to all `ValueX` classes
    - Added `update(Function<V,V>)` to all `Value` classes for in-place modification
    - Enhanced `Value` class to mimic `Optional` with: `empty()`, `equals(Object)`, `filter(Predicate)`, `flatMap(Function)`, `get()`, `hashCode()`, `ifPresent(Consumer)`, `isPresent()`, `map(Function)`, `orElseGet(Supplier)`, `orElseThrow(Supplier)`
    - Comprehensive unit tests created for all classes

- **TODO-35** ✅ Replace instances of Objects.requireNonNull with assertArgNotNull.
  - **Status**: COMPLETED
  - **Details**: Replaced 6 instances in `Function2/3/4/5`, `ReaderInputStream`, and `BasicJettyServerFactory` where `Objects.requireNonNull` was used for parameter validation. Other instances are for internal state validation and remain unchanged. Created `assertArgsNotNull` methods for combining multiple validations.

- **TODO-36** ✅ Replace instances of Object.equals with Utils.eq.
  - **Status**: COMPLETED
  - **Details**: Replaced 5 instances across 2 files:
    - `AssertionPredicates.java`: Updated `eq(Object)`, `eq(String)`, `ne(Object)`, and `ne(String)` methods to use `Utils.eq()` instead of `Objects.equals()`.
    - `HashKey.java`: Updated `equals()` method to use `Utils.eq()` for array element comparison.
    - `TestUtils.java`: Updated documentation comment to reflect the use of `Utils.eq()`.
    - Note: `Utils.eq()` implementation itself correctly uses `Objects.equals()` as a fallback, which was not changed.

- **TODO-37** ✅ Find and replace all instances of "x != null" with "nn(x)".
  - **Status**: COMPLETED
  - **Details**: Replaced approximately 1965+ instances of `!= null` with `nn()` across production code in juneau-core, juneau-rest, juneau-bean, and juneau-microservice modules (~98.6% reduction from initial ~1992). Includes:
    - Simple variable checks: 120+ variable names covering common patterns
    - Multi-variable expressions: `type != null && onClass != null`, `beanContext != null && beanContext.cmCache != null`, etc.
    - Field access patterns: `e.value != null`, `meta.dynaProperty != null`, `builder.xxx != null`, `p.xxx != null`, `n.uri != null`, etc.
    - Method call patterns: 30+ getter methods (`getDescription()`, `getUrl()`, `getTags()`, `getName()`, `getVersion()`, etc.)
    - Ternary expressions: `value != null ? ...`, `builder != null ? ...`, `c != null ? c.getName() : ...`, etc.
    - Array elements: `store[c] != null`, `vars[i] != null`, etc.
    - Return statements: `return value != null`, `return o != null`, `return nn(getPackage())`, etc.
    - Compound expressions: `&& authority != null`, `|| field != null`, `while (pc != null &&`, etc.
    - Boolean methods: `isAbstract()`, `isInterface()`, `isPrimitive()`, etc. all converted to use `nn(c)`
    - Lambda expressions: `x -> m.get(x.inner()) != null` → `x -> nn(m.get(x.inner()))`
    - Builder patterns: `.addIf(variable != null, ...)` → `.addIf(nn(variable), ...)` for 80+ common fields across OpenAPI/Swagger beans
    - Conservative ternary replacements: simple safe patterns only (including path, partParser, schema, getName, getSimpleName patterns)
    - Method call chains: `cm.getSwap(this) != null`, `beanFilter.getBeanDictionary() != null`, etc.
    - Variable assignments: `var isResolving = varResolver != null`, `boolean isLoaded = content != null`, etc.
    - While loops: `while (e != null)`, `while (c != null)`, `while (type != null && ...)`, `} while (t != null && ...)`, `while ((key = watchService.take()) != null)`, etc.
    - Else if patterns: 10+ else-if compound expressions
    - Field assignments: `this.def = mergedFormData != null &&`, `this.def = mergedHeader != null &&`, etc.
    - Additional compound expressions: 100+ patterns with logical operators (&&, ||)
    - Boolean variable assignments: `boolean encodeEn = elementName != null`, `boolean cr = o != null && ...`, `descriptionAdded |= description != null`, etc.
    - Complex multi-condition patterns: including validation checks, filters, type checks, getter chains, annotation checks, etc.
    - OpenAPI/Swagger getter patterns: `pathItem.getDelete() != null`, `pathItem.getGet() != null`, etc. (all HTTP methods)
    - Type checking patterns: `v != null && ! valueType.getInnerClass().isInstance(v)`, `arg != null && pt.isParentOf(arg.getClass())`, etc.
    - Stream filter lambdas: `filter(x -> x != null && ...)` → `filter(x -> nn(x) && ...)`
    - Complex nested ternaries: `nn(ed) && nn(ed.getDescription()) ? ed.getDescription() : (ed != null ? ed.getUrl() : null)` → `... (nn(ed) ? ed.getUrl() : null)`
    - Annotation chains: `m.getAnnotation(Deprecated.class) != null || m.getDeclaringClass().getAnnotation(Deprecated.class) != null` → `nn(...) || nn(...)`
    - Constructor/method reflection patterns: `c.getPublicConstructor(...) != null` → `nn(c.getPublicConstructor(...))`
  - **Final Result**: Only 27 instances remain (down from initial ~1992), all intentionally preserved:
    - 1 in `Utils.isNotNull()` method definition (must remain as-is)
    - 7 in `AssertionPredicates` lambda test predicates (preserved for clarity in test expressions)
    - 17 in `AssertionUtils` with `assertArg` statements (preserved for clarity in argument validation)
    - 2 in `PartList` and `HeaderList` with `assertArg` statements (preserved for clarity)
  - **Note**: Special care taken to preserve `Utils.isNotNull()` method definition by excluding `Utils.java` from all replacements.

- **TODO-64** ✅ Add a Utils.nn(Object...) that validates that all parameters are not null.
  - **Status**: COMPLETED
  - **Details**: Added varargs `nn(Object...)` method that returns true if all parameters are not null. Method includes comprehensive Javadoc with examples. Search of codebase found no existing patterns of `nn(x) && nn(y)` chains to consolidate, likely because TODO-37's systematic replacements converted compound null checks individually.

- **TODO-65** ✅ Add Utils.isEmpty(CharSequence) which redirects to StringUtils.isEmpty(CharSequence).
  - **Status**: COMPLETED
  - **Details**: Added `isEmpty(CharSequence)` method that delegates to `StringUtils.isEmpty(CharSequence)`, along with `isBlank(CharSequence)` and `isNotEmpty(CharSequence)` overloads. Also added overloads for `isEmpty(Collection)`, `isEmpty(Map)`, `isNotEmpty(Collection)`, and `isNotEmpty(Map)`. All methods include comprehensive Javadocs.

## Code Style and Consistency

- **TODO-30** ✅ Ensure all Builder methods are consistently using "value" as setter parameter names when it's a single parameter and it's obvious what property is being set.
  - **Status**: COMPLETED
  - **Details**: Updated 13 single-parameter builder methods across 7 files to use "value" as the parameter name:
    - `FileReaderBuilder.file(File)`, `FileWriterBuilder.file(File)`, `AsciiSet.Builder.chars(String)`
    - `Messages.Builder.locale(Locale)`, `Messages.Builder.locale(String)`, `Messages.Builder.name(String)`, `Messages.Builder.parent(Messages)`
    - `FileFinder.Builder.path(Path)`, `BeanPropertyMeta.Builder.beanRegistry(BeanRegistry)`, `BeanPropertyMeta.Builder.delegateFor(BeanPropertyMeta)`, `BeanPropertyMeta.Builder.overrideValue(Object)`
    - `RestContext.Builder.config(Config)`, `StaticFiles.Builder.mimeTypes(MimeTypeDetector)`, `StaticFiles.Builder.path(Path)`
  - **Note**: RestClient/MockRestClient builder methods with Apache HttpClient technical parameters (proxy, sslContext, retryHandler, etc.) were intentionally kept with descriptive names for clarity. User also updated additional methods in `AsciiSet`, `FileFinder`, and `BeanPropertyMeta` for consistency.

- **TODO-38** ✅ Find methods in com.sfdc.irs.Utils that don't exist in org.apache.juneau.common.utils.Utils and come up with a plan to add ones that make sense.
  - **Status**: COMPLETED
  - **Details**: Reviewed methods in `com.sfdc.irs.Utils` and determined that all generally useful utility methods have already been added to `org.apache.juneau.common.utils.Utils` or other appropriate `XUtils` classes. Methods remaining in `com.sfdc.irs.Utils` are either specific to IRS functionality or have equivalent implementations already available in Juneau's utility classes.

- **TODO-55** ✅ Replace multiple instances of assertArgNotNull with assertArgsNotNull.
  - **Status**: COMPLETED
  - **Details**: Replaced 8 occurrences across 3 files:
    - `BctAssertions.java`: 6 methods with consecutive calls (assertBean, assertBeans, assertContains, assertContainsAll, assertList, assertMapped)
    - `ClassUtils.java`: 1 method (getParameterType)
    - `SimpleMap.java`: 1 constructor
    All changes compile and all tests pass.

- **TODO-56** ✅ Rename AnnotationUtils.hashCode to hash.
  - **Status**: COMPLETED
  - **Details**: Already completed before this task was added.

- **TODO-57** ✅ Add method Utils.eq(Annotation, Annotation) that calls AnnotationUtils.equals.
  - **Status**: COMPLETED
  - **Details**: Added an overload of `Utils.eq()` specifically for annotations that delegates to `AnnotationUtils.equals()` to ensure proper annotation comparison according to the annotation equality contract defined in {@link java.lang.annotation.Annotation#equals(Object)}. Also added corresponding `Utils.ne(Annotation, Annotation)` method for inequality checks.

- **TODO-58** ✅ Update Utils.hash to use AnnotationUtils.hash() for calculating hashes of annotations.
  - **Status**: COMPLETED
  - **Details**: Updated `Utils.hash()` to check if values are annotations and use `AnnotationUtils.hash()` for them, maintaining the standard hash calculation algorithm (31 * result + element hash) for consistency with `Objects.hash()`.

- **TODO-59** ✅ Move ClassUtils.cn and scn to Utils.
  - **Status**: COMPLETED
  - **Details**: Already completed before this task was added.

- **TODO-61** ✅ Console.format seems to duplicate Utils.f. Let's remove it.
  - **Status**: COMPLETED
  - **Details**: Removed the `Console.format()` method which was duplicating functionality already provided by `Utils.f()`. Updated `Console.err()` and `Console.out()` to call `Utils.f()` directly instead. Also corrected outdated javadocs that incorrectly claimed the class used Json5 marshalling when it actually just called `toString()` on arguments - now properly documents the use of `MessageFormat`.

- **TODO-62** ✅ ResourceBundleUtils.empty() appears to be unused. Let's remove it if so.
  - **Status**: COMPLETED
  - **Details**: Already completed before this task was added.

- **TODO-60** ✅ There seems to be duplication in ArrayUtils and CollectionUtils. Let's merge ArrayUtils into CollectionUtils.
  - **Status**: COMPLETED
  - **Details**: Consolidated all array utility methods into `CollectionUtils` to eliminate duplication and provide a single location for both array and collection operations.
    - Copied all 16 ArrayUtils methods to CollectionUtils (except the duplicate `last()` method which already existed)
    - Deprecated the entire `ArrayUtils` class with `@Deprecated` annotation
    - Updated class-level javadoc to direct users to `CollectionUtils`
    - Made all `ArrayUtils` methods delegate to their `CollectionUtils` counterparts
    - Added deprecation javadoc tags to all methods pointing to the new locations
    - Added "Array utilities" section in `CollectionUtils` with comprehensive javadocs
    Methods migrated: `last()`, `append()`, `asSet()`, `combine()`, `indexOf()`, `isEmptyArray()`, `isNotEmptyArray()`, `equals()`, `reverse()`, `toArray()`, `toList()`, `copyToList()`, `toObjectList()`, `toStringArray()`, `copyOf()`, `contains()`
    This change maintains full backward compatibility while consolidating functionality.

- **TODO-63** ✅ Look for places in code where ThrowableUtils.illegalArg and runtimeException can be used.
  - **Status**: COMPLETED
  - **Details**: Replaced 2 instances in `BasicBeanConverter.java`:
    - Line 743: `new RuntimeException(f("Could not find extractor..."))` → `runtimeException("Could not find extractor...")`
    - Line 765: `new IllegalArgumentException(f("Object of type {0} could not be converted to a list."))` → `illegalArg("Object of type {0} could not be converted to a list.")`
    Added static import for `ThrowableUtils` methods. These utility methods provide cleaner syntax and consistent exception creation with formatted messages.

## Static Import Refactoring

- **TODO-39** ✅ Use static imports for all method calls to AnnotationUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for AnnotationUtils methods.

- **TODO-40** ✅ Use static imports for all method calls to ArrayUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for ArrayUtils methods.

- **TODO-41** ✅ Use static imports for all method calls to AssertionUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for AssertionUtils methods.

- **TODO-42** ✅ Use static imports for all method calls to ClassUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for ClassUtils methods.

- **TODO-43** ✅ Use static imports for all method calls to CollectionUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for CollectionUtils methods.

- **TODO-44** ✅ Use static imports for all method calls to DateUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for DateUtils methods.

- **TODO-45** ✅ Use static imports for all method calls to FileUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for FileUtils methods.

- **TODO-46** ✅ Use static imports for all method calls to IOUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for IOUtils methods.

- **TODO-47** ✅ Use static imports for all method calls to PredicateUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for PredicateUtils methods.

- **TODO-48** ✅ Use static imports for all method calls to ResourceBundleUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for ResourceBundleUtils methods.

- **TODO-50** ✅ Use static imports for all method calls to SystemUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for SystemUtils methods.

- **TODO-51** ✅ Use static imports for all method calls to ThrowableUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for ThrowableUtils methods.

- **TODO-53** ✅ Use static imports for all method calls to BctUtils.
  - **Status**: COMPLETED
  - **Details**: Refactored codebase to use static imports for BctUtils methods.

## Exception Creation Utilities

- **TODO-67** ✅ Add to ThrowableUtils: unsupportedOp, ioException.
  - **Status**: COMPLETED
  - **Details**: Added exception creation methods to `ThrowableUtils` following the same pattern as `runtimeException()` and `illegalArg()`:
    - `unsupportedOp(String msg, Object...args)` - Creates `UnsupportedOperationException` with formatted message
    - `unsupportedOp(Throwable cause, String msg, Object...args)` - Creates `UnsupportedOperationException` with cause and formatted message
    - `ioException(String msg, Object...args)` - Creates `IOException` with formatted message
    - `ioException(Throwable cause, String msg, Object...args)` - Creates `IOException` with cause and formatted message
    - Also added overloads with cause parameter for existing methods:
      - `illegalArg(Throwable cause, String msg, Object...args)`
      - `runtimeException(Throwable cause, String msg, Object...args)`
    - All methods support `MessageFormat` style formatting with varargs
    - Comprehensive unit tests added covering all new methods and overloads (19 total tests)
    - Tests verify message formatting, cause chaining, exception types, and exception throwing

- **TODO-68** ✅ Replace BasicRuntimeException with ThrowableUtils.runtimeException.
  - **Status**: COMPLETED
  - **Details**: Replaced all instances of `throw new BasicRuntimeException(...)` with `throw runtimeException(...)` across the entire codebase:
    - **45 source files** updated (excluding javadocs and generated files)
    - Added `import static org.apache.juneau.common.utils.ThrowableUtils.*;` to all affected files
    - **File breakdown:**
      - juneau-core/juneau-marshall: 11 files (BeanPropertyMeta, ExecutableInfo, FieldInfo, Context, HttpPartSchema, BeanContext, SerializerSet, ParserSet, Args, XmlParserSession, Namespace)
      - juneau-core/juneau-marshall-rdf: 1 file (RdfSerializerSession)
      - juneau-core/juneau-assertions: 1 file (Assertion)
      - juneau-bean/juneau-bean-swagger-v2: 5 files (Swagger, Items, HeaderInfo, SecurityScheme, ParameterInfo)
      - juneau-bean/juneau-bean-openapi-v3: 4 files (Items, SecuritySchemeInfo, Parameter, OpenApi)
      - juneau-rest: 17 files (MockPathResolver, SerializedEntity, HttpHeaders, HttpParts, SerializedPart, BasicPart, BasicIntegerHeader, IfRange, BasicLongHeader, RetryAfter, SerializedHeader, RestRequest, ResponseHeader, RestClient, RemoteMeta, RestUtils, BeanDescription)
      - juneau-microservice: 2 files (JettyMicroservice, Microservice)
      - juneau-examples: 1 file (ContentComboTestBase - test file)
      - Additional files: UonWriter, ReflectionMap, BeanStore
    - **Total replacements:** All `throw new BasicRuntimeException(...)` calls replaced with `throw runtimeException(...)`
    - Fixed several files that had missing or duplicate static imports
    - **Compilation verified:** Full clean compile successful (`mvn clean compile -DskipTests`)
    - This change provides consistency across the codebase and leverages the new utility methods from TODO-67

- [x] **Replace exception constructors with utility methods** (2025-01-29)
    - Systematically replaced all instances of `new IllegalArgumentException(`, `new RuntimeException(`, `new UnsupportedOperationException(`, and `new IOException(` with their corresponding utility methods
    - **Files modified:** 61 files across juneau-core, juneau-rest, and juneau-microservice
    - **Changes made:**
      - Replaced `throw new IllegalArgumentException(...)` with `throw illegalArg(...)`
      - Replaced `throw new RuntimeException(...)` with `throw runtimeException(...)`
      - Replaced `throw new UnsupportedOperationException(...)` with `throw unsupportedOp(...)`
      - Replaced `throw new IOException(...)` with `throw ioException(...)`
      - Added static imports for `ThrowableUtils` to all modified files
      - Fixed method calls with empty arguments to use proper overloads
      - Added single-argument overloads: `illegalArg(Throwable)`, `runtimeException(Throwable)`, `unsupportedOp(Throwable)`, `ioException(Throwable)`
    - **Notable file groups:**
      - Common utilities: `AssertionUtils`, `StringUtils`, `ClassUtils`, `IOUtils`, `AsciiSet`, `GranularZonedDateTime`
      - Collection classes: `SimpleMap`, `SimpleUnmodifiableMap`, `FloatValue`, `DoubleValue`, `MapBuilder`, `ControlledArrayList`, `JsonMap`, `JsonList`
      - Parser/Serializer: `Parser`, `Serializer`, `SerializerSession`, `SerializerPipe`, `CsvSerializerSession`, `UonReader`
      - REST components: `ResponseContent`, `RestRequest`, `RestOpContext`, `BasicSwaggerProviderSession`, various header/part classes
      - Reflection: `ExecutableInfo`, `FieldInfo`, `ClassInfo`
      - Other: `Context`, `BeanPropertyMeta`, `BeanContext`, `SerializerSet`, `ParserSet`, `Microservice`, `LogParser`
    - **Compilation verified:** Full clean compile successful (`mvn compile -DskipTests`)
    - This change provides consistency across the codebase, improves code readability, and leverages the utility methods from TODO-67

- **TODO-70** ✅ Convert instances of `Map.of(...)` to `m(...)`.
  - **Status**: COMPLETED
  - **Details**: Converted 43 instances of `Map.of()` to `m()` across 11 test files. The `m()` method provides advantages over `Map.of()`: supports null keys/values, preserves insertion order, no limit on entries (Map.of() is limited to 10), and is consistent with Juneau's other collection utilities like `l()` and `a()`.
    - **Files modified**: `Items_Test.java` (4), `SchemaInfo_Test.java` (6), `HeaderInfo_Test.java` (1), `BasicBeanConverter_Test.java` (5), `PropertyExtractor_Test.java` (1), `RoundTripBeanMaps_Test.java` (1), `PropertyExtractors_Test.java` (4), `BctAssertions_Test.java` (12), `Listifiers_Test.java` (3), `Stringifiers_Test.java` (5), `StringUtils_Test.java` (1)
    - **Compilation verified:** Full clean compile and test-compile successful

- **TODO-72** ✅ Convert instances of `List.of(...)` to `l(...)`.
  - **Status**: COMPLETED
  - **Details**: Converted 76 instances of `java.util.List.of()` to `l()` across 9 test files. The `l()` method provides advantages over `List.of()`: supports null elements, returns a modifiable list, and is consistent with Juneau's other collection utilities like `m()` and `a()`. Note: Only converted actual `java.util.List.of()` calls; class-specific factory methods like `PartList.of()`, `JsonList.of()`, `HeaderList.of()`, and `AnnotationWorkList.of()` were intentionally left unchanged as they are part of their respective class APIs.
    - **Files modified**: 
      - `BctAssertions_Test.java` (18 instances)
      - `StringUtils_Test.java` (14 instances)
      - `CollectionUtils_Test.java` (13 instances)
      - `Listifiers_Test.java` (13 instances)
      - `Stringifiers_Test.java` (7 instances)
      - `Swappers_Test.java` (3 instances, added CollectionUtils import)
      - `AssertionArgs_Test.java` (5 instances, added CollectionUtils import)
      - `ByteValue_Test.java` (1 instance, added CollectionUtils import)
      - `StringValue_Test.java` (2 instances, added CollectionUtils import)
    - **Compilation verified:** Full clean compile and test-compile successful
    - **Tests verified:** All tests passing

- **TODO-69** ✅ Look for places in code where `a(...)` can be used.
  - **Status**: COMPLETED
  - **Details**: Searched codebase for non-primitive array creation patterns that could be converted to use the `a()` utility method from `CollectionUtils`. Found only 2 instances that were appropriate for conversion (plus 1 javadoc example):
    - **Converted:**
      - `Hyperlink_Test.java`: Changed `new Hyperlink[]{a(),a()}` to `CollectionUtils.a(a(),a())` (required fully-qualified call due to method name conflict)
      - `StringUtils.java` (javadoc): Updated example from `new String[]{"a", "b"}` to `a("a", "b")` for consistency
    - **Not converted:**
      - 4 instances of `new String[]{}` in Remote annotation tests were changed to `new String[0]` for clarity (empty arrays with explicit size are clearer than `a()` with no type inference)
      - No other instances of `Arrays.asList()` needed conversion - only used in utility method implementations and documentation
    - **Key findings:**
      - The codebase already extensively uses `a()` where appropriate
      - Empty arrays (`new Type[0]`) are sometimes clearer than `a()` when type inference is ambiguous
      - Most array creation in the codebase is for primitive arrays or already using `a()`
    - **Compilation verified:** Full clean compile successful
    - **Tests verified:** All 25,831 tests passing

- **TODO-66** ✅ There are two ArrayUtilsTest classes whose tests should be merged into CollectionUtils_Test.
  - **Status**: COMPLETED
  - **Details**: Found and merged two identical `ArrayUtilsTest` classes into `CollectionUtils_Test`. Since `ArrayUtils` was deprecated in favor of `CollectionUtils` (per TODO-60), the test classes needed to be consolidated.
    - **Files removed:**
      - `juneau-utest/src/test/java/org/apache/juneau/utils/ArrayUtilsTest.java` (deleted)
      - `juneau-utest/src/test/java/org/apache/juneau/common/utils/ArrayUtilsTest.java` (deleted)
    - **Files modified:**
      - `CollectionUtils_Test.java`: Added 3 new test methods (a43_addAll_arrayToArray, a44_toSet_fromArray, a45_combine_arrays) covering array utility methods
    - **Tests merged:**
      - `addAll(T[], T...)` - Tests appending varargs to arrays
      - `toSet(T[])` - Tests converting arrays to unmodifiable sets
      - `combine(T[]...)` - Tests combining multiple arrays
    - **Additional changes:**
      - Added `import static org.apache.juneau.junit.bct.BctAssertions.*;` for `assertList` and `assertEmpty` methods
    - **Compilation verified:** Full clean compile successful
    - **Tests verified:** All CollectionUtils_Test tests passing

- **TODO-71** ✅ Convert `assertEquals(X, Y.size())` to `assertSize(X, Y)`.
  - **Status**: COMPLETED
  - **Details**: Converted 228 instances of `assertEquals(X, Y.size())` to `assertSize(X, Y)` across 20 test files. The conversion initially revealed ClassCastException errors with non-Comparable objects (Class, Tuple, Map.Entry) when the listifiers tried to sort collections. Fixed by implementing a flexible comparator that handles any object type.
    - **Key Solution:** Created `flexibleComparator()` in `Listifiers.java` that:
      - Handles null values (nulls first)
      - Uses natural ordering for Comparable objects
      - Falls back to string-based comparison for non-Comparable objects
      - Enables predictable test ordering without requiring Comparable implementation
    - **Files modified (conversions):**
      - `ConfigImportsTest.java` (37 instances)
      - `Cache_Test.java` (31 instances)
      - `NestedTokenizer_Test.java` (26 instances)
      - `Listifier_Test.java` (16 instances)
      - `BidiMap_Test.java` (15 instances)
      - `Cache2_Test.java` (11 instances)
      - `UonParser_Test.java` (9 instances)
      - `ConcurrentHashMap2Key_Test.java` (9 instances)
      - `UrlEncodingParser_Test.java` (8 instances)
      - `ChildResourceDescriptions_Test.java` (7 instances)
      - `SimpleUnmodifiableMap_Test.java` (7 instances)
      - `SimpleMap_Test.java` (7 instances)
      - `Listifiers_Test.java` (6 instances)
      - `UonPartParser_Test.java` (6 instances)
      - `BeanConverter_Test.java` (5 instances)
      - `ConcurrentHashMap5Key_Test.java` (5 instances)
      - `ConcurrentHashMap4Key_Test.java` (5 instances)
      - `ConcurrentHashMap3Key_Test.java` (5 instances)
      - `Args_Test.java` (5 instances)
      - Plus additional instances in `Cache3_Test`, `Cache4_Test`, `Cache5_Test`
    - **Framework changes:**
      - `Listifiers.java`: Added `flexibleComparator(BeanConverter)` method
      - Updated `collectionListifier()` and `mapListifier()` to use flexible comparator
    - **Compilation verified:** Full clean compile successful
    - **Tests verified:** All 25,828 tests passing
    - **Additional conversions (64 more instances):**
      - `ConfigImportsTest.java`: All remaining listener/event size checks (done earlier)
      - `NestedTokenizer_Test.java`: All remaining nested token size checks (done earlier)
      - `Args_Test.java`: 5 instances (`.getArgs().size()`)
      - `DataConversion_Test.java`: 2 instances (`.getList().size()`)
      - `BeanConverter_Test.java`: 2 instances (`.listify().size()`)
      - `OptionalObjects_RoundTripTest.java`: 2 instances (`.f1.get().size()`)
      - `CommonParser_UrlEncodingTest.java`: 1 instance (`.getInts().size()`)
      - `CommonParser_UonTest.java`: 1 instance (`.getInts().size()`)
      - `CommonParser_Test.java` (html): 1 instance (`.getInts().size()`)
      - `CommonParser_Test.java` (xml): 1 instance (`.getInts().size()`)
      - `CommonParser_Test.java` (json): 1 instance (`.getInts().size()`)
      - `SimpleMap_Test.java`: 1 instance (`.keySet().size()`)
      - `SimpleUnmodifiableMap_Test.java`: 1 instance (`.keySet().size()`)
      - `BasicHttpResource_Test.java`: 1 instance (`.getHeaders().size()`)
    - **Total instances converted:** 292 (228 from first batch + 64 from second batch)
    - **Final verification:** All 25,828 tests passing
    - **Follow-up refinement:** Converted 60 instances of `assertSize(0, x)` to `assertEmpty(x)` for better semantic clarity
      - Files affected: ConfigImportsTest, BeanConverter_Test, Listifier_Test, OperationMap_Test, OptionalObjects_RoundTripTest, ListBuilder_Test, SimpleMap_Test, BidiMap_Test, Cache_Test, ConcurrentHashMap tests, SetBuilder_Test, MultiSet_Test, MapBuilder_Test, BasicHttpResource_Test, Args_Test, ChildResourceDescriptions_Test
      - Kept 1 instance in BctAssertions_Test.java (tests error handling for null input)
      - All 25,828 tests still passing
    - **Final refinement:** Converted 68 instances of `assertTrue(x.isEmpty())` and `assertFalse(x.isEmpty())` to `assertEmpty(x)` and `assertNotEmpty(x)`
      - 53 instances of `assertTrue(x.isEmpty())` → `assertEmpty(x)`
      - 15 instances of `assertFalse(x.isEmpty())` → `assertNotEmpty(x)`
      - Files affected: UrlEncodingParser_Test, Config_Test, NestedTokenizer_Test, Listifiers_Test, AssertionArgs_Test, BctAssertions_Test, OperationMap_Test, AnnotationInheritance_Test, and Value tests (BooleanValue_Test, CharValue_Test, DoubleValue_Test, FloatValue_Test, IntegerValue_Test, LongValue_Test, ShortValue_Test)
      - Enhanced `assertEmpty()` and `assertNotEmpty()` in BctAssertions with comprehensive type support:
        - String: length-based emptiness checks
        - Optional: isEmpty() check
        - Value: isEmpty() check (supports BooleanValue, IntegerValue, etc.)
        - Map: isEmpty() check
        - Collection: isEmpty() check (explicitly handles List, Set, Queue, etc.)
        - Array: length-based check using reflection (supports both object and primitive arrays)
        - Other objects: fallback to listify() for convertible types
      - Refactored both methods to use if/else chains instead of early returns for clearer code flow
      - All 25,828 tests passing

## Notes

Items are marked as completed when:
1. The work has been finished and tested
2. Documentation has been updated if needed
3. All tests pass
4. The change has been verified in the codebase

