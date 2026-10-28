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
"""Secrets from the environment, the macOS keychain and `gh`: resolution order, failure handling, masking."""
import json
import subprocess
from pathlib import Path

import pytest

from jrm_release import keychain, runner
from jrm_release.ctx import StepError

pytestmark = pytest.mark.usefixtures("clean_env")

PW_ITEM = ["security", "find-generic-password", "-s", "jrm.apache.password", "-a", "jamesbognar", "-w"]
GPG_ITEM = ["security", "find-generic-password", "-s", "jrm.gpg.passphrase", "-a", "jamesbognar", "-w"]
GH_ITEM = ["security", "find-generic-password", "-s", "jrm.github.token", "-a", "default", "-w"]
GH_AUTH = ["gh", "auth", "token", "--hostname", "github.com"]
AVAILID = "credential.apache.availid"
PW, GPG, GH = "credential.apache.password", "credential.gpg.passphrase", "credential.github.token"


@pytest.fixture
def mac(monkeypatch):
    monkeypatch.setattr(keychain.sys, "platform", "darwin")
    monkeypatch.setattr(keychain.shutil, "which", lambda n: "/usr/bin/" + n)


@pytest.fixture
def bare(make_ctx):
    """A ctx with no cached credentials but the Apache id (the keychain account)."""
    def make(params=False, dry_run=False, answers=None):
        ctx = make_ctx(dry_run=dry_run, answers=answers)
        ctx.creds.clear()
        ctx.creds[AVAILID] = "jamesbognar"
        ctx.runner.secrets = ctx.secrets          # as release.build_ctx wires it
        if params:
            ctx.args.params = Path("release.yml")
        return ctx
    return make


def test_keychain_hit_is_trimmed_cached_masked_and_off_argv(mac, bare):
    ctx = bare()
    ctx.fake.script(*PW_ITEM, out="kc-pw-value\n")
    assert ctx.cred(PW, secret=True) == "kc-pw-value"
    assert ctx.creds[PW] == "kc-pw-value"
    assert "kc-pw-value" in ctx.secrets()
    call = ctx.fake.calls[0]
    assert call["argv"] == PW_ITEM and call["argv"][-1] == "-w"      # -w last, no value
    assert call.get("quiet") is True and call["timeout"] == keychain.TIMEOUT
    assert not any("kc-pw-value" in " ".join(c["argv"]) for c in ctx.fake.calls)
    assert ctx.prompt.asked == []
    ctx.cred(PW, secret=True)
    assert len(ctx.fake.calls) == 1                                  # cached: nothing is looked up twice
    assert ctx.runner.mask("x kc-pw-value y") == "x ******** y"


def test_keychain_miss_is_a_step_error_and_never_prompts(mac, bare):
    ctx = bare()
    ctx.fake.script(*PW_ITEM, ok=False, code=44)
    with pytest.raises(StepError) as e:
        ctx.cred(PW, secret=True)
    assert "JRM_APACHE_PASSWORD" in str(e.value)
    assert ctx.prompt.asked == []
    assert ctx.fake.said == ["keychain: jrm.apache.password not available (not found)"]


@pytest.mark.parametrize("code, reason", [(36, "denied or locked"), (128, "denied or locked"), (124, "timed out"),
                                          (1, "error (exit 1)")])
def test_keychain_failure_is_one_log_line_and_never_raises(mac, bare, code, reason):
    ctx = bare()
    ctx.fake.script(*PW_ITEM, ok=False, code=code, out="the-secret-in-an-error\n")
    assert keychain.lookup(ctx, "jrm.apache.password", "jamesbognar") is None
    assert ctx.fake.said == [f"keychain: jrm.apache.password not available ({reason})"]
    assert "the-secret-in-an-error" not in " ".join(ctx.fake.said)     # security's output is never logged


def test_keychain_empty_output_is_not_found(mac, bare):
    ctx = bare()
    ctx.fake.script(*PW_ITEM, out="\n")
    assert keychain.lookup(ctx, "jrm.apache.password", "jamesbognar") is None
    assert ctx.fake.said == ["keychain: jrm.apache.password not available (not found)"]


