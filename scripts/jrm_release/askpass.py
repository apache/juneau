#!/usr/bin/env python3
"""GIT_ASKPASS helper: answers git's Username/Password prompts from the environment, so no secret is on any argv."""
import os
import sys

prompt = sys.argv[1] if len(sys.argv) > 1 else ""
sys.stdout.write(os.environ.get("JRM_ASKPASS_USER" if "sername" in prompt else "JRM_ASKPASS_PASS", ""))
