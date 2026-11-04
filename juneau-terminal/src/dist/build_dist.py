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
"""Builds juneau-terminal's convenience packages: the wheel and sdist (through hatchling's build backend), the .pyz
zipapp and the npm tarball of the Playwright reporter.  The same source, version and --epoch give the same bytes,
whatever the umask, the file mtimes or the directory, so a voter can rebuild from the source release and compare.

    python3 src/dist/build_dist.py --version 10.0.0 --epoch 2026-01-09T19:00:20Z --out target/dist [--only wheel ...]

Writes, under --out, the files pom.xml's apache-release profile attaches: the wheel and sdist under the names pip and
PyPI require (apache_juneau_terminal-<pv>-py3-none-any.whl, apache_juneau_terminal-<pv>.tar.gz, where <pv> is the PEP 440
form of the version), juneau-terminal-<v>.pyz and juneau-terminal-<v>-playwright.tgz, plus the staged wheel root in
dist-src/.  maven/ holds copies of the wheel and sdist named juneau-terminal-<v>.whl and juneau-terminal-<v>-sdist.tar.gz
for the pom to attach.  Stdlib only; hatchling must be the version pinned in requirements-dist.txt, from this python
or through uv.  Exit 0 built, 1 a build step failed, 2 a usage error or a missing tool (named)."""

import argparse
import gzip
import io
import itertools
import json
import os
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import time
import zipfile
from datetime import datetime
from pathlib import Path

DIST = Path(__file__).resolve().parent
MODULE = DIST.parent.parent
PY = MODULE / "src" / "main" / "python"
REPORTER = MODULE / "src" / "main" / "js" / "playwright_reporter" / "juneau_terminal_playwright.cjs"
REPO = MODULE.parent
KINDS = ("wheel", "sdist", "pyz", "tgz")
NPM_NAME = "@apache/juneau-terminal-playwright"
PLAYWRIGHT_RANGE = ">=1.30.0"
ZIP_FLOOR = 315532800   # 1980-01-01T00:00:00Z: the earliest time a zip entry can hold
HEADER = "".join(itertools.takewhile(lambda line: line.startswith("#"),
                                     (PY / "bootstrap.py").read_text().splitlines(True)))   # the license comment block
VERSION_FILE = '"""Written by src/dist/build_dist.py into the packaged copy only; a source tree has no such file."""\n\n' \
               '__version__ = "{version}"\n'
BACKEND = ("import sys, hatchling.build as b; "
           "print(b.build_wheel(sys.argv[1]) if sys.argv[2] == 'wheel' else b.build_sdist(sys.argv[1]))")


class BuildError(Exception):
    """A build step failed; code is the exit code."""

    def __init__(self, message, code=1):
        super().__init__(message)
        self.code = code


def python_version(maven):
    """The PEP 440 form of a Maven version: 10.0.0 stays, 10.0.0-SNAPSHOT becomes 10.0.0.dev0, 10.0.0-RC1 10.0.0rc1."""
    v = re.sub(r"-SNAPSHOT$", ".dev0", maven)
    v = re.sub(r"-[Rr][Cc](\d+)", r"rc\1", v)
    if not re.fullmatch(r"\d+(\.\d+)*(rc\d+)?(\.dev0)?", v):
        raise BuildError(f"cannot map Maven version {maven!r} to a Python package version", 2)
    return v


def parse_epoch(text):
    """Seconds since 1970 from a number or an ISO-8601 time (Maven's project.build.outputTimestamp), floored at 1980."""
    if re.fullmatch(r"\d+", text):
        seconds = int(text)
    else:
        try:
            seconds = int(datetime.fromisoformat(text.replace("Z", "+00:00")).timestamp())
        except ValueError:
            raise BuildError(f"--epoch {text!r} is neither seconds nor an ISO-8601 time", 2) from None
    return max(seconds, ZIP_FLOOR)


def hatchling_pin():
    m = re.search(r"^hatchling==(\S+)$", (DIST / "requirements-dist.txt").read_text(), re.M)
    if not m:
        raise BuildError("requirements-dist.txt pins no hatchling version", 2)
    return m.group(1)


