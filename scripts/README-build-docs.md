# Build Documentation Script

## Overview

`build-docs.py` builds the complete Apache Juneau documentation, including both Docusaurus documentation and Maven-generated Javadocs. This script replicates the documentation generation steps from the GitHub Actions workflow, allowing you to build and test the documentation locally.

## What It Does

The script performs the following steps:

1. **Installs npm dependencies** - Runs `npm ci` in the `docs` directory
2. **Compiles Java modules** - Runs `mvn clean install -DskipTests` to build all modules
3. **Generates Maven site** - Runs `mvn site -DskipTests` to generate the Maven project site (includes aggregate javadocs)
4. **Copies current javadocs** - Copies generated javadocs to `docs/static/javadocs/<current-release>/`
5. **Updates javadocs index** - Updates `releases.json` with version information
6. **Copies Maven site** - Copies the generated site to `docs/static/site/` (Docusaurus will copy to build)
7. **Builds Docusaurus** - Runs `npm run build` to generate the static Docusaurus site (copies static/ to build/)
10. **Copies .asf.yaml** - Copies the ASF configuration file to the build directory
11. **Verifies apidocs** - Checks that the javadocs were successfully generated
12. **Checks topic links** - Validates all documentation links

## Usage

### Build all documentation

```bash
cd /path/to/juneau
python3 scripts/build-docs.py
```

This will build both the Docusaurus documentation and the Maven site with javadocs.

### Skip specific steps

You can skip individual steps if you only need to rebuild part of the documentation:

```bash
# Skip npm steps (useful if you only changed Java code)
python3 scripts/build-docs.py --skip-npm

# Skip Maven steps (useful if you only changed Docusaurus docs)
python3 scripts/build-docs.py --skip-maven

# Skip copying step (useful for testing without updating build directory)
python3 scripts/build-docs.py --skip-copy

# Build for staging (sets SITE_URL for staging environment)
python3 scripts/build-docs.py --staging

# Combine options
python3 scripts/build-docs.py --skip-npm --skip-copy
```

## Command-Line Options

- `--skip-npm` - Skip npm install and Docusaurus build
- `--skip-maven` - Skip Maven compilation and site generation
- `--skip-copy` - Skip copying Maven site to static directory
- `--staging` - Build for staging (sets SITE_URL to juneau.staged.apache.org)

## Prerequisites

The script requires the following tools to be installed and available in your PATH:

- **Node.js** (version 18 or higher recommended)
- **npm** (comes with Node.js)
- **Maven** (version 3.6 or higher)
- **Java** (version 17 or higher)

The script will check for these prerequisites and exit with an error message if any are missing.

## Output

After successful execution, the documentation will be available in:

- **Docusaurus docs**: `docs/build/`
- **Maven site**: `docs/build/site/`
- **Javadocs**: `docs/build/javadocs/` (versioned, copied from static/javadocs) and `docs/build/site/apidocs/` (current)
- **Javadocs index**: `docs/build/javadocs/index.html` (dynamically loads from `releases.json`)

The script will also verify that the javadocs were generated and report the number of HTML files found.

## Example Output

```
Project root: /path/to/juneau
Docs directory: /path/to/juneau/docs
✓ All required tools are available

=== Installing npm dependencies ===
Running: npm ci
  (in directory: /path/to/juneau/docs)

=== Building Docusaurus ===
Running: npm run build
  (in directory: /path/to/juneau/docs)

=== Compiling Java modules ===
Running: mvn clean compile -DskipTests

=== Generating Maven site ===
Running: mvn site -DskipTests

=== Copying current javadocs to versioned folder ===

=== Verifying apidocs generation ===
✓ Javadocs found in /path/to/juneau/target/site/apidocs
✓ Found 1234 HTML files in apidocs

=== Copying Maven site to build directory ===
Copying /path/to/juneau/target/site to /path/to/juneau/docs/build/site
Copying /path/to/juneau/.asf.yaml to /path/to/juneau/docs/build

=== Documentation build complete ===
Documentation is available in: /path/to/juneau/docs/build
```

## Exit Codes

- `0` - Success, all documentation built successfully
- `1` - Error occurred during build (command failed, missing prerequisites, etc.)

## When to Use

Run this script:

- **Before committing documentation changes** - Verify that your changes build correctly
- **When testing documentation locally** - Build the complete documentation site for local review
- **After updating Java code** - Regenerate javadocs to reflect API changes
- **After updating Docusaurus content** - Rebuild the static site to see your changes
- **To debug CI/CD issues** - Reproduce the documentation build process locally

## Common Use Cases

### Quick rebuild after Java changes

If you only changed Java code and want to regenerate javadocs:

```bash
python3 scripts/build-docs.py --skip-npm
```

### Quick rebuild after Docusaurus changes

If you only changed Docusaurus markdown files:

```bash
python3 scripts/build-docs.py --skip-maven
```

### Test without updating build directory

If you want to test the build process without modifying the build directory:

```bash
python3 scripts/build-docs.py --skip-copy
```

The Maven site will still be generated in `target/site/`, but won't be copied to `docs/static/site/`.

## Troubleshooting

### Missing tools

If you get an error about missing tools:

```
ERROR: Missing required tools: Node.js, npm
Please install the missing tools and try again.
```

Install the missing tools and ensure they're in your PATH.

### npm install fails

If `npm ci` fails, try:

```bash
cd docs
rm -rf node_modules package-lock.json
npm install
```

Then run the script again.

### Maven build fails

If Maven commands fail, check:

1. Java version: `java -version` should show Java 17 or higher
2. Maven version: `mvn -version` should show Maven 3.6 or higher
3. Project compilation: Try `mvn clean compile` manually to see detailed error messages

### Javadocs not generated

If the script reports that apidocs were not found:

1. Check if `mvn javadoc:aggregate` runs successfully when called directly
2. Verify that all modules have `Automatic-Module-Name` in their MANIFEST.MF
3. Check the Maven output for errors or warnings

## Requirements

- Python 3.6 or higher
- No external Python dependencies (uses only standard library)
- Node.js 18+ and npm
- Maven 3.6+ and Java 17+

## Notes

- The script automatically skips tests (`-DskipTests`) to speed up the build
- All commands are run from the project root directory
- The script will exit immediately if any command fails (unless using `--skip-*` options)
- The build directory (`docs/build/`) will be created if it doesn't exist
- Existing site contents will be replaced when copying

## Build Order

The script copies Maven site to `static/site/` and updates javadocs in `static/javadocs/` **before** building Docusaurus. Docusaurus automatically copies everything from the `static/` directory to the `build/` directory during the build process, so the files are available when Docusaurus processes links, preventing broken link warnings.

## Integration with CI/CD

This script is used by the documentation release scripts:
- `release-docs-stage.py` - Calls `build-docs.py --staging` to build for staging
- `release-docs.py` - Promotes already-built documentation from staging to production

## Related Scripts

- `release-docs-stage.py` - Deploys built documentation to `asf-staging` branch
- `release-docs.py` - Promotes documentation from `asf-staging` to `asf-site` branch

