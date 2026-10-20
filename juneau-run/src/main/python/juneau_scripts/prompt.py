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
"""Prompts for a release script: text, secret, confirm, choice, form and email.  Two transports, chosen once.  When a
host starts the script with JUNEAU_PROMPT_FD_OUT and JUNEAU_PROMPT_FD_IN (docs/prompt-protocol-v1.md), every prompt is a
JSON line to the host and the answer is the line that comes back.  Otherwise the prompts are inline: they pause the
console view, so the prompt isn't drawn over, and use plain input() and getpass() when juneau_run isn't loaded.

None always means cancelled: Ctrl-D inline, a cancel answer or EOF on the channel.  A secret is never published,
logged, written to a checkpoint or put on a command line."""

import contextlib
import dataclasses
import getpass
import json
import os
import sys
import urllib.parse

from . import _base

FD_OUT_ENV = "JUNEAU_PROMPT_FD_OUT"
FD_IN_ENV = "JUNEAU_PROMPT_FD_IN"
MAX_LINE = 1 << 20
MAILTO_LIMIT = 1800
RULE_WIDTH = 72

current_step = None   # set by steps.Registry.run: the protocol's "step" field
_chan = None          # None = unresolved, False = inline, else a _Channel
_hello = {}


@dataclasses.dataclass(frozen=True)
class Field:
    """One field of form().  type is "text" or "int" (the answer is then an int)."""
    name: str
    label: str
    default: str | None = None
    required: bool = False
    secret: bool = False
    multiline: bool = False
    type: str = "text"

    def kind(self):
        """The field's kind on the wire."""
        return "secret" if self.secret else "multiline" if self.multiline else self.type


def _warn(text):
    """A diagnostic on stderr.  Never include prompt or answer content."""
    jr = _base.jr()
    if jr is not None:
        jr.warn(text)
    else:
        print(f"juneau-run: {text}", file=sys.stderr, flush=True)


class _Channel:
    def __init__(self, fd_out, fd_in):
        self.out = os.fdopen(fd_out, "w", encoding="utf-8", buffering=1)
        self.inp = os.fdopen(fd_in, "r", encoding="utf-8", errors="replace")
        self.n = 0
        self.dead = False
        self.hello_sent = False

    def close(self):
        self.dead = True
        for f in (self.out, self.inp):
            try:
                f.close()
            except (OSError, ValueError):
                pass

    def send(self, obj):
        if self.dead:
            return
        line = json.dumps(obj, separators=(",", ":"), ensure_ascii=False)
        try:
            self.out.write(line + "\n")
            self.out.flush()
        except (OSError, ValueError):
            self.dead = True

    def recv(self):
        """The next JSON object from the host, or None at EOF / protocol error (the channel is then dead)."""
        while not self.dead:
            try:
                line = self.inp.readline(MAX_LINE + 1)
            except (OSError, ValueError):
                self.dead = True
                return None
            if not line:
                self.dead = True
                return None
            if len(line) > MAX_LINE and not line.endswith("\n"):
                while line and not line.endswith("\n"):   # drain the rest of the oversize line
                    line = self.inp.readline(MAX_LINE)
                _warn("prompt channel: dropped an oversize line")
                continue
            try:
                msg = json.loads(line)
            except ValueError:
                _warn(f"prompt channel: dropped a line that is not JSON ({len(line)} characters)")
                continue
            if not isinstance(msg, dict):
                continue
            if msg.get("v") != 1:
                _warn("prompt channel: unsupported protocol version, treating as end of input")
                self.dead = True
                return None
            return msg
        return None

    def exchange(self, kind, message, body):
        """Send one prompt; the matching answer dict, or None for cancel/EOF."""
        if not self.hello_sent:
            self.send({"v": 1, "type": "hello", "pid": os.getpid(),
                       "script": os.path.basename(sys.argv[0] or "script"), **_hello})
            self.hello_sent = True
        self.n += 1
        pid = f"p{self.n}"
        wire = {"v": 1, "type": "prompt", "id": pid, "kind": kind, "message": message}
        if current_step:
            wire["step"] = current_step
        wire.update({k: v for k, v in body.items() if v is not None})   # a missing field means null
        self.send(wire)
        while True:
            msg = self.recv()
            if msg is None:
                return None
            if msg.get("type") != "answer":
                continue                                   # unknown types are ignored
            if msg.get("id") != pid:
                _warn(f"prompt channel: dropped an answer for {str(msg.get('id'))[:16]}, waiting for {pid}")
                continue
            return None if msg.get("cancel") else msg


