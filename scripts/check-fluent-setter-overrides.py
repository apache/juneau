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
Script to check for missing fluent setter overrides in the Juneau codebase.

This script:
1. Scans all Java files in the source tree
2. Identifies classes and their parent classes
3. Finds fluent setter methods (methods that return 'this')
4. Checks if subclasses override these setters with the correct return type
5. Reports any missing overrides

A fluent setter is a method that:
- Starts with 'set' or is a builder-style method
- Returns the class type (for method chaining)
- Is public
"""

import os
import re
import sys
from pathlib import Path
from collections import defaultdict

class JavaClass:
    """Represents a Java class with its metadata."""
    def __init__(self, name, file_path, extends=None, package=None):
        self.name = name
        self.file_path = file_path
        self.extends = extends
        self.package = package
        self.fluent_setters = []  # List of (method_name, params, return_type)
        self.overridden_methods = set()  # Set of method signatures that are overridden
        
    def add_fluent_setter(self, method_name, params, return_type, has_beanp=False):
        """Add a fluent setter method."""
        self.fluent_setters.append({
            'name': method_name,
            'params': params,
            'return_type': return_type,
            'has_beanp': has_beanp
        })
    
    def add_overridden_method(self, method_name, params):
        """Add an overridden method."""
        normalized_params = normalize_params(params)
        signature = f"{method_name}({normalized_params})"
        self.overridden_methods.add(signature)
    
    def get_full_name(self):
        """Get the fully qualified class name."""
        if self.package:
            return f"{self.package}.{self.name}"
        return self.name

def normalize_params(params):
    """
    Normalize parameter string to types only (remove parameter names).
    E.g., "Object value, String name" -> "Object,String"
    """
    if not params or not params.strip():
        return ""
    
    # Split by comma
    param_list = params.split(',')
    types = []
    
    for param in param_list:
        param = param.strip()
        if not param:
            continue
        
        # Remove annotations (e.g., @NotNull)
        param = re.sub(r'@\w+\s+', '', param)
        
        # Split by whitespace and take all but the last token (which is the parameter name)
        # E.g., "Object value" -> ["Object", "value"] -> "Object"
        # E.g., "Map<String,Object> map" -> ["Map<String,Object>", "map"] -> "Map<String,Object>"
        tokens = param.split()
        if len(tokens) >= 2:
            # Everything except the last token is the type
            type_part = ' '.join(tokens[:-1])
        else:
            # Only one token, it's probably just a type (shouldn't happen but handle it)
            type_part = tokens[0] if tokens else param
        
        types.append(type_part.strip())
    
    return ','.join(types)

def extract_package(content):
    """Extract package name from Java file content."""
    match = re.search(r'^\s*package\s+([\w.]+)\s*;', content, re.MULTILINE)
    return match.group(1) if match else None

def extract_class_info(file_path):
    """Extract class information from a Java file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        package = extract_package(content)
        
        # Find class declarations (public class X extends Y)
        class_pattern = re.compile(
            r'^\s*public\s+(?:static\s+)?(?:abstract\s+)?class\s+(\w+)(?:\s+extends\s+([\w.<>, ]+?))?(?:\s+implements\s+[\w.<>, ]+?)?\s*\{',
            re.MULTILINE
        )
        
        classes = []
        for match in class_pattern.finditer(content):
            class_name = match.group(1)
            extends = match.group(2).strip() if match.group(2) else None
            
            # Skip inner classes (they are indented and usually implementation details)
            # We only want top-level public classes
            # Check if the matched text starts with whitespace
            matched_text = match.group(0)
            if matched_text[0].isspace():
                # This is an inner class (indented), skip it
                continue
            
            # Clean up extends (remove generics for simplicity)
            if extends:
                extends = re.sub(r'<.*?>', '', extends).strip()
            
            java_class = JavaClass(class_name, file_path, extends, package)
            
            # Calculate the class body boundaries to avoid finding methods in inner classes
            # Find the opening brace of the class
            class_body_start = match.end()
            
            # Find the matching closing brace (approximate - find next top-level closing brace)
            # This is a simplification; we'll search until the next top-level class or EOF
            next_class_match = class_pattern.search(content, class_body_start)
            if next_class_match:
                # If there's another top-level class, search up to that
                class_body_end = next_class_match.start()
            else:
                # Otherwise, search to end of file
                class_body_end = len(content)
            
            # Extract just the class body content
            class_body = content[class_body_start:class_body_end]
            
            # Find fluent setters in this class
            # Pattern: public ClassName methodName(...) { ... return this; }
            # We look for methods that return the class type
            method_pattern = re.compile(
                rf'^\s*(?:@\w+\s+)*public\s+{re.escape(class_name)}\s+(\w+)\s*\((.*?)\)\s*\{{',
                re.MULTILINE
            )
            
            for method_match in method_pattern.finditer(class_body):
                method_name = method_match.group(1)
                params = method_match.group(2).strip()
                
                # Check if method has @Beanp annotation
                # Look backwards from the method declaration to check for annotations
                method_start_pos = method_match.start()
                preceding_text = class_body[max(0, method_start_pos - 200):method_start_pos]
                has_beanp = '@Beanp' in preceding_text
                
                # Check if this method returns 'this'
                # Look ahead to see if there's a 'return this;' in the method body
                method_start = method_match.end()
                # Find the matching closing brace (simplified - just look for return this)
                method_body_sample = class_body[method_start:method_start + 500]
                if 'return this;' in method_body_sample or 'return this' in method_body_sample:
                    java_class.add_fluent_setter(method_name, params, class_name, has_beanp)
            
            # Find overridden methods (also within the class body scope)
            override_pattern = re.compile(
                r'@Override[^\n]*\n\s*public\s+\w+\s+(\w+)\s*\((.*?)\)',
                re.MULTILINE
            )
            
            for override_match in override_pattern.finditer(class_body):
                method_name = override_match.group(1)
                params = override_match.group(2).strip()
                java_class.add_overridden_method(method_name, params)
            
            classes.append(java_class)
        
        return classes
    
    except Exception as e:
        print(f"ERROR: Failed to process {file_path}: {e}", file=sys.stderr)
        return []

