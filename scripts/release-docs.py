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
Promote Documentation from Staging to Production

This script promotes the documentation from the asf-staging branch to the asf-site branch.
This makes the documentation live on the production website.

The script:
1. Fetches the asf-staging branch
2. Switches to a detached HEAD at origin/asf-staging
3. Force pushes to the asf-site branch

Usage:
    python3 scripts/release-docs.py [--no-push] [--commit-message MESSAGE]
    
Options:
    --no-push          Perform all steps except the final git push
    --commit-message   Not used (kept for consistency with release-docs-stage.py)
"""

import argparse
import os
import platform
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


def play_sound(success=True):
    """
    Play a system sound to indicate success or failure.
    
    Args:
        success: True for success sound, False for failure sound
    """
    try:
        system = platform.system()
        if system == "Darwin":  # macOS
            if success:
                # Success sound
                sound_path = "/System/Library/Sounds/Glass.aiff"
            else:
                # Failure sound
                sound_path = "/System/Library/Sounds/Basso.aiff"
            
            if os.path.exists(sound_path):
                subprocess.run(
                    ["afplay", sound_path],
                    capture_output=True,
                    timeout=5
                )
        elif system == "Linux":
            # Try to use paplay (PulseAudio) or aplay (ALSA)
            if success:
                # Try to play a beep or use speaker-test
                try:
                    subprocess.run(
                        ["paplay", "/usr/share/sounds/freedesktop/stereo/complete.oga"],
                        capture_output=True,
                        timeout=5
                    )
                except:
                    # Fallback to speaker-test
                    subprocess.run(
                        ["speaker-test", "-t", "sine", "-f", "1000", "-l", "1"],
                        capture_output=True,
                        timeout=2
                    )
            else:
                try:
                    subprocess.run(
                        ["paplay", "/usr/share/sounds/freedesktop/stereo/dialog-error.oga"],
                        capture_output=True,
                        timeout=5
                    )
                except:
                    # Fallback to speaker-test with lower frequency
                    subprocess.run(
                        ["speaker-test", "-t", "sine", "-f", "400", "-l", "1"],
                        capture_output=True,
                        timeout=2
                    )
        elif system == "Windows":
            # Use winsound module
            import winsound
            if success:
                winsound.MessageBeep(winsound.MB_OK)
            else:
                winsound.MessageBeep(winsound.MB_ICONHAND)
    except Exception:
        # Silently fail if sound can't be played
        pass


def main():
    parser = argparse.ArgumentParser(
        description="Promote documentation from asf-staging to asf-site branch",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument(
        '--no-push',
        action='store_true',
        help='Perform all steps except the final git push'
    )
    parser.add_argument(
        '--commit-message',
        type=str,
        help='Not used (kept for consistency with release-docs-stage.py)'
    )
    
    args = parser.parse_args()
    
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    print("=" * 79)
    print("Promote Documentation to Production (asf-site branch)")
    print("=" * 79)
    print()
    print("⚠️  WARNING: This will promote documentation from asf-staging to asf-site,")
    print("   making it live on the production website.")
    print()
    
    # Get git remote URL
    remote_url = get_git_remote_url()
    if not remote_url:
        print("❌ ERROR: Could not determine git remote URL")
        sys.exit(1)
    
    # Create temp directory
    temp_dir = Path(tempfile.mkdtemp(prefix='docs-promote-'))
    print(f"Temporary directory: {temp_dir}")
    
    try:
        # Step 1: Clone repository to temp directory
        if not run_command(
            ["git", "clone", remote_url, str(temp_dir)],
            description="Cloning repository to temp directory"
        ):
            print("\n❌ Failed to clone repository")
            sys.exit(1)
        
        # Step 2: Fetch asf-staging branch
        if not run_command(
            ["git", "fetch", "origin", "asf-staging"],
            cwd=temp_dir,
            description="Fetching asf-staging branch"
        ):
            print("\n❌ Failed to fetch asf-staging branch")
            sys.exit(1)
        
        # Step 3: Switch to detached HEAD at origin/asf-staging
        if not run_command(
            ["git", "switch", "--detach", "origin/asf-staging"],
            cwd=temp_dir,
            description="Switching to detached HEAD at origin/asf-staging"
        ):
            print("\n❌ Failed to switch to asf-staging branch")
            sys.exit(1)
        
        # Step 4: Set git user (use current user's git config or defaults)
        try:
            result = subprocess.run(
                ["git", "config", "--get", "user.name"],
                capture_output=True,
                text=True
            )
            git_user = result.stdout.strip() if result.returncode == 0 else "Documentation Promoter"
            
            result = subprocess.run(
                ["git", "config", "--get", "user.email"],
                capture_output=True,
                text=True
            )
            git_email = result.stdout.strip() if result.returncode == 0 else "docs@juneau.apache.org"
        except Exception:
            git_user = "Documentation Promoter"
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
        
        # Step 5: Push to asf-site branch (if not --no-push)
        if not args.no_push:
            print("\nStep 5: Pushing to remote asf-site branch...")
            if not run_command(
                ["git", "push", "origin", "HEAD:asf-site", "--force"],
                cwd=temp_dir,
                description="Pushing to asf-site branch"
            ):
                print("\n❌ Failed to push to asf-site branch")
                sys.exit(1)
        else:
            print("\n⏭️  Skipping push (--no-push flag set)")
            print(f"   Changes are ready in: {temp_dir}")
            print("   You can manually push with:")
            print(f"   cd {temp_dir}")
            print("   git push origin HEAD:asf-site --force")
        
        print("\n" + "=" * 79)
        print("✅ Documentation promotion to production complete!")
        print("=" * 79)
        if args.no_push:
            print(f"\nTemporary directory: {temp_dir}")
            print("(This directory will not be automatically cleaned up)")
        
        # Play success sound
        play_sound(success=True)
        
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

