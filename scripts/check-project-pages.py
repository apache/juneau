#!/usr/bin/env python3
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
"""
Check that src/data/projects.ts, the /about MDX pages and their links agree (checks C1 to C13).

Full docs: check-project-pages.md
"""

import argparse
import json
import re
import sys
from pathlib import Path

import project_index
from project_old_names import find_old_names

ABOUT_DIR = 'src/pages/about'
SLUG_RE = re.compile(r'[a-z0-9]+(-[a-z0-9]+)*')
LAYOUT_TAG_RE = re.compile(r'<ProjectLayout\b([^>]*)>', re.S)
TOPIC_LINK_RE = re.compile(r'/docs/topics/([A-Za-z0-9_.-]+)')
FRONT_MATTER_RE = re.compile(r'\A---\n(.*?)\n---', re.S)
FM_KEY_RE = re.compile(r'^(title|description|hide_table_of_contents):\s*(.*?)\s*$', re.M)


def _unquote(value):
    if len(value) >= 2 and value[0] == value[-1] and value[0] in '\'"':
        inner = value[1:-1]
        return inner.replace("''", "'") if value[0] == "'" else inner
    return value


def _front_matter(text):
    m = FRONT_MATTER_RE.match(text)
    if not m:
        return {}
    return {k: _unquote(v) for k, v in FM_KEY_RE.findall(m.group(1))}


def _topic_slugs(docs_root):
    slugs = set()
    for page in Path(docs_root, 'pages', 'topics').glob('*.md'):
        m = FRONT_MATTER_RE.match(page.read_text(encoding='utf-8'))
        slug = re.search(r'^slug:\s*(\S+)', m.group(1), re.M) if m else None
        slugs.add(slug.group(1) if slug else page.stem)
    return slugs


def _layout_slug_ok(attrs, slug):
    q = re.escape(slug)
    return re.search(rf'\bslug\s*=\s*(?:"{q}"|\'{q}\'|\{{\s*"{q}"\s*\}}|\{{\s*\'{q}\'\s*\}})', attrs) is not None


def _check_layout(rel, text, slug, errors):
    tags = LAYOUT_TAG_RE.findall(text)
    if len(tags) != 1:
        errors.append(f'C3: {rel}: expected exactly one <ProjectLayout>, found {len(tags)}')
    elif slug is None:
        if re.search(r'\bslug\s*=', tags[0]):
            errors.append(f'C3: {rel}: the overview <ProjectLayout> must have no slug attribute')
    elif not _layout_slug_ok(tags[0], slug):
        errors.append(f'C3: {rel}: <ProjectLayout> slug must be "{slug}"')


def _line_of(text, offset):
    return text.count('\n', 0, offset) + 1


