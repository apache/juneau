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
Restores one git-tracked file's working-tree edits to the staged (index) version, keeping what is already staged.

Full docs: revert-unstaged.md
"""

import argparse
import subprocess
import sys
from pathlib import Path


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True)


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Restore one tracked file's working-tree edits to the staged (index) version; staged changes are kept.",
        epilog="Example: ./scripts/revert-unstaged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java"
    )
    parser.add_argument("file_path", help="the file to restore (use -- before a path that starts with -)")
    file_path = parser.parse_args(argv).file_path

    if not Path(file_path).exists():
        print(f"Error: File not found: {file_path}")
        return 1

    if git("ls-files", "--error-unmatch", "--", file_path).returncode != 0:
        print(f"Error: File not tracked by git, so it has no staged version: {file_path}")
        return 1

    if git("diff", "--quiet", "--", file_path).returncode == 0:
        print(f"Nothing to revert: {file_path} already matches the staged version")
        return 0

    print(f"Reverting unstaged changes for: {file_path}")
    print(f"Command: git restore --worktree -- {file_path}")
    print()

    result = git("restore", "--worktree", "--", file_path)
    if result.returncode != 0:
        print(f"Error reverting file: {result.stderr}")
        return 1
    print(f"Successfully reverted unstaged changes for: {file_path}")
    print("   (Staged changes preserved)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
