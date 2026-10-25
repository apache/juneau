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
Build the Apache Juneau documentation, replicating the GitHub Actions workflow steps.

Full docs: build-docs.md
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path

import run_view
from juneau_root import find_juneau_root

# Module-level toggles flipped from main() so every run_command/stage_banner sees the choice.
DRY_RUN = False
VERBOSE = False
# The juneau-run live view, or plain output (the default, and whenever juneau-run is unavailable).
VIEW = run_view.View()


def say(text='', level='info'):
    """Status text: printed, or under the live view a note filtered by its detail level."""
    VIEW.say(text, level)


def stage_banner(title):
    """Emit a stage header. Verbose mode keeps the full banner; default mode prints one line."""
    VIEW.verbose = VERBOSE
    VIEW.banner(title)


def run_command(cmd, cwd=None, env=None, *, id, title, label=None, tool=None, parent=None):
    """Run a command as the row id (a child row with parent); title is its plain banner. Honors DRY_RUN.

    Raises CalledProcessError when the command fails.
    """
    stage_banner(title)
    if DRY_RUN:
        loc = f" (in {cwd})" if cwd else ""
        print(f"[dry-run] would run: {' '.join(map(str, cmd))}{loc}")
        return
    VIEW.cmd(cmd, id=id, title=title, label=label, tool=tool, cwd=cwd, env=env, parent=parent)

def row_plan(args, dev_reason=None):
    """The build's rows, in order, as (id, label, skip reason or None); dev_reason skips npm and the Docusaurus build."""
    npm_skip = '--skip-npm' if args.skip_npm else dev_reason
    maven_skip = '--skip-maven' if args.skip_maven else None
    skips = {
        'npm': npm_skip,
        'install': maven_skip,
        'site': maven_skip,
        'copy': maven_skip,
        'docs': npm_skip,
        'verify': '--skip-verify' if args.skip_verify else None,
        'links': maven_skip,
    }
    return [(id, label, skips.get(id)) for id, label in run_view.BUILD_ROWS]

def dev_server_reason(docs_dir):
    """Why npm ci and the production build must not run now (a Docusaurus dev server is up), or None.

    Running 'docusaurus build' (production webpack) or 'npm ci' while 'docusaurus start' runs makes
    both fight over node_modules/.cache/webpack/, which corrupts the dev server's cache and crashes it.
    """
    try:
        result = subprocess.run(['pgrep', '-f', 'docusaurus start'], capture_output=True, text=True)
        if result.returncode == 0 and result.stdout.strip():
            return "dev server running"
    except FileNotFoundError:
        pass
    try:
        result = subprocess.run(['lsof', '-ti:3000'], capture_output=True, text=True)
        if result.stdout.strip():
            return "dev server on port 3000"
    except FileNotFoundError:
        pass
    if (Path(docs_dir) / '.dev-server-running').exists():
        return "dev server lock file found"
    return None

def check_prerequisites():
    """Check that required tools are available."""
    required_tools = {
        'node': 'Node.js',
        'npm': 'npm',
        'mvn': 'Maven',
        'java': 'Java'
    }
    
    missing = []
    for tool, name in required_tools.items():
        result = subprocess.run(['which', tool], capture_output=True)
        if result.returncode != 0:
            missing.append(name)
    
    if missing:
        say(f"ERROR: Missing required tools: {', '.join(missing)}", 'error')
        say("Please install the missing tools and try again.")
        sys.exit(1)
    
    say("✓ All required tools are available")

def install_npm_dependencies(docs_dir):
    """Install npm dependencies for Docusaurus."""
    run_command(['npm', 'ci'], cwd=docs_dir, id='npm', label='npm', title="Installing npm dependencies")

def write_json_if_changed(out_path, mapping):
    """Write mapping as indented JSON, but only when the file content would change.

    Keeps the working tree clean (and file mtimes stable) on repeated builds.

    Returns:
        True if the file was written, False if it was already up to date.
    """
    out_path = Path(out_path)
    content = json.dumps(mapping, indent=2) + '\n'
    if out_path.is_file() and out_path.read_text() == content:
        return False
    out_path.write_text(content)
    return True

