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
"""release.py --params FILE: the params file (load, validate, resolve, scaffold, record) and its CLI wiring."""
import os
from argparse import Namespace

import pytest

yaml = pytest.importorskip("yaml")

from jrm_release import details, params  # noqa: E402

pytestmark = pytest.mark.usefixtures("clean_env")

CHECKOUT = os.path.expanduser("~/git/apache/juneau")
FIELDS_ORDER = ["branch", *details.FIELDS]

GOOD = """\
# release-9.2.1.yml
branch: juneau-9.2.1-branch
version: 9.2.1
developmentVersion: 9.2.2-SNAPSHOT
rc: 1
apache: {availid: jbognar, email: jamesbognar@apache.org}
gpg: {keyid: ABCD1234ABCD1234}
summary: |
  Security and maintenance release.
highlights: |
  - DirectoryResource hardened.
knownIssues: ""
acknowledgements: ""
"""


def write(tmp_path, text, name="rel.yml"):
    p = tmp_path / name
    p.write_text(text)
    return p


def good(tmp_path, **replace):
    text = GOOD
    for old, new in replace.items():
        text = text.replace(old.replace("__", " "), new)
    return write(tmp_path, text)


def load_error(path):
    with pytest.raises(params.ParamsError) as e:
        params.load(path)
    return str(e.value)


# 1 -------------------------------------------------------------------------------------------------------------------

def test_valid_file_loads(tmp_path):
    p = good(tmp_path)
    loaded = params.load(p)
    assert loaded.version == "9.2.1"
    assert loaded.creds == {"credential.apache.availid": "jbognar", "credential.apache.email": "jamesbognar@apache.org",
                            "credential.gpg.keyid": "ABCD1234ABCD1234"}
    assert loaded.details["summary"] == "Security and maintenance release."      # stripped
    assert loaded.details["highlights"] == "- DirectoryResource hardened."


def test_resolve_has_the_details_ask_shape_and_asks_nothing(tmp_path, make_ctx):
    ctx = make_ctx()
    d = params.resolve(ctx, params.load(good(tmp_path)), None)
    assert list(d) == FIELDS_ORDER
    assert d == {"branch": "juneau-9.2.1-branch", "version": "9.2.1", "developmentVersion": "9.2.2-SNAPSHOT", "rc": 1,
                 "summary": "Security and maintenance release.", "highlights": "- DirectoryResource hardened.",
                 "knownIssues": "", "acknowledgements": ""}
    assert ctx.prompt.asked == []


def test_absent_optional_text_fields_default_to_empty(tmp_path, make_ctx):
    p = write(tmp_path, "version: 9.2.1\nrc: 2\nsummary: s\nbranch: b\napache: {availid: jb}\ngpg: {keyid: A}\n")
    d = params.resolve(make_ctx(), params.load(p), None)
    assert (d["highlights"], d["knownIssues"], d["acknowledgements"], d["rc"]) == ("", "", "", 2)
    assert params.load(p).creds == {"credential.apache.availid": "jb", "credential.gpg.keyid": "A"}


# 2 -------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize("drop, name", [("version: 9.2.1\n", "version"), ("rc: 1\n", "rc"),
                                        ("summary: |\n  Security and maintenance release.\n", "summary"),
                                        ("apache: {availid: jbognar, email: jamesbognar@apache.org}\n", "apache.availid")])
def test_each_missing_required_key_is_named(tmp_path, drop, name):
    msg = load_error(write(tmp_path, GOOD.replace(drop, "")))
    assert f"missing required key(s): {name}" in msg
    assert str(tmp_path / "rel.yml") in msg


def test_all_missing_required_keys_are_listed_together(tmp_path):
    msg = load_error(write(tmp_path, "branch: b\n"))
    assert "missing required key(s): version, rc, summary, apache.availid" in msg


def test_blank_summary_counts_as_missing(tmp_path):
    msg = load_error(write(tmp_path, GOOD.replace("summary: |\n  Security and maintenance release.", 'summary: "  "')))
    assert "summary" in msg and "missing required" in msg


# 3 -------------------------------------------------------------------------------------------------------------------

