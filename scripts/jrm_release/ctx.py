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
"""The object every release step receives, and the error a step fails with."""
import os
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Callable

from . import keychain
from .params import SECRET_ENV


class StepError(RuntimeError):
    """A step failed.  The message is the row's failure text."""


SECRET_KEYS = ("credential.apache.password", "credential.gpg.passphrase", "credential.github.token")

# The non-secret credentials persisted in run.json 'identity' (so a resume does not re-ask them), by their key there.
# The only writer of 'identity' reads this map, so a secret cannot reach it.
IDENTITY = {"credential.apache.availid": "apacheId", "credential.apache.email": "apacheEmail",
            "credential.gpg.keyid": "gpgKeyId"}

LABELS = {
    "credential.apache.availid": "Apache id",
    "credential.apache.email": "Apache email address",
    "credential.apache.password": "Apache password",
    "credential.gpg.keyid": "GPG key id",
    "credential.gpg.passphrase": "GPG passphrase",
    "credential.github.token": "GitHub token",
}


def _now():
    return datetime.now().astimezone()


@dataclass
class Ctx:
    args: Any                 # parsed CLI namespace
    home: Path                # <JUNEAU_RELEASE_HOME>
    run: Any                  # juneau_terminal_scripts.run (say, cmd, ...) or a fake
    runner: Any               # jrm_release.runner.Runner
    nexus: Any                # jrm_release.nexus.Nexus or FakeNexus
    svn: Any                  # jrm_release.svn.Svn
    rf: Any                   # jrm_release.runfile.RunFile
    pause: Any                # juneau_terminal_scripts.steps.Pause (an exception class)
    http_ok: Callable[[str], bool] = lambda url: False
    http_text: Callable[[str], Any] = lambda url: None   # fn(url) -> body text, or None when unreachable
    sleep: Callable[[float], None] = lambda s: None
    now: Callable[[], Any] = _now
    creds: dict = field(default_factory=dict)
    prereq: Any = None        # failures list from prereqs.check, once the early check has run
    prereq_verdicts: Any = None   # {check name: Verdict} from the same run
    secret_source: dict = field(default_factory=dict)   # {secret key: "env" | "keychain" | "gh" | "cache"}; never a value
    pf: dict = field(default_factory=dict)              # preflight scratch: loaded params, details, scratch repo
    preflight_results: Any = None                       # [(group, name, Result)] from the pre-run preflight
    opener: Callable[[Path], None] = lambda path: None   # opens a file for the user; release.py installs the real one
    current_step: Callable[[], Any] = lambda: None        # the open board step's handle (release.py installs it)
    features: frozenset = frozenset()                   # the toolkit's BOARD_FEATURES; a step uses a feature only when listed

    @property
    def dry_run(self) -> bool:
        return bool(getattr(self.args, "dry_run", False))

    @property
    def d(self) -> dict:
        return self.rf.get("details") or {}

    @property
    def version(self) -> str:
        return self.d["version"]

    @property
    def rc_tag(self) -> str:
        return f"juneau-{self.version}-RC{self.d['rc']}"

    @property
    def resume_hint(self) -> str:
        """The command that continues this run after a pause."""
        return f"python3 scripts/release.py --resume --version {self.version}"

    @property
    def run_dir(self) -> Path:
        return self.home / "runs" / self.version

    @property
    def ws(self) -> Path:
        return self.run_dir / "workspace"

    def say(self, text="", level="info"):
        self.run.say(text, level)

    def note(self, text):
        """Live detail text on this step's board row; a log line when there is no board row to put it on."""
        detail = getattr(self.current_step(), "detail", None)
        if detail:
            detail(text)
        else:
            self.say(text)

    def fail(self, msg):
        raise StepError(msg)

    def find_secret(self, key):
        """A secret without asking: cached, the dry-run placeholder, the environment (an empty value counts as
        unset), the keychain, and for the GitHub token `gh auth token`.  None when nothing has it.  A value found is
        cached in creds, so it is masked like any secret and never looked up twice; where it came from (never the
        value) is kept in secret_source."""
        if key in self.creds:
            self.secret_source.setdefault(key, "cache")
            return self.creds[key]
        source = None
        if self.dry_run:   # no env, keychain or gh is touched, so no real secret is ever printed
            value, source = "********", "dry-run"
        else:
            value = os.environ.get(SECRET_ENV.get(key, "")) or None
            source = "env" if value else None
            if value is None and key == "credential.github.token":
                value = keychain.lookup(self, keychain.SERVICES[key], "default")
                source = "keychain" if value else None
                if value is None:
                    value = keychain.gh_token(self)
                    source = "gh" if value else None
            elif value is None and key in keychain.SERVICES:
                value = keychain.lookup(self, keychain.SERVICES[key], self.cred("credential.apache.availid"))
                source = "keychain" if value else None
        if value is not None:
            self.creds[key] = value
            self.secret_source[key] = source
        return value

    def cred(self, key, *, secret=False):
        """A credential, never asked for.  A secret comes from find_secret (cache, env, keychain, gh); an identity value
        from creds (the params file, or run.json 'identity' on a resume).  The Apache email defaults to
        <availid>@apache.org.  Anything missing is a StepError naming where to set it."""
        if key in self.creds:
            return self.creds[key]
        if secret:
            value = self.find_secret(key)
            if value is not None:
                return value
            raise StepError(f"{LABELS.get(key, key)} not found: set {SECRET_ENV.get(key, key)} (or store it in "
                            f"the keychain) and rerun")
        if key == "credential.apache.email" and self.creds.get("credential.apache.availid"):
            self.creds[key] = f"{self.creds['credential.apache.availid']}@apache.org"
            self.remember(key)
            return self.creds[key]
        raise StepError(f"{LABELS.get(key, key)} is not set: put it in the params file")

    def remember(self, key):
        """Merge a non-secret credential in creds into run.json 'identity' (the IDENTITY allow-list is the only way in)."""
        value = self.creds.get(key)
        if key in IDENTITY and isinstance(value, str) and value:
            self.rf.update({"identity": {**(self.rf.get("identity") or {}), IDENTITY[key]: value}})

    def identity(self):
        """The non-secret credentials held so far, as run.json 'identity' stores them."""
        return {IDENTITY[k]: v for k, v in self.creds.items() if k in IDENTITY and isinstance(v, str) and v}

    def seed_identity(self):
        """Load creds from run.json 'identity' (a resume): only non-empty strings under the three known keys are used;
        an absent or malformed identity changes nothing."""
        ident = self.rf.get("identity")
        if isinstance(ident, dict):
            for key, name in IDENTITY.items():
                if isinstance(ident.get(name), str) and ident[name]:
                    self.creds.setdefault(key, ident[name])

    def secrets(self):
        """Real secret values, for masking."""
        return [v for k, v in self.creds.items() if k in SECRET_KEYS and v and v != "********"]
