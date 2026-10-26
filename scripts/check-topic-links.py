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
Check for correct topic links in the Juneau source tree.

Full docs: check-topic-links.md
"""

import argparse
import html
import os
import re
import sys
from pathlib import Path

from juneau_root import find_juneau_root

def github_anchor(heading):
    """Docusaurus (github-slugger) heading id: lowercase, drop punctuation, spaces to hyphens."""
    h = heading.strip().lower().replace('`', '')
    return re.sub(r'[^a-z0-9 \-_]', '', h).replace(' ', '-')


def heading_anchors(content):
    """Return a dict of heading id -> heading text for a page (explicit {#id} or generated id), skipping code fences.

    The heading text has any {#id} suffix and backticks removed.
    """
    anchors = {}
    seen = {}
    in_fence = False
    for line in content.split('\n'):
        if line.lstrip().startswith('```'):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        m = re.match(r'^#{1,6}\s+(.*?)\s*$', line)
        if not m:
            continue
        text = m.group(1)
        explicit = re.search(r'\s*\{#([^}\s]+)\}\s*$', text)
        if explicit:
            anchors[explicit.group(1)] = text[:explicit.start()].replace('`', '').strip()
            continue
        base = github_anchor(text)
        n = seen.get(base, 0)
        seen[base] = n + 1
        anchors[base if n == 0 else f"{base}-{n}"] = text.replace('`', '').strip()
    return anchors


def extract_topic_info(docs_dir, anchors_out=None):
    """Extract slug and title information from all topic markdown files.

    When anchors_out is a dict, it is filled with slug -> {heading id: heading text}.
    """
    topics = {}
    topics_dir = Path(docs_dir) / "pages" / "topics"
    
    if not topics_dir.exists():
        print(f"ERROR: Topics directory not found: {topics_dir}")
        return topics
    
    for md_file in topics_dir.glob("*.md"):
        if md_file.name == "README.md":
            continue
            
        try:
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Extract frontmatter
            frontmatter_match = re.search(r'^---\n(.*?)\n---', content, re.DOTALL)
            if frontmatter_match:
                frontmatter = frontmatter_match.group(1)
                
                # Extract title and slug
                title_match = re.search(r'^title:\s*["\']?([^"\']+)["\']?', frontmatter, re.MULTILINE)
                slug_match = re.search(r'^slug:\s*([^\s]+)', frontmatter, re.MULTILINE)
                
                if title_match and slug_match:
                    title = title_match.group(1).strip()
                    slug = slug_match.group(1).strip()
                    topics[slug] = title
                    if anchors_out is not None:
                        anchors_out[slug] = heading_anchors(content[frontmatter_match.end():])
                    print(f"Found topic: {slug} -> {title}")
                else:
                    print(f"WARNING: Could not extract title/slug from {md_file.name}")
            else:
                print(f"WARNING: No frontmatter found in {md_file.name}")
                
        except Exception as e:
            print(f"ERROR: Failed to process {md_file.name}: {e}")
    
    return topics

def find_topic_links(source_dir):
    """Find all topic links in the source tree."""
    links = []
    
    # Pattern to match topic links
    link_pattern = re.compile(r'https://juneau\.apache\.org/docs/topics/([^"]+)">([^<]+)</a>')
    
    # File extensions to search
    extensions = {'.java', '.md', '.xml', '.properties', '.txt', '.adoc', '.rst'}
    
    for root, dirs, files in os.walk(source_dir):
        # Skip certain directories
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in {'target', 'node_modules', 'build', 'dist', 'scripts'}]
        
        for file in files:
            # Skip report files generated by this script
            if file.startswith('topic-link-check-') and file.endswith('.txt'):
                continue
                
            if any(file.endswith(ext) for ext in extensions):
                file_path = Path(root) / file
                try:
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        content = f.read()
                        
                        # For markdown files, exclude code blocks
                        if file.endswith('.md'):
                            # Remove code blocks (```...```) from content before searching
                            import re as re_module
                            code_block_pattern = re_module.compile(r'```.*?```', re_module.DOTALL)
                            content = code_block_pattern.sub('', content)
                            # Also remove inline code (`...`)
                            inline_code_pattern = re_module.compile(r'`[^`]+`')
                            content = inline_code_pattern.sub('', content)
                        
                        for match in link_pattern.finditer(content):
                            slug = match.group(1)
                            title = match.group(2)
                            links.append({
                                'file': str(file_path.relative_to(source_dir)),
                                'line': content[:match.start()].count('\n') + 1,
                                'slug': slug,
                                'title': title,
                                'full_match': match.group(0)
                            })
                except Exception as e:
                    print(f"ERROR: Failed to process {file_path}: {e}")
    
    return links

def check_links(links, topics, anchors=None):
    """Check if the found links match the expected slug/title combinations.

    A slug may carry a "#anchor" suffix: the slug part is resolved as usual, then the
    anchor must be a heading id of the target page (when anchors is given). A link with a
    resolving anchor may read either the page title or that anchor's heading text.
    """
    warnings = []
    
    for link in links:
        full_slug = link['slug']
        slug, _, anchor = full_slug.partition('#')
        title = html.unescape(link['title'])  # Javadoc link text is HTML, so '&amp;' reads as '&'
        
        if slug in topics and anchor and anchors is not None and anchor not in anchors.get(slug, {}):
            warnings.append({
                'type': 'missing_anchor',
                'file': link['file'],
                'line': link['line'],
                'slug': slug,
                'anchor': anchor,
                'full_match': link['full_match']
            })
        elif slug in topics:
            expected_title = topics[slug]
            heading = anchors.get(slug, {}).get(anchor) if anchor and anchors is not None else None
            if title != expected_title and title != heading:
                warnings.append({
                    'type': 'title_mismatch',
                    'file': link['file'],
                    'line': link['line'],
                    'slug': slug,
                    'expected_title': expected_title,
                    'expected_heading': heading,
                    'actual_title': title,
                    'full_match': link['full_match']
                })
        else:
            warnings.append({
                'type': 'unknown_slug',
                'file': link['file'],
                'line': link['line'],
                'slug': full_slug,
                'title': title,
                'full_match': link['full_match']
            })
    
    return warnings

def parse_args():
    script_dir = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description="Check Juneau topic links against the docs topic pages.")
    parser.add_argument("--docs-root", type=Path, default=script_dir.parent,
                        help="juneau-docs checkout root (default: parent of this script's folder)")
    parser.add_argument("--juneau-root", type=Path, default=None,
                        help="Juneau source checkout to scan (default: $JUNEAU_ROOT, then sibling ../master, then ../juneau)")
    return parser.parse_args()


def main():
    args = parse_args()
    docs_dir = args.docs_root.resolve()
    master_root = find_juneau_root(docs_dir, args.juneau_root)
    
    print("Juneau Topic Link Checker")
    print("=" * 50)
    
    # Extract topic information from docs
    print("\nExtracting topic information from docs...")
    anchors = {}
    topics = extract_topic_info(docs_dir, anchors)
    
    if not topics:
        print("ERROR: No topics found. Check if docs directory exists and contains topic files.")
        sys.exit(1)
    
    print(f"\nFound {len(topics)} topics")
    
    # Find all topic links in master branch
    print("\nScanning master branch source tree for topic links...")
    links = find_topic_links(master_root)
    
    print(f"Found {len(links)} topic links")
    
    # Check for issues
    print("\nChecking links for issues...")
    warnings = check_links(links, topics, anchors)
    
    # Report results
    if warnings:
        print(f"\nWARNINGS ({len(warnings)} found):")
        print("=" * 50)
        
        for warning in warnings:
            if warning['type'] == 'title_mismatch':
                print(f"TITLE MISMATCH:")
                print(f"  File: {warning['file']}:{warning['line']}")
                print(f"  Slug: {warning['slug']}")
                print(f"  Expected title: '{warning['expected_title']}'")
                if warning['expected_heading']:
                    print(f"  Or heading:     '{warning['expected_heading']}'")
                print(f"  Actual title:   '{warning['actual_title']}'")
                print(f"  Link: {warning['full_match']}")
                print()
            elif warning['type'] == 'missing_anchor':
                print(f"MISSING ANCHOR:")
                print(f"  File: {warning['file']}:{warning['line']}")
                print(f"  Slug: {warning['slug']}")
                print(f"  Anchor: '{warning['anchor']}'")
                print(f"  Link: {warning['full_match']}")
                print()
            elif warning['type'] == 'unknown_slug':
                print(f"UNKNOWN SLUG:")
                print(f"  File: {warning['file']}:{warning['line']}")
                print(f"  Slug: {warning['slug']}")
                print(f"  Title: '{warning['title']}'")
                print(f"  Link: {warning['full_match']}")
                print()
        
        print(f"Total warnings: {len(warnings)}")
        print("\nNote: Warnings are informational only and do not fail the build.")
        sys.exit(0)  # Exit successfully with warnings
    else:
        print("\n✓ All topic links are correct!")
        sys.exit(0)

if __name__ == "__main__":
    main()