def test_unknown_top_level_key(tmp_path):
    msg = load_error(write(tmp_path, GOOD + "knownIssue: x\n"))
    assert "unknown key 'knownIssue'" in msg
    assert "allowed: " in msg and "developmentVersion" in msg


def test_unknown_nested_key(tmp_path):
    msg = load_error(write(tmp_path, GOOD.replace("apache: {availid: jbognar,", "apache: {availid: jbognar, id: x,")))
    assert "unknown key 'apache.id'" in msg
    msg = load_error(write(tmp_path, GOOD.replace("gpg: {keyid: ABCD1234ABCD1234}", "gpg: {key: ABCD1234ABCD1234}")))
    assert "unknown key 'gpg.key'" in msg


def test_apache_and_gpg_must_be_mappings(tmp_path):
    assert "'apache' must be a mapping" in load_error(write(tmp_path, GOOD.replace(
        "apache: {availid: jbognar, email: jamesbognar@apache.org}", "apache: jbognar")))


# 4 -------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize("extra, key", [
    ("apache: {availid: jb, password: hunter2}", "apache.password"),
    ("apache: {availid: jb}\ngpg: {passphrase: hunter2}", "gpg.passphrase"),
    ("apache: {availid: jb}\ngithub: {token: ghp_x}", "github.token"),
    ("apache: {availid: jb}\ngpg: {keyid: A, nested: {deeper: {clientSecret: x}}}", "gpg.nested.deeper.clientSecret"),
    ("apache: {availid: jb}\nAPACHE_PASSWORD: x", "APACHE_PASSWORD"),
])
def test_secret_keys_are_rejected_with_the_env_vars(tmp_path, extra, key):
    base = "version: 9.2.1\nrc: 1\nsummary: s\n"
    msg = load_error(write(tmp_path, base + extra + "\n"))
    assert f"'{key}' is a secret and must not be in the file" in msg
    assert "JRM_APACHE_PASSWORD" in msg and "JRM_GPG_PASSPHRASE" in msg and "JRM_GITHUB_TOKEN" in msg
    assert "hunter2" not in msg and "ghp_x" not in msg                    # the value is never echoed


# 5 -------------------------------------------------------------------------------------------------------------------

def test_version_mismatch_and_match(tmp_path, make_ctx):
    loaded = params.load(good(tmp_path))
    with pytest.raises(params.ParamsError) as e:
        params.resolve(make_ctx(), loaded, "9.2.2")
    assert f"--version 9.2.2 does not match version 9.2.1 in {loaded.path}" == str(e.value)
    assert params.resolve(make_ctx(), loaded, "9.2.1")["version"] == "9.2.1"


# 8 -------------------------------------------------------------------------------------------------------------------

def dev_case(tmp_path, make_ctx, version, dev):
    line = "" if dev is None else f"developmentVersion: {dev}\n"
    p = write(tmp_path, f'version: "{version}"\nrc: 1\nsummary: s\nbranch: b\n{line}apache: {{availid: jb}}\ngpg: {{keyid: A}}\n')
    return params.resolve(make_ctx(), params.load(p), None)["developmentVersion"]


@pytest.mark.parametrize("dev", [None, '""', "null"])
def test_absent_or_blank_dev_version_derives_next_patch(tmp_path, make_ctx, dev):
    assert dev_case(tmp_path, make_ctx, "9.2.1", dev) == "9.2.2-SNAPSHOT"


def test_bare_dev_version_gets_snapshot(tmp_path, make_ctx):
    assert dev_case(tmp_path, make_ctx, "9.2.1", '"9.2.2"') == "9.2.2-SNAPSHOT"


def test_dot_zero_version_needs_a_dev_version(tmp_path, make_ctx):
    with pytest.raises(params.ParamsError) as e:
        dev_case(tmp_path, make_ctx, "9.3.0", None)
    assert "developmentVersion is required when version ends in .0 (for example 9.3.0-SNAPSHOT)" in str(e.value)
    assert dev_case(tmp_path, make_ctx, "9.3.0", '"9.3.1-SNAPSHOT"') == "9.3.1-SNAPSHOT"


