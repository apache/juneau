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
Script to categorize and manage SonarQube issues for interactive fixing.
"""

import csv
import re
from collections import defaultdict
from pathlib import Path
from typing import Dict, List, Tuple
import json

class Issue:
    def __init__(self, description: str, path: str, resource: str, location: str, 
                 issue_type: str, issue_id: str, creation_time: str):
        self.description = description
        self.path = path
        self.resource = resource
        self.location = location
        self.type = issue_type
        self.id = issue_id
        self.creation_time = creation_time
    
    def __repr__(self):
        return f"Issue({self.description[:50]}... @ {self.resource}:{self.location})"

def parse_issues(file_path: str) -> List[Issue]:
    """Parse the SonarQube issues TSV file."""
    issues = []
    with open(file_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f, delimiter='\t')
        for row in reader:
            issue = Issue(
                description=row['Description'],
                path=row['Path'],
                resource=row['Resource'],
                location=row['Location'],
                issue_type=row['Type'],
                issue_id=row['ID'],
                creation_time=row['Creation Time']
            )
            issues.append(issue)
    return issues

def categorize_issue(issue: Issue) -> str:
    """Categorize an issue based on its description."""
    desc = issue.description.lower()
    
    # Field shadowing issues
    if 'is the name of a field' in desc:
        return "Field Shadowing"
    
    # Unnecessary toString() calls
    if 'no need to call "tostring()"' in desc or 'already a string' in desc:
        return "Unnecessary toString() Calls"
    
    # Line separator issues
    if '%n should be used' in desc or ('\\n' in desc and 'line separator' in desc):
        return "Line Separator Issues"
    
    # Brain Method (complexity)
    if 'brain method' in desc:
        return "Brain Methods (Complexity)"
    
    # Deprecated annotation issues
    if 'deprecated annotation' in desc:
        return "Deprecated Annotation Issues"
    
    # Empty method implementations
    if 'empty, throw' in desc or 'empty method' in desc:
        return "Empty Method Implementations"
    
    # Missing private constructor
    if 'private constructor' in desc:
        return "Missing Private Constructors"
    
    # Missing clone() method
    if 'clone()' in desc.lower():
        return "Missing clone() Methods"
    
    # Missing exception handling
    if 'nosuchelementexception' in desc:
        return "Missing Exception Handling"
    
    # Empty catch blocks
    if 'catch clause' in desc and ('logic' in desc or 'eliminate' in desc):
        return "Empty Catch Blocks"
    
    # Test assertion issues
    if 'add at least one assertion' in desc or 'assertion' in desc and 'test' in desc:
        return "Missing Test Assertions"
    
    # Python f-string issues
    if 'f-string' in desc or 'replacement fields' in desc:
        return "Python f-string Issues"
    
    # HTML/documentation issues
    if '<title>' in desc or 'alt' in desc.lower() or ('<th>' in desc and 'attribute' in desc):
        return "HTML/Documentation Issues"
    
    # String concatenation in loops
    if 'string concatenation' in desc or ('stringbuilder' in desc and 'loop' in desc):
        return "String Concatenation Issues"
    
    # Null checks
    if 'null' in desc and ('check' in desc or 'should' in desc or 'must' in desc):
        return "Null Check Issues"
    
    # Generic type issues - check more specifically
    if 'raw type' in desc or ('generic' in desc and ('type' in desc or 'parameter' in desc)):
        return "Generic Type Issues"
    
    # Resource management
    if 'close' in desc.lower() and ('resource' in desc.lower() or 'stream' in desc.lower()):
        return "Resource Management Issues"
    
    # Code duplication
    if 'duplicate' in desc.lower():
        return "Code Duplication"
    
    # Magic numbers
    if 'magic number' in desc:
        return "Magic Numbers"
    
    # Unused code - be more specific
    if 'unused' in desc.lower() and ('parameter' in desc or 'variable' in desc or 'field' in desc or 'method' in desc or 'import' in desc):
        return "Unused Code"
    
    # Security issues
    if 'security' in desc.lower() or 'vulnerability' in desc.lower():
        return "Security Issues"
    
    # Performance issues
    if 'performance' in desc.lower() or 'inefficient' in desc.lower():
        return "Performance Issues"
    
    # Code smell
    if 'code smell' in desc.lower():
        return "Code Smells"
    
    # Missing @Override annotations
    if '@override' in desc or 'override annotation' in desc:
        return "Missing @Override Annotations"
    
    # Missing test coverage
    if 'add some tests' in desc or 'test coverage' in desc:
        return "Missing Test Coverage"
    
    # Missing Javadoc tags
    if '@deprecated' in desc.lower() and 'javadoc' in desc:
        return "Missing Javadoc Tags"
    
    # Singleton pattern issues
    if 'singleton' in desc.lower():
        return "Singleton Pattern Issues"
    
    # Optional access issues
    if 'optional' in desc.lower() and ('ispresent' in desc or 'isempty' in desc or 'get()' in desc):
        return "Optional Access Issues"
    
    # Test assertion issues
    if 'assertion' in desc.lower() and ('compare' in desc or 'dissimilar' in desc or 'primitive' in desc):
        return "Test Assertion Issues"
    
    # Constructor visibility
    if 'constructor' in desc.lower() and 'visibility' in desc.lower():
        return "Constructor Visibility Issues"
    
    # Exception catching issues
    if 'catch' in desc.lower() and ('exception' in desc.lower() or 'throwable' in desc.lower() or 'error' in desc.lower()):
        return "Exception Catching Issues"
    
    # ThreadLocal cleanup
    if 'remove()' in desc.lower() and ('threadlocal' in desc.lower() or 'localstore' in desc.lower()):
        return "ThreadLocal Cleanup Issues"
    
    # Type casting issues
    if 'cast' in desc.lower() and ('operand' in desc.lower() or 'multiplication' in desc.lower() or 'subtraction' in desc.lower()):
        return "Type Casting Issues"
    
    # Default category
    return "Other Issues"

def categorize_all_issues(issues: List[Issue]) -> Dict[str, List[Issue]]:
    """Categorize all issues."""
    categories = defaultdict(list)
    for issue in issues:
        category = categorize_issue(issue)
        categories[category].append(issue)
    return dict(categories)

def print_category_summary(categories: Dict[str, List[Issue]]):
    """Print a summary of all categories."""
    print("\n" + "="*80)
    print("SONARQUBE ISSUES CATEGORIZATION SUMMARY")
    print("="*80)
    print(f"\nTotal Issues: {sum(len(issues) for issues in categories.values())}")
    print(f"Total Categories: {len(categories)}\n")
    
    # Sort by count (descending)
    sorted_categories = sorted(categories.items(), key=lambda x: len(x[1]), reverse=True)
    
    print(f"{'Category':<50} {'Count':>10} {'Percentage':>10}")
    print("-" * 80)
    
    total = sum(len(issues) for issues in categories.values())
    for category, issue_list in sorted_categories:
        count = len(issue_list)
        percentage = (count / total) * 100
        print(f"{category:<50} {count:>10} {percentage:>9.1f}%")
    
    print("\n" + "="*80)

def print_category_details(category: str, issues: List[Issue], limit: int = 10):
    """Print details for a specific category."""
    print(f"\n{'='*80}")
    print(f"CATEGORY: {category}")
    print(f"Total Issues: {len(issues)}")
    print(f"{'='*80}\n")
    
    # Group by file for easier review
    by_file = defaultdict(list)
    for issue in issues:
        key = f"{issue.path}/{issue.resource}"
        by_file[key].append(issue)
    
    print(f"Affected Files: {len(by_file)}\n")
    
    # Show first few examples
    print("Sample Issues (first 10):")
    print("-" * 80)
    for i, issue in enumerate(issues[:limit], 1):
        print(f"{i}. {issue.description}")
        print(f"   File: {issue.path}/{issue.resource}:{issue.location}")
        print()
    
    if len(issues) > limit:
        print(f"... and {len(issues) - limit} more issues\n")

def save_categorized_issues(categories: Dict[str, List[Issue]], output_file: str):
    """Save categorized issues to a JSON file."""
    output_data = {}
    for category, issues in categories.items():
        output_data[category] = [
            {
                'description': issue.description,
                'path': issue.path,
                'resource': issue.resource,
                'location': issue.location,
                'type': issue.type,
                'id': issue.id
            }
            for issue in issues
        ]
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, indent=2, ensure_ascii=False)
    
    print(f"\nCategorized issues saved to: {output_file}")

def interactive_session(categories: Dict[str, List[Issue]]):
    """Run an interactive session to work through categories."""
    sorted_categories = sorted(categories.items(), key=lambda x: len(x[1]), reverse=True)
    
    print("\n" + "="*80)
    print("INTERACTIVE SONARQUBE ISSUE FIXING SESSION")
    print("="*80)
    print("\nCommands:")
    print("  <number> - Select category by number")
    print("  list     - List all categories")
    print("  details <number> - Show details for category")
    print("  next     - Move to next category")
    print("  quit     - Exit")
    print("  help     - Show this help")
    print("="*80)
    
    current_index = 0
    
    while True:
        if current_index >= len(sorted_categories):
            print("\nAll categories have been reviewed!")
            break
        
        category, issues = sorted_categories[current_index]
        count = len(issues)
        
        print(f"\n[{current_index + 1}/{len(sorted_categories)}] Current Category: {category}")
        print(f"  Issues: {count}")
        print(f"\nWhat would you like to do?")
        print(f"  - Type 'fix' to start fixing issues in this category")
        print(f"  - Type 'skip' to move to next category")
        print(f"  - Type 'details' to see issue details")
        print(f"  - Type a number to jump to that category")
        print(f"  - Type 'list' to see all categories")
        print(f"  - Type 'quit' to exit")
        
        command = input("\n> ").strip().lower()
        
        if command == 'quit' or command == 'q':
            print("\nExiting. Progress saved.")
            break
        elif command == 'skip' or command == 'next' or command == 'n':
            current_index += 1
        elif command == 'details' or command == 'd':
            print_category_details(category, issues)
        elif command == 'list' or command == 'l':
            print("\nCategories:")
            for i, (cat, iss) in enumerate(sorted_categories, 1):
                print(f"  {i}. {cat} ({len(iss)} issues)")
        elif command == 'fix' or command == 'f':
            print(f"\nStarting fixes for category: {category}")
            print(f"Total issues: {count}")
            print("\nFor each issue, you can:")
            print("  - Type 'auto' to attempt automatic fix")
            print("  - Type 'skip' to skip this issue")
            print("  - Type 'show' to see the code context")
            print("  - Type 'back' to return to category selection")
            
            # Group by file for batch processing
            by_file = defaultdict(list)
            for issue in issues:
                key = f"{issue.path}/{issue.resource}"
                by_file[key].append(issue)
            
            print(f"\nIssues grouped into {len(by_file)} files.")
            print("Would you like to process by file (recommended) or individually?")
            process_mode = input("  [f]ile/[i]ndividual (default: file): ").strip().lower() or 'f'
            
            if process_mode == 'f':
                for file_path, file_issues in by_file.items():
                    print(f"\n--- File: {file_path} ({len(file_issues)} issues) ---")
                    for issue in file_issues:
                        print(f"\n  Issue: {issue.description}")
                        print(f"  Location: {issue.location}")
                        action = input("  Action [auto/skip/show/back]: ").strip().lower()
                        if action == 'back':
                            break
                        elif action == 'show':
                            print(f"  TODO: Show code at {file_path}:{issue.location}")
                        elif action == 'auto':
                            print(f"  TODO: Auto-fix {issue.description}")
                        elif action == 'skip':
                            continue
                    else:
                        continue
                    break  # Break outer loop if 'back' was used
            else:
                for issue in issues:
                    print(f"\n  Issue: {issue.description}")
                    print(f"  File: {issue.path}/{issue.resource}:{issue.location}")
                    action = input("  Action [auto/skip/show/back]: ").strip().lower()
                    if action == 'back':
                        break
                    elif action == 'show':
                        print(f"  TODO: Show code at {issue.path}/{issue.resource}:{issue.location}")
                    elif action == 'auto':
                        print(f"  TODO: Auto-fix {issue.description}")
                    elif action == 'skip':
                        continue
        elif command.isdigit():
            num = int(command)
            if 1 <= num <= len(sorted_categories):
                current_index = num - 1
            else:
                print(f"Invalid category number. Please enter 1-{len(sorted_categories)}")
        else:
            print("Unknown command. Type 'help' for available commands.")

def main():
    import sys
    
    if len(sys.argv) < 2:
        print("Usage: categorize-sonar-issues.py <sonarqube-issues-file> [--save-json]")
        sys.exit(1)
    
    issues_file = sys.argv[1]
    save_json = '--save-json' in sys.argv
    
    print(f"Parsing issues from: {issues_file}")
    issues = parse_issues(issues_file)
    print(f"Parsed {len(issues)} issues")
    
    print("\nCategorizing issues...")
    categories = categorize_all_issues(issues)
    
    print_category_summary(categories)
    
    if save_json:
        output_file = Path(issues_file).with_suffix('.categorized.json')
        save_categorized_issues(categories, str(output_file))
    
    # Start interactive session
    interactive_session(categories)

if __name__ == '__main__':
    main()
