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
"""Tests for check-entry-pages.py: one story across the docs entry pages (rules K0 to K5)."""

import pytest

from conftest import load_script

checker = load_script('check-entry-pages.py')

CORE, RDF = 26, 12


def write(root, rel, text):
    f = root / rel
    f.parent.mkdir(parents=True, exist_ok=True)
    f.write_text(text, encoding='utf-8')
    return f


def badges(core, rdf):
    rows = [f'  <a href="/docs/topics/F{i}"><span className="badge badge--secondary margin--xs">F{i}</span></a>'
            for i in range(core)]
    rows += [f'  <a href="/docs/topics/Rdf"><span className="badge badge--secondary margin--xs">R{i}</span></a>'
             for i in range(rdf)]
    return '<div>\n' + '\n'.join(rows) + '\n</div>\n'


@pytest.fixture(autouse=True)
def _no_juneau_env(monkeypatch):
    monkeypatch.delenv('JUNEAU_ROOT', raising=False)


@pytest.fixture
def docs(tmp_path):
    root = tmp_path / 'docs'
    for rel in checker.ENTRY_PAGES:
        write(root, rel, '# Page\n\nText.\n')
    write(root, checker.MARSHALL_PAGE, '# Marshall\n\n' + badges(CORE, RDF))
    write(root, 'src/pages/about/marshall.mdx', '# About\n\nText.\n')
    return root


def juneau_tree(tmp_path, core=CORE, rdf=RDF):
    root = tmp_path / 'juneau'
    write(root, 'pom.xml', '<project/>\n')
    core_dir, rdf_dir = (root / d for d in checker.MARSHALL_DIRS)
    for i in range(core):
        write(core_dir, f'F{i}.java', f'package x;\n\npublic class F{i} extends CharMarshaller {{\n}}\n')
    for i in range(rdf):
        write(rdf_dir, f'R{i}.java', f'package x;\n\npublic final class R{i} extends StreamMarshaller {{\n}}\n')
    write(core_dir, 'CharMarshaller.java', 'package x;\n\npublic abstract class CharMarshaller extends Marshaller {\n}\n')
    write(core_dir, 'package-info.java', 'package x;\n')
    write(core_dir, 'MarshallUtils.java', 'package x;\n\npublic final class MarshallUtils {\n}\n')
    write(root, 'juneau-bean/juneau-bean-jsonpatch/src/main/java/x/JsonPatchMarshaller.java',
          'package x;\n\npublic class JsonPatchMarshaller extends CharMarshaller {\n}\n')
    return root


def run(docs, capsys, juneau=None):
    argv = ['--docs-root', str(docs)]
    if juneau is not None:
        argv += ['--juneau-root', str(juneau)]
    code = checker.main(argv)
    return code, capsys.readouterr().out


# ---- K0 and output -------------------------------------------------------------------------------------------------

def test_clean_tree_passes(docs, capsys):
    code, out = run(docs, capsys)
    assert code == 0
    assert 'Entry pages checked: 16; failures: 0' in out
    assert 'K1b skipped: no Juneau checkout at' in out


def test_missing_entry_page_fails(docs, capsys):
    (docs / 'pages/topics/24.StarterProjects.md').unlink()
    code, out = run(docs, capsys)
    assert code == 1
    assert 'pages/topics/24.StarterProjects.md:1: K0: entry page missing' in out


def test_no_about_page_fails(docs, capsys):
    (docs / 'src/pages/about/marshall.mdx').unlink()
    code, out = run(docs, capsys)
    assert code == 1
    assert 'src/pages/about:1: K0: no About page' in out


def test_redirect_stub_is_not_an_about_page(docs, capsys):
    (docs / 'src/pages/about/marshall.mdx').unlink()
    write(docs, 'src/pages/about/index.mdx', 'Zero configuration redirect stub.\n')
    code, out = run(docs, capsys)
    assert code == 1
    assert 'src/pages/about:1: K0: no About page' in out
    assert 'K5' not in out


def test_explicit_juneau_root_without_pom_exits_1(docs, tmp_path):
    not_juneau = tmp_path / 'empty'
    not_juneau.mkdir()
    with pytest.raises(SystemExit) as exc:
        checker.main(['--docs-root', str(docs), '--juneau-root', str(not_juneau)])
    assert exc.value.code == 1  # find_juneau_root's own exit (juneau_root.py:63-65), not a failure line


def test_about_md_is_the_fallback(docs, capsys):
    (docs / 'src/pages/about/marshall.mdx').unlink()
    write(docs, 'src/pages/about.md', '# About\n')
    code, out = run(docs, capsys)
    assert code == 0
    assert 'Entry pages checked: 16; failures: 0' in out

# ---- K1a -----------------------------------------------------------------------------------------------------------