def generate_artifact_paths(master_root, docs_dir):
    """Scan every module pom.xml under the master checkout and emit artifact-paths.json.

    Produces a map of <artifactId> -> module directory (relative to the repo root),
    consumed by docusaurus.config.ts (customFields.juneauModulePaths) to build the
    "Source" GitHub link in the <DependencyInfo> component. The committed snapshot in
    the docs repo is the dev-server fallback and is refreshed on every real build.
    """
    stage_banner("Generating artifact-paths.json")
    out_path = Path(docs_dir) / 'artifact-paths.json'
    if DRY_RUN:
        say(f"[dry-run] would scan {master_root} pom.xml files -> {out_path}")
        return

    import xml.etree.ElementTree as ET

    def read_pom(pom_path):
        """Return (artifactId, packaging) of a pom; packaging defaults to Maven's jar."""
        try:
            root = ET.parse(pom_path).getroot()
        except Exception:
            return None
        ns_uri = root.tag.split('}')[0].strip('{') if '}' in root.tag else ''
        prefix = ('{%s}' % ns_uri) if ns_uri else ''
        # Only direct <project> children (ignore parent/dependency ids).
        children = {child.tag: (child.text or '').strip() for child in root}
        aid = children.get(prefix + 'artifactId')
        return (aid, children.get(prefix + 'packaging') or 'jar') if aid is not None else None

    skip_dirs = {'.git', 'target', 'node_modules', 'src'}
    mapping = {}
    jar_modules = 0
    for dirpath, dirnames, filenames in os.walk(master_root):
        dirnames[:] = [d for d in dirnames if d not in skip_dirs]
        if 'pom.xml' in filenames:
            pom = read_pom(os.path.join(dirpath, 'pom.xml'))
            if pom and pom[0]:
                aid = pom[0]
                jar_modules += pom[1] == 'jar'
                rel = os.path.relpath(dirpath, master_root)
                rel = '' if rel == '.' else rel.replace(os.sep, '/')
                mapping[aid] = rel

    if not jar_modules:
        say(f"ERROR: No jar-packaged module pom.xml found under {master_root}; refusing to overwrite {out_path}. "
            "Check --juneau-root.", 'error')
        sys.exit(1)

    mapping = {k: mapping[k] for k in sorted(mapping)}
    changed = write_json_if_changed(out_path, mapping)
    say(f"✓ {'Wrote' if changed else 'Unchanged:'} {len(mapping)} artifacts in {out_path}")

def generate_javadoc_packages(master_root, docs_dir, juneau_version):
    """Derive an artifactId -> base-package map and emit artifact-packages.json.

    For each module in artifact-paths.json, the base package is the shallowest
    org/apache/juneau/... package in the module's src/main/java tree that actually
    has a generated package-summary.html under static/javadocs/<version>/. This map
    lets <DependencyInfo> point its "Javadocs" link at the module's top-level package
    summary instead of the aggregate index. Modules with no source (parents, bundles,
    shaded/aggregator poms) or no matching package summary are simply omitted, and the
    component falls back to the aggregate index for them. A committed snapshot is the
    dev-server fallback and is refreshed on every real build.
    """
    stage_banner("Generating artifact-packages.json")
    out_path = Path(docs_dir) / 'artifact-packages.json'
    paths_path = Path(docs_dir) / 'artifact-paths.json'
    javadocs_dir = Path(docs_dir) / 'static' / 'javadocs' / juneau_version

    if DRY_RUN:
        say(f"[dry-run] would derive base packages from {master_root} validated "
              f"against {javadocs_dir} -> {out_path}")
        return

    if not paths_path.exists():
        say(f"WARNING: {paths_path} not found; skipping javadoc-package map", 'warn')
        return
    if not javadocs_dir.is_dir():
        say(f"WARNING: {javadocs_dir} not found; skipping javadoc-package map", 'warn')
        return

    with open(paths_path) as f:
        artifact_paths = json.load(f)

    def base_package_for(module_rel):
        src = Path(master_root) / module_rel / 'src' / 'main' / 'java'
        if not src.is_dir():
            return None
        pkgs = []
        for dirpath, _dirnames, filenames in os.walk(src):
            if any(fn.endswith('.java') for fn in filenames):
                rel = os.path.relpath(dirpath, src).replace(os.sep, '/')
                if rel.startswith('org/apache/juneau'):
                    pkgs.append(rel)
        # Shallowest first (fewest segments), tie-break alphabetical, then pick the
        # first that actually has a generated package-summary.html.
        pkgs.sort(key=lambda p: (p.count('/'), p))
        for pkg in pkgs:
            if (javadocs_dir / pkg / 'package-summary.html').is_file():
                return pkg
        return None

    mapping = {}
    for artifact in sorted(artifact_paths):
        pkg = base_package_for(artifact_paths[artifact])
        if pkg:
            mapping[artifact] = pkg

    changed = write_json_if_changed(out_path, mapping)
    say(f"✓ {'Wrote' if changed else 'Unchanged:'} {len(mapping)} artifact base packages in {out_path}")

