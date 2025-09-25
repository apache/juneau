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

# Broken Link Checker for Juneau Documentation
# This script provides options for checking broken links in the documentation

echo "🔍 Juneau Documentation Link Checker"
echo "===================================="
echo ""

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if Node.js is available
if ! command -v node &> /dev/null; then
    echo "❌ ERROR: Node.js is required but not installed."
    echo "Please install Node.js and try again."
    exit 1
fi

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  static     Check static links only (no server required)"
    echo "  full       Check all links including HTTP links (requires server)"
    echo "  help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 static    # Quick check of static files"
    echo "  $0 full      # Full check with server"
    echo ""
}

# Parse command line arguments
case "${1:-static}" in
    "static")
        echo "🚀 Running static link check..."
        echo "This will check all local file references without starting a server."
        echo ""
        node check-static-links.js
        ;;
    "full")
        echo "🚀 Running full link check..."
        echo "This will start the Docusaurus server and check all links including HTTP links."
        echo ""
        node check-broken-links.js
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