@pytest.mark.parametrize("dev", ['"9.2.1-SNAPSHOT"', '"9.2.0"'])
def test_dev_version_not_greater_is_rejected(tmp_path, make_ctx, dev):
    with pytest.raises(params.ParamsError) as e:
        dev_case(tmp_path, make_ctx, "9.2.1", dev)
    assert "developmentVersion must be greater than version" in str(e.value)
    assert str(tmp_path / "rel.yml") in str(e.value)


def test_snapshot_version_is_rejected_by_validate(tmp_path, make_ctx):
    with pytest.raises(params.ParamsError) as e:
        dev_case(tmp_path, make_ctx, "9.2.1-SNAPSHOT", '"9.2.2-SNAPSHOT"')
    assert "version must look like x.y.z" in str(e.value)


# 9 -------------------------------------------------------------------------------------------------------------------

def test_absent_branch_uses_branch_default_and_explicit_wins(tmp_path, make_ctx):
    ctx = make_ctx()
    ctx.fake.script("git", "-C", CHECKOUT, "for-each-ref", out="origin/master\norigin/juneau-9.2.1-branch\n")
    p = write(tmp_path, "version: 9.2.1\nrc: 1\nsummary: s\napache: {availid: jb}\ngpg: {keyid: A}\n")
    assert params.resolve(ctx, params.load(p), None)["branch"] == "juneau-9.2.1-branch"
    assert ctx.fake.ran("git", "-C", CHECKOUT, "for-each-ref")[0]
    assert ctx.fake.calls[0]["quiet"] is True              # read through runner.quiet, as --init-params needs
    p = write(tmp_path, "version: 9.2.1\nrc: 1\nsummary: s\nbranch: other\napache: {availid: jb}\ngpg: {keyid: A}\n", "b.yml")
    assert params.resolve(ctx, params.load(p), None)["branch"] == "other"


def test_branch_default_falls_back_to_master(tmp_path, make_ctx):
    p = write(tmp_path, "version: 9.2.1\nrc: 1\nsummary: s\napache: {availid: jb}\ngpg: {keyid: A}\n")
    assert params.resolve(make_ctx(), params.load(p), None)["branch"] == "master"


# 10 ------------------------------------------------------------------------------------------------------------------

def test_yaml_syntax_error_has_file_line_and_column(tmp_path):
    p = write(tmp_path, "version: 9.2.1\nrc: [1\nsummary: s\n")
    msg = load_error(p)
    assert msg.startswith(f"{p}: not valid YAML: ")
    assert "line " in msg and "column " in msg


def test_non_mapping_top_level(tmp_path):
    for text in ("- a\n- b\n", "just text\n", ""):
        assert "top level must be a mapping of release parameters" in load_error(write(tmp_path, text))


def test_unreadable_file(tmp_path):
    msg = load_error(tmp_path / "nope.yml")
    assert msg.startswith(f"--params {tmp_path / 'nope.yml'}: cannot read {tmp_path / 'nope.yml'}: ")


@pytest.mark.parametrize("old, new, expect", [
    ("rc: 1", "rc: true", "'rc' must be a whole number"),
    ("rc: 1", 'rc: "1"', "'rc' must be a whole number"),
    ("rc: 1", "rc: 1.5", "'rc' must be a whole number"),
    ("rc: 1", "rc: 0", "rc must be 1 or more"),
    ("version: 9.2.1", "version: 9.2", "'version' must be a string (quote it)"),
    ("version: 9.2.1", "version: 9", "'version' must be a string (quote it)"),
    ("developmentVersion: 9.2.2-SNAPSHOT", "developmentVersion: 9.3", "'developmentVersion' must be a string (quote it)"),
    ("branch: juneau-9.2.1-branch", "branch: [a]", "'branch' must be a string"),
    ("knownIssues: \"\"", "knownIssues: 5", "'knownIssues' must be a string"),
    ("email: jamesbognar@apache.org", "email: 7", "'apache.email' must be a string"),
])
def test_wrong_types(tmp_path, make_ctx, old, new, expect):
    p = write(tmp_path, GOOD.replace(old, new))
    with pytest.raises(params.ParamsError) as e:
        params.resolve(make_ctx(), params.load(p), None)
    assert expect in str(e.value)


