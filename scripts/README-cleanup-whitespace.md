# Whitespace Cleanup Script

## Overview

`cleanup-whitespace.py` is a Python script that cleans up common whitespace inconsistencies in Java files.

## What It Does

The script performs the following cleanup operations:

1. **Removes consecutive blank lines** - Maximum of 1 blank line allowed between code sections
2. **Removes trailing whitespace** - Strips whitespace from the end of all lines
3. **Removes blank lines before final closing brace** - Ensures files end cleanly with `}`
4. **Ensures proper file endings** - Files end with `}` with no trailing newline

## Usage

### Clean the entire Juneau codebase

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/cleanup-whitespace.py
```

### Clean a specific directory

```bash
python3 scripts/cleanup-whitespace.py juneau-core/juneau-marshall
```

### Clean a specific module

```bash
python3 scripts/cleanup-whitespace.py juneau-utest/src/test/java
```

## Output

The script will:
- Scan for all `.java` files (excluding `target/` and `.git/` directories)
- Report each file that was modified
- Provide a summary of total files scanned and modified

Example output:
```
Scanning for Java files in: /Users/james.bognar/git/juneau
Found 1247 Java files
✓ Cleaned: juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanMap.java
✓ Cleaned: juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanContext.java
...

Summary:
  Total files scanned: 1247
  Files modified: 342
  Files unchanged: 905
```

## Safety

- The script only modifies `.java` files
- Original file encoding (UTF-8) is preserved
- If an error occurs processing a file, it is logged but doesn't stop the script
- It's recommended to commit your changes before running the script so you can review the diff

## Testing

After running the script, verify everything still compiles and tests pass:

```bash
mvn clean test
```

## Requirements

- Python 3.6 or higher
- No external dependencies (uses only standard library)

