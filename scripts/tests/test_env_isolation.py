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
"""A live run's JUNEAU_TERMINAL_* variables must not reach the tests, or their sessions write into the real run's logs."""

import os
import subprocess
import sys
from pathlib import Path

from conftest import LIVE_RUN_ENV

_CHECKED = "JUNEAU_TEST_ENV_ISOLATION_INNER"


def test_inner_sees_no_live_run_variables():
    """Runs only in the subprocess below: the suite's fixtures must have dropped every live-run variable."""
    if not os.environ.get(_CHECKED):
        return
    assert [n for n in LIVE_RUN_ENV if n in os.environ] == []


def test_live_run_variables_do_not_reach_tests(tmp_path):
    env = dict(os.environ)
    env[_CHECKED] = "1"
    live = tmp_path / "live"
    for name in LIVE_RUN_ENV:
        env[name] = str(live / (name + ".txt"))
    result = subprocess.run(
        [sys.executable, "-m", "pytest", "-q", "-p", "no:cacheprovider",
         f"{Path(__file__)}::test_inner_sees_no_live_run_variables"],
        env=env, capture_output=True, text=True, cwd=Path(__file__).parent)
    assert result.returncode == 0, result.stdout + result.stderr
    assert not live.exists()
