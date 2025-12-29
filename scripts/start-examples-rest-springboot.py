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
Start Juneau REST Examples (Spring Boot) Application

This script starts the Juneau REST examples application using Spring Boot.
It runs the org.apache.juneau.examples.rest.springboot.App main class using Maven's Spring Boot plugin.

Usage:
    python3 scripts/start-examples-rest-springboot.py
"""

import os
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
    """Start the Spring Boot examples application."""
    print(f"{BLUE}🚀 Starting Juneau REST Examples (Spring Boot)...{RESET}")
    
    # Find project root
    project_root = find_project_root()
    if not project_root:
        print(f"{RED}[ERROR] Could not find project root directory{RESET}")
        sys.exit(1)
    
    # Set working directory to the examples-rest-springboot module
    examples_dir = project_root / 'juneau-examples' / 'juneau-examples-rest-springboot'
    
    if not examples_dir.exists():
        print(f"{RED}[ERROR] Examples directory not found: {examples_dir}{RESET}")
        sys.exit(1)
    
    print(f"{BLUE}📁 Working directory: {examples_dir}{RESET}")
    
    # Check if project is built
    target_dir = examples_dir / 'target' / 'classes'
    if not target_dir.exists():
        print(f"{YELLOW}⚠️  Classes not found. Building project first...{RESET}")
        print(f"{BLUE}🔨 Running: mvn clean compile{RESET}\n")
        
        result = subprocess.run(
            ['mvn', 'clean', 'compile'],
            cwd=str(examples_dir),
            stdout=sys.stdout,
            stderr=sys.stderr
        )
        
        if result.returncode != 0:
            print(f"\n{RED}[ERROR] Build failed. Please fix compilation errors.{RESET}")
            sys.exit(1)
        
        print(f"\n{GREEN}✓ Build completed successfully{RESET}\n")
    
    # Run the application
    print(f"{BLUE}🚀 Starting Spring Boot application...{RESET}")
    print(f"{BLUE}Main class: org.apache.juneau.examples.rest.springboot.App{RESET}")
    print(f"{BLUE}Default URL: http://localhost:5000{RESET}")
    print(f"\n{YELLOW}Press Ctrl+C to stop the server{RESET}\n")
    print("=" * 80)
    
    try:
        # Use Spring Boot Maven plugin to run the application
        cmd = [
            'mvn',
            'spring-boot:run'
        ]
        
        process = subprocess.run(
            cmd,
            cwd=str(examples_dir),
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

