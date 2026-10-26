#!/usr/bin/env bash
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
# Records the Bazel parser's replay fixtures from the tiny workspace in fixtures/parsers/bazel/workspace.
#
#   record-bazel-fixtures.sh <scratch-dir> <fixtures-dir>
#
# Needs bazelisk (as `bazel`) and network for the first download; USE_BAZEL_VERSION pins the release (default 7.4.1).
# Review each case with `python3 parser_replay.py print <case>`, then store its events with `... expect <case>`.
set -euo pipefail

scratch=$(cd "$1" && pwd -P)
fixtures=$(cd "$2" && pwd -P)
here=$(cd "$(dirname "$0")" && pwd -P)
export USE_BAZEL_VERSION=${USE_BAZEL_VERSION:-7.4.1}

ws="$scratch/ws"
root="$scratch/root"
rm -rf "$ws"
cp -R "$fixtures/workspace" "$ws"
chmod +x "$ws"/*/*.sh
BAZEL=(bazel "--output_user_root=$root")
trap 'cd "$ws" && "${BAZEL[@]}" shutdown >/dev/null 2>&1 || true' EXIT

# record <case> <want-exit> <command> <targets...>
record() {
    local case=$1 want=$2 command=$3
    shift 3
    local raw="$scratch/raw-$case"
    rm -rf "$raw" && mkdir -p "$raw"
    local code=0
    (cd "$ws" && "${BAZEL[@]}" "$command" "--build_event_json_file=$raw/bep.json" --color=no --curses=no "$@") \
        >"$raw/stdout.txt" 2>&1 || code=$?
    if [ "$code" != "$want" ]; then
        echo "record: $case exited $code, wanted $want" >&2
        cat "$raw/stdout.txt" >&2
        exit 1
    fi
    python3 -I "$here/scrub-bazel-fixture.py" "$raw" "$fixtures/$case" "$scratch" "$code" bazel "$command" "$@"
}

record build 0 build //ok:all
record pass 0 test //ok:all
record cached 0 test //ok:all
record fail 3 test //ok:all //fail:all
record build-failure 1 build //broken:all

# truncated: the pass recording as if Bazel died mid-write: buildFinished and everything after it are dropped (so a
# row is still open), and the last record kept is cut in half
rm -rf "$fixtures/truncated"
cp -R "$fixtures/pass" "$fixtures/truncated"
python3 -I - "$fixtures/truncated" <<'EOF'
import json, sys
from pathlib import Path
case = Path(sys.argv[1])
lines = (case / "side.log").read_bytes().splitlines()
kinds = [next(iter(json.loads(line)["id"])) for line in lines]
cut = kinds.index("buildFinished") if "buildFinished" in kinds else len(lines) // 2
kept = lines[:cut]
if len(kept) < 2:
    sys.exit("truncated: too few records before buildFinished")
kept[-1] = kept[-1][:len(kept[-1]) // 2]
(case / "side.log").write_bytes(b"\n".join(kept))
expected = json.loads((case / "expected.log").read_text())
expected.update(exit=1, status="fail")
(case / "expected.log").write_text(json.dumps(expected) + "\n")
EOF
echo "recorded into $fixtures"
