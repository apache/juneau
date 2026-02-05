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
Apache Juneau Release Script

This script automates the release process for Apache Juneau, including:
- Prerequisite checks
- Maven repository cleanup
- Git repository cloning
- Build and verification
- Test workspace creation
- Maven deploy and release
- Binary artifact creation
- SVN distribution upload

The script supports resuming from any step, allowing you to restart at arbitrary points
in the release process.

Usage:
    python3 scripts/release.py [--start-step STEP_NAME] [--list-steps] [--skip-step STEP_NAME] [--resume]

Options:
    --start-step STEP_NAME    Start execution from the specified step (skips all previous steps)
    --list-steps              List all available steps and exit
    --skip-step STEP_NAME     Skip a specific step (can be used multiple times)
    --resume                  Resume from the last checkpoint (if available)
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta
from pathlib import Path

# Constants for duplicated string literals
POM_XML = 'pom.xml'
STAGING_DIR = '~/tmp/dist-release-juneau'
SVN_DIST_URL = 'https://dist.apache.org/repos/dist/dev/juneau'
RC_PATTERN = r'RC(\d+)'
from typing import Dict, Optional, List

# Minimum required versions
MIN_JAVA_VERSION = 17
MIN_MAVEN_VERSION = 3

# State file for checkpoint/resume functionality
STATE_FILE = Path.home() / '.juneau-release-state.json'

class ReleaseState:
    """Manages the release state for checkpoint/resume functionality."""
    
    def __init__(self):
        self.state_file = STATE_FILE
        self.data = self._load()
    
    def _load(self) -> Dict:
        """Load state from file."""
        if self.state_file.exists():
            try:
                with open(self.state_file, 'r') as f:
                    return json.load(f)
            except Exception as e:
                print(f"Warning: Could not load state file: {e}")
        return {}
    
    def save(self):
        """Save current state to file."""
        try:
            with open(self.state_file, 'w') as f:
                json.dump(self.data, f, indent=2)
        except Exception as e:
            print(f"Warning: Could not save state file: {e}")
    
    def get(self, key: str, default=None):
        """Get a state value."""
        return self.data.get(key, default)
    
    def set(self, key: str, value):
        """Set a state value."""
        self.data[key] = value
        self.save()
    
    def clear(self):
        """Clear all state."""
        self.data = {}
        if self.state_file.exists():
            self.state_file.unlink()
    
    def get_last_step(self) -> Optional[str]:
        """Get the last completed step."""
        return self.data.get('last_step')
    
    def set_last_step(self, step: str):
        """Set the last completed step."""
        self.set('last_step', step)

