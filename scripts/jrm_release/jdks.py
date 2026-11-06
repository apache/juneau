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
"""The multi-JDK gate: resolve the params file's `jdks` / `jdks_experimental` entries to JDK homes and run `mvn verify` on
each (the `jdk-verify` step).  The resolution rule matches Juneau's `push.py --jdk` but is self-contained here."""
import glob
import os

from .ctx import StepError

JDK_ROOT = "~/jdk"


class JdkError(Exception):
    """An entry that does not resolve to exactly one JDK home.  The message says why."""


def _home(path):
    """path normalised to a JDK home: a macOS bundle (<x>.jdk) has its java under Contents/Home."""
    if os.path.isfile(os.path.join(path, "bin", "java")):
        return path
    bundled = os.path.join(path, "Contents", "Home")
    if os.path.isfile(os.path.join(bundled, "bin", "java")):
        return bundled
    raise JdkError(f"{path} is not a JDK home (no bin/java, no Contents/Home/bin/java)")


def resolve(entry, root=None):
    """The JDK home for one params entry.  A path (it has a separator or starts with ~) is used as given.  Anything else
    is a version token (28, "28-ea"): <root>/jdk-<token>, else the single <root>/*<token>* entry.  JdkError when it is
    missing or ambiguous."""
    root = root or JDK_ROOT
    token = str(entry).strip()
    if not token:
        raise JdkError("empty entry")
    if os.sep in token or token.startswith("~"):
        path = os.path.expanduser(token)
        if not os.path.isdir(path):
            raise JdkError(f"{token}: no such directory")
        return _home(path)
    base = os.path.expanduser(root)
    exact = os.path.join(base, f"jdk-{token}")
    if os.path.isdir(exact):
        return _home(exact)
    found = sorted(glob.glob(os.path.join(base, f"*{glob.escape(token)}*")))
    if not found:
        raise JdkError(f"{token}: no {root}/jdk-{token} and nothing matching {root}/*{token}* "
                       f"(or list its path in the params file)")
    if len(found) > 1:
        raise JdkError(f"{token}: ambiguous, matches {', '.join(os.path.basename(f) for f in found)}; "
                       f"list its path in the params file")
    return _home(found[0])


def entries(details):
    """[(label, required)] from the run details: required JDKs first, then experimental."""
    return ([(str(j), True) for j in details.get("jdks") or []]
            + [(str(j), False) for j in details.get("jdksExperimental") or []])


def describe(label, required):
    return label if required else f"{label} (experimental)"


def tested_line(results):
    """'Tested on JDKs: 17, 21, 28 (experimental)' from the run's jdkResults, or '' when none passed."""
    ok = [describe(label, r["required"]) for label, r in (results or {}).items() if r.get("status") == "pass"]
    return f"Tested on JDKs: {', '.join(ok)}" if ok else ""


def verify(ctx):
    """The jdk-verify step: `mvn -B clean verify` on the release workspace under each listed JDK.  Every JDK runs even
    when an earlier one failed.  A required failure fails the step at the end; an experimental one is a warning in the
    result.  run.json 'jdkResults' holds one record per JDK, so a resume reruns only the ones that did not pass (or fail)."""
    todo = entries(ctx.d)
    if not todo:
        raise StepError("no jdks in the params file")
    results = dict(ctx.rf.get("jdkResults") or {})
    mark = len(ctx.runner.skipped)
    failed, warned = [], []
    for label, required in todo:
        prior = results.get(label)
        if prior and prior.get("status") in ("pass", "warn"):
            ctx.say(f"JDK {label}: already {prior['status']}, skipped on resume")
            if prior["status"] == "warn":
                warned.append(label)
            continue
        try:
            home = resolve(label)
        except JdkError as e:
            raise StepError(f"JDK {e}") from e
        env = {"JAVA_HOME": home, "PATH": os.path.join(home, "bin") + os.pathsep + os.environ.get("PATH", "")}
        r = ctx.runner.run(["mvn", "-B", "clean", "verify"], title=f"Verify on JDK {label}", mutating=True,
                           tool="maven", cwd=ctx.ws, env=env)
        status = "pass" if r.ok else ("fail" if required else "warn")
        results[label] = {"status": status, "required": required, "home": home, "code": r.code}
        ctx.rf.update({"jdkResults": results})
        if status == "fail":
            failed.append(label)
        elif status == "warn":
            warned.append(label)
            ctx.say(f"experimental JDK {label} failed (exit {r.code}); continuing", "warn")
    if failed:
        raise StepError(f"verify failed on JDK {', '.join(failed)}; fix and --resume (passing JDKs are not rerun)")
    passed = [label for label, _ in todo if results.get(label, {}).get("status") == "pass"]
    note = f"verified on JDK {', '.join(passed)}"
    if warned:
        note += f"; WARNING: experimental JDK {', '.join(warned)} failed"
    return ctx.runner.skipped_note(mark, note)
