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
1. Install npm dependencies in juneau-docs
2. Compile and install all Java modules to local Maven repository
3. Generate Maven site (includes aggregate javadocs via reporting section)
4. Build Docusaurus documentation (creates build directory)
5. Copy Maven site directly to juneau-docs/build/site
6. Copy javadocs to juneau-docs/build/javadocs
7. Copy .asf.yaml to juneau-docs/build (needed for deployment)
8. Verify that apidocs were generated
9. Check topic links (validates all documentation links)

Usage:
    python3 scripts/build-docs.py [--skip-npm] [--skip-maven] [--skip-copy]
    
Options:
    --skip-npm      Skip npm install and Docusaurus build
    --skip-maven    Skip Maven compilation and site generation
    --skip-copy     Skip copying Maven site to build directory
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path

def run_command(cmd, cwd=None, check=True):
    """Run a shell command and return the result."""
    print(f"Running: {' '.join(cmd)}")
    if cwd:
        print(f"  (in directory: {cwd})")
    result = subprocess.run(cmd, cwd=cwd, check=check, capture_output=False)
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
    print("\n=== Installing npm dependencies ===")
    run_command(['npm', 'ci'], cwd=docs_dir)

def build_docusaurus(docs_dir):
    """Build Docusaurus documentation."""
    print("\n=== Building Docusaurus ===")
    run_command(['npm', 'run', 'build'], cwd=docs_dir)

def compile_java_modules(project_root):
    """Compile and install all Java modules to local repository."""
    print("\n=== Compiling and installing Java modules ===")
    run_command(['mvn', 'clean', 'install', '-DskipTests'], cwd=project_root)

def generate_maven_site(project_root):
    """Generate Maven site."""
    print("\n=== Generating Maven site ===")
    run_command(['mvn', 'site', '-DskipTests'], cwd=project_root)

def copy_maven_site(project_root, docs_dir):
    """Copy Maven site directly to build directory."""
    print("\n=== Copying Maven site to build directory ===")
    
    source_site = Path(project_root) / 'target' / 'site'
    build_dir = Path(docs_dir) / 'build'
    build_site = build_dir / 'site'
    
    if not source_site.exists():
        print(f"ERROR: Maven site not found at {source_site}")
        sys.exit(1)
    
    if not build_dir.exists():
        print(f"ERROR: Build directory not found at {build_dir}. Docusaurus must be built first.")
        sys.exit(1)
    
    # Copy site contents directly to build/site
    print(f"Copying {source_site} to {build_site}")
    if build_site.exists():
        shutil.rmtree(build_site)
    shutil.copytree(source_site, build_site)
    print(f"✓ Maven site copied to {build_site}")

def verify_apidocs(project_root):
    """Verify that apidocs were generated."""
    print("\n=== Verifying apidocs generation ===")
    
    apidocs_dir = Path(project_root) / 'target' / 'site' / 'apidocs'
    
    if not apidocs_dir.exists():
        print(f"ERROR: target/site/apidocs directory not found after generation!")
        print(f"Contents of target/site:")
        site_dir = Path(project_root) / 'target' / 'site'
        if site_dir.exists():
            for item in site_dir.iterdir():
                print(f"  {item.name}")
        sys.exit(1)
    
    print(f"✓ Javadocs found in {apidocs_dir}")
    
    # Count files to give some indication of success
    file_count = sum(1 for _ in apidocs_dir.rglob('*.html'))
    print(f"✓ Found {file_count} HTML files in apidocs")

def get_current_release(project_root):
    """Get current release version using current-release.py script."""
    script_dir = Path(__file__).parent
    current_release_script = script_dir / 'current-release.py'
    
    try:
        result = subprocess.run(
            [sys.executable, str(current_release_script)],
            cwd=project_root,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"WARNING: Could not get current release: {e}")
        return None

