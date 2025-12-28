# Release Script

## Overview

`release.py` automates the complete release process for Apache Juneau, including prerequisite checks, building, testing, Maven release, binary artifact creation, and SVN distribution upload.

## What It Does

The script performs the following steps:

1. **Check Prerequisites** - Verifies required tools are available
2. **Check Java Version** - Automatically detects and verifies Java 17+
3. **Check Maven Version** - Automatically detects and verifies Maven 3+
4. **Clean Maven Repository** - Cleans local Maven repo (only on first run for a version)
5. **Make Git Folder** - Creates staging directory
6. **Clone Juneau** - Clones the repository to staging directory
7. **Configure Git** - Sets up git user and email
8. **Run Clean Verify** - Builds and verifies the project
9. **Run Javadoc Aggregate** - Generates aggregate javadocs
10. **Create Test Workspace** - Creates a test workspace
11. **Run Deploy** - Deploys to Maven staging repository
12. **Run Release Prepare** - Prepares the Maven release
13. **Run Git Diff** - Shows changes made by release:prepare
14. **Run Release Perform** - Performs the Maven release
15. **Create Binary Artifacts** - Downloads and processes source/binary artifacts
16. **Verify Distribution** - Verifies files on Apache distribution site

## Usage

### Start a New Release

```bash
python3 scripts/release.py --rc 1
```

### Resume from a Step

```bash
python3 scripts/release.py --start-step run_release_perform
```

### List Available Steps

```bash
python3 scripts/release.py --list-steps
```

### Skip Specific Steps

```bash
python3 scripts/release.py --rc 1 --skip-step clean_maven_repo
```

### Resume from Last Checkpoint

```bash
python3 scripts/release.py --resume
```

## Command-Line Options

### Required (for new releases)
- `--rc RC_NUMBER` - Release candidate number (e.g., `--rc 1` for RC1)

### Optional
- `--start-step STEP_NAME` - Start execution from the specified step
- `--list-steps` - List all available steps and exit
- `--skip-step STEP_NAME` - Skip a specific step (can be used multiple times)
- `--resume` - Resume from the last checkpoint (if available)

## Environment Variables

The script prompts for and sets the following environment variables:

- `X_VERSION` - Current release version (detected from pom.xml)
- `X_NEXT_VERSION` - Next version (calculated)
- `X_RELEASE` - Full release name (e.g., "juneau-9.2.0-RC1")
- `X_RELEASE_CANDIDATE` - Release candidate number
- `X_STAGING` - Staging directory path
- `X_USERNAME` - Apache username
- `X_EMAIL` - Apache email
- `X_GIT_BRANCH` - Git branch name
- `X_JAVA_HOME` - Java home directory
- `X_CLEANM2` - Whether to clean Maven repo (Y/N)

## History Files

The script maintains history files (`release-history-{version}.json`) that store:
- Default values for prompts
- Last run date
- Previous configuration values

This allows the script to provide intelligent defaults on subsequent runs.

## Checkpoint/Resume

The script supports checkpoint/resume functionality:
- State is saved to `~/.juneau-release-state.json`
- You can resume from any step using `--resume` or `--start-step`
- The `--rc` parameter is optional when resuming (extracted from state/history)

## Vote Email Generation

After successful distribution verification, the script automatically generates a vote email body with:
- Version and RC number
- Distribution URLs
- SHA-512 checksums (fetched from URLs)
- Git commit hash
- Vote end date (72 hours from now)

The email is printed to the console.

## Prerequisites

- Python 3.6 or higher
- Java 17 or higher
- Maven 3 or higher
- Git
- GPG (for signing)
- SVN client (for distribution upload)
- wget (for downloading artifacts)

## Exit Codes

- `0` - Success
- `1` - Error occurred

## Notes

- The script automatically excludes `docs` from the source release zip
- Source and binary artifacts are downloaded from Maven staging repository
- Distribution verification checks for expected files on Apache distribution site
- The script uses `current-release.py` and `maven-version.py` for version detection

