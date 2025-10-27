# TODO List

This file tracks pending tasks for the Apache Juneau project. For completed items, see [TODO-completed.md](TODO-completed.md).

## Issues to Fix

- [ ] TODO-3 Figure out why CallLogger and ThrownStore objects are not automatically injected into REST classes in spring boot.

## Code Quality Improvements

- [ ] TODO-5 Fields should be alphabetized.
- [ ] TODO-6 Investigate if there are any other W3 or RFC specifications that would make good candidates for new bean modules.
- [ ] TODO-12 Tests for Spring Boot testing.
- [ ] TODO-14 The name parameter on annotations like Query when used on method parameters should be optional if parameter names are persisted in the bytecode.
- [ ] TODO-15 It appears StringUtils is going to become a commonly-used external class. Let's see if we can enhance it with commonly used string utility methods.
- [ ] TODO-27 Determine if there are any other good candidates for Stringifiers and Listifiers.
- [ ] TODO-29 Finish setting up SonarQube analysis in git workflow.
- [x] TODO-37 Find and replace all instances of "x != null" with "nn(x)". **COMPLETE** - Replaced approximately 1965+ instances of `!= null` with `nn()` across production code in juneau-core, juneau-rest, juneau-bean, and juneau-microservice modules (~98.6% reduction from initial ~1992). Includes:
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
  - Builder patterns: `.addIf(variable != null, ...)` → `.addIf(nn(variable), ...)` for 80+ common fields across OpenAPI/Swagger beans (including all remaining addIf patterns: anyOf, authorizationCode, authorizationUrl, basePath, callbacks, clientCredentials, definitions, host, implicit, in, info, mapping, not, nullable, oneOf, password, paths, propertyName, refreshUrl, requestBody, requiredProperties, requirements, scopes, securityDefinitions, servers, swagger, tokenUrl, writeOnly)
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
  All changes compile and install successfully with all tests passing. **Only 27 instances remain (down from initial ~1992)**, all intentionally preserved:
  - 1 in `Utils.isNotNull()` method definition (must remain as-is)
  - 7 in `AssertionPredicates` lambda test predicates (preserved for clarity in test expressions)
  - 17 in `AssertionUtils` with `assertArg` statements (preserved for clarity in argument validation)
  - 2 in `PartList` and `HeaderList` with `assertArg` statements (preserved for clarity)
  **Special care taken to preserve Utils.isNotNull() method definition by excluding Utils.java from all replacements.**
- [ ] TODO-38 Find methods in com.sfdc.irs.Utils that don't exist in org.apache.juneau.common.utils.Utils and come up with a plan to add ones that make sense (e.g. they're not tied to other code in com.sfdc.irs and they don't already have equivalents in other XUtils classes).
- [ ] TODO-39 Use static imports for all method calls to AnnotationUtils.
- [ ] TODO-40 Use static imports for all method calls to ArrayUtils.
- [ ] TODO-41 Use static imports for all method calls to AssertionUtils.
- [ ] TODO-42 Use static imports for all method calls to ClassUtils.
- [ ] TODO-43 Use static imports for all method calls to CollectionUtils.
- [ ] TODO-44 Use static imports for all method calls to DateUtils.
- [ ] TODO-45 Use static imports for all method calls to FileUtils.
- [ ] TODO-46 Use static imports for all method calls to IOUtils.
- [ ] TODO-47 Use static imports for all method calls to PredicateUtils.
- [ ] TODO-48 Use static imports for all method calls to ResourceBundleUtils.
- [ ] TODO-49 Use static imports for all method calls to StringUtils.
- [ ] TODO-50 Use static imports for all method calls to SystemUtils.
- [ ] TODO-51 Use static imports for all method calls to ThrowableUtils.
- [ ] TODO-52 Use static imports for all method calls to Utils.
- [ ] TODO-53 Use static imports for all method calls to BctUtils.
- [ ] TODO-54 Search for places in code where Calendar should be replaced with ZonedDateTime.
- [ ] TODO-55 Replace multiple instances of assertArgNotNull with assertArgsNotNull.
- [ ] TODO-56 Rename AnnotationUtils.hashCode to hash.
- [ ] TODO-57 Add method Utils.eq(Annotation, Annotation) that calls AnnotationUtils.equals.
- [ ] TODO-58 Update Utils.hash to use AnnotationUtils.hash() for calculating hashes of annotations.
- [ ] TODO-59 Move ClassUtils.cn and scn to Utils.
- [ ] TODO-60 There seems to be duplication in ArrayUtils and CollectionUtils. Let's merge ArrayUtils into CollectionUtils.
- [ ] TODO-61 Console.format seems to duplicate Utils.f. Let's remove it.
- [ ] TODO-62 ResourceBundleUtils.empty() appears to be unused. Let's remove it if so.
- [ ] TODO-63 Look for places in code where ThrowableUtils.illegalArg and runtimeException can be used.
- [x] TODO-64 Add a Utils.nn(Object...) that validates that all parameters are not null. **COMPLETE** - Added varargs `nn(Object...)` method that returns true if all parameters are not null. Method includes comprehensive Javadoc with examples. Search of codebase found no existing patterns of `nn(x) && nn(y)` chains to consolidate, likely because TODO-37's systematic replacements converted compound null checks individually.
- [x] TODO-65 Add Utils.isEmpty(CharSequence) which redirects to StringUtils.isEmpty(CharSequence). **COMPLETE** - Added `isEmpty(CharSequence)` method that delegates to `StringUtils.isEmpty(CharSequence)`, along with `isBlank(CharSequence)` and `isNotEmpty(CharSequence)` overloads. Also added overloads for `isEmpty(Collection)`, `isEmpty(Map)`, `isNotEmpty(Collection)`, and `isNotEmpty(Map)`. All methods include comprehensive Javadocs.

## Framework Improvements

- [ ] TODO-19 ClassInfo improvements to getMethod (e.g. getMethodExact vs getMethod).
- [ ] TODO-21 Thrown NotFound causes - javax.servlet.ServletException: Invalid method response: 200

## HTTP Response/Exception Improvements

- [ ] TODO-22 HttpResponse should use list of Headers and have a headers(Header...) method.
- [ ] TODO-23 HttpResponse should allow you to set code.
- [ ] TODO-24 HttpException subclasses can set status, but does it use code?
- [ ] TODO-25 HttpException should use list of Headers and have a headers(Header...) method.

- [ ] TODO-26 @ResponseBody and @ResponseHeaders shouldn't be required on HttpResponse objects.

## Notes

This TODO list tracks specific issues that need to be addressed in the Juneau project.

## Website/Docs

- [ ] TODO-29 Add searching to website using Algolia DocSearch.

## Code Style and Consistency

- [ ] TODO-30 Ensure all Builder methods are consistently using "value" as setter parameter names when it's a single parameter and it's obvious what property is being set.
