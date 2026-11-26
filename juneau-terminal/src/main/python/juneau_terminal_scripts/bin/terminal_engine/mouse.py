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
"""SGR mouse reports (`ESC [ < b ; x ; y M` press, `m` release), as the kept-pane prompt and the toolbar read them."""

import re

_NUM = re.compile(r"[0-9]+\Z")


def left_press(seq, need_y=True):
    """seq is the bytes after the ESC of a report.  Returns (column, row) of a left-button press, else None.  The kept
    prompt requires a numeric row too; the toolbar (need_y=False) only needs the column."""
    if not seq.startswith("[<") or seq[-1:] != "M":
        return None
    parts = seq[2:-1].split(";")
    b = parts[0]
    x = parts[1] if len(parts) > 1 else ""
    y = ";".join(parts[2:])
    if b != "0" or not _NUM.match(x):
        return None
    if _NUM.match(y):
        return int(x), int(y)
    return None if need_y else (int(x), 0)
