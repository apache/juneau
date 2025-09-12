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

# Build and Push Script for Juneau
# Usage: ./build-and-push.sh "commit message"

set -e  # Exit immediately if a command exits with a non-zero status

# Check if commit message is provided
if [ -z "$1" ]; then
    echo "❌ Error: Commit message is required"
    echo "Usage: $0 \"commit message\""
    exit 1
fi

COMMIT_MESSAGE="$1"

echo "🚀 Starting build and push process..."

echo "🧪 Step 1: Running tests..."
if ! mvn test; then
    echo "❌ Step 1 FAILED: Tests failed! Aborting build process."
    exit 1
fi
echo "✅ Step 1: All tests passed."

echo "🏗️  Step 2: Building and installing project..."
if ! mvn clean package install; then
    echo "❌ Step 2 FAILED: Build failed! Aborting."
    exit 1
fi
echo "✅ Step 2: Build and install completed."

echo "📚 Step 3: Generating Javadocs..."
if ! mvn javadoc:javadoc; then
    echo "❌ Step 3 FAILED: Javadoc generation failed! Aborting."
    exit 1
fi
echo "✅ Step 3: Javadoc generation completed."

echo "📝 Step 4: Committing changes to Git..."
git add .
if ! git commit -m "$COMMIT_MESSAGE"; then
    echo "❌ Step 4 FAILED: Git commit failed! Aborting."
    exit 1
fi
echo "✅ Step 4: Git commit completed."

echo "🚀 Step 5: Pushing changes to remote repository..."
if ! git push; then
    echo "❌ Step 5 FAILED: Git push failed!"
    exit 1
fi
echo "✅ Step 5: Git push completed."

echo "🎉 All operations completed successfully!"
echo "📦 Commit message: $COMMIT_MESSAGE"