# 28 ------------------------------------------------------------------------------------------------------------------

def test_todo_placeholders_are_all_listed_together(tmp_path):
    text = GOOD.replace("ABCD1234ABCD1234", '"TODO: the key"').replace("Security and maintenance release.", "TODO: summary")
    msg = load_error(write(tmp_path, text))
    assert "'summary', 'gpg.keyid' still contain a TODO placeholder; replace them (or clear optional keys)" in msg


def test_single_todo_uses_the_singular_text(tmp_path):
    msg = load_error(write(tmp_path, GOOD.replace('knownIssues: ""', 'knownIssues: "  TODO delete"')))
    assert "'knownIssues' still contains a TODO placeholder; replace it (or clear the optional key)" in msg


def test_todo_sentinel_is_case_and_position_sensitive(tmp_path):
    text = GOOD.replace('knownIssues: ""', 'knownIssues: "todo later"').replace('acknowledgements: ""',
                                                                              'acknowledgements: "Foo TODO"')
    loaded = params.load(write(tmp_path, text))
    assert loaded.details["knownIssues"] == "todo later" and loaded.details["acknowledgements"] == "Foo TODO"


# scaffold, init and record ----------------------------------------------------------------------------------

def sec(keyid, uids, expires="", caps="scESC", validity="u"):
    rows = [f"sec:{validity}:4096:1:{keyid}:1500000000:{expires}::u:::{caps}:::+:::23::0:", f"fpr:::::::::{'A' * 24}{keyid}:"]
    rows += [f"uid:{validity}::::1500000000::HASH{i}::{u}::::::::::0:" for i, u in enumerate(uids)]
    return "\n".join(rows) + "\nssb:u:4096:1:SUBKEY0000000001:1500000000::::::e:::+:::23:\n"


MINE = sec("36292BD2BA7D3A86", ["James Bognar <jamesbognar@apache.org>"])
OTHER = sec("1111111111111111", ["Someone <someone@example.com>"])
GPG_LIST = ("gpg", "--list-secret-keys", "--with-colons", "--keyid-format", "long")
LISTING = "origin/master\norigin/juneau-9.2.1-branch\n"


@pytest.fixture
def scripted(make_ctx):
    """A ctx whose fake answers the three read-only commands the scaffold prefill runs."""
    def make(email="jamesbognar@apache.org\n", keys=MINE, branches=LISTING):
        ctx = make_ctx()
        ctx.fake.script("git", "config", "user.email", out=email, ok=bool(email))
        ctx.fake.script(*GPG_LIST, out=keys)
        ctx.fake.script("git", "-C", CHECKOUT, "for-each-ref", out=branches)
        return ctx
    return make


def doc_of(text):
    return yaml.safe_load(text)


def test_scaffold_header_lists_the_store_commands_and_no_secrets(scripted):
    text = params.scaffold(scripted(), "9.2.1")
    for service, account in (("jrm.apache.password", "<apache id>"), ("jrm.gpg.passphrase", "<apache id>"),
                             ("jrm.github.token", "default")):
        assert f"security add-generic-password -a {account} -s {service} -w\n" in text
    assert "gh auth login" in text
    assert "must not contain secrets" in text
    for word in ("hunter2", "s3cr3t"):
        assert word not in text


def test_scaffold_every_key_present_with_a_one_line_comment_and_parses(scripted):
    text = params.scaffold(scripted(), "9.2.1")
    doc = doc_of(text)
    assert set(doc) == set(params.ALLOWED[None])
    assert set(doc["apache"]) == {"availid", "email"} and set(doc["gpg"]) == {"keyid"}
    lines = text.splitlines()
    for key in ("branch", "version", "developmentVersion", "rc", "summary", "highlights", "knownIssues", "acknowledgements",
                "apache", "gpg"):
        i = next(n for n, ln in enumerate(lines) if ln.startswith(key + ":"))
        assert lines[i - 1].startswith("#"), key


