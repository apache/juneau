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
import os

import pytest

from jrm_release import inventory, preflight
from jrm_release.nexus import HttpError
from jrm_release.preflight import CANT, FAIL, PASS, Result
from jrm_release.runner import Outcome

pytestmark = pytest.mark.usefixtures("clean_env")

KEY_ROWS = ("sec:u:4096:1:ABCD1234:1500000000:{exp}::u:::{caps}:::+:::23::0:\n"
            "fpr:::::::::{fpr}:\n")
FPR = "F" * 32 + "ABCD1234"


def keyring(ctx, exp="", caps="scESC", validity="u"):
    out = KEY_ROWS.format(exp=exp, caps=caps, fpr=FPR).replace("sec:u:", f"sec:{validity}:")
    ctx.fake.script("gpg", "--list-secret-keys", out=out)


def have(monkeypatch, *names):
    monkeypatch.setattr(preflight.shutil, "which", lambda n: "/usr/bin/" + n if n in names else None)


def one(ctx, name):
    got = [r for g, n, r in preflight.run_checks(ctx) if n == name]
    assert len(got) == 1, name
    return got[0]


def run_group(ctx, group):
    return {n: r for g, n, r in preflight.run_checks(ctx, (group,))}


def tip(ctx, pom="", notes="", fetch_ok=True):
    """Answer the scratch-repo commands: init, a depth-1 fetch, and `git show FETCH_HEAD:<path>`."""
    orig = ctx.fake.cmd

    def cmd(argv, **kw):
        a = [str(x) for x in argv]
        ctx.fake.calls.append(dict(argv=a, title="", tool=None, cwd=None, env={}, input=None, tty=False, capture=True))
        if a[:3] == ["git", "init", "--bare"]:
            return Outcome(True, 0, "")
        if a[:1] == ["git"] and "fetch" in a:
            return Outcome(fetch_ok, 0 if fetch_ok else 1, "")
        if a[:1] == ["git"] and "show" in a:
            text = {"FETCH_HEAD:pom.xml": pom, "FETCH_HEAD:RELEASE-NOTES.txt": notes}.get(a[-1])
            return Outcome(text is not None, 0 if text is not None else 128, text or "")
        ctx.fake.calls.pop()
        return orig(argv, **kw)
    ctx.fake.cmd = cmd


# ---- core -----------------------------------------------------------------------------------------------------------

def test_exit_code_is_2_on_any_fail_else_1_on_any_cant_else_0():
    r = lambda *s: [("g", "n", Result(x, "r")) for x in s]  # noqa: E731
    assert preflight.exit_code(r(PASS, PASS)) == 0
    assert preflight.exit_code(r(PASS, CANT)) == 1
    assert preflight.exit_code(r(CANT, FAIL, PASS)) == 2


def test_table_has_a_line_per_check_and_a_summary():
    lines = preflight.format_table([("g", "alpha", Result(PASS, "ok")), ("g", "beta", Result(FAIL, "bad")),
                                    ("g", "gamma", Result(CANT, "unknown"))])
    assert lines[0].startswith("PASS") and "alpha" in lines[0]
    assert lines[1].startswith("FAIL") and "bad" in lines[1]
    assert lines[2].startswith("???") and "unknown" in lines[2]
    assert lines[-1] == "1 passed, 1 failed, 1 could not be checked"


def test_problems_lists_everything_that_is_not_a_pass():
    got = preflight.problems([("g", "a", Result(PASS, "ok")), ("g", "b", Result(FAIL, "bad")),
                              ("g", "c", Result(CANT, "?"))])
    assert got == ["b: bad", "c: ?"]


def test_one_check_never_stops_another_and_an_exception_is_a_cant(make_ctx, monkeypatch):
    ctx = make_ctx()

    def boom(ctx):
        raise RuntimeError("kaput")
    monkeypatch.setattr(preflight, "REGISTRY", [("tools", "first", boom), ("tools", "second", lambda c: Result(PASS, "ok")),
                                                 ("tools", "third", lambda c: (_ for _ in ()).throw(preflight.Cant("no input")))])
    got = preflight.run_checks(ctx)
    assert [(n, r.status) for _, n, r in got] == [("first", CANT), ("second", PASS), ("third", CANT)]
    assert got[0][2].reason == "RuntimeError: kaput"
    assert got[2][2].reason == "no input"


