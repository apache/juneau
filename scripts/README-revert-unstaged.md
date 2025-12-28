# Revert Unstaged Changes Script

## Overview

`revert-unstaged.py` reverts working directory changes back to what's in the staging area (INDEX), preserving any staged changes.

## What It Does

The script:
1. Verifies the file exists
2. Runs `git restore --source=INDEX <file>` to revert unstaged changes
3. Preserves any staged changes

## Usage

```bash
python3 scripts/revert-unstaged.py <file_path>
```

### Example

```bash
python3 scripts/revert-unstaged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java
```

## Use Case

This is useful when you have:
- Staged changes that you've tested and want to keep
- Unstaged changes that you want to discard

The script will revert the working directory to match the staged version, effectively discarding only the unstaged changes.

## Exit Codes

- `0` - Success
- `1` - Error (file not found, git command failed, etc.)

## Requirements

- Python 3.6 or higher
- Git (git command must be in PATH)
- No external Python dependencies (uses only standard library)

## Notes

- This only affects unstaged changes - staged changes are preserved
- Use `revert-staged.py` if you want to revert both staged and unstaged changes
- The file must exist (the script verifies this)

