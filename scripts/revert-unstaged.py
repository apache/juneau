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
Revert unstaged changes back to the staged (INDEX) version.

This script safely reverts working directory changes back to what's in the staging area,
preserving any staged changes that have been tested.

Usage:
    ./scripts/revert-unstaged.py <file_path>

Example:
    ./scripts/revert-unstaged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java
"""

import sys
import subprocess
from pathlib import Path

def main():
    if len(sys.argv) != 2:
        print("Error: Exactly one file path argument required")
        print()
        print("Usage: ./scripts/revert-unstaged.py <file_path>")
        print()
        print("Example:")
        print("    ./scripts/revert-unstaged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java")
        sys.exit(1)
    
    file_path = sys.argv[1]
    
    # Verify file exists
    if not Path(file_path).exists():
        print(f"Error: File not found: {file_path}")
        sys.exit(1)
    
    print(f"Reverting unstaged changes for: {file_path}")
    print(f"Command: git restore --source=INDEX {file_path}")
    print()
    
    try:
        result = subprocess.run(
            ["git", "restore", "--source=INDEX", file_path],
            check=True,
            capture_output=True,
            text=True
        )
        print(f"✅ Successfully reverted unstaged changes for: {file_path}")
        print("   (Staged changes preserved)")
        return 0
    except subprocess.CalledProcessError as e:
        print(f"❌ Error reverting file: {e.stderr}")
        return 1

if __name__ == "__main__":
    sys.exit(main())

