# SonarQube Issues Cleanup Plan

## Summary
After fixing property name literals with constants, we need to:
1. Remove false positives (issues already fixed)
2. Categorize remaining legitimate issues
3. Provide suggestions for fixes

## False Positives to Remove

These issues are **already fixed** - constants exist and are being used. SonarQube line numbers may be outdated:

### SchemaInfo.java (both OpenAPI v3 and Swagger v2)
- ✅ `additionalProperties` - Already has `PROP_additionalProperties` constant
- ✅ `allOf` - Already has `PROP_allOf` constant  
- ✅ `anyOf` - Already has `PROP_anyOf` constant (OpenAPI v3 only)
- ✅ `default` - Already has `PROP_default` constant
- ✅ `deprecated` - Already has `PROP_deprecated` constant
- ✅ `description` - Already has `PROP_description` constant
- ✅ `discriminator` - Already has `PROP_discriminator` constant
- ✅ `example` - Already has `PROP_example` constant
- ✅ `exclusiveMaximum` - Already has `PROP_exclusiveMaximum` constant
- ✅ `exclusiveMinimum` - Already has `PROP_exclusiveMinimum` constant
- ✅ `externalDocs` - Already has `PROP_externalDocs` constant
- ✅ `format` - Already has `PROP_format` constant
- ✅ `items` - Already has `PROP_items` constant
- ✅ `maximum` - Already has `PROP_maximum` constant
- ✅ `maxItems` - Already has `PROP_maxItems` constant
- ✅ `maxLength` - Already has `PROP_maxLength` constant
- ✅ `maxProperties` - Already has `PROP_maxProperties` constant
- ✅ `minimum` - Already has `PROP_minimum` constant
- ✅ `minItems` - Already has `PROP_minItems` constant
- ✅ `minLength` - Already has `PROP_minLength` constant
- ✅ `minProperties` - Already has `PROP_minProperties` constant
- ✅ `multipleOf` - Already has `PROP_multipleOf` constant
- ✅ `nullable` - Already has `PROP_nullable` constant (OpenAPI v3 only)
- ✅ `oneOf` - Already has `PROP_oneOf` constant (OpenAPI v3 only)
- ✅ `pattern` - Already has `PROP_pattern` constant
- ✅ `properties` - Already has `PROP_properties` constant
- ✅ `readOnly` - Already has `PROP_readOnly` constant
- ✅ `required` - Already has `PROP_required` constant
- ✅ `requiredProperties` - Already has `PROP_requiredProperties` constant (Swagger v2 only)
- ✅ `uniqueItems` - Already has `PROP_uniqueItems` constant
- ✅ `writeOnly` - Already has `PROP_writeOnly` constant (OpenAPI v3 only)

### HeaderInfo.java, Items.java, ParameterInfo.java (Swagger v2)
- ✅ All property names already have constants (collectionFormat, default, description, example, exclusiveMaximum, exclusiveMinimum, format, items, maximum, maxItems, maxLength, minimum, minItems, minLength, multipleOf, pattern, uniqueItems)

### Response.java (OpenAPI v3)
- ✅ `content` - Already has `PROP_content` constant
- ✅ `description` - Already has `PROP_description` constant
- ✅ `headers` - Already has `PROP_headers` constant
- ✅ `links` - Already has `PROP_links` constant

### ResponseInfo.java (Swagger v2)
- ✅ `description` - Already has `PROP_description` constant
- ✅ `examples` - Already has `PROP_examples` constant
- ✅ `headers` - Already has `PROP_headers` constant
- ✅ `schema` - Already has `PROP_schema` constant

### Server.java (OpenAPI v3)
- ✅ `variables` - Already has `PROP_variables` constant

### SecurityRequirement.java (OpenAPI v3)
- ✅ `requirements` - Already has `PROP_requirements` constant

### MediaType.java (OpenAPI v3)
- ✅ `encoding` - Already has `PROP_encoding` constant
- ✅ `examples` - Already has `PROP_examples` constant
- ✅ `schema` - Already has `PROP_schema` constant

## Legitimate Issues to Fix

### Category 1: HttpPartSchema.java - Property Names (LEGITIMATE ISSUES)
**File**: `/juneau-core/juneau-marshall/src/main/java/org/apache/juneau/httppart/HttpPartSchema.java`

**Status**: Constants exist (lines 92-107) but are NOT being used everywhere. String literals still present in:
- Line 2696: `"uniqueItems"` - **MISSING CONSTANT** `PROP_uniqueItems` needs to be added
- Lines 3758-3843: Multiple `addIf()` calls using string literals instead of constants
- Lines 4202-4221: More string literals in validation code