def copy_current_javadocs(project_root, docs_dir):
    """Copy current javadocs to versioned folder and update JSON index."""
    print("\n=== Copying current javadocs to versioned folder ===")
    
    # Get current release version
    current_version = get_current_release(project_root)
    if not current_version:
        print("WARNING: Could not determine current version, skipping javadocs copy")
        return
    
    source_apidocs = Path(project_root) / 'target' / 'site' / 'apidocs'
    javadocs_dir = Path(docs_dir) / 'javadocs'
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
    print("\n=== Updating javadocs release index ===")
    
    javadocs_dir = Path(docs_dir) / 'javadocs'
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

def check_topic_links(project_root):
    """Run the topic link checker to validate documentation links."""
    print("\n=== Checking topic links ===")
    checker_script = Path(project_root) / 'scripts' / 'check-topic-links.py'
    
    if not checker_script.exists():
        print(f"WARNING: Topic link checker not found at {checker_script}")
        return
    
    run_command(['python3', str(checker_script)], cwd=project_root)

def main():
    parser = argparse.ArgumentParser(description='Build Apache Juneau documentation')
    parser.add_argument('--skip-npm', action='store_true', help='Skip npm install and Docusaurus build')
    parser.add_argument('--skip-maven', action='store_true', help='Skip Maven compilation and site generation')
    parser.add_argument('--skip-copy', action='store_true', help='Skip copying Maven site to build directory')
    
    args = parser.parse_args()
    
    # Determine project root (parent of scripts directory)
    script_dir = Path(__file__).parent.absolute()
    project_root = script_dir.parent
    docs_dir = project_root / 'juneau-docs'
    
    print(f"Project root: {project_root}")
    print(f"Docs directory: {docs_dir}")
    
    # Check prerequisites
    check_prerequisites()
    
    # Change to project root
    os.chdir(project_root)
    
    # Delete build directory to ensure fresh build
    build_dir = Path(docs_dir) / 'build'
    if build_dir.exists():
        print(f"\n=== Cleaning build directory ===")
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
            compile_java_modules(project_root)
            generate_maven_site(project_root)
            verify_apidocs(project_root)
            
            # Copy current javadocs to versioned folder
            copy_current_javadocs(project_root, docs_dir)
        else:
            print("\n=== Skipping Maven steps ===")
        
        # Build Docusaurus documentation first (creates build directory)
        if not args.skip_npm:
            build_docusaurus(docs_dir)
        else:
            print("\n=== Skipping Docusaurus build ===")
        
        # Copy Maven site directly to build directory (after Docusaurus creates it)
        if not args.skip_copy:
            copy_maven_site(project_root, docs_dir)
        else:
            print("\n=== Skipping copy step ===")
        
        # Copy javadocs to build directory (after Docusaurus build)
        if not args.skip_copy:
            source_javadocs = Path(docs_dir) / 'javadocs'
            build_dir = Path(docs_dir) / 'build'
            build_javadocs = build_dir / 'javadocs'
            
            if source_javadocs.exists() and build_dir.exists():
                print(f"\n=== Copying javadocs to build directory ===")
                print(f"Copying {source_javadocs} to {build_javadocs}")
                if build_javadocs.exists():
                    shutil.rmtree(build_javadocs)
                shutil.copytree(source_javadocs, build_javadocs)
                print(f"✓ Javadocs copied successfully")
            elif not source_javadocs.exists():
                print(f"\n=== WARNING: Javadocs directory not found at {source_javadocs} ===")
                print("Skipping javadocs copy step")
        
        # Copy .asf.yaml to build directory (needed for deployment)
        if not args.skip_copy:
            asf_yaml = Path(project_root) / '.asf.yaml'
            build_dir = Path(docs_dir) / 'build'
            if asf_yaml.exists() and build_dir.exists():
                print(f"\n=== Copying .asf.yaml to build directory ===")
                shutil.copy2(asf_yaml, build_dir)
        
        # Check topic links (runs once at the end)
        check_topic_links(project_root)
        
        print("\n=== Documentation build complete ===")
        print(f"Documentation is available in: {docs_dir / 'build'}")
        
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

