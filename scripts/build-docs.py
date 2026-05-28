#!/usr/bin/env python3
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
"""
Script to build Apache Juneau documentation.

This script replicates the documentation generation steps from the GitHub Actions workflow,
allowing the documentation to be built locally for testing and verification.

Steps performed:
1. Install npm dependencies in docs
2. Compile and install all Java modules to local Maven repository
3. Generate Maven site (includes aggregate javadocs via reporting section)
4. Copy current javadocs to docs/static/javadocs/<version> (updates versioned javadocs)
5. Copy Maven site to docs/static/site (Docusaurus will copy to build)
6. Build Docusaurus documentation (copies static/ contents to build/)
7. Copy .asf.yaml to docs/build (needed for deployment)
8. Verify that apidocs were generated
9. Check topic links (validates all documentation links)

Usage:
    python3 scripts/build-docs.py [--skip-npm] [--skip-maven] [--skip-copy]
                                  [--staging] [--dry-run] [--verbose]
    
Options:
    --skip-npm      Skip npm install and Docusaurus build
    --skip-maven    Skip Maven compilation and site generation
    --skip-copy     Skip copying Maven site to static directory
    --staging       Build for staging (sets SITE_URL to juneau.staged.apache.org)
    --dry-run       Print the commands that would run; perform no work
    --verbose       Print full per-stage banner output (default: one line per stage)
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path


def find_master_branch_sibling(script_dir, allow_missing=False):
    """
    Find the master branch sibling folder.

    The docs branch and master branch should be sibling folders:
    - /git/apache/juneau/docs/
    - /git/apache/juneau/master/

    Args:
        script_dir: Path to the scripts directory (should be in docs/scripts)
        allow_missing: If True (e.g. during --dry-run), return the expected path
            even when it doesn't exist instead of aborting.

    Returns:
        Path to the master branch folder

    Raises:
        SystemExit: If the master branch sibling folder doesn't exist and
            allow_missing is False.
    """
    # Get the parent of the docs folder (should be /git/apache/juneau/)
    docs_dir = script_dir.parent  # docs/
    parent_dir = docs_dir.parent  # /git/apache/juneau/
    master_dir = parent_dir / 'master'

    if allow_missing:
        return master_dir

    if not master_dir.exists():
        print(f"ERROR: Master branch sibling folder not found at {master_dir}")
        print(f"Expected structure:")
        print(f"  {parent_dir}/docs/  (current)")
        print(f"  {parent_dir}/master/  (missing)")
        print("\nPlease ensure the master branch is cloned as a sibling folder.")
        sys.exit(1)
    
    if not (master_dir / '.git').exists():
        print(f"ERROR: {master_dir} exists but is not a git repository")
        sys.exit(1)
    
    return master_dir

# Module-level toggle flipped from main() so every run_command/stage_banner sees the choice.
DRY_RUN = False
VERBOSE = False


def stage_banner(title):
    """Emit a stage header. Verbose mode keeps the full banner; default mode prints one line."""
    if VERBOSE:
        print(f"\n=== {title} ===")
    else:
        print(f"-> {title}")


def run_command(cmd, cwd=None, check=True, env=None):
    """Run a shell command and return the result. Honors DRY_RUN."""
    cmd_str = ' '.join(cmd)
    if DRY_RUN:
        loc = f" (in {cwd})" if cwd else ""
        print(f"[dry-run] would run: {cmd_str}{loc}")
        return None
    print(f"Running: {cmd_str}")
    if cwd:
        print(f"  (in directory: {cwd})")
    result = subprocess.run(cmd, cwd=cwd, check=check, capture_output=False, env=env)
    return result

def check_prerequisites():
    """Check that required tools are available."""
    required_tools = {
        'node': 'Node.js',
        'npm': 'npm',
        'mvn': 'Maven',
        'java': 'Java'
    }
    
    missing = []
    for tool, name in required_tools.items():
        result = subprocess.run(['which', tool], capture_output=True)
        if result.returncode != 0:
            missing.append(name)
    
    if missing:
        print(f"ERROR: Missing required tools: {', '.join(missing)}")
        print("Please install the missing tools and try again.")
        sys.exit(1)
    
    print("✓ All required tools are available")

def install_npm_dependencies(docs_dir):
    """Install npm dependencies for Docusaurus."""
    stage_banner("Installing npm dependencies")
    run_command(['npm', 'ci'], cwd=docs_dir)

def build_docusaurus(docs_dir, staging=False):
    """Build Docusaurus documentation."""
    stage_banner("Building Docusaurus")
    env = os.environ.copy()
    if staging:
        env['SITE_URL'] = 'https://juneau.staged.apache.org'
        if VERBOSE:
            print("  Setting SITE_URL for staging build")
    run_command(['npm', 'run', 'build'], cwd=docs_dir, env=env)

def compile_java_modules(master_root):
    """Compile and install all Java modules to local repository."""
    stage_banner("Compiling and installing Java modules")
    run_command(['mvn', 'clean', 'install', '-DskipTests'], cwd=master_root)

def generate_maven_site(master_root):
    """Generate Maven site."""
    stage_banner("Generating Maven site")
    run_command(['mvn', 'site', '-DskipTests'], cwd=master_root)

def copy_maven_site(master_root, docs_dir):
    """Copy Maven site to static directory (Docusaurus will copy it to build)."""
    stage_banner("Copying Maven site to static directory")

    source_site = Path(master_root) / 'target' / 'site'
    static_dir = Path(docs_dir) / 'static'
    static_site = static_dir / 'site'

    if DRY_RUN:
        print(f"[dry-run] would copy {source_site} -> {static_site}")
        return

    if not source_site.exists():
        print(f"ERROR: Maven site not found at {source_site}")
        sys.exit(1)
    
    # Create static directory if it doesn't exist
    static_dir.mkdir(parents=True, exist_ok=True)
    
    # Copy site contents to static/site (Docusaurus will copy to build during build)
    print(f"Copying {source_site} to {static_site}")
    if static_site.exists():
        shutil.rmtree(static_site)
    shutil.copytree(source_site, static_site)
    print(f"✓ Maven site copied to {static_site}")

def verify_apidocs(master_root):
    """Verify that apidocs were generated."""
    stage_banner("Verifying apidocs generation")
    if DRY_RUN:
        print("[dry-run] would verify apidocs at target/site/apidocs")
        return

    apidocs_dir = Path(master_root) / 'target' / 'site' / 'apidocs'
    
    if not apidocs_dir.exists():
        print(f"ERROR: target/site/apidocs directory not found after generation!")
        print(f"Contents of target/site:")
        site_dir = Path(master_root) / 'target' / 'site'
        if site_dir.exists():
            for item in site_dir.iterdir():
                print(f"  {item.name}")
        sys.exit(1)
    
    print(f"✓ Javadocs found in {apidocs_dir}")
    
    # Count files to give some indication of success
    file_count = sum(1 for _ in apidocs_dir.rglob('*.html'))
    print(f"✓ Found {file_count} HTML files in apidocs")

def get_current_release(master_root):
    """Get current release version using current-release.py script from master branch."""
    script_dir = Path(__file__).parent
    master_scripts_dir = master_root / 'scripts'
    current_release_script = master_scripts_dir / 'current-release.py'
    
    if not current_release_script.exists():
        print(f"WARNING: current-release.py not found at {current_release_script}")
        return None
    
    try:
        result = subprocess.run(
            [sys.executable, str(current_release_script)],
            cwd=master_root,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"WARNING: Could not get current release: {e}")
        return None

def copy_current_javadocs(master_root, docs_dir):
    """Copy current javadocs to versioned folder and update JSON index."""
    stage_banner("Copying current javadocs to versioned folder")
    if DRY_RUN:
        print("[dry-run] would copy target/site/apidocs to static/javadocs/<version>")
        return

    # Get current release version
    current_version = get_current_release(master_root)
    if not current_version:
        print("WARNING: Could not determine current version, skipping javadocs copy")
        return
    
    source_apidocs = Path(master_root) / 'target' / 'site' / 'apidocs'
    javadocs_dir = Path(docs_dir) / 'static' / 'javadocs'
    versioned_javadocs = javadocs_dir / current_version
    
    if not source_apidocs.exists():
        print(f"WARNING: Source apidocs not found at {source_apidocs}, skipping copy")
        return
    
    # Create javadocs directory if it doesn't exist
    javadocs_dir.mkdir(parents=True, exist_ok=True)
    
    # Copy apidocs to versioned folder
    print(f"Copying {source_apidocs} to {versioned_javadocs}")
    if versioned_javadocs.exists():
        shutil.rmtree(versioned_javadocs)
    shutil.copytree(source_apidocs, versioned_javadocs)
    print(f"✓ Javadocs copied to {versioned_javadocs}")
    
    # Update JSON file with release information
    update_javadocs_json(docs_dir, current_version)

def update_javadocs_json(docs_dir, current_version):
    """Update or create JSON file with javadoc release information."""
    stage_banner("Updating javadocs release index")
    
    javadocs_dir = Path(docs_dir) / 'static' / 'javadocs'
    json_file = javadocs_dir / 'releases.json'
    
    # Load existing releases if file exists
    releases = []
    if json_file.exists():
        try:
            with open(json_file, 'r') as f:
                releases = json.load(f)
        except Exception as e:
            print(f"Warning: Could not load existing releases.json: {e}")
            releases = []
    
    # Check if current version already exists
    existing_index = None
    for i, release in enumerate(releases):
        if release.get('version') == current_version:
            existing_index = i
            break
    
    # Add or update current version
    release_info = {
        'version': current_version,
        'path': f'{current_version}/',
        'released': datetime.now().strftime('%Y-%m-%d')
    }
    
    if existing_index is not None:
        releases[existing_index] = release_info
        print(f"✓ Updated release {current_version} in index")
    else:
        releases.append(release_info)
        print(f"✓ Added release {current_version} to index")
    
    # Sort releases by version (newest first)
    def version_key(v):
        parts = v['version'].split('.')
        # Convert to integers, handling any non-numeric parts
        try:
            return tuple(int(p) if p.isdigit() else p for p in parts)
        except (ValueError, AttributeError):
            # Fallback: use string comparison
            return v['version']
    
    releases.sort(key=version_key, reverse=True)
    
    # Save JSON file
    try:
        with open(json_file, 'w') as f:
            json.dump(releases, f, indent=2)
        print(f"✓ Updated {json_file}")
    except Exception as e:
        print(f"Warning: Could not save releases.json: {e}")

def check_topic_links(master_root, docs_dir):
    """Run the topic link checker to validate documentation links."""
    stage_banner("Checking topic links")
    # Use the checker script from docs/scripts (it's already there)
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-topic-links.py'
    
    if not checker_script.exists():
        print(f"WARNING: Topic link checker not found at {checker_script}")
        return
    
    # The checker script needs both master_root and docs_dir
    # It will scan master_root for links and docs_dir for topics
    run_command(['python3', str(checker_script)], cwd=docs_dir)

def check_ai_artifacts(docs_dir):
    """Run AI artifact drift checks."""
    stage_banner("Checking AI artifacts")
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-ai-artifacts.py'
    if not checker_script.exists():
        print(f"WARNING: AI artifact checker not found at {checker_script}")
        return
    run_command(['python3', str(checker_script)], cwd=docs_dir)

def main():
    global DRY_RUN, VERBOSE

    parser = argparse.ArgumentParser(description='Build Apache Juneau documentation')
    parser.add_argument('--skip-npm', action='store_true', help='Skip npm install and Docusaurus build')
    parser.add_argument('--skip-maven', action='store_true', help='Skip Maven compilation and site generation')
    parser.add_argument('--skip-copy', action='store_true', help='Skip copying Maven site to static directory')
    parser.add_argument('--staging', action='store_true', help='Build for staging (sets SITE_URL to juneau.staged.apache.org)')
    parser.add_argument('--dry-run', action='store_true', help='Print every command/copy that would run; perform no work')
    parser.add_argument('--verbose', action='store_true', help='Print full per-stage banner output (default: one line per stage)')

    args = parser.parse_args()
    DRY_RUN = args.dry_run
    VERBOSE = args.verbose

    # Determine script directory and find master branch sibling
    script_dir = Path(__file__).parent.absolute()
    docs_dir = script_dir.parent  # docs/
    # When --skip-maven is set, the master branch is not needed; treat it as allowed-missing
    # so CI runners (which have no sibling master checkout) can still run the Docusaurus smoke.
    master_root = find_master_branch_sibling(script_dir, allow_missing=DRY_RUN or args.skip_maven)
    
    print(f"Master branch root: {master_root}")
    print(f"Docs directory: {docs_dir}")
    if DRY_RUN:
        print("DRY-RUN: no commands will be executed")

    # Check prerequisites
    if DRY_RUN:
        print("[dry-run] would check prerequisites (node, npm, mvn, java)")
    else:
        check_prerequisites()

    # Change to docs directory for npm operations
    os.chdir(docs_dir)
    
    # Delete build directory to ensure fresh build
    build_dir = Path(docs_dir) / 'build'
    if build_dir.exists():
        stage_banner("Cleaning build directory")
        if DRY_RUN:
            print(f"[dry-run] would remove {build_dir}")
        else:
            print(f"Removing {build_dir}")
            shutil.rmtree(build_dir)
            print("✓ Build directory cleaned")
    
    try:
        # Install npm dependencies (can be done early)
        if not args.skip_npm:
            install_npm_dependencies(docs_dir)
        else:
            print("\n=== Skipping npm install ===")
        
        # Generate Maven site and javadocs (must be done before building Docusaurus)
        # Note: Aggregate javadocs are now generated automatically as part of mvn site via the reporting section
        if not args.skip_maven:
            compile_java_modules(master_root)
            generate_maven_site(master_root)
            verify_apidocs(master_root)
            
            # Copy current javadocs to versioned folder
            copy_current_javadocs(master_root, docs_dir)
        else:
            print("\n=== Skipping Maven steps ===")
        
        # Copy Maven site to static directory BEFORE building Docusaurus
        # (Docusaurus will automatically copy static/ contents to build/ during build)
        # Note: javadocs are already in static/javadocs, so no copy needed
        # Also skip if --skip-maven: there is no Maven output to copy in that case.
        if not args.skip_copy and not args.skip_maven:
            # Copy Maven site to static directory
            copy_maven_site(master_root, docs_dir)
        else:
            print("\n=== Skipping copy step ===")
        
        # Build Docusaurus documentation (will copy static/ contents to build/)
        if not args.skip_npm:
            build_docusaurus(docs_dir, staging=args.staging)
        else:
            print("\n=== Skipping Docusaurus build ===")
        
        # Copy .asf.yaml to build directory (needed for deployment)
        # Skip when --skip-maven: master_root may not exist in that scenario (CI smoke check).
        if not args.skip_copy and not args.skip_maven:
            build_dir = Path(docs_dir) / 'build'
            asf_yaml = Path(master_root) / '.asf.yaml'
            if DRY_RUN:
                stage_banner("Copying .asf.yaml to build directory")
                print(f"[dry-run] would copy {asf_yaml} -> {build_dir}/.asf.yaml")
            elif asf_yaml.exists() and build_dir.exists():
                stage_banner("Copying .asf.yaml to build directory")
                shutil.copy2(asf_yaml, build_dir)

        # Checks that require sibling repos — skip in --skip-maven / CI smoke runs
        # where only the docs repo is checked out.
        if not args.skip_maven:
            check_topic_links(master_root, docs_dir)
            check_ai_artifacts(docs_dir)

        print("\n=== Documentation build complete ===")
        print(f"Documentation is available in: {docs_dir / 'build'}")

        # Publish reminder — only when this was a real, complete build
        # (no skips, not a dry-run). Skipped runs are typically partial/iterative
        # and the reminder would be misleading.
        any_skip = args.skip_npm or args.skip_maven or args.skip_copy
        if not DRY_RUN and not any_skip:
            print()
            print("Next step — publish:")
            print("  python3 scripts/release-docs-stage.py             # deploy to asf-staging")
            print("  python3 scripts/release-docs-stage.py --no-push   # rehearse without pushing")
            print("Then, after eyeballing https://juneau.staged.apache.org:")
            print("  python3 scripts/release-docs.py                   # promote asf-staging -> asf-site (LIVE)")

    except subprocess.CalledProcessError as e:
        print(f"\nERROR: Command failed with exit code {e.returncode}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n\nBuild interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nERROR: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()