class ReleaseScript:
    """Main release script class."""
    
    def __init__(self, rc: Optional[int] = None, start_step: Optional[str] = None, skip_steps: List[str] = None, resume: bool = False, load_env: bool = True):
        self.state = ReleaseState()
        self.rc = rc  # Will be set during _load_env if not provided
        self.start_step = start_step
        self.skip_steps = set(skip_steps or [])
        self.resume = resume
        self.start_time = None
        self.step_times = {}
        
        # Load environment variables (only if not just listing steps)
        if load_env:
            self._load_env()
        
        # Define all steps in order
        self.steps = [
            'check_prerequisites',
            'check_java_version',
            'check_maven_version',
            'clean_maven_repo',
            'make_git_folder',
            'clone_juneau',
            'configure_git',
            'run_clean_verify',
            'run_deploy',
            'run_release_prepare',
            'run_release_perform',
            'create_binary_artifacts',
            'verify_distribution',
        ]
    
    def _get_version_from_pom(self, pom_path: Path) -> str:
        """Extract version from pom.xml using current-release.py script."""
        script_dir = Path(__file__).parent
        current_release_script = script_dir / 'current-release.py'
        
        try:
            result = subprocess.run(
                [sys.executable, str(current_release_script)],
                cwd=pom_path.parent,
                capture_output=True,
                text=True,
                check=True
            )
            return result.stdout.strip()
        except Exception as e:
            self.fail(f"Could not determine version using current-release.py: {e}")
    
    def _increment_maintenance_version(self, version: str) -> str:
        """Increment the maintenance version (e.g., 9.0.0 -> 9.0.1)."""
        # Match version pattern: major.minor.maintenance
        match = re.match(r'^(\d+)\.(\d+)\.(\d+)$', version)
        if not match:
            self.fail(f"Invalid version format: {version}. Expected format: X.Y.Z")
        
        major, minor, maintenance = map(int, match.groups())
        next_version = f"{major}.{minor}.{maintenance + 1}-SNAPSHOT"
        return next_version
    
    def _load_history(self, version: str) -> Dict:
        """Load history file for the given version."""
        script_dir = Path(__file__).parent
        history_file = script_dir / f'release-history-{version}.json'
        if history_file.exists():
            try:
                with open(history_file, 'r') as f:
                    return json.load(f)
            except Exception as e:
                print(f"Warning: Could not load history file: {e}")
        return {}
    
    def _save_history(self, version: str, values: Dict):
        """Save history file for the given version."""
        script_dir = Path(__file__).parent
        history_file = script_dir / f'release-history-{version}.json'
        try:
            # Add last-run date
            history_data = values.copy()
            history_data['last_run_date'] = datetime.now().isoformat()
            
            with open(history_file, 'w') as f:
                json.dump(history_data, f, indent=2)
        except Exception as e:
            print(f"Warning: Could not save history file: {e}")
    
    def _prompt_with_default(self, prompt: str, default: Optional[str] = None, required: bool = True) -> str:
        """Prompt user for input with a default value."""
        if default:
            full_prompt = f"{prompt} [{default}]: "
        else:
            full_prompt = f"{prompt}: "
        
        while True:
            response = input(full_prompt).strip()
            if response:
                return response
            elif default:
                return default
            elif not required:
                return ""
            else:
                print("This field is required. Please enter a value.")
    
    def _load_env(self):
        """Initialize environment variables from pom.xml and user prompts."""
        # Get the Juneau root directory
        script_dir = Path(__file__).parent
        juneau_root = script_dir.parent
        pom_path = juneau_root / POM_XML
        
        if not pom_path.exists():
            self.fail(f"pom.xml not found at {pom_path}")
        
        # Get version from pom.xml
        version = self._get_version_from_pom(pom_path)
        next_version = self._increment_maintenance_version(version)
        
        # Load history for this version
        history = self._load_history(version)
        
        # Check if this is the first run for this version (determines X_CLEANM2)
        is_first_run = not history
        
        print('\n' + '=' * 79)
        print('Apache Juneau Release Configuration')
        print('=' * 79)
        print(f"Detected version from pom.xml: {version}")
        print(f"Calculated next version: {next_version}")
        if history.get('last_run_date'):
            last_run = datetime.fromisoformat(history['last_run_date'])
            print(f"Last run: {last_run.strftime('%Y-%m-%d %H:%M:%S')}")
        else:
            print("Last run: Never (first run for this version)")
        print('=' * 79 + '\n')
        
        # Prompt for RC number if not already set
        if self.rc is None:
            # Try to get default from history
            default_rc = None
            if history.get('X_RELEASE_CANDIDATE'):
                rc_match = re.search(RC_PATTERN, history.get('X_RELEASE_CANDIDATE', ''))
                if rc_match:
                    default_rc = rc_match.group(1)
            
            rc_prompt = "Release candidate number"
            if default_rc:
                rc_prompt += f" [{default_rc}]"
            rc_prompt += ": "
            
            while True:
                rc_input = input(rc_prompt).strip()
                if rc_input:
                    try:
                        self.rc = int(rc_input)
                        break
                    except ValueError:
                        print("Please enter a valid number.")
                elif default_rc:
                    self.rc = int(default_rc)
                    break
                else:
                    print("Release candidate number is required. Please enter a value.")
        
        release_candidate = f"RC{self.rc}"
        
        staging = self._prompt_with_default(
            "Staging directory",
            history.get('X_STAGING', STAGING_DIR),
            required=True
        )
        
        username = self._prompt_with_default(
            "Apache username",
            history.get('X_USERNAME', ''),
            required=True
        )
        
        email = self._prompt_with_default(
            "Apache email",
            history.get('X_EMAIL', ''),
            required=True
        )
        
        git_branch = self._prompt_with_default(
            "Git branch",
            history.get('X_GIT_BRANCH', 'master'),
            required=True
        )
        
        java_home = self._prompt_with_default(
            "JAVA_HOME",
            history.get('X_JAVA_HOME', os.environ.get('JAVA_HOME', '')),
            required=True
        )
        
        # Set environment variables
        os.environ['X_VERSION'] = version
        os.environ['X_NEXT_VERSION'] = next_version
        os.environ['X_RELEASE'] = f"juneau-{version}-{release_candidate}"
        os.environ['X_STAGING'] = staging
        
        # Save X_RELEASE to state for later retrieval
        self.state.set('X_RELEASE', os.environ['X_RELEASE'])
        os.environ['X_USERNAME'] = username
        os.environ['X_EMAIL'] = email
        os.environ['X_GIT_BRANCH'] = git_branch
        os.environ['X_JAVA_HOME'] = java_home
        os.environ['X_CLEANM2'] = 'Y' if is_first_run else 'N'
        
        # Set JAVA_HOME and PATH
        if java_home:
            os.environ['JAVA_HOME'] = java_home
            os.environ['PATH'] = f"{java_home}/bin:{os.environ.get('PATH', '')}"
        
        # Save history for next time
        history_values = {
            'X_VERSION': version,
            'X_RELEASE_CANDIDATE': release_candidate,
            'X_STAGING': staging,
            'X_USERNAME': username,
            'X_EMAIL': email,
            'X_GIT_BRANCH': git_branch,
            'X_JAVA_HOME': java_home,
        }
        self._save_history(version, history_values)
        
        # Display settings
        print('\n--- Settings ------------------------------------------------------------------')
        for key in ['X_VERSION', 'X_NEXT_VERSION', 'X_RELEASE', 'X_STAGING', 
                   'X_USERNAME', 'X_EMAIL', 'X_CLEANM2', 'X_GIT_BRANCH', 'X_JAVA_HOME']:
            value = os.environ.get(key, 'NOT SET')
            print(f"{key}: {value}")
        print('--------------------------------------------------------------------------------\n')
    
    def message(self, msg: str):
        """Print a formatted message."""
        timestamp = datetime.now().strftime('%H:%M:%S')
        print('\n' + '-' * 79)
        print(f"[{timestamp}] {msg}")
        print('-' * 79)
    
    def fail(self, msg: str = None):
        """Fail with a message."""
        if msg:
            self.message(f"FAILED: {msg}")
        print('\n' + '=' * 79)
        print('***** FAILED ******************************************************************')
        print('=' * 79 + '\n')
        sys.exit(1)
    
    def success(self):
        """Success message."""
        print('\n' + '=' * 79)
        print('***** SUCCESS *****************************************************************')
        print('=' * 79 + '\n')
        self.state.clear()
        sys.exit(0)
    
    def yprompt(self, prompt: str) -> bool:
        """Yes/no prompt. Returns True if yes, False if no."""
        print()
        response = input(f"{prompt} (Y/n): ").strip()
        if response and response.lower() not in ['y', 'yes']:
            return False
        return True
    
    def run_command(self, cmd: List[str], cwd: Optional[Path] = None, check: bool = True, 
                   capture_output: bool = False) -> subprocess.CompletedProcess:
        """Run a shell command."""
        print(f"Running: {' '.join(cmd)}")
        if cwd:
            print(f"  (in directory: {cwd})")
        
        result = subprocess.run(
            cmd,
            cwd=cwd,
            check=False,
            capture_output=capture_output,
            text=True
        )
        
        if check and result.returncode != 0:
            self.fail(f"Command failed: {' '.join(cmd)}")
        
        return result
    
    def start_timer(self):
        """Start timing a step."""
        self.start_time = time.time()
    
    def end_timer(self) -> float:
        """End timing and return elapsed seconds."""
        if self.start_time:
            elapsed = time.time() - self.start_time
            print(f"Execution time: {elapsed:.1f}s")
            self.start_time = None
            return elapsed
        return 0
    
    def should_run_step(self, step_name: str) -> bool:
        """Determine if a step should run based on start_step and skip_steps."""
        if step_name in self.skip_steps:
            return False
        
        if self.resume:
            last_step = self.state.get_last_step()
            if last_step:
                # Find the index of last_step and step_name
                try:
                    last_idx = self.steps.index(last_step)
                    step_idx = self.steps.index(step_name)
                    # Only run if this step comes after the last completed step
                    return step_idx > last_idx
                except ValueError:
                    pass
        
        if self.start_step:
            try:
                start_idx = self.steps.index(self.start_step)
                step_idx = self.steps.index(step_name)
                # Only run if this step is at or after the start step
                return step_idx >= start_idx
            except ValueError:
                self.fail(f"Unknown start step: {self.start_step}")
        
        return True
    
    def check_prerequisites(self):
        """Check that required tools are available."""
        self.message("Checking prerequisites")
        
        required_tools = {
            'wget': 'wget',
            'gpg': 'gpg',
            'svn': 'svn',
            'git': 'git',
            'java': 'Java',
            'mvn': 'Maven',
        }
        
        missing = []
        for cmd, name in required_tools.items():
            result = subprocess.run(['which', cmd], capture_output=True)
            if result.returncode != 0:
                missing.append(name)
        
        if missing:
            self.fail(f"Missing required tools: {', '.join(missing)}")
        
        # Set GPG_TTY
        tty = subprocess.run(['tty'], capture_output=True, text=True)
        if tty.returncode == 0:
            os.environ['GPG_TTY'] = tty.stdout.strip()
        
        self.state.set_last_step('check_prerequisites')
    
    def check_java_version(self):
        """Check Java version."""
        self.message("Checking Java version")
        
        # Run java -version and capture stderr (version info goes to stderr)
        result = subprocess.run(
            ['java', '-version'],
            capture_output=True,
            text=True,
            check=False
        )
        
        # Print the version output for user visibility
        if result.stderr:
            print(result.stderr)
        
        # Parse version from stderr
        version_text = result.stderr or result.stdout
        java_version = self._parse_java_version(version_text)
        
        if java_version is None:
            self.fail("Could not determine Java version from output")
        
        if java_version < MIN_JAVA_VERSION:
            self.fail(f"Java version {java_version} detected. Java {MIN_JAVA_VERSION} or higher is required.")
        
        print(f"✅ Java version {java_version} detected (Java {MIN_JAVA_VERSION}+ required)")
        self.state.set_last_step('check_java_version')
    
    def _parse_java_version(self, version_text: str) -> Optional[int]:
        """Parse Java version from java -version output."""
        # Try to find version patterns like:
        # - "openjdk version "17.0.1""
        # - "java version "17.0.1""
        # - "openjdk version "1.8.0_292"" (Java 8)
        # - "java version "1.8.0_292"" (Java 8)
        
        # Pattern 1: Modern format (Java 9+): "version "17.0.1""
        match = re.search(r'version\s+"(\d+)\.(\d+)', version_text)
        if match:
            major = int(match.group(1))
            minor = int(match.group(2))
            
            # If major is 1, it's old format (Java 8 and below)
            # The actual version is in the minor number
            if major == 1:
                return minor
            else:
                # Java 9+ uses the major version directly
                return major
        
        # Pattern 2: Try to find just a version number
        match = re.search(r'(\d+)\.(\d+)', version_text)
        if match:
            major = int(match.group(1))
            minor = int(match.group(2))
            if major == 1:
                return minor
            else:
                return major
        
        return None
    
    def check_maven_version(self):
        """Check Maven version."""
        self.message("Checking Maven version")
        
        # Run mvn -version and capture output
        result = subprocess.run(
            ['mvn', '-version'],
            capture_output=True,
            text=True,
            check=False
        )
        
        # Print the version output for user visibility
        if result.stdout:
            print(result.stdout)
        if result.stderr:
            print(result.stderr)
        
        # Get Maven version using maven-version.py script
        maven_version = self._get_maven_version()
        
        if maven_version is None:
            self.fail("Could not determine Maven version")
        
        if maven_version < MIN_MAVEN_VERSION:
            self.fail(f"Maven version {maven_version} detected. Maven {MIN_MAVEN_VERSION} or higher is required.")
        
        print(f"✅ Maven version {maven_version} detected (Maven {MIN_MAVEN_VERSION}+ required)")
        self.state.set_last_step('check_maven_version')
    
    def _get_maven_version(self) -> Optional[int]:
        """Get Maven version using maven-version.py script."""
        script_dir = Path(__file__).parent
        maven_version_script = script_dir / 'maven-version.py'
        
        try:
            result = subprocess.run(
                [sys.executable, str(maven_version_script)],
                capture_output=True,
                text=True,
                check=True
            )
            return int(result.stdout.strip())
        except Exception as e:
            print(f"Warning: Could not determine Maven version using maven-version.py: {e}")
            return None
    
    def clean_maven_repo(self):
        """Clean Maven repository."""
        self.message("Cleaning Maven repository")
        self.start_timer()
        
        clean_m2 = os.environ.get('X_CLEANM2', 'N')
        if clean_m2.upper() != 'N':
            m2_repo = Path.home() / '.m2' / 'repository'
            if m2_repo.exists():
                old_repo = Path.home() / '.m2' / 'repository-old'
                if old_repo.exists():
                    shutil.rmtree(old_repo)
                m2_repo.rename(old_repo)
                # Remove in background
                subprocess.Popen(['rm', '-rf', str(old_repo)])
        
        self.end_timer()
        self.state.set_last_step('clean_maven_repo')
    
    def make_git_folder(self):
        """Create git staging folder."""
        self.message("Making git folder")
        self.start_timer()
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        
        # Clean up entire staging directory to start fresh (avoids issues from previous runs)
        if staging.exists():
            print(f"  Removing existing staging directory: {staging}")
            shutil.rmtree(staging)
        
        staging.mkdir(parents=True, exist_ok=True)
        
        git_dir = staging / 'git'
        git_dir.mkdir(parents=True)
        
        self.end_timer()
        self.state.set_last_step('make_git_folder')
    
    def clone_juneau(self):
        """Clone juneau.git repository."""
        self.message("Cloning juneau.git")
        self.start_timer()
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        git_dir = staging / 'git'
        
        juneau_dir = git_dir / 'juneau'
        # Since we clean up the staging directory in make_git_folder, this should always be a fresh clone
        self.run_command(
            ['git', 'clone', 'https://gitbox.apache.org/repos/asf/juneau.git'],
            cwd=git_dir
        )
        
        self.end_timer()
        self.state.set_last_step('clone_juneau')
    
    def configure_git(self):
        """Configure git user name and email."""
        self.message("Configuring git")
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        username = os.environ.get('X_USERNAME')
        email = os.environ.get('X_EMAIL')
        
        if username:
            self.run_command(['git', 'config', 'user.name', username], cwd=juneau_dir)
        if email:
            self.run_command(['git', 'config', 'user.email', email], cwd=juneau_dir)
        
        self.state.set_last_step('configure_git')
    
    def run_clean_verify(self):
        """Run Maven clean verify."""
        self.message("Running clean verify")
        self.start_timer()
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        self.run_command(['mvn', 'clean', 'verify'], cwd=juneau_dir)
        
        self.end_timer()
        self.state.set_last_step('run_clean_verify')
    
    def create_test_workspace(self):
        """Create test workspace."""
        self.message("Creating test workspace")
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        workspace = juneau_dir / 'target' / 'workspace'
        
        version = os.environ.get('X_VERSION', '')
        version_snapshot = f"{version}-SNAPSHOT"
        
        if workspace.exists():
            shutil.rmtree(workspace)
        workspace.mkdir(parents=True)
        
        zip_files = [
            ('juneau-microservice/juneau-my-jetty-microservice/target/my-jetty-microservice-{}-bin.zip',
             'my-jetty-microservice'),
            ('juneau-microservice/juneau-my-springboot-microservice/target/my-springboot-microservice-{}-bin.zip',
             'my-springboot-microservice'),
            ('juneau-examples/juneau-examples-core/target/juneau-examples-core-{}-bin.zip',
             'juneau-examples-core'),
            ('juneau-examples/juneau-examples-rest-jetty/target/juneau-examples-rest-jetty-{}-bin.zip',
             'juneau-examples-rest-jetty'),
            ('juneau-examples/juneau-examples-rest-springboot/target/juneau-examples-rest-springboot-{}-bin.zip',
             'juneau-examples-rest-springboot'),
        ]
        
        for zip_src_pattern, zip_tgt_name in zip_files:
            zip_src = juneau_dir / zip_src_pattern.format(version_snapshot)
            zip_tgt = workspace / zip_tgt_name
            
            if not zip_src.exists():
                print(f"Warning: {zip_src} not found, skipping")
                continue
            
            print(f"Unzipping {zip_src} to {zip_tgt}")
            self.run_command(['unzip', '-o', str(zip_src), '-d', str(zip_tgt)], check=False)
        
        workspace_path = staging / 'git' / 'juneau' / 'target' / 'workspace'
        if not self.yprompt(f"Can all workspace projects in {workspace_path} be cleanly imported as Maven projects into Eclipse?"):
            self.fail("Workspace verification failed")
        
        self.state.set_last_step('create_test_workspace')
    
    def run_deploy(self):
        """Run Maven deploy."""
        self.message("Running deploy")
        self.start_timer()
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        self.run_command(['mvn', 'deploy', '-Daether.checksums.algorithms=MD5,SHA-1,SHA-512'], cwd=juneau_dir)
        
        self.end_timer()
        self.state.set_last_step('run_deploy')
    
    def run_release_prepare(self):
        """Run Maven release:prepare."""
        self.message("Running release:prepare")
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        version = os.environ.get('X_VERSION')
        release = os.environ.get('X_RELEASE')
        next_version = os.environ.get('X_NEXT_VERSION')
        
        # Check that the current POM version is a SNAPSHOT (required by release:prepare)
        pom_path = juneau_dir / POM_XML
        if pom_path.exists():
            # Read version directly from POM (don't use current-release.py as it strips SNAPSHOT)
            try:
                import xml.etree.ElementTree as ET
                tree = ET.parse(pom_path)
                root = tree.getroot()
                # Handle namespace
                ns_uri = root.tag.split('}')[0].strip('{') if '}' in root.tag else ''
                if ns_uri:
                    ns = {'maven': ns_uri}
                    version_elem = root.find('maven:version', ns)
                else:
                    version_elem = root.find('version')
                
                if version_elem is not None and version_elem.text:
                    current_version = version_elem.text.strip()
                else:
                    self.fail("Could not find version element in pom.xml")
            except Exception as e:
                self.fail(f"Could not parse version from pom.xml: {e}")
            
            if not current_version.endswith('-SNAPSHOT'):
                self.fail(
                    f"Current version in POM is '{current_version}', but release:prepare requires a SNAPSHOT version.\n"
                    f"This usually means a previous release attempt already changed the version.\n"
                    f"Please ensure the git repository is in the correct state (e.g., checkout the master branch)."
                )
            print(f"✓ Current POM version is SNAPSHOT: {current_version}")
        
        # run_command will automatically fail if the command doesn't succeed (check=True by default)
        self.run_command([
            'mvn', 'release:prepare',
            '-DautoVersionSubmodules=true',
            f'-DreleaseVersion={version}',
            f'-Dtag={release}',
            f'-DdevelopmentVersion={next_version}'
        ], cwd=juneau_dir)
        
        # If we get here, the command succeeded
        print("✅ release:prepare completed successfully")
        self.state.set_last_step('run_release_prepare')
    
    def run_git_diff(self):
        """Run git diff."""
        self.message("Running git diff")
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        release = os.environ.get('X_RELEASE')
        
        self.run_command(['git', 'diff', release], cwd=juneau_dir, check=False)
        
        self.state.set_last_step('run_git_diff')
    
    def run_release_perform(self):
        """Run Maven release:perform."""
        self.message("Running release:perform")
        self.start_timer()
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        self.run_command(['mvn', 'release:perform', '-Daether.checksums.algorithms=MD5,SHA-1,SHA-512'], cwd=juneau_dir)
        
        # Open Nexus staging repositories page
        subprocess.Popen(['open', 'https://repository.apache.org/#stagingRepositories'])
        
        print("\nOn Apache's Nexus instance, locate the staging repository for the code you just released.")
        print("It should be called something like orgapachejuneau-1000.")
        print("Check the Updated time stamp and click to verify its Content.")
        print("IMPORTANT - When all artifacts to be deployed are in the staging repository, tick the box next to it and click Close.")
        print("DO NOT CLICK RELEASE YET - the release candidate must pass [VOTE] emails on dev@juneau before we release.")
        print("Once closing has finished (check with Refresh), browse to the URL of the staging repository which should be something like https://repository.apache.org/content/repositories/orgapachejuneau-1000.")
        print()
        
        repo_input = input("Enter the staging repository name AFTER CLOSING IT!!!: orgapachejuneau-").strip()
        repo_name = f"orgapachejuneau-{repo_input}"
        
        if not self.yprompt(f"X_REPO = {repo_name}.  Is this correct?"):
            self.fail("Repository name confirmation failed")
        
        os.environ['X_REPO'] = repo_name
        self.state.set('X_REPO', repo_name)
        
        self.end_timer()
        self.state.set_last_step('run_release_perform')
    
    def create_binary_artifacts(self):
        """Create binary artifacts and upload to SVN."""
        self.message("Creating binary artifacts")
        self.start_timer()
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        version = os.environ.get('X_VERSION')
        release = os.environ.get('X_RELEASE')
        repo = self.state.get('X_REPO') or os.environ.get('X_REPO')
        
        if not repo:
            self.fail("X_REPO not set. Did you complete the release:perform step?")
        
        # Checkout SVN dist
        dist_dir = staging / 'dist'
        if dist_dir.exists():
            shutil.rmtree(dist_dir)
        
        self.run_command(['svn', 'checkout', SVN_DIST_URL, 'dist'], cwd=staging)
        
        # Remove old files
        source_dir = dist_dir / 'source'
        binaries_dir = dist_dir / 'binaries'
        if source_dir.exists():
            self.run_command(['svn', 'rm', 'source/*'], cwd=dist_dir, check=False)
        if binaries_dir.exists():
            self.run_command(['svn', 'rm', 'binaries/*'], cwd=dist_dir, check=False)
        
        # Create release directories
        release_source_dir = source_dir / release
        release_binaries_dir = binaries_dir / release
        release_source_dir.mkdir(parents=True)
        release_binaries_dir.mkdir(parents=True)
        
        # Download source artifacts
        repo_url = f"https://repository.apache.org/content/repositories/{repo}/org/apache/juneau/"
        self.run_command([
            'wget', '-e', 'robots=off', '--recursive', '--no-parent', '--no-directories',
            '-A', '*-source-release*', repo_url
        ], cwd=release_source_dir)
        
        # Rename source zip file
        source_zip = release_source_dir / f"juneau-{version}-source-release.zip"
        if source_zip.exists():
            target_zip = release_source_dir / f"apache-juneau-{version}-src.zip"
            
            # Simply rename the zip file (docs is already excluded by maven-source-plugin)
            print(f"Renaming source zip: {source_zip.name} -> {target_zip.name}")
            source_zip.rename(target_zip)
            print(f"✓ Source zip renamed to {target_zip.name}")
            
            # Process .asc file
            asc_file = release_source_dir / f"juneau-{version}-source-release.zip.asc"
            if asc_file.exists():
                asc_file.rename(release_source_dir / f"apache-juneau-{version}-src.zip.asc")
            
            # Process .sha512 file (copy as-is, same as .asc)
            sha512_file = release_source_dir / f"juneau-{version}-source-release.zip.sha512"
            if sha512_file.exists():
                sha512_file.rename(release_source_dir / f"apache-juneau-{version}-src.zip.sha512")
            
            # Remove old hash files
            for old_hash in release_source_dir.glob('*.sha1'):
                old_hash.unlink()
            for old_hash in release_source_dir.glob('*.md5'):
                old_hash.unlink()
        
        # Download binary artifacts
        self.run_command([
            'wget', '-e', 'robots=off', '--recursive', '--no-parent', '--no-directories',
            '-A', 'juneau-distrib*-bin.zip*', repo_url
        ], cwd=release_binaries_dir)
        
        # Rename and process binary files
        bin_zip = release_binaries_dir / f"juneau-distrib-{version}-bin.zip"
        if bin_zip.exists():
            target_bin = release_binaries_dir / f"apache-juneau-{version}-bin.zip"
            bin_zip.rename(target_bin)
            
            # Process .asc file
            bin_asc = release_binaries_dir / f"juneau-distrib-{version}-bin.zip.asc"
            if bin_asc.exists():
                bin_asc.rename(release_binaries_dir / f"apache-juneau-{version}-bin.zip.asc")
            
            # Process .sha512 file (copy as-is, same as .asc)
            bin_sha512 = release_binaries_dir / f"juneau-distrib-{version}-bin.zip.sha512"
            if bin_sha512.exists():
                bin_sha512.rename(release_binaries_dir / f"apache-juneau-{version}-bin.zip.sha512")
            
            # Remove old hash files
            for old_hash in release_binaries_dir.glob('*.sha1'):
                old_hash.unlink()
            for old_hash in release_binaries_dir.glob('*.md5'):
                old_hash.unlink()
        
        # Add and commit to SVN
        self.run_command(['svn', 'add', f'source/{release}'], cwd=dist_dir)
        self.run_command(['svn', 'add', f'binaries/{release}'], cwd=dist_dir)
        self.run_command(['svn', 'commit', '-m', release], cwd=dist_dir)
        
        self.end_timer()
        self.state.set_last_step('create_binary_artifacts')
    
    def verify_distribution(self):
        """Verify distribution files are available."""
        self.message("Verifying distribution")
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        version = os.environ.get('X_VERSION')
        release = os.environ.get('X_RELEASE')
        
        # Checkout or update SVN dist to verify files are available
        dist_dir = staging / 'dist'
        if dist_dir.exists():
            # Update existing checkout
            self.run_command(['svn', 'update'], cwd=dist_dir, check=False)
        else:
            # Fresh checkout
            self.run_command(['svn', 'checkout', SVN_DIST_URL, 'dist'], cwd=staging)
        
        # Expected files
        source_dir = dist_dir / 'source' / release
        binaries_dir = dist_dir / 'binaries' / release
        
        expected_files = [
            # Source files
            source_dir / f"apache-juneau-{version}-src.zip",
            source_dir / f"apache-juneau-{version}-src.zip.asc",
            source_dir / f"apache-juneau-{version}-src.zip.sha512",
            # Binary files
            binaries_dir / f"apache-juneau-{version}-bin.zip",
            binaries_dir / f"apache-juneau-{version}-bin.zip.asc",
            binaries_dir / f"apache-juneau-{version}-bin.zip.sha512",
        ]
        
        # Verify all files exist and are not empty
        missing_files = []
        empty_files = []
        
        for file_path in expected_files:
            if not file_path.exists():
                missing_files.append(str(file_path.relative_to(dist_dir)))
            elif file_path.stat().st_size == 0:
                empty_files.append(str(file_path.relative_to(dist_dir)))
        
        if missing_files:
            self.fail("Missing distribution files:\n  " + "\n  ".join(missing_files))
        
        if empty_files:
            self.fail("Empty distribution files:\n  " + "\n  ".join(empty_files))
        
        # All files verified
        print("\n✅ All distribution files verified:")
        for file_path in expected_files:
            size = file_path.stat().st_size
            size_mb = size / (1024 * 1024)
            print(f"  ✓ {file_path.relative_to(dist_dir)} ({size_mb:.2f} MB)")
        
        # Open browser for manual inspection
        subprocess.Popen(['open', SVN_DIST_URL])
        
        print("\n✅ Distribution verification successful. Voting can be started.")
        
        # Generate vote email
        self._generate_vote_email(version, release, dist_dir)
        
        self.state.set_last_step('verify_distribution')
    
    def _calculate_vote_end_date(self) -> str:
        """Calculate vote end date (72 hours from now, minimum 3 days)."""
        # Add 72 hours (3 days) to current time
        end_date = datetime.now() + timedelta(hours=72)
        # Format: "04-May-2016 1:30pm"
        formatted = end_date.strftime('%d-%b-%Y %I:%M%p').lstrip('0')
        # Convert to lowercase and fix spacing
        formatted = formatted.replace(' 0', ' ').lower()
        return formatted
    
    def _read_sha512_from_url(self, url: str) -> Optional[str]:
        """Read SHA-512 checksum from Apache distribution URL and return formatted checksum."""
        try:
            with urllib.request.urlopen(url) as response:
                content = response.read().decode('utf-8').strip()
                # Parse the formatted checksum from the file
                # Format: "/path/to/file:\nCHECKSUM_LINE_1\nCHECKSUM_LINE_2"
                lines = [line.strip() for line in content.split('\n') if line.strip()]
                if len(lines) >= 2:
                    # First line is the path, rest are the checksum
                    # Join checksum lines with newline to preserve formatting
                    checksum = '\n'.join(lines[1:])
                    return checksum
                elif len(lines) == 1:
                    # Single line format, extract just the checksum part
                    parts = lines[0].split(':')
                    if len(parts) > 1:
                        # Format: "path: checksum"
                        return parts[1].strip()
                    return lines[0]
        except Exception as e:
            print(f"Warning: Could not read SHA-512 from {url}: {e}")
        return None
    
    def _get_git_commit_hash(self, release_tag: str) -> Optional[str]:
        """Get git commit hash for a release tag."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        try:
            result = subprocess.run(
                ['git', 'rev-parse', release_tag],
                cwd=juneau_dir,
                capture_output=True,
                text=True,
                check=True
            )
            return result.stdout.strip()
        except Exception as e:
            print(f"Warning: Could not get git commit hash for tag {release_tag}: {e}")
        return None
    
    def _generate_vote_email(self, version: str, release: str, dist_dir: Path):
        """Generate vote email body for the release."""
        # Read SHA-512 checksums from Apache distribution URLs
        src_sha512_url = f"https://dist.apache.org/repos/dist/dev/juneau/source/{release}/apache-juneau-{version}-src.zip.sha512"
        bin_sha512_url = f"https://dist.apache.org/repos/dist/dev/juneau/binaries/{release}/apache-juneau-{version}-bin.zip.sha512"
        
        src_sha512 = self._read_sha512_from_url(src_sha512_url) or "SHA512_NOT_FOUND"
        bin_sha512 = self._read_sha512_from_url(bin_sha512_url) or "SHA512_NOT_FOUND"
        
        # Get git commit hash
        git_commit = self._get_git_commit_hash(release) or "COMMIT_HASH_NOT_FOUND"
        
        # Get staging repository
        repo = self.state.get('X_REPO') or os.environ.get('X_REPO') or "REPO_NOT_FOUND"
        
        # Calculate vote end date
        vote_end_date = self._calculate_vote_end_date()
        
        # Extract RC number from release (e.g., "juneau-9.2.0-RC1" -> "RC1")
            rc_match = re.search(RC_PATTERN, release)
        rc_number = rc_match.group(1) if rc_match else "x"
        
        # Generate email body
        email_body = f"""To: dev@juneau.apache.org