def backend_python(pin):
    """The argv prefix of a python whose hatchling is the pinned version: this one, else uv's (offline, then online)."""
    probe = "import hatchling.__about__ as a; print(a.__version__)"
    candidates = [[sys.executable]]
    uv = shutil.which("uv")
    if uv:
        req = str(DIST / "requirements-dist.txt")
        candidates += [[uv, "run", "--offline", "--no-project", "--with-requirements", req, "python"],
                       [uv, "run", "--no-project", "--with-requirements", req, "python"]]
    for argv in candidates:
        p = subprocess.run([*argv, "-B", "-c", probe], capture_output=True, text=True, cwd=tempfile.gettempdir())
        if p.returncode == 0 and p.stdout.strip() == pin:
            return argv
    raise BuildError(f"hatchling=={pin} not found: pip install -r {DIST / 'requirements-dist.txt'}, or install uv", 2)


def _write(path, data, mode):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    os.chmod(path, mode)


def stage(dest, version, pin):
    """The wheel root, renamed to the namespaced top-level names, plus pyproject.toml, README.md, LICENSE and NOTICE."""
    if dest.exists():
        shutil.rmtree(dest)
    plugin = PY / "pytest_plugin" / "juneau_terminal_pytest.py"
    copies = {"juneau_terminal.py": PY / "juneau_terminal.py",
              "juneau_terminal_cli.py": PY / "juneau_terminal_cli.py",
              "juneau_terminal_bootstrap.py": PY / "bootstrap.py",
              "juneau_terminal_pytest.py": plugin,                                  # the pytest11 entry point's module
              "juneau_terminal_pytest_plugin/juneau_terminal_pytest.py": plugin,    # the runner's PYTHONPATH copy
              "juneau_terminal_playwright_reporter/juneau_terminal_playwright.cjs": REPORTER,
              "README.md": MODULE / "README.md", "LICENSE": REPO / "LICENSE", "NOTICE": REPO / "NOTICE"}
    scripts = PY / "juneau_terminal_scripts"
    for f in sorted(scripts.rglob("*")):
        rel = f.relative_to(PY).as_posix()
        if f.is_file() and "__pycache__" not in f.parts and (f.suffix == ".py" or (f.parent == scripts / "bin" and f.suffix == ".sh")):
            copies[rel] = f
    for rel, src in copies.items():
        _write(dest / rel, src.read_bytes(), 0o755 if rel.endswith(".sh") else 0o644)
    pv = python_version(version)
    _write(dest / "juneau_terminal_scripts" / "_build_version.py", (HEADER + VERSION_FILE.format(version=pv)).encode(), 0o644)
    text = (DIST / "pyproject.toml").read_text().replace("@VERSION@", pv).replace("@HATCHLING@", pin)
    _write(dest / "pyproject.toml", text.encode(), 0o644)
    return dest


def build_backend(kind, staged, python, epoch, out):
    """wheel or sdist through hatchling's PEP 517 hooks, in a child so this script stays stdlib only.  Keeps the
    backend's file name: pip refuses a wheel whose name is not name-version-py3-none-any.whl."""
    with tempfile.TemporaryDirectory() as tmp:
        env = {**os.environ, "SOURCE_DATE_EPOCH": str(epoch), "PYTHONDONTWRITEBYTECODE": "1"}
        p = subprocess.run([*python, "-B", "-c", BACKEND, tmp, kind], cwd=staged, env=env, capture_output=True, text=True)
        if p.returncode != 0:
            raise BuildError(f"hatchling {kind} failed:\n{p.stderr.strip()}")
        name = p.stdout.strip().splitlines()[-1]
        target = out / name
        if target.exists():
            target.unlink()
        shutil.move(str(Path(tmp) / name), target)
    return target


def write_pyz(staged, target, epoch):
    """The wheel root plus the .pyz's own __main__.py behind a python3 shebang, as a zip with sorted entries, one
    timestamp and fixed modes."""
    members = {f.relative_to(staged).as_posix(): f for f in staged.rglob("*") if f.is_file()}
    for rel in ("pyproject.toml", "README.md"):
        members.pop(rel, None)
    members["__main__.py"] = DIST / "pyz" / "__main__.py"
    stamp = time.gmtime(epoch)[:6]
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        for rel in sorted(members):
            info = zipfile.ZipInfo(rel, date_time=stamp)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = (0o100755 if rel.endswith(".sh") else 0o100644) << 16
            z.writestr(info, members[rel].read_bytes(), compresslevel=9)
    _write(target, b"#!/usr/bin/env python3\n" + buf.getvalue(), 0o755)
    return target


