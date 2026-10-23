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
Discards the changes to one git-tracked file by restoring it from `HEAD`, the last commit.

Full docs: revert-staged.md
"""

import argparse
import subprocess
import sys


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True)


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Discard the changes to one tracked file by restoring it from HEAD.",
        epilog="Example: ./scripts/revert-staged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java"
    )
    parser.add_argument("file_path", help="the file to restore (use -- before a path that starts with -)")
    file_path = parser.parse_args(argv).file_path

    if git("ls-files", "--error-unmatch", "--", file_path).returncode != 0:
        print(f"Error: File not tracked by git: {file_path}")
        return 1

    print(f"WARNING: This will discard ALL changes (staged and unstaged) for: {file_path}")
    print(f"Command: git restore --source=HEAD -- {file_path}")
    print()

    result = git("restore", "--source=HEAD", "--", file_path)
    if result.returncode != 0:
        print(f"Error reverting file: {result.stderr}")
        return 1
    print(f"Successfully reverted to HEAD for: {file_path}")
    print("   (All staged and unstaged changes discarded)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
