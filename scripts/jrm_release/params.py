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
"""Release parameters from a YAML file: scaffold, locate, load, validate, secrets and the record copy.

A params file replaces the details form of a new release (see ../release.md).  It never holds a secret: those come from
JRM_* environment variables, the macOS keychain or `gh` (ctx.find_secret).  yaml is imported lazily, so importing this
module never fails; only load() needs PyYAML (release.py checks for it as a prerequisite)."""
import json
import os
import tempfile
import types
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

from . import details, gpgkeys, inventory

SECRET_ENV = {"credential.apache.password": "JRM_APACHE_PASSWORD",
              "credential.gpg.passphrase": "JRM_GPG_PASSPHRASE",
              "credential.github.token": "JRM_GITHUB_TOKEN"}
SECRET_WORDS = ("password", "passphrase", "token", "secret")
TEXT = ("branch", "developmentVersion", "highlights", "knownIssues", "acknowledgements")
VOTE_KEYS = ("plus1", "zero", "minus1", "binding")
ALLOWED = {None: ("apache", "acknowledgements", "branch", "developmentVersion", "gpg", "highlights", "knownIssues", "rc",
                  "summary", "version", "vote"),
           "apache": ("availid", "email"), "gpg": ("keyid",), "vote": VOTE_KEYS}
CREDS = {"apache.availid": "credential.apache.availid", "apache.email": "credential.apache.email",
         "gpg.keyid": "credential.gpg.keyid"}
SECRET_HINTS = {"credential.apache.password": "Apache password (JRM_APACHE_PASSWORD or keychain item jrm.apache.password)",
                "credential.gpg.passphrase": "GPG passphrase (JRM_GPG_PASSPHRASE or keychain item jrm.gpg.passphrase)",
                "credential.github.token": "GitHub token (JRM_GITHUB_TOKEN, keychain item jrm.github.token, "
                                           "or gh auth login)"}
PLACEHOLDER = "TODO"


class ParamsError(Exception):
    """A params file or flag combination that cannot start a release.  The message is printed as `❌ <message>`."""


@dataclass
class Params:
    path: Path
    version: str
    details: dict                 # the form's fields as the file gave them (branch and developmentVersion may be blank)
    creds: dict = field(default_factory=dict)   # the non-secret credential.* values


def _secret_key(node, prefix=""):
    """The dotted name of the first key anywhere in node whose name looks like a secret, else None."""
    if isinstance(node, dict):
        for k, v in node.items():
            name = f"{prefix}{k}"
            if any(w in str(k).lower() for w in SECRET_WORDS):
                return name
            found = _secret_key(v, name + ".")
            if found:
                return found
    elif isinstance(node, list):
        for v in node:
            found = _secret_key(v, prefix)
            if found:
                return found
    return None


def _read(path):
    try:
        import yaml
    except ImportError as e:   # prereqs.check reports a missing PyYAML first; this is the safety net
        raise ParamsError("PyYAML: not installed; --params needs it (python3 -m pip install pyyaml)") from e
    try:
        text = Path(path).read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as e:
        raise ParamsError(f"--params {path}: cannot read {path}: {getattr(e, 'strerror', None) or e}") from e
    try:
        return yaml.safe_load(text)
    except yaml.YAMLError as e:
        mark = getattr(e, "problem_mark", None)
        where = f" (line {mark.line + 1}, column {mark.column + 1})" if mark else ""
        raise ParamsError(f"{path}: not valid YAML: {getattr(e, 'problem', None) or e}{where}") from e


def _string(path, doc, key, label=None):
    """doc[key] as a stripped string; '' when absent or null."""
    v = doc.get(key)
    if v is None:
        return ""
    if not isinstance(v, str):
        hint = " (quote it)" if key in ("version", "developmentVersion") else ""
        raise ParamsError(f"{path}: '{label or key}' must be a string{hint}")
    return v.strip()