def channel():
    """The prompt channel, resolved once: JUNEAU_PROMPT_FD_OUT/_IN are read and removed from the environment (so
    children never inherit them), the fds validated with os.fstat and made non-inheritable.  None means inline."""
    global _chan
    if _chan is None:
        out, inp = os.environ.pop(FD_OUT_ENV, None), os.environ.pop(FD_IN_ENV, None)
        _chan = False
        if out is not None or inp is not None:
            try:
                fds = (int(out), int(inp))
                for fd in fds:
                    os.fstat(fd)
                    os.set_inheritable(fd, False)
                _chan = _Channel(*fds)
            except (TypeError, ValueError, OSError):
                _warn("prompt channel: bad descriptors, prompting on the terminal")
    return _chan or None


def _reset():
    """Forget the resolved channel and close its fds (tests)."""
    global _chan, _hello, current_step
    if _chan:
        _chan.close()
    _chan, _hello, current_step = None, {}, None


def hello(**fields):
    """Extra fields (e.g. attempt=2) for the hello line sent before the first prompt.  Optional."""
    _hello.update(fields)


def _paused():
    jr = _base.jr()
    return jr.paused() if jr is not None else contextlib.nullcontext()


def _show(line):
    """A line of inline prompt output, printed with the console view paused so its redraw can't overdraw it."""
    with _paused():
        print(line, flush=True)


def _text_prompt(msg, default):
    """msg as typed at the terminal: ' [default]: ' (or ': ') appended.  A msg with a colon-space in it or ending in a
    colon or whitespace (a caller's finished prompt, even one ending in a typed-in prefix) is left alone, and an empty default shows nothing."""
    if ": " in msg or msg.endswith(":") or msg[-1:].isspace():
        return msg
    return f"{msg} [{default}]: " if default else f"{msg}: "


def _ask(msg):
    """One line from the terminal, or None on EOF."""
    jr = _base.jr()
    try:
        return jr.ask(msg) if jr is not None else input(msg)
    except EOFError:
        return None


def _getpass(msg):
    """getpass() with the view paused, or None on EOF.  The answer is never published or logged."""
    with _paused():
        try:
            return getpass.getpass(msg)
        except EOFError:
            return None


def _ask_channel(chan, kind, message, body, check=None):
    """exchange() until check(value) accepts: check returns an error string to re-ask with, or None.  Returns
    (value, source), or None when cancelled."""
    while True:
        ans = chan.exchange(kind, message, body)
        if ans is None:
            return None
        value = ans.get("value")
        problem = check(value) if check is not None else None
        if problem is None:
            return value, ans.get("source")
        body = {**body, "error": problem}


def _record(message, shown, secret, source):
    """The transcript line (channel mode only; inline mode already echoed on the terminal) and, for non-secret
    answers, a bus 'ask' event.  A secret appears only as ********."""
    suffix = f" ({source})" if secret and source else ""
    print(f"? {message}: {'********' if secret else shown}{suffix}", flush=True)
    jr = _base.jr()
    if jr is not None and not secret:
        jr.publish("ask", prompt=message, answer=shown)


