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
Applies Eclipse IDE preference files to all Juneau project modules.

This script copies Eclipse JDT preferences from the eclipse-preferences directory
to each module's .settings folder, ensuring consistent IDE configuration across
all developers.

There are two sets of preferences:
- source-prefs: For main source modules
- test-prefs: For test modules (juneau-utest and *-ftest modules)
"""

import os
import sys
import shutil
from pathlib import Path


# Source modules (main code)
SOURCE_PROJECTS = [
    "juneau-bean/juneau-bean-atom",
    "juneau-bean/juneau-bean-common",
    "juneau-bean/juneau-bean-html5",
    "juneau-bean/juneau-bean-jsonschema",
    "juneau-bean/juneau-bean-openapi-v3",
    "juneau-bean/juneau-bean-swagger-v2",
    "juneau-core/juneau-assertions",
    "juneau-core/juneau-bct",
    "juneau-core/juneau-common",
    "juneau-core/juneau-config",
    "juneau-core/juneau-marshall",
    "juneau-core/juneau-marshall-rdf",
    "juneau-examples/juneau-examples-core",
    "juneau-examples/juneau-examples-rest",
    "juneau-examples/juneau-examples-rest-jetty",
    "juneau-examples/juneau-examples-rest-springboot",
    "juneau-microservice/juneau-microservice-core",
    "juneau-microservice/juneau-microservice-jetty",
    "juneau-microservice/juneau-my-jetty-microservice",
    "juneau-microservice/juneau-my-springboot-microservice",
    "juneau-rest/juneau-rest-client",
    "juneau-rest/juneau-rest-common",
    "juneau-rest/juneau-rest-mock",
    "juneau-rest/juneau-rest-server",
    "juneau-rest/juneau-rest-server-rdf",
    "juneau-rest/juneau-rest-server-springboot",
    "juneau-sc/juneau-sc-client",
    "juneau-sc/juneau-sc-server",
]

# Test modules
TEST_PROJECTS = [
    "juneau-utest",
    "juneau-examples/juneau-examples-rest-jetty-ftest",
]


def apply_preferences(root_dir, projects, prefs_type):
    """
    Apply Eclipse preferences to a list of projects.
    
    Args:
        root_dir: Root directory of the Juneau project
        projects: List of project paths relative to root_dir
        prefs_type: Either 'source-prefs' or 'test-prefs'
    
    Returns:
        Tuple of (success_count, failed_count)
    """
    script_dir = Path(__file__).parent
    prefs_dir = script_dir / "eclipse-preferences" / prefs_type
    
    if not prefs_dir.exists():
        print(f"Error: Preferences directory not found: {prefs_dir}", file=sys.stderr)
        return 0, len(projects)
    
    core_prefs = prefs_dir / "org.eclipse.jdt.core.prefs"
    ui_prefs = prefs_dir / "org.eclipse.jdt.ui.prefs"
    
    if not core_prefs.exists() or not ui_prefs.exists():
        print(f"Error: Preference files not found in {prefs_dir}", file=sys.stderr)
        return 0, len(projects)
    
    success_count = 0
    failed_count = 0
    
    for project in projects:
        project_path = Path(root_dir) / project
        settings_dir = project_path / ".settings"
        
        if not project_path.exists():
            print(f"⚠ Warning: Project not found: {project}")
            failed_count += 1
            continue
        
        # Create .settings directory if it doesn't exist
        settings_dir.mkdir(exist_ok=True)
        
        try:
            # Copy preference files
            shutil.copy2(core_prefs, settings_dir / "org.eclipse.jdt.core.prefs")
            shutil.copy2(ui_prefs, settings_dir / "org.eclipse.jdt.ui.prefs")
            print(f"✓ Preferences applied to {project}")
            success_count += 1
        except Exception as e:
            print(f"✗ Failed to apply preferences to {project}: {e}", file=sys.stderr)
            failed_count += 1
    
    return success_count, failed_count


def main():
    """
    Main entry point for the script.
    """
    # Determine root directory (parent of scripts directory)
    script_dir = Path(__file__).parent
    root_dir = script_dir.parent
    
    if len(sys.argv) > 1:
        root_dir = Path(sys.argv[1])
    
    print(f"Applying Eclipse preferences to Juneau modules in: {root_dir}\n")
    
    # Apply source preferences
    print("Applying source preferences...")
    src_success, src_failed = apply_preferences(root_dir, SOURCE_PROJECTS, "source-prefs")
    
    print("\nApplying test preferences...")
    test_success, test_failed = apply_preferences(root_dir, TEST_PROJECTS, "test-prefs")
    
    # Summary
    total_success = src_success + test_success
    total_failed = src_failed + test_failed
    total_projects = len(SOURCE_PROJECTS) + len(TEST_PROJECTS)
    
    print(f"\n{'='*60}")
    print("Summary:")
    print(f"  Total projects: {total_projects}")
    print(f"  Successfully updated: {total_success}")
    print(f"  Failed: {total_failed}")
    print(f"{'='*60}")
    
    if total_failed > 0:
        sys.exit(1)


if __name__ == '__main__':
    main()

