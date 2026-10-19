"""<home>/runs/<version>/run.json (schema 1) and the per-attempt log directories."""
import copy
import json
import os
import tempfile
from datetime import datetime
from pathlib import Path

from .inventory import RETIRED

SCHEMA = 1


class RunFileError(Exception):
    pass


def stamp(now=None):
    return (now or datetime.now()).strftime("%Y%m%d-%H%M%S")


def new_attempt_dir(home, now=None):
    """<home>/attempts/<YYYYMMDD-HHMMSS>-<NNN>, created with os.mkdir; a taken number is skipped."""
    base = Path(home) / "attempts"
    base.mkdir(parents=True, exist_ok=True)
    n = 1
    while True:
        d = base / f"{stamp(now)}-{n:03d}"
        try:
            os.mkdir(d)
            return d
        except FileExistsError:
            n += 1


class RunFile:
    """The release checkpoint.  Same get/set/update/clear/last() surface as state.Checkpoint (which steps.Registry
    drives), atomic writes, and an in-memory copy when dry_run (a dry run never writes run.json)."""

    def __init__(self, path, dry_run=False):
        self.path, self.dry_run = Path(path), dry_run
        self.data = {"schema": SCHEMA}
        if self.path.exists():
            try:
                self.data = json.loads(self.path.read_text())
            except (OSError, ValueError) as e:
                raise RunFileError(f"cannot read {self.path}: {e}.  Inspect or delete it; a release never "
                                   f"resumes from a guess.") from e
            if not isinstance(self.data, dict) or self.data.get("schema") != SCHEMA:
                raise RunFileError(f"{self.path} is not a schema-{SCHEMA} run file")
        if self.data.get("last") in RETIRED:   # saved by a version that still had the step
            self.data["last"] = RETIRED[self.data["last"]]
        if dry_run:
            self.data = copy.deepcopy(self.data)

    def exists(self):
        return self.path.exists()

    def get(self, key, default=None):
        return self.data.get(key, default)

    def last(self):
        """The last completed step's name, or None."""
        value = self.data.get("last")
        return value if isinstance(value, str) else None

    def set(self, key, value):
        self.update({key: value})

    def update(self, mapping):
        for k, v in mapping.items():
            if v is None:
                self.data.pop(k, None)
            else:
                self.data[k] = v
        self._save()

    def clear(self):
        self.data = {"schema": SCHEMA}
        self._save()

    def _save(self):
        if self.dry_run:
            return
        self.path.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(dir=self.path.parent, prefix="run.json.")
        with os.fdopen(fd, "w") as f:
            json.dump(self.data, f, indent=2, sort_keys=True)
        os.replace(tmp, self.path)

    def reset(self):
        """Start the file over (in memory only; the next update saves it)."""
        self.data = {"schema": SCHEMA}

    def attempt_start(self, d, command=None):
        a = list(self.data.get("attempts", []))
        a.append({"n": len(a) + 1, "dir": str(d), "command": command, "status": "running", "started": stamp()})
        self.update({"attempts": a})

    def attempt_end(self, status):
        a = list(self.data.get("attempts", []))
        if a:
            a[-1] = {**a[-1], "status": status, "ended": stamp()}
            self.update({"attempts": a})
