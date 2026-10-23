#!/usr/bin/env python3
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""Apache Juneau release, from a terminal: release candidate, vote, promote.  Full docs: release.md"""
import argparse
import os
import sys
import time
import urllib.request
from dataclasses import dataclass
from pathlib import Path

REPO_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO_DIR / "scripts"))

import bootstrap  # noqa: E402
from jrm_release import inventory  # noqa: E402
from jrm_release.ctx import Ctx  # noqa: E402
from jrm_release.runfile import RunFile, RunFileError, new_attempt_dir  # noqa: E402

TITLE = "Release"
FINISHED = ("released",)


def default_home():
    return Path(os.environ.get("JUNEAU_RELEASE_HOME") or Path.home() / ".juneau-release-manager")


def build_parser():
    p = argparse.ArgumentParser(prog="release.py", description="Apache Juneau release, from a terminal: release candidate, vote, promote.")
    g = p.add_mutually_exclusive_group()
    g.add_argument("--resume", action="store_true", help="continue the active run after its last finished step")
    g.add_argument("--start-step", choices=inventory.NAMES, metavar="NAME",
                   help="start (or restart) at this step; one of: " + ", ".join(inventory.NAMES))
    p.add_argument("--skip-step", action="append", default=[], metavar="NAME",
                   help="skip a step; only: " + ", ".join(inventory.SKIPPABLE))
    p.add_argument("--list-steps", action="store_true", help="print the steps by phase, with finished ones marked")
    p.add_argument("--version", help="x.y.z: with --list-steps, which run's marks to show; on a new release, prefill the details")
    p.add_argument("--revert", action="store_true", help="undo the current release candidate")
    p.add_argument("--dry-run", action="store_true", help="run read-only commands; print every mutation")
    p.add_argument("--console", choices=("full", "condensed", "none"))
    p.add_argument("--home", type=Path, default=None, help="state directory (default: $JUNEAU_RELEASE_HOME)")
    p.add_argument("--attempt-dir", type=Path, help=argparse.SUPPRESS)
    return p


def parse(argv):
    p = build_parser()
    args = p.parse_args(argv)
    bad = [s for s in args.skip_step if s not in inventory.SKIPPABLE]
    if bad:
        p.error(f"--skip-step {bad[0]} is not skippable; skippable steps: {', '.join(inventory.SKIPPABLE)}")
    if args.revert and (args.resume or args.start_step or args.skip_step):
        p.error("--revert cannot be combined with --resume, --start-step or --skip-step")
    if args.version and not inventory.parse_version(args.version):
        p.error(f"--version {args.version} must look like x.y.z")
    args.home = args.home or default_home()
    return args


def list_steps(data, out=print):
    last = data.get("last")
    done = set()
    if last in inventory.NAMES:
        done = set(inventory.NAMES[:inventory.NAMES.index(last) + 1])
    if data.get("phase") == "released":
        done = set(inventory.NAMES)
    for phase, title in inventory.PHASES:
        out(title)
        for s in (s for s in inventory.STEPS if s.phase == phase):
            mark = "✔" if s.name in done else " "
            skip = "  (skippable)" if s.skippable else ""
            out(f"  {mark} {s.num:>2}. {s.name:<24} {s.title}{skip}")


def find_run(home, version=None, active_only=False):
    """(run.json path, data) of the named version's run, else the newest run (the newest active one when
    active_only), else (None, {}).  A corrupt file raises RunFileError."""
    runs = home / "runs"
    if version:
        paths = [runs / version / "run.json"]  # NOSONAR python:S1192 - a file name reads more clearly inline
    else:
        paths = sorted(runs.glob("*/run.json"), key=lambda p: p.stat().st_mtime, reverse=True) if runs.is_dir() else []
    for path in paths:
        if not path.exists():
            continue
        data = RunFile(path).data
        if active_only and (not data.get("details") or data.get("phase") in FINISHED):
            continue
        return path, data
    return None, {}


def active_run_data(home, version=None):
    return find_run(home, version)[1]


def guard_start(name, data):
    """A refusal message for --start-step NAME against run data, or None.  The irreversible line is nexus-release."""
    idx, last = inventory.NAMES.index(name), data.get("last")
    released = inventory.NAMES.index("nexus-release")
    public = last in inventory.NAMES and inventory.NAMES.index(last) >= released
    if public and idx < released:
        return f"cannot start at {name}: nexus-release already ran (the release is public).  Continue from dist-promote."
    step = inventory.step(name)
    if step.phase == "promote" and not public and (data.get("vote") or {}).get("outcome") != "passed":
        return f"cannot start at {name}: the vote has not passed (run vote-tally)."
    if step.phase == "vote" and not (last in inventory.NAMES and
                                      inventory.NAMES.index(last) >= inventory.NAMES.index("vote-gate")):
        return f"cannot start at {name}: vote-gate has not been passed yet."
    return None


def plan_for(home, resume=False, start=None):
    """The step names a run would execute, from the active run's state."""
    data = find_run(home, active_only=True)[1]
    last = data.get("last")
    if start:
        return inventory.NAMES[inventory.NAMES.index(start):]
    i = inventory.NAMES.index(last) + 1 if resume and last in inventory.NAMES else 0
    return inventory.NAMES[i:]