def build_docusaurus(docs_dir, staging=False, juneau_version=None):
    """Build Docusaurus documentation."""
    env = os.environ.copy()
    if staging:
        env['SITE_URL'] = 'https://juneau.staged.apache.org'
        if VERBOSE:
            say("  Setting SITE_URL for staging build")
    if juneau_version:
        env['JUNEAU_VERSION'] = juneau_version
        if VERBOSE:
            say(f"  Setting JUNEAU_VERSION={juneau_version} for the build")
    run_command(['npm', 'run', 'build'], cwd=docs_dir, env=env, id='docs', label='Docs build',
                title="Building Docusaurus")

def compile_java_modules(master_root):
    """Compile and install all Java modules to local repository."""
    run_command(['mvn', 'clean', 'install', '-DskipTests'], cwd=master_root, id='install', label='Install',
                title="Compiling and installing Java modules")

def generate_maven_site(master_root):
    """Generate Maven site."""
    run_command(['mvn', 'site', '-DskipTests'], cwd=master_root, id='site', label='Site',
                title="Generating Maven site")

def copy_maven_site(master_root, docs_dir):
    """Copy Maven site to static directory (Docusaurus will copy it to build)."""
    stage_banner("Copying Maven site to static directory")

    source_site = Path(master_root) / 'target' / 'site'
    static_dir = Path(docs_dir) / 'static'
    static_site = static_dir / 'site'

    if DRY_RUN:
        say(f"[dry-run] would copy {source_site} -> {static_site}")
        return

    if not source_site.exists():
        say(f"ERROR: Maven site not found at {source_site}", 'error')
        sys.exit(1)
    
    # Create static directory if it doesn't exist
    static_dir.mkdir(parents=True, exist_ok=True)
    
    # Copy site contents to static/site (Docusaurus will copy to build during build)
    say(f"Copying {source_site} to {static_site}")
    if static_site.exists():
        shutil.rmtree(static_site)
    shutil.copytree(source_site, static_site)
    say(f"✓ Maven site copied to {static_site}")

def resolve_javadocs_version(docs_dir):
    """Resolve the current javadocs version robustly for the apidocs symlink.

    Prefers the latest entry in static/javadocs/releases.json that has a matching
    committed static/javadocs/<version>/ directory; falls back to the sole versioned
    directory when the index is unavailable. Returns None if none can be determined.
    """
    javadocs_dir = Path(docs_dir) / 'static' / 'javadocs'
    releases_json = javadocs_dir / 'releases.json'

    def version_key(v):
        try:
            return tuple(int(p) if p.isdigit() else p for p in v.split('.'))
        except (ValueError, AttributeError):
            return v

    if releases_json.is_file():
        try:
            with open(releases_json) as f:
                releases = json.load(f)
            versions = [r.get('version') for r in releases if r.get('version')]
            versions = [v for v in versions if (javadocs_dir / v).is_dir()]
            if versions:
                versions.sort(key=version_key, reverse=True)
                return versions[0]
        except Exception as e:
            say(f"WARNING: Could not read {releases_json}: {e}", 'warn')

    # Fallback: a single versioned directory unambiguously identifies the version.
    if javadocs_dir.is_dir():
        subdirs = [d.name for d in javadocs_dir.iterdir()
                   if d.is_dir() and d.name[:1].isdigit()]
        if len(subdirs) == 1:
            return subdirs[0]

    return None

