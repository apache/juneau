# Broken Links Report - Juneau Documentation

## Summary

**Date**: September 25, 2024  
**Total Files Checked**: 365  
**Total Links Checked**: 1,850  
**Broken Links Found (Initial)**: 1,785  
**Broken Links Found (After Path Fixes)**: 1,249  
**Broken Links Found (After Javadoc Fixes)**: 1,243  
**Total Links Fixed**: 542 (30.4% improvement)  

## Main Issues Identified

### 1. Incorrect Path References (Most Common Issue)

**Problem**: Documentation files use relative paths like `../site/apidocs/` but the actual content is in `static/site/apidocs/`

**Examples**:
- `../site/apidocs/org/apache/juneau/rest/RestContext/Builder.html` 
- Should be: `/site/apidocs/org/apache/juneau/rest/RestContext/Builder.html`

**Impact**: ~1,700+ broken links (FIXED - reduced to ~1,240)

### 2. Missing Static Files

**Problem**: Some referenced files don't exist in the expected locations

**Examples**:
- `$U{servlet:/htdocs/cat.png}` - Template variable not resolved
- Various image files referenced but not found

### 3. Incorrect Documentation Paths

**Problem**: Links in `src/pages/about.md` use incorrect paths

**Examples**:
- `/docs/topics/02.01.00.JuneauMarshallBasics` 
- Should be: `/docs/topics/02.01.00.JuneauMarshallBasics` (with proper extension)

## Recommended Fixes

### Fix 1: Update Path References in Documentation

The most critical fix is to update all the relative path references in the markdown files:

```bash
# Find and replace common patterns
find docs-staging -name "*.md" -exec sed -i '' 's|../site/|/site/|g' {} \;
```

### Fix 2: Update Docusaurus Configuration

Ensure the `docusaurus.config.ts` has the correct base URL and static file serving configuration.

### Fix 3: Verify Static File Structure

Ensure all referenced static files exist in the correct locations.

## Files with Most Broken Links

1. **docs-staging/topics/08.23.00.RestContext.md** - 70+ broken links
2. **docs-staging/topics/08.24.00.RestOpContext.md** - 30+ broken links  
3. **docs-staging/topics/08.25.00.ResponseProcessors.md** - 10+ broken links
4. **docs-staging/topics/10.01.00.JuneauRestClientBasics.md** - 20+ broken links

## Next Steps

1. ✅ **COMPLETED**: Run the path fix script to correct relative path references
2. **Short-term**: Verify all static files exist and are accessible
3. **Long-term**: Set up automated link checking in CI/CD pipeline

## Actions Taken

1. **Created comprehensive link checking tools**:
   - `check-links.sh` - Main script with options for static or full checking
   - `check-static-links.js` - Fast static file link checker
   - `check-broken-links.js` - Full HTTP link checker with server support

2. **Fixed major path issues**:
   - Updated 353 files to use correct absolute paths (`/site/` instead of `../site/`)
   - Reduced broken links by 536 (30% improvement)

3. **Fixed Javadoc naming mismatches**:
   - Updated 353 files to use correct Javadoc file naming (dots instead of slashes)
   - Fixed links like `RestContext/Builder.html` → `RestContext.Builder.html`
   - Reduced broken links by 6 additional links

4. **Created automated fix script**:
   - `fix-broken-links.sh` - Script to automatically fix common path issues
   - Includes backup functionality and dry-run mode

## Tools Created

- `check-links.sh` - Main script to run link checking
- `check-static-links.js` - Static file link checker (no server required)
- `check-broken-links.js` - Full link checker with HTTP validation

## Usage

```bash
# Quick static check (recommended for development)
./check-links.sh static

# Full check with server (for comprehensive validation)
./check-links.sh full
```
