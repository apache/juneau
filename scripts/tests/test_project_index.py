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
"""Tests for project_index.py, the parser for src/data/projects.ts."""

import pytest

import project_index as pi

FILE = """// comment outside the literals is fine
import type {X} from 'y';

export const GROUPS: Group[] = [
  {
    id: 'data',
    label: 'Data',
  },
];

export const PROJECTS: Project[] = [
  {
    slug: 'marshall',
    name: 'Juneau Marshall',
    group: 'data',
    tagline: 'One bean. Every format.',
    modules: ['juneau-marshall', 'juneau-marshall-rdf'],
    docs: ['JuneauMarshall'],
  },
];

// trailing comment: export const PROJECTS must not be matched twice
"""


def test_parses_groups_and_projects():
    groups, projects = pi.parse(FILE)
    assert groups == [{'id': 'data', 'label': 'Data'}]
    assert projects[0]['slug'] == 'marshall'
    assert projects[0]['modules'] == ['juneau-marshall', 'juneau-marshall-rdf']
    assert projects[0]['docs'] == ['JuneauMarshall']


def test_parser_ignores_comments_outside_literals():
    groups, projects = pi.parse('// export const PROJECTS lookalike\n' + FILE)
    assert len(projects) == 1


def test_parser_rejects_comment_inside_literal():
    bad = FILE.replace("    group: 'data',", "    // why\n    group: 'data',")
    assert bad != FILE
    with pytest.raises(pi.ProjectIndexError, match=r'line \d+'):
        pi.parse(bad)


def test_parser_rejects_escaped_quote():
    bad = FILE.replace("One bean. Every format.", "It\\'s one bean.")
    assert bad != FILE
    with pytest.raises(pi.ProjectIndexError, match=r'line \d+'):
        pi.parse(bad)


def test_parser_allows_typographic_apostrophe():
    ok = FILE.replace("One bean. Every format.", "It’s one bean.")
    _, projects = pi.parse(ok)
    assert projects[0]['tagline'] == 'It’s one bean.'


def test_parser_rejects_multiline_array():
    bad = FILE.replace("    modules: ['juneau-marshall', 'juneau-marshall-rdf'],",
                       "    modules: [\n      'juneau-marshall',\n    ],")
    assert bad != FILE
    with pytest.raises(pi.ProjectIndexError, match=r'line \d+'):
        pi.parse(bad)


def test_last_entry_may_close_without_comma():
    ok = FILE.replace("    docs: ['JuneauMarshall'],\n  },\n];", "    docs: ['JuneauMarshall'],\n  }\n];")
    assert ok != FILE
    _, projects = pi.parse(ok)
    assert projects[0]['slug'] == 'marshall'


def test_missing_literal_fails():
    with pytest.raises(pi.ProjectIndexError, match='PROJECTS'):
        pi.parse("export const GROUPS: Group[] = [\n];\n")


def test_unclosed_object_fails():
    bad = FILE.replace("  },\n];\n\n// trailing", "];\n\n// trailing")
    assert bad != FILE
    with pytest.raises(pi.ProjectIndexError, match=r'line \d+'):
        pi.parse(bad)


def test_aggregators_constant_is_exposed():
    assert 'juneau' in pi.AGGREGATOR_POMS and 'juneau-core' in pi.AGGREGATOR_POMS


MODULE_DOCS_FILE = FILE + """
export const MODULE_DOCS: Record<string, string> = {
  'juneau-marshall': 'JuneauMarshall',
  'juneau-marshall-rdf': 'JuneauMarshallRdf',
};
"""


def test_parses_module_docs():
    assert pi.parse_module_docs(MODULE_DOCS_FILE) == {
        'juneau-marshall': 'JuneauMarshall', 'juneau-marshall-rdf': 'JuneauMarshallRdf'}


def test_module_docs_missing_is_an_error():
    with pytest.raises(pi.ProjectIndexError, match='MODULE_DOCS'):
        pi.parse_module_docs(FILE)


def test_module_docs_rejects_bad_line():
    bad = MODULE_DOCS_FILE.replace("'juneau-marshall': 'JuneauMarshall',", "'juneau-marshall': JuneauMarshall,")
    with pytest.raises(pi.ProjectIndexError, match=r'line \d+'):
        pi.parse_module_docs(bad)


def test_module_docs_rejects_duplicate_key():
    bad = MODULE_DOCS_FILE.replace("'juneau-marshall-rdf'", "'juneau-marshall'")
    with pytest.raises(pi.ProjectIndexError, match=r'line \d+.*duplicate'):
        pi.parse_module_docs(bad)


def test_module_docs_does_not_disturb_projects():
    groups, projects = pi.parse(MODULE_DOCS_FILE)
    assert len(projects) == 1