def ensure_apidocs_symlink(docs_dir, version):
    """Point static/site/apidocs at the committed static/javadocs/<version> tree.

    Creates a RELATIVE symlink (target '../javadocs/<version>', resolved relative to
    static/site/) so the unversioned /site/apidocs/... links resolve to the committed
    javadocs with no Maven rebuild. Idempotent, and replaces an existing real directory
    (e.g. the one the Maven-site copy just produced) with the symlink.
    """
    stage_banner("Linking static/site/apidocs to committed javadocs")

    static_site = Path(docs_dir) / 'static' / 'site'
    apidocs = static_site / 'apidocs'
    target = os.path.join('..', 'javadocs', version)
    versioned = Path(docs_dir) / 'static' / 'javadocs' / version

    if DRY_RUN:
        say(f"[dry-run] would symlink {apidocs} -> {target}")
        return

    if not versioned.is_dir():
        say(f"WARNING: Committed javadocs not found at {versioned}; skipping apidocs symlink", 'warn')
        return

    static_site.mkdir(parents=True, exist_ok=True)

    # Idempotent: leave an already-correct relative symlink alone.
    if apidocs.is_symlink():
        if os.readlink(apidocs) == target:
            say(f"✓ apidocs already links to {target}")
            return
        apidocs.unlink()
    elif apidocs.is_dir():
        shutil.rmtree(apidocs)
    elif apidocs.exists():
        apidocs.unlink()

    os.symlink(target, apidocs)
    say(f"✓ Linked {apidocs} -> {target}")

def verify_apidocs(master_root):
    """Verify that apidocs were generated."""
    stage_banner("Verifying apidocs generation")
    if DRY_RUN:
        say("[dry-run] would verify apidocs at target/site/apidocs")
        return

    apidocs_dir = Path(master_root) / 'target' / 'site' / 'apidocs'
    
    if not apidocs_dir.exists():
        say(f"ERROR: target/site/apidocs directory not found after generation!", 'error')
        say(f"Contents of target/site:")
        site_dir = Path(master_root) / 'target' / 'site'
        if site_dir.exists():
            for item in site_dir.iterdir():
                say(f"  {item.name}")
        sys.exit(1)
    
    say(f"✓ Javadocs found in {apidocs_dir}")
    
    # Count files to give some indication of success
    file_count = sum(1 for _ in apidocs_dir.rglob('*.html'))
    say(f"✓ Found {file_count} HTML files in apidocs")

