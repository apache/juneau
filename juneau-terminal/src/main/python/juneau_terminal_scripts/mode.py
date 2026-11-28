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
"""`--mode auto|raw|dash|split` for push, release and deploy scripts.

A script adds the option with add_argument(parser) and, after parsing and before any real work, calls

    applied = mode.apply(args, oneshot=...)
    if applied.done:
        sys.exit(applied.exit_code)

raw: plain stdout, no board and no log or result files.  dash: the board view the script always had.  split: this process is
only the launcher; it re-runs the script as `--mode dash` through the watch-run engine (the panes, the log tail and the
toolbar), or, outside tmux on a terminal, in a new tmux session it attaches to.  Environment: JUNEAU_TERMINAL_MODE (the
requested mode when no flag is given), JUNEAU_TERMINAL_ENGINE=shim|direct|auto (how the engine is reached), and the
test-only JUNEAU_TERMINAL_ATTACH_CMD (replaces `tmux attach`).

The engine is reached two ways.  shim runs the extracted watch-run.sh as a child, the literal path a sandbox exclusion
can match.  direct imports terminal_engine and calls it in process.  auto picks shim when a functional probe
(`tmux display-message`, 5 s) fails, which is what a sandbox that blocks the tmux socket looks like.  No environment
variable is trusted as a sandbox marker: none was found in a Claude Code shell (see the README, "Split mode")."""

import os
import shlex
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from collections import namedtuple
from pathlib import Path

MODES = ("auto", "raw", "dash", "split")
ENGINES = ("auto", "shim", "direct")
MODE_ENV = "JUNEAU_TERMINAL_MODE"
KEEPS = ("never", "fail", "always")
KEEP_ENV = "JUNEAU_TERMINAL_KEEP"
ENGINE_ENV = "JUNEAU_TERMINAL_ENGINE"
ATTACH_ENV = "JUNEAU_TERMINAL_ATTACH_CMD"
ALT_ON, ALT_OFF = "\033[?1049h\033[H\033[2J", "\033[?1049l"
FINAL_FILE_ENV = "JUNEAU_TERMINAL_FINAL_FILE"
CONSOLE_ENV = "JUNEAU_TERMINAL_CONSOLE"
LOG_DIR_ENV = "JUNEAU_TERMINAL_LOG_DIR"
# Removed for a raw run, so no leftover session or result file nests it into a parent board.
RAW_SCRUB = ("JUNEAU_TERMINAL_FULL_LOG", "JUNEAU_TERMINAL_BOARD_LOG", "JUNEAU_TERMINAL_RESULT", "JUNEAU_TERMINAL_RESULT_PARTS",
             "JUNEAU_TERMINAL_CANCEL", "JUNEAU_TERMINAL_MARKERS_FILE", "JUNEAU_TERMINAL_SESSION", "JUNEAU_TERMINAL_ACTIVE")
# Set inside a juneau-terminal run: a script that inherits one is already being watched and never splits again.
NESTED_ENV = ("JUNEAU_TERMINAL_SESSION", "JUNEAU_TERMINAL_ACTIVE")
# Set by watch-run.sh and tmux-pane.sh for a hand-wrapped run (the optional safety net).
WRAPPED_ENV = ("JUNEAU_TERMINAL_RESULT", "JUNEAU_TERMINAL_CANCEL", "TMUX_PANE_RUN_ID")
TMUX_TIMEOUT = 5
EXIT_REFUSED = 3

Probes = namedtuple("Probes", "toolkit tmux tty_out tty_in inside")
Applied = namedtuple("Applied", "mode done exit_code")

_warned = set()


def add_argument(parser):
    """Add --mode to an argparse parser, next to the script's --console argument."""
    parser.add_argument(
        "--mode",
        choices=MODES,
        help="Layout: auto (default), raw, dash or split. See --console for how much dash shows (also JUNEAU_TERMINAL_MODE; the flag wins)."
    )
    parser.add_argument(
        "--keep",
        choices=KEEPS,
        help="Split mode: when the run's panes stay open after it ends: never, fail (default; only after a non-zero exit) or always "
             "(also JUNEAU_TERMINAL_KEEP; the flag wins)."
    )


def warn(script, text):
    """One stderr line prefixed with the script name, once per process per text."""
    if text in _warned:
        return
    _warned.add(text)
    print(f"{script}: {text}", file=sys.stderr)


def _tmux(*args, env=None):
    """(returncode, stdout) of a tmux call; (1, "") when tmux is missing, hangs or cannot start."""
    try:
        r = subprocess.run(["tmux", *args], capture_output=True, text=True, timeout=TMUX_TIMEOUT, env=env)
    except (OSError, subprocess.SubprocessError):
        return 1, ""
    return r.returncode, r.stdout