def test_groups_filter_the_run(make_ctx):
    ctx = make_ctx()
    names = {g for g, _, _ in preflight.run_checks(ctx, ("secrets-present",))}
    assert names == {"secrets-present"}


def test_every_group_has_checks_and_the_resume_groups_are_a_subset():
    assert {g for g, _, _ in preflight.REGISTRY} == set(preflight.GROUPS)
    assert set(preflight.RESUME_GROUPS) <= set(preflight.GROUPS)


def test_version_in_a_name_is_filled_in(make_ctx):
    ctx = make_ctx()
    names = [n for _, n, _ in preflight.run_checks(ctx, ("repo", "notes"))]
    assert "pom is 9.2.1-SNAPSHOT" in names
    assert "RELEASE-NOTES has 9.2.1" in names


# ---- tools ----------------------------------------------------------------------------------------------------------

def test_every_tool_has_a_check_and_passes_when_present(make_ctx, monkeypatch):
    have(monkeypatch, *preflight.TOOL_NAMES, "svn")
    ctx = make_ctx()
    got = run_group(ctx, "tools")
    for name in preflight.TOOL_NAMES:
        assert got[f"tool {name}"].status == PASS


def test_missing_tool_fails_with_the_hint(make_ctx, monkeypatch):
    have(monkeypatch, *(set(preflight.TOOL_NAMES) - {"gh"}))
    got = run_group(make_ctx(), "tools")
    assert got["tool gh"].status == FAIL
    assert "brew install gh" in got["tool gh"].reason


def test_old_svn_fails_and_unreadable_version_fails(make_ctx, monkeypatch):
    have(monkeypatch, *preflight.TOOL_NAMES)
    ctx = make_ctx()
    ctx.fake.script("svn", "--version", out="1.9.7\n")
    ctx.fake.script("mvn", "-version", out="garbled\n")
    got = run_group(ctx, "tools")
    assert got["tool svn"].status == FAIL and "1.10" in got["tool svn"].reason
    assert got["tool mvn"].status == FAIL and "garbled" in got["tool mvn"].reason


def test_pyyaml_check(make_ctx, monkeypatch):
    ctx = make_ctx()
    assert run_group(ctx, "tools")["PyYAML"].status == PASS
    real = preflight.importlib.util.find_spec
    monkeypatch.setattr(preflight.importlib.util, "find_spec", lambda n, *a: None if n == "yaml" else real(n, *a))
    got = run_group(ctx, "tools")["PyYAML"]
    assert got.status == FAIL and "pip install pyyaml" in got.reason


def test_reachability_is_a_cant_not_a_fail(make_ctx, monkeypatch):
    have(monkeypatch, *preflight.TOOL_NAMES)
    ctx = make_ctx(http_ok=lambda url: False)
    ctx.fake.script("svn", "ls", ok=False)
    got = run_group(ctx, "tools")
    assert got["nexus reachable"].status == CANT
    assert got["dist/dev reachable"].status == CANT
    assert inventory.DIST_DEV in got["dist/dev reachable"].reason


def test_reachable_passes_and_dist_dev_is_listed_anonymously(make_ctx, monkeypatch):
    have(monkeypatch, *preflight.TOOL_NAMES)
    ctx = make_ctx()
    ctx.fake.script("svn", "ls", out="juneau-9.2.0/\n")
    got = run_group(ctx, "tools")
    assert got["nexus reachable"].status == PASS and got["dist/dev reachable"].status == PASS
    assert ctx.fake.ran("svn", "ls", "--non-interactive", "--no-auth-cache", inventory.DIST_DEV + "/")


def test_dist_dev_is_skipped_when_svn_is_missing(make_ctx, monkeypatch):
    have(monkeypatch, "git")
    got = run_group(make_ctx(), "tools")["dist/dev reachable"]
    assert got.status == CANT and got.reason.startswith("skipped")
    assert not preflight.blocks(got)


# ---- params ---------------------------------------------------------------------------------------------------------

