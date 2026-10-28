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
import subprocess
import sys
import tempfile
import types
from argparse import Namespace
from pathlib import Path

import pytest

SCRIPTS = Path(__file__).resolve().parent.parent
if str(SCRIPTS) not in sys.path:
    sys.path.append(str(SCRIPTS))

from jrm_release.ctx import Ctx  # noqa: E402
from jrm_release import runner as runner_module  # noqa: E402
from jrm_release.runfile import RunFile  # noqa: E402
from jrm_release.runner import Outcome, Runner  # noqa: E402
from jrm_release.svn import Svn  # noqa: E402


@pytest.fixture(scope="session", autouse=True)
def _no_real_run_history():
    """Keep the suite out of ~/.cache/juneau-terminal/history: main()-calling tests would otherwise record their stub
    steps beside the real push and release records.  No fixture may strip these two variables."""
    tmp = tempfile.mkdtemp(prefix="juneau-terminal-test-history-")
    names = ("JUNEAU_TERMINAL_HISTORY", "JUNEAU_TERMINAL_HISTORY_DIR")
    saved = {n: os.environ.get(n) for n in names}
    os.environ["JUNEAU_TERMINAL_HISTORY"] = "0"
    os.environ["JUNEAU_TERMINAL_HISTORY_DIR"] = tmp
    try:
        yield
    finally:
        for n, v in saved.items():
            if v is None:
                os.environ.pop(n, None)
            else:
                os.environ[n] = v
        shutil.rmtree(tmp, ignore_errors=True)


@pytest.fixture(autouse=True)
def _pyyaml_presence_stub(monkeypatch):
    """Make the PyYAML preflight check (`importlib.util.find_spec("yaml")`) pass whether or not PyYAML is installed, so
    "every check passes" tests do not depend on the environment (push.py runs the suite in an env without it).  Only
    a missing "yaml" is faked; tests that parse YAML use pytest.importorskip("yaml"), and tests about the check itself
    re-patch find_spec to control presence explicitly."""
    from jrm_release import preflight
    real = preflight.importlib.util.find_spec

    def find_spec(name, *args, **kwargs):
        found = real(name, *args, **kwargs)
        if found is None and name == "yaml":
            return types.SimpleNamespace(name="yaml")
        return found

    monkeypatch.setattr(preflight.importlib.util, "find_spec", find_spec)


class PauseExc(Exception):
    def __init__(self, message, *, done=True):
        super().__init__(message)
        self.message, self.done = message, done


