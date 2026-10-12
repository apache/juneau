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
"""Terminal prompts that pause the console view, so the prompt isn't drawn over.  Plain input() and getpass() when
juneau_run isn't loaded."""

import contextlib
import getpass

from . import _base


def _ask(msg):
    """One line from the terminal, or None on EOF."""
    jr = _base.jr()
    try:
        return jr.ask(msg) if jr is not None else input(msg)
    except EOFError:
        return None


def text(msg, *, default=None):
    """The stripped answer.  An empty answer gives default; EOF gives None, whatever the default."""
    answer = _ask(msg)
    if answer is None:
        return None
    return answer.strip() or default


def confirm(msg, *, default=False):
    """True for y or yes (any case).  An empty answer or EOF gives default; anything else is False."""
    answer = _ask(msg)
    if answer is None or not answer.strip():
        return default
    return answer.strip().lower() in ("y", "yes")


def secret(msg):
    """getpass() with the view paused.  The answer is never published on juneau_run's bus or logged.  None on EOF."""
    jr = _base.jr()
    with jr.paused() if jr is not None else contextlib.nullcontext():
        try:
            return getpass.getpass(msg)
        except EOFError:
            return None
