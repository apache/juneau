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
Release Documentation to Staging Branch

This script:
1. Runs build-docs.py to build the documentation
2. Checks out the asf-staging branch to a temporary directory
3. Copies the contents of juneau-docs/build to the temp directory
4. Adds and commits the changes
5. Pushes to the remote asf-staging branch

Usage:
    python3 scripts/release-docs-stage.py [--no-push] [--commit-message MESSAGE]
    
Options:
    --no-push          Build and commit but don't push to remote
    --commit-message   Custom commit message (default: "Deploy documentation staging")
"""

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


def run_command(cmd, cwd=None, check=True, description=None):
    """Run a shell command and handle errors."""
    if description:
        print(f"\n{description}")
    print(f"Running: {' '.join(cmd) if isinstance(cmd, list) else cmd}")
    
    try:
        result = subprocess.run(
            cmd,
            cwd=cwd,
            shell=isinstance(cmd, str),
            check=check,
            capture_output=False,
            text=True
        )
        if description:
            print(f"✅ {description} - SUCCESS")
        return True
    except subprocess.CalledProcessError as e:
        if description:
            print(f"❌ {description} - FAILED (exit code: {e.returncode})")
        return False
    except Exception as e:
        if description:
            print(f"❌ {description} - FAILED: {e}")
        return False


def get_git_remote_url():
    """Get the git remote URL."""
    try:
        result = subprocess.run(
            ["git", "config", "--get", "remote.origin.url"],
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception:
        return None


def main():
    parser = argparse.ArgumentParser(
        description="Release documentation to asf-staging branch",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument(
        '--no-push',
        action='store_true',
        help='Build and commit but don\'t push to remote'
    )
    parser.add_argument(
        '--commit-message',
        type=str,
        default='Deploy documentation staging',
        help='Custom commit message (default: "Deploy documentation staging")'
    )
    
    args = parser.parse_args()
    
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    docs_dir = project_root / 'juneau-docs'
    build_dir = docs_dir / 'build'
    
    print("=" * 79)
    print("Release Documentation to Staging Branch")
    print("=" * 79)
    print()
    
    # Step 1: Run build-docs.py with --staging flag
    print("Step 1: Building documentation for staging...")
    build_script = script_dir / 'build-docs.py'
    if not build_script.exists():
        print(f"❌ ERROR: {build_script} not found")
        sys.exit(1)
    
    if not run_command(
        [sys.executable, str(build_script), '--staging'],
        cwd=project_root,
        description="Building documentation for staging"
    ):
        print("\n❌ Documentation build failed. Aborting.")
        sys.exit(1)
    
    if not build_dir.exists():
        print(f"❌ ERROR: Build directory not found at {build_dir}")
        print("   Documentation build may have failed.")
        sys.exit(1)
    
    # Step 2: Create temp directory and checkout asf-staging
    print("\nStep 2: Setting up temporary directory with asf-staging branch...")
    
    # Get git remote URL
    remote_url = get_git_remote_url()
    if not remote_url:
        print("❌ ERROR: Could not determine git remote URL")
        sys.exit(1)
    
    # Create temp directory
    temp_dir = Path(tempfile.mkdtemp(prefix='juneau-docs-staging-'))
    print(f"Temporary directory: {temp_dir}")
    
    try:
        # Clone repository to temp directory
        if not run_command(
            ["git", "clone", remote_url, str(temp_dir)],
            description="Cloning repository to temp directory"
        ):
            print("\n❌ Failed to clone repository")
            sys.exit(1)
        
        # Fetch asf-staging branch
        if not run_command(
            ["git", "fetch", "origin", "asf-staging"],
            cwd=temp_dir,
            check=False,  # Don't fail if branch doesn't exist yet
            description="Fetching asf-staging branch"
        ):
            print("⚠ Warning: Could not fetch asf-staging (branch may not exist yet)")
        
        # Checkout or create asf-staging branch
        result = subprocess.run(
            ["git", "checkout", "-B", "asf-staging", "origin/asf-staging"],
            cwd=temp_dir,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True
        )
        
        if result.returncode != 0:
            # Branch doesn't exist, create it
            if not run_command(
                ["git", "checkout", "-b", "asf-staging"],
                cwd=temp_dir,
                description="Creating new asf-staging branch"
            ):
                print("\n❌ Failed to create asf-staging branch")
                sys.exit(1)
        else:
            print("✅ Checked out asf-staging branch")
        
        # Step 3: Remove all existing files (except .git)
        print("\nStep 3: Cleaning temp directory...")
        for item in temp_dir.iterdir():
            if item.name != '.git':
                if item.is_dir():
                    shutil.rmtree(item)
                else:
                    item.unlink()
        print("✅ Cleaned temp directory")
        
        # Step 4: Copy build directory contents
        print("\nStep 4: Copying build directory contents...")
        for item in build_dir.iterdir():
            dest = temp_dir / item.name
            if item.is_dir():
                shutil.copytree(item, dest)
            else:
                shutil.copy2(item, dest)
        print(f"✅ Copied contents from {build_dir} to {temp_dir}")
        
        # Step 5: Add and commit changes
        print("\nStep 5: Committing changes...")
        
        # Set git user (use current user's git config or defaults)
        try:
            result = subprocess.run(
                ["git", "config", "--get", "user.name"],
                capture_output=True,
                text=True
            )
            git_user = result.stdout.strip() if result.returncode == 0 else "Documentation Builder"
            
            result = subprocess.run(
                ["git", "config", "--get", "user.email"],
                capture_output=True,
                text=True
            )
            git_email = result.stdout.strip() if result.returncode == 0 else "docs@juneau.apache.org"
        except Exception:
            git_user = "Documentation Builder"
            git_email = "docs@juneau.apache.org"
        
        run_command(
            ["git", "config", "user.name", git_user],
            cwd=temp_dir,
            description="Setting git user name"
        )
        run_command(
            ["git", "config", "user.email", git_email],
            cwd=temp_dir,
            description="Setting git user email"
        )
        
        # Add all files
        if not run_command(
            ["git", "add", "-A"],
            cwd=temp_dir,
            description="Adding all files"
        ):
            print("\n❌ Failed to add files")
            sys.exit(1)
        
        # Check if there are changes to commit
        result = subprocess.run(
            ["git", "diff", "--staged", "--quiet"],
            cwd=temp_dir
        )
        
        if result.returncode != 0:
            # There are changes, commit them
            if not run_command(
                ["git", "commit", "-m", args.commit_message],
                cwd=temp_dir,
                description=f"Committing changes: {args.commit_message}"
            ):
                print("\n❌ Failed to commit changes")
                sys.exit(1)
        else:
            print("ℹ️  No changes to commit")
        
        # Step 6: Push to remote (if not --no-push)
        if not args.no_push:
            print("\nStep 6: Pushing to remote asf-staging branch...")
            if not run_command(
                ["git", "push", "origin", "asf-staging", "--force"],
                cwd=temp_dir,
                description="Pushing to remote"
            ):
                print("\n❌ Failed to push to remote")
                sys.exit(1)
        else:
            print("\n⏭️  Skipping push (--no-push flag set)")
            print(f"   Changes are in: {temp_dir}")
            print("   You can manually push with:")
            print(f"   cd {temp_dir}")
            print("   git push origin asf-staging --force")
        
        print("\n" + "=" * 79)
        print("✅ Documentation staging deployment complete!")
        print("=" * 79)
        if args.no_push:
            print(f"\nTemporary directory: {temp_dir}")
            print("(This directory will not be automatically cleaned up)")
        
    except KeyboardInterrupt:
        print("\n\n⚠️  Process interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ ERROR: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        # Clean up temp directory if push was successful
        if not args.no_push and temp_dir.exists():
            print(f"\nCleaning up temporary directory: {temp_dir}")
            shutil.rmtree(temp_dir)
            print("✅ Cleanup complete")


if __name__ == '__main__':
    main()

