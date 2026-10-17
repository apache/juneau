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
"""juneau_run's pytest progress plugin: one `##pytest {json}` line per event, read by juneau_run.PytestParser.

Stdlib only and Python 3.8-compatible syntax: the child's interpreter may be older than the runner's, and a plugin that
fails to import makes pytest exit 4.  Progress is best-effort: a write that fails silences the plugin, never the run."""

import json
import os

PREFIX = "##pytest "
_STREAM_FD = None
try:
    _STREAM_FD = os.dup(1)      # -p plugins import before global capture replaces fd 1: this is the real stream
except OSError:
    pass
_dead = False
_total_sent = -1


def _worker(config):
    return bool(os.environ.get("PYTEST_XDIST_WORKER")) or hasattr(config, "workerinput")


def _emit(config, obj):
    global _dead
    if _dead:
        return
    try:
        line = PREFIX + json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
        tr = config.pluginmanager.get_plugin("terminalreporter")
        if tr is not None:
            cap = config.pluginmanager.get_plugin("capturemanager")
            if obj["ev"] in ("hello", "total") and cap is not None:
                try:
                    ctx = cap.global_and_fixture_disabled()
                except AttributeError:      # older pytest: no such context manager
                    ctx = None
                if ctx is not None:
                    with ctx:
                        _write(tr, line)
                else:
                    _write(tr, line)
            else:
                _write(tr, line)
        elif _STREAM_FD is not None:
            os.write(_STREAM_FD, (line + "\n").encode("utf-8", "replace"))
        else:
            _dead = True
    except Exception:
        _dead = True


def _write(tr, line):
    _line_start(tr)
    tr.write_line(line)
    try:
        tr._tw.flush()      # piped stdout is block-buffered: without this a marker surfaces a test late
    except Exception:
        pass


def _line_start(tr):
    """Make sure the marker starts a fresh line: -q and xdist progress characters leave the cursor mid-line, where
    ensure_newline (which only knows about the current file path) does nothing."""
    tr.ensure_newline()
    try:
        mid = tr._tw.width_of_current_line > 0
    except Exception:
        mid = False
    if mid:
        tr.write_line("")


def pytest_configure(config):
    global _total_sent, _dead
    _total_sent = -1
    _dead = False
    _durations.clear()
    _CONFIG[0] = None
    if config.pluginmanager.hasplugin("xdist"):
        config.pluginmanager.register(_XdistHooks(), "juneau_run_pytest_xdist")
    if not _worker(config):
        _emit(config, {"ev": "hello", "v": 1})


def pytest_collection_finish(session):
    if not _worker(session.config):
        _total(session.config, len(session.items), _file_counts(i.nodeid for i in session.items))


class _XdistHooks:
    """Registered only when xdist is loaded: pytest rejects a plain hook function for an unknown hook name."""

    def pytest_xdist_node_collection_finished(self, node, ids):
        _total(node.config, len(ids), _file_counts(ids))


def _file_counts(ids):
    """Tests per test file, keyed by the file part of the node id; the runner draws one bar cell per file."""
    counts = {}
    for nid in ids:
        f = nid.split("::", 1)[0][:400]
        counts[f] = counts.get(f, 0) + 1
    return counts


def _total(config, n, files=None):
    global _total_sent
    if n > _total_sent:             # once (even for 0), and again only for a larger count (spec 1.1)
        _total_sent = n
        mark = {"ev": "total", "n": n}
        if files:
            mark["f"] = files
        _emit(config, mark)


_durations = {}


def pytest_runtest_logreport(report):
    config = _CONFIG[0]
    if config is None or _worker(config):
        return
    if report.outcome not in ("passed", "failed", "skipped"):
        return      # e.g. "rerun" from pytest-rerunfailures: not a finished test
    outcome = _outcome(report)
    nid = report.nodeid[:400]
    if report.when in ("setup", "call"):
        _durations[nid] = _durations.get(nid, 0.0) + (report.duration or 0.0)
    finished = report.when == "call" or (report.when == "setup" and report.outcome != "passed")
    if finished:
        _emit(config, {"ev": "test", "o": outcome, "id": nid, "s": round(_durations.pop(nid, 0.0), 3)})
    elif report.when == "teardown" and report.outcome == "failed":
        _emit(config, {"ev": "test", "o": "error", "id": nid, "s": round(report.duration or 0.0, 3), "late": 1})


def _outcome(report):
    wasxfail = hasattr(report, "wasxfail")
    if report.outcome == "passed":
        return "xpassed" if wasxfail else "passed"
    if report.outcome == "skipped":
        return "xfailed" if wasxfail else "skipped"
    return "error" if report.when != "call" else "failed"


_CONFIG = [None]


def pytest_sessionstart(session):
    _CONFIG[0] = session.config
