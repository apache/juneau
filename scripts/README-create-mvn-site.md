# Create Maven Site Script

## Overview

`create-mvn-site.py` generates the Maven site with Javadocs and copies it to the Docusaurus static directory for local testing and broken link validation.

## What It Does

The script performs these operations:

1. **Detects Version** - Automatically gets the project version from POM
2. **Cleans Old Site** - Removes existing `target/site` directory
3. **Generates Site** - Runs `mvn clean compile site` to create the Maven site
4. **Copies to Docusaurus** - Moves site to `/juneau-docs/static/site/`
5. **Logs Output** - Creates `create-mvn-site.log` in project root

## Usage

### Generate Maven Site

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/create-mvn-site.py
```

The generated site will be available at `http://localhost:3000/site/` (after starting Docusaurus).

## Output Example

```
Creating Maven site with javadocs for local testing...
Working from: /Users/james.bognar/git/juneau
Detected project version: 9.2.0-SNAPSHOT

Generating maven site for root project...
Removing existing site directory: /Users/james.bognar/git/juneau/target/site

Running Maven site generation...
[INFO] Scanning for projects...
[INFO] Building Apache Juneau 9.2.0-SNAPSHOT
...
[INFO] BUILD SUCCESS

Found Maven site in: /Users/james.bognar/git/juneau/target/site

Setting up local testing directory for Docusaurus...
Removing existing static site directory: /Users/james.bognar/git/juneau/juneau-docs/static/site

Copying entire Maven site to Docusaurus static directory...
Copying directory: apidocs/
Copying directory: css/
Copying file: index.html
...

*******************************************************************************
***** SUCCESS *****************************************************************
*******************************************************************************
Maven site has been generated and copied successfully!
Complete Maven site is now available in: /Users/james.bognar/git/juneau/juneau-docs/static/site/
This includes javadocs, project reports, and all other site content.
You can now access it at: http://localhost:3000/site/
Ready for broken link testing in your Docusaurus documentation!
```

## What Gets Generated

The Maven site includes:

- **Javadocs** - `/site/apidocs/` - Complete API documentation
- **Project Information** - Summary, team, dependencies
- **Project Reports** - Test reports, code coverage (if configured)
- **Site Navigation** - Full Maven site structure
- **CSS/Assets** - All styling and resources

## When to Use

Use this script when:
- **Testing Documentation Links** - Validate links between docs and Javadocs
- **Preparing for Release** - Preview the complete site structure
- **Broken Link Testing** - Find and fix broken links before deployment
- **Local Development** - Test Javadoc generation without full build

## Typical Workflow

### Test Documentation Links

```bash
# Step 1: Generate Maven site
python3 scripts/create-mvn-site.py

# Step 2: Start Docusaurus
python3 scripts/start-docusaurus.py

# Step 3: Test in browser
# Visit http://localhost:3000
# Click through documentation
# Verify all links to /site/apidocs/ work correctly

# Step 4: Check for broken links
# Use browser tools or link checkers
```

### Verify Javadoc Generation

```bash
# Generate site
python3 scripts/create-mvn-site.py

# Check for errors in log
less create-mvn-site.log

# Browse Javadocs
open http://localhost:3000/site/apidocs/index.html
```

## Output Location

After running the script:

```
juneau-docs/
└── static/
    └── site/              # Maven site (temporary, for testing)
        ├── apidocs/       # Javadocs
        ├── css/           # Styles
        ├── images/        # Images
        ├── index.html     # Site home
        └── ...            # Other Maven site files
```

**Note:** The `/static/site/` directory is temporary and typically not committed to Git. It's for local testing only.

## Log File

The script creates a log file: `/Users/james.bognar/git/juneau/create-mvn-site.log`

This contains the complete Maven output for debugging.

**Check the log if:**
- Site generation fails
- Javadoc warnings occur
- You need detailed build information

```bash
# View the log
cat create-mvn-site.log

# Search for errors
grep -i error create-mvn-site.log

# Search for warnings
grep -i warning create-mvn-site.log
```

## Troubleshooting

### Maven Site Generation Fails

**Check Java version:**
```bash
java -version
mvn -version
```

**Ensure project compiles:**
```bash
mvn clean compile
```

**Check the log file:**
```bash
tail -50 create-mvn-site.log
```

### Out of Memory Errors

Increase Maven heap size:

```bash
export MAVEN_OPTS="-Xmx4g"
python3 scripts/create-mvn-site.py
```

### Javadoc Warnings/Errors

Javadoc generation is strict. Common issues:
- Missing `@param` or `@return` tags
- Invalid HTML in Javadoc comments
- Broken `@link` references

Check the log for specific Javadoc errors.

### Site Not Appearing in Browser

1. Make sure Docusaurus is running: `python3 scripts/start-docusaurus.py`
2. Clear browser cache
3. Check that files exist: `ls -la juneau-docs/static/site/`
4. Visit directly: `http://localhost:3000/site/index.html`

## Requirements

- **Python**: 3.6 or higher
- **Maven**: 3.6 or higher
- **Java**: JDK 11 or higher (for Javadoc generation)
- **Project Built**: Run `mvn install` at least once

## Replacing the Old Script

This Python script replaces `/juneau-docs/create-mvn-site.sh` with:
- ✅ No dependency on `juneau-env.sh`
- ✅ Automatic version detection from POM
- ✅ Better error handling
- ✅ Real-time output streaming
- ✅ Cross-platform compatibility
- ✅ Clearer progress messages

## Performance Notes

- **Time**: Generation typically takes 5-15 minutes depending on system
- **Disk Space**: Maven site requires ~100-200 MB
- **CPU/Memory**: Maven uses significant resources during generation
- **First Run**: Slower if Maven needs to download dependencies

## Notes

- The script automatically detects the Juneau root directory
- Old site directories are cleaned before generation
- Output is both displayed and logged to file
- The generated site is a snapshot - not kept in sync with code changes
- Run this script again after making significant code/documentation changes

