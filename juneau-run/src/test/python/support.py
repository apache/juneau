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
"""Loads juneau_run.py the way an adopter does (by path), so tests exercise the shipped file and no installed copy."""

import importlib.util
import sys
from pathlib import Path

MODULE_ROOT = Path(__file__).resolve().parents[3]
SCRIPT = MODULE_ROOT / "src" / "main" / "python" / "juneau_run.py"
FIXTURES = Path(__file__).resolve().parent / "fixtures"
DOCS = MODULE_ROOT / "docs"

_spec = importlib.util.spec_from_file_location("juneau_run", SCRIPT)
jr = importlib.util.module_from_spec(_spec)
sys.modules["juneau_run"] = jr
_spec.loader.exec_module(jr)


def reset_run_state():
    """Forget run ownership between tests (the module keeps it in a global and in os.environ)."""
    import os
    jr._owns_run = False
    os.environ.pop(jr.ACTIVE_ENV, None)
