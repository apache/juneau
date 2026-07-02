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

Starts the Docusaurus development server. Kills any existing process on port
3000 before starting. Use --clean to also clear all caches (useful when pages
are stale or webpack is behaving unexpectedly).

Usage:
    python3 start-docusaurus.py [--clean]

Options:
    --clean     Clear .docusaurus/, build/, and node_modules/.cache before starting.
"""

import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path


def print_step(message):
    """Print a step message with emoji."""
    print(f"{message}")


def resolve_javadocs_version(docs_dir):
    """Resolve the current javadocs version robustly for the apidocs symlink.

    Prefers the latest entry in static/javadocs/releases.json that has a matching
    committed static/javadocs/<version>/ directory; falls back to the sole versioned
    directory when the index is unavailable. Returns None if none can be determined.
    """
    javadocs_dir = docs_dir / "static" / "javadocs"
    releases_json = javadocs_dir / "releases.json"

    def version_key(v):
        try:
            return tuple(int(p) if p.isdigit() else p for p in v.split('.'))
        except (ValueError, AttributeError):
            return v

    if releases_json.is_file():
        try:
            with open(releases_json) as f:
                releases = json.load(f)
            versions = [r.get("version") for r in releases if r.get("version")]
            versions = [v for v in versions if (javadocs_dir / v).is_dir()]
            if versions:
                versions.sort(key=version_key, reverse=True)
                return versions[0]
        except Exception as e:
            print_step(f"⚠️  Could not read {releases_json}: {e}")

    if javadocs_dir.is_dir():
        subdirs = [d.name for d in javadocs_dir.iterdir()
                   if d.is_dir() and d.name[:1].isdigit()]
        if len(subdirs) == 1:
            return subdirs[0]

    return None


def ensure_apidocs_symlink(docs_dir):
    """Point static/site/apidocs at the committed static/javadocs/<version> tree.

    Creates a RELATIVE symlink (target '../javadocs/<version>') so the unversioned
    /site/apidocs/... links resolve against the committed javadocs with no Maven build.
    Idempotent, replaces an existing real directory, and skips (with a warning) when the
    committed javadocs are missing rather than failing startup.
    """
    version = resolve_javadocs_version(docs_dir)
    if not version:
        print_step("⚠️  Could not resolve javadocs version — skipping apidocs symlink")
        return

    static_site = docs_dir / "static" / "site"
    apidocs = static_site / "apidocs"
    target = os.path.join("..", "javadocs", version)
    versioned = docs_dir / "static" / "javadocs" / version

    if not versioned.is_dir():
        print_step(f"⚠️  Committed javadocs not found at {versioned} — skipping apidocs symlink")
        return

    static_site.mkdir(parents=True, exist_ok=True)

    if apidocs.is_symlink():
        if os.readlink(apidocs) == target:
            print_step(f"🔗 apidocs already links to {target}")
            return
        apidocs.unlink()
    elif apidocs.is_dir():
        shutil.rmtree(apidocs)
    elif apidocs.exists():
        apidocs.unlink()

    os.symlink(target, apidocs)
    print_step(f"🔗 Linked static/site/apidocs -> {target}")


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


def install_dependencies(docs_dir):
    """Install npm dependencies if node_modules doesn't exist."""
    node_modules = docs_dir / "node_modules"
    if not node_modules.exists():
        print_step("📦 Installing dependencies (this may take a few minutes)...")
        try:
            subprocess.run(
                ["npm", "install"],
                cwd=docs_dir,
                check=True
            )
            print_step("✅ Dependencies installed successfully")
        except subprocess.CalledProcessError:
            print("❌ ERROR: Failed to install dependencies")
            return False
    return True


def main():
    import argparse
    parser = argparse.ArgumentParser(description='Start Docusaurus dev server')
    parser.add_argument('--clean', action='store_true',
                        help='Clear .docusaurus/, build/, and node_modules/.cache before starting')
    args = parser.parse_args()

    # Get directories
    script_dir = Path(__file__).parent
    docs_dir = script_dir.parent  # docs/
    
    print_step("🔄 Starting Docusaurus server...")
    print_step(f"📁 Working directory: {docs_dir}")
    
    # Verify package.json exists
    package_json = docs_dir / "package.json"
    if not package_json.exists():
        print(f"❌ ERROR: package.json not found in {docs_dir}")
        return 1
    
    # Install dependencies if needed
    if not install_dependencies(docs_dir):
        return 1
    
    # Only kill port 3000 if the dev-server lock file is NOT present.
    # If the lock file exists, another instance of the server is intentionally
    # running and we should not terminate it.
    lock_file_check = docs_dir / '.dev-server-running'
    if lock_file_check.exists():
        print("⚠️  Dev server lock file found — another instance is already running. Exiting to avoid cache conflicts.")
        return 0
    kill_port_3000()
    
    # Clear caches only when explicitly requested
    if args.clean:
        clear_caches(docs_dir)

    # Point static/site/apidocs at the committed javadocs so /site/apidocs/... links
    # work with no Maven build. Non-fatal if the committed javadocs are missing.
    ensure_apidocs_symlink(docs_dir)

    # Start the server
    print_step("🚀 Starting Docusaurus server...")
    print()

    # Write a lock file so build-docs.py knows the dev server is running
    # and skips the production Docusaurus build to avoid webpack cache conflicts.
    lock_file = docs_dir / '.dev-server-running'
    lock_file.touch()

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
    finally:
        # Remove the lock file when the server stops
        try:
            lock_file.unlink(missing_ok=True)
        except Exception:
            pass

    return 0


if __name__ == "__main__":
    sys.exit(main())

