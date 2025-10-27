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

## Notes

Items are marked as completed when:
1. The work has been finished and tested
2. Documentation has been updated if needed
3. All tests pass
4. The change has been verified in the codebase

