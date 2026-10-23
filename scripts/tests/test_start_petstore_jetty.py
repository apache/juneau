# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
# ***************************************************************************************************************************
"""
Tests for scripts/start-petstore-jetty.py: the strict port check (a busy port exits non-zero before anything is built
or launched) and the plain-java launch command.  Nothing here runs a real mvn or java.
"""

from __future__ import annotations

import importlib.util
import socket
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture
def module():
    path = SCRIPTS_DIR / "start-petstore-jetty.py"
    spec = importlib.util.spec_from_file_location("_undertest_start_petstore_jetty", path)
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


@pytest.fixture
def busy_port():
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    s.listen(1)
    yield s.getsockname()[1]
    s.close()


def _free_port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def test_port_in_use_detects_listener(module, busy_port):
    assert module.port_in_use(busy_port)
    assert not module.port_in_use(_free_port())


def test_busy_port_exits_nonzero_with_message_before_building(module, busy_port, monkeypatch, capsys):
    def boom(*a, **k):
        raise AssertionError("nothing may be built or launched on a busy port")
    monkeypatch.setattr(module.subprocess, "run", boom)
    monkeypatch.setattr(module, "port_holder", lambda port: "PID 4242 (java)")
    monkeypatch.setattr(module.sys, "argv", ["start-petstore-jetty.py", "--port", str(busy_port)])
    with pytest.raises(SystemExit) as e:
        module.main()
    assert e.value.code != 0
    err = capsys.readouterr().err
    assert f"Port {busy_port} is already in use by PID 4242 (java)" in err


def test_busy_message_without_holder_names_port_and_lsof_hint(module):
    msg = module.port_busy_message(10000, None)
    assert "Port 10000 is already in use" in msg
    assert "lsof" in msg


def test_launch_command_uses_plain_java_with_classpath_and_port(module, monkeypatch):
    monkeypatch.delenv("JAVA_HOME", raising=False)
    cmd = module.launch_command("a.jar:b.jar", Path("/x/target/classes"), 10100)
    assert cmd[0] == "java"
    assert "-Dpetstore.port=10100" in cmd
    assert cmd[cmd.index("-cp") + 1] == "/x/target/classes:a.jar:b.jar"
    assert cmd[-1] == "org.apache.juneau.petstore.jetty.App"
    assert "exec:java" not in " ".join(cmd)


def test_launch_command_honours_java_home(module, monkeypatch):
    monkeypatch.setenv("JAVA_HOME", "/jdk")
    assert module.launch_command("cp", Path("c"), 1)[0] == "/jdk/bin/java"


def test_classpath_command_writes_output_file(module):
    cmd = module.classpath_command(Path("/t/cp.txt"))
    assert cmd[:3] == ["mvn", "-q", "dependency:build-classpath"]
    assert "-Dmdep.outputFile=/t/cp.txt" in cmd


def test_resolve_classpath_runs_mvn_once_then_uses_cache(module, tmp_path, monkeypatch):
    (tmp_path / "target").mkdir()
    (tmp_path / "pom.xml").write_text("<project/>")
    calls = []

    def fake_run(cmd, **kw):
        calls.append(cmd)
        (tmp_path / "target" / module.CLASSPATH_FILE).write_text("a.jar:b.jar\n")
        return type("R", (), {"returncode": 0})()
    monkeypatch.setattr(module.subprocess, "run", fake_run)
    assert module.resolve_classpath(tmp_path) == "a.jar:b.jar"
    assert module.resolve_classpath(tmp_path) == "a.jar:b.jar"
    assert len(calls) == 1


def test_resolve_classpath_failure_returns_none(module, tmp_path, monkeypatch):
    (tmp_path / "target").mkdir()
    (tmp_path / "pom.xml").write_text("<project/>")
    monkeypatch.setattr(module.subprocess, "run", lambda cmd, **kw: type("R", (), {"returncode": 1})())
    assert module.resolve_classpath(tmp_path) is None