def lock_busy_message(pid, attempt):
    return f"Another release.py is running this release (pid {pid}, attempt #{attempt})."


def precondition(args, active):
    """(message, exit code) when the arguments cannot run against the active run's state, else None."""
    d = active.get("details") or {}
    if (args.revert or args.resume or args.start_step) and not active:
        if args.revert:
            what = "revert"
        elif args.resume:
            what = "resume"
        else:
            what = "restart"
        return f"No active release to {what}; start one with: release.py", 2
    if args.start_step:
        problem = guard_start(args.start_step, active)
        if problem:
            return problem, 2
    if not (args.resume or args.revert or args.start_step) and active:
        rc = d.get("rc")
        return (f"Run {d.get('version')} RC{rc} is in progress (phase {str(active.get('phase')).capitalize()}, "
                f"last {active.get('last')}).  Use --resume, or --revert to drop RC{rc}."), 2
    return None


@dataclass
class Deps:
    """The toolkit and collaborators main() uses; tests swap in fakes."""
    run: object
    steps: object
    prompt: object
    lock: object
    nexus: object = None        # a jrm_release.nexus.Nexus stand-in; None = the real client
    nexus_opener: object = None # the real client's urllib opener (tests); None = urlopen
    http_ok: object = None      # fn(url) -> bool; None = a real GET
    http_text: object = None    # fn(url) -> str | None (None = unreachable); None = a real GET
    sleep: object = None

    @classmethod
    def real(cls, args):
        cache = REPO_DIR / "target" / "juneau-run"
        bootstrap.load(REPO_DIR / "pom.xml", run=False, cache_dir=cache)
        from juneau_scripts import lock, prompt, run, steps
        run.export_console(args.console)   # before juneau_run loads: the flag beats JUNEAU_RUN_CONSOLE
        bootstrap.load(REPO_DIR / "pom.xml", run=True, cache_dir=cache)
        return cls(run, steps, prompt, lock)


def _http_ok(url):
    try:
        with urllib.request.urlopen(url, timeout=20) as r:   # noqa: S310 - fixed https endpoints
            return r.status == 200
    except OSError:
        return False


def _http_text(url):
    try:
        with urllib.request.urlopen(url, timeout=20) as r:   # noqa: S310 - fixed https endpoints
            return r.read().decode("utf-8", "replace")
    except OSError:
        return None


