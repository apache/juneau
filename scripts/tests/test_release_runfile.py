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
import json

import pytest

from jrm_release.runfile import RunFile, RunFileError, new_attempt_dir

pytestmark = pytest.mark.usefixtures("clean_env")


def test_attempt_running_then_outcome(home):
    rf = RunFile(home / "runs" / "9.2.1" / "run.json")
    d = new_attempt_dir(home)
    rf.attempt_start(d)
    assert rf.get("attempts")[-1]["status"] == "running"
    rf.attempt_end("failed")
    a = rf.get("attempts")[-1]
    assert a["status"] == "failed" and a["dir"] == str(d)
    assert json.loads(rf.path.read_text())["attempts"][-1]["status"] == "failed"


def test_attempt_dir_numbering_race(home, monkeypatch):
    stamp = "20261019-120000"
    monkeypatch.setattr("jrm_release.runfile.stamp", lambda now=None: stamp)
    (home / "attempts").mkdir(parents=True)
    (home / "attempts" / f"{stamp}-001").mkdir()      # a concurrent run already took 001
    d = new_attempt_dir(home)
    assert d.name == f"{stamp}-002" and d.is_dir()


def test_corrupt_run_json_fails_never_guesses(home):
    p = home / "runs" / "9.2.1" / "run.json"
    p.parent.mkdir(parents=True)
    p.write_text("{not json")
    with pytest.raises(RunFileError) as e:
        RunFile(p)
    assert str(p) in str(e.value)
    assert p.read_text() == "{not json"    # left alone for inspection


def test_attempt_records_pid_origin_step_exit(home):
    rf = RunFile(home / "runs" / "9.2.1" / "run.json")
    d = new_attempt_dir(home)
    rf.attempt_start(d, "start", origin="ui", pid=4242)
    a = rf.get("attempts")[-1]
    assert (a["status"], a["origin"], a["pid"], a["command"]) == ("running", "ui", 4242, "start")
    rf.attempt_end("paused", step="vote-gate", exit=0)
    a = json.loads(rf.path.read_text())["attempts"][-1]
    assert (a["status"], a["step"], a["exit"], a["pid"]) == ("paused", "vote-gate", 0, 4242)


def test_attempt_defaults_to_cli_and_this_pid(home):
    import os
    rf = RunFile(home / "runs" / "9.2.1" / "run.json")
    rf.attempt_start(new_attempt_dir(home), "resume")
    a = rf.get("attempts")[-1]
    assert a["origin"] == "cli" and a["pid"] == os.getpid()
