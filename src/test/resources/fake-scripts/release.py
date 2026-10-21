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
"""Test fixture standing in for scripts/release.py under JRM: speaks prompt protocol v1 and writes a run.json."""
import argparse
import json
import os
import signal
import sys
import time
from pathlib import Path


def _interrupt(signum, frame):
    raise KeyboardInterrupt


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--console")
    p.add_argument("--home", type=Path, required=True)
    p.add_argument("--attempt-dir", type=Path, required=True)
    p.add_argument("--dry-run", action="store_true")
    p.add_argument("--resume", action="store_true")
    p.add_argument("--revert", action="store_true")
    a = p.parse_args()
    signal.signal(signal.SIGTERM, _interrupt)
    cfg_path = a.home / "fake-script.json"
    cfg = json.loads(cfg_path.read_text()) if cfg_path.exists() else {}
    (a.attempt_dir / "fake-argv.json").write_text(json.dumps(sys.argv[1:]))
    command = "revert" if a.revert else "resume" if a.resume else "start"
    out_fd, in_fd = os.environ.pop("JUNEAU_PROMPT_FD_OUT", None), os.environ.pop("JUNEAU_PROMPT_FD_IN", None)
    origin = "ui" if out_fd else "cli"
    run_json = a.home / "runs" / cfg.get("version", "9.2.1") / "run.json"

    def record(status, exit_code=None):
        if a.dry_run:
            return
        run_json.parent.mkdir(parents=True, exist_ok=True)
        data = json.loads(run_json.read_text()) if run_json.exists() else {
            "schema": 1, "details": {"version": cfg.get("version", "9.2.1"), "rc": cfg.get("rc", 1)},
            "phase": cfg.get("phase", "rc"), "attempts": []}
        att = data["attempts"]
        if not att or att[-1]["dir"] != str(a.attempt_dir):
            att.append({"n": len(att) + 1, "dir": str(a.attempt_dir), "command": command, "origin": origin,
                        "pid": os.getpid(), "status": "running", "started": time.strftime("%Y%m%d-%H%M%S")})
        att[-1].update({"status": status, "step": "fake-step"})
        if exit_code is not None:
            att[-1]["exit"] = exit_code
        run_json.write_text(json.dumps(data))

    print(f"fake release.py {command}{' (dry run)' if a.dry_run else ''}", flush=True)
    if cfg.get("say"):
        print(cfg["say"], flush=True)
    record("running")
    try:
        if cfg.get("prompts"):
            w = os.fdopen(int(out_fd), "w", buffering=1)
            r = os.fdopen(int(in_fd), "r")
            w.write(json.dumps({"v": 1, "type": "hello", "pid": os.getpid(), "script": "release.py"}) + "\n")
            for n, pr in enumerate(cfg["prompts"], 1):
                w.write(json.dumps({"v": 1, "type": "prompt", "id": f"p{n}", **pr}) + "\n")
                ans = json.loads(r.readline() or "{}")
                with open(a.home / "fake-answers.jsonl", "a") as f:
                    f.write(json.dumps(ans) + "\n")
                shown = "********" if pr.get("kind") == "secret" else ans.get("value")
                print(f"? {pr.get('message')}: {shown}", flush=True)
        time.sleep(cfg.get("sleep", 0))
    except KeyboardInterrupt:
        record("cancelled", 130)
        return 130
    code = cfg.get("exit", 0)
    record("ok" if code == 0 else "failed", code)
    return code


if __name__ == "__main__":
    sys.exit(main())