def test_non_macos_makes_no_security_call_and_no_log_line(monkeypatch, bare):
    monkeypatch.setattr(keychain.sys, "platform", "linux")
    ctx = bare()
    assert keychain.lookup(ctx, "jrm.apache.password", "jamesbognar") is None
    assert ctx.fake.calls == [] and ctx.fake.said == []


def test_security_missing_is_skipped_silently(monkeypatch, bare):
    monkeypatch.setattr(keychain.sys, "platform", "darwin")
    monkeypatch.setattr(keychain.shutil, "which", lambda n: None)
    ctx = bare()
    assert keychain.lookup(ctx, "jrm.apache.password", "jamesbognar") is None
    assert ctx.fake.calls == [] and ctx.fake.said == []


def test_env_beats_keychain(mac, bare, monkeypatch):
    monkeypatch.setenv("JRM_APACHE_PASSWORD", "env-pw")
    ctx = bare()
    ctx.fake.script(*PW_ITEM, out="kc-pw\n")
    assert ctx.cred(PW, secret=True) == "env-pw"
    assert ctx.fake.ran("security") == []
    assert "env-pw" in ctx.secrets()


def test_empty_env_string_counts_as_unset(mac, bare, monkeypatch):
    monkeypatch.setenv("JRM_APACHE_PASSWORD", "")
    ctx = bare()
    ctx.fake.script(*PW_ITEM, out="kc-pw\n")
    assert ctx.cred(PW, secret=True) == "kc-pw"


def test_keychain_beats_prompt(mac, bare):
    ctx = bare()
    ctx.fake.script(*GPG_ITEM, out="kc-gpg\n")
    assert ctx.cred(GPG, secret=True) == "kc-gpg"
    assert ctx.prompt.asked == []


def test_env_is_found_and_nothing_found_is_a_step_error_never_a_prompt(bare, monkeypatch):
    ctx = bare()                                   # not macOS in effect: the keychain is skipped
    monkeypatch.setattr(keychain.sys, "platform", "linux")
    monkeypatch.setenv("JRM_GPG_PASSPHRASE", "env-gpg")
    assert ctx.cred(GPG, secret=True) == "env-gpg"
    with pytest.raises(StepError):
        ctx.cred(PW, secret=True)
    assert ctx.prompt.asked == []


def test_github_token_chain_env_then_keychain_then_gh(mac, bare, monkeypatch):
    ctx = bare()
    monkeypatch.setenv("JRM_GITHUB_TOKEN", "env-gh")
    assert ctx.cred(GH, secret=True) == "env-gh"
    assert ctx.fake.calls == []
    ctx = bare()
    monkeypatch.delenv("JRM_GITHUB_TOKEN")
    ctx.fake.script(*GH_ITEM, out="kc-gh\n")
    ctx.fake.script(*GH_AUTH, out="gh-gh\n")
    assert ctx.cred(GH, secret=True) == "kc-gh"
    assert ctx.fake.ran("gh") == []
    ctx = bare()
    ctx.fake.script(*GH_ITEM, ok=False, code=44)
    ctx.fake.script(*GH_AUTH, out="gh-gh\n")
    assert ctx.cred(GH, secret=True) == "gh-gh"
    gh_call = ctx.fake.ran("gh")[0]
    assert gh_call == GH_AUTH


def test_github_token_gh_failure_or_empty_is_a_step_error(mac, bare):
    for scripted in (dict(ok=False, code=1), dict(out="\n")):
        ctx = bare()
        ctx.fake.script(*GH_ITEM, ok=False, code=44)
        ctx.fake.script(*GH_AUTH, **scripted)
        with pytest.raises(StepError):
            ctx.cred(GH, secret=True)
        assert ctx.prompt.asked == []


def test_dry_run_touches_nothing(mac, bare, monkeypatch):
    for name in ("JRM_APACHE_PASSWORD", "JRM_GPG_PASSPHRASE", "JRM_GITHUB_TOKEN"):
        monkeypatch.setenv(name, "real-" + name)
    ctx = bare(dry_run=True)
    for key in (PW, GPG, GH):
        assert ctx.cred(key, secret=True) == "********"
    assert ctx.fake.calls == [] and ctx.fake.said == []
    assert ctx.secrets() == []


