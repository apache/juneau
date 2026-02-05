# SonarQube Duplicated String Literals Cleanup - Continuation Guide

## Current Status

We are working on addressing SonarQube issues related to duplicated string literals (`java:S1192` rule). A comprehensive cleanup plan has been created and saved in `SONARQUBE_CLEANUP_PLAN.md`.

## What We've Done

1. **Analyzed SonarQube Issues**: Reviewed a list of duplicated string literal issues from SonarQube
2. **Identified False Positives**: Found that many issues are already fixed - constants exist and are being used in:
   - `SchemaInfo.java` (both OpenAPI v3 and Swagger v2 versions)
   - `HeaderInfo.java`, `Items.java`, `ParameterInfo.java` (Swagger v2)
   - `Response.java` (OpenAPI v3), `ResponseInfo.java` (Swagger v2)
   - `Server.java`, `SecurityRequirement.java`, `MediaType.java` (OpenAPI v3)
3. **Identified Legitimate Issues**: Found that `HttpPartSchema.java` has constants defined but they're not being used everywhere
4. **Created Cleanup Plan**: Documented all findings in `SONARQUBE_CLEANUP_PLAN.md` with categorized issues and suggestions

## Key Findings

### False Positives (Already Fixed)
Most property name issues in the `juneau-bean` module are false positives. Constants like `PROP_additionalProperties`, `PROP_allOf`, `PROP_anyOf`, etc. already exist and are being used in `get()` and `keySet()` methods. SonarQube line numbers may be outdated.

### Legitimate Issues Found

**Priority 1: HttpPartSchema.java**
- **File**: `/juneau-core/juneau-marshall/src/main/java/org/apache/juneau/httppart/HttpPartSchema.java`
- **Status**: Constants exist (lines 92-107) but are NOT being used everywhere
- **Issues**:
  - Missing `PROP_uniqueItems` constant (line ~108 needs to be added)
  - String literals still used in `addIf()` calls (lines ~3758-3843)
  - String literals still used in validation code (lines ~4202-4221)
- **Fix Needed**: 
  1. Add `PROP_uniqueItems = "uniqueItems"` constant
  2. Replace all string literals with existing `PROP_*` constants

**Other Categories** (documented in plan):
- UI files (CSS class names)
- HTML parser/serializer (HTML tag names)
- Error messages
- Example/test data
- Script files (Python)
- Reflection property names
- HTTP methods
- Console commands
- JSON schema types
- Character encoding

## Next Steps

### Immediate Action (High Priority)
1. **Fix HttpPartSchema.java**:
   - Add missing `PROP_uniqueItems` constant after line 107
   - Replace string literal `"uniqueItems"` on line 2696 with `PROP_uniqueItems`
   - Replace all string literals in `addIf()` calls (lines ~3758-3843) with constants
   - Replace string literals in validation code (lines ~4202-4221) with constants

### Follow-up Actions
2. **Remove False Positives from SonarQube**: Mark issues in SchemaInfo.java, HeaderInfo.java, etc. as resolved/false positives
3. **Fix Other Categories**: Work through remaining categories in `SONARQUBE_CLEANUP_PLAN.md` based on priority

## Files to Reference

- **Plan Document**: `SONARQUBE_CLEANUP_PLAN.md` - Contains full details of all issues and suggestions
- **Main File to Fix**: `/juneau-core/juneau-marshall/src/main/java/org/apache/juneau/httppart/HttpPartSchema.java`
- **Reference Files** (for pattern examples):
  - `/juneau-bean/juneau-bean-openapi-v3/src/main/java/org/apache/juneau/bean/openapi3/SchemaInfo.java`
  - `/juneau-bean/juneau-bean-swagger-v2/src/main/java/org/apache/juneau/bean/swagger/SchemaInfo.java`

## Constants Pattern

The codebase uses a consistent pattern for property name constants:
```java
private static final String PROP_propertyName = "propertyName";
```

Examples:
- `PROP_additionalProperties = "additionalProperties"`
- `PROP_collectionFormat = "collectionFormat"`
- `PROP_exclusiveMaximum = "exclusiveMaximum"`
- etc.

## Notes

- SonarQube line numbers may be outdated after previous refactoring
- Many issues in `juneau-bean` module are false positives (constants already exist and are used)
- `HttpPartSchema.java` is the main file needing fixes - constants exist but aren't used everywhere
- The `"value"` literal appears frequently in some files - may be acceptable to suppress in certain contexts

## How to Continue

1. Read `SONARQUBE_CLEANUP_PLAN.md` for full context
2. Start with fixing `HttpPartSchema.java` (highest priority)
3. Test changes to ensure no regressions
4. Mark false positives as resolved in SonarQube
5. Continue with remaining categories as needed