def check(docs_root):
    """Every violation as 'C<n>: <where>: <what>'; empty list when the project pages are consistent."""
    docs_root = Path(docs_root)
    try:
        groups, projects = project_index.load(docs_root)
        module_docs = project_index.load_module_docs(docs_root)
    except project_index.ProjectIndexError as e:
        return [f'C0: {project_index.PROJECTS_FILE}: {e}']
    errors = []
    about = docs_root / ABOUT_DIR
    pages = {p.stem: p for p in sorted(about.glob('*.mdx'))}
    overview = pages.pop('index', None)
    slugs = [p.get('slug', '') for p in projects]

    # C4, C1, C2: slugs are unique kebab-case and match the page files one to one.
    seen = set()
    for slug in slugs:
        if slug in seen:
            errors.append(f'C4: {project_index.PROJECTS_FILE}: duplicate slug "{slug}"')
        seen.add(slug)
        if not SLUG_RE.fullmatch(slug):
            errors.append(f'C4: {project_index.PROJECTS_FILE}: slug "{slug}" is not kebab-case')
    for slug in sorted(set(slugs) - set(pages)):
        errors.append(f'C1: {ABOUT_DIR}/{slug}.mdx: project "{slug}" has no page')
    for stem in sorted(set(pages) - set(slugs)):
        errors.append(f'C2: {ABOUT_DIR}/{stem}.mdx: page has no project in {project_index.PROJECTS_FILE}')

    # C3: each page wraps its content in one <ProjectLayout> with the matching slug; the overview has none.
    for stem, page in pages.items():
        _check_layout(f'{ABOUT_DIR}/{stem}.mdx', page.read_text(encoding='utf-8'), stem, errors)
    if overview is None:
        errors.append(f'C3: {ABOUT_DIR}/index.mdx: the overview page is missing')
    else:
        _check_layout(f'{ABOUT_DIR}/index.mdx', overview.read_text(encoding='utf-8'), None, errors)

    # C5, C10: every project's group exists, every group has a project, and the order is group by group.
    group_ids = [g.get('id', '') for g in groups]
    used = {p.get('group') for p in projects}
    for p in projects:
        if p.get('group') not in group_ids:
            errors.append(f'C5: {project_index.PROJECTS_FILE}: project "{p.get("slug")}" has unknown group "{p.get("group")}"')
    for gid in group_ids:
        if gid not in used:
            errors.append(f'C5: {project_index.PROJECTS_FILE}: group "{gid}" has no project')
    order = [group_ids.index(p['group']) for p in projects if p.get('group') in group_ids]
    if order != sorted(order):
        errors.append(f'C10: {project_index.PROJECTS_FILE}: projects must be contiguous per group, in GROUPS order')

    # C6: modules are real, documentable artifacts (a key in artifact-paths.json that is not an aggregator pom).
    try:
        keys = set(json.loads(Path(docs_root, 'artifact-paths.json').read_text(encoding='utf-8')))
    except (OSError, ValueError) as e:
        keys = set()
        errors.append(f'C6: artifact-paths.json: cannot read: {e}')
    for p in projects:
        for mod in p.get('modules', []):
            if mod in project_index.AGGREGATOR_POMS:
                errors.append(f'C6: {project_index.PROJECTS_FILE}: project "{p.get("slug")}" lists aggregator pom "{mod}"')
            elif mod not in keys:
                errors.append(f'C6: {project_index.PROJECTS_FILE}: project "{p.get("slug")}" lists "{mod}", '
                              f'which is not a key in artifact-paths.json')

    # C7: every Read-more topic is a real topic slug.
    topics = _topic_slugs(docs_root)
    for p in projects:
        for d in p.get('docs', []):
            if d not in topics:
                errors.append(f'C7: {project_index.PROJECTS_FILE}: project "{p.get("slug")}" lists topic "{d}", '
                              f'which is no pages/topics slug')

    # C13: every MODULE_DOCS key is a module some project lists, and every value is a real topic slug.
    listed = {m for p in projects for m in p.get('modules', [])}
    for mod, slug in module_docs.items():
        if mod not in listed:
            errors.append(f'C13: {project_index.PROJECTS_FILE}: MODULE_DOCS key "{mod}" is no project\'s module')
        if slug not in topics:
            errors.append(f'C13: {project_index.PROJECTS_FILE}: MODULE_DOCS maps "{mod}" to "{slug}", '
                          f'which is no pages/topics slug')

    # C8: names start with "Juneau " and taglines are not empty.
    for p in projects:
        if not p.get('name', '').startswith('Juneau '):
            errors.append(f'C8: {project_index.PROJECTS_FILE}: name "{p.get("name")}" must start with "Juneau "')
        if not p.get('tagline', '').strip():
            errors.append(f'C8: {project_index.PROJECTS_FILE}: project "{p.get("slug")}" has an empty tagline')

    # C11: page front matter matches the index (the overview needs only a title and description).
    by_slug = {p.get('slug'): p for p in projects}
    for stem, page in list(pages.items()) + ([('index', overview)] if overview else []):
        rel = f'{ABOUT_DIR}/{stem}.mdx'
        fm = _front_matter(page.read_text(encoding='utf-8'))
        project = by_slug.get(stem)
        if project is not None:
            if fm.get('title') != project.get('name'):
                errors.append(f'C11: {rel}: title "{fm.get("title")}" must equal the project name "{project.get("name")}"')
            if fm.get('description') != project.get('tagline'):
                errors.append(f'C11: {rel}: description must equal the project tagline')
        elif stem == 'index':
            if not fm.get('title') or not fm.get('description'):
                errors.append(f'C11: {rel}: needs a title and a description')
        if stem in by_slug or stem == 'index':
            if fm.get('hide_table_of_contents') != 'true':
                errors.append(f'C11: {rel}: needs hide_table_of_contents: true')

    # C12: every /docs/topics/ link in a page body is a real topic slug.
    for stem, page in list(pages.items()) + ([('index', overview)] if overview else []):
        text = page.read_text(encoding='utf-8')
        for m in TOPIC_LINK_RE.finditer(text):
            if m.group(1).rstrip('.') not in topics:
                errors.append(f'C12: {ABOUT_DIR}/{stem}.mdx:{_line_of(text, m.start())}: '
                              f'link to unknown topic "{m.group(1)}"')

    # C9: no pre-rename name in the pages or the index.
    sources = [(f'{ABOUT_DIR}/{stem}.mdx', page) for stem, page in pages.items()]
    if overview:
        sources.append((f'{ABOUT_DIR}/index.mdx', overview))
    sources.append((project_index.PROJECTS_FILE, docs_root / project_index.PROJECTS_FILE))
    for rel, page in sources:
        for label, lineno, hit in find_old_names(page.read_text(encoding='utf-8')):
            errors.append(f'C9: {rel}:{lineno}: pre-rename {label} name "{hit}"')
    return errors


def main(argv=None):
    parser = argparse.ArgumentParser(description='Check the Juneau project pages against src/data/projects.ts.')
    parser.add_argument('--docs-dir', default=str(Path(__file__).resolve().parent.parent),
                        help='the docs checkout (default: the parent of scripts/)')
    args = parser.parse_args(argv)
    errors = check(args.docs_dir)
    for line in errors:
        print(line)
    if errors:
        return 1
    _, projects = project_index.load(args.docs_dir)
    print(f'Project pages OK ({len(projects)} projects)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
