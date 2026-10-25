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
Shared lookup for the Juneau source checkout that the docs scripts run against.

Used by build-docs.py, check-topic-links.py, check-guide-snippets.py and create-mvn-site.py
so they all resolve the checkout the same way.
"""

import os
import sys
from pathlib import Path

ENV_VAR = 'JUNEAU_ROOT'


def candidate_roots(docs_dir):
    """Sibling checkouts tried, in order, when nothing is given explicitly."""
    parent_dir = Path(docs_dir).resolve().parent
    return [parent_dir / 'master', parent_dir / 'juneau']


def find_juneau_root(docs_dir, juneau_root_arg=None, allow_missing=False, validate=True):
    """
    Resolve the Juneau source checkout.

    Order: --juneau-root, then the JUNEAU_ROOT environment variable, then the legacy
    sibling layout (../master next to the docs folder), then ../juneau next to the
    docs folder.

    Args:
        docs_dir: Path to the docs checkout root
        juneau_root_arg: Path from --juneau-root, or None
        allow_missing: If True (e.g. during --dry-run or --skip-maven), return the
            expected ../master path when no sibling checkout is found instead of
            aborting.
        validate: If False (e.g. during --dry-run), an explicit path is returned
            without checking that it is a Juneau checkout.

    Returns:
        Path to the Juneau source checkout

    Raises:
        SystemExit: If an explicit path is not a Juneau checkout, or no checkout can
            be found and allow_missing is False.
    """
    explicit, source = juneau_root_arg, '--juneau-root'
    if explicit is None and os.environ.get(ENV_VAR):
        explicit, source = Path(os.environ[ENV_VAR]).expanduser(), ENV_VAR
    if explicit is not None:
        juneau_root = Path(explicit).resolve()
        if validate and not (juneau_root / 'pom.xml').is_file():
            print(f"ERROR: {source} {juneau_root} is not a Juneau checkout (no pom.xml)")
            sys.exit(1)
        return juneau_root

    candidates = candidate_roots(docs_dir)
    for candidate in candidates:
        if candidate.is_dir() and (candidate / '.git').exists():
            return candidate

    if allow_missing:
        return candidates[0]

    print("ERROR: Could not locate the Juneau source checkout.")
    print("Looked for:")
    for candidate in candidates:
        print(f"  {candidate}")
    print(f"\nPass --juneau-root /path/to/juneau (or set {ENV_VAR}) to specify it explicitly.")
    sys.exit(1)
