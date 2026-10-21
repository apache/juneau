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
import dataclasses
import os
import shutil
import sys
import tempfile
from argparse import Namespace
from pathlib import Path

import pytest

SCRIPTS = Path(__file__).resolve().parent.parent
if str(SCRIPTS) not in sys.path:
    sys.path.append(str(SCRIPTS))

from jrm_release.ctx import Ctx  # noqa: E402
from jrm_release.runfile import RunFile  # noqa: E402
from jrm_release.runner import Outcome, Runner  # noqa: E402
from jrm_release.svn import Svn  # noqa: E402


@pytest.fixture(scope="session", autouse=True)
def _no_real_run_history():
    """Keep the suite out of ~/.cache/juneau-run/history: main()-calling tests would otherwise record their stub
    steps beside the real push and release records.  No fixture may strip these two variables."""
    tmp = tempfile.mkdtemp(prefix="juneau-run-test-history-")
    names = ("JUNEAU_RUN_HISTORY", "JUNEAU_RUN_HISTORY_DIR")
    saved = {n: os.environ.get(n) for n in names}
    os.environ["JUNEAU_RUN_HISTORY"] = "0"
    os.environ["JUNEAU_RUN_HISTORY_DIR"] = tmp
    try:
        yield
    finally:
        for n, v in saved.items():
            if v is None:
                os.environ.pop(n, None)
            else:
                os.environ[n] = v
        shutil.rmtree(tmp, ignore_errors=True)


class PauseExc(Exception):
    def __init__(self, message, *, done=True):
        super().__init__(message)
        self.message, self.done = message, done


class FakeRunner:
    """Stands in for juneau_scripts.run: records run.cmd calls, answers them from scripted rules."""

    def __init__(self):
        self.calls, self.rules, self.said = [], [], []

    def script(self, *prefix, out="", ok=True):
        self.rules.append((prefix, out, ok))

    def cmd(self, cmd, *, title="", id=None, n=None, tool=None, cwd=None, env=None, capture=False, tty=False,
            summarize=None, input=None):
        rec = dict(argv=[str(c) for c in cmd], title=title, tool=tool, cwd=cwd, env=dict(env or {}), input=input,
                   tty=tty, capture=capture)
        self.calls.append(rec)
        for prefix, out, ok in reversed(self.rules):
            if tuple(rec["argv"][:len(prefix)]) == prefix:
                return Outcome(ok, 0 if ok else 1, out)
        return Outcome(True, 0, self.DEFAULT_VERSIONS.get(rec["argv"][-1], "") if len(rec["argv"]) <= 3 else "")

    # What an unscripted `<tool> --version` prints, so prerequisites (which fail on unreadable output) pass by default.
    DEFAULT_VERSIONS = {"-version": 'openjdk version "17.0.9" 2023-10-17\n', "--version": "9.9.9\n",
                        "--quiet": "1.14.3\n"}

    def quiet_cmd(self, cmd, *, title="", cwd=None):
        """Runner.quiet's hook: answered like cmd, but flagged as kept out of the log."""
        out = self.cmd(cmd, title=title, cwd=cwd, capture=True)
        self.calls[-1]["quiet"] = True
        return out

    def say(self, text="", level="info"):
        self.said.append(str(text))

    def argvs(self):
        return [c["argv"] for c in self.calls]

    def ran(self, *prefix):
        return [a for a in self.argvs() if tuple(a[:len(prefix)]) == prefix]


@dataclasses.dataclass(frozen=True)
class FakeField:
    name: str
    label: str
    default: object = None
    required: bool = False
    secret: bool = False
    multiline: bool = False
    type: str = "text"


class FakePrompts:
    """answers: key -> value, or list of values (consumed in order; the last repeats).  An unexpected key raises."""
    Field = FakeField

    def __init__(self, answers=None):
        self.answers, self.asked = dict(answers or {}), []

    def _next(self, kind, key, msg, **kw):
        self.asked.append((kind, key, msg, kw))
        if key not in self.answers:
            raise AssertionError(f"unexpected prompt {kind} key={key!r}: {msg}")
        v = self.answers[key]
        if isinstance(v, list):
            return v.pop(0) if len(v) > 1 else v[0]
        return v

    def text(self, msg, *, default=None, key=None, title=None, error=None):
        return self._next("text", key, msg, default=default)

    def secret(self, msg, *, key=None, title=None):
        return self._next("secret", key, msg)

    def confirm(self, msg, *, default=False, key=None, title=None, expect=None, danger=False):
        return self._next("confirm", key, msg, expect=expect, danger=danger)

    def choice(self, msg, options, *, default=None, key=None, title=None):
        return self._next("choice", key, msg, options=options)

    def form(self, msg, fields, *, key=None, title=None):
        got = self._next("form", key, msg, fields=fields)
        if got is None:
            return None
        return {**{f.name: f.default for f in fields}, **got}

    def email(self, *, to, subject, body, cc=(), key=None, title=None):
        return self._next("email", key, subject, to=to, body=body, cc=cc)


