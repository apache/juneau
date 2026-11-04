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
"""The PyPI and npm tokens come from the macOS keychain, never from a prompt: which entry is read, what a missing entry
says, and that a value is never in an argv, a log line or an error.  `security` is stubbed; nothing touches the keychain."""
import pytest

from jrm_release.ctx import KEYCHAIN, StepError

pytestmark = pytest.mark.usefixtures("clean_env")

PYPI, NPM = "credential.pypi.token", "credential.npm.token"


def stored(ctx, service, token):
    """What `security find-generic-password -w` prints for an entry that exists."""
    ctx.fake.script("security", "find-generic-password", "-s", service, out=token + "\n")


def test_the_service_names_follow_the_apps_pattern():
    assert KEYCHAIN == {PYPI: "juneau-rm.pypi", NPM: "juneau-rm.npm"}


@pytest.mark.parametrize("key,service,token", [(PYPI, "juneau-rm.pypi", "pypi-s3cr3t"), (NPM, "juneau-rm.npm", "npm-s3cr3t")])
def test_the_token_is_read_by_service_and_never_asked_for(make_ctx, key, service, token):
    ctx = make_ctx()
    stored(ctx, service, token)
    assert ctx.keychain_token(key) == token
    assert ctx.fake.ran("security") == [["security", "find-generic-password", "-s", service, "-a", "token", "-w"]]


def test_a_token_is_read_once_per_process(make_ctx):
    ctx = make_ctx()
    stored(ctx, "juneau-rm.pypi", "pypi-s3cr3t")
    assert ctx.keychain_token(PYPI) == ctx.keychain_token(PYPI) == "pypi-s3cr3t"
    assert len(ctx.fake.ran("security")) == 1


def test_the_value_is_masked_and_never_logged(make_ctx):
    ctx = make_ctx()
    stored(ctx, "juneau-rm.pypi", "pypi-s3cr3t")
    ctx.keychain_token(PYPI)
    assert "pypi-s3cr3t" in ctx.secrets()
    assert ctx.fake.calls[-1]["quiet"] is True                    # the quiet runner: not a row, not in full.log
    assert all("pypi-s3cr3t" not in " ".join(a) for a in ctx.fake.argvs())
    assert all("pypi-s3cr3t" not in line for line in ctx.fake.said)


@pytest.mark.parametrize("key,service", [(PYPI, "juneau-rm.pypi"), (NPM, "juneau-rm.npm")])
@pytest.mark.parametrize("missing", [{"ok": False}, {"out": "\n"}])
def test_a_missing_entry_names_it_and_says_how_to_add_it(make_ctx, key, service, missing):
    ctx = make_ctx()
    ctx.fake.script("security", "find-generic-password", "-s", service, **missing)
    with pytest.raises(StepError) as e:
        ctx.keychain_token(key)
    assert f"no keychain entry for service {service} (account token)" in str(e.value)
    assert f"security add-generic-password -s {service} -a token -w" in str(e.value)
    assert key not in ctx.creds