def find_java_files(source_dir):
    """Find all Java files in the source tree, excluding test directories."""
    java_files = []
    
    for root, dirs, files in os.walk(source_dir):
        # Skip certain directories including test directories
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in {'target', 'node_modules', 'build', 'dist'}]
        
        # Skip test directories (src/test, juneau-utest, etc.)
        root_path = Path(root)
        if any(part in ['test', 'juneau-utest'] for part in root_path.parts):
            continue
        if 'src/test' in str(root_path) or '/test/' in str(root_path):
            continue
        
        for file in files:
            if file.endswith('.java'):
                # Also skip files with "Test" suffix
                if not file.endswith('Test.java') and not file.endswith('Tests.java'):
                    java_files.append(Path(root) / file)
    
    return java_files

def build_class_hierarchy(classes):
    """Build a hierarchy of classes by name."""
    class_map = {}  # Maps class name to JavaClass objects (may have duplicates)
    
    for java_class in classes:
        class_name = java_class.name
        if class_name not in class_map:
            class_map[class_name] = []
        class_map[class_name].append(java_class)
    
    return class_map

def check_missing_overrides(classes, class_map):
    """Check for missing fluent setter overrides."""
    missing_overrides = []
    
    for java_class in classes:
        if not java_class.extends:
            continue
        
        parent_name = java_class.extends
        
        # Find parent class
        if parent_name not in class_map:
            continue
        
        # Get all parent classes with this name (there may be multiple in different packages)
        parent_classes = class_map[parent_name]
        
        # Filter parent classes by package to avoid false positives from name collisions
        # Try to match parent classes from the same package or commonly imported packages
        filtered_parents = []
        for pc in parent_classes:
            # If child and parent are in the same package, use it
            if java_class.package == pc.package:
                filtered_parents.append(pc)
            # Otherwise, if there's only one parent class with this name, use it
            elif len(parent_classes) == 1:
                filtered_parents.append(pc)
        
        # If we couldn't filter by package, fall back to all parent classes
        # (This maintains backward compatibility but may produce false positives)
        if not filtered_parents:
            filtered_parents = parent_classes
        
        # Check each parent class
        for parent_class in filtered_parents:
            # Check each fluent setter in the parent
            for setter in parent_class.fluent_setters:
                method_name = setter['name']
                params = setter['params']
                normalized_params = normalize_params(params)
                signature = f"{method_name}({normalized_params})"
                
                # Skip methods marked with @Beanp in the parent
                # These are automatically handled by annotation inheritance (since 9.2.0)
                # and are less critical for type safety
                if setter.get('has_beanp'):
                    continue
                
                # Check if this method is overridden in the child class
                if signature not in java_class.overridden_methods:
                    # Also check if the child has this as a fluent setter
                    # (it might define it without @Override annotation)
                    has_fluent = False
                    for child_setter in java_class.fluent_setters:
                        child_normalized = normalize_params(child_setter['params'])
                        if child_setter['name'] == method_name and child_normalized == normalized_params:
                            has_fluent = True
                            break
                    
                    if not has_fluent:
                        missing_overrides.append({
                            'child_class': java_class.name,
                            'child_file': str(java_class.file_path),
                            'parent_class': parent_class.name,
                            'parent_file': str(parent_class.file_path),
                            'method_name': method_name,
                            'method_params': params,
                            'method_signature': signature
                        })
    
    return missing_overrides

