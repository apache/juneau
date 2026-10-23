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
Starts the Juneau petstore sample application on the embedded Jetty microservice, with plain `java` and a cached Maven classpath.

Full docs: start-petstore-jetty.md
"""

import argparse
import os
import socket
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

DEFAULT_PORT = 10000

CLASSPATH_FILE = 'petstore-classpath.txt'
MAIN_CLASS = 'org.apache.juneau.petstore.jetty.App'

def port_in_use(port):
    """True if something is already listening on localhost:port."""
    try:
        with socket.create_connection(('localhost', port), timeout=2):
            return True
    except OSError:
        return False

def port_holder(port):
    """A description of the process listening on port ('PID 123 (java)'), or None if it can't be found."""
    try:
        r = subprocess.run(['lsof', '-nP', f'-iTCP:{port}', '-sTCP:LISTEN', '-Fpc'],
                           capture_output=True, text=True, timeout=10)
    except (OSError, subprocess.SubprocessError):
        return None
    pid = cmd = None
    for line in r.stdout.splitlines():
        if line.startswith('p') and pid is None:
            pid = line[1:]
        elif line.startswith('c') and cmd is None:
            cmd = line[1:]
    if pid is None:
        return None
    return f'PID {pid}' + (f' ({cmd})' if cmd else '')

def port_busy_message(port, holder):
    """The message printed when the requested port is already bound."""
    by = f' by {holder}' if holder else ' (holder not found; try: lsof -nP -iTCP:%d -sTCP:LISTEN)' % port
    return f'Port {port} is already in use{by}.  Stop it or choose another port with --port N.'

def classpath_command(output_file):
    """The Maven command that writes the module's runtime classpath to output_file."""
    return ['mvn', '-q', 'dependency:build-classpath', f'-Dmdep.outputFile={output_file}']

def launch_command(classpath, classes_dir, port):
    """The java command that runs the petstore on port."""
    java = str(Path(os.environ['JAVA_HOME']) / 'bin' / 'java') if os.environ.get('JAVA_HOME') else 'java'
    return [java, f'-Dpetstore.port={port}', '-cp', f'{classes_dir}{os.pathsep}{classpath}', MAIN_CLASS]

def resolve_classpath(petstore_dir):
    """Returns the module's dependency classpath, running dependency:build-classpath when the cached copy is stale."""
    cache = petstore_dir / 'target' / CLASSPATH_FILE
    pom = petstore_dir / 'pom.xml'
    if not cache.exists() or cache.stat().st_mtime < pom.stat().st_mtime:
        result = subprocess.run(classpath_command(cache), cwd=str(petstore_dir),
                                stdout=sys.stdout, stderr=sys.stderr)
        if result.returncode != 0 or not cache.exists():
            return None
    return cache.read_text(encoding='utf-8').strip()

def find_project_root():
    """Find the Juneau project root directory."""
    current = Path(__file__).resolve().parent
    while current != current.parent:
        if (current / 'pom.xml').exists():
            return current
        current = current.parent
    return None

def parse_args(argv=None):
    p = argparse.ArgumentParser(description='Start the Juneau petstore (Jetty).')
    p.add_argument('--port', type=int, default=DEFAULT_PORT, metavar='N',
                   help=f'port to listen on (default {DEFAULT_PORT})')
    args = p.parse_args(argv)
    if not 1 <= args.port <= 65535:
        p.error('--port must be between 1 and 65535')
    return args

def main():
    """Start the petstore Jetty application."""
    args = parse_args()
    if port_in_use(args.port):
        print(f"{RED}[ERROR] {port_busy_message(args.port, port_holder(args.port))}{RESET}", file=sys.stderr)
        sys.exit(2)
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

    classpath = resolve_classpath(petstore_dir)
    if classpath is None:
        print(f"{RED}[ERROR] Could not resolve the petstore classpath (dependency:build-classpath failed){RESET}")
        sys.exit(1)

    print(f"{BLUE}🚀 Starting Jetty/Microservice...{RESET}")
    print(f"{BLUE}Main class: {MAIN_CLASS}{RESET}")
    print(f"{BLUE}URL: http://localhost:{args.port}{RESET}")
    print(f"\n{YELLOW}Press Ctrl+C to stop the server{RESET}\n")
    print("=" * 80)

    try:
        cmd = launch_command(classpath, target_dir, args.port)

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