def load(path):
    """Read and structurally validate a params file; Params.  Raises ParamsError.  Everything the other modules own
    (x.y.z, dev > version, rc >= 1) is checked later by resolve(), through details.validate."""
    doc = _read(path)
    if not isinstance(doc, dict):
        raise ParamsError(f"{path}: top level must be a mapping of release parameters")
    secret = _secret_key(doc)
    if secret:
        raise ParamsError(f"{path}: '{secret}' is a secret and must not be in the file; set JRM_APACHE_PASSWORD "
                          "(JRM_GPG_PASSPHRASE, JRM_GITHUB_TOKEN) in the environment instead")
    for group in (None, "apache", "gpg", "vote"):
        node = doc if group is None else doc.get(group)
        if group and node is not None and not isinstance(node, dict):
            raise ParamsError(f"{path}: '{group}' must be a mapping")
        for k in node or {}:
            if k not in ALLOWED[group]:
                raise ParamsError(f"{path}: unknown key '{group + '.' if group else ''}{k}' "
                                  f"(allowed: {', '.join(ALLOWED[group])})")
    _vote_types(doc.get("vote"), path)
    apache, gpg = doc.get("apache") or {}, doc.get("gpg") or {}
    values = {"version": doc.get("version"), "rc": doc.get("rc"), "summary": doc.get("summary"),
              "apache.availid": apache.get("availid"), "gpg.keyid": gpg.get("keyid")}
    missing = [k for k, v in values.items() if v is None or (isinstance(v, str) and not v.strip())]
    if missing:
        raise ParamsError(f"{path}: missing required key(s): {', '.join(missing)}")
    rc = doc["rc"]
    if isinstance(rc, bool) or not isinstance(rc, int):
        raise ParamsError(f"{path}: 'rc' must be a whole number")
    d = {"version": _string(path, doc, "version"), "rc": rc, "summary": _string(path, doc, "summary")}
    for key in TEXT:
        d[key] = _string(path, doc, key)
    creds = {}
    for key, cred in CREDS.items():
        group, name = key.split(".")
        v = _string(path, doc.get(group) or {}, name, key)
        if v:
            creds[cred] = v
    todo = [k for k, v in {**d, **{k: creds.get(c, "") for k, c in CREDS.items()}}.items()
            if isinstance(v, str) and v.startswith(PLACEHOLDER)]
    if todo:
        names = ", ".join(f"'{k}'" for k in todo)
        raise ParamsError(f"{path}: {names} still contain{'s' if len(todo) == 1 else ''} a TODO placeholder; "
                          f"replace {'it' if len(todo) == 1 else 'them'} (or clear {'the optional key' if len(todo) == 1 else 'optional keys'})")
    return Params(path=Path(path), version=d["version"], details=d, creds=creds)


def _is_todo(v):
    return isinstance(v, str) and v.strip().startswith(PLACEHOLDER)


def _vote_types(node, path):
    """ParamsError for a present, non-TODO vote value of the wrong type (missing values and TODO placeholders pass, so a
    half-filled block never stops a release from starting or being checked)."""
    if not isinstance(node, dict):
        return                                   # absent/empty is fine; a non-mapping was already rejected by load
    for key in ("plus1", "zero", "minus1"):
        v = node.get(key)
        if v is not None and not _is_todo(v) and (isinstance(v, bool) or not isinstance(v, int) or v < 0):
            raise ParamsError(f"{path}: vote.{key} must be a whole number >= 0")
    names = node.get("binding")
    if names is not None and names != [] and not _is_todo(names):
        if not isinstance(names, list) or not all(isinstance(n, str) for n in names):
            raise ParamsError(f"{path}: vote.binding must be a list of names")


def resolve(ctx, loaded, cli_version):
    """The final details dict (branch plus details.FIELDS), from a loaded file.
    Checks --version against the file, defaults the branch and the development version, then details.validate."""
    p, d = loaded.path, loaded.details
    if cli_version and cli_version != loaded.version:
        raise ParamsError(f"--version {cli_version} does not match version {loaded.version} in {p}")
    version = d["version"]
    dev = details.normalize_dev(version, d["developmentVersion"])
    if dev is None and inventory.parse_version(version):
        raise ParamsError(f"{p}: developmentVersion is required when version ends in .0 (for example 9.3.0-SNAPSHOT)")
    out = {"branch": d["branch"] or details.branch_default(ctx, version),
           **{k: d.get(k) for k in details.FIELDS}, "developmentVersion": dev}
    err = details.validate(out)
    if err:
        raise ParamsError(f"{p}: {err}")
    return out