def build_ctx(args, deps, rf):
    from jrm_release.nexus import Nexus
    from jrm_release.runner import Runner
    from jrm_release.svn import Svn
    ctx = Ctx(args=args, home=args.home, run=deps.run, prompt=deps.prompt, runner=None, nexus=None, svn=None, rf=rf,
              pause=deps.steps.Pause, http_ok=deps.http_ok or _http_ok, http_text=deps.http_text or _http_text, sleep=deps.sleep or time.sleep)
    ctx.runner = Runner(deps.run, dry_run=args.dry_run, secrets=ctx.secrets)
    user = lambda: ctx.cred("credential.apache.availid")  # noqa: E731
    password = lambda: ctx.cred("credential.apache.password", secret=True)  # noqa: E731
    ctx.svn = Svn(ctx.runner, user, password)
    ctx.nexus = deps.nexus or Nexus(ctx.runner, lambda: (user(), password()),
                                    **({"opener": deps.nexus_opener} if deps.nexus_opener else {}))
    return ctx


def not_implemented(name):
    def fn(ctx):
        from jrm_release.ctx import StepError
        raise StepError(f"step {name} is not implemented yet")
    return fn


def impl_map():
    funcs = {}
    for modname in ("rc", "vote", "promote"):
        try:
            mod = __import__(f"jrm_release.{modname}", fromlist=["FUNCS"])
        except ImportError:
            continue
        funcs.update(mod.FUNCS)
    return funcs


def registry(deps, funcs):
    reg = deps.steps.Registry()
    reg._prog = "release.py"   # the resume hint's script name
    from jrm_release import rc
    for s in inventory.STEPS:
        if s.name in rc.CHECKS and hasattr(reg, "checks"):   # a bar of checks, which stays when they are all green
            reg.checks(s.name, s.title, rc.CHECKS[s.name], label=s.label)
        else:
            reg.step(s.name, s.title, label=s.label)(funcs.get(s.name) or not_implemented(s.name))
    return reg


def revert_registry(deps):
    from jrm_release import revert
    reg = deps.steps.Registry()
    for name, title, label in revert.ROWS:
        reg.step(name, title, label=label)(revert.FUNCS[name])
    return reg


def _command(args):
    if args.revert:
        return "revert"
    if args.resume:
        return "resume"
    if args.start_step:
        return "start-step"
    return "start"


def _execute(args, deps, ctx, attempt):
    """Run the registry (or the revert rows) under the open session; the exit code (0 or 1).  Ctrl-C propagates."""
    rf = ctx.rf
    if args.revert:
        reg, checkpoint = revert_registry(deps), None
        run_args = argparse.Namespace(list_steps=False, start_step=None, resume=False, skip_step=[])
    else:
        reg, checkpoint, run_args = registry(deps, impl_map()), rf, args
    from juneau_scripts import prompt
    origin = "ui" if prompt.channel() else "cli"
    rf.attempt_start(attempt, _command(args), origin=origin, pid=os.getpid())

    def stopped_at():
        cur = getattr(reg, "_current", None)
        return getattr(cur, "id", None) or rf.last()

    try:
        result = reg.run(run_args, ctx, checkpoint, show_unselected=not args.revert)
    except KeyboardInterrupt:
        rf.attempt_end("cancelled", step=stopped_at(), exit=130)
        raise
    except BaseException:
        rf.attempt_end("failed", step=stopped_at(), exit=1)
        raise
    code = 0 if result.ok else (result.code or 1)
    if result.status == "paused":
        status = "paused"
    else:
        status = "ok" if result.ok else "failed"
    rf.attempt_end(status, step=stopped_at(), exit=code)
    return code


def _locked(args, deps, ctx, attempt):
    if args.dry_run:   # a dry run keeps its state in memory, so it cannot collide with a real one
        return _execute(args, deps, ctx, attempt)
    lock_path = ctx.run_dir / ".lock"
    try:
        with deps.lock.flock(lock_path, blocking=False):
            lock_path.write_text(f"{os.getpid()} {len(ctx.rf.get('attempts', [])) + 1}")
            return _execute(args, deps, ctx, attempt)
    except deps.lock.LockBusy:
        pid, att = (lock_path.read_text().split() + ["?", "?"])[:2]
        print("❌ " + lock_busy_message(pid, att), file=sys.stderr)
        return 1


