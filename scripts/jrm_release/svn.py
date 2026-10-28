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
"""svn / svnmucc through the Runner.  The password is stdin only; flags are --non-interactive --no-auth-cache."""
from .ctx import StepError

BASE = ["--non-interactive", "--no-auth-cache"]


class Svn:
    def __init__(self, runner, user, password):
        self.runner, self.user, self.password = runner, user, password

    def ls(self, url, *, quiet=False):
        """Entry names (trailing / kept), or None when the url cannot be listed.  quiet keeps the listing off the
        console and out of the log (it is not a row)."""
        title = f"svn ls {url.rstrip('/').rsplit('/', 1)[-1]}"
        if quiet:
            r = self.runner.quiet(["svn", "ls", *BASE, url], title=title)
        else:
            r = self.runner.run(["svn", "ls", *BASE, url], title=title, mutating=False, tool="svn")
        return [line for line in r.out.splitlines() if line.strip()] if r.ok else None

    def exists(self, url):
        """True when url lists, False when svn says it is not there (E170000, W160013 or "non-existent"), None when it
        cannot be told (any other failure, such as a network error).  Anonymous, quiet."""
        r = self.runner.quiet(["svn", "ls", *BASE, url], title=f"svn exists {url.rstrip('/').rsplit('/', 1)[-1]}",
                              merge_err=True)
        if r.ok:
            return True
        low = r.out.lower()
        return False if ("e170000" in low or "w160013" in low or "non-existent" in low) else None

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
