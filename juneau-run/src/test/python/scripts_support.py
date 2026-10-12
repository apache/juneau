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
"""Puts src/main/python on sys.path so the toolkit tests import juneau_scripts in place, and holds their shared helpers.
Importing support first loads juneau_run.py by path into sys.modules (support.py), as the other suites do."""

import json
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

from support import MODULE_ROOT, SCRIPT, isolate_run_env, jr, reset_run_state  # noqa: F401

PYTHON_SRC = MODULE_ROOT / "src" / "main" / "python"
PACKAGE = PYTHON_SRC / "juneau_scripts"
BOOTSTRAP = PYTHON_SRC / "bootstrap.py"
ASSEMBLY = MODULE_ROOT / "src" / "assembly" / "juneau-scripts.xml"
PREFIX = "##run "

if str(PYTHON_SRC) not in sys.path:
    sys.path.insert(0, str(PYTHON_SRC))

import juneau_scripts  # noqa: E402

# Imported in place, not through bootstrap.load(), so record the API the bootstrap would have set.
juneau_scripts.BOOTSTRAP_API = juneau_scripts.REQUIRED_BOOTSTRAP_API


def package_files():
    """Every .py under juneau_scripts/, relative to src/main/python, in zip layout."""
    return sorted(p.relative_to(PYTHON_SRC).as_posix() for p in PACKAGE.rglob("*.py") if "__pycache__" not in p.parts)


def build_zip(path, *, init=True):
    """The assembly's layout, built with zipfile: juneau_scripts/** plus bootstrap.py, LICENSE and NOTICE at the
    root.  init=False leaves out juneau_scripts/__init__.py."""
    with zipfile.ZipFile(path, "w") as z:
        for rel in package_files():
            if not init and rel == "juneau_scripts/__init__.py":
                continue
            z.write(PYTHON_SRC / rel, rel)
        z.write(BOOTSTRAP, "bootstrap.py")
        for name in ("LICENSE", "NOTICE"):
            z.write(MODULE_ROOT.parent / name, name)
    return Path(path)


def copy_package(dest):
    """A copy of src/main/python's juneau_scripts/ and juneau_run.py under dest, without __pycache__."""
    dest = Path(dest)
    dest.mkdir(parents=True, exist_ok=True)
    shutil.copytree(PACKAGE, dest / "juneau_scripts", ignore=shutil.ignore_patterns("__pycache__"))
    shutil.copy(SCRIPT, dest / "juneau_run.py")
    return dest


def python(args, *, cwd=None, env=None, input=None, bytecode=False, timeout=120):
    """Run sys.executable in a subprocess.  -B unless bytecode=True (the no-.pyc tests need writes to be possible)."""
    flags = [] if bytecode else ["-B"]
    return subprocess.run([sys.executable, *flags, *args], cwd=cwd, env=env, input=input, capture_output=True,
                          text=True, timeout=timeout)


def markers(out):
    """The ##run markers in out, as dicts, with ms removed."""
    found = []
    for line in out.splitlines():
        if line.startswith(PREFIX):
            obj = json.loads(line[len(PREFIX):])
            obj.pop("ms", None)
            found.append(obj)
    return found


def pycache_under(root):
    return [str(p) for p in Path(root).rglob("__pycache__")] + [str(p) for p in Path(root).rglob("*.pyc")]


def base_env(**extra):
    """os.environ without the run protocol variables, plus extra."""
    env = {k: v for k, v in os.environ.items()
           if k not in ("RUN_MARKERS", "RUN_ARTIFACTS", "JUNEAU_PUSH_ACTIVE", "JUNEAU_SCRIPTS_ZIP")
           and not k.startswith("JUNEAU_RUN_")}
    env.update(extra)
    return env


class FakeTty:
    """A stdout stand-in that says it is a terminal."""

    def __init__(self, tty=True):
        self.tty, self.out = tty, ""

    def isatty(self):
        return self.tty

    def write(self, text):
        self.out += text
        return len(text)

    def flush(self):
        pass