def require(args):
    """The params file a new release (or --check) uses: --params FILE, else <home>/params/<version>.yml.  ParamsError
    (exit 2) when there is none: a new release is always params-driven."""
    if args.params:
        return Path(args.params)
    if args.version:
        conv = Path(args.home) / "params" / f"{args.version}.yml"
        if conv.exists():
            return conv
        raise ParamsError(f"no params file {conv}; run --init-params --version {args.version}")
    raise ParamsError("no params file; run --init-params --version <version>")


def record(path, run_dir):
    """Copy the params file used to <run_dir>/params.yml (byte for byte, atomically), as a record of the release.
    Skipped when the file already is that path.  OSError on failure; the caller warns."""
    target = Path(run_dir) / "params.yml"
    if target.exists() and os.path.samefile(path, target):
        return
    fd, tmp = tempfile.mkstemp(dir=run_dir, prefix="params.yml.")
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(Path(path).read_bytes())
        os.replace(tmp, target)
    except BaseException:
        if os.path.exists(tmp):
            os.unlink(tmp)
        raise


def vote_block(path):
    """The `vote:` mapping of a params file, or None when absent or empty.  ParamsError when the file cannot be read or
    the block is not a mapping."""
    doc = _read(path)
    if not isinstance(doc, dict):
        raise ParamsError(f"{path}: top level must be a mapping of release parameters")
    node = doc.get("vote")
    if node is not None and not isinstance(node, dict):
        raise ParamsError(f"{path}: 'vote' must be a mapping")
    return node


def _count(node, key, required):
    """(value, problem) for one tally count."""
    v = node.get(key)
    if v is None:
        return (None, f"vote.{key} is required (a whole number)") if required else (0, None)
    if isinstance(v, bool) or not isinstance(v, int) or v < 0:     # includes a TODO placeholder string
        return None, f"vote.{key} must be a whole number >= 0"
    return v, None


def check_vote(node):
    """(tally, problems) for a parsed `vote:` mapping (None or {} counts as empty).  tally is {plus1, zero, minus1,
    binding} when problems is empty, else None.  problems are "what to fill in" strings."""
    node = node or {}
    problems = [f"unknown key 'vote.{k}' (allowed: {', '.join(VOTE_KEYS)})" for k in node if k not in VOTE_KEYS]
    tally = {}
    for key in ("plus1", "zero", "minus1"):
        tally[key], problem = _count(node, key, key == "plus1")
        if problem:
            problems.append(problem)
    names = node.get("binding")
    if names is None:
        problems.append("vote.binding is required (a list of the binding voters' names)")
    elif not isinstance(names, list) or not names or not all(isinstance(n, str) and n.strip() for n in names):
        problems.append("vote.binding must be a non-empty list of names")
    elif any(_is_todo(n) for n in names):
        problems.append("vote.binding still contains a TODO placeholder")
    else:
        tally["binding"] = [n.strip() for n in names]
    return (None, problems) if problems else (tally, [])


def load_vote(path):
    """(tally, problems) from the `vote:` block of a params file; an unreadable or malformed file is one problem."""
    try:
        node = vote_block(path)
    except ParamsError as e:
        return None, [str(e)]
    return check_vote(node)


# ---- --init-params -------------------------------------------------------------------------------------------------

HEADER = """\
# Juneau release parameters for {version}.
# Run it with:  python3 scripts/release.py --version {version}   (when this file is <home>/params/{version}.yml)
#               python3 scripts/release.py --params FILE
# This file must not contain secrets.  They come from the environment or, on macOS, the login keychain.  Store them once
# (each command prompts for the value, so it is never on a command line or in your shell history):
#   security add-generic-password -a <apache id> -s jrm.apache.password -w
#   security add-generic-password -a <apache id> -s jrm.gpg.passphrase -w
#   security add-generic-password -a default -s jrm.github.token -w
# or export JRM_APACHE_PASSWORD, JRM_GPG_PASSPHRASE and JRM_GITHUB_TOKEN.  `gh auth login` can replace the GitHub token item.
# Replace every TODO value below (or delete an optional key); a TODO left in the file stops the release from starting.
"""


def _q(value):
    """A YAML double-quoted scalar (JSON string syntax is valid YAML)."""
    return json.dumps(value, ensure_ascii=False)


