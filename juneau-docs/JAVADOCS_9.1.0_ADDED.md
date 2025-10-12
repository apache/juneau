# Version 9.1.0 Javadocs Added

**Date:** October 12, 2024

## Changes Made

### ✅ Created 9.1.0 Directory
- Copied from 9.0.1: `cp -r 9.0.1 9.1.0`
- Size: 176 MB (same as 9.0.1)
- Location: `juneau-docs/static/javadocs/9.1.0/`

### ✅ Updated `latest` Symlink
- Changed from: `latest -> 9.0.1`
- Changed to: `latest -> 9.1.0`

### ✅ Updated Landing Page (`index.html`)
- Updated "Latest Stable Release" card to show 9.1.0
- Added 9.1.0 as the first version in the 9.x section
- Updated Release Notes link to point to `/docs/release-notes/9.1.0`

## Current Status

**Total Versions:** 13 (added 1)
**Total Size:** 1.6 GB (increased by 176 MB)

### Version List
- 9.1.0 ← NEW (Latest)
- 9.0.1
- 9.0.0
- 8.2.0
- 8.1.3
- 8.1.2
- 8.1.1
- 8.1.0
- 8.0.0
- 7.2.2
- 7.2.1
- 7.2.0
- 7.1.0

## URLs After Deployment

- **Latest:** https://juneau.apache.org/javadocs/latest/ → 9.1.0
- **Specific:** https://juneau.apache.org/javadocs/9.1.0/
- **Landing:** https://juneau.apache.org/javadocs/

## Next Steps

The javadocs are identical to 9.0.1 (as you mentioned, only version and dependency changes). If you need to update any version-specific references within the javadocs themselves, you can do a find/replace:

```bash
# Example: Update version strings in HTML files (if needed)
cd juneau-docs/static/javadocs/9.1.0
find . -type f -name "*.html" -exec sed -i '' 's/9\.0\.1/9.1.0/g' {} +
```

However, since you mentioned the docs are essentially identical except for version and dependencies, the current copy should work fine!

## Ready to Commit

With Git LFS configured, you can commit:

```bash
git add juneau-docs/static/javadocs/9.1.0
git add juneau-docs/static/javadocs/latest
git add juneau-docs/static/javadocs/index.html
git commit -m "Add javadocs for version 9.1.0 and update latest"
git push
```

---

**Note:** The javadocs content is identical to 9.0.1. If you need to update any version references within the javadocs files themselves, let me know and I can help with that.
