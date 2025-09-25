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

# Fix Javadoc Link References Script
# This script fixes the mismatch between Javadoc file naming (dots) and link references (slashes)

echo "🔧 Javadoc Link Reference Fixer"
echo "==============================="
echo ""

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Backup directory
BACKUP_DIR="backup-javadoc-$(date +%Y%m%d-%H%M%S)"
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
    echo "  fix          Fix Javadoc link references (Class/Inner.html -> Class.Inner.html)"
    echo "  dry-run      Show what would be changed without making changes"
    echo "  help         Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 dry-run   # See what would be changed"
    echo "  $0 fix       # Apply the fixes"
    echo ""
}

# Function to fix Javadoc references
fix_javadoc_links() {
    local dry_run="$1"
    echo "🔗 Fixing Javadoc link references..."
    
    # Find all markdown files
    local files=$(find docs-staging -name "*.md" -type f)
    local count=0
    
    for file in $files; do
        if [ "$dry_run" = "true" ]; then
            echo "Would fix Javadoc links in: $file"
            grep -n "/site/apidocs.*/[A-Za-z]*/[A-Za-z]*\.html" "$file" | head -3 | sed 's/^/  /'
        else
            backup_file "$file"
            
            # Fix common Javadoc inner class patterns
            # RestContext/Builder.html -> RestContext.Builder.html
            sed -i '' 's|/site/apidocs/org/apache/juneau/rest/RestContext/Builder\.html|/site/apidocs/org/apache/juneau/rest/RestContext.Builder.html|g' "$file"
            sed -i '' 's|/site/apidocs/org/apache/juneau/rest/RestOpContext/Builder\.html|/site/apidocs/org/apache/juneau/rest/RestOpContext.Builder.html|g' "$file"
            sed -i '' 's|/site/apidocs/org/apache/juneau/rest/client/RestClient/Builder\.html|/site/apidocs/org/apache/juneau/rest/client/RestClient.Builder.html|g' "$file"
            sed -i '' 's|/site/apidocs/org/apache/juneau/microservice/Microservice/Builder\.html|/site/apidocs/org/apache/juneau/microservice/Microservice.Builder.html|g' "$file"
            sed -i '' 's|/site/apidocs/org/apache/juneau/microservice/jetty/JettyMicroservice/Builder\.html|/site/apidocs/org/apache/juneau/microservice/jetty/JettyMicroservice.Builder.html|g' "$file"
            
            # Fix other common patterns
            sed -i '' 's|/site/apidocs/org/apache/juneau/rest/RestChildren/Builder\.html|/site/apidocs/org/apache/juneau/rest/RestChildren.Builder.html|g' "$file"
            sed -i '' 's|/site/apidocs/org/apache/juneau/rest/RestChildren/Void\.html|/site/apidocs/org/apache/juneau/rest/RestChildren.Void.html|g' "$file"
            
            echo "Fixed Javadoc links in: $file"
            ((count++))
        fi
    done
    
    if [ "$dry_run" = "false" ]; then
        echo "✅ Fixed Javadoc links in $count files"
    fi
}

# Parse command line arguments
case "${1:-help}" in
    "fix")
        fix_javadoc_links false
        echo ""
        echo "✅ Javadoc link fixes applied!"
        echo "📁 Backup created in: $BACKUP_DIR"
        echo ""
        echo "🔍 Run link checker to verify fixes:"
        echo "   ./check-links.sh static"
        ;;
    "dry-run")
        echo "🔍 DRY RUN - No changes will be made"
        echo ""
        fix_javadoc_links true
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
