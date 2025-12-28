# Current Release Version Script

## Overview

`current-release.py` extracts the current release version from the root `pom.xml` file, removing the `-SNAPSHOT` suffix if present.

## What It Does

The script:
1. Tries to use Maven's `help:evaluate` to get the version (most reliable)
2. Falls back to parsing the `pom.xml` XML directly if Maven fails
3. Removes the `-SNAPSHOT` suffix if present
4. Prints the version number to stdout

## Usage

```bash
python3 scripts/current-release.py
```

## Output

Prints the version number (e.g., "9.2.0") to stdout.

### Example

```bash
$ python3 scripts/current-release.py
9.2.0
```

## Exit Codes

- `0` - Success, version printed to stdout
- `1` - Error, could not determine version

## Use Cases

This script is typically used by other scripts (like `release.py`) to:
- Determine the current release version
- Calculate the next version
- Generate version-specific file names

## Requirements

- Python 3.6 or higher
- Maven (optional, for primary method)
- No external Python dependencies (uses only standard library)

## Notes

- The script prefers Maven's evaluation method as it's more reliable
- Falls back to XML parsing if Maven is not available or fails
- Handles Maven POM namespaces correctly