def probe(env=None):
    """The four probes of the resolution table.  toolkit is True: this module is part of the toolkit, so a script that
    imported it has one (a script whose toolkit is missing never gets here and runs raw)."""
    env = os.environ if env is None else env
    has_tmux = shutil.which("tmux") is not None
    inside = bool(env.get("TMUX")) and has_tmux and _tmux("display", "-p", "#{pane_id}")[0] == 0
    return Probes(True, has_tmux, sys.stdout.isatty(), sys.stdin.isatty(), inside)


def requested_mode(flag, env):
    """flag, else JUNEAU_TERMINAL_MODE, else auto.  A bad environment value raises ValueError."""
    value = flag or env.get(MODE_ENV) or "auto"
    if value not in MODES:
        raise ValueError(f"{MODE_ENV} must be one of {', '.join(MODES)}, not {value!r}")
    return value


def requested_keep(flag, env):
    """flag, else JUNEAU_TERMINAL_KEEP, else fail.  A bad environment value raises ValueError."""
    value = flag or env.get(KEEP_ENV) or "fail"
    if value not in KEEPS:
        raise ValueError(f"{KEEP_ENV} must be one of {', '.join(KEEPS)}, not {value!r}")
    return value


def _can_split(p):
    """tmux is there and there is somewhere to open panes: the current tmux, or a terminal to attach a new session to."""
    return p.toolkit and p.tmux and (p.inside or (p.tty_out and p.tty_in))


def resolve(requested, probes, env=None, oneshot=False):
    """(effective mode, warning text or None).  Pure: the probes are passed in.  See the spec's section 4.1."""
    env = os.environ if env is None else env
    if not probes.toolkit:
        return ("raw", None) if requested == "auto" else ("raw", f"--mode {requested}: juneau-terminal not available; using raw")
    if requested == "raw":
        return "raw", None
    if requested == "dash":
        mode, warning = "dash", None
    elif requested == "split":
        if not probes.tmux:
            return "raw", "--mode split: tmux not found; using raw"
        if not _can_split(probes):
            return "raw", "--mode split: not inside tmux and no terminal to attach one to; using raw"
        mode, warning = "split", None
    elif oneshot:
        mode, warning = "dash", None
    else:
        mode, warning = ("split" if _can_split(probes) else "dash"), None
    if mode == "split" and any(env.get(k) for k in NESTED_ENV + WRAPPED_ENV):
        mode = "dash"       # already inside a watched run: never a second layer of panes
    return mode, warning


def rewrite_argv(argv):
    """The inner argument list: any --mode X or --mode=X becomes --mode dash; when absent, `--mode dash` goes before a `--`."""
    out, found, i = [], False, 0
    argv = list(argv)
    while i < len(argv):
        a = argv[i]
        if a == "--":
            break
        if a == "--mode":
            out += ["--mode", "dash"]
            i += 2
            found = True
            continue
        if a.startswith("--mode="):
            out.append("--mode=dash")
            found = True
        else:
            out.append(a)
        i += 1
    rest = argv[i:]
    if not found:
        out += ["--mode", "dash"]
    return out + rest


def prepare_raw(args=None):
    """Make the process a plain run: scrub the session variables and force the console to full, on the namespace too, so a
    later export_console(args.console) cannot override it."""
    for k in RAW_SCRUB:
        os.environ.pop(k, None)
    os.environ[CONSOLE_ENV] = "full"
    if args is not None:
        args.console = "full"


# ---- the engine ----------------------------------------------------------------------------------------------------

def _source():
    """The toolkit source this module came from: the zip, or the directory holding juneau_terminal_scripts."""
    env = os.environ.get("JUNEAU_TERMINAL_SCRIPTS_ZIP")
    if env:
        return Path(env).expanduser().resolve()
    p = Path(__file__).resolve()
    for parent in p.parents:
        if parent.is_file():
            return parent
    return p.parent.parent


def engine_script(name="watch-run.sh"):
    """The engine shim's path: in the source tree for a directory source, extracted from the zip otherwise
    (bootstrap.bin_path, which is the literal path a sandbox exclusion names)."""
    import bootstrap
    return Path(bootstrap.bin_path(name, source=_source()))


def _under_cache(path):
    return Path.home().joinpath(".cache", "juneau-terminal") in Path(path).parents


