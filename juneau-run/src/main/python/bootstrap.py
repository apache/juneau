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
"""
Load the juneau_scripts toolkit (and, when asked, juneau_run).  The one file a consumer vendors: copy it from the
installed zip with

    unzip -p ~/.m2/repository/org/apache/juneau/juneau-run/<v>/juneau-run-<v>.zip bootstrap.py > scripts/bootstrap.py

    from bootstrap import load
    jr, js = load(pom="pom.xml")

Where the toolkit comes from, in order:
  1. JUNEAU_SCRIPTS_ZIP (a zip, or a directory holding juneau_scripts/), and JUNEAU_RUN_PY.  With only JUNEAU_RUN_PY,
     the toolkit is <stem>.zip beside it, or a juneau_scripts/ directory beside it.
  2. In-tree: juneau-run/src/main/python/ under the pom's directory (Juneau itself).
  3. ~/.m2/repository/org/apache/juneau/juneau-run/<v>/juneau-run-<v>.{py,zip}, in place when both are there;
     otherwise `mvn dependency:copy` of both into cache_dir (a SNAPSHOT once per process, a release when missing).

juneau_run is loaded only when run=True, or when run is None and RUN_MARKERS=1, JUNEAU_RUN_MARKERS_FILE,
JUNEAU_RUN_RESULT or JUNEAU_RUN_CONSOLE=board or none.
"""

import hashlib
import importlib
import importlib.util
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path

BOOTSTRAP_API = 1
_REMEDY = "run `mvn install` in apache/juneau's juneau-run, or set JUNEAU_SCRIPTS_ZIP"
_located = None     # (toolkit source, juneau_run.py path or None), once per process
_copied = set()


class BootstrapError(Exception):
    """The toolkit can't be found or imported.  The message names what was expected and the remedy."""


def run_requested():
    """True when the environment asks for juneau_run: RUN_MARKERS=1, JUNEAU_RUN_MARKERS_FILE, JUNEAU_RUN_RESULT or
    JUNEAU_RUN_CONSOLE=board or none.  Under none juneau_run is loaded so a tool's output goes to the sinks and not
    the terminal."""
    env = os.environ
    return (env.get("RUN_MARKERS") == "1" or bool(env.get("JUNEAU_RUN_MARKERS_FILE"))
            or bool(env.get("JUNEAU_RUN_RESULT")) or env.get("JUNEAU_RUN_CONSOLE") in ("board", "none"))


def _pom_version(pom):
    try:
        text = Path(pom).read_text(encoding="utf-8")
    except OSError as e:
        raise BootstrapError(f"cannot read {pom}: {e}") from e
    m = re.search(r"<juneau\.version>\s*([^<\s]+)\s*</juneau\.version>", text)
    if not m:
        raise BootstrapError(f"no <juneau.version> in {pom}; pass version= or set JUNEAU_SCRIPTS_ZIP")
    return m.group(1)


def _beside(py):
    py = Path(py)
    zip_path = py.with_suffix(".zip")
    if zip_path.exists():
        return zip_path
    if (py.parent / "juneau_scripts").is_dir():
        return py.parent
    raise BootstrapError(f"juneau_scripts not found beside JUNEAU_RUN_PY: expected {zip_path}; {_REMEDY}")


def _copy(version, kind, cache):
    target = cache / f"juneau-run.{kind}"
    if (version, kind, str(cache)) in _copied or (not version.endswith("-SNAPSHOT") and target.exists()):
        return target
    coordinate = f"org.apache.juneau:juneau-run:{version}:{kind}"
    try:
        done = subprocess.run(["mvn", "-q", "-nsu", "dependency:copy", f"-Dartifact={coordinate}",
                               f"-DoutputDirectory={cache}", "-Dmdep.stripVersion=true"],
                              stdout=subprocess.DEVNULL, check=False)
    except OSError as e:
        raise BootstrapError(f"cannot run mvn to fetch {coordinate}: {e}; {_REMEDY}") from e
    if done.returncode != 0 or not target.exists():
        raise BootstrapError(f"could not fetch {coordinate} (mvn exit code {done.returncode}); {_REMEDY}")
    _copied.add((version, kind, str(cache)))
    return target


def _env_source(tree):
    """(source, py) from JUNEAU_SCRIPTS_ZIP / JUNEAU_RUN_PY as _locate treats them, or None when neither is set."""
    zip_env, py_env = os.environ.get("JUNEAU_SCRIPTS_ZIP"), os.environ.get("JUNEAU_RUN_PY")
    # Absolute, so a script's later os.chdir cannot break lazy imports from the zip on sys.path.
    zip_env = str(Path(zip_env).expanduser().resolve()) if zip_env else zip_env
    py_env = str(Path(py_env).expanduser().resolve()) if py_env else py_env
    if zip_env:
        if not Path(zip_env).exists():
            raise BootstrapError(f"JUNEAU_SCRIPTS_ZIP={zip_env} does not exist")
        in_tree = tree / "juneau_run.py"
        return Path(zip_env), py_env or (str(in_tree) if in_tree.exists() else None)
    if py_env:
        return _beside(py_env), py_env
    return None


