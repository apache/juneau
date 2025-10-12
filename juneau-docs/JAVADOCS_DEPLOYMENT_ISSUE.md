# Javadocs Deployment Issue

## Problem

Git push is failing with HTTP 406 error because **Apache GitBox does not support Git LFS**.

```
batch response: Client error from HTTP 406
```

## Root Cause

- Javadocs are configured to use Git LFS (.gitattributes)
- Apache's GitBox repository hosting doesn't support LFS
- Total javadocs size: 1.6 GB (too large for regular Git)

## Recommended Solutions

### Option 1: External Hosting (RECOMMENDED)

Host javadocs separately from the main repository:

**Apache Distribution:**
```bash
# Upload to Apache distribution server
scp -r static/javadocs/ people.apache.org:/www/juneau.apache.org/javadocs/
```

**Or use Apache's artifact repository:**
- Store in Maven Central or Apache's artifact repository
- Download during website build/deployment
- Keep git repo lean

**Benefits:**
- No git repo bloat
- Faster clones
- Apache-approved approach

### Option 2: .gitignore Javadocs

Add to `.gitignore`:
```
juneau-docs/static/javadocs/*/
!juneau-docs/static/javadocs/README.md
!juneau-docs/static/javadocs/index.html
```

Deploy javadocs separately during CI/CD.

### Option 3: Keep Only Recent Versions

Commit only the last 2-3 versions (~400-600 MB):
```bash
# Remove old versions
rm -rf juneau-docs/static/javadocs/{7.*,8.0.0,8.1.*}

# Update index.html to only show recent versions
```

### Option 4: Remove LFS, Keep Existing (CURRENT STATE)

If you already pushed the old versions with LFS, you have two choices:

**A) Leave existing LFS files, add new ones as regular:**
- Old versions (7.1.0-9.0.1): Stay in LFS (already pushed)
- New versions (9.1.0+): Add as regular Git files
- Mixed approach but works

**B) Complete LFS removal (complex):**
Requires rewriting history and force-pushing (coordinate with team).

## Current Status

- `.gitattributes`: Cleared of LFS rules
- Old javadocs (7.1.0-9.0.1): Already in repo with LFS
- New javadocs (9.1.0): Not yet committed
- LFS: Uninstalled locally

## Immediate Next Steps

### Quick Fix (Mixed Approach):

1. Leave old versions as-is (they're already in LFS)
2. Add new 9.1.0 without LFS:

```bash
# Ensure LFS is off
git lfs uninstall

# Clear LFS from .gitattributes (already done)

# Add new files as regular git
git add .gitattributes
git add juneau-docs/static/javadocs/9.1.0
git add juneau-docs/static/javadocs/latest
git add juneau-docs/static/javadocs/index.html
git add juneau-docs/docusaurus.config.ts

# Commit
git commit -m "Add javadocs 9.1.0 (regular git, not LFS)"

# Push
git push origin master
```

### Long-term Solution:

Contact Apache Infrastructure (users@infra.apache.org) to discuss:
1. Enabling LFS for juneau.git, OR
2. Setting up external javadoc hosting

## Apache Infra Contact

For questions about LFS or external hosting:
- Email: users@infra.apache.org
- Jira: https://issues.apache.org/jira/browse/INFRA

## References

- Apache Infra Git Services: https://infra.apache.org/git.html
- Apache Infra FAQ: https://infra.apache.org/
