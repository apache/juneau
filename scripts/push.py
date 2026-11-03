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
Gate, commit, push and stage the documentation: the juneau-docs counterpart of Juneau's push.py.

Full docs: push.md
"""

import argparse
import importlib.util
import os
import subprocess
import sys
import traceback
from pathlib import Path

import terminal_view
from juneau_root import find_juneau_root

_HELP_EPILOG = """
Push the documentation

This script:
1. Runs the Prechecks: git identity, branch docs, no unreviewed (unstaged or untracked) files, not behind upstream
2. Runs build-docs.py (the one list of checks, plus the Docusaurus build); --staging unless --no-stage
3. Commits whatever is already staged (it stages nothing) and pushes docs to its upstream
4. Stages the same build to asf-staging (release-docs-stage.py's fetch/copy/commit/push), unless --no-stage

Usage:
    python3 scripts/push.py "Commit message" [--no-stage] [--dry-run]
                            [--juneau-root DIR] [--console full|board|none]

Options:
    --no-stage       Gate, commit and push only: build with --skip-maven (no Maven site) and do not stage
    --dry-run        Print the steps and run the Prechecks; build, commit and push nothing
    --juneau-root    Juneau source checkout (default: $JUNEAU_ROOT, then sibling ../master, then ../juneau)
    --console        Console output when the Juneau checkout has juneau-terminal: full, board (default) or none
    --stage-message  Commit message for the asf-staging commit (default: "Deploy documentation staging")

Never promotes to production; that stays release-docs.py.
"""

REQUIRED_BRANCH = 'docs'


def load_sibling(filename):
    """Import a hyphenated sibling script (release-docs-stage.py) as a module."""
    path = Path(__file__).parent / filename
    spec = importlib.util.spec_from_file_location(filename.replace('-', '_').removesuffix('.py'), path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def git_out(repo, *args):
    """(exit code, stdout, stderr) of a git command in repo."""
    result = subprocess.run(['git', *args], cwd=repo, capture_output=True, text=True, check=False)
    return result.returncode, result.stdout, result.stderr


def staged_paths(repo):
    """Paths in the index that differ from HEAD."""
    code, out, err = git_out(repo, 'diff', '--cached', '--name-only')
    if code != 0:
        raise RuntimeError(err.strip() or 'git diff --cached failed')
    return [line for line in out.splitlines() if line]


def unreviewed_paths(repo):
    """Tracked files with unstaged changes plus untracked files that are not gitignored."""
    paths = []
    for args in (('diff', '--name-only'), ('ls-files', '--others', '--exclude-standard')):
        code, out, err = git_out(repo, *args)
        if code != 0:
            raise RuntimeError(err.strip() or f"git {args[0]} failed")
        paths += [line for line in out.splitlines() if line]
    return sorted(set(paths))


def branch_problem(repo):
    """None on branch docs; otherwise (status, reason, hints)."""
    code, out, _ = git_out(repo, 'symbolic-ref', '--short', '-q', 'HEAD')
    branch = out.strip() if code == 0 else None
    if branch != REQUIRED_BRANCH:
        return ('fail', f"on {branch or 'a detached HEAD'}, not {REQUIRED_BRANCH}",
                (f"git checkout {REQUIRED_BRANCH}",))
    return None


def unreviewed_problem(repo):
    """None when nothing is unstaged or untracked; otherwise (status, reason, hints) listing the paths."""
    try:
        paths = unreviewed_paths(repo)
    except RuntimeError as e:
        return ('fail', f"could not check the working tree: {e}", ())
    if not paths:
        return None
    hint = [*paths[:20], *([f"... and {len(paths) - 20} more"] if len(paths) > 20 else []),
            "Staging is how this script knows a path was reviewed: stage each one you have read, or set it aside",
            "(git stash push -- <paths>, then git stash pop after). Nothing was built, committed or pushed."]
    return ('fail', f"{len(paths)} unreviewed path{'s' if len(paths) != 1 else ''} (not staged)", tuple(hint))


def upstream_problem(repo):
    """None when docs has an upstream it is not behind; otherwise (status, reason, hints)."""
    warning = terminal_view.branch_warning(repo)
    if warning is not None:
        return ('fail', warning, ("git pull --rebase", "Nothing was built, committed or pushed."))
    code, _, _ = git_out(repo, 'rev-parse', '--abbrev-ref', '--symbolic-full-name', '@{upstream}')
    if code != 0:
        return ('fail', f"{REQUIRED_BRANCH} has no upstream", (f"git push -u origin {REQUIRED_BRANCH}",))
    return None


def ahead_count(repo):
    """Commits on HEAD that the upstream does not have."""
    code, out, _ = git_out(repo, 'rev-list', '--count', '@{upstream}..HEAD')
    return int(out.strip()) if code == 0 and out.strip().isdigit() else 0


def precheck_list(stage, docs_dir):
    """The Prechecks row's (name, fn) pairs, in order."""
    return [("Git identity (apache.org email)", lambda: stage.identity_problem(docs_dir)),
            ("Docs branch", lambda: branch_problem(docs_dir)),
            ("Unreviewed changes", lambda: unreviewed_problem(docs_dir)),
            ("Upstream", lambda: upstream_problem(docs_dir))]


def push_plan(args):
    """The run's top-level rows, in order, as (id, label)."""
    plan = [('prechecks', 'Prechecks'), *terminal_view.BUILD_ROWS, ('commit', 'Commit'), ('push', 'Push')]
    if not args.no_stage:
        plan += [('stage', 'Stage push'), ('url', 'URL')]
    return plan


def fail(view, message, *hints):
    """Report a failure and return the failing exit code."""
    view.say(f"❌ {message}", 'error')
    for hint in hints:
        view.say(f"   {hint}", 'error')
    return 1


def commit_and_push(view, docs_dir, message):
    """Commit the index as found (if anything is staged) and push to the upstream; returns the exit code."""
    try:
        staged = staged_paths(docs_dir)
    except RuntimeError as e:
        return fail(view, f"Could not read the index: {e}")
    ahead = ahead_count(docs_dir)
    if staged:
        view.cmd(['git', 'commit', '-q', '-m', message], cwd=docs_dir, id='commit', label='Commit',
                 title=f"Committing {len(staged)} staged path{'s' if len(staged) != 1 else ''}", tool='generic')
        ahead += 1
    else:
        view.skip('commit', 'Commit', "commit", "nothing staged")
    if ahead:
        with view.paused():   # a credential or passphrase prompt shows on a cleared screen
            view.cmd(['git', 'push'], cwd=docs_dir, id='push', label='Push', title=f"Pushing {REQUIRED_BRANCH}",
                     tool='generic', tty=True)
    else:
        view.skip('push', 'Push', "push", "nothing to push")
    return 0


def push_steps(args, view, stage, docs_dir, juneau_root):
    """Prechecks, the build gate, commit, push and (unless --no-stage) the staging deploy; returns the exit code."""
    view.banner("Prechecks")
    if not view.prechecks(precheck_list(stage, docs_dir)):
        return 1

    view.banner("Building and checking the documentation")
    if args.no_stage:
        if not (stage.BUILD_SCRIPT).exists():
            return fail(view, f"ERROR: {stage.BUILD_SCRIPT} not found")
        cmd = [sys.executable, str(stage.BUILD_SCRIPT), '--skip-maven']
        if args.juneau_root is not None:
            cmd += ['--juneau-root', str(juneau_root)]
        if args.console is not None:
            cmd += ['--console', args.console]
        with view.handoff() as extra:
            code = subprocess.run(cmd, cwd=docs_dir, env={**os.environ, **extra}).returncode
        if code != 0:
            return fail(view, "The documentation build or its checks failed. Nothing was committed or pushed.")
    else:
        code = stage.build_for_staging(view, docs_dir, juneau_root, juneau_root_given=args.juneau_root is not None,
                                       console=args.console)
        if code is not None:
            return code

    # The build can rewrite tracked files (artifact maps, the javadoc copy and index).  Those are build output, not
    # reviewed work, so they are reported but never committed (only the index is) and never block.
    try:
        built = unreviewed_paths(docs_dir)
    except RuntimeError:
        built = []
    if built:
        view.say(f"⚠ The build left {len(built)} changed path{'s' if len(built) != 1 else ''} in the tree "
                 f"(not committed): {', '.join(built[:5])}{', ...' if len(built) > 5 else ''}", 'warn')
    problem = upstream_problem(docs_dir)
    if problem is not None:
        return fail(view, f"After the build: {problem[1]}", *problem[2])

    code = commit_and_push(view, docs_dir, args.message)
    if code:
        return code

    if args.no_stage:
        view.say("✅ Pushed. Staging skipped (--no-stage).")
        return 0
    stage_args = argparse.Namespace(no_push=False, commit_message=args.stage_message)
    return stage.publish_build(stage_args, view, docs_dir)


def dry_run(args, view, stage, docs_dir):
    """Print the steps and run the Prechecks; returns the exit code."""
    print("Steps that would be executed:")
    print(f"  1. Prechecks: identity {stage.REQUIRED_GIT_EMAIL}, branch {REQUIRED_BRANCH}, no unreviewed "
          "(unstaged/untracked) changes, not behind upstream")
    print(f"  2. python3 scripts/build-docs.py {'--skip-maven' if args.no_stage else '--staging'} "
          "(every docs check, then the Docusaurus build)")
    print(f"  3. Commit whatever is already staged: git commit -m \"{args.message}\" (stages nothing)")
    print(f"  4. Push {REQUIRED_BRANCH} to its upstream")
    if args.no_stage:
        print("  5. Staging skipped (--no-stage)")
    else:
        print("  5. Stage the same build: fetch ../asf-staging, copy build/, commit, force-push asf-staging")
    ok = view.prechecks(precheck_list(stage, docs_dir))
    print("\nDry run complete. Use without --dry-run to execute.")
    return 0 if ok else 1


def resolve_message(message):
    """The commit message, prompted for when omitted; exits 1 when empty."""
    if message:
        return message
    try:
        message = input("Commit message: ").strip()
    except (EOFError, KeyboardInterrupt):
        message = ''
    if not message:
        print("❌ Aborted: a commit message is required.")
        sys.exit(1)
    return message


def main():
    parser = argparse.ArgumentParser(
        description="Gate, commit, push and stage the Juneau documentation",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=_HELP_EPILOG
    )
    parser.add_argument('message', nargs='?', help='Git commit message; prompted for if omitted')
    parser.add_argument('--no-stage', action='store_true',
                        help='Gate, commit and push only: build with --skip-maven and do not stage')
    parser.add_argument('--dry-run', action='store_true',
                        help='Print the steps and run the Prechecks; build, commit and push nothing')
    parser.add_argument('--stage-message', default='Deploy documentation staging',
                        help='Commit message for the asf-staging commit (default: "Deploy documentation staging")')
    parser.add_argument('--juneau-root', type=Path, default=None,
                        help='Juneau source checkout (default: $JUNEAU_ROOT, then sibling ../master, then ../juneau)')
    terminal_view.add_console_argument(parser)
    args = parser.parse_args()

    stage = load_sibling('release-docs-stage.py')
    docs_dir = Path(__file__).parent.absolute().parent
    juneau_root = find_juneau_root(docs_dir, args.juneau_root, allow_missing=True, validate=False)
    if not args.no_stage and (juneau_root is None or not Path(juneau_root).is_dir()):
        print("❌ A staged push needs the Juneau checkout for the Maven site and javadocs; "
              "pass --juneau-root or use --no-stage.")
        sys.exit(1)
    view = terminal_view.open_view(juneau_root, args.console, plain=args.dry_run)

    if args.dry_run:
        args.message = args.message or '<message>'
        sys.exit(dry_run(args, view, stage, docs_dir))
    args.message = resolve_message(args.message)

    if view.live:
        header = ["Juneau Docs", REQUIRED_BRANCH]
        if args.no_stage:
            header.append("--no-stage: not staged")
        code = view.session("Push", lambda: push_steps(args, view, stage, docs_dir, juneau_root),
                            header=header, lines=[f"Commit Message: {args.message}", f"Docs: {docs_dir}"],
                            plan=push_plan(args), repo=docs_dir, mode='push')
    else:
        print("=" * 79)
        print("Push Documentation")
        print("=" * 79)
        try:
            code = push_steps(args, view, stage, docs_dir, juneau_root)
        except subprocess.CalledProcessError as e:
            print(f"\n❌ Command failed (exit code: {e.returncode}): {' '.join(map(str, e.cmd))}")
            code = 1
        except KeyboardInterrupt:
            print("\n\n⚠️  Process interrupted by user")
            code = 130
        except Exception as e:
            print(f"\n❌ ERROR: {e}")
            traceback.print_exc()
            code = 1

    stage.play_sound(success=code == 0)
    sys.exit(code)


if __name__ == '__main__':
    main()