def test_params_checks_pass_with_a_loaded_file(make_ctx, tmp_path):
    pytest.importorskip("yaml")
    from types import SimpleNamespace
    path = tmp_path / "p.yml"
    path.write_text('version: "9.2.1"\nrc: 1\nsummary: s\napache: {availid: jb}\ngpg: {keyid: ABCD1234}\n'
                    'developmentVersion: "9.2.2-SNAPSHOT"\n', encoding="utf-8")
    ctx = make_ctx(with_details=False)
    preflight.prepare(ctx, SimpleNamespace(params=str(path), version=None, home=str(tmp_path)))
    got = run_group(ctx, "params")
    assert got["params file"] == Result(PASS, str(path))
    assert got["release details"].status == PASS
    assert "9.2.1 RC1" in got["release details"].reason
    assert ctx.creds["credential.gpg.keyid"] == "ABCD1234"


def test_a_missing_params_file_is_a_fail_and_details_are_cant(make_ctx, tmp_path):
    from types import SimpleNamespace
    ctx = make_ctx(with_details=False)
    preflight.prepare(ctx, SimpleNamespace(params=str(tmp_path / "no.yml"), version=None, home=str(tmp_path)))
    got = run_group(ctx, "params")
    assert got["params file"].status == FAIL
    assert got["release details"].status == CANT


def test_no_params_at_all_names_the_init_params_command(make_ctx, tmp_path):
    from types import SimpleNamespace
    ctx = make_ctx(with_details=False)
    preflight.prepare(ctx, SimpleNamespace(params=None, version="9.2.1", home=str(tmp_path)))
    assert "--init-params --version 9.2.1" in run_group(ctx, "params")["params file"].reason


def test_bad_details_fail_with_the_validation_message(make_ctx, tmp_path):
    pytest.importorskip("yaml")
    from types import SimpleNamespace
    path = tmp_path / "p.yml"
    path.write_text('version: "9.2.1"\nrc: 1\nsummary: s\napache: {availid: jb}\ngpg: {keyid: ABCD1234}\n'
                    'developmentVersion: "9.2.0-SNAPSHOT"\n', encoding="utf-8")
    ctx = make_ctx(with_details=False)
    preflight.prepare(ctx, SimpleNamespace(params=str(path), version=None, home=str(tmp_path)))
    got = run_group(ctx, "params")
    assert got["params file"].status == PASS
    assert got["release details"].status == FAIL


# ---- secrets present ------------------------------------------------------------------------------------------------

def test_secrets_present_reports_the_source_never_the_value(make_ctx, monkeypatch):
    ctx = make_ctx()
    ctx.creds.clear()
    ctx.creds["credential.apache.availid"] = "jb"
    monkeypatch.setenv("JRM_APACHE_PASSWORD", "env-secret-pw")
    monkeypatch.setenv("JRM_GPG_PASSPHRASE", "env-secret-gpg")
    monkeypatch.setenv("JRM_GITHUB_TOKEN", "env-secret-gh")
    got = run_group(ctx, "secrets-present")
    assert all(r.status == PASS and "env" in r.reason for r in got.values())
    assert all("secret" not in r.reason for r in got.values())


def test_a_missing_secret_fails_with_the_hint(make_ctx):
    ctx = make_ctx()
    ctx.creds.clear()
    ctx.creds["credential.apache.availid"] = "jb"
    ctx.fake.script("security", ok=False)
    ctx.fake.script("gh", "auth", "token", ok=False)
    got = run_group(ctx, "secrets-present")
    assert all(r.status == FAIL for r in got.values())
    assert "JRM_APACHE_PASSWORD" in got["Apache password present"].reason
    assert "gh auth login" in got["GitHub token present"].reason


def test_secrets_are_skipped_in_a_dry_run(make_ctx):
    ctx = make_ctx(dry_run=True)
    ctx.creds.clear()
    for r in run_group(ctx, "secrets-present").values():
        assert r == Result(PASS, "skipped (dry run)")
    for n in ("Apache password valid", "GPG passphrase valid", "GitHub token valid"):
        assert run_group(ctx, "secrets-valid")[n] == Result(PASS, "skipped (dry run)")
    assert ctx.fake.ran("gh") == [] and ctx.fake.ran("security") == []


# ---- secrets valid --------------------------------------------------------------------------------------------------

def test_apache_password_valid_passes_on_a_nexus_answer(make_ctx):
    assert one(make_ctx(), "Apache password valid").status == PASS


@pytest.mark.parametrize("status,expected", [(401, FAIL), (403, FAIL), (500, CANT)])
def test_apache_password_http_statuses(make_ctx, status, expected):
    ctx = make_ctx()

    def boom():
        raise HttpError("GET", "u", status)
    ctx.nexus.repositories = boom
    got = one(ctx, "Apache password valid")
    assert got.status == expected and str(status) in got.reason


