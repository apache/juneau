# Build and Push Script

## Overview

`build-and-push.py` automates the complete build, test, and deployment workflow for Juneau, ensuring code quality before pushing changes to the remote repository.

## What It Does

The script executes the following steps in order:

1. **Run Tests** - Executes `mvn test` to ensure all tests pass
2. **Build and Install** - Runs `mvn clean package install` to build the project
3. **Generate Javadocs** - Executes `mvn javadoc:javadoc` to create API documentation
4. **Git Commit** - Stages all changes and commits with the provided message
5. **Git Push** - Pushes the commit to the remote repository

If any step fails, the script stops immediately and reports the failure.

## Usage

### Basic Usage

```bash
cd /Users/james.bognar/git/juneau
python3 scripts/build-and-push.py "Your commit message here"
```

### Skip Tests (for documentation-only changes)

```bash
python3 scripts/build-and-push.py "Updated README" --skip-tests
```

### Skip Javadoc Generation

```bash
python3 scripts/build-and-push.py "Minor code fix" --skip-javadoc
```

### Skip Both Tests and Javadoc

```bash
python3 scripts/build-and-push.py "Quick formatting fix" --skip-tests --skip-javadoc
```

### Dry Run (see what would happen without actually doing it)

```bash
python3 scripts/build-and-push.py "Testing changes" --dry-run
```

## Command-Line Options

### Required Arguments

- `message` - The Git commit message (must be provided)

### Optional Flags

- `--skip-tests` - Skip running tests (useful for documentation-only changes)
- `--skip-javadoc` - Skip Javadoc generation
- `--dry-run` - Show what would be done without actually executing commands

## Examples

### Example 1: Full Build and Push

```bash
python3 scripts/build-and-push.py "Fixed bug in RestClient connection handling"
```

Output:
```
======================================================================
🚀 Juneau Build and Push Script
======================================================================
Working directory: /Users/james.bognar/git/juneau
Commit message: 'Fixed bug in RestClient connection handling'
======================================================================

🧪 Step 1: Running tests...
Running: mvn test
✅ Step 1: Running tests... - SUCCESS

🏗️  Step 2: Building and installing project...
Running: mvn clean package install
✅ Step 2: Building and installing project... - SUCCESS

📚 Step 3: Generating Javadocs...
Running: mvn javadoc:javadoc
✅ Step 3: Generating Javadocs... - SUCCESS

📝 Step 4: Committing changes to Git...
  4.1: Staging all changes...
Running: git add .
✅   4.1: Staging all changes... - SUCCESS
  4.2: Creating commit...
Running: git commit -m Fixed bug in RestClient connection handling
✅   4.2: Creating commit... - SUCCESS
✅ Step 4: Git commit completed.

🚀 Step 5: Pushing changes to remote repository...
Running: git push
✅ Step 5: Pushing changes to remote repository... - SUCCESS

======================================================================
🎉 All operations completed successfully!
📦 Commit message: 'Fixed bug in RestClient connection handling'
======================================================================
```

### Example 2: Documentation Changes (Skip Tests)

```bash
python3 scripts/build-and-push.py "Updated REST client documentation" --skip-tests
```

### Example 3: Formatting Changes (Skip Tests and Javadoc)

```bash
python3 scripts/build-and-push.py "Fixed code formatting in BeanContext" --skip-tests --skip-javadoc
```

### Example 4: Dry Run

```bash
python3 scripts/build-and-push.py "Testing my changes" --dry-run
```

Output:
```
🔍 DRY RUN MODE - No actual changes will be made

Steps that would be executed:
  1. Run tests: mvn test
  2. Build and install: mvn clean package install
  3. Generate Javadocs: mvn javadoc:javadoc
  4. Commit changes: git add . && git commit -m "Testing my changes"
  5. Push to remote: git push

Dry run complete. Use without --dry-run to execute.
```

## Exit Codes

- `0` - Success, all operations completed
- `1` - Failure at any step

## Error Handling

The script uses a fail-fast approach:
- If tests fail, the build is aborted immediately
- If the build fails, Javadoc generation is skipped
- If Javadoc generation fails, the commit is not created
- If the commit fails, the push is not attempted
- If the push fails, you'll have a local commit that needs to be pushed manually

## Safety Features

1. **No Changes Detection** - If there are no changes to commit, the script skips commit and push steps
2. **Fail-Fast** - Stops immediately on any error to prevent incomplete deployments
3. **Clear Output** - Each step is clearly marked with emojis and status indicators
4. **Dry Run Mode** - Preview what will happen without making any changes

## When to Use

### Use the full script when:
- Making code changes that affect functionality
- Adding new features
- Fixing bugs
- Refactoring code

### Use `--skip-tests` when:
- Only updating documentation (README, Javadocs, markdown files)
- Making cosmetic changes (formatting, comments)
- Tests have already been run successfully in another context

### Use `--skip-javadoc` when:
- Making changes that don't affect public APIs
- Quick bug fixes
- Internal code refactoring

### Use `--dry-run` when:
- Testing the script itself
- Verifying what steps will be executed
- Learning how the script works

## Requirements

- Python 3.6 or higher
- Maven (mvn command must be in PATH)
- Git (git command must be in PATH)
- No external Python dependencies (uses only standard library)

## Replacing the Old Shell Script

This Python script replaces `build-and-push.sh` with:
- Better error handling and reporting
- Optional step skipping (--skip-tests, --skip-javadoc)
- Dry run mode for testing
- More detailed output with step numbering
- Cross-platform compatibility (Windows, macOS, Linux)
- Argument parsing with help documentation
- Check for uncommitted changes before attempting commit