def _prerequisites(ctx):
    """Run the answer-independent checks before any prompt; the exit code (1 with the whole list) or 0."""
    from jrm_release import prereqs
    failed = prereqs.check(ctx)
    if not failed:
        return 0
    ctx.say(f"❌ {len(failed)} prerequisite(s) not met; fix them and re-run (nothing was asked or changed):", "error")
    for f in failed:
        ctx.say(f"   - {f}", "error")
    return 1


def _drive(args, deps, ctx, attempt, new):
    from jrm_release import details
    if not args.revert:   # before the branch, details and credential prompts, on a new run and on a resume alike
        code = _prerequisites(ctx)
        if code:
            return code
    if new:
        d = details.ask(ctx, version=args.version)
        rf = RunFile(args.home / "runs" / d["version"] / "run.json", dry_run=args.dry_run)
        rf.reset()
        rf.update({"details": d, "phase": "rc"})
        ctx.rf = rf
    return _locked(args, deps, ctx, attempt)


def run_release(args, deps, path, active):
    """Attempt dir, ctx, session; returns the exit code."""
    new = not (args.resume or args.revert or (args.start_step and active))
    rf = RunFile(path, dry_run=args.dry_run) if not new else RunFile(args.home / "runs" / "_new" / "run.json",
                                                                       dry_run=True)
    ctx = build_ctx(args, deps, rf)
    attempt = args.attempt_dir or (args.home / "attempts" / "dry-run" if args.dry_run else new_attempt_dir(args.home))
    attempt.mkdir(parents=True, exist_ok=True)
    (attempt / "full.log").touch()   # the banner's `tail -f` target; a dry run writes logs but never runs/
    version = (active.get("details") or {}).get("version")
    header = ("JRM", f"drop RC{(active.get('details') or {}).get('rc')} of {version}" if args.revert
              else version or "new release")
    plan = ([(n, t) for n, t, _ in __import__("jrm_release.revert", fromlist=["ROWS"]).ROWS] if args.revert
            else [(s.name, s.title) for s in inventory.STEPS])
    saved = os.environ.get("JUNEAU_RUN_FULL_LOG")
    os.environ["JUNEAU_RUN_FULL_LOG"] = str(attempt / "full.log")
    try:
        with deps.run.session(TITLE, header=header, bell=True, silence_env="JRM_RELEASE_NO_BELL", mode="release",
                              project="juneau-release-manager", repo=REPO_DIR, exit=False, plan=plan) as s:
            code = _drive(args, deps, ctx, attempt, new)
            if code:
                raise SystemExit(code)
    finally:
        if saved is None:
            os.environ.pop("JUNEAU_RUN_FULL_LOG", None)
        else:
            os.environ["JUNEAU_RUN_FULL_LOG"] = saved
    return s.code


def main(argv=None, deps=None):
    try:
        args = parse(argv)
    except SystemExit as e:  # NOSONAR python:S5754 - argparse's exit (--help, a usage error) becomes main's return code
        return e.code if isinstance(e.code, int) else 2
    try:
        if args.list_steps:
            list_steps(active_run_data(args.home, args.version))
            return 0
        path, active = find_run(args.home, active_only=True)
        refusal = precondition(args, active)
        if refusal:
            print(f"❌ {refusal[0]}", file=sys.stderr)
            return refusal[1]
        if deps is None:
            try:
                deps = Deps.real(args)
            except bootstrap.BootstrapError as e:
                print(f"❌ ERROR: {e}", file=sys.stderr)
                print("   (juneau-run 10.0.0-SNAPSHOT must be in ~/.m2: build Juneau first.)", file=sys.stderr)
                return 1
        return run_release(args, deps, path, active)
    except RunFileError as e:
        print(f"❌ {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
