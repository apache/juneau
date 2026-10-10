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
- Maven deploy and release
- Review of the release:prepare diff
- Binary artifact creation
- SVN distribution upload

The script supports resuming from any step, allowing you to restart at arbitrary points
in the release process.

Usage:
    python3 scripts/release.py [--start-step STEP_NAME] [--list-steps] [--skip-step STEP_NAME] [--resume]
                               [--revert] [--detail LEVEL]

Options:
    --start-step STEP_NAME    Start execution from the specified step (skips all previous steps)
    --list-steps              List all available steps and exit
    --skip-step STEP_NAME     Skip a specific step (can be used multiple times)
    --resume                  Resume from the last checkpoint (if available)
    --revert                  Delete the git tag, revert the Maven versions and remove the RC from SVN
    --detail LEVEL            Console detail: summary, actionable (default), modules or all (JUNEAU_RUN_DETAIL)
"""

import argparse
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request
import xml.etree.ElementTree as ET
from contextlib import contextmanager
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

# Full log of every release run: ~/.juneau-release-logs/<release>-<YYYYMMDD-HHMMSS>.log
RELEASE_LOG_DIR = Path.home() / '.juneau-release-logs'

# Maven and `svn commit` run under a PTY so gpg or svn can prompt.  After this many silent seconds, a partial
# line is shown as a prompt.
WATCHDOG_SECONDS = 20

# Row label (at most 10 characters, so rows line up with push.py's and test.py's) and title of each release step.
STEP_TITLES = {
    'check_prerequisites': ('Tools', 'Checking prerequisites'),
    'check_java_version': ('Java', 'Checking Java version'),
    'check_maven_version': ('Maven', 'Checking Maven version'),
    'clean_maven_repo': ('Clean .m2', 'Cleaning Maven repository'),
    'make_git_folder': ('Staging', 'Making git folder'),
    'clone_juneau': ('Clone', 'Cloning juneau.git'),
    'configure_git': ('Git config', 'Configuring git'),
    'run_clean_verify': ('Verify', 'Running clean verify'),
    'run_deploy': ('Deploy', 'Running deploy'),
    'run_release_prepare': ('Prepare', 'Running release:prepare'),
    'run_git_diff': ('Diff', 'Reviewing the release:prepare diff'),
    'run_release_perform': ('Perform', 'Running release:perform'),
    'create_binary_artifacts': ('Artifacts', 'Creating binary artifacts'),
    'verify_distribution': ('Dist check', 'Verifying distribution'),
}

RUN_MODULE_PATH = Path(__file__).resolve().parent.parent / 'juneau-run' / 'src' / 'main' / 'python' / 'juneau_run.py'


def run_module():
    """The in-tree juneau_run module (shared if already loaded), or None if its source is not there."""
    module = sys.modules.get('juneau_run')
    if module is None and RUN_MODULE_PATH.exists():
        spec = importlib.util.spec_from_file_location('juneau_run', RUN_MODULE_PATH)
        module = importlib.util.module_from_spec(spec)
        sys.modules['juneau_run'] = module
        # Keep the module's source directory free of __pycache__ (the Maven build's RAT check scans it).
        previous, sys.dont_write_bytecode = sys.dont_write_bytecode, True
        try:
            spec.loader.exec_module(module)
        finally:
            sys.dont_write_bytecode = previous
    return module


def say(text='', level='info'):
    """Status text: printed as before, or a filtered note when a console view owns the screen."""
    run = run_module()
    if run is None:
        print(text)
    else:
        run.say(text, level=level)


def tool_options(cmd):
    """
    (command, parser, tty) for run_command.  Maven gets -B and the maven parser.  Maven, `svn commit` and
    `git push` get a PTY, because gpg signing or svn/gitbox credentials may prompt.
    """
    if cmd[:1] == ['mvn']:
        return ['mvn', '-B', *cmd[1:]], 'maven', True
    if cmd[:2] in (['svn', 'commit'], ['git', 'push']):
        return list(cmd), 'generic', True
    return list(cmd), 'generic', False


def command_title(cmd, width=60):
    """A command as a row title, cut to width."""
    text = ' '.join(str(part) for part in cmd)
    return text if len(text) <= width else text[:width - 1] + '…'


def ask_rc(run, prompt, default=None) -> int:
    """Ask for a release candidate number until you give one.  Enter takes the default when there is one."""
    while True:
        answer = run.ask(prompt).strip()
        if not answer and default is not None:
            return int(default)
        if not answer:
            run.say("Release candidate number is required. Please enter a value.", level='warn')
            continue
        try:
            return int(answer)
        except ValueError:
            run.say("Please enter a valid number.", level='warn')


def release_log_path(release, now=None) -> Path:
    """~/.juneau-release-logs/<release>-<YYYYMMDD-HHMMSS>.log"""
    return RELEASE_LOG_DIR / f"{release}-{(now or datetime.now()):%Y%m%d-%H%M%S}.log"

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
                say(f"Warning: Could not load state file: {e}", 'warn')
        return {}
    
    def save(self):
        """Save current state to file."""
        try:
            with open(self.state_file, 'w') as f:
                json.dump(self.data, f, indent=2)
        except Exception as e:
            say(f"Warning: Could not save state file: {e}", 'warn')
    
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
        self.jr = run_module()   # juneau_run; not self.run, which would hide the run() method
        if self.jr is None:
            sys.exit(f"release.py needs juneau_run.py at {RUN_MODULE_PATH}")
        self.current = None      # the step whose row run_command's sub-rows go under
        self._sub = 0            # sub-row counter within the current step
        self.summary = None      # the current step's row text, set by the step method
        self.full_log = None
        self._versions = None    # (java -version, mvn -version) output, probed once
        self.state = ReleaseState()
        self.rc = rc  # Will be set during _load_env if not provided
        self.start_step = start_step
        self.skip_steps = set(skip_steps or [])
        self.resume = resume
        
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
            'run_git_diff',
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
                say(f"Warning: Could not load history file: {e}", 'warn')
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
            say(f"Warning: Could not save history file: {e}", 'warn')
    
    def _prompt_with_default(self, prompt: str, default: Optional[str] = None, required: bool = True) -> str:
        """Prompt for a value with a default."""
        full_prompt = f"{prompt} [{default}]: " if default else f"{prompt}: "
        while True:
            response = self.jr.ask(full_prompt).strip()
            if response:
                return response
            if default:
                return default
            if not required:
                return ""
            say("This field is required. Please enter a value.", 'warn')
    
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
        
        last_run = (datetime.fromisoformat(history['last_run_date']).strftime('%Y-%m-%d %H:%M:%S')
                    if history.get('last_run_date') else "Never (first run for this version)")
        self.jr.show(f"{'=' * 79}\nApache Juneau Release Configuration\n{'=' * 79}\n"
                      f"Detected version from pom.xml: {version}\n"
                      f"Calculated next version: {next_version}\n"
                      f"Last run: {last_run}\n{'=' * 79}\n")
        
        # Prompt for RC number if not already set, defaulting to the last one used for this version
        if self.rc is None:
            rc_match = re.search(RC_PATTERN, history.get('X_RELEASE_CANDIDATE') or '')
            default_rc = rc_match.group(1) if rc_match else None
            rc_prompt = "Release candidate number" + (f" [{default_rc}]" if default_rc else "") + ": "
            self.rc = ask_rc(self.jr, rc_prompt, default_rc)
        
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
        keys = ['X_VERSION', 'X_NEXT_VERSION', 'X_RELEASE', 'X_STAGING',
                'X_USERNAME', 'X_EMAIL', 'X_CLEANM2', 'X_GIT_BRANCH', 'X_JAVA_HOME']
        self.jr.show('--- Settings ' + '-' * 67 + '\n'
                      + ''.join(f"{key}: {os.environ.get(key, 'NOT SET')}\n" for key in keys)
                      + '-' * 80 + '\n')
    
    def fail(self, msg: str = None):
        """Show the failure under the current step and exit 1.  _in_session ends the session as fail."""
        self.jr.failure(f"❌ {msg}" if msg else "❌ Release failed", step=self.current)
        sys.exit(1)
    
    def success(self):
        """Every step ran: clear the checkpoint.  The session's final line reports the success."""
        self.state.clear()
    
    def yprompt(self, prompt: str) -> bool:
        """Yes/no prompt.  Enter means yes."""
        response = self.jr.ask(f"{prompt} (Y/n): ").strip()
        return not response or response.lower() in ('y', 'yes')
    
    def run_command(self, cmd: List[str], cwd: Optional[Path] = None, check: bool = True,
                    capture_output: bool = False) -> subprocess.CompletedProcess:
        """
        Run a command through run_tool as a sub-row of the current step.  Its output goes to the full log.  Maven
        gets -B, the maven parser and a PTY with the watchdog, and so does `svn commit`.  With check, a non-zero exit
        fails the release.  The result's stdout is the merged output when capture_output is set.
        """
        cmd, parser, tty = tool_options(cmd)
        self._sub += 1
        step_id = f"{self.current or 'release'}.{self._sub}"
        result = self.jr.run_tool(cmd, parser, step_id, command_title(cmd), n=self._sub, parent=self.current,
                                  cwd=cwd, capture=capture_output, tty=tty,
                                  watchdog=WATCHDOG_SECONDS if tty else None)
        if check and result.exit != 0:
            self.fail(f"Command failed (exit {result.exit}): {' '.join(cmd)}")
        return subprocess.CompletedProcess(cmd, result.exit, result.output, None)
    
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
        
        self.summary = f"{len(required_tools)} tools found"
        self.state.set_last_step('check_prerequisites')
    
    def check_java_version(self):
        """Check Java version, from the java -version output the header already probed."""
        java_version = self._parse_java_version(self._probe_versions()[0])
        
        if java_version is None:
            self.fail("Could not determine Java version from output")
        
        if java_version < MIN_JAVA_VERSION:
            self.fail(f"Java version {java_version} detected. Java {MIN_JAVA_VERSION} or higher is required.")
        
        self.summary = f"Java {self._java_version_text() or java_version}"
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
        """Check Maven version, from the mvn -version output the header already probed (maven-version.py if not)."""
        match = re.search(r'Apache Maven (\d+)', self._probe_versions()[1])
        maven_version = int(match.group(1)) if match else self._get_maven_version()
        
        if maven_version is None:
            self.fail("Could not determine Maven version")
        
        if maven_version < MIN_MAVEN_VERSION:
            self.fail(f"Maven version {maven_version} detected. Maven {MIN_MAVEN_VERSION} or higher is required.")
        
        self.summary = f"Maven {self._maven_version_text() or maven_version}"
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
            say(f"Warning: Could not determine Maven version using maven-version.py: {e}", 'warn')
            return None
    
    def clean_maven_repo(self):
        """Clean Maven repository."""
        clean_m2 = os.environ.get('X_CLEANM2', 'N')
        if clean_m2.upper() == 'N':
            self.summary = "kept (X_CLEANM2=N)"
        else:
            m2_repo = Path.home() / '.m2' / 'repository'
            if m2_repo.exists():
                old_repo = Path.home() / '.m2' / 'repository-old'
                if old_repo.exists():
                    shutil.rmtree(old_repo)
                m2_repo.rename(old_repo)
                # Remove in background
                subprocess.Popen(['rm', '-rf', str(old_repo)])
                self.summary = "moved aside, deleting in the background"
            else:
                self.summary = "already empty"
        
        self.state.set_last_step('clean_maven_repo')
    
    def make_git_folder(self):
        """Create git staging folder."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        
        # Clean up entire staging directory to start fresh (avoids issues from previous runs)
        if staging.exists():
            say(f"Removing existing staging directory: {staging}")
            shutil.rmtree(staging)
        
        staging.mkdir(parents=True, exist_ok=True)
        
        git_dir = staging / 'git'
        git_dir.mkdir(parents=True)
        
        self.summary = str(staging)
        self.state.set_last_step('make_git_folder')
    
    def clone_juneau(self):
        """Clone juneau.git repository."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        git_dir = staging / 'git'
        
        juneau_dir = git_dir / 'juneau'
        # Since we clean up the staging directory in make_git_folder, this should always be a fresh clone
        self.run_command(
            ['git', 'clone', 'https://gitbox.apache.org/repos/asf/juneau.git'],
            cwd=git_dir
        )
        
        self.state.set_last_step('clone_juneau')
    
    def configure_git(self):
        """Configure git user name and email."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        username = os.environ.get('X_USERNAME')
        email = os.environ.get('X_EMAIL')
        
        if username:
            self.run_command(['git', 'config', 'user.name', username], cwd=juneau_dir)
        if email:
            self.run_command(['git', 'config', 'user.email', email], cwd=juneau_dir)
        
        self.summary = f"{username or '?'} <{email or '?'}>"
        self.state.set_last_step('configure_git')
    
    def run_clean_verify(self):
        """Run Maven clean verify."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        self.run_command(['mvn', 'clean', 'verify'], cwd=juneau_dir)
        
        self.state.set_last_step('run_clean_verify')
    
    def run_deploy(self):
        """Run Maven deploy."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        self.run_command(['mvn', 'deploy', '-Daether.checksums.algorithms=MD5,SHA-1,SHA-512'], cwd=juneau_dir)
        
        self.state.set_last_step('run_deploy')
    
    def run_release_prepare(self):
        """Run Maven release:prepare."""
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
            say(f"Current POM version is SNAPSHOT: {current_version}")
        
        # run_command will automatically fail if the command doesn't succeed (check=True by default)
        self.run_command([
            'mvn', 'release:prepare',
            '-DautoVersionSubmodules=true',
            f'-DreleaseVersion={version}',
            f'-Dtag={release}',
            f'-DdevelopmentVersion={next_version}'
        ], cwd=juneau_dir)
        
        self.summary = f"tag {release}"
        self.state.set_last_step('run_release_prepare')
    
    def run_git_diff(self):
        """Show what release:prepare changed against the release tag, in the pager, before release:perform."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        release = os.environ.get('X_RELEASE')
        
        result = subprocess.run(['git', 'diff', release], cwd=juneau_dir, capture_output=True, text=True,
                                check=False)
        if result.returncode != 0:
            say(f"⚠ git diff {release} failed (exit {result.returncode}): {result.stderr.strip()}", 'warn')
            self.summary = f"git diff failed (exit {result.returncode})"
        else:
            self.jr.show(result.stdout or f"No differences against {release}.", pager=True)
            files = sum(1 for line in result.stdout.splitlines() if line.startswith('diff --git '))
            self.summary = f"{files} file(s)" if files else "no changes"
        self.state.set_last_step('run_git_diff')
    
    def run_release_perform(self):
        """Run Maven release:perform."""
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        juneau_dir = staging / 'git' / 'juneau'
        
        self.run_command(['mvn', 'release:perform', '-Daether.checksums.algorithms=MD5,SHA-1,SHA-512'], cwd=juneau_dir)
        
        # Open Nexus staging repositories page
        subprocess.Popen(['open', 'https://repository.apache.org/#stagingRepositories'])
        
        self.jr.show(
            "On Apache's Nexus instance, locate the staging repository for the code you just released.\n"
            "It should be called something like orgapachejuneau-1000.\n"
            "Check the Updated time stamp and click to verify its Content.\n"
            "IMPORTANT - When all artifacts to be deployed are in the staging repository, tick the box next to it and "
            "click Close.\n"
            "DO NOT CLICK RELEASE YET - the release candidate must pass [VOTE] emails on dev@juneau before we "
            "release.\n"
            "Once closing has finished (check with Refresh), browse to the URL of the staging repository which should "
            "be something like https://repository.apache.org/content/repositories/orgapachejuneau-1000.\n")
        
        repo_input = self.jr.ask("Enter the staging repository name AFTER CLOSING IT!!!: orgapachejuneau-").strip()
        repo_name = f"orgapachejuneau-{repo_input}"
        
        if not self.yprompt(f"X_REPO = {repo_name}.  Is this correct?"):
            self.fail("Repository name confirmation failed")
        
        os.environ['X_REPO'] = repo_name
        self.state.set('X_REPO', repo_name)
        
        self.summary = f"staging repo {repo_name}"
        self.state.set_last_step('run_release_perform')
    
    def create_binary_artifacts(self):
        """Create binary artifacts and upload to SVN."""
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
        
        # Remove old files.  No shell runs these, so list the entries here: 'source/*' would reach svn as a literal *.
        source_dir = dist_dir / 'source'
        binaries_dir = dist_dir / 'binaries'
        removed = self._svn_rm_contents(dist_dir, source_dir, binaries_dir)
        
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
            say(f"Renaming source zip: {source_zip.name} -> {target_zip.name}")
            source_zip.rename(target_zip)
            
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
        
        self.summary = f"committed {release} to dist/dev" + (f" · removed {removed} old" if removed else "")
        self.state.set_last_step('create_binary_artifacts')
    
    def _svn_rm_contents(self, dist_dir: Path, *dirs: Path) -> int:
        """
        One svn rm for every entry under each existing directory in dirs (paths relative to dist_dir), skipping
        dot-entries as a shell * would.  Returns how many it removed.
        """
        items = [str(item.relative_to(dist_dir)) for directory in dirs if directory.exists()
                 for item in sorted(directory.iterdir()) if not item.name.startswith('.')]
        if items:
            self.run_command(['svn', 'rm', *items], cwd=dist_dir, check=False)
        return len(items)
    
    def verify_distribution(self):
        """Verify distribution files are available."""
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
        total_mb = sum(file_path.stat().st_size for file_path in expected_files) / (1024 * 1024)
        for file_path in expected_files:
            size_mb = file_path.stat().st_size / (1024 * 1024)
            say(f"✓ {file_path.relative_to(dist_dir)} ({size_mb:.2f} MB)")
        
        # Open browser for manual inspection
        subprocess.Popen(['open', SVN_DIST_URL])
        
        say("Distribution verification successful. Voting can be started.")
        self.summary = f"{len(expected_files)} files, {total_mb:.1f} MB"
        
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
            say(f"Warning: Could not read SHA-512 from {url}: {e}", 'warn')
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
            say(f"Warning: Could not get git commit hash for tag {release_tag}: {e}", 'warn')
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
        self.jr.show(f"{'=' * 79}\nVOTE EMAIL BODY:\n{'=' * 79}\n{email_body}{'=' * 79}")
    
    def list_steps(self):
        """List all available steps."""
        print("\nAvailable steps:")
        for i, step in enumerate(self.steps, 1):
            print(f"  {i:2d}. {step}")
        print()
    
    def revert_release(self):
        """Revert a release by deleting the tag, reverting Maven versions, and cleaning up SVN files: one row each."""
        # Get version and release from state or environment
        version = self.state.get('X_VERSION') or os.environ.get('X_VERSION')
        release = self.state.get('X_RELEASE') or os.environ.get('X_RELEASE')
        
        if not version or not release:
            self.fail("X_VERSION and X_RELEASE must be set. Cannot determine what to revert.")
        
        # Confirm with user
        if not self.yprompt(f"Are you sure you want to revert release {release}? This will delete the git tag and clean up SVN files."):
            say("Revert cancelled.")
            return 'cancelled'
        
        staging = Path(os.environ.get('X_STAGING', STAGING_DIR)).expanduser()
        git_dir = staging / 'git' / 'juneau'
        dist_dir = staging / 'dist'
        has_clone = (git_dir / '.git').exists()
        
        with self._phase('revert_pull', 1, 'Pulling latest changes from git', 'Pull') as s:
            if not has_clone:
                s.skip()
                self.summary = f"no clone at {git_dir}"
            else:
                try:
                    self.run_command(['git', 'pull'], cwd=git_dir)
                except Exception as e:
                    say(f"Warning: Could not pull from git: {e}", 'warn')
        
        with self._phase('revert_tag', 2, f"Deleting git tag {release}", 'Tag') as s:
            if not has_clone:
                s.skip()
                self.summary = f"no clone at {git_dir}"
            else:
                try:
                    # Check if tag exists locally
                    local = subprocess.run(['git', 'tag', '-l', release], cwd=git_dir, capture_output=True, text=True,
                                           check=False)
                    if local.stdout.strip():
                        self.run_command(['git', 'tag', '-d', release], cwd=git_dir, check=False)
                    pushed = self.run_command(['git', 'push', 'origin', f':{release}'], cwd=git_dir, check=False)
                    self.summary = ("deleted" if pushed.returncode == 0
                                    else f"remote tag not deleted (exit {pushed.returncode})")
                except Exception as e:
                    say(f"Warning: Could not delete git tag: {e}", 'warn')
        
        with self._phase('revert_versions', 3, 'Reverting Maven versions', 'Versions') as s:
            development_version = f"{version}-SNAPSHOT"
            if not (git_dir / POM_XML).exists():
                s.skip()
                self.summary = f"no {POM_XML} in {git_dir}"
            else:
                try:
                    self.run_command([
                        'mvn', 'release:update-versions',
                        '-DautoVersionSubmodules=true',
                        f'-DdevelopmentVersion={development_version}'
                    ], cwd=git_dir)
                    self.summary = f"back to {development_version}"
                except Exception as e:
                    say(f"Warning: Could not revert Maven versions: {e}", 'warn')
        
        with self._phase('revert_svn', 4, 'Cleaning up SVN files', 'SVN') as s:
            if not (dist_dir / '.svn').exists():
                s.skip()
                self.summary = f"no checkout at {dist_dir}"
            else:
                try:
                    self.summary = self._revert_svn(dist_dir, release)
                except Exception as e:
                    say(f"Warning: Could not clean up SVN files: {e}", 'warn')
        
        self.jr.show("Release revert complete.  You may need to manually:\n"
                      "  - Reset your local git repository if needed\n"
                      "  - Verify Maven versions were reverted correctly\n"
                      "  - Check SVN repository for any remaining files")
    
    def _revert_svn(self, dist_dir: Path, release: str) -> str:
        """svn rm every RC directory under binaries/ and source/, then commit if you say so.  Returns the row text."""
        self.run_command(['svn', 'update'], cwd=dist_dir, check=False)
        
        removed = [item for sub in ('binaries', 'source') if (dist_dir / sub).exists()
                   for item in sorted((dist_dir / sub).iterdir()) if item.is_dir() and 'RC' in item.name]
        for item in removed:
            self.run_command(['svn', 'rm', str(item.relative_to(dist_dir))], cwd=dist_dir, check=False)
        if not removed:
            return "no RC directories"
        
        status = subprocess.run(['svn', 'status'], cwd=dist_dir, capture_output=True, text=True, check=False)
        if 'D' not in (status.stdout or ''):
            return "no SVN changes to commit"
        self.jr.show(f"SVN changes ready to commit:\n{status.stdout}")
        if not self.yprompt("Commit SVN deletions?"):
            return f"{len(removed)} removed, not committed"
        self.run_command(['svn', 'commit', '-m', f'Remove {release} release candidate'], cwd=dist_dir)
        return f"{len(removed)} removed and committed"
    
    def _probe_versions(self):
        """(java -version, mvn -version) output, run once: the header and the two version steps share it."""
        if self._versions is None:
            def probe(cmd):
                try:
                    result = subprocess.run(cmd, capture_output=True, text=True, check=False)
                except OSError:
                    return ''
                return (result.stderr or '') + (result.stdout or '')
            self._versions = (probe(['java', '-version']), probe(['mvn', '-version']))
        return self._versions
    
    def _java_version_text(self):
        match = re.search(r'version\s+"([^"]+)"', self._probe_versions()[0])
        return match.group(1) if match else None
    
    def _maven_version_text(self):
        match = re.search(r'Apache Maven (\S+)', self._probe_versions()[1])
        return match.group(1) if match else None
    
    def header_lines(self):
        """Release, Java and Maven versions for the session header; session() adds the full log."""
        release = os.environ.get('X_RELEASE') or self.state.get('X_RELEASE') or '?'
        java, maven = self._java_version_text(), self._maven_version_text()
        return [release] + ([f"Java {java}"] if java else []) + ([f"Maven {maven}"] if maven else [])
    
    def start_session(self, title, header):
        """
        Start the console view.  The full log always goes to ~/.juneau-release-logs/<release>-<ts>.log, whatever
        JUNEAU_RUN_CONSOLE says, and session() puts its path in the header.
        """
        release = os.environ.get('X_RELEASE') or self.state.get('X_RELEASE') or 'juneau-release'
        self.full_log = release_log_path(release)
        self.full_log.parent.mkdir(parents=True, exist_ok=True)
        self.full_log.touch()
        os.environ['JUNEAU_RUN_FULL_LOG'] = str(self.full_log)
        self.jr.session(f"🚀 {title}", header)
    
    def _in_session(self, title, body, header):
        """Run body in a console session and end it with done(ok|fail|cancelled).  body may return 'cancelled'."""
        self.start_session(title, header)
        try:
            status = body() or 'ok'
        except (KeyboardInterrupt, EOFError):   # Ctrl-C, or Ctrl-D at a prompt
            self._end('cancelled')
            sys.exit(130)
        except SystemExit as e:
            self._end('fail' if e.code else 'ok')
            raise
        except BaseException:
            self._end('fail')
            raise
        self._end(status)
    
    def _end(self, status):
        """done(status).  The final line names the full log only on fail (juneau_run's format_final), so say it
        first, at warn: info would be hidden at the default detail."""
        if status != 'fail':
            self.jr.say(f"full log: {self.full_log}", level='warn')
        self.jr.done(status)
    
    def run(self):
        """Run the release steps in one console session."""
        self._in_session('Juneau release', self._run_steps, self.header_lines())
    
    def run_revert(self):
        """Run revert_release in its own console session."""
        release = os.environ.get('X_RELEASE') or self.state.get('X_RELEASE') or '?'
        self._in_session('Juneau release revert', self.revert_release, [release])
    
    def _prompt_pgp(self):
        """Prime the gpg agent before any long step, on the real terminal: pinentry needs it."""
        prompt_script = Path(__file__).parent / 'prompt-pgp-passphrase.py'
        if not prompt_script.exists():
            return
        try:
            result = self.jr.passthrough([sys.executable, str(prompt_script)], 'pgp', 'Prime the PGP agent',
                                          label='PGP')
        except OSError as e:
            say(f"⚠ Could not run PGP passphrase prompt: {e}", 'warn')
            return
        if result.exit != 0:
            say("⚠ PGP passphrase prompt failed; gpg will ask again when Maven signs.", 'warn')
    
    @contextmanager
    def _phase(self, name, n, title, label):
        """A step row.  run_command's sub-rows go under it, and self.summary becomes its row text."""
        self.current, self._sub, self.summary = name, 0, None
        try:
            with self.jr.step(name, n, title, label=label) as s:
                yield s
                s.summary = self.summary
        finally:
            self.current = None
    
    def _run_steps(self):
        """The release steps in order, one row each.  A step that does not run is a dim row saying why."""
        self._prompt_pgp()
        
        if self.resume:
            last_step = self.state.get_last_step()
            if last_step in self.steps[:-1]:
                self.start_step = self.steps[self.steps.index(last_step) + 1]
                say(f"Resuming after {last_step}: starting from {self.start_step}")
        
        for n, name in enumerate(self.steps, 1):
            label, title = STEP_TITLES.get(name, (None, name))
            if not self.should_run_step(name):
                with self.jr.row(name, title, label=label) as s:
                    s.skip()
                    s.summary = '(skipped)' if name in self.skip_steps else '(done earlier)'
                continue
            
            resume = f"To resume: python3 scripts/release.py --start-step {name}"
            try:
                with self._phase(name, n, title, label):
                    getattr(self, name)()
            except KeyboardInterrupt:
                self.jr.failure(f"Interrupted in {name}. {resume}", step=name)
                raise
            except SystemExit as e:
                if e.code:
                    self.jr.failure(resume, step=name)
                raise
            except Exception as e:
                self.jr.failure(f"❌ Error in step {name}: {e}\n{resume}", step=name)
                raise
        
        self.success()