WHY = 'pages/topics/01.02.WhyJuneau.md'
CANON = ('Juneau has [38 formats](/docs/topics/JuneauMarshall#counting-formats) '
         '(26 in `juneau-marshall`, 12 RDF in `juneau-marshall-rdf`).\n')


def test_k1a_plus_fails(docs, capsys):
    write(docs, WHY, 'Juneau speaks 38+ formats.\n')
    code, out = run(docs, capsys)
    assert code == 1 and f"{WHY}:1: K1a: '38+ formats'" in out


def test_k1a_wrong_total_fails(docs, capsys):
    write(docs, WHY, 'Juneau speaks 37 formats.\n')
    code, out = run(docs, capsys)
    assert code == 1 and f"{WHY}:1: K1a: '37 formats': the count is 38" in out


def test_k1a_canonical_linked_phrase_passes(docs, capsys):
    write(docs, WHY, CANON)
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k1a_count_inside_a_fence_is_checked(docs, capsys):
    write(docs, WHY, '```java\n// ...and the rest of the 38+ formats\n```\n')
    code, out = run(docs, capsys)
    assert code == 1 and f'{WHY}:2: K1a:' in out


def test_k1a_wrong_split_fails(docs, capsys):
    write(docs, WHY, '38 formats (27 in `juneau-marshall`, 11 RDF in `juneau-marshall-rdf`)\n')
    code, out = run(docs, capsys)
    assert code == 1 and 'the split is (26 in juneau-marshall, 12 RDF in juneau-marshall-rdf)' in out


