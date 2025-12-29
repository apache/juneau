#!/usr/bin/env python3
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""
Revert staged changes back to HEAD (last commit).

This script reverts both staged AND unstaged changes back to the last committed version.
⚠️  WARNING: This will discard all changes (staged and unstaged) for the specified file.

Usage:
    ./scripts/revert-staged.py <file_path>

Example:
    ./scripts/revert-staged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java
"""

import sys
import subprocess
from pathlib import Path

def main():
    if len(sys.argv) != 2:
        print("Error: Exactly one file path argument required")
        print()
        print("Usage: ./scripts/revert-staged.py <file_path>")
        print()
        print("Example:")
        print("    ./scripts/revert-staged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java")
        sys.exit(1)
    
    file_path = sys.argv[1]
    
    # Verify file exists or is tracked by git
    try:
        subprocess.run(
            ["git", "ls-files", "--error-unmatch", file_path],
            check=True,
            capture_output=True,
            text=True
        )
    except subprocess.CalledProcessError:
        print(f"Error: File not tracked by git: {file_path}")
        sys.exit(1)
    
    print(f"⚠️  WARNING: This will discard ALL changes (staged and unstaged) for: {file_path}")
    print(f"Command: git restore --source=HEAD {file_path}")
    print()
    
    try:
        result = subprocess.run(
            ["git", "restore", "--source=HEAD", file_path],
            check=True,
            capture_output=True,
            text=True
        )
        print(f"✅ Successfully reverted to HEAD for: {file_path}")
        print("   (All staged and unstaged changes discarded)")
        return 0
    except subprocess.CalledProcessError as e:
        print(f"❌ Error reverting file: {e.stderr}")
        return 1

if __name__ == "__main__":
    sys.exit(main())