[VOTE] Release Apache Juneau {version} RC{rc_number}

I am pleased to be calling this vote for the source release of Apache Juneau {version} RC{rc_number}.

The binaries are available at:

https://dist.apache.org/repos/dist/dev/juneau/binaries/{release}/

The release candidate to be voted over is available at:
https://dist.apache.org/repos/dist/dev/juneau/source/{release}/

SHA-512 checksums:

apache-juneau-{version}-src.zip: 
{src_sha512}

apache-juneau-{version}-bin.zip: 
{bin_sha512}

Build the release candidate using:

mvn clean install


The release candidate is signed with a GPG key available at:
https://dist.apache.org/repos/dist/release/juneau/KEYS

A staged Maven repository is available for review at:
https://repository.apache.org/content/repositories/{repo}/

The Git commit for this release is...
https://gitbox.apache.org/repos/asf?p=juneau.git;a=commit;h={git_commit}

Please vote on releasing this package as:
Apache Juneau {version}

This vote will be open until {vote_end_date} and passes if a majority of at least three +1 Apache Juneau PMC votes are cast.
(needs to be at least 72 weekday hours)

[ ] +1 Release this package
[ ] 0 I don't feel strongly about it, but don't object
[ ] -1 Do not release this package because...

Anyone can participate in testing and voting, not just committers, please feel free to try out the release candidate and provide your votes.
"""
        
        # Display email to console
        print("\n" + "=" * 79)
        print("VOTE EMAIL BODY:")
        print("=" * 79)
        print(email_body)
        print("=" * 79)
    
    def list_steps(self):
        """List all available steps."""
        print("\nAvailable steps:")
        for i, step in enumerate(self.steps, 1):
            print(f"  {i:2d}. {step}")
        print()
    
    def revert_release(self):
        """Revert a release by deleting the tag, reverting Maven versions, and cleaning up SVN files."""
        self.message("Reverting release")
        self.start_timer()
        
        # Get version and release from state or environment
        version = self.state.get('X_VERSION') or os.environ.get('X_VERSION')
        release = self.state.get('X_RELEASE') or os.environ.get('X_RELEASE')
        
        if not version or not release:
            self.fail("X_VERSION and X_RELEASE must be set. Cannot determine what to revert.")
        
        print(f"Version: {version}")
        print(f"Release: {release}")
        
        # Confirm with user
        if not self.yprompt(f"Are you sure you want to revert release {release}? This will delete the git tag and clean up SVN files."):
            print("Revert cancelled.")
            return
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        git_dir = staging / 'git' / 'juneau'
        
        # Pull latest changes from git
        print("\nPulling latest changes from git...")
        if git_dir.exists() and (git_dir / '.git').exists():
            try:
                self.run_command(['git', 'pull'], cwd=git_dir)
                print("  ✓ Git pull completed")
            except Exception as e:
                print(f"  Warning: Could not pull from git: {e}")
        else:
            print(f"  Warning: Git directory not found at {git_dir}, skipping git pull")
        
        # Step 1: Delete the git tag
        print("\nStep 1: Deleting git tag...")
        tag_name = release  # e.g., "juneau-9.2.0-RC2"
        if git_dir.exists() and (git_dir / '.git').exists():
            try:
                # Check if tag exists locally
                result = subprocess.run(
                    ['git', 'tag', '-l', tag_name],
                    cwd=git_dir,
                    capture_output=True,
                    text=True,
                    check=False
                )
                if result.stdout.strip():
                    print(f"  Found local tag: {tag_name}")
                    self.run_command(['git', 'tag', '-d', tag_name], cwd=git_dir, check=False)
                
                # Delete remote tag
                print(f"  Deleting remote tag: {tag_name}")
                self.run_command(['git', 'push', 'origin', f':{tag_name}'], cwd=git_dir, check=False)
                print("  ✓ Git tag deleted")
            except Exception as e:
                print(f"  Warning: Could not delete git tag: {e}")
        else:
            print(f"  Warning: Git directory not found at {git_dir}, skipping tag deletion")
        
        # Step 2: Revert Maven versions
        print("\nStep 2: Reverting Maven versions...")
        development_version = f"{version}-SNAPSHOT"
        if git_dir.exists() and (git_dir / POM_XML).exists():
            try:
                print(f"  Reverting to development version: {development_version}")
                self.run_command([
                    'mvn', 'release:update-versions',
                    '-DautoVersionSubmodules=true',
                    f'-DdevelopmentVersion={development_version}'
                ], cwd=git_dir)
                print("  ✓ Maven versions reverted")
            except Exception as e:
                print(f"  Warning: Could not revert Maven versions: {e}")
        else:
            print(f"  Warning: Git directory or pom.xml not found at {git_dir}, skipping Maven version revert")
        
        # Step 3: Clean up SVN files
        print("\nStep 3: Cleaning up SVN files...")
        dist_dir = staging / 'dist'
        if dist_dir.exists() and (dist_dir / '.svn').exists():
            try:
                # Update SVN first
                self.run_command(['svn', 'update'], cwd=dist_dir, check=False)
                
                # Find and remove RC directories
                binaries_dir = dist_dir / 'binaries'
                source_dir = dist_dir / 'source'
                
                removed_any = False
                
                # Remove from binaries
                if binaries_dir.exists():
                    for item in binaries_dir.iterdir():
                        if item.is_dir() and 'RC' in item.name:
                            print(f"  Removing: binaries/{item.name}")
                            self.run_command(['svn', 'rm', str(item.relative_to(dist_dir))], cwd=dist_dir, check=False)
                            removed_any = True
                
                # Remove from source
                if source_dir.exists():
                    for item in source_dir.iterdir():
                        if item.is_dir() and 'RC' in item.name:
                            print(f"  Removing: source/{item.name}")
                            self.run_command(['svn', 'rm', str(item.relative_to(dist_dir))], cwd=dist_dir, check=False)
                            removed_any = True
                
                if removed_any:
                    # Check status
                    result = subprocess.run(
                        ['svn', 'status'],
                        cwd=dist_dir,
                        capture_output=True,
                        text=True,
                        check=False
                    )
                    if result.stdout and 'D' in result.stdout:
                        print("\n  SVN changes ready to commit:")
                        print(result.stdout)
                        if self.yprompt("Commit SVN deletions?"):
                            self.run_command(['svn', 'commit', '-m', f'Remove {release} release candidate'], cwd=dist_dir)
                            print("  ✓ SVN files cleaned up and committed")
                        else:
                            print("  SVN deletions staged but not committed")
                    else:
                        print("  No SVN changes to commit")
                else:
                    print("  No RC directories found in SVN")
            except Exception as e:
                print(f"  Warning: Could not clean up SVN files: {e}")
        else:
            print(f"  Warning: SVN directory not found at {dist_dir}, skipping SVN cleanup")
        
        self.end_timer()
        print("\n" + "=" * 79)
        print("✅ Release revert complete!")
        print("=" * 79)
        print("\nNote: You may need to manually:")
        print("  - Reset your local git repository if needed")
        print("  - Verify Maven versions were reverted correctly")
        print("  - Check SVN repository for any remaining files")
    
    def run(self):
        """Run the release script."""
        # Prompt for PGP passphrase early (before any time-consuming operations)
        script_dir = Path(__file__).parent
        prompt_script = script_dir / 'prompt-pgp-passphrase.py'
        if prompt_script.exists():
            print("\n" + "=" * 79)
            print("Prompting for PGP passphrase...")
            print("=" * 79)
            try:
                subprocess.run(
                    [sys.executable, str(prompt_script)],
                    check=False  # Don't fail if this doesn't work
                )
            except Exception as e:
                print(f"⚠ Could not run PGP passphrase prompt: {e}")
        
        if self.resume:
            last_step = self.state.get_last_step()
            if last_step:
                print(f"Resuming from last checkpoint: {last_step}")
                # Find the step after last_step
                try:
                    last_idx = self.steps.index(last_step)
                    if last_idx < len(self.steps) - 1:
                        self.start_step = self.steps[last_idx + 1]
                        print(f"Starting from step: {self.start_step}")
                except ValueError:
                    pass
        
        for step_name in self.steps:
            if not self.should_run_step(step_name):
                print(f"Skipping step: {step_name}")
                continue
            
            step_method = getattr(self, step_name)
            try:
                step_method()
            except KeyboardInterrupt:
                print("\n\nScript interrupted by user")
                print(f"Last completed step: {step_name}")
                print(f"To resume, run: python3 scripts/release.py --start-step {step_name}")
                sys.exit(1)
            except Exception as e:
                print(f"\nError in step {step_name}: {e}")
                print(f"To resume, run: python3 scripts/release.py --start-step {step_name}")
                raise
        
        self.success()

def main():
    parser = argparse.ArgumentParser(
        description='Apache Juneau Release Script',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument(
        '--start-step',
        help='Start execution from the specified step (skips all previous steps)'
    )
    parser.add_argument(
        '--list-steps',
        action='store_true',
        help='List all available steps and exit'
    )
    parser.add_argument(
        '--skip-step',
        action='append',
        default=[],
        help='Skip a specific step (can be used multiple times)'
    )
    parser.add_argument(
        '--resume',
        action='store_true',
        help='Resume from the last checkpoint (if available)'
    )
    parser.add_argument(
        '--revert',
        action='store_true',
        help='Revert a release by deleting the git tag, reverting Maven versions, and cleaning up SVN files'
    )
    
    args = parser.parse_args()
    
    # If just listing steps, don't require RC
    if args.list_steps:
        # Create a dummy script just to call list_steps (don't load env)
        script = ReleaseScript(rc=None, start_step=None, skip_steps=[], resume=False, load_env=False)
        script.list_steps()
        return
    
    # If reverting, handle it separately
    if args.revert:
        # Try to determine RC and load state
        rc = None
        state_data = {}
        
        # Try to extract from X_RELEASE environment variable
        x_release = os.environ.get('X_RELEASE')
        if x_release:
            rc_match = re.search(RC_PATTERN, x_release)
            if rc_match:
                rc = int(rc_match.group(1))
                print(f"📌 Detected RC number from X_RELEASE: {rc}")
        
        # Try to extract from state file
        state_file = STATE_FILE
        if state_file.exists():
            try:
                with open(state_file, 'r') as f:
                    state_data = json.load(f)
                x_release = state_data.get('X_RELEASE')
                if x_release and rc is None:
                    rc_match = re.search(RC_PATTERN, x_release)
                    if rc_match:
                        rc = int(rc_match.group(1))
                        print(f"📌 Detected RC number from state file: {rc}")
                
                # Set environment variables from state to avoid prompts
                if state_data.get('X_VERSION'):
                    os.environ['X_VERSION'] = state_data['X_VERSION']
                if state_data.get('X_RELEASE'):
                    os.environ['X_RELEASE'] = state_data['X_RELEASE']
                if state_data.get('X_STAGING'):
                    os.environ['X_STAGING'] = state_data['X_STAGING']
            except Exception as e:
                print(f"Warning: Could not load state file: {e}")
        
        # Get version - from state, environment, or pom.xml
        version = state_data.get('X_VERSION') or os.environ.get('X_VERSION')
        release = None
        
        if not version:
            # Try to get version from pom.xml
            script_dir = Path(__file__).parent
            juneau_root = script_dir.parent
            pom_path = juneau_root / POM_XML
            if pom_path.exists():
                try:
                    result = subprocess.run(
                        ["mvn", "help:evaluate", "-Dexpression=project.version", "-q", "-DforceStdout"],
                        cwd=pom_path.parent,
                        capture_output=True,
                        text=True,
                        check=True
                    )
                    version = result.stdout.strip()
                    if version.endswith('-SNAPSHOT'):
                        version = version[:-9]
                    print(f"📌 Detected version from pom.xml: {version}")
                except Exception:
                    pass
        
        # If we have version but no RC, try to get RC from history file
        if version and rc is None:
            script_dir = Path(__file__).parent
            history_file = script_dir / f'release-history-{version}.json'
            if history_file.exists():
                try:
                    with open(history_file, 'r') as f:
                        history = json.load(f)
                    release_candidate = history.get('X_RELEASE_CANDIDATE', '')
                    if release_candidate:
                        rc_match = re.search(RC_PATTERN, release_candidate)
                        if rc_match:
                            rc = int(rc_match.group(1))
                            print(f"📌 Detected RC number from history file: {rc}")
                except Exception as e:
                    print(f"Warning: Could not load history file: {e}")
        
        # If we still don't have RC, prompt for it
        if version and rc is None:
            while True:
                rc_input = input("Release candidate number: ").strip()
                if rc_input:
                    try:
                        rc = int(rc_input)
                        break
                    except ValueError:
                        print("Please enter a valid number.")
                else:
                    print("Release candidate number is required.")
        
        # Construct X_RELEASE if we have version and RC
        if version and rc:
            release = f"juneau-{version}-RC{rc}"
            os.environ['X_RELEASE'] = release
            print(f"📌 Constructed release: {release}")
        
        # Set version in environment if we have it
        if version:
            os.environ['X_VERSION'] = version
        
        # Create script without loading env (we'll set what we need manually)
        script = ReleaseScript(rc=rc, start_step=None, skip_steps=[], resume=False, load_env=False)
        
        # Ensure state and environment have the necessary info
        if version:
            script.state.set('X_VERSION', version)
            os.environ['X_VERSION'] = version
        if release:
            script.state.set('X_RELEASE', release)
            os.environ['X_RELEASE'] = release
        elif state_data.get('X_RELEASE'):
            # Use release from state if we couldn't construct it
            release = state_data.get('X_RELEASE')
            script.state.set('X_RELEASE', release)
            os.environ['X_RELEASE'] = release
        if state_data.get('X_STAGING'):
            script.state.set('X_STAGING', state_data['X_STAGING'])
            os.environ['X_STAGING'] = state_data['X_STAGING']
        elif not os.environ.get('X_STAGING'):
            # Set default staging if not set
            os.environ['X_STAGING'] = STAGING_DIR
        
        script.revert_release()
        return
    
    # Try to determine RC from context if resuming (will prompt if not found)
    rc = None
    if args.resume or args.start_step:
        # Try to extract from X_RELEASE environment variable
        x_release = os.environ.get('X_RELEASE')
        if x_release:
            rc_match = re.search(RC_PATTERN, x_release)
            if rc_match:
                rc = int(rc_match.group(1))
                print(f"📌 Detected RC number from X_RELEASE: {rc}")
        
        # Try to extract from state file
        if rc is None:
            state_file = STATE_FILE
            if state_file.exists():
                try:
                    with open(state_file, 'r') as f:
                        state = json.load(f)
                    x_release = state.get('X_RELEASE')
                    if x_release:
                        rc_match = re.search(RC_PATTERN, x_release)
                        if rc_match:
                            rc = int(rc_match.group(1))
                            print(f"📌 Detected RC number from state file: {rc}")
                except Exception:
                    pass
        
        # Try to extract from history files (get latest version's RC)
        if rc is None:
            script_dir = Path(__file__).parent
            juneau_root = script_dir.parent
            pom_path = juneau_root / POM_XML
            if pom_path.exists():
                try:
                    # Get version from pom
                    result = subprocess.run(
                        ["mvn", "help:evaluate", "-Dexpression=project.version", "-q", "-DforceStdout"],
                        cwd=pom_path.parent,
                        capture_output=True,
                        text=True,
                        check=True
                    )
                    version = result.stdout.strip()
                    if version.endswith('-SNAPSHOT'):
                        version = version[:-9]
                    
                    # Load history for this version
                    history_file = script_dir / f'release-history-{version}.json'
                    if history_file.exists():
                        with open(history_file, 'r') as f:
                            history = json.load(f)
                        release_candidate = history.get('X_RELEASE_CANDIDATE', '')
                        rc_match = re.search(RC_PATTERN, release_candidate)
                        if rc_match:
                            rc = int(rc_match.group(1))
                            print(f"📌 Detected RC number from history file: {rc}")
                except Exception:
                    pass
    
    script = ReleaseScript(
        rc=rc,
        start_step=args.start_step,
        skip_steps=args.skip_step,
        resume=args.resume
    )
    
    script.run()

if __name__ == '__main__':
    main()