def main():
    """Main entry point."""
    # Get the script directory (should be /juneau/scripts)
    script_dir = Path(__file__).parent
    juneau_root = script_dir.parent
    
    print("Juneau Fluent Setter Override Checker")
    print("=" * 50)
    
    # Find all Java files
    print("\nScanning for Java files...")
    java_files = find_java_files(juneau_root)
    print(f"Found {len(java_files)} Java files")
    
    # Extract class information
    print("\nExtracting class information...")
    all_classes = []
    for java_file in java_files:
        classes = extract_class_info(java_file)
        all_classes.extend(classes)
    
    print(f"Found {len(all_classes)} classes")
    
    # Count fluent setters
    total_fluent_setters = sum(len(c.fluent_setters) for c in all_classes)
    print(f"Found {total_fluent_setters} fluent setter methods")
    
    # Build class hierarchy
    print("\nBuilding class hierarchy...")
    class_map = build_class_hierarchy(all_classes)
    
    # Check for missing overrides
    print("\nChecking for missing fluent setter overrides...")
    missing = check_missing_overrides(all_classes, class_map)
    
    # Report results
    if missing:
        print(f"\nMISSING OVERRIDES ({len(missing)} found):")
        print("=" * 50)
        
        # Group by child class for better readability
        by_class = defaultdict(list)
        for item in missing:
            by_class[item['child_class']].append(item)
        
        for child_class, items in sorted(by_class.items()):
            print(f"\nClass: {child_class}")
            print(f"  File: {items[0]['child_file']}")
            print(f"  Missing {len(items)} override(s):")
            
            for item in items:
                print(f"    - {item['method_name']}({item['method_params']})")
                print(f"      From parent: {item['parent_class']}")
        
        print(f"\n{'=' * 50}")
        print(f"Total missing overrides: {len(missing)}")
        print("\nNote: These are informational and do not fail the build.")
        print("Consider adding these overrides to maintain fluent API consistency.")
    else:
        print("\n✓ All fluent setters are properly overridden!")
    
    sys.exit(0)

if __name__ == "__main__":
    main()

