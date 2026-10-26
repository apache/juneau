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
"""Tests for check-topic-links.py anchor handling."""

from pathlib import Path

from conftest import load_script

ctl = load_script('check-topic-links.py')

PAGE = (
    '---\ntitle: "Swap"\nslug: Swaps\n---\nIntro.\n\n'
    '## Per-media-type swaps {#old-id}\n\ntext\n\n'
    '## Using `@Swap` Now\n\n'
    '```java\n## not a heading\n```\n'
)


def build(tmp_path):
    page = Path(tmp_path, 'pages', 'topics', '03.13.00.Swaps.md')
    page.parent.mkdir(parents=True)
    page.write_text(PAGE)
    anchors = {}
    topics = ctl.extract_topic_info(tmp_path, anchors)
    return topics, anchors


def link(slug, title='Swap'):
    return {'file': 'A.java', 'line': 1, 'slug': slug, 'title': title, 'full_match': slug}


def test_good_anchors_pass(tmp_path):
    topics, anchors = build(tmp_path)
    links = [link('Swaps#old-id'), link('Swaps#using-swap-now'), link('Swaps')]
    assert ctl.check_links(links, topics, anchors) == []


def test_missing_anchor_is_distinct_category(tmp_path):
    topics, anchors = build(tmp_path)
    warnings = ctl.check_links([link('Swaps#nope')], topics, anchors)
    assert [w['type'] for w in warnings] == ['missing_anchor']
    assert warnings[0]['anchor'] == 'nope'


def test_unknown_slug_with_anchor_is_unknown_slug(tmp_path):
    topics, anchors = build(tmp_path)
    warnings = ctl.check_links([link('Gone#old-id')], topics, anchors)
    assert [w['type'] for w in warnings] == ['unknown_slug']


def test_code_fence_heading_is_not_an_anchor(tmp_path):
    topics, anchors = build(tmp_path)
    warnings = ctl.check_links([link('Swaps#not-a-heading')], topics, anchors)
    assert [w['type'] for w in warnings] == ['missing_anchor']


def test_anchor_heading_text_is_accepted(tmp_path):
    topics, anchors = build(tmp_path)
    links = [link('Swaps#old-id', 'Per-media-type swaps'), link('Swaps#using-swap-now', 'Using @Swap Now')]
    assert ctl.check_links(links, topics, anchors) == []


def test_heading_text_does_not_apply_to_plain_slug(tmp_path):
    topics, anchors = build(tmp_path)
    warnings = ctl.check_links([link('Swaps', 'Per-media-type swaps')], topics, anchors)
    assert [w['type'] for w in warnings] == ['title_mismatch']


def test_anchor_text_must_match_its_own_heading(tmp_path):
    topics, anchors = build(tmp_path)
    warnings = ctl.check_links([link('Swaps#old-id', 'Using @Swap Now')], topics, anchors)
    assert [w['type'] for w in warnings] == ['title_mismatch']
    assert warnings[0]['expected_title'] == 'Swap'
    assert warnings[0]['expected_heading'] == 'Per-media-type swaps'


def test_title_mismatch_still_reported_with_anchor(tmp_path):
    topics, anchors = build(tmp_path)
    warnings = ctl.check_links([link('Swaps#old-id', 'Wrong')], topics, anchors)
    assert [w['type'] for w in warnings] == ['title_mismatch']


def test_html_entity_in_link_text_matches_title(tmp_path):
    topics, anchors = build(tmp_path)
    topics['Amp'] = 'Null & Inclusion Policies'
    assert ctl.check_links([link('Amp', 'Null &amp; Inclusion Policies')], topics, anchors) == []
