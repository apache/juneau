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
"""Shared pieces: the Result type, the juneau_run lookup and the POSIX guard.  Private to the package."""

import os
import sys
from dataclasses import dataclass


@dataclass(frozen=True)
class Result:
    """Outcome of a git or tool helper.  summary is juneau_run's tests totals for a tool run; status is set by gates
    ("pass", "fail", "skip" or "error") and by Registry.run ("paused")."""

    ok: bool
    code: int
    out: str = ""
    err: str = ""
    ms: int | None = None
    summary: dict | None = None
    status: str | None = None


def jr():
    """The loaded juneau_run module, or None.  Looked up on every call: the toolkit never caches it."""
    return sys.modules.get("juneau_run")


def posix_only(name):
    """Raise NotImplementedError on Windows: lock and proc need fcntl and process groups."""
    if os.name == "nt":
        raise NotImplementedError(f"{name} is POSIX-only")


_title = [None]


def remember(title):
    """Record the title of the command about to run, for Session.require's default message."""
    _title[0] = title


def last_title():
    return _title[0]
