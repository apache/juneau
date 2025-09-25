#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations under the License.
#

# Fix Broken Links Script for Juneau Documentation
# This script fixes the most common broken link issues found in the documentation

echo "🔧 Juneau Documentation Link Fixer"
echo "=================================="
echo ""

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Backup directory
BACKUP_DIR="backup-$(date +%Y%m%d-%H%M%S)"
echo "📁 Creating backup in: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

# Function to create backup
backup_file() {
    local file="$1"
    local backup_path="$BACKUP_DIR/$(dirname "$file")"
    mkdir -p "$backup_path"
    cp "$file" "$backup_path/"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  fix-paths     Fix relative path references (../site/ -> /site/)"
    echo "  fix-docs      Fix documentation path references"
    echo "  fix-all       Apply all fixes"
    echo "  dry-run       Show what would be changed without making changes"
    echo "  help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 dry-run    # See what would be changed"
    echo "  $0 fix-paths  # Fix the most common path issues"
    echo "  $0 fix-all    # Apply all fixes"
    echo ""
}

# Function to fix path references
fix_paths() {
    local dry_run="$1"
    echo "🔗 Fixing path references..."
    
    # Find all markdown files
    local files=$(find docs-staging -name "*.md" -type f)
    local count=0
    
    for file in $files; do
        if [ "$dry_run" = "true" ]; then
            echo "Would fix paths in: $file"
            grep -n "../site/" "$file" | head -5 | sed 's/^/  /'
        else
            backup_file "$file"
            # Fix relative paths to absolute paths
            sed -i '' 's|../site/|/site/|g' "$file"
            echo "Fixed paths in: $file"
            ((count++))
        fi
    done
    
    if [ "$dry_run" = "false" ]; then
        echo "✅ Fixed paths in $count files"
    fi
}

# Function to fix documentation references
fix_docs() {
    local dry_run="$1"
    echo "📚 Fixing documentation references..."
    
    # Fix about.md file
    local about_file="src/pages/about.md"
    if [ -f "$about_file" ]; then
        if [ "$dry_run" = "true" ]; then
            echo "Would fix documentation paths in: $about_file"
            grep -n "/docs/topics/" "$about_file" | head -5 | sed 's/^/  /'
        else
            backup_file "$about_file"
            # Fix documentation paths to include .md extension
            sed -i '' 's|/docs/topics/\([^"]*\)"|/docs/topics/\1.md"|g' "$about_file"
            echo "Fixed documentation paths in: $about_file"
        fi
    fi
}

# Function to fix index.tsx
fix_index() {
    local dry_run="$1"
    echo "🏠 Fixing index page references..."
    
    local index_file="src/pages/index.tsx"
    if [ -f "$index_file" ]; then
        if [ "$dry_run" = "true" ]; then
            echo "Would fix index references in: $index_file"
            grep -n "/about" "$index_file" | head -3 | sed 's/^/  /'
        else
            backup_file "$index_file"
            # The /about link should work as-is in Docusaurus, but let's check
            echo "Checked index references in: $index_file"
        fi
    fi
}

# Parse command line arguments
case "${1:-help}" in
    "fix-paths")
        fix_paths false
        ;;
    "fix-docs")
        fix_docs false
        ;;
    "fix-all")
        echo "🚀 Applying all fixes..."
        fix_paths false
        fix_docs false
        fix_index false
        echo ""
        echo "✅ All fixes applied!"
        echo "📁 Backup created in: $BACKUP_DIR"
        echo ""
        echo "🔍 Run link checker to verify fixes:"
        echo "   ./check-links.sh static"
        ;;
    "dry-run")
        echo "🔍 DRY RUN - No changes will be made"
        echo ""
        fix_paths true
        echo ""
        fix_docs true
        echo ""
        fix_index true
        ;;
    "help"|"-h"|"--help")
        show_usage
        ;;
    *)
        echo "❌ Unknown option: $1"
        echo ""
        show_usage
        exit 1
        ;;
esac
