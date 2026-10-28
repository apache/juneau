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
"""Secret lookups outside the prompt: the macOS login keychain (`security`) and `gh auth token`.

Every lookup goes through ctx.runner.quiet (nothing echoed, nothing in full.log) with a short timeout, and never raises:
a failure is "not found" plus one log line naming the item and the failure class, never the tool's output."""
import shutil
import sys

TIMEOUT = 10   # seconds; the access dialog needs a human, so a longer wait is pointless
SERVICES = {"credential.apache.password": "jrm.apache.password",
            "credential.gpg.passphrase": "jrm.gpg.passphrase",
            "credential.github.token": "jrm.github.token"}
DENIED = (36, 51, 128)   # security's exit codes for a locked keychain or a refused access dialog


def _reason(code):
    if code == 124:
        return "timed out"
    if code == 44:
        return "not found"
    if code in DENIED:
        return "denied or locked"
    return f"error (exit {code})"


def _value(ctx, r, label):
    """The trimmed output of a successful lookup, else None with one log line."""
    value = r.out.rstrip("\r\n") if r.ok else ""
    if value:
        return value
    ctx.say(f"{label} not available ({_reason(r.code) if not r.ok else 'not found'})")
    return None


def lookup(ctx, service, account):
    """The generic-password item (service, account) from the login keychain, or None.  macOS only; elsewhere skipped
    silently.  The secret is only ever read from stdout (`-w` last, no value), never put on a command line."""
    if sys.platform != "darwin" or shutil.which("security") is None:
        return None
    r = ctx.runner.quiet(["security", "find-generic-password", "-s", service, "-a", account, "-w"],
                         title=f"Keychain {service}", timeout=TIMEOUT)
    return _value(ctx, r, f"keychain: {service}")


def gh_token(ctx):
    """The GitHub token of the current `gh auth login`, or None."""
    r = ctx.runner.quiet(["gh", "auth", "token", "--hostname", "github.com"], title="gh auth token", timeout=TIMEOUT)
    return _value(ctx, r, "gh: auth token")
