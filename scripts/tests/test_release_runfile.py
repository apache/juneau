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
