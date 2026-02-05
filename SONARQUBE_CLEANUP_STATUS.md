# SonarQube Duplicated String Literals Cleanup - Status Report

## Summary
This document tracks the status of fixing SonarQube `java:S1192` (duplicated string literals) issues.

## Completed Work ✅

### 1. HTML Tag Names
- ✅ **HtmlParserSession.java**: Added `HTMLTAG_array` and `HTMLTAG_object` constants
- ✅ **BasicHtmlDocTemplate.java**: Suppressed with `@SuppressWarnings("java:S1192")` (reverted constants)
- ✅ **HtmlSerializerSession.java**: Added `HTMLTAG_style` and `HTMLTAG_table` constants

### 2. Error Messages
- ✅ **ParserReader.java**: Added `MSG_bufferUnderflow` constant
- ✅ **BeanPropertyMeta.java**: Added `MSG_getterOrFieldNotDefined` constant
- ✅ **FluentResponseBodyAssertion.java**: Added `MSG_exceptionDuringCall` constant
- ✅ **FluentRequestContentAssertion.java**: Added `MSG_exceptionDuringCall` constant
- ✅ **RestServlet.java**: Added `MSG_servletInitError` constant

### 3. CSS Class Names / UI Files
- ✅ **OpenApiUI.java**: Suppressed with `@SuppressWarnings("java:S1192")` (reverted constants)
- ✅ **SwaggerUI.java**: Suppressed with `@SuppressWarnings("java:S1192")` (reverted constants)

### 4. Script Files
- ✅ **release.py**: Added Python constants (`POM_XML`, `STAGING_DIR`, `SVN_DIST_URL`, `RC_PATTERN`)

### 5. HTTP Method Names
- ✅ **RestClient.java**: Replaced `"PATCH"` with `HttpMethod.PATCH` constant

### 6. Console Commands / Message Bundle Keys
- ✅ **ConfigCommand.java**: Added `MKEY_*` constants for all message bundle keys:
  - `MKEY_invalidArguments`
  - `MKEY_tooManyArguments`
  - `MKEY_keyNotFound`
  - `MKEY_configSet`
  - `MKEY_configRemove`
  - `MKEY_description`
  - `MKEY_info`

### 7. Character Encoding
- ✅ **StringUtils.java**: Replaced `"UTF-8"` with `UTF_8.name()` from `StandardCharsets`

---

## Remaining Work 🔲

### Category 1: Bean Property Names (OpenAPI v3 / Swagger v2)
These are property names used in JSON/API schema definitions. Should use `PROP_*` constants.

**OpenAPI v3:**
- 🔲 **Example.java**: `"description"` (3 times) - line 117
- 🔲 **ExternalDocumentation.java**: `"description"` (3 times) - line 100
- 🔲 **ServerVariable.java**: `"default"` (3 times) - line 147, `"description"` (3 times) - line 148
- 🔲 **Tag.java**: `"description"` (3 times) - line 114

**Swagger v2:**
- 🔲 **ExternalDocumentation.java**: `"description"` (3 times) - line 111
- 🔲 **Info.java**: `"description"` (3 times) - line 142
- 🔲 **Tag.java**: `"description"` (3 times) - line 113

**Action**: Add `PROP_description` and `PROP_default` constants and replace string literals.

### Category 2: Reflection / Property Names
These are property names used in reflection/metadata operations.

- 🔲 **ReflectionMap.java**: 
  - `"fullClassName"` (3 times) - line 363
  - `"simpleClassName"` (3 times) - line 364
  - `"value"` (4 times) - line 323

- 🔲 **AppliedAnnotationObject.java**: 
  - `"value"` (20 times) - line 217
  - **Note**: This is very common in Java annotations. Consider suppressing at class level.

- 🔲 **BeanCreator2.java**: 
  - `"names"` (4 times) - line 580
  - `"Using fallback supplier"` (4 times) - line 1223