def test_scaffold_prefill_present(scripted):
    ctx = scripted()
    doc = doc_of(params.scaffold(ctx, "9.2.1"))
    assert doc["version"] == "9.2.1" and doc["rc"] == 1
    assert doc["branch"] == "juneau-9.2.1-branch"
    assert doc["developmentVersion"] == "9.2.2-SNAPSHOT"
    assert doc["apache"] == {"availid": "jamesbognar", "email": "jamesbognar@apache.org"}
    assert doc["gpg"]["keyid"] == "36292BD2BA7D3A86"
    for key in ("summary", "highlights", "knownIssues", "acknowledgements"):
        assert doc[key].startswith("TODO")
    assert all(c.get("quiet") for c in ctx.fake.calls)               # all read through runner.quiet


def test_scaffold_prefill_absent_non_apache_email_and_no_branch(scripted):
    doc = doc_of(params.scaffold(scripted(email="me@example.com\n", branches=""), "9.2.1"))
    assert doc["apache"]["email"].startswith("TODO") and doc["apache"]["availid"].startswith("TODO")
    assert doc["branch"] == "master"


def test_scaffold_no_git_email_is_a_todo(scripted):
    doc = doc_of(params.scaffold(scripted(email=""), "9.2.1"))
    assert doc["apache"]["email"].startswith("TODO")


@pytest.mark.parametrize("keys, candidates", [
    ("", None),
    (MINE + OTHER, ["36292BD2BA7D3A86", "1111111111111111"]),
])
def test_scaffold_zero_or_several_signing_keys_is_a_todo_naming_candidates(scripted, keys, candidates):
    text = params.scaffold(scripted(keys=keys), "9.2.1")
    assert doc_of(text)["gpg"]["keyid"].startswith("TODO")
    for c in candidates or ():
        assert c in text


@pytest.mark.parametrize("key", [
    sec("2222222222222222", ["a <a@apache.org>"], validity="r"),
    sec("2222222222222222", ["a <a@apache.org>"], validity="e"),
    sec("2222222222222222", ["a <a@apache.org>"], expires="1500000001"),
    sec("2222222222222222", ["a <a@apache.org>"], caps="cC"),
])
def test_scaffold_ignores_revoked_expired_and_non_signing_keys(scripted, key):
    ctx = scripted(keys=MINE + key)
    ctx.now = lambda: __import__("datetime").datetime.fromtimestamp(1_800_000_000).astimezone()
    assert doc_of(params.scaffold(ctx, "9.2.1"))["gpg"]["keyid"] == "36292BD2BA7D3A86"


def test_scaffold_dot_zero_version_leaves_dev_blank_with_a_comment(scripted):
    text = params.scaffold(scripted(), "9.3.0")
    assert doc_of(text)["developmentVersion"] in ("", None)
    assert "cannot be derived" in text


def test_scaffold_values_round_trip_through_quoting():
    tricky = 'a: "b" # c\\d é {x}'
    text = params.render_scaffold("9.2.1", tricky, "9.2.2-SNAPSHOT", 1, "e@apache.org", "e", "ABCD1234ABCD1234")
    doc = doc_of(text)
    assert doc["branch"] == tricky


def test_a_fresh_scaffold_is_rejected_listing_every_placeholder(scripted, tmp_path):
    p = write(tmp_path, params.scaffold(scripted(email="x@example.com\n", keys=""), "9.2.1"))
    msg = load_error(p)
    for key in ("summary", "highlights", "knownIssues", "acknowledgements", "apache.availid", "apache.email", "gpg.keyid"):
        assert f"'{key}'" in msg


def test_a_scaffold_with_the_todos_replaced_loads(scripted, tmp_path):
    text = params.scaffold(scripted(), "9.2.1")
    for key in ("summary", "highlights", "knownIssues", "acknowledgements"):
        text = "".join(f'{key}: "done"\n' if ln.startswith(key + ":") else ln for ln in text.splitlines(True))
    loaded = params.load(write(tmp_path, text))
    assert loaded.version == "9.2.1"


# 24 init
def init_args(home, version="9.2.1", target=None):
    return Namespace(home=home, version=version, params=target)


