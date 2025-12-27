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
4. Copy Maven site to juneau-docs/static/site
   (Docusaurus copies static/ to build/, making site/ available during build)
5. Build Docusaurus documentation (after site/javadocs exist so links are valid)
6. Copy .asf.yaml to juneau-docs/build (needed for deployment)
7. Verify that apidocs were generated
8. Check topic links (validates all documentation links)

Usage:
    python3 scripts/build-docs.py [--skip-npm] [--skip-maven] [--skip-copy]
    
Options:
    --skip-npm      Skip npm install and Docusaurus build
    --skip-maven    Skip Maven compilation and site generation
    --skip-copy     Skip copying Maven site to build directory
"""

import argparse
import os
import shutil
import subprocess
import sys
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
    """Copy Maven site to Docusaurus static directory so it's available during build."""
    print("\n=== Copying Maven site to Docusaurus static directory ===")
    
    source_site = Path(project_root) / 'target' / 'site'
    static_site = Path(docs_dir) / 'static' / 'site'
    asf_yaml = Path(project_root) / '.asf.yaml'
    static_dir = Path(docs_dir) / 'static'
    
    if not source_site.exists():
        print(f"ERROR: Maven site not found at {source_site}")
        sys.exit(1)
    
    # Create static directory if it doesn't exist
    static_dir.mkdir(parents=True, exist_ok=True)
    
    # Copy site contents to static/site (Docusaurus will copy this to build/site)
    print(f"Copying {source_site} to {static_site}")
    if static_site.exists():
        shutil.rmtree(static_site)
    shutil.copytree(source_site, static_site)

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
        else:
            print("\n=== Skipping Maven steps ===")
        
        # Copy Maven site to build directory (so Docusaurus can link to it)
        if not args.skip_copy:
            copy_maven_site(project_root, docs_dir)
        else:
            print("\n=== Skipping copy step ===")
        
        # Build Docusaurus documentation (after site/javadocs exist in static/)
        if not args.skip_npm:
            build_docusaurus(docs_dir)
        else:
            print("\n=== Skipping Docusaurus build ===")
        
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

