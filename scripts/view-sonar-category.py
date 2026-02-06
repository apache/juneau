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
Helper script to view SonarQube issue categories without interactive input.
Usage: view-sonar-category.py <category-name> [--limit N]
"""

import json
import sys
from pathlib import Path

# NOSONAR -- S3776: Cognitive complexity is acceptable for this script function
def main():
    json_file = Path('/Users/james.bognar/Downloads/SonarQubeIssues.categorized.json')
    
    if not json_file.exists():
        print(f"Error: Categorized JSON file not found: {json_file}")
        print("Please run categorize-sonar-issues.py first with --save-json")
        sys.exit(1)
    
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    if len(sys.argv) < 2:
        print("Available categories:")
        print("=" * 80)
        sorted_cats = sorted(data.items(), key=lambda x: len(x[1]), reverse=True)
        for i, (category, issues) in enumerate(sorted_cats, 1):
            print(f"{i:2d}. {category:<50} {len(issues):>5} issues")
        sys.exit(0)
    
    category_name = sys.argv[1]
    limit = int(sys.argv[3]) if len(sys.argv) > 3 and sys.argv[2] == '--limit' else 20
    
    if category_name not in data:
        print(f"Error: Category '{category_name}' not found.")
        print("\nAvailable categories:")
        for cat in sorted(data.keys()):
            print(f"  - {cat}")
        sys.exit(1)
    
    issues = data[category_name]
    
    print("=" * 80)
    print(f"CATEGORY: {category_name}")
    print(f"Total Issues: {len(issues)}")
    print("=" * 80)
    
    # Group by file
    by_file = {}
    for issue in issues:
        file_key = f"{issue['path']}/{issue['resource']}"
        if file_key not in by_file:
            by_file[file_key] = []
        by_file[file_key].append(issue)
    
    print(f"\nAffected Files: {len(by_file)}\n")
    
    # Show sample issues
    print(f"Sample Issues (showing first {min(limit, len(issues))}):")
    print("-" * 80)
    for i, issue in enumerate(issues[:limit], 1):
        print(f"\n{i}. {issue['description']}")
        print(f"   File: {issue['path']}/{issue['resource']}:{issue['location']}")
        print(f"   ID: {issue['id']}")
    
    if len(issues) > limit:
        print(f"\n... and {len(issues) - limit} more issues")
    
    print("\n" + "=" * 80)
    print("Files affected:")
    print("-" * 80)
    sorted_files = sorted(by_file.items(), key=lambda x: len(x[1]), reverse=True)
    for file_path, file_issues in sorted_files[:20]:
        print(f"  {file_path}: {len(file_issues)} issues")
    
    if len(by_file) > 20:
        print(f"  ... and {len(by_file) - 20} more files")

if __name__ == '__main__':
    main()