**Issues to fix**:
- Add missing `PROP_uniqueItems` constant
- Replace all string literals in `addIf()` calls (lines ~3758-3843) with constants
- Replace string literals in validation code (lines ~4202-4221) with constants

**Suggestion**: 
1. Add `PROP_uniqueItems = "uniqueItems"` constant (line ~108)
2. Replace all `"properties"`, `"additionalProperties"`, `"exclusiveMaximum"`, `"exclusiveMinimum"`, `"uniqueItems"`, `"collectionFormat"`, `"pattern"`, `"items"`, `"maximum"`, `"minimum"`, `"multipleOf"`, `"maxItems"`, `"maxLength"`, `"maxProperties"`, `"minItems"`, `"minLength"`, `"minProperties"` string literals with their corresponding `PROP_*` constants

### Category 2: UI Files - CSS Class Names / HTML Attributes
**Files**: 
- `/juneau-bean-openapi-v3/src/main/java/org/apache/juneau/bean/openapi3/ui/OpenApiUI.java`
- `/juneau-bean-swagger-v2/src/main/java/org/apache/juneau/bean/swagger/ui/SwaggerUI.java`

These appear to be CSS class names or HTML attribute values used in UI generation:

- `"Description"` (3-6 times) - Capitalized version, likely for display
- `"description"` (4-6 times) - Lowercase version
- `"model"` (5-7 times)
- `"parameter-key"` (3 times)
- `"response-key"` (3 times)

**Suggestion**: Create constants like `CSS_CLASS_Description`, `CSS_CLASS_description`, `CSS_CLASS_model`, `CSS_CLASS_parameterKey`, `CSS_CLASS_responseKey`. These are UI-specific constants, so a different naming convention might be appropriate (e.g., `UI_CLASS_*` or `HTML_CLASS_*`).

### Category 3: HTML Parser/Serializer - HTML Element Names
**Files**:
- `/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlParserSession.java`
- `/juneau-marshall/src/main/java/org/apache/juneau/html/BasicHtmlDocTemplate.java`
- `/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlSerializerSession.java`

These are HTML element names:

- `"array"` (5 times)
- `"aside"` (8 times)
- `"object"` (4 times)
- `"section"` (6 times)
- `"style"` (4 times)
- `"table"` (6 times)

**Suggestion**: Create constants like `HTML_TAG_array`, `HTML_TAG_aside`, `HTML_TAG_object`, `HTML_TAG_section`, `HTML_TAG_style`, `HTML_TAG_table`. Or use a shared constant class for HTML tag names.

### Category 4: Error Messages
**Files**:
- `/juneau-marshall/src/main/java/org/apache/juneau/parser/ParserReader.java`
- `/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/assertion/FluentResponseBodyAssertion.java`
- `/juneau-rest-server/src/main/java/org/apache/juneau/rest/assertions/FluentRequestContentAssertion.java`
- `/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/RestServlet.java`
- `/juneau-marshall/src/main/java/org/apache/juneau/BeanPropertyMeta.java`

- `"Buffer underflow."` (3 times)
- `"Exception occurred during call."` (3-4 times)
- `"Getter or public field not defined on property ''{0}''"` (3 times)
- `"Servlet init error on class ''{0}''"` (3 times)

**Suggestion**: These are error messages that could be moved to a messages resource file or constants class. However, if they're only used 3-4 times, consider:
- For error messages: Create `ERROR_MSG_*` constants
- For messages with placeholders: Consider using a message format utility or resource bundle

### Category 5: Example/Test Data
**Files**:
- `/juneau-examples-core/src/main/java/org/apache/juneau/examples/bean/BeanExample.java`
- `/juneau-examples-core/src/main/java/org/apache/juneau/examples/bean/atom/AtomFeed.java`
- `/juneau-examples-rest/src/main/java/org/apache/juneau/examples/rest/dto/AtomFeedResource.java`

- `"http://foo.org/"` (3 times each file)

**Suggestion**: Create example constants like `EXAMPLE_URL_FOO_ORG = "http://foo.org/"`. These are test/example data, so low priority unless they're duplicated across many files.

### Category 6: Script Files
**File**: `/juneau/scripts/release.py`

- `'https://dist.apache.org/repos/dist/dev/juneau'` (3 times)
- `'pom.xml'` (5 times)
- `'~/tmp/dist-release-juneau'` (15 times)
- `r'RC(\d+)'` (8 times)

