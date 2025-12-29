#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Get the Maven version.

This script runs 'mvn -version' and extracts the major version number.

Usage:
    python3 scripts/maven-version.py

Output:
    Prints the major version number (e.g., "3") to stdout.
"""

import re
import subprocess
import sys


def get_maven_version():
    """Get the Maven major version from mvn -version output."""
    try:
        result = subprocess.run(
            ['mvn', '-version'],
            capture_output=True,
            text=True,
            check=False
        )
        
        # Parse version from output
        version_text = result.stdout or result.stderr
        
        # Pattern: "Apache Maven 3.9.5" or "Maven 3.9.5"
        match = re.search(r'Maven\s+(\d+)\.(\d+)', version_text)
        if match:
            major = int(match.group(1))
            return major
        
        # Fallback: try to find just a version number at the start
        match = re.search(r'^(\d+)\.(\d+)', version_text)
        if match:
            major = int(match.group(1))
            return major
        
        print("ERROR: Could not determine Maven version from output", file=sys.stderr)
        if version_text:
            print(f"Output was: {version_text[:200]}", file=sys.stderr)
        sys.exit(1)
        
    except FileNotFoundError:
        print("ERROR: mvn command not found. Please install Maven.", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: Could not get Maven version: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    version = get_maven_version()
    print(version)