def main(argv=None):
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
    parser.add_argument(
        '--detail',
        help='Console detail: summary, actionable (default), modules or all (also JUNEAU_RUN_DETAIL)'
    )
    
    args = parser.parse_args(argv)
    
    run = run_module()
    if run is None:
        parser.error(f"juneau_run.py not found at {RUN_MODULE_PATH}")
    if os.environ.pop('RUN_MARKERS', None):
        run.warn("release.py does not emit run markers; RUN_MARKERS is ignored")
    try:
        run.export_detail(args.detail)
    except ValueError as e:
        parser.error(str(e))
    
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
                say(f"📌 Detected RC number from X_RELEASE: {rc}")
        
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
                        say(f"📌 Detected RC number from state file: {rc}")
                
                # Set environment variables from state to avoid prompts
                if state_data.get('X_VERSION'):
                    os.environ['X_VERSION'] = state_data['X_VERSION']
                if state_data.get('X_RELEASE'):
                    os.environ['X_RELEASE'] = state_data['X_RELEASE']
                if state_data.get('X_STAGING'):
                    os.environ['X_STAGING'] = state_data['X_STAGING']
            except Exception as e:
                say(f"Warning: Could not load state file: {e}", 'warn')
        
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
                    say(f"📌 Detected version from pom.xml: {version}")
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
                            say(f"📌 Detected RC number from history file: {rc}")
                except Exception as e:
                    say(f"Warning: Could not load history file: {e}", 'warn')
        
        # If we still don't have RC, prompt for it
        if version and rc is None:
            rc = ask_rc(run, "Release candidate number: ")
        
        # Construct X_RELEASE if we have version and RC
        if version and rc:
            release = f"juneau-{version}-RC{rc}"
            os.environ['X_RELEASE'] = release
            say(f"📌 Constructed release: {release}")
        
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
        
        script.run_revert()
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
                say(f"📌 Detected RC number from X_RELEASE: {rc}")
        
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
                            say(f"📌 Detected RC number from state file: {rc}")
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
                            say(f"📌 Detected RC number from history file: {rc}")
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
    try:
        main()
    except (KeyboardInterrupt, EOFError):   # Ctrl-C or Ctrl-D at a prompt before the session starts
        sys.exit(130)

