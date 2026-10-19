"""svn / svnmucc through the Runner.  The password is stdin only; flags are --non-interactive --no-auth-cache."""
from .ctx import StepError

BASE = ["--non-interactive", "--no-auth-cache"]


class Svn:
    def __init__(self, runner, user, password):
        self.runner, self.user, self.password = runner, user, password

    def ls(self, url):
        """Entry names (trailing / kept), or None when the url cannot be listed."""
        r = self.runner.run(["svn", "ls", *BASE, url], title=f"svn ls {url.rstrip('/').rsplit('/', 1)[-1]}",
                            mutating=False, tool="svn")
        return [line for line in r.out.splitlines() if line.strip()] if r.ok else None

    def mucc(self, message, ops, *, title="svnmucc commit"):
        """One svnmucc commit.  ops are argv fragments such as ['rm', url] or ['put', local, url]."""
        pw = self.password()
        argv = ["svnmucc", *BASE, "--username", self.user(), "--password-from-stdin", "-m", message]
        for op in ops:
            argv += list(op)
        r = self.runner.run(argv, title=title, mutating=True, tool="svn", input=(pw + "\n").encode())
        if not r.ok:
            raise StepError(f"{title} failed (exit {r.code})")
        return r