def _todo(what):
    return _q(f"{PLACEHOLDER}: {what}")


def render_scaffold(version, branch, dev, rc, email, availid, keyid, key_note=""):
    """The commented template text.  A None email, availid or keyid becomes a TODO placeholder; a blank dev is left
    empty with a note.  Built by formatting (yaml.dump would drop the comments); no secret is ever written."""
    dev_note = ("# Next development version, x.y.z-SNAPSHOT (blank = next patch).\n" if dev else
                "# Next development version, x.y.z-SNAPSHOT.  It cannot be derived for an x.y.0 version: set it.\n")
    return HEADER.format(version=version) + f"""
# The release branch.
branch: {_q(branch)}
# The release version, x.y.z.
version: {_q(version)}
{dev_note}developmentVersion: {_q(dev or "")}
# The release candidate number.
rc: {rc}
# Your Apache identity; availid is also the keychain account for the Apache password and the GPG passphrase.
apache:
  # Your Apache id.
  availid: {_q(availid) if availid else _todo("your Apache id")}
  # Your Apache email address (the release commits and the emails use it).
  email: {_q(email) if email else _todo("your Apache email address")}
# The signing key.
gpg:
  # The 16-hex long id (or full fingerprint) of the [SC] key to sign with.{key_note}
  keyid: {_q(keyid) if keyid else _todo("your GPG key id")}
# One or two sentences describing the release (required).
summary: {_todo("one or two sentences describing the release")}
# Release highlights (delete or leave empty if none).
highlights: {_todo("highlights, one per line")}
# Known issues (delete or leave empty if none).
knownIssues: {_todo("known issues")}
# Acknowledgements (delete or leave empty if none).
acknowledgements: {_todo("acknowledgements")}
# After the vote closes, uncomment and fill in, then run:  release.py --resume --version {version}
# vote:
#   plus1: 3          # binding +1 votes (whole number)
#   zero: 0           # 0 votes
#   minus1: 0         # -1 votes
#   binding:          # names of the binding voters, one per entry
#     - Jane Doe
"""


def _signing_keys(ctx):
    r = ctx.runner.quiet(["gpg", "--list-secret-keys", "--with-colons", "--keyid-format", "long"], title="List GPG keys")
    now = ctx.now().timestamp()
    return [k for k in (gpgkeys.parse(r.out) if r.ok else [])
            if "s" in k.caps.lower() and k.validity not in ("r", "e", "i", "d") and not (k.expires and k.expires <= now)]


def scaffold(ctx, version):
    """The template for a version, prefilled from read-only commands through ctx.runner."""
    dev = inventory.next_dev_version(version)
    r = ctx.runner.quiet(["git", "config", "user.email"], title="git user.email")
    email = r.out.strip() if r.ok else ""
    mine = email.endswith("@apache.org") and len(email) > len("@apache.org")
    keys = _signing_keys(ctx)
    keyid = keys[0].keyid if len(keys) == 1 else None
    note = f"\n  # Signing keys found: {', '.join(k.keyid for k in keys)}." if len(keys) > 1 else ""
    return render_scaffold(version, details.branch_default(ctx, version), dev, 1, email if mine else None,
                           email.split("@")[0] if mine else None, keyid, note)


def init(args, runner, out=print):
    """--init-params: write the template to the convention path (or --params FILE) and print the next command; the exit
    code (0).  Never overwrites (ParamsError).  Needs no PyYAML, session or run state."""
    version = args.version
    target = Path(args.params) if args.params else Path(args.home) / "params" / f"{version}.yml"
    if target.exists():
        raise ParamsError(f"{target} already exists; not overwriting (edit it, or delete it to start over)")
    pseudo = types.SimpleNamespace(runner=runner, now=lambda: datetime.now().astimezone())
    text = scaffold(pseudo, version)
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=target.parent, prefix=target.name + ".")
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(text)
        os.replace(tmp, target)
    except BaseException:
        if os.path.exists(tmp):
            os.unlink(tmp)
        raise
    nxt = f"--params {target}" if args.params else f"--version {version}"
    out(f"Wrote {target}.  Fill in the TODO values, then run: python3 scripts/release.py {nxt}")
    return 0
