# TODO: Replace Utils References with Static Imports

This document lists files that still contain `Utils.` method calls that can be replaced with static imports.

## Files with Utils. calls that need static import replacement

### High Priority Files (Multiple Utils. calls) - ✅ COMPLETED

#### 1. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java` - ✅ COMPLETED
- **Line 272**: `Utils.isEmpty(s)` → `Utils.isEmpty(s)` (kept qualified due to import conflicts)
- **Line 275**: `Utils.splita(s.trim(), ',')` → `Utils.splita(s.trim(), ',')` (kept qualified due to import conflicts)
- **Status**: ⚠️ Import conflicts with `java.util.*` prevent static imports

#### 2. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanMeta.java` - ✅ COMPLETED
- **Line 902**: `Utils.array(properties.values(), BeanPropertyMeta.class)` → `array(properties.values(), BeanPropertyMeta.class)`
- **Line 922**: `Utils.eq(this, (BeanMeta<?>)o, (x, y) -> Utils.eq(x.classMeta, y.classMeta))` → `eq(this, (BeanMeta<?>)o, (x, y) -> eq(x.classMeta, y.classMeta))`
- **Status**: ✅ Successfully replaced with static imports

#### 3. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlParserSession.java` - ✅ COMPLETED
- **Line 289**: `Utils.opt(parseAnything(eType.getElementType(), r, outer, isRoot, pMeta))` → `opt(parseAnything(eType.getElementType(), r, outer, isRoot, pMeta))`
- **Status**: ✅ Successfully replaced with static imports

#### 4. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/internal/CollectionUtils.java` - ✅ COMPLETED
- **Line 89**: `Utils.set(entries)` → `set(entries)`
- **Line 547**: `Utils.set(values)` → `set(values)`
- **Status**: ✅ Successfully replaced with static imports

#### 5. `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyMicroservice.java`
- **Line 548**: `Utils.firstNonNull(builder.ports, cf.get("Jetty/port").as(int[].class).orElseGet(() -> mf.getWithDefault("Jetty-Port", new int[] { 8000 }, int[].class)))`
- **Line 556**: `Utils.firstNonNull(builder.jettyXmlResolveVars, cf.get("Jetty/resolveVars").asBoolean().orElse(false))`
- **Status**: Needs `import static org.apache.juneau.common.utils.Utils.*;`

#### 6. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/urlencoding/UrlEncodingParserSession.java`
- **Line 232**: `Utils.opt(parseAnything(eType.getElementType(), r, outer))`
- **Status**: Needs `import static org.apache.juneau.common.utils.Utils.*;`

#### 7. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlSerializerSession.java` - ✅ COMPLETED
- **Line 612**: `Utils.s(o)` → `s(o)`
- **Line 676**: `Utils.s(value)` → `s(value)`
- **Line 883**: `Utils.firstNonNull(bpHtml.getRender(), cHtml.getRender())` → `firstNonNull(bpHtml.getRender(), cHtml.getRender())`
- **Status**: ✅ Successfully replaced with static imports

#### 8. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/csv/CsvSerializerSession.java` - ✅ COMPLETED
- **Line 246**: `Utils.isNotEmpty(l)` → `isNotEmpty(l)`
- **Status**: ✅ Successfully replaced with static imports

#### 9. `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/util/RestUtils.java` - ✅ COMPLETED
- **Line 268**: `Utils.isEmpty(Utils.s(qs))` → `isEmpty(s(qs))`
- **Status**: ✅ Successfully replaced with static imports

#### 10. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json/JsonParserSession.java` - ✅ COMPLETED
- **Line 215**: `Utils.opt(parseAnything(eType.getElementType(), r, outer, pMeta))` → `opt(parseAnything(eType.getElementType(), r, outer, pMeta))`
- **Status**: ✅ Successfully replaced with static imports

### Test Files (Many Utils. calls)

#### 11. `juneau-utest/src/test/java/org/apache/juneau/utils/StringUtils_Test.java`
- **Lines 398-888**: Multiple `Utils.` method calls in test methods
- **Status**: Already has `import static org.apache.juneau.common.utils.Utils.*;` on line 22
- **Note**: This file has many test cases that call Utils methods directly for testing purposes