def package_json(version):
    return {"name": NPM_NAME, "version": version,
            "description": "Apache Juneau terminal-protocol reporter for Playwright: one ##playwright line per test event.",
            "license": "Apache-2.0", "main": "juneau_terminal_playwright.cjs",
            "files": ["juneau_terminal_playwright.cjs", "LICENSE", "NOTICE"],
            "engines": {"node": ">=16"}, "peerDependencies": {"@playwright/test": PLAYWRIGHT_RANGE},
            "publishConfig": {"access": "public"},
            "repository": {"type": "git", "url": "git+https://github.com/apache/juneau.git", "directory": "juneau-terminal"},
            "homepage": "https://juneau.apache.org/"}


def write_tgz(version, target, epoch):
    """The npm tarball: package/ with package.json, the reporter, LICENSE and NOTICE; USTAR, owner 0, one mtime, gzip
    header without a name or time."""
    files = {"package/package.json": (json.dumps(package_json(version), indent=2) + "\n").encode(),
             "package/juneau_terminal_playwright.cjs": REPORTER.read_bytes(),
             "package/LICENSE": (REPO / "LICENSE").read_bytes(), "package/NOTICE": (REPO / "NOTICE").read_bytes()}
    raw = io.BytesIO()
    with tarfile.open(fileobj=raw, mode="w", format=tarfile.USTAR_FORMAT) as tar:
        for name in sorted(files):
            info = tarfile.TarInfo(name)
            info.size, info.mtime, info.mode = len(files[name]), epoch, 0o644
            info.uid = info.gid = 0
            info.uname = info.gname = ""
            tar.addfile(info, io.BytesIO(files[name]))
    out = io.BytesIO()
    with gzip.GzipFile(filename="", mode="wb", fileobj=out, mtime=0, compresslevel=9) as gz:
        gz.write(raw.getvalue())
    _write(target, out.getvalue(), 0o644)
    return target


def build(version, epoch, out, only=KINDS):
    """Build the requested kinds into out; returns {kind: path}."""
    out = Path(out).resolve()
    out.mkdir(parents=True, exist_ok=True)
    pin = hatchling_pin()
    staged = stage(out / "dist-src", version, pin)
    built = {}
    python = backend_python(pin) if {"wheel", "sdist"} & set(only) else None
    for kind in KINDS:
        if kind not in only:
            continue
        if kind in ("wheel", "sdist"):
            built[kind] = build_backend(kind, staged, python, epoch, out)
            # Maven cannot compute the PEP 440 name, so the pom attaches a copy under a name it can spell.
            alias = out / "maven" / (f"juneau-terminal-{version}.whl" if kind == "wheel" else f"juneau-terminal-{version}-sdist.tar.gz")
            _write(alias, built[kind].read_bytes(), 0o644)
        elif kind == "pyz":
            built[kind] = write_pyz(staged, out / f"juneau-terminal-{version}.pyz", epoch)
        else:
            built[kind] = write_tgz(version, out / f"juneau-terminal-{version}-playwright.tgz", epoch)
    return built


def main(argv=None):
    ap = argparse.ArgumentParser(prog="build_dist.py", description="Build juneau-terminal's wheel, sdist, .pyz and npm tarball.")
    ap.add_argument("--version", required=True, help="the Maven project version, e.g. 10.0.0 or 10.0.0-SNAPSHOT")
    ap.add_argument("--epoch", required=True, help="timestamp for every entry: seconds, or ISO-8601 (project.build.outputTimestamp)")
    ap.add_argument("--out", required=True, help="output directory (target/dist)")
    ap.add_argument("--only", action="append", choices=KINDS, help="build only this kind (repeatable; default all)")
    args = ap.parse_args(argv)
    try:
        built = build(args.version, parse_epoch(args.epoch), args.out, tuple(args.only or KINDS))
    except BuildError as e:
        print(f"build_dist.py: {e}", file=sys.stderr)
        return e.code
    for kind, path in built.items():
        print(f"{kind}: {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