def _locate(pom, version, cache_dir):
    tree = Path(pom).resolve().parent / "juneau-run" / "src" / "main" / "python"
    env = _env_source(tree)
    if env:
        return env
    if (tree / "juneau_scripts").is_dir():
        return tree, str(tree / "juneau_run.py")
    version = version or _pom_version(pom)
    m2 = Path.home() / ".m2" / "repository" / "org" / "apache" / "juneau" / "juneau-run" / version
    py, zip_path = m2 / f"juneau-run-{version}.py", m2 / f"juneau-run-{version}.zip"
    if py.exists() and zip_path.exists():
        return zip_path, str(py)
    cache = Path(cache_dir).resolve()
    _copy(version, "py", cache)
    return _copy(version, "zip", cache), str(cache / "juneau-run.py")


def _import(source):
    if Path(source).is_file():
        # Checked here, not left to zipimport, whose behaviour differs by version.  Before 3.14, a zip with no
        # __init__.py fails with a bare "No module named" unless it has directory entries, and then imports as a
        # namespace package.  From 3.14 it always imports as a namespace package.
        try:
            with zipfile.ZipFile(source) as z:
                has_init = "juneau_scripts/__init__.py" in z.namelist()
        except (OSError, zipfile.BadZipFile) as e:
            raise BootstrapError(f"cannot read {source}: {e}; {_REMEDY}") from e
        if not has_init:
            raise BootstrapError(f"{source} has no juneau_scripts/__init__.py, so it isn't the toolkit; {_REMEDY}")
    entry = str(source)
    if entry not in sys.path:
        sys.path.insert(0, entry)
    if Path(source).is_dir():
        # Left set, not restored: the toolkit imports some modules lazily, and none may write .pyc into a source
        # tree (RAT).  A zip never gets bytecode written into it.
        sys.dont_write_bytecode = True
    try:
        js = importlib.import_module("juneau_scripts")
    except ImportError as e:
        raise BootstrapError(f"cannot import juneau_scripts from {source}: {e}; {_REMEDY}") from e
    if getattr(js, "__file__", None) is None:
        # A directory source without __init__.py imports as a namespace package rather than failing.
        sys.modules.pop("juneau_scripts", None)
        raise BootstrapError(f"{source} has no juneau_scripts/__init__.py, so it isn't the toolkit; {_REMEDY}")
    js.BOOTSTRAP_API = BOOTSTRAP_API
    js.check_bootstrap()
    return js


def _load_run(path, forced):
    existing = sys.modules.get("juneau_run")
    if existing is not None:
        return existing
    if path is None or not Path(path).exists():
        problem = f"juneau_run.py not found{f' at {path}' if path else ''}"
        if forced:
            raise BootstrapError(f"{problem}; {_REMEDY}")
        print(f"warning: juneau-run unavailable ({problem}); running commands without it", file=sys.stderr)
        return None
    spec = importlib.util.spec_from_file_location("juneau_run", path)
    module = importlib.util.module_from_spec(spec)
    sys.modules["juneau_run"] = module
    try:
        spec.loader.exec_module(module)
    except BaseException:
        sys.modules.pop("juneau_run", None)
        raise
    return module


def load(pom="pom.xml", version=None, *, run=None, cache_dir="target/juneau-run"):
    """(juneau_run module or None, juneau_scripts package).  Idempotent within a process: the toolkit is located,
    copied and imported once; only the juneau_run decision is made again on each call."""
    global _located
    if _located is None:
        _located = _locate(pom, version, cache_dir)
    source, run_py = _located
    js = _import(source)
    wanted = run if run is not None else run_requested()
    return (_load_run(run_py, forced=run is True) if wanted else None), js


BIN_DIR = "juneau_scripts/bin"
_TEMP_TTL = 3600     # seconds a leftover dot-prefixed temp file survives before the next extract removes it


def _extract_bin(zip_path, dest):
    """Extract every juneau_scripts/bin/ file into dest, replacing each atomically; returns the names."""
    dest.mkdir(parents=True, exist_ok=True)
    wanted = {}
    with zipfile.ZipFile(zip_path) as z:
        for info in z.infolist():
            if info.is_dir() or not info.filename.startswith(BIN_DIR + "/"):
                continue
            wanted[Path(info.filename).name] = z.read(info)
    if not wanted:
        raise BootstrapError(f"{zip_path} has no {BIN_DIR}/ (an older toolkit); {_REMEDY}")
    for name, data in wanted.items():
        target = dest / name
        if target.is_file() and target.read_bytes() == data and os.access(target, os.X_OK):
            continue
        fd, tmp = tempfile.mkstemp(dir=str(dest), prefix="." + name + ".")
        try:
            with os.fdopen(fd, "wb") as f:
                f.write(data)
            os.chmod(tmp, 0o755)
            os.replace(tmp, target)      # atomic; a running bash keeps reading the inode it opened
        except BaseException:
            try:
                os.unlink(tmp)
            except OSError:
                pass
            raise
    now = time.time()
    for old in dest.iterdir():
        if not old.is_file():
            continue
        if old.name.startswith("."):      # a crashed extract's temp file; leave a concurrent caller's fresh one
            if now - old.stat().st_mtime > _TEMP_TTL:
                old.unlink()
        elif old.name not in wanted:
            old.unlink()
    return sorted(wanted)


