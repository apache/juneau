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

"""Turns one raw Bazel recording into a replay case: drops the record kinds the parser ignores and that carry
machine-specific data, replaces the scratch path, home directory, user and host, and writes stdout.log, side.log and
expected.log (events null until `parser_replay.py expect`).  Fails if any machine-specific text survives.

    python3 -I scrub-bazel-fixture.py <raw-dir> <case-dir> <scratch> <exit> <argv...>"""

import getpass
import json
import os
import socket
import sys
from pathlib import Path

DROP = {"workspaceStatus", "buildMetadata", "structuredCommandLine", "unstructuredCommandLine", "optionsParsed",
        "workspace", "workspaceInfo", "configuration", "fetch", "buildMetrics", "buildToolLogs",
        "convenienceSymlinksIdentified"}


def main(raw, case, scratch, exit_code, argv):
    raw, case = Path(raw), Path(case)
    pairs = [("/private" + scratch, "/work/bazel"), (scratch, "/work/bazel"), (str(Path.home()), "/home/user"),
             (getpass.getuser(), "user"), (socket.gethostname(), "host")]
    pairs = [(a, b) for a, b in pairs if a]

    def scrub(text):
        for a, b in pairs:
            text = text.replace(a, b)
        return text

    records = []
    for line in (raw / "bep.json").read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        obj = json.loads(line)
        if next(iter(obj["id"])) in DROP:
            continue
        if "started" in obj:
            obj["started"] = {k: v for k, v in obj["started"].items() if k in ("command", "buildToolVersion")}
        records.append(scrub(json.dumps(obj)))
    stdout = scrub((raw / "stdout.txt").read_text(encoding="utf-8", errors="replace"))
    for a, _ in pairs:
        for text in (stdout, *records):
            if a in text:
                sys.exit(f"scrub: {a!r} survives in {case.name}")
    case.mkdir(parents=True, exist_ok=True)
    (case / "side.log").write_text("".join(r + "\n" for r in records), encoding="utf-8")
    (case / "stdout.log").write_text(stdout, encoding="utf-8")
    exit_code = int(exit_code)
    (case / "expected.log").write_text(json.dumps({"argv": argv, "exit": exit_code,
                                                   "status": "ok" if exit_code == 0 else "fail", "events": None}) + "\n",
                                       encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) < 6:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], os.path.realpath(sys.argv[3]).removeprefix("/private"), sys.argv[4], sys.argv[5:])
