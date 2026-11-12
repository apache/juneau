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
2. Regenerates stale AI artifacts (static/llms.txt, static/ai/) and stages exactly the files that run rewrote
3. Runs build-docs.py (the one list of checks, plus the Docusaurus build); --staging unless --no-stage
   Stages the javadoc pages that build rewrote (static/javadocs/) so they ride in the same commit
4. Commits whatever is staged (it stages nothing else) and pushes docs to its upstream
5. Stages the same build to asf-staging (release-docs-stage.py's fetch/copy/commit/push), unless --no-stage

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
AI_CHECK = 'check-ai-artifacts.py'
AI_GENERATE = 'generate-ai-artifacts.py'
AI_ROW = 'ai-artifacts'


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
    plan = [('prechecks', 'Prechecks'), (AI_ROW, 'AI artifacts'), *terminal_view.BUILD_ROWS, ('commit', 'Commit'), ('push', 'Push')]
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


def ai_artifact_path(path):
    """True for a path the AI-artifacts generator may write: static/llms.txt and anything under static/ai/."""
    return path == 'static/llms.txt' or path.startswith('static/ai/')


def regenerate_ai_artifacts(view, docs_dir, juneau_root):
    """
    Regenerate stale AI artifacts and stage exactly what that run wrote; returns (exit code, staged paths).

    This is the one thing this script stages, and the test is provenance: only files the generator itself wrote in this
    run, after the unreviewed-changes refusal has passed (so every unstaged or untracked path now is that output).  It
    refuses, staging nothing, when the generator wrote anything outside static/llms.txt and static/ai/, or when the
    operator had already staged one of those paths (unstaging on a failed run would drop their review).
    """
    check = docs_dir / 'scripts' / AI_CHECK
    generate = docs_dir / 'scripts' / AI_GENERATE
    if juneau_root is None or not check.exists() or not generate.exists():
        view.skip(AI_ROW, 'AI artifacts', "AI artifacts", "no Juneau checkout")
        return 0, []
    argv = ['--juneau-root', str(juneau_root)]

    def run(script):
        return subprocess.run([sys.executable, '-B', str(script), *argv], cwd=docs_dir, capture_output=True,
                              text=True, check=False)

    if run(check).returncode == 0:
        view.skip(AI_ROW, 'AI artifacts', "AI artifacts", "up to date")
        return 0, []
    try:
        already = [p for p in staged_paths(docs_dir) if ai_artifact_path(p)]
        before = set(unreviewed_paths(docs_dir))
    except RuntimeError as e:
        return fail(view, f"Could not read the git state: {e}"), []
    if already:
        return fail(view, "The AI artifacts are stale, but you have staged some of them already: " + ", ".join(already),
                    f"Regenerate them yourself (python3 scripts/{AI_GENERATE}) or unstage them, then run again."), []
    with view.stage(AI_ROW, 'AI artifacts', "Regenerating AI artifacts"):
        result = run(generate)
    if result.returncode != 0:
        return fail(view, f"scripts/{AI_GENERATE} failed (exit {result.returncode}). Nothing was committed or pushed.",
                    *(result.stdout + result.stderr).strip().splitlines()[-10:]), []
    try:
        written = sorted(set(unreviewed_paths(docs_dir)) - before)
    except RuntimeError as e:
        return fail(view, f"Could not read the git state: {e}"), []
    stray = [p for p in written if not ai_artifact_path(p)]
    if stray:
        return fail(view, f"scripts/{AI_GENERATE} wrote outside static/llms.txt and static/ai/; nothing was staged:",
                    *stray), []
    if run(check).returncode != 0:
        return fail(view, "The AI artifacts are still stale after regenerating them. Nothing was committed or pushed."), []
    if written:
        code, _, err = git_out(docs_dir, 'add', '--', *written)
        if code != 0:
            unstage_regenerated(docs_dir, written)
            return fail(view, f"git add failed: {err.strip()}"), []
        view.say(f"Regenerated {len(written)} AI artifact file{'s' if len(written) != 1 else ''} and staged "
                 f"{'it' if len(written) == 1 else 'them'} for this commit:")
        for path in written:
            view.say(f"   {path}")
    return 0, written


JAVADOC_PREFIX = 'static/javadocs/'


def stage_built_javadocs(view, docs_dir, staged_before):
    """
    Stage the javadoc output the build wrote (new, modified or deleted files under static/javadocs/); returns
    (exit code, paths newly staged by this run).

    Provenance is the same as for the AI artifacts: the unreviewed-changes refusal passed before the build, so every
    unstaged or untracked path now is build output.  A path the operator had staged before the run is re-added (it is
    generated) but not returned, so a failed run never unstages it.
    """
    try:
        built = [p for p in unreviewed_paths(docs_dir) if p.startswith(JAVADOC_PREFIX)]
    except RuntimeError as e:
        return fail(view, f"Could not read the git state: {e}"), []
    if not built:
        return 0, []
    code, _, err = git_out(docs_dir, 'add', '-A', '--', *built)
    if code != 0:
        unstage_regenerated(docs_dir, [p for p in built if p not in staged_before])
        return fail(view, f"git add failed: {err.strip()}"), []
    view.say(f"Staged {len(built)} regenerated javadoc file{'s' if len(built) != 1 else ''} for this commit.")
    return 0, [p for p in built if p not in staged_before]


def unstage_regenerated(docs_dir, paths):
    """Unstage exactly the paths regenerate_ai_artifacts staged (a no-op once they are committed); the files stay on disk."""
    if paths:
        git_out(docs_dir, 'restore', '--staged', '--', *paths)


def push_steps(args, view, stage, docs_dir, juneau_root):
    """
    Prechecks, the AI-artifacts regeneration, the build gate, commit, push and (unless --no-stage) the staging deploy;
    returns the exit code.  A run that ends in failure (or an exception) unstages whatever it regenerated and staged.
    """
    regenerated = []
    try:
        code = _run_steps(args, view, stage, docs_dir, juneau_root, regenerated)
    except BaseException:
        unstage_regenerated(docs_dir, regenerated)
        raise
    if code:
        unstage_regenerated(docs_dir, regenerated)
    return code


def _run_steps(args, view, stage, docs_dir, juneau_root, regenerated):
    """The steps of push_steps; appends to `regenerated` the paths it staged."""
    view.banner("Prechecks")
    if not view.prechecks(precheck_list(stage, docs_dir)):
        return 1

    view.banner("AI artifacts")
    code, paths = regenerate_ai_artifacts(view, docs_dir, juneau_root)
    regenerated.extend(paths)
    if code:
        return code

    try:
        staged_before = set(staged_paths(docs_dir))
    except RuntimeError as e:
        return fail(view, f"Could not read the index: {e}")

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

    # The build copies fresh javadocs into static/javadocs/: stage them so they ride in this commit.  Any other tracked
    # file it rewrote (artifact maps, the javadoc index) is build output, not reviewed work: reported, never committed.
    code, paths = stage_built_javadocs(view, docs_dir, staged_before)
    regenerated.extend(paths)
    if code:
        return code
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
    print(f"  2. python3 scripts/{AI_CHECK}; when stale, python3 scripts/{AI_GENERATE}, then stage only the "
          "files that run rewrote (static/llms.txt, static/ai/; refuses if it wrote anything else; unstaged again if "
          "the run fails before the commit)")
    print(f"  3. python3 scripts/build-docs.py {'--skip-maven' if args.no_stage else '--staging'} "
          "(every docs check, then the Docusaurus build)")
    print(f"  3b. Stage the javadoc output the build wrote under {JAVADOC_PREFIX} (unstaged again if the run fails "
          "before the commit; other build output is left unstaged)")
    print(f"  4. Commit whatever is staged: git commit -m \"{args.message}\" (stages nothing else)")
    print(f"  5. Push {REQUIRED_BRANCH} to its upstream")
    if args.no_stage:
        print("  6. Staging skipped (--no-stage)")
    else:
        print("  6. Stage the same build: fetch ../asf-staging, copy build/, commit, force-push asf-staging")
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