def text(msg, *, default=None, key=None, title=None, error=None):
    """The stripped answer.  An empty answer gives default; EOF or cancel gives None, whatever the default."""
    chan = channel()
    if chan is None:
        if error:
            _show(error)
        answer = _ask(_text_prompt(msg, default))
        return None if answer is None else (answer.strip() or default)
    with _paused():
        got = _ask_channel(chan, "text", msg, {"key": key, "title": title, "default": default, "error": error})
        if got is None:
            return None
        value, source = got
        value = ("" if value is None else str(value)).strip() or default
        _record(msg, value, False, source)
        return value


def yes_no_hint(default):
    """The yes/no hint for a confirm: (y/N) when default is False, (Y/n) when True."""
    return "(Y/n)" if default else "(y/N)"


def confirm(msg, *, default=False, key=None, title=None, expect=None, danger=False):
    """True for y or yes (any case); an empty answer or EOF gives default; anything else is False.  With expect,
    True only when the typed text equals expect (EOF or cancel then gives None).  The yes/no hint, (y/N) or (Y/n),
    follows msg on the terminal and rides in the channel's prompt body as hint; with expect there is none."""
    chan = channel()
    hint = None if expect is not None else yes_no_hint(default)
    if chan is None:
        answer = _ask(msg if hint is None else f"{msg.rstrip()} {hint} ")
        if expect is not None:
            return None if answer is None else answer.strip() == expect
        if answer is None or not answer.strip():
            return default
        return answer.strip().lower() in ("y", "yes")
    with _paused():
        got = _ask_channel(chan, "confirm", msg, {"key": key, "title": title, "default": default, "expect": expect,
                                                  "hint": hint, "danger": True if danger else None})
        if got is None:
            return None
        value, source = got
        if expect is not None:
            result = value == expect
        else:
            result = default if value is None else value in (True, "y", "yes", "Y", "YES", "true")
        _record(msg, "yes" if result else "no", False, source)
        return result


def secret(msg, *, key=None, title=None):
    """The answer, read with the view paused.  It is never published on juneau_run's bus or logged.  None on EOF."""
    chan = channel()
    if chan is None:
        return _getpass(msg)
    with _paused():
        got = _ask_channel(chan, "secret", msg, {"key": key, "title": title})
        if got is None:
            return None
        value, source = got
        _record(msg, "", True, source)
        return "" if value is None else str(value)


def _options(options):
    return [{"value": o[0], "label": o[1]} if isinstance(o, (tuple, list)) else {"value": o, "label": str(o)}
            for o in options]


def choice(msg, options, *, default=None, key=None, title=None):
    """One of the option values.  options: [(value, label), ...] or [value, ...].  Inline, the answer is a value or
    its 1-based number."""
    opts = _options(options)
    values = [o["value"] for o in opts]
    chan = channel()
    if chan is None:
        for i, o in enumerate(opts, 1):
            print(f"  {i}) {o['label']}" + (f" ({o['value']})" if o["label"] != o["value"] else ""), flush=True)
        suffix = f" [{default}]" if default is not None else ""
        while True:
            answer = _ask(f"{msg}{suffix}: ")
            if answer is None:
                return None
            answer = answer.strip()
            if not answer and default is not None:
                return default
            if answer in values:
                return answer
            if answer.isdigit() and 1 <= int(answer) <= len(values):
                return values[int(answer) - 1]
            _show("Choose one of the listed options.")

    def check(value):
        return None if value in values else "Choose one of the listed options"

    with _paused():
        got = _ask_channel(chan, "choice", msg, {"key": key, "title": title, "options": opts, "default": default},
                           check)
        if got is None:
            return None
        value, source = got
        _record(msg, value, False, source)
        return value


def _clean_field(f, raw):
    """(value, error) for a form field's raw answer.  The error names the label, never the content."""
    if raw is None:
        raw = ""
    raw = str(raw)
    if not f.secret:
        raw = raw.strip()
    if not raw and f.default is not None:
        raw = str(f.default)
    if not raw:
        return (None, f"{f.label} is required") if f.required else ("" if f.type != "int" else None, None)
    if f.type == "int":
        try:
            return int(raw), None
        except ValueError:
            return None, f"{f.label} must be a whole number"
    return raw, None


