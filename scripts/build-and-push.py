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
Build and Push Script for Juneau

This script automates the build, test, and deployment workflow:
1. Runs all tests
2. Builds and installs the project
3. Generates Javadocs
4. Commits changes to Git
5. Pushes to remote repository

Usage: python3 build-and-push.py "commit message"
       python3 build-and-push.py "commit message" --skip-tests
       python3 build-and-push.py "commit message" --skip-javadoc
"""

import argparse
import os
import platform
import subprocess
import sys
from pathlib import Path


def run_command(cmd, description, cwd=None):
    """
    Run a shell command and handle errors.
    
    Args:
        cmd: Command to run (string or list)
        description: Description of the step for output
        cwd: Working directory (defaults to script parent directory)
    
    Returns:
        True if successful, False otherwise
    """
    if cwd is None:
        cwd = Path(__file__).parent.parent
    
    print(f"\n{description}")
    print(f"Running: {' '.join(cmd) if isinstance(cmd, list) else cmd}")
    
    try:
        result = subprocess.run(
            cmd,
            cwd=cwd,
            shell=isinstance(cmd, str),
            check=True,
            capture_output=False,
            text=True
        )
        print(f"✅ {description} - SUCCESS")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ {description} - FAILED (exit code: {e.returncode})")
        return False
    except Exception as e:
        print(f"❌ {description} - FAILED: {e}")
        return False


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


def check_git_status(repo_dir):
    """Check if there are any changes to commit."""
    try:
        result = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        return len(result.stdout.strip()) > 0
    except Exception as e:
        print(f"⚠ Warning: Could not check git status: {e}")
        return True


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


def prompt_pgp_passphrase():
    """
    Make a dummy PGP call to prompt for passphrase early in the execution.
    
    This ensures the user is prompted for their PGP passphrase at the beginning
    rather than waiting until signing is needed near the end of the process.
    """
    print("\n🔐 Prompting for PGP passphrase (dummy call)...")
    try:
        # Create a small dummy file to sign
        import tempfile
        import os
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as tmp:
            tmp.write("dummy")
            tmp_path = tmp.name
        
        try:
            # Attempt to sign the dummy file (this will prompt for passphrase)
            # Don't use --batch so it will prompt interactively for passphrase
            # Use --yes to auto-confirm overwrite prompts, but allow passphrase prompt
            result = subprocess.run(
                ["gpg", "--yes", "--clearsign", tmp_path],
                capture_output=False,  # Don't capture output so user can see the prompt
                text=True,
                timeout=60  # 60 second timeout for passphrase entry
            )
            # Clean up the dummy file and signature
            try:
                os.unlink(tmp_path)
                if os.path.exists(tmp_path + ".asc"):
                    os.unlink(tmp_path + ".asc")
            except:
                pass
            print("✅ PGP passphrase entered successfully")
            return True
        except subprocess.TimeoutExpired:
            print("⚠ PGP passphrase prompt timed out (this is okay if signing isn't needed)")
            return True
        except FileNotFoundError:
            print("⚠ gpg command not found - skipping PGP passphrase prompt")
            return True
        except Exception as e:
            # If signing fails for any reason, that's okay - we're just trying to prompt early
            print(f"⚠ Could not prompt for PGP passphrase: {e}")
            return True
    except Exception as e:
        print(f"⚠ Could not set up PGP passphrase prompt: {e}")
        return True


def main():
    parser = argparse.ArgumentParser(
        description="Build, test, and push Juneau project to Git repository",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 build-and-push.py "Fixed bug in RestClient"
  python3 build-and-push.py "Updated documentation" --skip-tests
  python3 build-and-push.py "Minor formatting changes" --skip-javadoc
  python3 build-and-push.py "Quick fix" --skip-tests --skip-javadoc
        """
    )
    
    parser.add_argument(
        "message",
        help="Git commit message"
    )
    
    parser.add_argument(
        "--skip-tests",
        action="store_true",
        help="Skip running tests (useful for documentation-only changes)"
    )
    
    parser.add_argument(
        "--skip-javadoc",
        action="store_true",
        help="Skip Javadoc generation"
    )
    
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be done without actually doing it"
    )
    
    args = parser.parse_args()
    
    # Get the Juneau root directory
    script_dir = Path(__file__).parent
    juneau_root = script_dir.parent
    
    print("=" * 70)
    print("🚀 Juneau Build and Push Script")
    print("=" * 70)
    print(f"Working directory: {juneau_root}")
    print(f"Commit message: '{args.message}'")
    if args.skip_tests:
        print("⚠ Tests will be SKIPPED")
    if args.skip_javadoc:
        print("⚠ Javadoc generation will be SKIPPED")
    if args.dry_run:
        print("🔍 DRY RUN MODE - No actual changes will be made")
    print("=" * 70)
    
    if args.dry_run:
        print("\nSteps that would be executed:")
        step_num = 1
        print(f"  {step_num}. Prompt for PGP passphrase (dummy call)")
        step_num += 1
        if not args.skip_tests:
            print(f"  {step_num}. Run tests: mvn test")
            step_num += 1
        print(f"  {step_num}. Build and install: mvn clean package install")
        step_num += 1
        if not args.skip_javadoc:
            print(f"  {step_num}. Generate Javadocs: mvn javadoc:javadoc")
            step_num += 1
        print(f"  {step_num}. Commit changes: git add . && git commit -m \"{args.message}\"")
        step_num += 1
        print(f"  {step_num}. Push to remote: git push")
        print("\nDry run complete. Use without --dry-run to execute.")
        return 0
    
    # Prompt for PGP passphrase early (before any time-consuming operations)
    prompt_pgp_passphrase()
    
    step_num = 1
    
    # Step 1: Run tests (optional)
    if not args.skip_tests:
        if not run_command(
            ["mvn", "test"],
            f"🧪 Step {step_num}: Running tests...",
            juneau_root
        ):
            print("\n❌ Build process aborted due to test failures.")
            play_sound(success=False)
            return 1
        step_num += 1
    else:
        print(f"\n⏭️  Step {step_num}: Skipping tests (--skip-tests flag)")
        step_num += 1
    
    # Step 2: Build and install
    if not run_command(
        ["mvn", "clean", "package", "install"],
        f"🏗️  Step {step_num}: Building and installing project...",
        juneau_root
    ):
        print("\n❌ Build process aborted due to build failure.")
        play_sound(success=False)
        return 1
    step_num += 1
    
    # Step 3: Generate Javadocs (optional)
    if not args.skip_javadoc:
        if not run_command(
            ["mvn", "javadoc:javadoc"],
            f"📚 Step {step_num}: Generating Javadocs...",
            juneau_root
        ):
            print("\n❌ Build process aborted due to Javadoc generation failure.")
            play_sound(success=False)
            return 1
        step_num += 1
    else:
        print(f"\n⏭️  Step {step_num}: Skipping Javadoc generation (--skip-javadoc flag)")
        step_num += 1
    
    # Check if there are changes to commit
    if not check_git_status(juneau_root):
        print(f"\n⚠ Warning: No changes detected. Skipping commit and push.")
        print("🎉 Build completed successfully (nothing to commit)!")
        play_sound(success=True)
        return 0
    
    # Step 4: Git add and commit
    print(f"\n📝 Step {step_num}: Committing changes to Git...")
    if not run_command(
        ["git", "add", "."],
        f"  {step_num}.1: Staging all changes...",
        juneau_root
    ):
        print("\n❌ Build process aborted due to git add failure.")
        play_sound(success=False)
        return 1
    
    if not run_command(
        ["git", "commit", "-m", args.message],
        f"  {step_num}.2: Creating commit...",
        juneau_root
    ):
        print("\n❌ Build process aborted due to git commit failure.")
        play_sound(success=False)
        return 1
    print(f"✅ Step {step_num}: Git commit completed.")
    step_num += 1
    
    # Step 5: Push to remote
    if not run_command(
        ["git", "push"],
        f"🚀 Step {step_num}: Pushing changes to remote repository...",
        juneau_root
    ):
        print("\n❌ Build process aborted due to git push failure.")
        print("⚠ Your changes have been committed locally but not pushed.")
        play_sound(success=False)
        return 1
    
    # Success!
    print("\n" + "=" * 70)
    print("🎉 All operations completed successfully!")
    print(f"📦 Commit message: '{args.message}'")
    print("=" * 70)
    play_sound(success=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())