def _bin_version(source, version):
    """(version string, sha-or-None); the zip is hashed at most once and only for the fallback name."""
    if version:
        return str(version), None
    m = re.fullmatch(r"juneau-run-(.+)\.zip", Path(str(source)).name)
    if m:
        return m.group(1), None
    digest = hashlib.sha256(Path(str(source)).read_bytes()).hexdigest()
    return "sha-" + digest[:12], digest


def bin_path(name, pom="pom.xml", version=None, cache_dir="target/juneau-run", *, bin_root=None, source=None):
    """Path of a juneau_scripts/bin/ script: in place for a directory source, extracted from a zip otherwise."""
    global _located
    if source is None:
        if _located is None:
            _located = _locate(pom, version, cache_dir)
        source = _located[0]
    root = Path(bin_root).expanduser() if bin_root else Path.home() / ".cache" / "juneau-run" / "bin"
    src = Path(str(source))
    if src.is_dir():
        d = src / BIN_DIR
        shipped = sorted(f.name for f in d.glob("*.sh")) if d.is_dir() else []
        if name not in shipped:
            raise BootstrapError(f"no script {name!r} in {d}; shipped: {', '.join(shipped) or '(none)'}")
        f = d / name
        if not os.access(f, os.X_OK):
            raise BootstrapError(f"{f} is not executable (a packaging bug)")
        return f
    ver, digest = _bin_version(src, version)
    dest = root / ver
    shipped = _extract_bin(src, dest)
    if digest:                              # sha fallback: prune earlier sibling sha-* directories
        for sib in root.glob("sha-*"):
            if sib.is_dir() and sib != dest:
                shutil.rmtree(sib, ignore_errors=True)
    if name not in shipped:
        raise BootstrapError(f"no script {name!r} in {src}; shipped: {', '.join(shipped)}")
    return dest / name


def _cli_source(pom, cache_dir, version):
    """Where the CLI looks, in order, never running mvn: env, a zip it was imported from, beside bootstrap, in-tree,
    the ~/.m2 zip, the cached zip."""
    tree = Path(pom).resolve().parent / "juneau-run" / "src" / "main" / "python"
    env = _env_source(tree)
    if env:
        return env[0]
    archive = getattr(globals().get("__loader__"), "archive", None)    # set when bootstrap is imported from a zip
    if archive:
        return Path(archive)
    here = Path(__file__).resolve().parent
    if (here / "juneau_scripts").is_dir():
        return here
    if (tree / "juneau_scripts").is_dir():
        return tree
    ver = version or _pom_version(pom)
    m2 = Path.home() / ".m2" / "repository" / "org" / "apache" / "juneau" / "juneau-run" / ver / f"juneau-run-{ver}.zip"
    if m2.is_file():
        return m2
    cached = Path(cache_dir).expanduser().resolve() / "juneau-run.zip"
    if cached.is_file():
        return cached
    raise BootstrapError(f"no juneau-run toolkit found; {_REMEDY}, or run the repo's scripts/push.py once")


def main(argv=None):
    import argparse
    ap = argparse.ArgumentParser(prog="bootstrap.py", description="Print the path of a juneau-run bin script.")
    ap.add_argument("--bin", metavar="NAME", help="script name, e.g. watch-run.sh")
    ap.add_argument("--pom", help="pom with <juneau.version> (default: the nearest pom.xml above bootstrap.py)")
    ap.add_argument("--cache-dir", help="where mvn dependency:copy cached the zip (default: target/juneau-run beside the pom)")
    ap.add_argument("--version", help="juneau-run version (default: <juneau.version> of the pom)")
    args = ap.parse_args(argv)
    if not args.bin:
        ap.print_usage(sys.stderr)
        return 2
    here = Path(__file__).resolve().parent
    pom = args.pom or str(next((d / "pom.xml" for d in (here, *here.parents) if (d / "pom.xml").is_file()), Path("pom.xml")))
    cache = args.cache_dir or str(Path(pom).resolve().parent / "target" / "juneau-run")
    try:
        print(bin_path(args.bin, source=_cli_source(pom, cache, args.version), version=args.version))
    except BootstrapError as e:
        print(e, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