def run_init(make_ctx, home, capsys, **kw):
    ctx = make_ctx()
    ctx.fake.script("git", "config", "user.email", out="jamesbognar@apache.org\n")
    ctx.fake.script(*GPG_LIST, out=MINE)
    ctx.fake.script("git", "-C", CHECKOUT, "for-each-ref", out=LISTING)
    code = params.init(init_args(home, **kw), ctx.runner)
    return code, capsys.readouterr().out


def test_init_writes_the_convention_file_and_prints_the_next_command(make_ctx, home, capsys):
    code, out = run_init(make_ctx, home, capsys)
    target = home / "params" / "9.2.1.yml"
    assert code == 0 and target.is_file()
    assert out.strip() == f"Wrote {target}.  Fill in the TODO values, then run: python3 scripts/release.py --version 9.2.1"
    assert doc_of(target.read_text())["version"] == "9.2.1"
    assert [p.name for p in (home / "params").iterdir()] == ["9.2.1.yml"]      # atomic: no temp file left


def test_init_honors_an_explicit_params_path(make_ctx, home, capsys, tmp_path):
    target = tmp_path / "sub" / "r.yml"
    code, out = run_init(make_ctx, home, capsys, target=target)
    assert code == 0 and target.is_file() and not (home / "params").exists()
    assert out.strip().endswith(f"python3 scripts/release.py --params {target}")


def test_init_never_overwrites(make_ctx, home, capsys):
    target = home / "params" / "9.2.1.yml"
    target.parent.mkdir(parents=True)
    target.write_bytes(b"mine: 1\n")
    with pytest.raises(params.ParamsError) as e:
        run_init(make_ctx, home, capsys)
    assert str(e.value) == f"{target} already exists; not overwriting (edit it, or delete it to start over)"
    assert target.read_bytes() == b"mine: 1\n"


# 29 record
def test_record_copies_byte_for_byte_and_overwrites(tmp_path):
    src = write(tmp_path, GOOD + "# trailing comment\n")
    run_dir = tmp_path / "runs" / "9.2.1"
    run_dir.mkdir(parents=True)
    params.record(src, run_dir)
    assert (run_dir / "params.yml").read_bytes() == src.read_bytes()
    src2 = write(tmp_path, GOOD.replace("rc: 1", "rc: 2"), "second.yml")
    params.record(src2, run_dir)
    assert (run_dir / "params.yml").read_bytes() == src2.read_bytes()
    assert [p.name for p in run_dir.iterdir()] == ["params.yml"]


def test_record_skips_when_the_file_is_the_record(tmp_path):
    run_dir = tmp_path / "runs" / "9.2.1"
    run_dir.mkdir(parents=True)
    rec = run_dir / "params.yml"
    rec.write_text(GOOD)
    params.record(rec, run_dir)
    assert rec.read_text() == GOOD


def test_gpg_keyid_is_required(tmp_path):
    msg = load_error(write(tmp_path, GOOD.replace("gpg: {keyid: ABCD1234ABCD1234}\n", "")))
    assert "missing required key(s): gpg.keyid" in msg


def test_require_returns_flag_path(tmp_path):
    args = Namespace(params=tmp_path / "x.yml", version="9.2.1", home=tmp_path)
    assert params.require(args) == tmp_path / "x.yml"


def test_require_uses_convention_file(tmp_path):
    (tmp_path / "params").mkdir()
    (tmp_path / "params" / "9.2.1.yml").write_text("x")
    args = Namespace(params=None, version="9.2.1", home=tmp_path)
    assert params.require(args) == tmp_path / "params" / "9.2.1.yml"


def test_require_without_any_file_says_to_init(tmp_path):
    args = Namespace(params=None, version="9.2.1", home=tmp_path)
    with pytest.raises(params.ParamsError) as e:
        params.require(args)
    assert "run --init-params --version 9.2.1" in str(e.value) and "9.2.1.yml" in str(e.value)


def test_require_without_version_names_the_placeholder(tmp_path):
    args = Namespace(params=None, version=None, home=tmp_path)
    with pytest.raises(params.ParamsError) as e:
        params.require(args)
    assert "no params file; run --init-params --version <version>" in str(e.value)
