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


# Robust Docusaurus Server Starter
# Kills any existing process on port 3000 and starts fresh

echo "🔄 Starting Docusaurus server..."

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "📁 Working directory: $SCRIPT_DIR"

# Kill any existing process on port 3000
echo "🔍 Checking for existing processes on port 3000..."
PID=$(lsof -ti:3000)
if [ ! -z "$PID" ]; then
    echo "⚡ Killing existing process on port 3000 (PID: $PID)"
    kill -9 $PID 2>/dev/null || true
    sleep 2
fi

# Verify npm and package.json exist
if [ ! -f "package.json" ]; then
    echo "❌ ERROR: package.json not found in $SCRIPT_DIR"
    exit 1
fi

# Clear any cache issues
echo "🧹 Clearing cache..."
rm -rf .docusaurus build node_modules/.cache 2>/dev/null || true

# Start the server
echo "🚀 Starting Docusaurus server..."
npm start

# Check if npm start failed
if [ $? -ne 0 ]; then
    echo "❌ ERROR: Failed to start Docusaurus server"
    exit 1
fi
