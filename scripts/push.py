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
3. Commits changes to Git
4. Pushes to remote repository

Usage: python3 push.py "commit message"
       python3 push.py "commit message" --skip-tests
"""

# Sound file paths
MACOS_SUCCESS_SOUND = "/System/Library/Sounds/Glass.aiff"
MACOS_FAILURE_SOUND = "/System/Library/Sounds/Basso.aiff"
LINUX_SUCCESS_SOUND = "/usr/share/sounds/freedesktop/stereo/complete.oga"
LINUX_FAILURE_SOUND = "/usr/share/sounds/freedesktop/stereo/dialog-error.oga"

import argparse
import os
import platform
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
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
        subprocess.run(
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


def play_sound(success=True):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this utility function
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
                sound_path = MACOS_SUCCESS_SOUND
            else:
                # Failure sound
                sound_path = MACOS_FAILURE_SOUND
            
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
                        ["paplay", LINUX_SUCCESS_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
                    # Fallback to speaker-test
                    subprocess.run(
                        ["speaker-test", "-t", "sine", "-f", "1000", "-l", "1"],
                        capture_output=True,
                        timeout=2
                    )
            else:
                try:
                    subprocess.run(
                        ["paplay", LINUX_FAILURE_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
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


def play_sound(success=True):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this utility function
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
                sound_path = MACOS_SUCCESS_SOUND
            else:
                # Failure sound
                sound_path = MACOS_FAILURE_SOUND
            
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
                        ["paplay", LINUX_SUCCESS_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
                    # Fallback to speaker-test
                    subprocess.run(
                        ["speaker-test", "-t", "sine", "-f", "1000", "-l", "1"],
                        capture_output=True,
                        timeout=2
                    )
            else:
                try:
                    subprocess.run(
                        ["paplay", LINUX_FAILURE_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
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


def check_upstream_changes(repo_dir):
    """
    Check if local branch is behind upstream/remote branch.
    
    Returns:
        tuple: (is_behind, error_message)
        - is_behind: True if local is behind remote, False otherwise
        - error_message: Error message if check failed, None otherwise
    """
    try:
        # Get current branch name
        result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        current_branch = result.stdout.strip()
        
        # Fetch latest from remote
        subprocess.run(
            ["git", "fetch"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        
        # Check if there's a remote tracking branch
        result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", f"{current_branch}@{{upstream}}"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=False
        )
        
        if result.returncode != 0:
            # No upstream branch configured, skip check
            return (False, None)
        
        upstream_branch = result.stdout.strip()
        
        # Get commit counts
        result = subprocess.run(
            ["git", "rev-list", "--left-right", "--count", f"{current_branch}...{upstream_branch}"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        
        # Output format: "X\tY" where X is commits ahead, Y is commits behind
        counts = result.stdout.strip().split('\t')
        if len(counts) != 2:
            return (False, "Could not parse upstream comparison")
        
        commits_behind = int(counts[1])
        return (commits_behind > 0, None)
        
    except subprocess.CalledProcessError as e:
        return (False, f"Git command failed: {e}")
    except Exception as e:
        return (False, f"Error checking upstream changes: {e}")


def current_branch(repo_dir):
    """Get current git branch name."""
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception:
        return "unknown"


def timing_log_path(repo_dir):
    """Out-of-repo branch-specific timing history location."""
    branch = current_branch(repo_dir).replace("/", "__")
    return Path.home() / ".cache" / "juneau-push-timings" / f"{branch}.jsonl"


def play_sound(success=True):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this utility function
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
                sound_path = MACOS_SUCCESS_SOUND
            else:
                # Failure sound
                sound_path = MACOS_FAILURE_SOUND
            
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
                        ["paplay", LINUX_SUCCESS_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
                    # Fallback to speaker-test
                    subprocess.run(
                        ["speaker-test", "-t", "sine", "-f", "1000", "-l", "1"],
                        capture_output=True,
                        timeout=2
                    )
            else:
                try:
                    subprocess.run(
                        ["paplay", LINUX_FAILURE_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
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


def _collect_surefire_stats(juneau_root: Path):
    """Aggregate tests/failures/errors/skipped from all Surefire XML files under juneau-integration-tests."""
    reports = juneau_root / "juneau-integration-tests" / "target" / "surefire-reports"
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    found = False
    for xml_path in sorted(reports.rglob("TEST-*.xml")):
        try:
            root = ET.parse(xml_path).getroot()
            for key in totals:
                totals[key] += int(root.attrib.get(key, 0))
            found = True
        except Exception:
            pass
    return (totals["tests"], totals["failures"], totals["errors"], totals["skipped"]) if found else None


def _append_test_run_history(juneau_root: Path, wall_sec: int) -> None:
    """Append one row to juneau-integration-tests/test-run-history.tsv; creates with header if absent. Never raises."""
    try:
        tsv_path = juneau_root / "juneau-integration-tests" / "test-run-history.tsv"
        header = "timestamp\tgit_sha\tbranch\ttests_run\tfailures\terrors\tskipped\twall_sec"
        need_header = not tsv_path.exists() or tsv_path.stat().st_size == 0

        ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

        try:
            sha = subprocess.check_output(
                ["git", "rev-parse", "HEAD"], cwd=juneau_root, text=True, stderr=subprocess.DEVNULL
            ).strip()[:12]
        except Exception:
            sha = "?"

        branch = current_branch(juneau_root)

        stats = _collect_surefire_stats(juneau_root)
        if stats is not None:
            tests_run, failures, errors, skipped = stats
        else:
            tests_run = failures = errors = skipped = "?"

        row = "\t".join(str(v) for v in [ts, sha, branch, tests_run, failures, errors, skipped, wall_sec])

        with tsv_path.open("a", encoding="utf-8") as f:
            if need_header:
                f.write(header + "\n")
            f.write(row + "\n")

        print(f"📊 Test metrics appended → juneau-integration-tests/test-run-history.tsv ({tests_run} tests, {wall_sec}s)")
    except Exception as exc:
        print(f"⚠ Warning: Could not append test metrics: {exc}")


def main():  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this main function
    parser = argparse.ArgumentParser(
        description="Build, test, and push Juneau project to Git repository",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 push.py "Fixed bug in RestClient"
  python3 push.py "Updated documentation" --skip-tests
  python3 push.py "Quick fix" --skip-tests
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
    if args.dry_run:
        print("🔍 DRY RUN MODE - No actual changes will be made")
    print("=" * 70)
    
    if args.dry_run:
        print("\nSteps that would be executed:")
        step_num = 1
        print(f"  {step_num}. Prompt for PGP passphrase (dummy call)")
        step_num += 1
        if not args.skip_tests:
            print(f"  {step_num}. Verify container test tags: python3 scripts/check-container-tags.py")
            step_num += 1
            print(f"  {step_num}. Run tests with timing capture: python3 scripts/test.py --full --timing-log ~/.cache/juneau-push-timings/<branch>.jsonl")
            step_num += 1
            print(f"  {step_num}. Print timing deltas: python3 scripts/push-timings.py --log ~/.cache/juneau-push-timings/<branch>.jsonl")
            step_num += 1
        print(f"  {step_num}. Build and install: mvn clean package install -DskipTests")
        step_num += 1
        print(f"  {step_num}. Commit changes: git add . && git commit -m \"{args.message}\"")
        step_num += 1
        print(f"  {step_num}. Push to remote: git push")
        print("\nDry run complete. Use without --dry-run to execute.")
        return 0
    
    # Prompt for PGP passphrase early (before any time-consuming operations)
    prompt_script = script_dir / 'prompt-pgp-passphrase.py'
    if prompt_script.exists():
        try:
            subprocess.run(
                [sys.executable, str(prompt_script)],
                check=False  # Don't fail if this doesn't work
            )
        except Exception as e:
            print(f"⚠ Could not run PGP passphrase prompt: {e}")
    
    step_num = 1
    
    # Step 1: Run tests (optional)
    if not args.skip_tests:
        check_container_tags = script_dir / "check-container-tags.py"
        if check_container_tags.exists():
            if not run_command(
                [sys.executable, str(check_container_tags)],
                f"🔎 Step {step_num}: Checking container test tags...",
                juneau_root
            ):
                print("\n❌ Build process aborted due to missing container test tags.")
                play_sound(success=False)
                return 1
            step_num += 1

        test_script = script_dir / 'test.py'
        timing_file = timing_log_path(juneau_root)
        if test_script.exists():
            print(f"\n🧪 Step {step_num}: Running tests via test.py...")
            try:
                _test_start = time.time()
                result = subprocess.run(
                    [sys.executable, str(test_script), "--full", "--timing-log", str(timing_file)],
                    cwd=juneau_root,
                    check=False
                )
                _test_wall_sec = int(time.time() - _test_start)
                if result.returncode != 0:
                    print("\n❌ Build process aborted due to test failures.")
                    play_sound(success=False)
                    return 1
                print(f"✅ Step {step_num}: Tests passed")
                _append_test_run_history(juneau_root, _test_wall_sec)
            except Exception as e:
                print(f"\n❌ Error running tests: {e}")
                play_sound(success=False)
                return 1
        else:
            # Fallback to direct mvn test if test.py doesn't exist
            _test_start = time.time()
            _mvn_ok = run_command(
                ["mvn", "test"],
                f"🧪 Step {step_num}: Running tests...",
                juneau_root
            )
            _test_wall_sec = int(time.time() - _test_start)
            if not _mvn_ok:
                print("\n❌ Build process aborted due to test failures.")
                play_sound(success=False)
                return 1
            _append_test_run_history(juneau_root, _test_wall_sec)
        timing_report = script_dir / "push-timings.py"
        if timing_report.exists():
            run_command(
                [sys.executable, str(timing_report), "--log", str(timing_file)],
                f"📊 Step {step_num}: Timing regression report...",
                juneau_root
            )
        step_num += 1
    else:
        print(f"\n⏭️  Step {step_num}: Skipping tests (--skip-tests flag)")
        step_num += 1
    
    # Step 2: Build and install (skip tests - already run in Step 1)
    if not run_command(
        ["mvn", "clean", "package", "install", "-DskipTests"],
        f"🏗️  Step {step_num}: Building and installing project...",
        juneau_root
    ):
        print("\n❌ Build process aborted due to build failure.")
        play_sound(success=False)
        return 1
    step_num += 1
    
    # Check if local branch is behind upstream
    print("\n🔍 Checking for upstream changes...")
    is_behind, error_msg = check_upstream_changes(juneau_root)
    if error_msg:
        print(f"\n⚠ Warning: Could not check upstream changes: {error_msg}")
        print("Continuing anyway...")
    elif is_behind:
        print("\n❌ ERROR: Local branch is behind upstream/remote branch.")
        print("Please pull/merge upstream changes before pushing.")
        print("Run: git pull")
        play_sound(success=False)
        return 1
    
    # Check if there are changes to commit
    if not check_git_status(juneau_root):
        print("\n⚠ Warning: No changes detected. Skipping commit and push.")
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
    
    step_num += 1

    # Step 6 (optional): juneau-docs follow-up — smoke check + commit + push
    docs_root = juneau_root.parent / "juneau-docs"
    if docs_root.exists() and check_git_status(docs_root):
        print(f"\n📚 Step {step_num}: juneau-docs has changes — running Docusaurus smoke check first...")

        docs_build_script = docs_root / "scripts" / "build-docs.py"
        docs_smoke_start = time.time()
        try:
            result = subprocess.run(
                [sys.executable, str(docs_build_script), "--skip-maven"],
                cwd=docs_root,
                check=False
            )
            docs_smoke_elapsed = time.time() - docs_smoke_start
            if result.returncode != 0:
                print(f"\n❌ Docs smoke check failed — fix the Docusaurus build before pushing juneau-docs.")
                play_sound(success=False)
                return 1
            print(f"✅ Docs smoke check passed ({docs_smoke_elapsed:.1f}s)")
        except Exception as e:
            print(f"\n❌ Docs smoke check failed: {e}")
            play_sound(success=False)
            return 1

        if not run_command(
            ["git", "add", "."],
            f"  {step_num}.1: Staging juneau-docs changes...",
            docs_root
        ):
            print("\n❌ Build process aborted due to juneau-docs git add failure.")
            play_sound(success=False)
            return 1

        if not run_command(
            ["git", "commit", "-m", args.message],
            f"  {step_num}.2: Committing juneau-docs changes...",
            docs_root
        ):
            print("\n❌ Build process aborted due to juneau-docs git commit failure.")
            play_sound(success=False)
            return 1

        if not run_command(
            ["git", "push"],
            f"  {step_num}.3: Pushing juneau-docs changes...",
            docs_root
        ):
            print("\n❌ juneau-docs push failed.")
            print("⚠ juneau-docs changes have been committed locally but not pushed.")
            play_sound(success=False)
            return 1

        print(f"✅ Step {step_num}: juneau-docs pushed successfully.")

    # Success!
    print("\n" + "=" * 70)
    print("🎉 All operations completed successfully!")
    print(f"📦 Commit message: '{args.message}'")
    print("=" * 70)
    play_sound(success=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())