def test_account_is_the_apache_id_for_password_and_passphrase_and_default_for_the_token(mac, bare):
    ctx = bare()
    ctx.creds[AVAILID] = "otherid"
    for key in (PW, GPG, GH):
        ctx.fake.script("security", ok=False, code=44)
        assert ctx.find_secret(key) is None
    accounts = [a[a.index("-a") + 1] for a in ctx.fake.ran("security")]
    assert accounts == ["otherid", "otherid", "default"]


def test_gh_failure_logs_one_line_without_output(mac, bare):
    ctx = bare()
    ctx.fake.script(*GH_AUTH, ok=False, code=1, out="gho_tokenintheerror\n")
    assert keychain.gh_token(ctx) is None
    assert len(ctx.fake.said) == 1 and "gho_tokenintheerror" not in ctx.fake.said[0]


def test_quiet_timeout_maps_to_124(monkeypatch):
    def boom(argv, **kw):
        assert kw["timeout"] == 3
        raise subprocess.TimeoutExpired(argv, 3)

    monkeypatch.setattr(runner.subprocess, "run", boom)
    out = runner.Runner(None).quiet(["security", "x"], title="t", timeout=3)
    assert out == runner.Outcome(False, 124, "timed out")


def test_quiet_without_timeout_passes_none(monkeypatch):
    seen = {}

    def ok(argv, **kw):
        seen.update(kw)
        return subprocess.CompletedProcess(argv, 0, stdout="v\n", stderr="")

    monkeypatch.setattr(runner.subprocess, "run", ok)
    assert runner.Runner(None).quiet(["x"], title="t").out == "v\n"
    assert seen["timeout"] is None


def test_keychain_secrets_never_reach_run_json_argv_or_log(mac, bare):
    ctx = bare()
    ctx.fake.script(*PW_ITEM, out="kc-pw-secret\n")
    ctx.fake.script(*GPG_ITEM, out="kc-gpg-secret\n")
    ctx.fake.script(*GH_ITEM, ok=False, code=44)
    ctx.fake.script(*GH_AUTH, out="gh-token-secret\n")
    for key in (PW, GPG, GH):
        ctx.cred(key, secret=True)
    ctx.rf.set("probe", True)
    ctx.runner.run(["git", "status"], title="t", mutating=False)
    blob = ctx.rf.path.read_text() + "\n".join(ctx.fake.said) + json.dumps([c["argv"] for c in ctx.fake.calls])
    for secret in ("kc-pw-secret", "kc-gpg-secret", "gh-token-secret"):
        assert secret not in blob


def test_cred_never_prompts_for_missing_identity(bare):
    ctx = bare()
    ctx.creds.clear()
    with pytest.raises(StepError) as e:
        ctx.cred("credential.gpg.keyid")
    assert "GPG key id" in str(e.value) and ctx.prompt.asked == []


def test_email_defaults_from_availid(bare):
    ctx = bare()
    ctx.creds[AVAILID] = "jbognar"
    assert ctx.cred("credential.apache.email") == "jbognar@apache.org"
    assert ctx.rf.get("identity")["apacheEmail"] == "jbognar@apache.org"


def test_find_secret_records_the_source_never_the_value(bare, monkeypatch):
    ctx = bare()
    monkeypatch.setattr(keychain.sys, "platform", "linux")
    monkeypatch.setenv("JRM_GPG_PASSPHRASE", "env-gpg")
    assert ctx.find_secret(GPG) == "env-gpg"
    assert ctx.secret_source[GPG] == "env"
    assert "env-gpg" not in repr(ctx.secret_source)
    assert ctx.find_secret(GPG) == "env-gpg" and ctx.secret_source[GPG] == "env"


def test_source_is_keychain_or_gh(mac, bare):
    ctx = bare()
    ctx.fake.script(*PW_ITEM, out="kc-pw\n")
    ctx.fake.script(*GH_ITEM, ok=False, code=44)
    ctx.fake.script(*GH_AUTH, out="gh-gh\n")
    ctx.find_secret(PW), ctx.find_secret(GH)
    assert (ctx.secret_source[PW], ctx.secret_source[GH]) == ("keychain", "gh")
