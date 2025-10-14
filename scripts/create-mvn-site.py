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
Create Maven Site Script

Generates the Maven site with javadocs and copies it to the Docusaurus static directory
for local testing and broken link validation.

Usage:
    python3 create-mvn-site.py
"""

import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path


def print_timestamp(message):
    """Print message with timestamp."""
    timestamp = datetime.now().strftime('%H:%M:%S')
    print(f"[{timestamp}] {message}")


def fail_with_message(message):
    """Print error message and exit."""
    print()
    print("-" * 79)
    print_timestamp(f"ERROR: {message}")
    print("-" * 79)
    sys.exit(1)


def run_maven_command(command, description, cwd):
    """Run a Maven command and handle errors."""
    print(f"\n{description}")
    try:
        # Create log file in juneau root
        log_file = cwd / "create-mvn-site.log"
        
        # Run command and tee output to both console and log file
        with open(log_file, 'w') as f:
            process = subprocess.Popen(
                command,
                cwd=cwd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1
            )
            
            # Print and log output in real-time
            for line in process.stdout:
                print(line, end='')
                f.write(line)
            
            process.wait()
            
            if process.returncode != 0:
                return False
        
        return True
    except Exception as e:
        print(f"Command failed: {e}")
        return False


def get_project_version(juneau_root):
    """Get the project version from Maven POM."""
    try:
        result = subprocess.run(
            ["mvn", "help:evaluate", "-Dexpression=project.version", "-q", "-DforceStdout"],
            cwd=juneau_root,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"Warning: Could not detect project version: {e}")
        return "unknown"


def main():
    # Get directories
    script_dir = Path(__file__).parent
    juneau_root = script_dir.parent
    docs_dir = juneau_root / "juneau-docs"
    site_dir = juneau_root / "target" / "site"
    static_site_dir = docs_dir / "static" / "site"
    
    print("Creating Maven site with javadocs for local testing...")
    print(f"Working from: {juneau_root}")
    
    # Get project version
    project_version = get_project_version(juneau_root)
    print(f"Detected project version: {project_version}")
    
    # Generate Maven site
    print("\nGenerating maven site for root project...")
    
    # Clean existing site
    if site_dir.exists():
        print(f"Removing existing site directory: {site_dir}")
        shutil.rmtree(site_dir)
    
    # Run mvn clean compile site
    if not run_maven_command(
        ["mvn", "clean", "compile", "site"],
        "Running Maven site generation...",
        juneau_root
    ):
        fail_with_message("Maven site generation failed")
    
    # Check if site was generated
    if not site_dir.exists():
        fail_with_message("Maven site generation failed - target/site directory not found.")
    
    print(f"\nFound Maven site in: {site_dir}")
    
    # Setup Docusaurus static directory
    print("\nSetting up local testing directory for Docusaurus...")
    
    if static_site_dir.exists():
        print(f"Removing existing static site directory: {static_site_dir}")
        shutil.rmtree(static_site_dir)
    
    static_site_dir.mkdir(parents=True, exist_ok=True)
    
    # Copy site to Docusaurus static directory
    print("\nCopying entire Maven site to Docusaurus static directory...")
    
    try:
        # Copy all files and directories from site_dir to static_site_dir
        for item in site_dir.iterdir():
            dest = static_site_dir / item.name
            if item.is_dir():
                print(f"Copying directory: {item.name}/")
                shutil.copytree(item, dest)
            else:
                print(f"Copying file: {item.name}")
                shutil.copy2(item, dest)
    except Exception as e:
        fail_with_message(f"Failed to copy site: {e}")
    
    # Success message
    print()
    print("*" * 79)
    print("***** SUCCESS " + "*" * 64)
    print("*" * 79)
    print("Maven site has been generated and copied successfully!")
    print(f"Complete Maven site is now available in: {static_site_dir}")
    print("This includes javadocs, project reports, and all other site content.")
    print("You can now access it at: http://localhost:3000/site/")
    print("Ready for broken link testing in your Docusaurus documentation!")
    print()
    
    return 0


if __name__ == "__main__":
    sys.exit(main())

