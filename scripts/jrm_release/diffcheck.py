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
"""Pure checker for the release plugin's two commits (spec: R0048).  No I/O: callers pass diff text in.

maven-release-plugin makes two commits on the branch: "[maven-release-plugin] prepare release <tag>" (the tagged
commit: X-SNAPSHOT -> X in every reactor pom, plus the SCM <tag> and the juneau.version property of the example
microservices) and, as its child, "[maven-release-plugin] prepare for next development iteration" (X ->
next-SNAPSHOT, the tag back).  Juneau's pom already names the RC tag in <scm><tag> on the branch, so a tag change is
allowed but not required."""
import re
from dataclasses import dataclass, field

RELEASE_SUBJECT = "prepare release"
NEXT_SUBJECT = "prepare for next development iteration"


@dataclass
class Result:
    problems: list = field(default_factory=list)   # each a printable block: what is wrong, then the offending hunk
    summary: str = ""

    @property
    def clean(self):
        return not self.problems


def _git_header(line):
    """(old path, new path) of a `diff --git a/<old> b/<new>` line, splitting at the last " b/", else None."""
    if not line.startswith("diff --git a/"):
        return None
    old, sep, new = line[len("diff --git a/"):].rpartition(" b/")
    return (old, new) if sep else None


def parse(diff):
    """{path: {'flags': [str], 'hunks': [(header, removed, added)]}} from a unified diff (any context size)."""
    files, cur, hunk = {}, None, None
    for line in diff.splitlines():
        m = _git_header(line)
        if m:
            cur, hunk = files.setdefault(m[1], {"flags": [], "hunks": []}), None
            if m[0] != m[1]:
                cur["flags"].append("renamed")
        elif cur is None:
            continue
        elif line.startswith(("new file mode", "deleted file mode")):
            cur["flags"].append(line.split(" mode")[0])
        elif line.startswith(("--- ", "+++ ", "index ", "similarity ", "rename ")) and hunk is None:
            continue
        elif line.startswith("@@"):
            hunk = (line, [], [])
            cur["hunks"].append(hunk)
        elif hunk is not None and line.startswith("-"):
            hunk[1].append(line[1:].strip())
        elif hunk is not None and line.startswith("+"):
            hunk[2].append(line[1:].strip())
    return files


def _expected(old_version, new_version, old_tag, new_tag):
    return {(f"<{el}>{old}</{el}>", f"<{el}>{new}</{el}>")
            for el, old, new in (("version", old_version, new_version), ("juneau.version", old_version, new_version),
                                 ("tag", old_tag, new_tag))}


def _hunk_text(path, hunk):
    return "\n".join([f"  {path} {hunk[0]}"] + [f"  -{r}" for r in hunk[1]] + [f"  +{a}" for a in hunk[2]])


def check_commit(diff, subject, subject_part, old_version, new_version, old_tag, new_tag):  # NOSONAR python:S3776 - one pass of per-file checks; splitting it would scatter them
    """(changed pom paths, tag changes seen, problems) for one commit's diff."""
    problems = []
    if subject_part not in (subject or ""):
        problems.append(f"commit message is {subject!r}, expected one containing {subject_part!r}")
    ok_pairs = _expected(old_version, new_version, old_tag, new_tag)
    files = parse(diff)
    if not files:
        problems.append("the commit changes nothing")
    poms, tags = set(), 0
    for path, info in files.items():
        if not (path == "pom.xml" or path.endswith("/pom.xml")):
            problems.append(f"not a pom.xml: {path}" + (f" ({', '.join(info['flags'])})" if info["flags"] else ""))
            continue
        poms.add(path)
        if info["flags"]:
            problems.append(f"{path} was {', '.join(info['flags'])}")
        for h in info["hunks"]:
            pairs = list(zip(h[1], h[2]))
            if len(h[1]) != len(h[2]) or any(p not in ok_pairs for p in pairs):
                problems.append("unexpected change:\n" + _hunk_text(path, h))
            tags += sum(1 for p in pairs if p[0].startswith("<tag>") and p in ok_pairs)
    return poms, tags, problems


def reactor_snapshot_poms(read, version):
    """The poms of the reactor (root pom.xml, then each <module> recursively) that carry <version>X-SNAPSHOT</version>.
    read(path) -> text or None.  A pom outside the reactor is never touched by the release plugin, so it is not
    expected in the diff."""
    out, todo, seen = [], [""], set()
    needle = f"<version>{version}-SNAPSHOT</version>"
    while todo:
        d = todo.pop()
        path = f"{d}/pom.xml" if d else "pom.xml"
        text = read(path)
        if text is None or path in seen:
            continue
        seen.add(path)
        if needle in text:
            out.append(path)
        todo += [f"{d}/{m}".lstrip("/") for m in re.findall(r"<module>\s*([^<\s]+)\s*</module>", text)]
    return sorted(out)


def check_release(*, version, dev_version, rc_tag, release_diff, release_subject, next_diff, next_subject,
                  snapshot_poms):
    """Both commits against what the plugin should have done.  snapshot_poms: the poms that carry
    <version>X-SNAPSHOT</version> before the release commit (each must appear in its diff)."""
    snap = f"{version}-SNAPSHOT"
    r_poms, _, problems = check_commit(release_diff, release_subject, RELEASE_SUBJECT, snap, version, "HEAD",
                                            rc_tag)
    missing = sorted(set(snapshot_poms) - r_poms)
    if missing:
        problems.append("poms still at the snapshot version, skipped by the release commit: " + ", ".join(missing))
    n_poms, _, n_problems = check_commit(next_diff, next_subject, NEXT_SUBJECT, version, dev_version, rc_tag,
                                              "HEAD")
    problems += [f"[next-iteration commit] {p}" for p in n_problems]
    if n_poms != r_poms:
        problems.append("[next-iteration commit] changes different poms than the release commit: "
                        + ", ".join(sorted(n_poms ^ r_poms)))
    summary = (f"{len(r_poms)} poms {snap} -> {version}, tag {rc_tag}; next iteration {version} -> "
               f"{dev_version}")
    return Result(problems, summary)
