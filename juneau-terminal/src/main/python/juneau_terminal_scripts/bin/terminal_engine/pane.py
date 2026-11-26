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
"""tmux-pane: the subcommands (copy, zoom, cancel, list, close, equalize) or, for anything else, a run."""

from . import panes


def main(argv):
    first = argv[0] if argv else ""
    rest = argv[1:]
    if first == "list":
        return panes.cmd_list()
    if first == "close":
        return panes.cmd_close(rest[0] if rest else "--kept")
    if first == "equalize":
        return panes.cmd_equalize(rest[0] if rest else "", closing=len(rest) > 1 and rest[1] == "--closing")
    if first == "copy":
        return panes.cmd_copy(rest)
    if first == "zoom":
        return panes.cmd_zoom(rest)
    if first == "cancel":
        from . import cancel
        return cancel.cmd_cancel(rest[0] if rest else "")
    from . import runner
    return runner.run(argv)
