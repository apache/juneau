# Switch-With-Return Syntax Opportunities

This document lists all locations in the Apache Juneau codebase where the new Java switch expression syntax (switch-with-return) could potentially be used.

## High Priority Candidates

All high-priority candidates have been converted to switch expressions! ✅

## Medium Priority Candidates

These might benefit from switch expressions but have some complexity:

### 15. HtmlSerializerSession.java
**Location:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlSerializerSession.java:233`
```java
switch (getUriAnchorText()) {
    case LAST_TOKEN:
        s = resolveUri(s);
        if (s.indexOf('/') != -1)
            s = s.substring(s.lastIndexOf('/') + 1);
        // ... more string manipulation
        return urlDecode(s);
    case URI_ANCHOR:
        if (s.indexOf('#') != -1)
            s = s.substring(s.lastIndexOf('#') + 1);
        return urlDecode(s);
    // ... more cases
}
```
**Note:** Each case does multiple operations on `s` before returning. Could be converted but might be less readable.

## Not Recommended for Conversion

These switch statements are not good candidates due to side effects, void returns, or complex logic:

### 16. JsonParserSession.java:681
**Reason:** Uses side effects (r.replace()) for character escape handling, no return value per case

### 17. MsgPackInputStream.java:140
**Reason:** Sets instance variable `length`, uses breaks, doesn't return values

### 18. HttpPartSchema.java:2844
**Reason:** Builder pattern with multiple method calls, side effects, validation logic

### 19. HttpPartSchema.java:3680, 4203, 4342
**Reason:** Complex validation logic with list building and error handling

### 20. ClassMeta.java:381
**Reason:** Assigns to variable `example` without returning in each case

### 21. ClassInfo.java:1439, 1524
**Reason:** Boolean flag checking with early returns inside loop, complex logic

### 22. FieldInfo.java:290, 338
**Reason:** Boolean flag checking with early returns inside loop, complex logic

### 23. ExecutableInfo.java:547, 611
**Reason:** Boolean flag checking with early returns inside loop, complex logic

### 24. LogEntryFormatter.java:150
**Reason:** Side effects with regex building and field index tracking

### 25. RdfSerializerSession.java:403
**Reason:** Calls serialization methods with side effects, doesn't always return a value

### 26. Utils.java:249
**Reason:** Appends to StringBuilder, no return values

## Summary

- **High Priority (14 candidates):** ✅ **COMPLETED** - All converted to switch expressions
- **Medium Priority (1 candidate):** Could be converted but readability might be impacted
- **Not Recommended (11 locations):** Should remain as traditional switch statements due to side effects or complexity

## Completed Conversions

All 14 high-priority candidates have been successfully converted to switch expressions:

1. ✅ ContentComboTestBase.java - Client media type selection
2. ✅ DateUtils.java - Date string formatting by state
3. ✅ Visibility.java - Visibility modifier checking
4. ✅ XmlParserSession.java - JSON type parsing (outer switch)
5. ✅ XmlParserSession.java - JSON type parsing (nested switch)
6. ✅ HttpParts.java - HTTP part name extraction
7. ✅ HttpParts.java - HTTP part type checking with fall-through
8. ✅ OutputStreamSerializerSession.java - Binary format serialization
9. ✅ ParserPipe.java - Binary format parsing
10. ✅ RdfSerializer.java - RDF accept header generation
11. ✅ RdfSerializer.java - RDF produces header generation
12. ✅ RdfParser.java - RDF consumes header generation with fall-through
13. ✅ PropertyExtractor_Test.java - Test property extraction
14. ✅ PropertyExtractor_Test.java - Test map property extraction

## Notes

- Switch expressions were introduced in Java 14 and became a standard feature
- Benefits include:
  - More concise code
  - Exhaustiveness checking by compiler
  - Expression-based (can be assigned directly to variables)
  - Less repetitive code (no need for break statements)
  - Safer (no fall-through bugs)

## Example Conversion

Before:
```java
String result;
switch (type) {
    case A:
        result = "a";
        break;
    case B:
        result = "b";
        break;
    default:
        result = "default";
}
return result;
```

After:
```java
return switch (type) {
    case A -> "a";
    case B -> "b";
    default -> "default";
};
```

