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
"""What the launcher prints when a run ends.  A split run in a new tmux session ends with the session itself, so the launcher also
appends what it prints to the file JUNEAU_TERMINAL_FINAL_FILE names, and mode.py prints that file in the user's own terminal
once `tmux attach` returns."""

import os
import sys

FINAL_FILE_ENV = "JUNEAU_TERMINAL_FINAL_FILE"


def emit(text="", end="\n"):
    """print(text), and the same text appended to the final file when there is one."""
    sys.stdout.write(text + end)
    sys.stdout.flush()
    path = os.environ.get(FINAL_FILE_ENV)
    if path:
        try:
            with open(path, "a") as f:
                f.write(text + end)
        except OSError:
            pass


def write_exit_code(rc):
    """Record the run's exit code beside the final file (<file>.exit), for a launcher that outlives the session."""
    path = os.environ.get(FINAL_FILE_ENV)
    if path:
        try:
            with open(path + ".exit", "w") as f:
                f.write("%d\n" % rc)
        except OSError:
            pass
