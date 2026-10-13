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
Per-package Java coverage report for the Juneau Release Manager.  The implementation is Juneau's
juneau_scripts.coverage, loaded by the vendored scripts/bootstrap.py; this file only supplies this repository's settings.

    ./scripts/coverage.py --help
"""

import sys
from pathlib import Path

import bootstrap

REPO_ROOT = Path(__file__).resolve().parent.parent


def main():
    bootstrap.load(REPO_ROOT / "pom.xml", run=False, cache_dir=REPO_ROOT / "target" / "juneau-run")
    try:
        from juneau_scripts import coverage
    except ImportError:
        sys.exit("ERROR: the installed juneau-run predates juneau_scripts.coverage. Refresh it by building or installing "
                 "juneau-run from apache/juneau, or set JUNEAU_SCRIPTS_ZIP to a current juneau-run.")
    coverage.main(coverage.Config(
        repo_root=REPO_ROOT,
        base_pkg="org/apache/juneau/releng",
        report_name="Release Manager coverage",
        title="the Juneau Release Manager",
        scope="""The Release Manager is a single Maven module, so "module" here means a top-level package
under org.apache.juneau.releng (sub-packages such as engine/steps roll up into their
top-level package; classes directly in org.apache.juneau.releng are reported as "(root)").""",
        package_example="engine",
        mvn_args=("-Drat.skip=true",),
    ), sys.argv[1:])


if __name__ == "__main__":
    main()
