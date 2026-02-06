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
Prompt for PGP Passphrase Script

This script makes a dummy PGP call to prompt for passphrase early in the execution.
This ensures the user is prompted for their PGP passphrase at the beginning
rather than waiting until signing is needed near the end of the process.

Usage: python3 scripts/prompt-pgp-passphrase.py
"""

import os
import subprocess
import sys
import tempfile


def prompt_pgp_passphrase():
    """
    Make a dummy PGP call to prompt for passphrase early in the execution.
    
    This ensures the user is prompted for their PGP passphrase at the beginning
    rather than waiting until signing is needed near the end of the process.
    """
    print("\n🔐 Prompting for PGP passphrase (dummy call)...")
    try:
        # Create a small dummy file to sign
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as tmp:
            tmp.write("dummy")
            tmp_path = tmp.name
        
        try:
            # Attempt to sign the dummy file (this will prompt for passphrase)
            # Don't use --batch so it will prompt interactively for passphrase
            # Use --yes to auto-confirm overwrite prompts, but allow passphrase prompt
            subprocess.run(
                ["gpg", "--yes", "--clearsign", tmp_path],
                capture_output=False,  # Don't capture output so user can see the prompt
                text=True,
                timeout=60  # 60 second timeout for passphrase entry
            )
            # Clean up the dummy file and signature
            try:
                os.unlink(tmp_path)
                if os.path.exists(tmp_path + ".asc"):
                    os.unlink(tmp_path + ".asc")
            except OSError:
                pass
            print("✅ PGP passphrase entered successfully")
            return True
        except subprocess.TimeoutExpired:
            print("⚠ PGP passphrase prompt timed out (this is okay if signing isn't needed)")
            return True
        except FileNotFoundError:
            print("⚠ gpg command not found - skipping PGP passphrase prompt")
            return True
        except Exception as e:
            # If signing fails for any reason, that's okay - we're just trying to prompt early
            print(f"⚠ Could not prompt for PGP passphrase: {e}")
            return True
    except Exception as e:
        print(f"⚠ Could not set up PGP passphrase prompt: {e}")
        return True


def main():
    """Main entry point."""
    success = prompt_pgp_passphrase()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()

