#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
The one loader push.py and test.py share: the in-tree juneau_terminal_scripts toolkit and juneau_terminal, from
juneau-terminal/src/main/python beside this directory.

    import _juneau
    jr, js = _juneau.load()

bootstrap.load does the work (in-tree mode, idempotent within a process, an already-loaded juneau_terminal reused).  It
leaves sys.dont_write_bytecode set, so nothing writes __pycache__ into the source tree the Maven build's RAT check
scans.
"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PYTHON_SRC = ROOT / "juneau-terminal" / "src" / "main" / "python"


def load():
    """(juneau_terminal, juneau_terminal_scripts).  Raises bootstrap.BootstrapError when juneau-terminal's sources are not there."""
    src = str(PYTHON_SRC)
    if src not in sys.path:
        sys.path.insert(0, src)
    sys.dont_write_bytecode = True      # before bootstrap itself is imported from the source tree
    import bootstrap
    return bootstrap.load(pom=str(ROOT / "pom.xml"), terminal=True)


def same_shape_runs(records):
    """The `tests` records whose command signature (`sig`) equals the newest `tests` record's, in run order.  A run
    is compared only with earlier runs of its own command shape, so a --no-container or wrapped run never skews the
    baseline.  Empty when there is no `tests` record."""
    tests = [r for r in records if isinstance(r, dict) and r.get("step") == "tests"]
    return [r for r in tests if r.get("sig") == tests[-1].get("sig")] if tests else []


def no_baseline_line(runs):
    """The line printed instead of a verdict when the newest command shape has too few runs to have a baseline."""
    return f"no baseline for this command shape ({runs} run{'' if runs == 1 else 's'})"