**Action**: Create appropriate constants (e.g., `PROP_fullClassName`, `PROP_simpleClassName`, `PROP_value`, `MSG_usingFallbackSupplier`).

### Category 3: JSON Schema / Context Property Names
- 🔲 **Context.java**: `"values"` (3 times) - line 313
- 🔲 **JsonSchemaGenerator.java**: `"values"` (3 times) - line 161
- 🔲 **RestOpContext.java**: `"values"` (4 times) - line 391
- 🔲 **RestContext.java**: `"values"` (7 times) - line 867

**Action**: Create `PROP_values` constant and replace string literals.

### Category 4: Example / Test Data
- 🔲 **BeanExample.java**: `"http://foo.org/"` (3 times) - line 112
- 🔲 **AtomFeed.java**: `"http://foo.org/"` (3 times) - line 42
- 🔲 **AtomFeedResource.java**: `"http://foo.org/"` (3 times) - line 91

**Action**: Create `EXAMPLE_URL` or `TEST_URL` constant. These are example/test data, so suppression might be acceptable.

### Category 5: Assertion / Messages
- 🔲 **AssertionPredicate.java**: `"Messages"` (4 times) - line 75

**Action**: Create `MKEY_Messages` or `BUNDLE_Messages` constant.

### Category 6: HttpPartSchema.java (High Priority)
- 🔲 **HttpPartSchema.java**: Multiple property name literals still in use
  - Missing `PROP_uniqueItems` constant
  - String literals in `addIf()` calls (lines ~3758-3843)
  - String literals in validation code (lines ~4202-4221)

**Action**: 
1. Add `PROP_uniqueItems` constant
2. Replace all string literals with existing `PROP_*` constants

---

## Naming Conventions Used

- **HTML tag names**: `HTMLTAG_x`
- **Error messages**: `MSG_x`
- **CSS class names**: Suppressed (reverted)
- **Script files**: Descriptive names (e.g., `POM_XML`, `STAGING_DIR`)
- **HTTP method names**: Use existing `HttpMethod.*` constants
- **Console commands / Message bundle keys**: `MKEY_x`
- **Character encoding**: Use `StandardCharsets.*.name()`
- **Bean property names**: `PROP_x` (to be used for remaining issues)

---

## Recommendations

1. **High Priority**: Fix HttpPartSchema.java - constants exist but aren't used everywhere
2. **Medium Priority**: Fix bean property names in Example.java, ExternalDocumentation.java, etc.
3. **Low Priority**: Consider suppressing `"value"` in AppliedAnnotationObject.java (very common in annotations)
4. **Low Priority**: Example/test data URLs - consider suppressing or using test constants

---

## Files Modified

### Java Files
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlParserSession.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/BasicHtmlDocTemplate.java` (suppressed)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlSerializerSession.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parser/ParserReader.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanPropertyMeta.java`
- `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/assertion/FluentResponseBodyAssertion.java`
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/assertions/FluentRequestContentAssertion.java`
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/RestServlet.java`
- `juneau-bean/juneau-bean-openapi-v3/src/main/java/org/apache/juneau/bean/openapi3/ui/OpenApiUI.java` (suppressed)
- `juneau-bean/juneau-bean-swagger-v2/src/main/java/org/apache/juneau/bean/swagger/ui/SwaggerUI.java` (suppressed)
- `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestClient.java`
- `juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/console/ConfigCommand.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/StringUtils.java`

### Python Files
- `scripts/release.py`

---

## Next Steps

1. Review and fix HttpPartSchema.java (highest priority)
2. Fix remaining bean property names (Example.java, ExternalDocumentation.java, etc.)
3. Fix reflection property names (ReflectionMap.java, BeanCreator2.java)
4. Consider suppressing AppliedAnnotationObject.java for `"value"` literal
5. Fix remaining JSON schema property names (`"values"` in various files)
6. Address example/test data URLs (low priority)
