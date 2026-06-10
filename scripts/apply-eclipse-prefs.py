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
Applies Eclipse IDE preference files to every Juneau Maven module.

Walks the reactor under each REACTOR_GROUPS entry plus STANDALONE_PROJECTS,
finds every leaf directory with a pom.xml that is NOT a packaging=pom aggregator,
classifies each as source or test (test = name == 'juneau-integration-tests' or
*-ftest suffix), and copies the matching .prefs files into each module's
.settings/ directory.

Two sets of preferences live under scripts/eclipse-preferences/:
- source-prefs/ — main source modules (warnings on resource leaks, unused params, etc.)
- test-prefs/  — test modules (those warnings relaxed to ignore)

Each set contains:
- org.eclipse.jdt.core.prefs — JDT compiler problem severities + warning-token settings
- org.eclipse.jdt.ui.prefs   — JDT UI cleanup-on-save settings

Run this whenever a new Maven module is added to the reactor, or whenever the
canonical preference baseline is updated. Module discovery is automatic — the
script does not need to be edited when modules are added or removed (only when
a new top-level reactor group is introduced).

The .settings/ directories are gitignored locally; this script's output is a
developer-machine-only convenience and never enters version control. The
canonical preference files under scripts/eclipse-preferences/ ARE tracked.
"""

import os
import sys
import shutil
from pathlib import Path


# Top-level reactor groups under which to scan for modules.
# Each leaf directory containing a pom.xml that is NOT a packaging=pom aggregator
# is treated as a project to apply preferences to.
REACTOR_GROUPS = [
    "juneau-bean",
    "juneau-core",
    "juneau-examples",
    "juneau-microservice",
    "juneau-petstore",
    "juneau-rest",
    "juneau-sc",
    "juneau-shaded",
]

# Standalone (non-grouped) projects at the repo root.
STANDALONE_PROJECTS = [
    "juneau-integration-tests",
]


def is_aggregator_pom(pom_path):
    """Return True if the pom.xml has <packaging>pom</packaging>."""
    try:
        with open(pom_path, "r", encoding="utf-8") as f:
            content = f.read()
        return "<packaging>pom</packaging>" in content
    except OSError:
        return False


def is_test_module(project_path):
    """
    Returns True if the project should get test-prefs applied (relaxed rules
    around resource leaks, unused parameters, etc.) — typically integration-test
    aggregators or *-ftest functional-test overlays.
    """
    name = Path(project_path).name
    return name == "juneau-integration-tests" or name.endswith("-ftest")


def discover_projects(root_dir):
    """
    Walk the reactor groups and standalone roots; return (source_projects, test_projects)
    as lists of paths relative to root_dir.
    """
    source_projects = []
    test_projects = []

    candidates = []
    for group in REACTOR_GROUPS:
        group_dir = Path(root_dir) / group
        if not group_dir.exists():
            continue
        for child in sorted(group_dir.iterdir()):
            if not child.is_dir():
                continue
            if (child / "pom.xml").exists():
                candidates.append(child)
    for standalone in STANDALONE_PROJECTS:
        standalone_dir = Path(root_dir) / standalone
        if standalone_dir.exists() and (standalone_dir / "pom.xml").exists():
            candidates.append(standalone_dir)

    for project_dir in candidates:
        if is_aggregator_pom(project_dir / "pom.xml"):
            continue
        relative = project_dir.relative_to(root_dir).as_posix()
        if is_test_module(project_dir):
            test_projects.append(relative)
        else:
            source_projects.append(relative)

    return source_projects, test_projects


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

    source_projects, test_projects = discover_projects(root_dir)

    print(f"Discovered {len(source_projects)} source modules + {len(test_projects)} test modules.\n")

    # Apply source preferences
    print("Applying source preferences...")
    src_success, src_failed = apply_preferences(root_dir, source_projects, "source-prefs")

    print("\nApplying test preferences...")
    test_success, test_failed = apply_preferences(root_dir, test_projects, "test-prefs")

    # Summary
    total_success = src_success + test_success
    total_failed = src_failed + test_failed
    total_projects = len(source_projects) + len(test_projects)

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

