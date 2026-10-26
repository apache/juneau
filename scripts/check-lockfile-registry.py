#!/usr/bin/env python3
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
"""Check that every `resolved` URL in the e2e npm lockfile is on the public npm registry.  Full docs: check-lockfile-registry.md"""

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_LOCKFILE = REPO_ROOT / "e2e" / "package-lock.json"
PUBLIC_NPM_REGISTRY = "https://registry.npmjs.org/"
RESOLVED_RE = re.compile(r'"resolved":\s*"([^"]*)"')


def bad_urls(lockfile):
    """The (line, url) pairs in the lockfile whose `resolved` URL is not on the public npm registry."""
    out = []
    for n, text in enumerate(Path(lockfile).read_text(encoding="utf-8").splitlines(), 1):
        m = RESOLVED_RE.search(text)
        if m and not m.group(1).startswith(PUBLIC_NPM_REGISTRY):
            out.append((n, m.group(1)))
    return out


def main(argv=None):
    parser = argparse.ArgumentParser(description="Fail when a lockfile `resolved` URL is not on the public npm registry.")
    parser.add_argument("--lockfile", default=str(DEFAULT_LOCKFILE), help="Lockfile to check (default: e2e/package-lock.json).")
    args = parser.parse_args(argv)
    lockfile = Path(args.lockfile)
    if not lockfile.is_file():
        print(f"ERROR: lockfile not found: {lockfile}", file=sys.stderr)
        return 2
    bad = bad_urls(lockfile)
    for n, url in bad:
        print(f"{lockfile}:{n}: `resolved` URL is not on the public npm registry: {url}")
    if bad:
        print(f"check-lockfile-registry: {len(bad)} URL(s) off the public npm registry")
        return 1
    print("check-lockfile-registry: all `resolved` URLs are on the public npm registry")
    return 0


if __name__ == "__main__":
    sys.exit(main())
