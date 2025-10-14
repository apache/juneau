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
Start Docusaurus Server Script

Starts the Docusaurus development server, automatically killing any existing
process on port 3000 and clearing caches for a clean start.

Usage:
    python3 start-docusaurus.py
"""

import os
import shutil
import subprocess
import sys
import time
from pathlib import Path


def print_step(message):
    """Print a step message with emoji."""
    print(f"{message}")


def kill_port_3000():
    """Kill any process running on port 3000."""
    print_step("🔍 Checking for existing processes on port 3000...")
    
    try:
        # Try lsof first (macOS/Linux)
        result = subprocess.run(
            ["lsof", "-ti:3000"],
            capture_output=True,
            text=True
        )
        
        if result.stdout.strip():
            pids = result.stdout.strip().split('\n')
            for pid in pids:
                if pid:
                    print_step(f"⚡ Killing existing process on port 3000 (PID: {pid})")
                    try:
                        subprocess.run(["kill", "-9", pid], check=False)
                    except Exception:
                        pass
            time.sleep(2)
    except FileNotFoundError:
        # lsof not available, try netstat (Windows/some Linux)
        try:
            if sys.platform == "win32":
                # Windows: use netstat
                result = subprocess.run(
                    ["netstat", "-ano"],
                    capture_output=True,
                    text=True
                )
                for line in result.stdout.split('\n'):
                    if ':3000' in line and 'LISTENING' in line:
                        parts = line.split()
                        pid = parts[-1]
                        print_step(f"⚡ Killing existing process on port 3000 (PID: {pid})")
                        subprocess.run(["taskkill", "/F", "/PID", pid], check=False)
                        time.sleep(2)
                        break
        except Exception:
            # If all fails, just continue
            pass


def clear_caches(docs_dir):
    """Clear Docusaurus caches."""
    print_step("🧹 Clearing cache...")
    
    cache_dirs = [
        docs_dir / ".docusaurus",
        docs_dir / "build",
        docs_dir / "node_modules" / ".cache"
    ]
    
    for cache_dir in cache_dirs:
        if cache_dir.exists():
            try:
                shutil.rmtree(cache_dir)
                print(f"   Removed: {cache_dir.name}")
            except Exception as e:
                print(f"   Warning: Could not remove {cache_dir.name}: {e}")


def main():
    # Get directories
    script_dir = Path(__file__).parent
    juneau_root = script_dir.parent
    docs_dir = juneau_root / "juneau-docs"
    
    print_step("🔄 Starting Docusaurus server...")
    print_step(f"📁 Working directory: {docs_dir}")
    
    # Verify package.json exists
    package_json = docs_dir / "package.json"
    if not package_json.exists():
        print(f"❌ ERROR: package.json not found in {docs_dir}")
        return 1
    
    # Kill any existing process on port 3000
    kill_port_3000()
    
    # Clear caches
    clear_caches(docs_dir)
    
    # Start the server
    print_step("🚀 Starting Docusaurus server...")
    print()
    
    try:
        subprocess.run(
            ["npm", "start"],
            cwd=docs_dir,
            check=True
        )
    except subprocess.CalledProcessError:
        print()
        print("❌ ERROR: Failed to start Docusaurus server")
        return 1
    except KeyboardInterrupt:
        print()
        print("🛑 Server stopped by user")
        return 0
    
    return 0


if __name__ == "__main__":
    sys.exit(main())

