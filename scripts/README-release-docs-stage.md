# Release Documentation to Staging Script

## Overview

`release-docs-stage.py` builds the documentation and deploys it to the `asf-staging` branch, making it available on the staging website.

## What It Does

The script:
1. Runs `build-docs.py` with `--staging` flag to build documentation for staging
2. Clones the repository to a temporary directory
3. Checks out the `asf-staging` branch (or creates it if it doesn't exist)
4. Removes all existing files (except `.git`)
5. Copies the built documentation from `juneau-docs/build` to the temp directory
6. Adds and commits the changes
7. Pushes to the remote `asf-staging` branch

## Usage

### Deploy to Staging

```bash
python3 scripts/release-docs-stage.py
```

### Custom Commit Message

```bash
python3 scripts/release-docs-stage.py --commit-message "Updated API documentation"
```

### Test Without Pushing

```bash
python3 scripts/release-docs-stage.py --no-push
```

## Command-Line Options

- `--no-push` - Build and commit but don't push to remote
- `--commit-message MESSAGE` - Custom commit message (default: "Deploy documentation staging")

## Prerequisites

- Python 3.6 or higher
- Git (git command must be in PATH)
- All prerequisites for `build-docs.py` (Node.js, npm, Maven, Java)
- Write access to the remote repository

## Exit Codes

- `0` - Success
- `1` - Error occurred

## Notes

- The script uses a temporary directory for git operations
- The temp directory is automatically cleaned up after successful push
- If `--no-push` is used, the temp directory is not cleaned up (for manual inspection)
- The script sets `SITE_URL` environment variable to `https://juneau.staged.apache.org` when building
- A success sound is played when the script completes successfully

## Workflow

This script is typically used as part of the documentation release workflow:

1. Make documentation changes
2. Run `release-docs-stage.py` to deploy to staging
3. Review the staging site
4. Run `release-docs.py` to promote to production

