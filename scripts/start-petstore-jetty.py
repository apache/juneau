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
Start Juneau Petstore (Jetty) Application

This script starts the Juneau petstore sample application using the embedded Jetty/Microservice server.
It runs the org.apache.juneau.petstore.jetty.App main class using Maven's exec plugin.

Usage:
    python3 scripts/start-petstore-jetty.py
"""

import sys
import subprocess
from pathlib import Path

# ANSI color codes
GREEN = '\033[92m'
BLUE = '\033[94m'
YELLOW = '\033[93m'
RED = '\033[91m'
RESET = '\033[0m'
BOLD = '\033[1m'

def find_project_root():
    """Find the Juneau project root directory."""
    current = Path(__file__).resolve().parent
    while current != current.parent:
        if (current / 'pom.xml').exists():
            return current
        current = current.parent
    return None

def main():
    """Start the petstore Jetty application."""
    print(f"{BLUE}🚀 Starting Juneau Petstore (Jetty)...{RESET}")

    project_root = find_project_root()
    if not project_root:
        print(f"{RED}[ERROR] Could not find project root directory{RESET}")
        sys.exit(1)

    petstore_dir = project_root / 'juneau-petstore' / 'juneau-petstore-jetty'

    if not petstore_dir.exists():
        print(f"{RED}[ERROR] Petstore Jetty directory not found: {petstore_dir}{RESET}")
        sys.exit(1)

    print(f"{BLUE}📁 Working directory: {petstore_dir}{RESET}")

    target_dir = petstore_dir / 'target' / 'classes'
    if not target_dir.exists():
        print(f"{YELLOW}⚠️  Classes not found. Building project first...{RESET}")
        print(f"{BLUE}🔨 Running: mvn clean compile{RESET}\n")

        result = subprocess.run(
            ['mvn', 'clean', 'compile'],
            cwd=str(petstore_dir),
            stdout=sys.stdout,
            stderr=sys.stderr
        )

        if result.returncode != 0:
            print(f"\n{RED}[ERROR] Build failed. Please fix compilation errors.{RESET}")
            sys.exit(1)

        print(f"\n{GREEN}✓ Build completed successfully{RESET}\n")

    print(f"{BLUE}🚀 Starting Jetty/Microservice...{RESET}")
    print(f"{BLUE}Main class: org.apache.juneau.petstore.jetty.App{RESET}")
    print(f"{BLUE}Default URL: http://localhost:10000{RESET}")
    print(f"\n{YELLOW}Press Ctrl+C to stop the server{RESET}\n")
    print("=" * 80)

    try:
        cmd = [
            'mvn',
            'exec:java',
            '-Dexec.mainClass=org.apache.juneau.petstore.jetty.App'
        ]

        process = subprocess.run(
            cmd,
            cwd=str(petstore_dir),
            stdout=sys.stdout,
            stderr=sys.stderr
        )

        sys.exit(process.returncode)

    except KeyboardInterrupt:
        print(f"\n\n{YELLOW}🛑 Shutting down server...{RESET}")
        print(f"{GREEN}Server stopped{RESET}")
        sys.exit(0)
    except Exception as e:
        print(f"\n{RED}[ERROR] Failed to start server: {e}{RESET}")
        sys.exit(1)

if __name__ == '__main__':
    main()
