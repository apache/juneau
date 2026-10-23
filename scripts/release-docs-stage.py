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
Release the documentation to the asf-staging branch.

Full docs: release-docs-stage.md
"""

import argparse
import os
import platform
import shutil
import subprocess
import sys
import traceback
import webbrowser
from pathlib import Path

import run_view
from juneau_root import find_juneau_root

_HELP_EPILOG = """
Release Documentation to Staging Branch

This script:
1. Checks the git identity (and warns when the docs branch is behind its upstream)
2. Runs build-docs.py --staging to build the documentation
3. Checks out the asf-staging branch in the sibling asf-staging directory
4. Copies the contents of docs/build there, and adds and commits the changes
5. Pushes to the remote asf-staging branch

Usage:
    python3 scripts/release-docs-stage.py [--no-push] [--commit-message MESSAGE]
                                          [--juneau-root DIR] [--console full|condensed|none]

Options:
    --no-push          Build and commit but don't push to remote
    --commit-message   Custom commit message (default: "Deploy documentation staging")
    --juneau-root      Juneau source checkout (default: $JUNEAU_ROOT, then sibling ../master,
                       then sibling ../juneau); passed on to build-docs.py
    --console          Console output when the Juneau checkout has juneau-run: full, condensed
                       (default) or none (also $JUNEAU_RUN_CONSOLE)

