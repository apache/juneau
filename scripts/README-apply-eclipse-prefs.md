# Apply Eclipse Preferences Script

## Overview

`apply-eclipse-prefs.py` applies Eclipse IDE preference files to all Juneau project modules, ensuring consistent code formatting and IDE configuration across all developers.

## What It Does

The script copies Eclipse JDT (Java Development Tools) preferences from the `eclipse-preferences` directory to each module's `.settings` folder. There are two sets of preferences:

1. **source-prefs** - Applied to main source modules (28 modules)
   - Code formatting rules
   - Code cleanup rules
   - Compiler settings
   - Save actions

2. **test-prefs** - Applied to test modules (2 modules)
   - Same as source-prefs but optimized for test code
   - Applied to: `juneau-utest` and `*-ftest` modules

## Usage

### Apply preferences to all modules

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/apply-eclipse-prefs.py
```

### Apply preferences from a different directory

```bash
python3 scripts/apply-eclipse-prefs.py /path/to/juneau
```

## Preference Files

The script copies these files to each module's `.settings` directory:

- `org.eclipse.jdt.core.prefs` - Compiler and formatter settings
- `org.eclipse.jdt.ui.prefs` - UI preferences (save actions, code templates, etc.)

## Eclipse Preferences Location

All Eclipse preference files are stored in:
```
scripts/eclipse-preferences/
├── source-prefs/
│   ├── org.eclipse.jdt.core.prefs
│   └── org.eclipse.jdt.ui.prefs
├── test-prefs/
│   ├── org.eclipse.jdt.core.prefs
│   └── org.eclipse.jdt.ui.prefs
├── juneau-cleanup-rules.xml
├── juneau-code-templates.xml
├── juneau-formatter-rules.xml
├── juneau-organize-imports.importorder
├── user-dictionary.txt
└── user-dictionary.xml
```

## Output

The script provides detailed progress and a summary:

```
Applying Eclipse preferences to Juneau modules in: /Users/james.bognar/git/juneau

Applying source preferences...
✓ Preferences applied to juneau-bean/juneau-bean-atom
✓ Preferences applied to juneau-bean/juneau-bean-common
...

Applying test preferences...
✓ Preferences applied to juneau-utest
✓ Preferences applied to juneau-examples/juneau-examples-rest-jetty-ftest

============================================================
Summary:
  Total projects: 30
  Successfully updated: 30
  Failed: 0
============================================================
```

## When to Use

Run this script when:
- Setting up a new development environment
- After cloning the Juneau repository
- After Eclipse preference files are updated in the repository
- To sync your IDE settings with the project standards

## Modules Covered

### Source Modules (28)
- All juneau-bean modules
- All juneau-core modules  
- All juneau-examples modules
- All juneau-microservice modules
- All juneau-rest modules
- All juneau-sc modules

### Test Modules (2)
- juneau-utest
- juneau-examples-rest-jetty-ftest

## Requirements

- Python 3.6 or higher
- No external dependencies (uses only standard library)

## Replacing the Old Shell Script

This Python script replaces `juneau-apply-prefs.sh` with:
- Updated module list matching current project structure
- Better error handling and reporting
- Cross-platform compatibility (works on Windows, macOS, Linux)
- Clearer output and progress indication