def get_current_release(master_root):
    """Get current release version using current-release.py script from master branch."""
    script_dir = Path(__file__).parent
    master_scripts_dir = master_root / 'scripts'
    current_release_script = master_scripts_dir / 'current-release.py'
    
    if not current_release_script.exists():
        say(f"WARNING: current-release.py not found at {current_release_script}", 'warn')
        return None
    
    try:
        result = subprocess.run(
            [sys.executable, str(current_release_script)],
            cwd=master_root,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception as e:
        say(f"WARNING: Could not get current release: {e}", 'warn')
        return None

def copy_current_javadocs(master_root, docs_dir):
    """Copy current javadocs to versioned folder and update JSON index."""
    stage_banner("Copying current javadocs to versioned folder")
    if DRY_RUN:
        say("[dry-run] would copy target/site/apidocs to static/javadocs/<version>")
        return

    # Get current release version
    current_version = get_current_release(master_root)
    if not current_version:
        say("WARNING: Could not determine current version, skipping javadocs copy", 'warn')
        return
    
    source_apidocs = Path(master_root) / 'target' / 'site' / 'apidocs'
    javadocs_dir = Path(docs_dir) / 'static' / 'javadocs'
    versioned_javadocs = javadocs_dir / current_version
    
    if not source_apidocs.exists():
        say(f"WARNING: Source apidocs not found at {source_apidocs}, skipping copy", 'warn')
        return
    
    # Create javadocs directory if it doesn't exist
    javadocs_dir.mkdir(parents=True, exist_ok=True)
    
    # Copy apidocs to versioned folder
    say(f"Copying {source_apidocs} to {versioned_javadocs}")
    if versioned_javadocs.exists():
        shutil.rmtree(versioned_javadocs)
    shutil.copytree(source_apidocs, versioned_javadocs)
    say(f"✓ Javadocs copied to {versioned_javadocs}")
    
    # Update JSON file with release information
    update_javadocs_json(docs_dir, current_version)

def update_javadocs_json(docs_dir, current_version):
    """Update or create JSON file with javadoc release information."""
    stage_banner("Updating javadocs release index")
    
    javadocs_dir = Path(docs_dir) / 'static' / 'javadocs'
    json_file = javadocs_dir / 'releases.json'
    
    # Load existing releases if file exists
    releases = []
    if json_file.exists():
        try:
            with open(json_file, 'r') as f:
                releases = json.load(f)
        except Exception as e:
            say(f"Warning: Could not load existing releases.json: {e}", 'warn')
            releases = []
    
    # Check if current version already exists
    existing_index = None
    for i, release in enumerate(releases):
        if release.get('version') == current_version:
            existing_index = i
            break
    
    # Add or update current version
    release_info = {
        'version': current_version,
        'path': f'{current_version}/',
        'released': datetime.now().strftime('%Y-%m-%d')
    }
    
    if existing_index is not None:
        releases[existing_index] = release_info
        say(f"✓ Updated release {current_version} in index")
    else:
        releases.append(release_info)
        say(f"✓ Added release {current_version} to index")
    
    # Sort releases by version (newest first)
    def version_key(v):
        parts = v['version'].split('.')
        # Convert to integers, handling any non-numeric parts
        try:
            return tuple(int(p) if p.isdigit() else p for p in parts)
        except (ValueError, AttributeError):
            # Fallback: use string comparison
            return v['version']
    
    releases.sort(key=version_key, reverse=True)
    
    # Save JSON file
    try:
        with open(json_file, 'w') as f:
            json.dump(releases, f, indent=2)
        say(f"✓ Updated {json_file}")
    except Exception as e:
        say(f"Warning: Could not save releases.json: {e}", 'warn')

def check_topic_links(master_root, docs_dir):
    """Run the topic link checker to validate documentation links (a child row of Links)."""
    # Use the checker script from docs/scripts (it's already there)
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-topic-links.py'
    
    if not checker_script.exists():
        say(f"WARNING: Topic link checker not found at {checker_script}", 'warn')
        return
    
    # The checker script needs both master_root and docs_dir
    # It will scan master_root for links and docs_dir for topics
    run_command(['python3', str(checker_script), '--juneau-root', str(master_root)], cwd=docs_dir,
                id='links-topics', label='Topic links', title="Checking topic links", tool='generic', parent='links')

def verify_docs_integrity(docs_dir):
    """Run the documentation-integrity checker (verify-docs.py).

    Validates topic frontmatter, filename/slug conventions, sidebars.ts
    consistency (dangling refs, orphans, directory-vs-sidebar order), and internal
    links. Only needs the docs checkout, so it runs even in --skip-maven CI smoke
    runs. Fails the build (non-zero) when the checker reports any errors.
    """
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'verify-docs.py'
    if not checker_script.exists():
        say(f"WARNING: Documentation integrity checker not found at {checker_script}", 'warn')
        return
    # run_command uses check=True, so a non-zero exit (errors found) raises
    # CalledProcessError and fails the build in main()'s handler.
    run_command(['python3', str(checker_script)], cwd=docs_dir, id='verify-docs', label='Integrity',
                title="Verifying documentation integrity", tool='generic', parent='verify')

def check_project_pages(docs_dir):
    """Run the project-pages checker (a child row of Verify): the About pages match src/data/projects.ts."""
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-project-pages.py'
    if not checker_script.exists():
        say(f"WARNING: Project pages checker not found at {checker_script}", 'warn')
        return
    run_command(['python3', str(checker_script), '--docs-dir', str(docs_dir)], cwd=docs_dir,
                id='verify-projects', label='Project pages', title="Checking project pages", tool='generic',
                parent='verify')

def check_ai_artifacts(master_root, docs_dir):
    """Run AI artifact drift checks (a child row of Links)."""
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-ai-artifacts.py'
    if not checker_script.exists():
        say(f"WARNING: AI artifact checker not found at {checker_script}", 'warn')
        return
    run_command(['python3', str(checker_script), '--juneau-root', str(master_root)], cwd=docs_dir,
                id='links-ai', label='AI artifacts', title="Checking AI artifacts", tool='generic', parent='links')

def check_code_fences(docs_dir):
    """Run the code-fence checker (a child row of Links): no HTML anchors inside fenced code blocks."""
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-code-fences.py'
    if not checker_script.exists():
        say(f"WARNING: Code fence checker not found at {checker_script}", 'warn')
        return
    run_command(['python3', str(checker_script), '--docs-root', str(docs_dir)], cwd=docs_dir,
                id='links-fences', label='Code fences', title="Checking code fences", tool='generic', parent='links')

def check_java_fences(docs_dir):
    """Run the Java-fence parse check (a child row of Links): every ```java fence parses unless marked `fragment`."""
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-java-fences.py'
    if not checker_script.exists():
        say(f"WARNING: Java fence checker not found at {checker_script}", 'warn')
        return
    run_command(['python3', str(checker_script), '--docs-root', str(docs_dir), '--strict'], cwd=docs_dir,
                id='links-java', label='Java fences', title="Checking Java fences", tool='generic', parent='links')

def check_guide_snippets(master_root, docs_dir):
    """Run the guide-snippet checker (a child row of Links): every guide= fence matches its region in juneau-examples-guides."""
    script_dir = Path(__file__).parent
    checker_script = script_dir / 'check-guide-snippets.py'
    if not checker_script.exists():
        say(f"WARNING: Guide snippet checker not found at {checker_script}", 'warn')
        return
    run_command(['python3', str(checker_script), '--juneau-root', str(master_root)], cwd=docs_dir,
                id='links-guides', label='Guide snippets', title="Checking guide snippets", tool='generic', parent='links')

def build(args, master_root, docs_dir):
    """Every build stage, one row each; returns the exit code (failures raise or exit)."""
    rows = {id: reason for id, _label, reason in row_plan(args, None if DRY_RUN else dev_server_reason(docs_dir))}

    with VIEW.stage('clean', 'Clean', "Cleaning build directory") as step:
        # Check prerequisites
        if DRY_RUN:
            say("[dry-run] would check prerequisites (node, npm, mvn, java)")
        else:
            check_prerequisites()
        # Delete build directory to ensure fresh build
        build_dir = Path(docs_dir) / 'build'
        if build_dir.exists():
            if DRY_RUN:
                say(f"[dry-run] would remove {build_dir}")
            else:
                say(f"Removing {build_dir}")
                shutil.rmtree(build_dir)
                say("✓ Build directory cleaned")
                step.summary = "build/ removed"

    # Install npm dependencies (can be done early).  A running dev server skips it: npm ci wipes
    # node_modules/.cache and corrupts the running server's webpack cache.
    if rows['npm']:
        VIEW.skip('npm', 'npm', "npm install", rows['npm'])
    else:
        install_npm_dependencies(docs_dir)

    # Resolve the release version and refresh artifact-paths.json from the module
    # poms whenever the master checkout is available (needed by <DependencyInfo>).
    juneau_version = None
    if not args.skip_maven or (master_root and master_root.exists()):
        juneau_version = get_current_release(master_root)
        if juneau_version:
            say(f"Juneau release version: {juneau_version}")
        generate_artifact_paths(master_root, docs_dir)

    # Generate Maven site and javadocs (must be done before building Docusaurus)
    # Note: Aggregate javadocs are now generated automatically as part of mvn site via the reporting section
    if rows['install']:
        VIEW.skip('install', 'Install', "Maven install", rows['install'])
        VIEW.skip('site', 'Site', "Maven site", rows['site'])
    else:
        compile_java_modules(master_root)
        generate_maven_site(master_root)
        verify_apidocs(master_root)

    # Copy current javadocs to versioned folder, and the Maven site to the static directory
    # BEFORE building Docusaurus (Docusaurus will automatically copy static/ contents to build/).
    # Both need Maven output, so --skip-maven skips the row; --skip-copy skips the site copy only.
    if rows['copy']:
        VIEW.skip('copy', 'Copy', "Copying javadocs and site", rows['copy'])
    else:
        with VIEW.stage('copy', 'Copy', "Copying javadocs and site") as step:
            copy_current_javadocs(master_root, docs_dir)
            if args.skip_copy:
                say("Skipping Maven site copy (--skip-copy)")
                step.summary = "javadocs; site skipped (--skip-copy)"
            else:
                copy_maven_site(master_root, docs_dir)

    # Derive the artifact -> base-package map for the <DependencyInfo> "Javadocs"
    # link, validated against the javadocs now in place (freshly copied above, or
    # the committed snapshot when Maven is skipped). Requires the master checkout
    # for source trees and a resolved version.
    if master_root and master_root.exists() and juneau_version:
        generate_javadoc_packages(master_root, docs_dir, juneau_version)

    # Point static/site/apidocs at the committed versioned javadocs via a relative
    # symlink so the unversioned /site/apidocs/... links resolve without a rebuild.
    apidocs_version = juneau_version or resolve_javadocs_version(docs_dir)
    if apidocs_version:
        ensure_apidocs_symlink(docs_dir, apidocs_version)
    else:
        say("WARNING: Could not resolve javadocs version; skipping apidocs symlink", 'warn')

    # Build Docusaurus documentation (will copy static/ contents to build/)
    if rows['docs']:
        VIEW.skip('docs', 'Docs build', "Docusaurus build", rows['docs'])
    else:
        build_docusaurus(docs_dir, staging=args.staging, juneau_version=juneau_version)

    # Copy .asf.yaml to build directory (needed for deployment)
    # Skip when --skip-maven: master_root may not exist in that scenario (CI smoke check).
    if not args.skip_copy and not args.skip_maven:
        build_dir = Path(docs_dir) / 'build'
        asf_yaml = Path(master_root) / '.asf.yaml'
        if DRY_RUN:
            stage_banner("Copying .asf.yaml to build directory")
            say(f"[dry-run] would copy {asf_yaml} -> {build_dir}/.asf.yaml")
        elif asf_yaml.exists() and build_dir.exists():
            stage_banner("Copying .asf.yaml to build directory")
            shutil.copy2(asf_yaml, build_dir)

    # Documentation-integrity and project-page checks — need only the docs checkout, so they run
    # even in --skip-maven CI smoke runs. Fails the build if errors are found.
    if rows['verify']:
        VIEW.skip('verify', 'Verify', "documentation checks", rows['verify'])
    else:
        with VIEW.stage('verify', 'Verify', "Verifying documentation"):
            verify_docs_integrity(docs_dir)
            # check_project_pages(docs_dir) is re-hooked together with the About pages.

    # Checks that require sibling repos — skip in --skip-maven / CI smoke runs
    # where only the docs repo is checked out.
    if rows['links']:
        VIEW.skip('links', 'Links', "link checks", rows['links'])
    else:
        with VIEW.stage('links', 'Links', "Checking links"):
            check_topic_links(master_root, docs_dir)
            check_ai_artifacts(master_root, docs_dir)
            check_code_fences(docs_dir)
            check_java_fences(docs_dir)
            check_guide_snippets(master_root, docs_dir)
    return 0

def print_completion(args, docs_dir):
    """The closing lines: where the build is, and the publish reminder after a full build."""
    print("\n=== Documentation build complete ===")
    print(f"Documentation is available in: {docs_dir / 'build'}")

    # Publish reminder — only when this was a real, complete build
    # (no skips, not a dry-run). Skipped runs are typically partial/iterative
    # and the reminder would be misleading.
    any_skip = args.skip_npm or args.skip_maven or args.skip_copy or args.skip_verify
    if not DRY_RUN and not any_skip:
        print()
        print("Next step — publish:")
        print("  python3 scripts/release-docs-stage.py             # deploy to asf-staging")
        print("  python3 scripts/release-docs-stage.py --no-push   # rehearse without pushing")
        print("Then, after eyeballing https://juneau.staged.apache.org:")
        print("  python3 scripts/release-docs.py                   # promote asf-staging -> asf-site (LIVE)")

def main():
    global DRY_RUN, VERBOSE, VIEW

    parser = argparse.ArgumentParser(description='Build Apache Juneau documentation')
    parser.add_argument('--skip-npm', action='store_true', help='Skip npm install and Docusaurus build')
    parser.add_argument('--skip-maven', action='store_true', help='Skip Maven compilation and site generation')
    parser.add_argument('--skip-copy', action='store_true', help='Skip copying Maven site to static directory')
    parser.add_argument('--skip-verify', action='store_true', help='Skip the documentation-integrity check (verify-docs.py)')
    parser.add_argument('--staging', action='store_true', help='Build for staging (sets SITE_URL to juneau.staged.apache.org)')
    parser.add_argument('--dry-run', action='store_true', help='Print every command/copy that would run; perform no work')
    parser.add_argument('--verbose', action='store_true', help='Print full per-stage banner output (default: one line per stage)')
    parser.add_argument('--juneau-root', type=Path, default=None,
                        help='Juneau source checkout to build against (default: $JUNEAU_ROOT, then sibling ../master, then ../juneau)')
    run_view.add_console_argument(parser)

    args = parser.parse_args()
    DRY_RUN = args.dry_run
    VERBOSE = args.verbose

    # Determine script directory and find the Juneau source checkout
    script_dir = Path(__file__).parent.absolute()
    docs_dir = script_dir.parent  # docs/
    # When --skip-maven is set, the Juneau checkout is not needed; treat it as allowed-missing
    # so CI runners (which have no sibling checkout) can still run the Docusaurus smoke.
    master_root = find_juneau_root(script_dir.parent, args.juneau_root,
                                   allow_missing=DRY_RUN or args.skip_maven, validate=not DRY_RUN)
    VIEW = run_view.open_view(master_root, args.console, verbose=VERBOSE, plain=DRY_RUN)

    # Change to docs directory for npm operations
    os.chdir(docs_dir)

    if VIEW.live:
        header = ["Juneau Docs"]
        if args.staging:
            header.append("Staging build (SITE_URL juneau.staged.apache.org)")
        lines = [f"Juneau root: {master_root}", f"Docs: {docs_dir}"]
        code = VIEW.session("Build", lambda: build(args, master_root, docs_dir), header=header, lines=lines,
                            plan=[(id, label) for id, label in run_view.BUILD_ROWS], repo=docs_dir, mode='test')
        if code == 0 and not os.environ.get('JUNEAU_PUSH_ACTIVE'):
            print_completion(args, docs_dir)
        sys.exit(code)

    print(f"Juneau source root: {master_root}")
    print(f"Docs directory: {docs_dir}")
    if DRY_RUN:
        print("DRY-RUN: no commands will be executed")
    try:
        build(args, master_root, docs_dir)
        print_completion(args, docs_dir)
    except subprocess.CalledProcessError as e:
        print(f"\nERROR: Command failed with exit code {e.returncode}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n\nBuild interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nERROR: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
