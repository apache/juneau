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
Add Versioned Javadocs Script

Copies generated Javadocs to the static documentation site with version control.

Usage:
    python3 add-versioned-javadocs.py <version> <source_path> [--make-latest]

Examples:
    python3 add-versioned-javadocs.py 9.2.0 ../target/site/apidocs
    python3 add-versioned-javadocs.py 9.1.0 /path/to/old/javadocs --make-latest
"""

import argparse
import os
import shutil
import sys
from pathlib import Path


class Colors:
    """ANSI color codes for terminal output."""
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    RED = '\033[0;31m'
    NC = '\033[0m'  # No Color


def print_info(message):
    """Print info message in green."""
    print(f"{Colors.GREEN}[INFO]{Colors.NC} {message}")


def print_warn(message):
    """Print warning message in yellow."""
    print(f"{Colors.YELLOW}[WARN]{Colors.NC} {message}")


def print_error(message):
    """Print error message in red."""
    print(f"{Colors.RED}[ERROR]{Colors.NC} {message}")


def validate_source(source_path):
    """Validate that the source path contains valid javadocs."""
    source = Path(source_path)
    
    if not source.exists():
        print_error(f"Source path does not exist: {source_path}")
        return False
    
    if not source.is_dir():
        print_error(f"Source path is not a directory: {source_path}")
        return False
    
    index_file = source / "index.html"
    if not index_file.exists():
        print_error(f"Source path does not appear to contain javadocs (missing index.html)")
        return False
    
    return True


def prompt_overwrite():
    """Prompt user to confirm overwrite of existing version."""
    try:
        response = input("Do you want to overwrite it? (y/N) ").strip().lower()
        return response in ['y', 'yes']
    except (EOFError, KeyboardInterrupt):
        print()
        return False


def copy_javadocs(source_path, target_dir):
    """Copy javadocs from source to target directory."""
    source = Path(source_path)
    target = Path(target_dir)
    
    # Create target directory
    target.mkdir(parents=True, exist_ok=True)
    
    # Copy all files
    for item in source.iterdir():
        dest = target / item.name
        if item.is_dir():
            if dest.exists():
                shutil.rmtree(dest)
            shutil.copytree(item, dest)
        else:
            shutil.copy2(item, dest)


def update_latest_link(javadocs_dir, version):
    """Update the 'latest' link to point to the specified version."""
    javadocs_path = Path(javadocs_dir)
    latest_path = javadocs_path / "latest"
    version_path = javadocs_path / version
    
    # Remove existing latest (whether symlink or directory)
    if latest_path.exists() or latest_path.is_symlink():
        if latest_path.is_dir() and not latest_path.is_symlink():
            shutil.rmtree(latest_path)
        else:
            latest_path.unlink()
    
    # Try to create symlink
    try:
        latest_path.symlink_to(version)
        print_info(f"Created symlink: latest -> {version}")
    except (OSError, NotImplementedError):
        # Symlinks not supported, copy instead
        print_warn("Symlinks not supported, copying instead...")
        shutil.copytree(version_path, latest_path)
        print_info("Copied to 'latest' directory")


def main():
    parser = argparse.ArgumentParser(
        description="Add versioned javadocs to the static documentation site",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    python3 add-versioned-javadocs.py 9.2.0 ../target/site/apidocs
    python3 add-versioned-javadocs.py 9.1.0 /path/to/old/javadocs --make-latest
        """
    )
    
    parser.add_argument(
        "version",
        help="Version number (e.g., 9.2.0)"
    )
    
    parser.add_argument(
        "source_path",
        help="Path to the javadocs to copy (e.g., ../target/site/apidocs)"
    )
    
    parser.add_argument(
        "--make-latest",
        action="store_true",
        help="Update the 'latest' symlink to this version"
    )
    
    args = parser.parse_args()
    
    # Get directories
    script_dir = Path(__file__).parent
    juneau_root = script_dir.parent
    javadocs_dir = juneau_root / "juneau-docs" / "static" / "javadocs"
    target_dir = javadocs_dir / args.version
    
    # Validate source
    if not validate_source(args.source_path):
        return 1
    
    # Check if version already exists
    if target_dir.exists():
        print_warn(f"Version {args.version} already exists at {target_dir}")
        if not prompt_overwrite():
            print_info("Aborted.")
            return 0
        print_info("Removing existing version...")
        shutil.rmtree(target_dir)
    
    # Copy javadocs
    print_info(f"Creating directory for version {args.version}...")
    print_info(f"Copying javadocs from {args.source_path} to {target_dir}...")
    
    try:
        copy_javadocs(args.source_path, target_dir)
    except Exception as e:
        print_error(f"Copy failed: {e}")
        return 1
    
    # Verify copy
    if not (target_dir / "index.html").exists():
        print_error("Copy failed - index.html not found in target directory")
        return 1
    
    print_info(f"Successfully added javadocs for version {args.version}")
    
    # Update latest if requested
    if args.make_latest:
        print_info(f"Updating 'latest' to point to version {args.version}...")
        update_latest_link(javadocs_dir, args.version)
    
    # Print summary
    print()
    print_info("=" * 48)
    print_info(f"Javadocs for version {args.version} successfully added!")
    print_info("=" * 48)
    print()
    print(f"  Location: {target_dir}")
    print(f"  URL (after deployment): /javadocs/{args.version}/")
    if args.make_latest:
        print("  Latest URL: /javadocs/latest/")
    print()
    print_info("Next steps:")
    print(f"  1. Review the copied files: ls -lh {target_dir}")
    print("  2. Test locally: python3 scripts/start-docusaurus.py")
    print("  3. Visit http://localhost:3000/javadocs/")
    print("  4. Commit the changes if everything looks good")
    print("  5. Consider updating index.html to add this version to the version grid")
    print()
    
    return 0


if __name__ == "__main__":
    sys.exit(main())

