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
"""
juneau_scripts: helpers for writing push.py and release.py scripts.  A toolkit, not a framework: every policy (what
may be committed, which identity is required, what a dry run means) is the script author's own code.

Modules: git, run, tools, notify, java, lock, prompt, state, steps, proc, coverage, testing.  Import them explicitly
(`from juneau_scripts import git, run`); this file imports nothing heavy, and never testing or its pytest plugin.

No helper in git, run or tools runs `git add`, passes `--force` or `--no-verify`, or writes git config.  The toolkit
never decides what may be committed.  git.commit_index commits the index exactly as found.  git.commit_paths is a
pathspec commit with git's --only semantics: it commits the working-tree content of the named tracked paths,
including unstaged edits to them, and leaves every other staged path staged.  It does not respect the index for the
named paths.
"""

import sys

from ._base import Result

__all__ = ["Result", "check_bootstrap"]

# The bootstrap.py API this package needs.  bootstrap.load() records its own BOOTSTRAP_API here after the import.
REQUIRED_BOOTSTRAP_API = 1
BOOTSTRAP_API = None
_warned = False


def check_bootstrap():
    """Warn once, on stderr, when the vendored bootstrap.py is missing its API stamp or older than this package."""
    global _warned
    if _warned or (BOOTSTRAP_API is not None and BOOTSTRAP_API >= REQUIRED_BOOTSTRAP_API):
        return
    _warned = True
    sys.stderr.write(
        f"warning: bootstrap.py is older than juneau_scripts expects (BOOTSTRAP_API {BOOTSTRAP_API}, need "
        f"{REQUIRED_BOOTSTRAP_API}); refresh it with: unzip -p "
        f"~/.m2/repository/org/apache/juneau/juneau-run/<v>/juneau-run-<v>.zip bootstrap.py > scripts/bootstrap.py\n")