def test_apache_password_network_error_is_cant(make_ctx):
    ctx = make_ctx()

    def boom():
        raise OSError("no route")
    ctx.nexus.repositories = boom
    assert one(ctx, "Apache password valid").status == CANT


def test_apache_password_is_cant_when_the_secret_is_absent(make_ctx):
    ctx = make_ctx()
    ctx.creds.pop("credential.apache.password")
    ctx.fake.script("security", ok=False)
    assert one(ctx, "Apache password valid").status == CANT


def test_gpg_passphrase_goes_on_stdin_and_never_argv(make_ctx, monkeypatch):
    have(monkeypatch, "gpg")
    ctx = make_ctx()
    got = one(ctx, "GPG passphrase valid")
    assert got.status == PASS
    call = [c for c in ctx.fake.calls if c["argv"][:1] == ["gpg"] and "--detach-sign" in c["argv"]][0]
    assert call["input"] == b"s3cr3t-gpg\n"
    assert all("s3cr3t-gpg" not in a for a in call["argv"])
    assert "--passphrase-fd" in call["argv"] and call["argv"][call["argv"].index("--passphrase-fd") + 1] == "0"
    assert "ABCD1234" in call["argv"]
    assert call.get("quiet") is True


def test_gpg_passphrase_rejected_fails_and_cleans_up(make_ctx, monkeypatch):
    have(monkeypatch, "gpg")
    ctx = make_ctx()
    ctx.fake.script("gpg", "--batch", ok=False)
    got = one(ctx, "GPG passphrase valid")
    assert got.status == FAIL and "s3cr3t-gpg" not in got.reason
    blob = [c for c in ctx.fake.calls if "--detach-sign" in c["argv"]][0]["argv"][-1]
    assert not os.path.exists(os.path.dirname(blob))


def test_gpg_passphrase_is_cant_without_gpg_or_key_or_secret(make_ctx, monkeypatch):
    have(monkeypatch)
    assert one(make_ctx(), "GPG passphrase valid").status == CANT
    have(monkeypatch, "gpg")
    ctx = make_ctx()
    ctx.creds.pop("credential.gpg.keyid")
    assert one(ctx, "GPG passphrase valid").status == CANT


def test_gpg_key_valid_reports_the_expiry(make_ctx):
    ctx = make_ctx()
    keyring(ctx, exp="4102444800")
    got = one(ctx, "GPG key not expired")
    assert got.status == PASS and "expires 20" in got.reason or "expires 21" in got.reason


def test_gpg_key_with_no_expiry_passes(make_ctx):
    ctx = make_ctx()
    keyring(ctx)
    assert "no expiry" in one(ctx, "GPG key not expired").reason


@pytest.mark.parametrize("kw", [dict(exp="1600000000"), dict(validity="r"), dict(validity="e"), dict(caps="eE")])
def test_gpg_key_unusable_fails(make_ctx, kw):
    ctx = make_ctx()
    keyring(ctx, **kw)
    assert one(ctx, "GPG key not expired").status == FAIL