def choose_engine(env=None, sandboxed=None):
    """shim or direct: JUNEAU_TERMINAL_ENGINE, else direct unless the functional probe says tmux is blocked.  sandboxed
    (a callable) replaces the probe in tests."""
    env = os.environ if env is None else env
    value = env.get(ENGINE_ENV) or "auto"
    if value not in ENGINES:
        raise ValueError(f"{ENGINE_ENV} must be one of {', '.join(ENGINES)}, not {value!r}")
    if value != "auto":
        return value
    blocked = sandboxed() if sandboxed else _tmux("display-message", "-p", "ok")[0] != 0
    return "shim" if blocked else "direct"


def _watch_args(cwd, title, no_close_kept, inner, keep="fail"):
    return ["-d", cwd, "-t", title, "-k", keep] + (["--no-close-kept"] if no_close_kept else []) + ["--", *inner]


def _tmux_version():
    """(major, minor) from `tmux -V`, or None."""
    import re
    m = re.search(r"(\d+)\.(\d+)", _tmux("-V")[1])
    return (int(m.group(1)), int(m.group(2))) if m else None


def running_leader(script):
    """The id of a pane in this window whose run is still going, or None.  tmux before 3.2 has no @tmux_state to read, so the
    check is skipped with one note."""
    version = _tmux_version()
    if version is not None and version < (3, 2):
        warn(script, "tmux is older than 3.2: cannot tell whether a run is still going in this window")
        return None
    target = os.environ.get("TMUX_PANE")
    # The run hides this pane's launcher in a window of its own, so match the run by its agent pane as well as by window.
    mine = _tmux("display-message", "-p", "-t", target, "#{window_id}")[1].strip() if target else ""
    for line in _tmux("list-panes", "-a", "-F", "#{pane_id} #{@tmux_state}|#{@tmux_agent}|#{window_id}")[1].splitlines():
        head, _, rest = line.partition("|")
        pane, _, state = head.partition(" ")
        agent, _, window = rest.partition("|")
        if state.strip() == "running" and (not target or agent == target or window == mine):
            return pane
    return None


def _run_shim(shim, args):
    """Run the shim as a child, forwarding INT and TERM, and return its exit code."""
    child = subprocess.Popen([str(shim), *args])
    old = {s: signal.signal(s, lambda signum, frame: child.terminate()) for s in (signal.SIGINT, signal.SIGTERM)}
    try:
        rc = child.wait()
    finally:
        for s, h in old.items():
            signal.signal(s, h)
    return rc if rc >= 0 else 128 - rc


def _run_direct(args):
    import bootstrap
    engine = bootstrap.load_engine(source=_source())
    return engine.main(["watch-run", *args])


def _launch_inside(script, cwd, title, no_close_kept, inner, engine=None, keep="fail"):
    pane = running_leader(script)
    if pane:
        print(f"{script}: another run is still running in this window: pane {pane} (cancel: tmux-pane.sh cancel {pane})",
              file=sys.stderr)
        return EXIT_REFUSED
    args = _watch_args(cwd, title, no_close_kept, inner, keep)
    try:
        choice = choose_engine() if engine is None else engine
    except ValueError as e:
        print(f"{script}: {e}", file=sys.stderr)
        return 2
    if choice == "direct":
        return _run_direct(args)
    shim = engine_script()
    if not _under_cache(shim):
        warn(script, f"engine shim {shim} is not an extracted cache path; a sandbox that blocks tmux may not allow it")
    return _run_shim(shim, args)


def _sessions(stem, env):
    """(name, attached, running) for every jt-<stem>-* session."""
    out = []
    for line in _tmux("list-sessions", "-F", "#{session_name} #{session_attached}", env=env)[1].splitlines():
        name, _, attached = line.partition(" ")
        if not name.startswith(f"jt-{stem}-"):
            continue
        states = _tmux("list-panes", "-s", "-t", name, "-F", "#{@tmux_state}", env=env)[1].split()
        out.append((name, attached.strip() != "0", "running" in states))
    return out


