# SonarQube Issues Management Tools

This directory contains tools for categorizing and managing SonarQube issues in the Apache Juneau project.

## Overview

The Apache Juneau project has **3,116 SonarQube issues** that have been categorized into **30 categories** for systematic fixing.

## Quick Start

### 1. View Category Summary

```bash
cd /Users/james.bognar/git/apache/juneau/master
cat SONARQUBE_ISSUES_SUMMARY.md
```

### 2. List All Categories

```bash
python3 scripts/view-sonar-category.py
```

### 3. View Specific Category Details

```bash
# View Security Issues (high priority)
python3 scripts/view-sonar-category.py "Security Issues"

# View with custom limit
python3 scripts/view-sonar-category.py "Brain Methods (Complexity)" --limit 5
```

### 4. Start Interactive Fixing Session

```bash
python3 scripts/categorize-sonar-issues.py /Users/james.bognar/Downloads/SonarQubeIssues.txt
```

## Tools

### `categorize-sonar-issues.py`

Main script for categorizing and interactively fixing SonarQube issues.

**Usage:**
```bash
python3 scripts/categorize-sonar-issues.py <sonarqube-issues-file> [--save-json]
```

**Features:**
- Parses SonarQube TSV export file
- Categorizes issues into 30 categories
- Interactive session for fixing issues category by category
- Saves categorized JSON for later reference

**Options:**
- `--save-json`: Save categorized issues to JSON file

**Interactive Commands:**
- `fix` - Start fixing issues in current category
- `skip` - Move to next category  
- `details` - Show detailed issue information
- `list` - List all categories
- `<number>` - Jump to specific category number
- `quit` - Exit

### `view-sonar-category.py`

Helper script to view category details without interactive input.

**Usage:**
```bash
# List all categories
python3 scripts/view-sonar-category.py

# View specific category
python3 scripts/view-sonar-category.py "Category Name"

# View with limit
python3 scripts/view-sonar-category.py "Category Name" --limit 10
```

## Recommended Workflow

### Step 1: Review Summary

Read `SONARQUBE_ISSUES_SUMMARY.md` to understand:
- Total issues and categories
- Priority levels
- Recommended fix order

### Step 2: Explore Categories

```bash
# List all categories
python3 scripts/view-sonar-category.py

# Review high-priority categories
python3 scripts/view-sonar-category.py "Security Issues"
python3 scripts/view-sonar-category.py "Null Check Issues"
python3 scripts/view-sonar-category.py "Brain Methods (Complexity)"
```

### Step 3: Start Fixing

Begin with high-priority categories:

```bash
python3 scripts/categorize-sonar-issues.py /Users/james.bognar/Downloads/SonarQubeIssues.txt
```

Then:
1. Navigate to high-priority categories (Security, Null Checks, Brain Methods)
2. For each category:
   - Review sample issues with `details`
   - Decide on fix strategy (auto-fix vs manual review)
   - Use `fix` to start fixing issues
   - Process by file (recommended) or individually

### Step 4: Track Progress

The categorized JSON file (`SonarQubeIssues.categorized.json`) can be used to:
- Track which categories have been fixed
- Generate progress reports
- Identify remaining issues

## Category Priorities

### High Priority (Fix First)
- **Security Issues** (6 issues)
- **Null Check Issues** (15 issues)
- **Brain Methods (Complexity)** (30 issues)
- **Missing Exception Handling** (1 issue)

### Medium Priority
- **Exception Catching Issues** (30 issues)
- **Generic Type Issues** (121 issues)
- **Empty Method Implementations** (99 issues)
- **Test Assertion Issues** (61 issues)
- **Optional Access Issues** (18 issues)
- **Code Duplication** (16 issues)
- **ThreadLocal Cleanup Issues** (7 issues)

### Low Priority
- **Unused Code** (96 issues)
- **Missing Private Constructors** (91 issues)
- All other categories

## Fix Strategies

### Auto-Fix Categories (Mechanical Fixes)
These can often be fixed automatically or with simple find/replace:
- Missing @Override Annotations
- Unnecessary toString() Calls
- Missing Private Constructors
- Line Separator Issues
- Python f-string Issues

### Manual Review Categories (Require Code Review)
These need careful review and testing:
- Security Issues
- Brain Methods (Complexity)
- Null Check Issues
- Exception Catching Issues
- Code Duplication

### Batch Fix Categories (Similar Patterns)
These can be fixed in batches:
- Missing Private Constructors
- Generic Type Issues
- Field Shadowing
- Unused Code

## Files

- `SonarQubeIssues.txt` - Original SonarQube export (TSV format)
- `SonarQubeIssues.categorized.json` - Categorized issues (JSON format)
- `SONARQUBE_ISSUES_SUMMARY.md` - Summary document
- `scripts/categorize-sonar-issues.py` - Main categorization tool
- `scripts/view-sonar-category.py` - Category viewer tool

## Tips

1. **Start Small**: Begin with categories that have few issues to build momentum
2. **Batch Similar Fixes**: Group similar issues together for efficient fixing
3. **Test After Each Category**: Run tests after fixing each category
4. **Use Version Control**: Commit fixes category by category for easier review
5. **Document Decisions**: For "Empty Method Implementations" and similar categories, document why methods are intentionally empty

## Example Session

```bash
# 1. View summary
cat SONARQUBE_ISSUES_SUMMARY.md

# 2. Check Security Issues (high priority, only 6 issues)
python3 scripts/view-sonar-category.py "Security Issues"

# 3. Start interactive session
python3 scripts/categorize-sonar-issues.py /Users/james.bognar/Downloads/SonarQubeIssues.txt

# In interactive session:
# > list                    # See all categories
# > 1                       # Jump to Security Issues
# > details                 # See issue details
# > fix                     # Start fixing
# > [choose file mode]     # Process by file
# > auto                    # Auto-fix each issue
# > skip                    # Move to next category
```

## Support

For questions or issues with these tools, refer to:
- `SONARQUBE_ISSUES_SUMMARY.md` - Detailed category descriptions
- The categorized JSON file for complete issue details
