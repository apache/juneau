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
"""Test fixture standing in for scripts/release.py under JRM: writes a run.json and exits."""
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
    side = a.attempt_dir / "jrm-attempt.json"
    try:                                                   # same tolerance as the real release.py _origin
        side_data = json.loads(side.read_text())
    except (OSError, ValueError):
        side_data = {}
    origin = "ui" if isinstance(side_data, dict) and side_data.get("origin") == "ui" else "cli"
    run_json = a.home / "runs" / cfg.get("version", "9.2.1") / "run.json"

    def record(status, exit_code=None, pause=None):
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
        if pause:
            data["pause"] = {"step": pause.get("step", "fake-step"), "message": pause.get("message", "paused"),
                             "at": time.strftime("%Y%m%d-%H%M%S")}
        else:
            data.pop("pause", None)
        run_json.write_text(json.dumps(data))

    print(f"fake release.py {command}{' (dry run)' if a.dry_run else ''}", flush=True)
    if cfg.get("say"):
        print(cfg["say"], flush=True)
    record("running")
    try:
        if isinstance(cfg.get("env"), list):
            (a.attempt_dir / "fake-env.txt").write_text("\n".join(os.environ.get(n, "") for n in cfg["env"]))
        time.sleep(cfg.get("sleep", 0))
        if cfg.get("pause"):
            record("ok", 0, pause=cfg["pause"])
            return 0
    except KeyboardInterrupt:
        record("cancelled", 130)
        return 130
    code = cfg.get("exit", 0)
    record("ok" if code == 0 else "failed", code)
    return code


if __name__ == "__main__":
    sys.exit(main())
