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
"""Set gpg-agent's cache TTLs in gpg-agent.conf so the release passphrase survives a release.  Full docs: gpg-agent-ttl.md"""

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

DEFAULT_TTL = 7200
MAX_TTL = 14400
KEYS = ("default-cache-ttl", "max-cache-ttl")
COMMENT = "# default-cache-ttl / max-cache-ttl added by juneau-release-manager scripts/gpg-agent-ttl.py"


def resolve_homedir(arg):
    """The gnupg home: --homedir, else $GNUPGHOME, else ~/.gnupg."""
    if arg:
        return Path(arg).expanduser()
    return Path(os.environ.get("GNUPGHOME") or "~/.gnupg").expanduser()


def as_int(text):
    """The int value of text, or 0 when it is not a number."""
    try:
        return int(text)
    except ValueError:
        return 0


def plan(lines, targets):
    """(new lines, actions) after raising lower values in place and appending missing keys; lines are kept otherwise."""
    out, actions, seen = [], [], set()
    for line in lines:
        parts = line.split()
        if parts and not parts[0].startswith("#") and parts[0] in targets:
            key = parts[0]
            seen.add(key)
            old = as_int(parts[1]) if len(parts) > 1 else 0
            if old < targets[key]:
                out.append(f"{key} {targets[key]}")
                actions.append(f"raise {key} from {old} to {targets[key]}")
                continue
        out.append(line)
    missing = [k for k in targets if k not in seen]
    if missing:
        if not any(line.startswith(COMMENT) for line in out):
            out.append(COMMENT)
        for key in missing:
            out.append(f"{key} {targets[key]}")
            actions.append(f"add {key} {targets[key]}")
    return out, actions


def write_atomic(path, text, mode):
    """Write text to path through a temp file in the same directory and a rename, leaving the file at `mode`."""
    fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=path.name + ".")
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(text)
        os.chmod(tmp, mode)
        os.replace(tmp, path)
    except BaseException:
        if os.path.exists(tmp):
            os.unlink(tmp)
        raise


def gpgconf(args, homedir_arg):
    """Run gpgconf (with --homedir when given) and return the CompletedProcess."""
    cmd = ["gpgconf"] + (["--homedir", str(homedir_arg)] if homedir_arg else []) + args
    return subprocess.run(cmd, capture_output=True, text=True, timeout=30)


def effective_values(output):
    """{name: effective value} for the two TTL keys from `gpgconf --list-options` output (configured, else default)."""
    out = {}
    for line in output.splitlines():
        f = line.split(":")
        if len(f) > 9 and f[0] in KEYS:
            out[f[0]] = f[9] or f[7] or "?"
    return out


def main(argv=None):
    parser = argparse.ArgumentParser(description="Raise gpg-agent's cache TTLs in gpg-agent.conf (never lowers a value).")
    parser.add_argument("--default-ttl", type=int, default=DEFAULT_TTL, help="default-cache-ttl seconds (default: 7200).")
    parser.add_argument("--max-ttl", type=int, default=MAX_TTL, help="max-cache-ttl seconds (default: 14400).")
    parser.add_argument("--homedir", help="gnupg home (default: $GNUPGHOME, else ~/.gnupg).")
    parser.add_argument("--no-reload", action="store_true", help="Do not run `gpgconf --reload gpg-agent` after a change.")
    parser.add_argument("--dry-run", action="store_true", help="Print what would change; write and reload nothing.")
    args = parser.parse_args(argv)
    if args.max_ttl < args.default_ttl:
        print(f"ERROR: --max-ttl ({args.max_ttl}) is lower than --default-ttl ({args.default_ttl})", file=sys.stderr)
        return 2
    targets = {"default-cache-ttl": args.default_ttl, "max-cache-ttl": args.max_ttl}
    home = resolve_homedir(args.homedir)
    conf = home / "gpg-agent.conf"
    dry = "would " if args.dry_run else ""
    if conf.exists():
        mode = conf.stat().st_mode & 0o7777
        lines, actions = plan(conf.read_text(encoding="utf-8").splitlines(), targets)
    else:
        mode = 0o600
        lines, actions = plan([], targets)
        actions = [f"{dry}create {conf}"] + actions
    changed = bool(actions)
    for a in actions:
        print(a if a.startswith(dry + "create") or not dry else dry + a)
    if not changed:
        print(f"{conf}: already OK")
    elif not args.dry_run:
        if not home.exists():
            home.mkdir(mode=0o700, parents=True)
        write_atomic(conf, "\n".join(lines) + "\n", mode)
    have_gpgconf = shutil.which("gpgconf") is not None
    if not have_gpgconf:
        print("WARNING: gpgconf not found on PATH; skipping reload and read-back", file=sys.stderr)
        return 0
    if changed and not args.dry_run and not args.no_reload:
        try:
            r = gpgconf(["--reload", "gpg-agent"], args.homedir)
            ok = r.returncode == 0
            err = r.stderr.strip()
        except (OSError, subprocess.SubprocessError) as e:
            ok, err = False, str(e)
        if ok:
            print("reloaded gpg-agent")
        else:
            print(f"WARNING: gpgconf --reload gpg-agent failed ({err}); run it by hand", file=sys.stderr)
    try:
        r = gpgconf(["--list-options", "gpg-agent"], args.homedir)
        vals = effective_values(r.stdout) if r.returncode == 0 else {}
    except (OSError, subprocess.SubprocessError):
        vals = {}
    if vals:
        print("effective: " + ", ".join(f"{k}={vals.get(k, '?')}" for k in KEYS))
    else:
        print("WARNING: could not read effective values from gpgconf --list-options gpg-agent", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
