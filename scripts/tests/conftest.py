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
"""Shared pytest setup for the docs script tests."""

import importlib.util
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))


def load_script(filename):
    """Import a hyphenated script (e.g. build-docs.py) as a module."""
    spec = importlib.util.spec_from_file_location(filename.replace('-', '_').removesuffix('.py'),
                                                  SCRIPTS_DIR / filename)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module  # dataclasses look up their defining module here
    spec.loader.exec_module(module)
    return module


# Every JUNEAU_RUN_* variable that points a session at a live parent run (its logs, cancel command, markers, result,
# header and pending/overall rows, or the fact that one is active).  JUNEAU_RUN_CONSOLE, _DETAIL and _HISTORY* are
# settings, not pointers, and are handled elsewhere.
LIVE_RUN_ENV = (
    "JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_SESSION", "JUNEAU_RUN_LIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_RUN_LABEL_WIDTH",
    "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_BOARD_LOG", "JUNEAU_RUN_CANCEL", "JUNEAU_RUN_HEADER",
    "JUNEAU_RUN_PENDING", "JUNEAU_RUN_OVERALL", "JUNEAU_RUN_HANDBACK",
    "JUNEAU_RUN_MARKERS_FILE", "JUNEAU_RUN_RESULT", "JUNEAU_RUN_RESULT_PARTS",
)


@pytest.fixture(autouse=True)
def _no_outer_run_files(monkeypatch):
    """A run under watch-run.sh or push.py exports its live-run variables (see LIVE_RUN_ENV) to this suite; drop them so
    the tests see the same run state as under a plain run (and don't write the outer run's logs or files)."""
    for name in LIVE_RUN_ENV:
        monkeypatch.delenv(name, raising=False)
