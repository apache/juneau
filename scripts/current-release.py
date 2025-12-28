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
Get the current release version from pom.xml.

This script parses the root pom.xml file and returns the version number
with the -SNAPSHOT suffix removed (if present).

Usage:
    python3 scripts/current-release.py

Output:
    Prints the version number (e.g., "9.2.0") to stdout.
"""

import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def get_current_release():
    """Get the current release version from pom.xml, removing -SNAPSHOT if present."""
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    pom_path = project_root / 'pom.xml'
    
    if not pom_path.exists():
        print(f"ERROR: pom.xml not found at {pom_path}", file=sys.stderr)
        sys.exit(1)
    
    # Try using Maven first (most reliable)
    try:
        result = subprocess.run(
            ["mvn", "help:evaluate", "-Dexpression=project.version", "-q", "-DforceStdout"],
            cwd=project_root,
            capture_output=True,
            text=True,
            check=True
        )
        version = result.stdout.strip()
        if version.endswith('-SNAPSHOT'):
            version = version[:-9]
        return version
    except Exception as e:
        print(f"Warning: Could not get version using Maven: {e}", file=sys.stderr)
    
    # Fallback: parse XML directly
    try:
        tree = ET.parse(pom_path)
        root = tree.getroot()
        # Handle namespace - get namespace from root element
        ns_uri = root.tag.split('}')[0].strip('{') if '}' in root.tag else ''
        if ns_uri:
            ns = {'maven': ns_uri}
            version_elem = root.find('maven:version', ns)
        else:
            version_elem = root.find('version')
        
        if version_elem is not None and version_elem.text:
            version = version_elem.text.strip()
            # Remove -SNAPSHOT if present
            if version.endswith('-SNAPSHOT'):
                version = version[:-9]
            return version
    except Exception as e:
        print(f"Warning: Could not parse version from pom.xml: {e}", file=sys.stderr)
    
    print("ERROR: Could not determine version from pom.xml", file=sys.stderr)
    sys.exit(1)


if __name__ == '__main__':
    version = get_current_release()
    print(version)

