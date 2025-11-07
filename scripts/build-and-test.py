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
    """Run a command and return exit code and full output."""
    print(f"Running: {cmd}")
    print("-" * 80)
    
    if verbose:
        # Show full output
        result = subprocess.run(cmd, shell=True, cwd="/Users/james.bognar/git/juneau", capture_output=True, text=True)
        print(result.stdout)
        print(result.stderr, file=sys.stderr)
        return result.returncode, result.stdout + result.stderr
    else:
        # Run command and capture all output, then show last lines
        result = subprocess.run(
            cmd,
            shell=True,
            cwd="/Users/james.bognar/git/juneau",
            capture_output=True,
            text=True
        )
        # Combine stdout and stderr
        output = result.stdout + result.stderr
        # Show last 50 lines
        lines = output.splitlines()
        if len(lines) > 50:
            print('\n'.join(lines[-50:]))
        else:
            print(output)
        return result.returncode, output

def build(verbose=False):
    """Run Maven clean install without tests."""
    return run_command("mvn clean install -q -DskipTests", verbose)

def test(verbose=False):
    """Run Maven tests."""
    return run_command("mvn test -q -Drat.skip=true", verbose)

def parse_test_results(output):
    """Parse Maven test output and extract failure/error counts."""
    import re
    # Look for the last occurrence of: [ERROR] Tests run: 25916, Failures: 0, Errors: 12, Skipped: 1
    # This will be the total across all modules
    matches = list(re.finditer(r'\[ERROR\]\s+Tests run:\s+(\d+),\s+Failures:\s+(\d+),\s+Errors:\s+(\d+)', output))
    if matches:
        # Use the last match (final total)
        match = matches[-1]
        total = int(match.group(1))
        failures = int(match.group(2))
        errors = int(match.group(3))
        return total, failures, errors
    return None, None, None

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
        exit_code, output = build(verbose)
        if exit_code != 0:
            print("\n❌ Build failed!")
            return exit_code
        print("\n✅ Build succeeded!")
    
    if test_only or full:
        if full:
            print("\n" + "=" * 80)
        exit_code, output = test(verbose)
        if exit_code != 0:
            # Try to parse test results
            total, failures, errors = parse_test_results(output)
            if failures is not None and errors is not None:
                failed_count = failures + errors
                print(f"\n❌ Tests failed! ({failed_count} failed: {failures} failures, {errors} errors)")
            else:
                print("\n❌ Tests failed!")
            return exit_code
        print("\n✅ Tests passed!")
    
    return exit_code

if __name__ == '__main__':
    sys.exit(main())