def test_gpg_key_not_in_the_keyring_fails_and_listing_error_is_cant(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("gpg", "--list-secret-keys", out="")
    assert one(ctx, "GPG key not expired").status == FAIL
    ctx = make_ctx()
    ctx.fake.script("gpg", "--list-secret-keys", ok=False)
    assert one(ctx, "GPG key not expired").status == CANT


def gh_answer(ctx, status="200 OK", scopes="repo, gist"):
    head = f"HTTP/2.0 {status}\n" + (f"X-Oauth-Scopes: {scopes}\n" if scopes is not None else "") + "\n{}"
    ctx.fake.script("gh", "api", "user", out=head)


def test_github_token_goes_in_the_environment_never_argv(make_ctx):
    ctx = make_ctx()
    gh_answer(ctx)
    assert one(ctx, "GitHub token valid").status == PASS
    call = ctx.fake.ran("gh", "api", "user", "--include")
    rec = [c for c in ctx.fake.calls if c["argv"][:3] == ["gh", "api", "user"]][0]
    assert call and rec["env"] == {"GH_TOKEN": "s3cr3t-gh"}
    assert all("s3cr3t-gh" not in a for a in rec["argv"])


@pytest.mark.parametrize("kw,expected", [(dict(status="401 Unauthorized"), FAIL), (dict(scopes="gist"), FAIL),
                                         (dict(scopes=None), CANT), (dict(status="500 Server Error"), CANT)])
def test_github_token_outcomes(make_ctx, kw, expected):
    ctx = make_ctx()
    gh_answer(ctx, **kw)
    assert one(ctx, "GitHub token valid").status == expected


def test_github_token_with_no_http_answer_is_cant(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("gh", "api", "user", out="", ok=False)
    assert one(ctx, "GitHub token valid").status == CANT


# ---- repo, notes, remote --------------------------------------------------------------------------------------------

POM = "<project><parent><version>1</version></parent><version>9.2.1-SNAPSHOT</version></project>"
NOTES = "Release Notes - Juneau - Version 9.2.1 - 2026-10-28\n\nstuff\n"


def test_branch_and_tag_checks(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("git", "ls-remote", inventory.GIT_URL, "refs/heads/juneau-9.2.1-branch", out="abc\trefs/heads/x\n")
    assert one(ctx, "branch on origin").status == PASS
    assert one(ctx, "RC tag absent") == Result(PASS, "juneau-9.2.1-RC1 is free")
    ctx.fake.script("git", "ls-remote", inventory.GIT_URL, "refs/tags/juneau-9.2.1-RC1", out="abc\trefs/tags/x\n")
    got = one(ctx, "RC tag absent")
    assert got.status == FAIL and "--revert" in got.reason


def test_missing_branch_fails_and_unreachable_remote_is_cant(make_ctx):
    ctx = make_ctx()
    assert one(ctx, "branch on origin").status == FAIL        # ls-remote answered nothing
    ctx.fake.script("git", "ls-remote", ok=False)
    assert one(ctx, "branch on origin").status == CANT
    assert one(ctx, "RC tag absent").status == CANT


def test_pom_and_notes_come_from_one_depth_1_fetch_and_the_scratch_repo_is_removed(make_ctx):
    ctx = make_ctx()
    tip(ctx, pom=POM, notes=NOTES)
    got = {n: r for g, n, r in preflight.run_checks(ctx, ("repo", "notes"))}
    assert got["pom is 9.2.1-SNAPSHOT"].status == PASS
    assert got["RELEASE-NOTES has 9.2.1"].status == PASS
    fetches = [a for a in ctx.fake.argvs() if "fetch" in a]
    assert len(fetches) == 1
    assert fetches[0][-4:] == ["--depth", "1", inventory.GIT_URL, "juneau-9.2.1-branch"]
    scratch = fetches[0][fetches[0].index("-C") + 1]
    assert not os.path.exists(scratch) and not ctx.pf.get("scratch")


def test_wrong_pom_version_and_missing_notes_section_fail(make_ctx):
    ctx = make_ctx()
    tip(ctx, pom=POM.replace("9.2.1-SNAPSHOT", "9.2.0"), notes="Release Notes - Juneau - Version 9.2.0 - x\n")
    got = {n: r for g, n, r in preflight.run_checks(ctx, ("repo", "notes"))}
    assert got["pom is 9.2.1-SNAPSHOT"].status == FAIL and "9.2.0" in got["pom is 9.2.1-SNAPSHOT"].reason
    assert got["RELEASE-NOTES has 9.2.1"].status == FAIL


def test_notes_header_without_a_date_matches(make_ctx):
    ctx = make_ctx()
    tip(ctx, pom=POM, notes="Release Notes - Juneau - Version 9.2.1\n")
    assert one(ctx, "RELEASE-NOTES has 9.2.1").status == PASS


def test_a_failed_fetch_makes_both_content_checks_cant(make_ctx):
    ctx = make_ctx()
    tip(ctx, fetch_ok=False)
    got = {n: r for g, n, r in preflight.run_checks(ctx, ("repo", "notes"))}
    assert got["pom is 9.2.1-SNAPSHOT"].status == CANT
    assert got["RELEASE-NOTES has 9.2.1"].status == CANT
    assert len([a for a in ctx.fake.argvs() if "fetch" in a]) == 1


def test_gpg_key_in_keys(make_ctx):
    ctx = make_ctx()
    keyring(ctx)
    ctx.http_text = lambda url: f"pub  rsa4096\n      {FPR}\nuid x\n"
    assert one(ctx, "GPG key in KEYS").status == PASS
    ctx.http_text = lambda url: "nothing here\n"
    got = one(ctx, "GPG key in KEYS")
    assert got.status == FAIL and "KEYS" in got.reason
    ctx.http_text = lambda url: None
    assert one(ctx, "GPG key in KEYS").status == CANT


def test_open_staging_repo_fails_and_closed_is_informational(make_ctx):
    ctx = make_ctx()
    ctx.nexus.repos = [{"repositoryId": "orgapachejuneau-1000", "state": "closed"}]
    got = one(ctx, "no open staging repo")
    assert got.status == PASS and "1 closed" in got.reason
    ctx.nexus.repos = [{"repositoryId": "orgapachejuneau-1001", "state": "open"}]
    ctx.nexus._listed = 0
    got = one(ctx, "no open staging repo")
    assert got.status == FAIL and "orgapachejuneau-1001" in got.reason


def test_staging_listing_errors_are_cant(make_ctx):
    ctx = make_ctx()

    def boom():
        raise HttpError("GET", "u", 401)
    ctx.nexus.repositories = boom
    assert one(ctx, "no open staging repo").status == CANT


def test_dist_dev_rc_dirs(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("svn", "ls", out="x/\n")
    assert one(ctx, "no dist/dev RC dir").status == FAIL
    ctx = make_ctx()
    ctx.fake.script("svn", "ls", ok=False, out="svn: E170000: URL 'x' doesn't exist\n")
    assert one(ctx, "no dist/dev RC dir").status == PASS
    ctx = make_ctx()
    ctx.fake.script("svn", "ls", ok=False, out="svn: E670002: name resolution\n")
    assert one(ctx, "no dist/dev RC dir").status == CANT


def test_repo_checks_are_cant_without_details(make_ctx):
    ctx = make_ctx(with_details=False)
    got = run_group(ctx, "repo")
    assert all(r.status == CANT for n, r in got.items() if n != "GPG key in KEYS"), got


# ---- gpg-agent cache TTL -------------------------------------------------------------------------------------------------

def agent_options(default=("600", ""), max_=("7200", "")):
    return (f"default-cache-ttl:24:0:expire cached PINs after N seconds:3:3:N:{default[0]}::{default[1]}\n"
            f"max-cache-ttl:24:2:set maximum PIN cache lifetime to N seconds:3:3:N:{max_[0]}::{max_[1]}\n")


def ttl(make_ctx, monkeypatch, out, which=("gpgconf",)):
    have(monkeypatch, *which)
    ctx = make_ctx()
    ctx.fake.script("gpgconf", "--list-options", out=out)
    return one(ctx, "gpg-agent cache TTL")


def test_agent_ttl_passes_for_configured_large_values(make_ctx, monkeypatch):
    got = ttl(make_ctx, monkeypatch, agent_options(("600", "7200"), ("7200", "14400")))
    assert got.status == PASS and "7200" in got.reason and "14400" in got.reason


def test_agent_ttl_fails_for_the_defaults(make_ctx, monkeypatch):
    got = ttl(make_ctx, monkeypatch, agent_options())
    assert got.status == FAIL
    assert "default-cache-ttl is 600s (needs >= 1800)" in got.reason and "max-cache-ttl" not in got.reason.split(":")[0]
    assert "python3 scripts/gpg-agent-ttl.py" in got.reason


def test_agent_ttl_fails_for_a_low_max(make_ctx, monkeypatch):
    got = ttl(make_ctx, monkeypatch, agent_options(("600", "3600"), ("7200", "3600")))
    assert got.status == FAIL and "max-cache-ttl is 3600s (needs >= 7200)" in got.reason
    assert "default-cache-ttl is" not in got.reason


def test_agent_ttl_is_cant_without_gpgconf_or_with_garbage(make_ctx, monkeypatch):
    assert ttl(make_ctx, monkeypatch, "", which=()).status == CANT
    assert ttl(make_ctx, monkeypatch, "nonsense\nmore:garbage\n").status == CANT
    assert ttl(make_ctx, monkeypatch, agent_options(("x", ""), ("7200", ""))).status == CANT


def test_agent_ttl_runs_on_resume():
    assert "tools" in preflight.RESUME_GROUPS