**Suggestion**: Python scripts - create constants at the top of the file. These are configuration values that should definitely be constants.

### Category 7: Reflection/Internal Property Names
**Files**:
- `/juneau-commons/src/main/java/org/apache/juneau/commons/reflect/ReflectionMap.java`
- `/juneau-commons/src/main/java/org/apache/juneau/commons/inject/BeanCreator2.java`
- `/juneau-marshall/src/main/java/org/apache/juneau/httppart/HttpPartSchema.java`

- `"fullClassName"` (3 times)
- `"simpleClassName"` (3 times)
- `"value"` (3-20 times) - Very common, used in many contexts
- `"values"` (3-7 times)
- `"names"` (4 times)
- `"Using fallback supplier"` (4 times)

**Suggestion**: 
- For property names: Create `PROP_fullClassName`, `PROP_simpleClassName`, etc.
- For `"value"`: This is extremely common and used in many different contexts. Consider if it's worth creating a constant, or if it's better to suppress the warning for this specific case.
- For messages: Create `MSG_*` constants

### Category 8: HTTP Methods / REST
**File**: `/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestClient.java`

- `"PATCH"` (3 times)

**Suggestion**: Use existing HTTP method constants if available, or create `HTTP_METHOD_PATCH` constant.

### Category 9: Configuration/Console Commands
**File**: `/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/console/ConfigCommand.java`

- `"InvalidArguments"` (3 times)
- `"TooManyArguments"` (3 times)

**Suggestion**: Create constants like `CMD_ERROR_InvalidArguments`, `CMD_ERROR_TooManyArguments`.

### Category 10: JSON Schema / Type Names
**Files**:
- `/juneau-marshall/src/main/java/org/apache/juneau/jsonschema/JsonSchemaGeneratorSession.java`
- `/juneau-marshall/src/main/java/org/apache/juneau/annotation/SchemaAnnotation.java`

- `"items"` (4-7 times) - Already has constant in some files, check if used here
- `"string"` (4 times)

**Suggestion**: Create constants like `JSONSCHEMA_TYPE_string`, `JSONSCHEMA_TYPE_items`, or use existing constants if available.

### Category 11: Character Encoding
**File**: `/juneau-commons/src/main/java/org/apache/juneau/commons/utils/StringUtils.java`

- `"UTF-8"` (3 times)

**Suggestion**: Use `StandardCharsets.UTF_8.name()` or create `CHARSET_UTF8 = "UTF-8"` constant. Better yet, use `StandardCharsets.UTF_8` directly if possible.

### Category 12: AppliedAnnotationObject
**File**: `/juneau-commons/src/main/java/org/apache/juneau/commons/annotation/AppliedAnnotationObject.java`

- `"value"` (20 times)

**Suggestion**: This is a very common property name in annotations. Consider creating `PROP_value = "value"` constant, or suppress the warning if it's used in a context where a constant wouldn't improve readability.

## Recommended Action Plan

### Phase 1: Remove False Positives (High Priority)
1. Remove all SchemaInfo.java issues (both versions) - these are false positives
2. Remove HeaderInfo.java, Items.java, ParameterInfo.java issues - already fixed
3. Remove Response.java, ResponseInfo.java issues - already fixed
4. Remove Server.java, SecurityRequirement.java, MediaType.java issues - already fixed

### Phase 2: Fix HttpPartSchema.java (High Priority)
- Verify constants exist (they do, lines 92-107)
- Check `apply(JsonMap m)` method around lines 2655-2680
- Replace all string literals with existing constants

### Phase 3: Fix UI Files (Medium Priority)
- Create CSS class name constants for OpenApiUI.java and SwaggerUI.java
- Use consistent naming convention (e.g., `UI_CLASS_*`)

### Phase 4: Fix HTML Files (Medium Priority)
- Create HTML tag name constants
- Consider a shared `HtmlTags` class for reusability

### Phase 5: Fix Error Messages (Low-Medium Priority)
- Create error message constants
- Consider moving to resource bundle if internationalization is needed

### Phase 6: Fix Remaining Issues (Low Priority)
- Script files (Python)
- Example/test data
- Reflection property names
- Other miscellaneous issues

## Notes

- Many issues are false positives because SonarQube line numbers may be outdated after our refactoring
- The `"value"` literal appears 20+ times in AppliedAnnotationObject.java - this is very common in Java annotations and may be acceptable to suppress
- Some literals (like `"UTF-8"`) should use `StandardCharsets.UTF_8` instead of string constants
- UI and HTML-related constants might benefit from shared constant classes for better organization