With juneau-run available, the run is one live view: Prechecks, build-docs.py's rows, Stage push
(Fetch, Copy, Commit, Push) and the staged URL.  Without it the output is plain.
"""


# Identity guard — required for any push to ASF.
REQUIRED_GIT_EMAIL = "jamesbognar@apache.org"
STAGING_URL = "http://juneau.staged.apache.org"
BUILD_SCRIPT = Path(__file__).parent / 'build-docs.py'


def identity_problem(repo_dir):
    """None when git is configured with the ASF committer identity; otherwise (status, reason, hint lines)."""
    try:
        result = subprocess.run(
            ["git", "config", "--get", "user.email"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=False,
        )
        email = result.stdout.strip()
    except Exception as e:
        return ('fail', f"could not read git user.email: {e}", ())

    if email != REQUIRED_GIT_EMAIL:
        return ('fail', f"user.email is '{email or '(unset)'}', not the ASF committer identity '{REQUIRED_GIT_EMAIL}'",
                ("Fix (this script cannot mutate git config):",
                 f"  git config user.email {REQUIRED_GIT_EMAIL}",
                 '  git config user.name "James Bognar"'))
    return None


def branch_problem(repo_dir):
    """None when the docs checkout is current; otherwise a warning (it never stops the release)."""
    warning = run_view.branch_warning(repo_dir)
    return None if warning is None else ('warn', warning, ())


def staging_dir_for(docs_dir):
    """The persistent asf-staging checkout, a sibling of the docs checkout."""
    return Path(docs_dir).parent / 'asf-staging'


def stage_plan():
    """The run's top-level rows, in order, as (id, label): Prechecks, build-docs.py's rows, Stage push and URL."""
    return [('prechecks', 'Prechecks'), *run_view.BUILD_ROWS, ('stage', 'Stage push'), ('url', 'URL')]


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


def git_identity():
    """The user name and email to commit with (the current git config, or defaults)."""
    def get(key, default):
        result = subprocess.run(["git", "config", "--get", key], capture_output=True, text=True)
        return result.stdout.strip() if result.returncode == 0 else default
    try:
        return get("user.name", "Documentation Builder"), get("user.email", "docs@juneau.apache.org")
    except Exception:
        return "Documentation Builder", "docs@juneau.apache.org"


def fail(view, message, *hints):
    """Report a failure and return the failing exit code."""
    view.say(f"❌ {message}", 'error')
    for hint in hints:
        view.say(f"   {hint}", 'error')
    return 1


def fetch_staging(view, staging_dir, remote_url):
    """Clone or update the asf-staging checkout and check out the asf-staging branch."""
    git = dict(parent='stage', tool='generic')
    if staging_dir.exists() and (staging_dir / '.git').exists():
        # Directory exists and is a git repo, update it
        view.say("📁 Staging directory already exists, updating...")
        view.remote_cmd(["git", "fetch", "origin"], cwd=staging_dir, id='stage-fetch', label='Fetch',
                        title="Fetching latest changes", **git)
    else:
        # Directory doesn't exist or isn't a git repo, clone it
        if staging_dir.exists():
            view.say(f"⚠️  Directory exists but is not a git repository. Removing: {staging_dir}", 'warn')
            shutil.rmtree(staging_dir)
        view.remote_cmd(["git", "clone", remote_url, str(staging_dir)], id='stage-fetch', label='Fetch',
                        title="Cloning repository to staging directory", **git)

    # Fetch asf-staging branch; don't fail if the branch doesn't exist yet
    result = subprocess.run(["git", "fetch", "origin", "asf-staging"], cwd=staging_dir, capture_output=True, text=True)
    if result.returncode != 0:
        view.say("⚠ Warning: Could not fetch asf-staging (branch may not exist yet)", 'warn')

    # Checkout or create asf-staging branch
    result = subprocess.run(
        ["git", "checkout", "-B", "asf-staging", "origin/asf-staging"],
        cwd=staging_dir,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True
    )
    if result.returncode != 0:
        # Branch doesn't exist, create it
        view.cmd(["git", "checkout", "-b", "asf-staging"], cwd=staging_dir, id='stage-branch', label='Branch',
                 title="Creating new asf-staging branch", **git)
    else:
        view.say("✅ Checked out asf-staging branch")


def copy_build(view, build_dir, staging_dir):
    """Replace everything in the staging checkout (except .git) with the build output."""
    with view.stage('stage-copy', 'Copy', "Copying build directory contents", parent='stage') as step:
        for item in staging_dir.iterdir():
            if item.name != '.git':
                if item.is_dir():
                    shutil.rmtree(item)
                else:
                    item.unlink()
        view.say("✅ Cleaned staging directory")
        for item in build_dir.iterdir():
            dest = staging_dir / item.name
            if item.is_dir():
                shutil.copytree(item, dest)
            else:
                shutil.copy2(item, dest)
        view.say(f"✅ Copied contents from {build_dir} to {staging_dir}")
        step.summary = f"build/ -> {staging_dir.name}"


def commit_staging(view, staging_dir, message):
    """Add everything and commit it when there is a change."""
    git_user, git_email = git_identity()
    for key, value in (("user.name", git_user), ("user.email", git_email)):
        subprocess.run(["git", "config", key, value], cwd=staging_dir, check=False)
    view.cmd(["git", "add", "-A"], cwd=staging_dir, id='stage-add', label='Add', title="Adding all files",
             parent='stage', tool='generic')

    # Check if there are changes to commit
    result = subprocess.run(["git", "diff", "--staged", "--quiet"], cwd=staging_dir)
    if result.returncode != 0:
        view.cmd(["git", "commit", "-q", "-m", message], cwd=staging_dir, id='stage-commit', label='Commit',
                 title=f"Committing changes: {message}", parent='stage', tool='generic')
    else:
        view.skip('stage-commit', 'Commit', "commit", "no changes", parent='stage')


def release_stage(args, view, docs_dir, juneau_root):
    """Prechecks, the staging build, then the asf-staging commit and push; returns the exit code."""
    build_dir = docs_dir / 'build'
    staging_dir = staging_dir_for(docs_dir)

    # Prechecks — must hold before any work begins.
    view.banner("Prechecks")
    if not view.prechecks([("Git identity (apache.org email)", lambda: identity_problem(docs_dir)),
                           ("Docs branch", lambda: branch_problem(docs_dir))]):
        return 1

    # Build the documentation for staging; under the view its rows join this run's.
    view.banner("Building documentation for staging")
    if not BUILD_SCRIPT.exists():
        return fail(view, f"ERROR: {BUILD_SCRIPT} not found")
    build_cmd = [sys.executable, str(BUILD_SCRIPT), '--staging']
    if args.juneau_root is not None:
        build_cmd += ['--juneau-root', str(juneau_root)]
    if args.console is not None:
        build_cmd += ['--console', args.console]
    with view.handoff() as extra:
        code = subprocess.run(build_cmd, cwd=docs_dir, env={**os.environ, **extra}).returncode
    if code != 0:
        return fail(view, "Documentation build failed. Aborting.")
    if not build_dir.exists():
        return fail(view, f"ERROR: Build directory not found at {build_dir}", "Documentation build may have failed.")

    remote_url = get_git_remote_url()
    if not remote_url:
        return fail(view, "ERROR: Could not determine git remote URL")
    if not staging_dir.parent.exists():
        return fail(view, f"ERROR: Parent directory not found: {staging_dir.parent}")
    view.say(f"Staging directory: {staging_dir}")

    with view.stage('stage', 'Stage push', "Staging to asf-staging") as step:
        fetch_staging(view, staging_dir, remote_url)
        copy_build(view, build_dir, staging_dir)
        commit_staging(view, staging_dir, args.commit_message)
        if args.no_push:
            view.skip('stage-push', 'Push', "push", "--no-push", parent='stage')
            step.summary = "push skipped (--no-push)"
        else:
            with view.paused():   # a credential or passphrase prompt shows on a cleared screen
                view.cmd(["git", "push", "origin", "asf-staging", "--force"], cwd=staging_dir, id='stage-push',
                         label='Push', title="Pushing to remote", parent='stage', tool='generic', tty=True)
            step.summary = "pushed asf-staging"

    if args.no_push:
        view.skip('url', 'URL', "staged site", "--no-push")
        view.say("⏭️  Skipping push (--no-push flag set)", 'warn')
        view.say(f"   Changes are in: {staging_dir}", 'warn')
        view.say(f"   You can manually push with: cd {staging_dir} && git push origin asf-staging --force", 'warn')
    else:
        with view.stage('url', 'URL', "Staged site") as step:
            step.summary = STAGING_URL
            view.say(f"🌐 Opening staging site in browser: {STAGING_URL}")
            try:
                webbrowser.open(STAGING_URL)
            except Exception as e:
                view.say(f"⚠️  Could not open browser automatically: {e}", 'warn')
                view.say(f"   Please visit {STAGING_URL} manually", 'warn')

    view.say("✅ Documentation staging deployment complete!")
    view.say(f"Staging directory: {staging_dir} (persistent, reused for future deployments)")
    return 0


def main():
    parser = argparse.ArgumentParser(
        description="Release documentation to asf-staging branch",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=_HELP_EPILOG
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
    parser.add_argument(
        '--juneau-root',
        type=Path,
        default=None,
        help='Juneau source checkout (default: $JUNEAU_ROOT, then sibling ../master, then ../juneau)'
    )
    run_view.add_console_argument(parser)

    args = parser.parse_args()

    script_dir = Path(__file__).parent.absolute()
    docs_dir = script_dir.parent  # docs/
    juneau_root = find_juneau_root(docs_dir, args.juneau_root, allow_missing=True, validate=False)
    view = run_view.open_view(juneau_root, args.console)

    if view.live:
        header = ["Juneau Docs"]
        if args.no_push:
            header.append("--no-push: nothing is pushed")
        lines = [f"Docs: {docs_dir}", f"Staging: {staging_dir_for(docs_dir)}"]
        code = view.session("Stage", lambda: release_stage(args, view, docs_dir, juneau_root),
                            header=header, lines=lines, plan=stage_plan(), repo=docs_dir, mode='push')
    else:
        print("=" * 79)
        print("Release Documentation to Staging Branch")
        print("=" * 79)
        try:
            code = release_stage(args, view, docs_dir, juneau_root)
        except subprocess.CalledProcessError as e:
            print(f"\n❌ Command failed (exit code: {e.returncode}): {' '.join(map(str, e.cmd))}")
            code = 1
        except KeyboardInterrupt:
            print("\n\n⚠️  Process interrupted by user")
            code = 1
        except Exception as e:
            print(f"\n❌ ERROR: {e}")
            traceback.print_exc()
            code = 1

    play_sound(success=code == 0)
    sys.exit(code)


if __name__ == '__main__':
    main()
