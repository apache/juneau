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
"""Email text and the send flow.  VOTE from Juneau's release.py."""
import urllib.parse

from . import inventory

MAILTO_LIMIT = 1800


def propose(d):
    sub = f"[PROPOSE] Release Apache Juneau {d['version']}"
    body = (f"I propose to release Apache Juneau {d['version']} (RC{d['rc']}) from branch {d['branch']}.\n\n"
            f"{d['summary']}\n\n"
            "If there are last-minute blockers, please reply on this thread before the build starts.\n")
    return inventory.DEV_LIST, sub, body


def send(ctx, kind, to, subject, body, cc=()):
    """Ask the prompt channel to open the mail.  Sent: recorded in run.json.  'Not yet': Pause(done=False).  Under
    --dry-run nothing is sent, so the mail is shown and the question answers itself."""
    if ctx.dry_run:
        ctx.say(f"(dry run: nothing is sent) {kind} email to {to}: {subject}\n\n{body}")
        return f"skipped (dry run): {kind} email not sent"
    sent = ctx.prompt.email(to=to, subject=subject, body=body, cc=cc, key=f"email.{kind}")
    if sent is None:
        raise KeyboardInterrupt
    if "body=" not in mailto_url(to, subject, body):
        ctx.say("body too long for a mailto link; copy it from the log")
    if not sent:
        raise ctx.pause(f"{kind} email not sent yet.  Re-run with --resume when it has been sent.", done=False)
    emails = dict(ctx.rf.get("emails") or {})
    emails[kind] = ctx.now().isoformat() if ctx.now else "sent"
    ctx.rf.set("emails", emails)
    return f"{kind} email sent"


def mailto_url(to, subject, body):
    """The mailto URL; the body is dropped when the URL would exceed MAILTO_LIMIT characters."""
    q = urllib.parse.quote
    full = f"mailto:{to}?subject={q(subject)}&body={q(body)}"
    return full if len(full) <= MAILTO_LIMIT else f"mailto:{to}?subject={q(subject)}"


def _optional(d):
    parts = []
    if d.get("highlights"):
        parts.append(f"Highlights:\n{d['highlights']}")
    if d.get("knownIssues"):
        parts.append(f"Known issues:\n{d['knownIssues']}")
    return "\n\n".join(parts)


def vote(d, f):
    v, rel = d["version"], f"juneau-{d['version']}-RC{d['rc']}"
    opt = _optional(d)
    body = f"""I am pleased to be calling this vote for the source release of Apache Juneau {v} RC{d['rc']}.
{chr(10) + opt + chr(10) if opt else ''}
The binaries are available at:

{inventory.DIST_DEV}/binaries/{rel}/

The release candidate to be voted over is available at:
{inventory.DIST_DEV}/source/{rel}/

SHA-512 checksums:

apache-juneau-{v}-src.zip: 
{f['src_sha']}

apache-juneau-{v}-bin.zip: 
{f['bin_sha']}

Build the release candidate using:

mvn clean install


The release candidate is signed with a GPG key available at:
{inventory.DIST_RELEASE}/KEYS

A staged Maven repository is available for review at:
{inventory.NEXUS_URL}/content/repositories/{f['repo']}/

The Git commit for this release is...
https://gitbox.apache.org/repos/asf?p=juneau.git;a=commit;h={f['commit']}

Please vote on releasing this package as:
Apache Juneau {v}

This vote will be open until {f['deadline']} and passes if a majority of at least three +1 Apache Juneau PMC votes are cast.
(needs to be at least 72 weekday hours)

[ ] +1 Release this package
[ ] 0 I don't feel strongly about it, but don't object
[ ] -1 Do not release this package because...

Anyone can participate in testing and voting, not just committers, please feel free to try out the release candidate and provide your votes.
"""
    return inventory.DEV_LIST, f"[VOTE] Release Apache Juneau {v} RC{d['rc']}", body


def result(d, outcome, tally):
    v, rc = d["version"], d["rc"]
    ack = f"\n\n{d['acknowledgements']}" if d.get("acknowledgements") else ""
    body = (f"The vote to release Apache Juneau {v} RC{rc} has {outcome}.\n\n{tally}{ack}\n\n"
            "Thanks to everyone who voted.\n")
    return inventory.DEV_LIST, f"[RESULT][VOTE] Release Apache Juneau {v} RC{rc}", body


def announce(d, github_url):
    v = d["version"]
    parts = [f"The Apache Juneau team is pleased to announce the release of Apache Juneau {v}.", d["summary"]]
    if d.get("highlights"):
        parts.append(f"Highlights:\n{d['highlights']}")
    if d.get("knownIssues"):
        parts.append(f"Known issues:\n{d['knownIssues']}")
    parts.append(f"Downloads: {inventory.DIST_RELEASE}/{v}/\nGitHub Release: {github_url or 'n/a'}\n"
                 "Release notes: https://juneau.apache.org/#release-notes")
    if d.get("acknowledgements"):
        parts.append(d["acknowledgements"])
    parts.append("The Apache Juneau Team")
    return inventory.ANNOUNCE_LIST, f"[ANNOUNCE] Apache Juneau {v} released", "\n\n".join(parts) + "\n"