class FakeHttp:
    """A urllib opener: opener(request) -> (status, bytes).  routes: (method, url-suffix) -> (status, body)."""

    def __init__(self, routes=None):
        self.routes, self.requests = dict(routes or {}), []

    def __call__(self, request):
        self.requests.append((request.get_method(), request.full_url, request.data, dict(request.headers)))
        for (method, suffix), resp in self.routes.items():
            if request.get_method() == method and request.full_url.endswith(suffix):
                return resp
        return 404, b""


class FakeNexus:
    def __init__(self):
        self.repos, self.repos_after, self.states, self.posts, self.files = [], [], {}, [], {}
        self._listed = 0

    def repositories(self):
        self._listed += 1
        return list(self.repos) if self._listed == 1 else list(self.repos) + list(self.repos_after)

    def repo(self, rid):
        seq = self.states.get(rid, [{"state": "open"}])
        return dict(seq.pop(0) if len(seq) > 1 else seq[0])

    def close(self, rid):
        self.posts.append(("close", rid))

    def drop(self, rid):
        self.posts.append(("drop", rid))
        self.states[rid] = [{"state": "missing"}]

    def promote(self, rid):
        self.posts.append(("promote", rid))

    def walk(self, rid):
        return dict(self.files)

    def download(self, url, dest):
        Path(dest).write_bytes(b"data")


@pytest.fixture
def clean_env(monkeypatch):
    """Scrub JUNEAU_RUN_* for the release tests (each opts in with pytestmark; test_push has its own scrub)."""
    for k in list(os.environ):
        if k.startswith("JUNEAU_RUN_") and k not in ("JUNEAU_RUN_PY", "JUNEAU_RUN_HISTORY", "JUNEAU_RUN_HISTORY_DIR"):
            monkeypatch.delenv(k)
    monkeypatch.delenv("RUN_MARKERS", raising=False)
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "full")
    monkeypatch.delenv("JUNEAU_RELEASE_HOME", raising=False)
    sys.modules.pop("juneau_run", None)


@pytest.fixture
def home(tmp_path):
    return tmp_path / "jrm-home"


DETAILS = dict(version="9.2.1", developmentVersion="9.2.2-SNAPSHOT", rc=1, branch="juneau-9.2.1-branch",
               summary="Bug fixes.", highlights="", knownIssues="", acknowledgements="")
CREDS = {"credential.apache.availid": "jamesbognar", "credential.apache.email": "jamesbognar@apache.org",
         "credential.apache.password": "s3cr3t-pw", "credential.gpg.keyid": "ABCD1234",
         "credential.gpg.passphrase": "s3cr3t-gpg", "credential.github.token": "s3cr3t-gh"}


@pytest.fixture
def make_ctx(home):
    def make(dry_run=False, answers=None, state=None, nexus=None, http_ok=None, with_details=True):
        fake = FakeRunner()
        runner = Runner(fake, dry_run=dry_run)
        rf = RunFile(home / "runs" / "9.2.1" / "run.json", dry_run=dry_run)
        if with_details:
            rf.set("details", dict(DETAILS))
        for k, v in (state or {}).items():
            rf.set(k, v)
        prompts = FakePrompts({**CREDS, **(answers or {})})
        ctx = Ctx(args=Namespace(dry_run=dry_run), home=home, run=fake, prompt=prompts, runner=runner,
                  nexus=nexus or FakeNexus(), svn=Svn(runner, lambda: "jamesbognar", lambda: "s3cr3t-pw"), rf=rf,
                  pause=PauseExc, http_ok=http_ok or (lambda url: True))
        ctx.creds.update(CREDS)
        ctx.fake = fake
        return ctx
    return make
