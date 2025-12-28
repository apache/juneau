# Test Script

## Overview

`test.py` is a build and test helper script for Apache Juneau. It provides a convenient way to run Maven builds and tests with various options.

## What It Does

The script can:
- Build the project (clean install without tests)
- Run tests
- Do both (full clean build + tests)

## Usage

### Full Build and Test (Default)

```bash
python3 scripts/test.py
# or
python3 scripts/test.py --full
# or
python3 scripts/test.py -f
```

This performs a clean build and runs all tests.

### Build Only (Skip Tests)

```bash
python3 scripts/test.py --build-only
# or
python3 scripts/test.py -b
```

This runs `mvn clean install -DskipTests` to build the project without running tests.

### Test Only (No Build)

```bash
python3 scripts/test.py --test-only
# or
python3 scripts/test.py -t
```

This runs `mvn test -Drat.skip=true` to run tests without rebuilding.

### Verbose Output

```bash
python3 scripts/test.py --verbose
# or
python3 scripts/test.py -v
```

Shows full Maven output instead of just the last 50 lines.

## Command-Line Options

- `--build-only, -b` - Only build (skip tests)
- `--test-only, -t` - Only run tests (no build)
- `--full, -f` - Clean build + run tests (default)
- `--verbose, -v` - Show full Maven output
- `--help, -h` - Show help message

## Examples

### Quick Test Run

```bash
python3 scripts/test.py -t
```

### Rebuild After Code Changes

```bash
python3 scripts/test.py -b
```

### Full Build and Test with Verbose Output

```bash
python3 scripts/test.py -f -v
```

## Output

By default, the script shows the last 50 lines of Maven output. Use `--verbose` to see the full output.

The script also attempts to parse test results and display failure/error counts if tests fail.

## Exit Codes

- `0` - Success
- `1` - Failure (build or tests failed)

## Requirements

- Python 3.6 or higher
- Maven (mvn command must be in PATH)
- No external Python dependencies (uses only standard library)

## Notes

- The script automatically skips RAT checks when running tests (`-Drat.skip=true`)
- Tests are skipped during build (`-DskipTests`)
- All commands are run from the project root directory