def _inline_field(f):
    """One form field read from the terminal: (value, cancelled).  Re-asks until it is valid."""
    suffix = f" [{f.default}]" if f.default is not None and not f.secret else ""
    while True:
        if f.multiline:
            _show(f"{f.label} (end with a line holding only '.'):")
            lines = []
            while True:
                line = _ask("> ")
                if line is None:
                    return None, True
                if line.strip() == ".":
                    break
                lines.append(line)
            raw = "\n".join(lines)
        elif f.secret:
            raw = _getpass(f"{f.label}: ")
        else:
            raw = _ask(f"{f.label}{suffix}: ")
        if raw is None:
            return None, True
        value, problem = _clean_field(f, raw)
        if problem is None:
            return value, False
        _show(problem)


def form(msg, fields, *, key=None, title=None):
    """A dict of field name to answer (an int for an int field, a str otherwise); None when cancelled."""
    fields = list(fields)
    chan = channel()
    if chan is None:
        print(msg, flush=True)
        result = {}
        for f in fields:
            value, cancelled = _inline_field(f)
            if cancelled:
                return None
            result[f.name] = value
        return result
    result = {}

    def check(value):
        if not isinstance(value, dict):
            return "Expected the form's fields"
        result.clear()
        for f in fields:
            cleaned, problem = _clean_field(f, value.get(f.name))
            if problem is not None:
                return problem
            result[f.name] = cleaned
        return None

    wire = [{k: v for k, v in (("name", f.name), ("label", f.label), ("kind", f.kind()), ("default", f.default),
                               ("required", True if f.required else None)) if v is not None} for f in fields]
    with _paused():
        got = _ask_channel(chan, "form", msg, {"key": key, "title": title or msg, "fields": wire}, check)
        if got is None:
            return None
        shown = ", ".join(f"{f.name}={'********' if f.secret else result[f.name]}" for f in fields)
        _record(msg, shown, False, got[1])
        return dict(result)


def _mailto(to, cc, subject, body):
    """A mailto: URL, or None when it would be too long for a mail client to take."""
    query = [("subject", subject), ("body", body)] + ([("cc", ",".join(cc))] if cc else [])
    url = "mailto:" + ",".join(to) + "?" + "&".join(f"{k}={urllib.parse.quote(v, safe='')}" for k, v in query)
    return url if len(url) <= MAILTO_LIMIT else None


def _email_block(to, cc, subject, body, mailto):
    """The text the inline email prompt prints: rules, To, Cc, Subject, a blank line, the body and the link."""
    lines = [f" Email: {subject} ".center(RULE_WIDTH, "─"), f"To: {', '.join(to)}"]
    if cc:
        lines.append(f"Cc: {', '.join(cc)}")
    lines += [f"Subject: {subject}", "", body, "─" * RULE_WIDTH,
              f"mailto: {mailto}" if mailto else "(body too long for a link; paste it)"]
    return "\n".join(lines)


def _listed(addresses):
    return [addresses] if isinstance(addresses, str) else list(addresses)


def email(*, to, subject, body, cc=(), key=None, title=None):
    """Show an email for the user to send from their own mail client.  True when they say it was sent, False when
    not yet, None when cancelled."""
    to, cc = _listed(to), _listed(cc)
    mailto = _mailto(to, cc, subject, body)
    chan = channel()
    if chan is None:
        print(_email_block(to, cc, subject, body, mailto), flush=True)
        return confirm("Sent it from your apache.org address?", default=False)
    wire = {"to": to, "cc": cc, "subject": subject, "body": body, "mailto": mailto}
    with _paused():
        got = _ask_channel(chan, "email", f"Email: {subject}", {"key": key, "title": title, "email": wire})
        if got is None:
            return None
        sent = got[0] == "sent"
        _record(f"Email: {subject}", "sent" if sent else "not yet", False, got[1])
        return sent
