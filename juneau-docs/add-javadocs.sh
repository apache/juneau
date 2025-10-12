#!/bin/bash
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
# ***************************************************************************************************************************

# Script to add versioned javadocs to the static documentation site
#
# Usage:
#   ./add-javadocs.sh <version> <source_path> [--make-latest]
#
# Examples:
#   ./add-javadocs.sh 9.2.0 /path/to/apidocs
#   ./add-javadocs.sh 9.1.0 /path/to/apidocs --make-latest

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check arguments
if [ $# -lt 2 ]; then
    print_error "Usage: $0 <version> <source_path> [--make-latest]"
    echo ""
    echo "  <version>      Version number (e.g., 9.2.0)"
    echo "  <source_path>  Path to the javadocs to copy (e.g., /path/to/apidocs or ../target/site/apidocs)"
    echo "  --make-latest  Optional: Update the 'latest' symlink to this version"
    echo ""
    echo "Examples:"
    echo "  $0 9.2.0 ../target/site/apidocs"
    echo "  $0 9.1.0 /path/to/old/javadocs --make-latest"
    exit 1
fi

VERSION="$1"
SOURCE_PATH="$2"
MAKE_LATEST=false

if [ "$3" == "--make-latest" ]; then
    MAKE_LATEST=true
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVADOCS_DIR="$SCRIPT_DIR/static/javadocs"
TARGET_DIR="$JAVADOCS_DIR/$VERSION"

# Validate source path
if [ ! -d "$SOURCE_PATH" ]; then
    print_error "Source path does not exist: $SOURCE_PATH"
    exit 1
fi

# Check if source contains index.html (basic validation)
if [ ! -f "$SOURCE_PATH/index.html" ]; then
    print_error "Source path does not appear to contain javadocs (missing index.html)"
    exit 1
fi

# Check if version already exists
if [ -d "$TARGET_DIR" ]; then
    print_warn "Version $VERSION already exists at $TARGET_DIR"
    read -p "Do you want to overwrite it? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Aborted."
        exit 0
    fi
    print_info "Removing existing version..."
    rm -rf "$TARGET_DIR"
fi

# Create target directory
print_info "Creating directory for version $VERSION..."
mkdir -p "$TARGET_DIR"

# Copy javadocs
print_info "Copying javadocs from $SOURCE_PATH to $TARGET_DIR..."
cp -r "$SOURCE_PATH"/* "$TARGET_DIR/"

# Verify copy
if [ ! -f "$TARGET_DIR/index.html" ]; then
    print_error "Copy failed - index.html not found in target directory"
    exit 1
fi

print_info "Successfully added javadocs for version $VERSION"

# Update latest if requested
if [ "$MAKE_LATEST" = true ]; then
    print_info "Updating 'latest' to point to version $VERSION..."
    
    # Remove existing latest (whether it's a symlink or directory)
    if [ -e "$JAVADOCS_DIR/latest" ] || [ -L "$JAVADOCS_DIR/latest" ]; then
        rm -rf "$JAVADOCS_DIR/latest"
    fi
    
    # Try to create symlink, fall back to copy if symlinks don't work
    if ln -s "$VERSION" "$JAVADOCS_DIR/latest" 2>/dev/null; then
        print_info "Created symlink: latest -> $VERSION"
    else
        print_warn "Symlinks not supported, copying instead..."
        cp -r "$TARGET_DIR" "$JAVADOCS_DIR/latest"
        print_info "Copied to 'latest' directory"
    fi
fi

# Print summary
echo ""
print_info "================================================"
print_info "Javadocs for version $VERSION successfully added!"
print_info "================================================"
echo ""
echo "  Location: $TARGET_DIR"
echo "  URL (after deployment): /javadocs/$VERSION/"
if [ "$MAKE_LATEST" = true ]; then
    echo "  Latest URL: /javadocs/latest/"
fi
echo ""
print_info "Next steps:"
echo "  1. Review the copied files: ls -lh $TARGET_DIR"
echo "  2. Test locally: ./start-server.sh and visit http://localhost:3000/javadocs/"
echo "  3. Commit the changes if everything looks good"
echo "  4. Consider updating index.html to add this version to the version grid"
echo ""

