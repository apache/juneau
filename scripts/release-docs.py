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
Promote the documentation from the asf-staging branch to asf-site.

Full docs: release-docs.md
"""

import argparse
import os
import platform
import shutil
import subprocess
import sys
import traceback
from pathlib import Path

import run_view
from juneau_root import find_juneau_root

_HELP_EPILOG = """
Promote Documentation from Staging to Production

This script promotes the documentation from the asf-staging branch to the asf-site branch.
This makes the documentation live on the production website.

The script:
1. Checks the git identity
2. Fetches the asf-staging branch into the sibling asf-site directory
3. Switches to a detached HEAD at origin/asf-staging
4. Force pushes to the asf-site branch

Usage:
    python3 scripts/release-docs.py [--no-push] [--commit-message MESSAGE]
                                    [--juneau-root DIR] [--console full|condensed|none]

Options:
    --no-push          Perform all steps except the final git push
    --commit-message   Not used (kept for consistency with release-docs-stage.py)
    --juneau-root      Juneau source checkout whose juneau-run draws the live view (default:
                       $JUNEAU_ROOT, then sibling ../master, then sibling ../juneau)
    --console          Console output when juneau-run is available: full, condensed (default)
                       or none (also $JUNEAU_RUN_CONSOLE)

With juneau-run available, the run is one live view: Prechecks, then Promote (Fetch, Switch,
Push).  Without it the output is plain.
"""


# Identity guard — required for any push to ASF.
REQUIRED_GIT_EMAIL = "jamesbognar@apache.org"
PLAN = [('prechecks', 'Prechecks'), ('promote', 'Promote')]


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


def site_dir_for(docs_dir):
    """The persistent asf-site checkout, a sibling of the docs checkout."""
    return Path(docs_dir).parent / 'asf-site'


def git_identity():
    """The user name and email to record in the asf-site checkout (the current git config, or defaults)."""
    def get(key, default):
        result = subprocess.run(["git", "config", "--get", key], capture_output=True, text=True)
        return result.stdout.strip() if result.returncode == 0 else default
    try:
        return get("user.name", "Documentation Promoter"), get("user.email", "docs@juneau.apache.org")
    except Exception:
        return "Documentation Promoter", "docs@juneau.apache.org"


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


def promote(args, view, docs_dir):
    """Prechecks, then asf-staging force-pushed to asf-site; returns the exit code."""
    site_dir = site_dir_for(docs_dir)

    # Prechecks — must hold before any work begins.
    view.banner("Prechecks")
    if not view.prechecks([("Git identity (apache.org email)", lambda: identity_problem(docs_dir))]):
        return 1

    remote_url = get_git_remote_url()
    if not remote_url:
        view.say("❌ ERROR: Could not determine git remote URL", 'error')
        return 1
    if not site_dir.parent.exists():
        view.say(f"❌ ERROR: Parent directory not found: {site_dir.parent}", 'error')
        return 1
    view.say(f"Site directory: {site_dir}")

    git = dict(parent='promote', tool='generic')
    with view.stage('promote', 'Promote', "Promoting asf-staging to asf-site") as step:
        if site_dir.exists() and (site_dir / '.git').exists():
            # Directory exists and is a git repo, update it
            view.say("📁 Site directory already exists, updating...")
            view.remote_cmd(["git", "fetch", "origin"], cwd=site_dir, id='promote-fetch', label='Fetch',
                            title="Fetching latest changes", **git)
        else:
            # Directory doesn't exist or isn't a git repo, clone it
            if site_dir.exists():
                view.say(f"⚠️  Directory exists but is not a git repository. Removing: {site_dir}", 'warn')
                shutil.rmtree(site_dir)
            view.remote_cmd(["git", "clone", remote_url, str(site_dir)], id='promote-fetch', label='Fetch',
                            title="Cloning repository to site directory", **git)
        view.cmd(["git", "fetch", "origin", "asf-staging"], cwd=site_dir, id='promote-staging', label='Staging',
                 title="Fetching asf-staging branch", **git)
        view.cmd(["git", "switch", "--detach", "origin/asf-staging"], cwd=site_dir, id='promote-switch',
                 label='Switch', title="Switching to detached HEAD at origin/asf-staging", **git)

        git_user, git_email = git_identity()
        for key, value in (("user.name", git_user), ("user.email", git_email)):
            subprocess.run(["git", "config", key, value], cwd=site_dir, check=False)

        if args.no_push:
            view.skip('promote-push', 'Push', "push", "--no-push", parent='promote')
            step.summary = "push skipped (--no-push)"
        else:
            with view.paused():   # a credential or passphrase prompt shows on a cleared screen
                view.cmd(["git", "push", "origin", "HEAD:asf-site", "--force"], cwd=site_dir, id='promote-push',
                         label='Push', title="Pushing to asf-site branch", tty=True, **git)
            step.summary = "pushed asf-site"

    if args.no_push:
        view.say("⏭️  Skipping push (--no-push flag set)", 'warn')
        view.say(f"   Changes are ready in: {site_dir}", 'warn')
        view.say(f"   You can manually push with: cd {site_dir} && git push origin HEAD:asf-site --force", 'warn')
    view.say("✅ Documentation promotion to production complete!")
    view.say(f"Site directory: {site_dir} (persistent, reused for future deployments)")
    return 0


def main():
    parser = argparse.ArgumentParser(
        description="Promote documentation from asf-staging to asf-site branch",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=_HELP_EPILOG
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
    parser.add_argument(
        '--juneau-root',
        type=Path,
        default=None,
        help='Juneau source checkout whose juneau-run draws the live view (default: $JUNEAU_ROOT, then sibling '
             '../master, then ../juneau)'
    )
    run_view.add_console_argument(parser)

    args = parser.parse_args()

    script_dir = Path(__file__).parent.absolute()
    docs_dir = script_dir.parent  # docs/
    juneau_root = find_juneau_root(docs_dir, args.juneau_root, allow_missing=True, validate=False)
    view = run_view.open_view(juneau_root, args.console)

    if view.live:
        header = ["Juneau Docs", "--no-push: nothing is pushed" if args.no_push else "makes asf-staging LIVE on asf-site"]
        lines = [f"Docs: {docs_dir}", f"Site: {site_dir_for(docs_dir)}"]
        code = view.session("Promote", lambda: promote(args, view, docs_dir), header=header, lines=lines, plan=PLAN,
                            repo=docs_dir, mode='push')
    else:
        print("=" * 79)
        print("Promote Documentation to Production (asf-site branch)")
        print("=" * 79)
        print()
        print("⚠️  WARNING: This will promote documentation from asf-staging to asf-site,")
        print("   making it live on the production website.")
        print()
        try:
            code = promote(args, view, docs_dir)
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

    if code == 0:
        play_sound(success=True)
    sys.exit(code)


if __name__ == '__main__':
    main()