def test_k1a_ignores_non_entry_pages_and_static(docs, capsys):
    write(docs, 'pages/topics/06.02.00.Other.md', 'Pick from 3 formats.\n')
    write(docs, 'static/llms.txt', 'Juneau speaks 38+ formats.\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k1a_no_badges_fails_and_still_reports_plus(docs, capsys):
    write(docs, checker.MARSHALL_PAGE, '# Marshall\n')
    write(docs, WHY, 'Juneau speaks 38+ formats.\n')
    code, out = run(docs, capsys)
    assert code == 1 and 'K1a: no format badges found' in out
    assert f"{WHY}:1: K1a: '38+ formats'" in out


# ---- K1b -----------------------------------------------------------------------------------------------------------

def test_k1b_counts_only_facades_in_the_two_packages(docs, capsys, tmp_path):
    code, out = run(docs, capsys, juneau_tree(tmp_path))
    assert code == 0, out
    assert 'K1b skipped' not in out


def test_k1b_badge_mismatch_fails(docs, capsys, tmp_path):
    code, out = run(docs, capsys, juneau_tree(tmp_path, core=25))
    assert code == 1
    assert 'K1b: badges show 26 core + 12 RDF formats; the marshaller packages have 25 + 12 facades' in out


def test_k1b_skips_without_juneau_but_k1a_still_runs(docs, capsys):
    write(docs, WHY, 'Juneau speaks 37 formats.\n')
    code, out = run(docs, capsys)
    assert code == 1
    assert 'K1b skipped: no Juneau checkout at' in out and 'K1a' in out

# ---- K2 ------------------------------------------------------------------------------------------------------------

def test_k2a_flags_a_deep_page(docs, capsys):
    write(docs, 'pages/topics/99.Deep.md', 'Open `/swagger-ui.html`.\n')
    code, out = run(docs, capsys)
    assert code == 1 and "pages/topics/99.Deep.md:1: K2a:" in out


def test_k2a_flags_src_pages(docs, capsys):
    write(docs, 'src/pages/security.md', 'Java-serialized-object\n')
    code, out = run(docs, capsys)
    assert code == 1 and 'src/pages/security.md:1: K2a:' in out


def test_k2_skips_release_notes(docs, capsys):
    write(docs, 'pages/release-notes/9.0.md', 'Swagger UI at /swagger-ui.html\n\n```java\n'
                                              'import org.apache.juneau.rest.annotation.*;\n```\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k2b_rename_table_prose_passes(docs, capsys):
    write(docs, 'pages/topics/V10.md', '| `org.apache.juneau.rest.annotation` | `org.apache.juneau.rest.server` |\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k2b_java_fence_fails(docs, capsys):
    write(docs, 'pages/topics/V10.md', '```java\nimport org.apache.juneau.rest.annotation.*;\n```\n')
    code, out = run(docs, capsys)
    assert code == 1 and 'pages/topics/V10.md:2: K2b:' in out


# ---- K3 ------------------------------------------------------------------------------------------------------------

GS3 = 'pages/topics/00.03.GettingStartedRestApp.md'
REST = 'pages/topics/10.00.JuneauRestServer.md'


def test_k3a_named_path_passes(docs, capsys):
    write(docs, GS3, '```java\npublic Pet getPet(@Path("id") int id) {\n```\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k3a_bare_path_fails(docs, capsys):
    write(docs, GS3, '```java\npublic Pet getPet(@Path String id) {\n```\n')
    code, out = run(docs, capsys)
    assert code == 1 and f'{GS3}:2: K3a:' in out


def test_k3b_resource_needs_the_tip_link(docs, capsys):
    write(docs, REST, '```java fragment\npublic class MyChild extends BasicRestResource {\n```\n')
    code, out = run(docs, capsys)
    assert code == 1 and f'{REST}:2: K3b:' in out


def test_k3b_resource_with_the_tip_link_passes(docs, capsys):
    write(docs, REST, '```java fragment\npublic class MyChild extends BasicRestResource {\n```\n\nSee '
                      '[Servlet or Resource?](/docs/topics/GettingStartedRestApp#servlet-or-resource).\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k3c_shaded_on_a_first_resource_page_fails(docs, capsys):
    write(docs, WHY, '```xml\n<artifactId>juneau-shaded-all</artifactId>\n```\n')
    code, out = run(docs, capsys)
    assert code == 1 and f'{WHY}:2: K3c:' in out


def test_k3c_shaded_on_ecosystem_passes(docs, capsys):
    write(docs, 'pages/topics/01.00.JuneauEcosystemOverview.md', '```xml\n<artifactId>juneau-shaded-all</artifactId>\n```\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k3_about_rest_server_page_when_present(docs, capsys):
    write(docs, 'src/pages/about/rest-server.mdx', '```java\nUser get(@Path String id);\n```\n')
    code, out = run(docs, capsys)
    assert code == 1 and 'src/pages/about/rest-server.mdx:2: K3a:' in out

# ---- K4 ------------------------------------------------------------------------------------------------------------

MCP = 'pages/topics/11.01.JuneauMcpQuickstart.md'


def test_k4a_unknown_port_fails(docs, capsys):
    write(docs, 'pages/topics/00.00.GettingStarted.md', 'Open http://localhost:9090/pets\n')
    code, out = run(docs, capsys)
    assert code == 1 and 'pages/topics/00.00.GettingStarted.md:1: K4a:' in out


def test_k4a_8080_on_the_starters_page_passes(docs, capsys):
    write(docs, 'pages/topics/24.StarterProjects.md', 'Tomcat: http://localhost:8080/helloWorld\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k4b_mcp_quickstart_must_use_10000(docs, capsys):
    write(docs, MCP, 'curl -s http://localhost:8080/mcp\n')
    code, out = run(docs, capsys)
    assert code == 1 and f'{MCP}:1: K4b:' in out


# ---- K5 ------------------------------------------------------------------------------------------------------------

def test_k5_banned_phrase_in_prose_fails(docs, capsys):
    write(docs, WHY, '| **Configuration** | Zero configuration | Requires setup |\n')
    code, out = run(docs, capsys)
    assert code == 1 and f"{WHY}:1: K5: 'Zero configuration'" in out


def test_k5_minimal_dependencies_fails_on_the_home_page(docs, capsys):
    write(docs, 'src/pages/index.tsx', 'An integrated solution with Minimal Dependencies.\n')
    code, out = run(docs, capsys)
    assert code == 1 and 'src/pages/index.tsx:1: K5:' in out


def test_k5_self_contained_passes(docs, capsys):
    write(docs, WHY, 'A self-contained example.\n')
    code, out = run(docs, capsys)
    assert code == 0, out


def test_k5_phrase_inside_a_fence_passes(docs, capsys):
    write(docs, WHY, '```text\nzero configuration\n```\n')
    code, out = run(docs, capsys)
    assert code == 0, out


# ---- allow-list ----------------------------------------------------------------------------------------------------

def test_allowlist_exempts_exactly_its_entry(docs, capsys):
    write(docs, WHY, 'Zero configuration. Fast startup.\n')
    write(docs, checker.ALLOWLIST, f'# comment\n\n{WHY}\tK5\tzero configuration\tquoted from a user report\n')
    code, out = run(docs, capsys)
    assert code == 1
    assert "'Zero configuration'" not in out and "K5: 'Fast startup'" in out


def test_stale_allowlist_entry_fails(docs, capsys):
    write(docs, checker.ALLOWLIST, f'{WHY}\tK5\tzero configuration\tno longer on the page\n')
    code, out = run(docs, capsys)
    assert code == 1 and f'{checker.ALLOWLIST}:1: allow-list: stale entry' in out


def test_malformed_allowlist_entry_fails(docs, capsys):
    write(docs, checker.ALLOWLIST, f'{WHY}\tK5\n')
    code, out = run(docs, capsys)
    assert code == 1 and f'{checker.ALLOWLIST}:1: allow-list: malformed entry' in out
