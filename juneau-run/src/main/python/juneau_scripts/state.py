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
"""A JSON checkpoint file, so a release can resume where it stopped."""

import contextlib
import json
import os
import tempfile
from pathlib import Path


class CheckpointError(Exception):
    """The checkpoint file is corrupt or unreadable.  A release never resumes from a guess; the script decides
    whether to clear() it."""


class Checkpoint:
    def __init__(self, path):
        self.path = Path(path)

    def _load(self):
        try:
            text = self.path.read_text(encoding="utf-8")
        except FileNotFoundError:
            return {}
        except OSError as e:
            raise CheckpointError(f"cannot read checkpoint {self.path}: {e}") from e
        try:
            data = json.loads(text)
        except ValueError as e:
            raise CheckpointError(f"corrupt checkpoint {self.path}: {e}") from e
        if not isinstance(data, dict):
            raise CheckpointError(f"corrupt checkpoint {self.path}: not a JSON object")
        return data

    def get(self, key, default=None):
        return self._load().get(key, default)

    def set(self, key, value):
        """Write key=value atomically: a temporary file in the same directory, then os.replace."""
        data = self._load()
        data[key] = value
        self.path.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(prefix=f".{self.path.name}.", dir=self.path.parent)
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, sort_keys=True)
                f.write("\n")
            os.replace(tmp, self.path)
        except BaseException:
            with contextlib.suppress(OSError):
                os.unlink(tmp)
            raise

    def clear(self):
        with contextlib.suppress(FileNotFoundError):
            self.path.unlink()

    def last(self):
        """The last completed step's name, or None."""
        value = self.get("last")
        return value if isinstance(value, str) else None
