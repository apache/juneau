# Add Versioned Javadocs Script

## Overview

`add-versioned-javadocs.py` adds versioned Javadocs to the static documentation site, maintaining a history of API documentation for all releases.

## What It Does

The script performs these operations:

1. **Validates Source** - Ensures the source directory contains valid Javadocs
2. **Checks for Conflicts** - Prompts if the version already exists
3. **Copies Javadocs** - Copies all Javadoc files to the versioned directory
4. **Updates "Latest"** - Optionally creates/updates the "latest" link (with `--make-latest`)
5. **Creates Symlink** - Uses symlinks when possible, falls back to copying

## Usage

### Add a New Version

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/add-versioned-javadocs.py 9.2.0 target/site/apidocs
```

### Add and Make it the Latest

```bash
python3 scripts/add-versioned-javadocs.py 9.2.0 target/site/apidocs --make-latest
```

### Add from External Location

```bash
python3 scripts/add-versioned-javadocs.py 9.1.0 /path/to/old/javadocs
```

## Arguments

- **`version`** (required) - Version number (e.g., 9.2.0, 9.1.0, 8.2.1)
- **`source_path`** (required) - Path to Javadocs directory (must contain `index.html`)
- **`--make-latest`** (optional) - Update the "latest" symlink to this version

## Output Example

```bash
$ python3 scripts/add-versioned-javadocs.py 9.2.0 target/site/apidocs --make-latest

[INFO] Creating directory for version 9.2.0...
[INFO] Copying javadocs from target/site/apidocs to /Users/james.bognar/git/juneau/juneau-docs/static/javadocs/9.2.0...
[INFO] Successfully added javadocs for version 9.2.0
[INFO] Updating 'latest' to point to version 9.2.0...
[INFO] Created symlink: latest -> 9.2.0

================================================
[INFO] Javadocs for version 9.2.0 successfully added!
================================================

  Location: /Users/james.bognar/git/juneau/juneau-docs/static/javadocs/9.2.0
  URL (after deployment): /javadocs/9.2.0/
  Latest URL: /javadocs/latest/

[INFO] Next steps:
  1. Review the copied files: ls -lh /Users/james.bognar/git/juneau/juneau-docs/static/javadocs/9.2.0
  2. Test locally: python3 scripts/start-docusaurus.py
  3. Visit http://localhost:3000/javadocs/
  4. Commit the changes if everything looks good
  5. Consider updating index.html to add this version to the version grid
```

## Version Overwrite

If a version already exists, the script will prompt:

```
[WARN] Version 9.2.0 already exists at /Users/james.bognar/git/juneau/juneau-docs/static/javadocs/9.2.0
Do you want to overwrite it? (y/N)
```

- Type `y` or `yes` to overwrite
- Type `n` or press Enter to abort

## Directory Structure

After adding versions:

```
juneau-docs/
└── static/
    └── javadocs/
        ├── 8.2.1/         # Old release
        ├── 9.0.0/         # Previous release
        ├── 9.1.0/         # Previous release
        ├── 9.2.0/         # Current release
        └── latest -> 9.2.0  # Symlink to current (if --make-latest used)
```

## The "Latest" Link

The `--make-latest` flag updates a special "latest" link that always points to the most recent version.

### Symlink (Preferred)

On systems that support symlinks (macOS, Linux, Git for Windows):
```
latest -> 9.2.0
```

### Copy (Fallback)

On systems that don't support symlinks (some Windows configurations):
```
latest/  (full copy of 9.2.0/)
```

**URL:** `http://localhost:3000/javadocs/latest/` always shows the current version.

## When to Use

### New Release

```bash
# Step 1: Build project with Javadocs
mvn clean install javadoc:javadoc

# Step 2: Add versioned Javadocs
python3 scripts/add-versioned-javadocs.py 9.2.0 target/site/apidocs --make-latest

# Step 3: Test locally
python3 scripts/start-docusaurus.py
# Visit http://localhost:3000/javadocs/9.2.0/

# Step 4: Commit
git add juneau-docs/static/javadocs/
git commit -m "Added Javadocs for version 9.2.0"
```

### Adding Historical Versions

If you have old Javadocs archived somewhere:

```bash
# Add old versions (don't make them latest)
python3 scripts/add-versioned-javadocs.py 9.0.0 /path/to/archived/9.0.0/apidocs
python3 scripts/add-versioned-javadocs.py 9.1.0 /path/to/archived/9.1.0/apidocs

# Add current version as latest
python3 scripts/add-versioned-javadocs.py 9.2.0 target/site/apidocs --make-latest
```

### Updating Documentation Between Releases

During development, you might regenerate Javadocs multiple times:

```bash
# Regenerate Javadocs
mvn javadoc:javadoc

# Update the SNAPSHOT version (overwrite)
python3 scripts/add-versioned-javadocs.py 9.2.0-SNAPSHOT target/site/apidocs --make-latest
# Type 'y' when prompted to overwrite
```

## Validation

The script validates the source before copying:

1. **Directory Exists** - Source path must be a valid directory
2. **Contains Javadocs** - Must have an `index.html` file
3. **Not Empty** - Directory must contain actual content

If validation fails, you'll see:
```
[ERROR] Source path does not exist: /path/to/nonexistent
```

or

```
[ERROR] Source path does not appear to contain javadocs (missing index.html)
```

## Troubleshooting

### Source Path Not Found

Make sure you've generated Javadocs first:

```bash
mvn javadoc:javadoc
ls -la target/site/apidocs/index.html
```

### Permission Denied

If you get permission errors:

```bash
# Fix permissions (macOS/Linux)
sudo chown -R $USER:$USER juneau-docs/static/javadocs/

# Or use sudo (not recommended)
sudo python3 scripts/add-versioned-javadocs.py 9.2.0 target/site/apidocs
```

### Symlink Not Created

On Windows or some file systems, symlinks might not work. The script automatically falls back to copying:

```
[WARN] Symlinks not supported, copying instead...
[INFO] Copied to 'latest' directory
```

This is normal and the site will work the same way.

### Version Already Exists

If you see this warning:
```
[WARN] Version 9.2.0 already exists
```

Options:
1. Answer `y` to overwrite (useful for SNAPSHOT versions)
2. Answer `n` and use a different version number
3. Manually delete the old version first: `rm -rf juneau-docs/static/javadocs/9.2.0`

## Requirements

- **Python**: 3.6 or higher
- **Javadocs**: Pre-generated (run `mvn javadoc:javadoc` first)
- **Disk Space**: Each version requires ~50-100 MB

## Replacing the Old Script

This Python script replaces `/juneau-docs/add-javadocs.sh` with:
- ✅ Better validation and error messages
- ✅ Clearer prompts and output
- ✅ Cross-platform symlink support with fallback
- ✅ More robust copy operations
- ✅ Colored output for better UX

## Best Practices

1. **Use Semantic Versioning** - Follow the pattern: `MAJOR.MINOR.PATCH`
2. **Always Use `--make-latest` for Current Release** - Keeps the "latest" link updated
3. **Don't Make Old Versions Latest** - Only the current release should be "latest"
4. **Test Before Committing** - Always view Javadocs in browser first
5. **Commit Versioned Javadocs** - They're part of the documentation history

## Notes

- Javadocs are static files and safe to commit to Git
- Each version is independent - no shared files between versions
- The "latest" link is for convenience - users can also access by specific version
- Javadoc URLs in documentation should use version numbers, not "latest"
- The script preserves timestamps and permissions from the source

