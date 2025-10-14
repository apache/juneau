#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Cleans up whitespace inconsistencies in Java files:
- Removes consecutive blank lines (max 1 blank line allowed)
- Removes trailing whitespace from lines
- Removes blank lines before the final closing brace
- Ensures file ends with "}" with no trailing newline
"""

import os
import sys
import re
from pathlib import Path


def clean_java_file(file_path):
    """
    Clean whitespace issues in a Java file.
    Returns True if file was modified, False otherwise.
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        lines = content.splitlines(keepends=True)
        
        # Step 1: Remove trailing whitespace from each line
        lines = [line.rstrip() + ('\n' if line.endswith('\n') else '') for line in lines]
        
        # Step 2: Remove consecutive blank lines (keep max 1)
        cleaned_lines = []
        prev_blank = False
        for line in lines:
            is_blank = line.strip() == ''
            if is_blank and prev_blank:
                # Skip consecutive blank line
                continue
            cleaned_lines.append(line)
            prev_blank = is_blank
        
        # Step 3: Remove blank lines before final closing brace
        # Find the last non-empty line
        while cleaned_lines and cleaned_lines[-1].strip() == '':
            cleaned_lines.pop()
        
        # Now remove any blank lines right before the final '}'
        # Work backwards from the end
        if cleaned_lines:
            # Find the last closing brace
            last_line_idx = len(cleaned_lines) - 1
            if cleaned_lines[last_line_idx].strip() == '}':
                # Remove blank lines before this closing brace
                idx = last_line_idx - 1
                while idx >= 0 and cleaned_lines[idx].strip() == '':
                    cleaned_lines.pop(idx)
                    idx -= 1
        
        # Step 4: Remove trailing newline after final }
        new_content = ''.join(cleaned_lines)
        # Remove any trailing newlines
        new_content = new_content.rstrip('\n')
        # Ensure we end with at least the closing brace (no newline after it)
        if new_content and not new_content.endswith('}'):
            # This shouldn't happen for valid Java files, but just in case
            new_content += '\n'
        
        # Check if content changed
        if new_content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            return True
        
        return False
    
    except Exception as e:
        print(f"Error processing {file_path}: {e}", file=sys.stderr)
        return False


def find_java_files(root_dir):
    """
    Find all .java files in the given directory recursively.
    Excludes target/ and .git/ directories.
    """
    java_files = []
    root_path = Path(root_dir)
    
    for java_file in root_path.rglob('*.java'):
        # Skip target and .git directories
        if 'target' in java_file.parts or '.git' in java_file.parts:
            continue
        java_files.append(java_file)
    
    return java_files


def main():
    """
    Main entry point for the script.
    """
    if len(sys.argv) > 1:
        root_dir = sys.argv[1]
    else:
        # Default to parent directory of this script (assumes script is in /juneau/scripts)
        script_dir = Path(__file__).parent
        root_dir = script_dir.parent
    
    print(f"Scanning for Java files in: {root_dir}")
    java_files = find_java_files(root_dir)
    print(f"Found {len(java_files)} Java files")
    
    modified_count = 0
    for java_file in java_files:
        if clean_java_file(java_file):
            modified_count += 1
            print(f"✓ Cleaned: {java_file}")
    
    print(f"\nSummary:")
    print(f"  Total files scanned: {len(java_files)}")
    print(f"  Files modified: {modified_count}")
    print(f"  Files unchanged: {len(java_files) - modified_count}")


if __name__ == '__main__':
    main()

