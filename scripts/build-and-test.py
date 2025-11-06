#!/usr/bin/env python3
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""
Build and test helper script for Apache Juneau.

Usage:
    ./scripts/build-and-test.py [options]

Options:
    --build-only, -b    Only build (skip tests)
    --test-only, -t     Only run tests (no build)
    --full, -f          Clean build + run tests (default)
    --verbose, -v       Show full Maven output
    --help, -h          Show this help message
"""

import subprocess
import sys
import os

def run_command(cmd, verbose=False):
    """Run a command and return exit code."""
    print(f"Running: {cmd}")
    print("-" * 80)
    
    if verbose:
        # Show full output
        result = subprocess.run(cmd, shell=True, cwd="/Users/james.bognar/git/juneau")
        return result.returncode
    else:
        # Show only last lines
        result = subprocess.run(
            f"{cmd} 2>&1 | tail -50",
            shell=True,
            cwd="/Users/james.bognar/git/juneau",
            capture_output=True,
            text=True
        )
        print(result.stdout)
        if result.stderr:
            print(result.stderr, file=sys.stderr)
        return result.returncode

def build(verbose=False):
    """Run Maven clean install without tests."""
    return run_command("mvn clean install -q -DskipTests", verbose)

def test(verbose=False):
    """Run Maven tests."""
    return run_command("mvn test -q -Drat.skip=true", verbose)

def main():
    args = sys.argv[1:]
    
    # Parse arguments
    build_only = False
    test_only = False
    full = True
    verbose = False
    
    for arg in args:
        if arg in ['--help', '-h']:
            print(__doc__)
            return 0
        elif arg in ['--build-only', '-b']:
            build_only = True
            full = False
        elif arg in ['--test-only', '-t']:
            test_only = True
            full = False
        elif arg in ['--full', '-f']:
            full = True
        elif arg in ['--verbose', '-v']:
            verbose = True
        else:
            print(f"Unknown option: {arg}")
            print(__doc__)
            return 1
    
    # Execute commands
    exit_code = 0
    
    if build_only or full:
        exit_code = build(verbose)
        if exit_code != 0:
            print("\n❌ Build failed!")
            return exit_code
        print("\n✅ Build succeeded!")
    
    if test_only or full:
        if full:
            print("\n" + "=" * 80)
        exit_code = test(verbose)
        if exit_code != 0:
            print("\n❌ Tests failed!")
            return exit_code
        print("\n✅ Tests passed!")
    
    return exit_code

if __name__ == '__main__':
    sys.exit(main())

