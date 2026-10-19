"""The one place a command runs.  Under --dry-run a mutating command is printed, never executed."""
import subprocess
from collections import namedtuple

Outcome = namedtuple("Outcome", "ok code out")
MASK = "********"


class Runner:
    def __init__(self, run_mod, dry_run=False, secrets=lambda: ()):
        self.run_mod, self.dry_run, self.secrets = run_mod, dry_run, secrets

    def mask(self, text):
        for s in self.secrets():
            text = text.replace(s, MASK)
        return text

    def gate(self, describe, mutating=True):
        """True when the caller should really do it.  Under --dry-run a mutation prints and returns False."""
        if self.dry_run and mutating:
            self.run_mod.say(self.mask(f"would run: {describe}"))
            return False
        return True

    def run(self, cmd, *, title, mutating, tool="generic", cwd=None, env=None, input=None, tty=False, capture=None,
            merge_err=False):
        """Run argv.  capture defaults to read-only commands (their stdout is the answer); pass capture=True for a
        mutating command whose output the caller needs.  merge_err appends stderr to the answer, for tools like
        `java -version` that print there (the toolkit's plain mode keeps the two apart)."""
        argv = [str(c) for c in cmd]
        if not self.gate(" ".join(argv), mutating):
            return Outcome(True, 0, "")
        child_env = getattr(self.run_mod, "child_env", None)
        if child_env is not None:   # the toolkit's: this process's environment minus juneau-run's own variables
            env = {**child_env(), **(env or {})}
        r = self.run_mod.cmd(argv, title=title, tool=tool, cwd=cwd, env=env, input=input, tty=tty,
                             capture=(not mutating) if capture is None else capture)
        out = getattr(r, "out", "") or ""
        if merge_err:
            out += getattr(r, "err", "") or ""
        return Outcome(r.ok, r.code, out)

    def quiet(self, cmd, *, title, cwd=None):
        """Run a read-only argv whose output the caller wants but the log does not (a ~1,000-line pom).  Never
        echoed, so it is not a row and never reaches full.log; honors the same dry-run rule (reads always run)."""
        argv = [str(c) for c in cmd]
        hook = getattr(self.run_mod, "quiet_cmd", None)
        if hook is not None:   # tests' fake
            r = hook(argv, title=title, cwd=cwd)
            return Outcome(r.ok, r.code, getattr(r, "out", "") or "")
        child_env = getattr(self.run_mod, "child_env", None)
        try:
            r = subprocess.run(argv, cwd=cwd, env=child_env() if child_env else None, capture_output=True, text=True,
                               check=False)
        except OSError as e:
            return Outcome(False, 127, str(e))
        return Outcome(r.returncode == 0, r.returncode, r.stdout)
