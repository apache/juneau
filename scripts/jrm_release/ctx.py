"""The object every release step receives, and the error a step fails with."""
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Callable


class StepError(RuntimeError):
    """A step failed.  The message is the row's failure text."""


SECRET_KEYS = ("credential.apache.password", "credential.gpg.passphrase", "credential.github.token")

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
    run: Any                  # juneau_scripts.run (say, cmd, ...) or a fake
    prompt: Any               # juneau_scripts.prompt or FakePrompts
    runner: Any               # jrm_release.runner.Runner
    nexus: Any                # jrm_release.nexus.Nexus or FakeNexus
    svn: Any                  # jrm_release.svn.Svn
    rf: Any                   # jrm_release.runfile.RunFile
    pause: Any                # juneau_scripts.steps.Pause (an exception class)
    http_ok: Callable[[str], bool] = lambda url: False
    http_text: Callable[[str], Any] = lambda url: None   # fn(url) -> body text, or None when unreachable
    sleep: Callable[[float], None] = lambda s: None
    now: Callable[[], Any] = _now
    creds: dict = field(default_factory=dict)
    prereq: Any = None        # failures list from prereqs.check, once the early check has run
    prereq_verdicts: Any = None   # {check name: Verdict} from the same run

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
    def run_dir(self) -> Path:
        return self.home / "runs" / self.version

    @property
    def ws(self) -> Path:
        return self.run_dir / "workspace"

    def say(self, text="", level="info"):
        self.run.say(text, level)

    def review(self, question, *, key, look_at):
        """Ask the user to inspect something: look_at says what and where, and the (y/n) hint is part of the text
        (the toolkit's confirm shows none inline).  Under --dry-run nothing was produced, so it answers itself."""
        if self.dry_run:
            self.say(f"(dry run: nothing to review) {question}")
            return True
        return self.prompt.confirm(f"{look_at}\n{question}  (y/n)", key=key)

    def fail(self, msg):
        raise StepError(msg)

    def cred(self, key, *, secret=False):
        """A credential, asked once per process through the prompt API.  Secrets are not asked under --dry-run."""
        if key in self.creds:
            return self.creds[key]
        if secret and self.dry_run:
            value = "********"
        else:
            ask = self.prompt.secret if secret else self.prompt.text
            value = ask(LABELS.get(key, key), key=key)
            if value is None:
                raise KeyboardInterrupt
        self.creds[key] = value
        return value

    def secrets(self):
        """Real secret values, for masking."""
        return [v for k, v in self.creds.items() if k in SECRET_KEYS and v and v != "********"]
