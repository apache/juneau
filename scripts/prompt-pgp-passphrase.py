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
Makes a throwaway `gpg --clearsign` call so the PGP passphrase is asked for (and cached by the agent) at the start of a long run, not at the end when signing is needed.

Full docs: prompt-pgp-passphrase.md
"""

import os
import shutil
import subprocess
import sys
import tempfile


# gpg probes for a smartcard on every signing run and prints this when none is present; it is not a signing failure.
_CARD_NOISE = "error retrieving key fingerprint from card"


def _resolve_gpg():
    """
    Resolve a usable path to the gpg binary.

    Tries, in order: a PATH lookup, the path configured via
    `git config --get gpg.program`, and a few common install locations
    (Homebrew on Apple Silicon/Intel, and the typical Linux location).
    Returns None if no usable gpg binary can be found.
    """
    found = shutil.which("gpg")
    if found:
        return found

    try:
        result = subprocess.run(
            ["git", "config", "--get", "gpg.program"],
            capture_output=True,
            text=True
        )
        configured = result.stdout.strip()
        if configured and os.path.isfile(configured) and os.access(configured, os.X_OK):
            return configured
    except (OSError, subprocess.SubprocessError):
        pass

    for candidate in ("/opt/homebrew/bin/gpg", "/usr/local/bin/gpg", "/usr/bin/gpg"):
        if os.path.isfile(candidate) and os.access(candidate, os.X_OK):
            return candidate

    return None


def prompt_pgp_passphrase():
    """
    Make a dummy PGP call to prompt for passphrase early in the execution.

    Returns False when gpg ran and exited non-zero; True otherwise, including timeout and gpg-missing.

    This ensures the user is prompted for their PGP passphrase at the beginning
    rather than waiting until signing is needed near the end of the process.
    """
    print("\n🔐 Prompting for PGP passphrase (dummy call)...")
    gpg_ok = True
    try:
        # Create a small dummy file to sign
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as tmp:
            tmp.write("dummy")
            tmp_path = tmp.name

        try:
            gpg_path = _resolve_gpg()
            if gpg_path is None:
                raise FileNotFoundError("gpg command not found")
            # Attempt to sign the dummy file (this will prompt for passphrase)
            # Don't use --batch so it will prompt interactively for passphrase
            # Use --yes to auto-confirm overwrite prompts, but allow passphrase prompt
            # stderr is piped only to drop gpg's smartcard-probe noise (see _CARD_NOISE); the pinentry prompt
            # goes through the tty, not stderr, and every other stderr line is passed on unchanged.
            result = subprocess.run(
                [gpg_path, "--yes", "--clearsign", tmp_path],
                stderr=subprocess.PIPE,
                text=True,
                timeout=60  # 60 second timeout for passphrase entry
            )
            for line in (result.stderr or "").splitlines():
                if _CARD_NOISE not in line:
                    print(line, file=sys.stderr)
            # Clean up the dummy file and signature
            try:
                os.unlink(tmp_path)
                if os.path.exists(tmp_path + ".asc"):
                    os.unlink(tmp_path + ".asc")
            except OSError:
                pass
            if result.returncode == 0:
                print("✅ PGP passphrase entered successfully")
            else:
                gpg_ok = False
                print(f"⚠ gpg exited with code {result.returncode} - the PGP passphrase was not accepted; "
                      "signing later in this run will probably fail")
        except subprocess.TimeoutExpired:
            print("⚠ PGP passphrase prompt timed out (this is okay if signing isn't needed)")
        except FileNotFoundError:
            print("⚠ gpg command not found - skipping PGP passphrase prompt")
        except Exception as e:
            # If signing fails for any reason, that's okay - we're just trying to prompt early
            print(f"⚠ Could not prompt for PGP passphrase: {e}")
    except Exception as e:
        print(f"⚠ Could not set up PGP passphrase prompt: {e}")
    # Best-effort: the callers (push.py, release.py) treat a non-zero exit as a warning only and never stop the build.
    return gpg_ok


def main():
    """Main entry point."""
    success = prompt_pgp_passphrase()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()