### Files with Single Utils. calls

#### 12. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/svl/VarResolverSession.java`
- **Status**: Already has `import static org.apache.juneau.common.utils.Utils.*;` on line 22
- **Note**: Check for any remaining Utils. calls

#### 13. `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyLogger.java`
- **Status**: Already has `import static org.apache.juneau.common.utils.Utils.*;` on line 20
- **Note**: Check for any remaining Utils. calls

#### 14. `juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/resources/DirectoryResource.java`
- **Status**: Already has `import static org.apache.juneau.common.utils.Utils.*;` on line 21
- **Note**: Check for any remaining Utils. calls

#### 15. `juneau-utest/src/test/java/org/apache/juneau/serializer/UriResolution_Test.java`
- **Status**: Already has `import static org.apache.juneau.common.utils.Utils.*;` on line 19
- **Note**: Check for any remaining Utils. calls

#### 16. `juneau-core/juneau-bct/src/main/java/org/apache/juneau/junit/bct/BasicBeanConverter.java`
- **Status**: Has `import static org.apache.juneau.junit.bct.Utils.*;` on line 20
- **Note**: This is a different Utils class (junit.bct.Utils), not the common Utils

#### 17. `juneau-core/juneau-bct/src/main/java/org/apache/juneau/junit/bct/Stringifiers.java`
- **Status**: Has `import static org.apache.juneau.junit.bct.Utils.*;` on line 21
- **Note**: This is a different Utils class (junit.bct.Utils), not the common Utils

## Files with Import Statements Only (Already Fixed)

These files only contain import statements for Utils and no actual Utils. method calls:

- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/util/UrlPath.java` (line 21)
- `juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/SpringBeanStore.java` (line 19)
- `juneau-core/juneau-config/src/main/java/org/apache/juneau/config/internal/ConfigMap.java` (line 22)
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java` (line 25)
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/logger/CallLogger.java` (line 23)
- `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/utils/StringUtils.java` (line 24)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/uon/UonParserSession.java` (line 22)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/UriResolver.java` (line 22)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MediaRange.java` (line 19)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/http/annotation/PathRemainderAnnotation.java` (line 21)

## Non-Code Files (Ignore)

- `TODO-switch.md` - Documentation file
- `juneau-docs/docs/release-notes/9.2.0.md` - Documentation file

## Summary

**Total files needing Utils. replacement**: 17 files
**✅ High Priority Files Completed**: 8 out of 10 files (80% complete)
**Files with static imports already**: 10 files (just need to replace Utils. calls)
**Files needing static imports added**: 2 files (JettyMicroservice.java, UrlEncodingParserSession.java)
**Test files**: 1 file (StringUtils_Test.java - may need special handling)
**Different Utils classes**: 2 files (junit.bct.Utils - not the common Utils)

## Progress Update

### ✅ Completed High Priority Files (8/10):
1. ✅ BeanMeta.java - 2 Utils. calls replaced
2. ✅ HtmlParserSession.java - 1 Utils. call replaced  
3. ✅ CollectionUtils.java - 2 Utils. calls replaced
4. ✅ HtmlSerializerSession.java - 3 Utils. calls replaced
5. ✅ CsvSerializerSession.java - 1 Utils. call replaced
6. ✅ RestUtils.java - 1 Utils. call replaced
7. ✅ JsonParserSession.java - 1 Utils. call replaced
8. ⚠️ JsonList.java - Import conflicts prevent static imports (kept qualified)

### 🔄 Remaining High Priority Files (2/10):
1. JettyMicroservice.java - 2 Utils. calls (needs static import added)
2. UrlEncodingParserSession.java - 1 Utils. call (needs static import added)

## Recommended Order

1. ✅ **High Priority**: Files with multiple Utils. calls that already have static imports (8/10 completed)
2. **Medium Priority**: Files that need static imports added (2 files remaining)
3. **Low Priority**: Test files and single-call files
4. **Ignore**: Documentation files and different Utils classes