class FakeRunner:
    """Stands in for juneau_terminal_scripts.run: records run.cmd calls, answers them from scripted rules."""

    def __init__(self):
        self.calls, self.rules, self.said = [], [], []

    def script(self, *prefix, out="", ok=True, code=None):
        """Answer argvs starting with prefix; code is the exit code when ok is False (default 1)."""
        self.rules.append((prefix, out, ok, code))

    def cmd(self, cmd, *, title="", id=None, n=None, tool=None, cwd=None, env=None, capture=False, tty=False,
            summarize=None, input=None):
        rec = dict(argv=[str(c) for c in cmd], title=title, tool=tool, cwd=cwd, env=dict(env or {}), input=input,
                   tty=tty, capture=capture)
        self.calls.append(rec)
        for prefix, out, ok, code in reversed(self.rules):
            if tuple(rec["argv"][:len(prefix)]) == prefix:
                return Outcome(ok, 0 if ok else (code or 1), out)
        known = self.preflight_default(rec["argv"])
        if known is not None:
            return known
        return Outcome(True, 0, self.DEFAULT_VERSIONS.get(rec["argv"][-1], "") if len(rec["argv"]) <= 3 else "")

    # What an unscripted preflight probe answers, so a new run's preflight passes by default: the branch tip's pom and
    # release notes (through the scratch repo), the secret keyring, the GitHub token check, and no RC dir on dist/dev.
    TIP_FILES = {"FETCH_HEAD:pom.xml": "<project><version>9.2.1-SNAPSHOT</version></project>",
                 "FETCH_HEAD:RELEASE-NOTES.txt": "Release Notes - Juneau - Version 9.2.1 - 2026-10-28\n"}
    KEY_FPR = "F" * 24 + "ABCD1234ABCD1234"
    KEYRING = f"sec:u:4096:1:ABCD1234ABCD1234:1500000000:::u:::scESC:::+:::23::0:\nfpr:::::::::{KEY_FPR}:\n"

    def preflight_default(self, argv):
        if argv[:1] == ["git"] and "show" in argv and argv[-1].startswith("FETCH_HEAD:"):
            text = self.TIP_FILES.get(argv[-1])
            return Outcome(text is not None, 0 if text is not None else 128, text or "")
        if argv[:2] == ["gpg", "--list-secret-keys"]:
            return Outcome(True, 0, self.KEYRING)
        if argv[:3] == ["gh", "api", "user"]:
            return Outcome(True, 0, "HTTP/2.0 200 OK\nX-Oauth-Scopes: repo, gist\n\n{}")
        if argv[:2] == ["svn", "ls"] and any("/dev/juneau/source/juneau-" in a or "/dev/juneau/binaries/juneau-" in a
                                              for a in argv[2:]):
            return Outcome(False, 1, "svn: E170000: URL doesn't exist\n")
        return None

    # What an unscripted `<tool> --version` prints, so prerequisites (which fail on unreadable output) pass by default.
    DEFAULT_VERSIONS = {"-version": 'openjdk version "17.0.9" 2023-10-17\n', "--version": "9.9.9\n",
                        "--quiet": "1.14.3\n"}

    def quiet_cmd(self, cmd, *, title="", cwd=None, timeout=None, env=None, input=None):
        """Runner.quiet's hook: answered like cmd, but flagged as kept out of the log."""
        out = self.cmd(cmd, title=title, cwd=cwd, capture=True, env=env, input=input)
        self.calls[-1]["quiet"] = True
        self.calls[-1]["timeout"] = timeout
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
    """Scrub JUNEAU_TERMINAL_* for the release tests (each opts in with pytestmark; test_push has its own scrub)."""
    for k in list(os.environ):
        if k.startswith("JUNEAU_TERMINAL_") and k not in ("JUNEAU_TERMINAL_PY", "JUNEAU_TERMINAL_HISTORY", "JUNEAU_TERMINAL_HISTORY_DIR"):
            monkeypatch.delenv(k)
    monkeypatch.delenv("TERMINAL_MARKERS", raising=False)
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "full")
    monkeypatch.delenv("JUNEAU_RELEASE_HOME", raising=False)
    sys.modules.pop("juneau_terminal", None)


@pytest.fixture(autouse=True)
def _no_real_credentials(monkeypatch):
    """No test may read a secret from the developer's environment, or reach a real command through the Runner (which
    would mean the real keychain, gpg keyring or gh login): the Runner's subprocess is a stub that fails loudly.
    A test that exercises the real subprocess path replaces its `run` again."""
    for name in ("JRM_APACHE_PASSWORD", "JRM_GPG_PASSPHRASE", "JRM_GITHUB_TOKEN"):
        monkeypatch.delenv(name, raising=False)

    def refuse(*a, **k):
        raise AssertionError(f"a test reached a real subprocess through Runner: {a!r}")

    monkeypatch.setattr(runner_module, "subprocess", types.SimpleNamespace(
        run=refuse, TimeoutExpired=subprocess.TimeoutExpired))


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


# Every JUNEAU_TERMINAL_* variable that points a session at a live parent run (its logs, cancel command, markers, result,
# header and pending/overall rows, or the fact that one is active).  JUNEAU_TERMINAL_CONSOLE, _DETAIL and _HISTORY* are
# settings, not pointers, and are handled elsewhere.
LIVE_RUN_ENV = (
    "JUNEAU_TERMINAL_ACTIVE", "JUNEAU_TERMINAL_SESSION", "JUNEAU_TERMINAL_LIVE", "JUNEAU_TERMINAL_N_BASE", "JUNEAU_TERMINAL_LABEL_WIDTH",
    "JUNEAU_TERMINAL_FULL_LOG", "JUNEAU_TERMINAL_BOARD_LOG", "JUNEAU_TERMINAL_CANCEL", "JUNEAU_TERMINAL_HEADER",
    "JUNEAU_TERMINAL_PENDING", "JUNEAU_TERMINAL_OVERALL", "JUNEAU_TERMINAL_HANDBACK",
    "JUNEAU_TERMINAL_MARKERS_FILE", "JUNEAU_TERMINAL_RESULT", "JUNEAU_TERMINAL_RESULT_PARTS",
)


@pytest.fixture(autouse=True)
def _no_outer_run_files(monkeypatch):
    """A run under watch-run.sh or push.py exports its live-run variables (see LIVE_RUN_ENV) to this suite; drop them so
    the tests see the same run state as under a plain run (and don't write the outer run's logs or files)."""
    for name in LIVE_RUN_ENV:
        monkeypatch.delenv(name, raising=False)
