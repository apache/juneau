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
"""The release details: branch first (so the pom default is read from that branch), then one form."""
import dataclasses
import os
import re

from . import inventory

FIELDS = ("version", "developmentVersion", "rc", "summary", "highlights", "knownIssues", "acknowledgements")


def pom_version(text):
    """The project's own <version> (the first one outside <parent>), or None."""
    m = re.search(r"<version>\s*([^<\s]+)\s*</version>", re.sub(r"<parent>.*?</parent>", "", text or "", flags=re.S))
    return m.group(1) if m else None


def validate(d):
    """An error message for bad details, or None."""
    v, dev, rc = d.get("version"), d.get("developmentVersion"), d.get("rc")
    vt, dt = inventory.parse_version(v), inventory.parse_version(dev)
    if not vt or (v or "").endswith("-SNAPSHOT"):
        return "version must look like x.y.z"
    if not dt or not (dev or "").endswith("-SNAPSHOT"):
        return "developmentVersion must be x.y.z-SNAPSHOT"
    if dt <= vt:
        return "developmentVersion must be greater than version"
    if not isinstance(rc, int) or rc < 1:
        return "rc must be 1 or more"
    return None


def checkout():
    return os.path.expanduser(inventory.JUNEAU_CHECKOUT)


def pom_defaults(ctx, branch):
    """(version, developmentVersion) read from the Juneau checkout's origin/<branch>:pom.xml, or (None, None)."""
    r = ctx.runner.quiet(["git", "-C", checkout(), "show", f"origin/{branch}:pom.xml"], title="Read branch pom")
    cur = pom_version(r.out) if r.ok else None
    if not cur:
        return None, None
    v = cur.removesuffix("-SNAPSHOT")
    return v, inventory.next_dev_version(v)


def branch_default(ctx, version):
    """The branch to default to for a given release version: an existing origin/juneau-X-branch, else origin/X.Y.x,
    else master."""
    t = inventory.parse_version(version)
    if not t:
        return "master"
    wanted = [f"juneau-{version}-branch", f"{t[0]}.{t[1]}.x"]
    r = ctx.runner.run(["git", "-C", checkout(), "for-each-ref", "--format=%(refname:short)", "refs/remotes/origin"],
                       title="List release branches", mutating=False)
    have = {ln.strip().removeprefix("origin/") for ln in (r.out.splitlines() if r.ok else [])}
    return next((b for b in wanted if b in have), "master")


def normalize_dev(version, dev):
    """The development version as typed: blank derives from version; a bare x.y.z gets -SNAPSHOT."""
    dev = (dev or "").strip()
    if not dev:
        return inventory.next_dev_version(version)
    return dev + "-SNAPSHOT" if inventory.parse_version(dev) and not dev.endswith("-SNAPSHOT") else dev


def ask(ctx, version=None):
    """Prompt for the branch, then the details; validate, record in run.json 'details', return the details dict.
    version (from --version) prefills the Version default, derives the dev default and picks the branch default."""
    P = ctx.prompt
    default_branch = ctx.d.get("branch") or (branch_default(ctx, version) if version else "master")
    branch = P.text("Release branch", default=default_branch, key="release.branch")
    if branch is None:
        raise KeyboardInterrupt
    pom_v, pom_dev = pom_defaults(ctx, branch)
    if version:
        pom_v, pom_dev = version, inventory.next_dev_version(version)
    F = P.Field
    fields = [F("version", "Version", default=pom_v, required=True),
              F("developmentVersion", "Next development version (blank = next patch, x.y.z-SNAPSHOT)", default=pom_dev),
              F("rc", "Release candidate number", default="1", required=True, type="int"),
              F("summary", "Summary", multiline=True, required=True),
              F("highlights", "Highlights", multiline=True),
              F("knownIssues", "Known issues", multiline=True),
              F("acknowledgements", "Acknowledgements", multiline=True)]
    while True:
        got = P.form("Release details", fields, key="release.details")
        if got is None:
            raise KeyboardInterrupt
        if got.get("developmentVersion") == pom_dev and got.get("version") != pom_v:
            got["developmentVersion"] = None   # the prefilled dev version belongs to the old Version; re-derive
        got["developmentVersion"] = normalize_dev(got.get("version"), got.get("developmentVersion"))
        err = validate(got)
        if not err:
            break
        ctx.say(f"❌ {err}", "error")
        fields = [dataclasses.replace(f, default=str(got[f.name]) if got.get(f.name) not in (None, "") else f.default)
                  for f in fields]
    d = {"branch": branch, **{k: got.get(k) for k in FIELDS}}
    ctx.rf.set("details", d)
    return d
