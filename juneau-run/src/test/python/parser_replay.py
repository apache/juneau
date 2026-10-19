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

"""Replays a recorded parser case: writes side.log into the parser's side file and feeds stdout.log to it in chunks,
polling after each, then finishes and closes it, and returns its events as JSON-shaped lists.

    python3 parser_replay.py print  <case-dir>    one event per line, to review a recording
    python3 parser_replay.py expect <case-dir>    store those events in the case's expected.log

A case directory is fixtures/parsers/<tool>/<case>/ with stdout.log, side.log and expected.log (single-line JSON:
argv, exit, status, events)."""

import json
import sys
import tempfile
from pathlib import Path


class FakeClock:
    """A monotonic clock that advances step seconds per call, so durations in parser events are exact."""

    def __init__(self, step=0.5):
        self.t = 0.0
        self.step = step

    def __call__(self):
        self.t += self.step
        return self.t


def load(case_dir):
    """(expected dict, stdout text, side bytes) for a case."""
    case_dir = Path(case_dir)
    expected = json.loads((case_dir / "expected.log").read_text(encoding="utf-8"))
    stdout = (case_dir / "stdout.log").read_text(encoding="utf-8")
    side = (case_dir / "side.log").read_bytes()
    return expected, stdout, side


def _slices(n, parts):
    return [(n * i // parts, n * (i + 1) // parts) for i in range(parts)]


def replay(parser, case_dir, *, side_path, step_id="t", interactive=False, chunks=3):
    """(events, prepared command).  side_path must be where the parser reads its side file."""
    expected, stdout, side = load(case_dir)
    side_path = Path(side_path)
    side_path.write_bytes(b"")
    parser.step_id = step_id
    parser.interactive = interactive
    cmd, _ = parser.prepare(list(expected["argv"]), {}, str(case_dir))
    lines = stdout.splitlines()
    events = []
    for (a, b), (c, d) in zip(_slices(len(side), chunks), _slices(len(lines), chunks)):
        with open(side_path, "ab") as f:
            f.write(side[a:b])
        for line in lines[c:d]:
            events += parser.on_line(line)
        events += parser.poll()
    events += parser.finish(expected["exit"])
    parser.close()
    return json.loads(json.dumps(events)), cmd


def make_parser(tool, side_path):
    """The registered parser tool, bound to juneau_run, reading side_path, on a FakeClock."""
    import scripts_support  # noqa: F401  (puts src/main/python on sys.path)
    from support import jr
    from juneau_scripts import parsers
    parsers.bind(jr)
    return parsers.load(tool)(side_path=side_path, clock=FakeClock())


FAKE_BAZEL = r'''#!PYTHON
import json, os, sys, time
open(os.environ["FAKE_BAZEL_ARGV"], "w").write(json.dumps(sys.argv[1:]))
path = next(a.split("=", 1)[1] for a in sys.argv if a.startswith("--build_event_json_file="))
def put(record):
    with open(path, "a") as f:
        f.write(json.dumps(record) + "\n")
put({"id": {"started": {}}, "started": {"command": "build"}})
put({"id": {"progress": {}}, "progress": {"stderr": "INFO: Analyzed 1 target.\n"}})
put({"id": {"targetConfigured": {"label": "//a:a"}}, "configured": {"targetKind": "genrule rule"}})
time.sleep(float(os.environ.get("FAKE_BAZEL_SLEEP", "0")))
put({"id": {"targetCompleted": {"label": "//a:a"}}, "completed": {"success": True}})
code = int(os.environ.get("FAKE_BAZEL_EXIT", "0"))
put({"id": {"buildFinished": {}}, "finished": {"exitCode": {"code": code} if code else {}}})
print("INFO: Build completed")
sys.exit(code)
'''


def write_fake_bazel(bin_dir):
    """A `bazel` in bin_dir that records its argv to $FAKE_BAZEL_ARGV and writes one target's build events."""
    bin_dir = Path(bin_dir)
    bin_dir.mkdir(parents=True, exist_ok=True)
    path = bin_dir / "bazel"
    path.write_text(FAKE_BAZEL.replace("PYTHON", sys.executable, 1), encoding="utf-8")
    path.chmod(0o755)
    return path


def main(argv):
    if len(argv) != 2 or argv[0] not in ("print", "expect"):
        sys.exit("usage: parser_replay.py print|expect <case-dir>")
    mode, case = argv[0], Path(argv[1])
    with tempfile.TemporaryDirectory() as tmp:
        side = Path(tmp) / "side.json"
        events, _ = replay(make_parser(case.parent.name, side), case, side_path=side)
    if mode == "print":
        for event in events:
            print(json.dumps(event))
        return
    expected = json.loads((case / "expected.log").read_text(encoding="utf-8"))
    expected["events"] = events
    (case / "expected.log").write_text(json.dumps(expected) + "\n", encoding="utf-8")


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    main(sys.argv[1:])