def _launch_new_session(script, cwd, title, inner, keep="fail"):
    """(done, exit code).  done is False when new-session failed and the caller should run dash instead."""
    env = {k: v for k, v in os.environ.items() if k != "TMUX"}     # a stale $TMUX must not aim new-session at a dead server
    stem = Path(script).stem
    for name, attached, running in _sessions(stem, env):
        if running:
            print(f"{script}: another run is still running in session {name} (attach: tmux attach -t {name})", file=sys.stderr)
            return True, EXIT_REFUSED
        if not attached:
            _tmux("kill-session", "-t", name, env=env)
    name = f"jt-{stem}-{os.getpid()}"
    shim = engine_script()
    fd, final = tempfile.mkstemp(prefix="jt-final-")
    os.close(fd)
    cmd = " ".join([f"{FINAL_FILE_ENV}={shlex.quote(final)}"] + [shlex.quote(a) for a in [str(shim), *_watch_args(cwd, title, True, inner, keep)]])    # the session is this run's alone
    size = shutil.get_terminal_size()
    r = subprocess.run(["tmux", "new-session", "-d", "-s", name, "-x", str(size.columns), "-y", str(size.lines), "-c", cwd, cmd],
                       capture_output=True, text=True, env=env)
    if r.returncode != 0:
        os.unlink(final)
        warn(script, f"could not start a tmux session ({(r.stderr or '').strip() or 'exit ' + str(r.returncode)}); using dash")
        return False, 0
    logdir = os.environ.get(LOG_DIR_ENV) or str(Path.home() / ".cache" / "juneau-terminal")
    print(f"{script}: logs in {logdir}")
    sys.stdout.flush()
    stub = os.environ.get(ATTACH_ENV)
    attach = [*shlex.split(stub), name] if stub else ["tmux", "attach", "-t", name]
    _await_hidden_launcher(name, env)
    alt = sys.stdout.isatty()
    if alt:                                  # the board's frames stay on the alternate screen, never in the user's scrollback
        sys.stdout.write(ALT_ON)
        sys.stdout.flush()
    try:
        rc = subprocess.run(attach, env=env).returncode
    finally:
        if alt:
            sys.stdout.write(ALT_OFF)
            sys.stdout.flush()
    return True, _final_report(final, rc)


def _await_hidden_launcher(name, env, limit=5.0):
    """Hold the attach until the session's launcher pane has moved to its own window (or the session is gone), so the first frame
    the user sees is the run's panes.  Gives up after limit seconds and attaches anyway."""
    deadline = time.monotonic() + limit
    while time.monotonic() < deadline:
        rc, out = _tmux("list-windows", "-t", name, "-F", "#{window_name}", env=env)
        if rc != 0 or "launcher" in out.split():
            return
        time.sleep(0.05)


def _final_report(final, attach_rc):
    """Print what the run left for the user's own terminal (the session is gone by now) and return the run's exit code, else the attach's."""
    code = attach_rc
    try:
        with open(final) as f:
            sys.stdout.write(f.read())
        sys.stdout.flush()
        with open(final + ".exit") as f:
            code = int(f.read().strip())
    except (OSError, ValueError):
        pass
    for p in (final, final + ".exit"):
        try:
            os.unlink(p)
        except OSError:
            pass
    return code


def _python():
    """`python3` when that resolves to this interpreter (the engine names the logs after the first script-like word, and a full
    interpreter path would make every log python3-...), else the interpreter's full path."""
    found = shutil.which("python3")
    return "python3" if found and os.path.realpath(found) == os.path.realpath(sys.executable) else sys.executable


def launch_split(script, argv, *, inside, title=None, no_close_kept=False, engine=None, keep="fail"):
    """Run the script as `--mode dash` in tmux panes.  Returns (done, exit code); done is False only when a new session could
    not be created, and the caller continues the run as dash."""
    cwd = os.getcwd()
    title = title or Path(script).stem
    inner = [_python(), str(Path(script).resolve()), *rewrite_argv(argv)]
    if inside:
        return True, _launch_inside(script, cwd, title, no_close_kept, inner, engine, keep)
    return _launch_new_session(script, cwd, title, inner, keep)


def apply(args=None, argv=None, *, oneshot=False, script=None, title=None, no_close_kept=False, probes=None, env=None):
    """Resolve the mode and act on it.  Returns Applied(mode, done, exit_code): done means this process was the launcher and
    must exit with exit_code; otherwise the script continues in the returned mode (raw or dash).

    argv is the script's own argument list (default sys.argv[1:]); oneshot marks an informational run (--help, --list-steps,
    --check), which auto resolves to dash instead of opening panes.  The nested-run rule and the sinks are handled here."""
    env = os.environ if env is None else env
    script = script or sys.argv[0]
    name = Path(script).name
    argv = sys.argv[1:] if argv is None else argv
    try:
        requested = requested_mode(getattr(args, "mode", None), env)
        keep = requested_keep(getattr(args, "keep", None), env)
    except ValueError as e:
        print(f"{name}: {e}", file=sys.stderr)
        return Applied("auto", True, 2)
    p = probes or probe(env)
    mode, warning = resolve(requested, p, env, oneshot)
    if warning:
        warn(name, warning)
    os.environ[MODE_ENV] = "dash" if mode == "split" else mode      # nested scripts never split
    if mode == "raw":
        prepare_raw(args)
    elif mode == "split":
        done, code = launch_split(script, argv, inside=p.inside, title=title, no_close_kept=no_close_kept, keep=keep)
        if done:
            return Applied("split", True, code)
        mode = "dash"
        os.environ[MODE_ENV] = mode
    return Applied(mode, False, 0)
