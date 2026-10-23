# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
# ***************************************************************************************************************************
"""Tests for the shared Juneau checkout lookup."""

import pytest

from juneau_root import find_juneau_root


def make_checkout(path, git=True, pom=True):
    path.mkdir(parents=True)
    if git:
        (path / '.git').mkdir()
    if pom:
        (path / 'pom.xml').write_text('<project/>')
    return path


@pytest.fixture(autouse=True)
def no_env(monkeypatch):
    monkeypatch.delenv('JUNEAU_ROOT', raising=False)


@pytest.fixture
def docs_dir(tmp_path):
    docs = tmp_path / 'docs'
    docs.mkdir()
    return docs


def test_explicit_arg_wins_over_env_and_siblings(tmp_path, docs_dir, monkeypatch):
    make_checkout(tmp_path / 'master')
    env_root = make_checkout(tmp_path / 'env-root')
    arg_root = make_checkout(tmp_path / 'arg-root')
    monkeypatch.setenv('JUNEAU_ROOT', str(env_root))
    assert find_juneau_root(docs_dir, arg_root) == arg_root.resolve()


def test_env_wins_over_siblings(tmp_path, docs_dir, monkeypatch):
    make_checkout(tmp_path / 'master')
    env_root = make_checkout(tmp_path / 'env-root')
    monkeypatch.setenv('JUNEAU_ROOT', str(env_root))
    assert find_juneau_root(docs_dir) == env_root.resolve()


def test_master_sibling_wins_over_juneau_sibling(tmp_path, docs_dir):
    master = make_checkout(tmp_path / 'master')
    make_checkout(tmp_path / 'juneau')
    assert find_juneau_root(docs_dir) == master.resolve()


def test_juneau_sibling_used_when_no_master(tmp_path, docs_dir):
    juneau = make_checkout(tmp_path / 'juneau')
    assert find_juneau_root(docs_dir) == juneau.resolve()


def test_sibling_without_git_dir_is_skipped(tmp_path, docs_dir):
    make_checkout(tmp_path / 'master', git=False)
    juneau = make_checkout(tmp_path / 'juneau')
    assert find_juneau_root(docs_dir) == juneau.resolve()


def test_explicit_non_checkout_exits(tmp_path, docs_dir, capsys):
    bad = make_checkout(tmp_path / 'bad', pom=False)
    with pytest.raises(SystemExit) as exc:
        find_juneau_root(docs_dir, bad)
    assert exc.value.code == 1
    assert '--juneau-root' in capsys.readouterr().out


def test_bad_env_names_env_var_in_error(tmp_path, docs_dir, monkeypatch, capsys):
    monkeypatch.setenv('JUNEAU_ROOT', str(tmp_path / 'nope'))
    with pytest.raises(SystemExit):
        find_juneau_root(docs_dir)
    assert 'JUNEAU_ROOT' in capsys.readouterr().out


def test_validate_false_skips_explicit_check(tmp_path, docs_dir):
    missing = tmp_path / 'missing'
    assert find_juneau_root(docs_dir, missing, validate=False) == missing.resolve()


def test_nothing_found_exits(docs_dir, capsys):
    with pytest.raises(SystemExit) as exc:
        find_juneau_root(docs_dir)
    assert exc.value.code == 1
    assert 'Could not locate' in capsys.readouterr().out


def test_allow_missing_returns_master_sibling(tmp_path, docs_dir):
    assert find_juneau_root(docs_dir, allow_missing=True) == (tmp_path / 'master').resolve()
