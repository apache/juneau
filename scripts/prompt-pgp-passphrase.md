# prompt-pgp-passphrase.py

Makes a throwaway `gpg --clearsign` call so the PGP passphrase is asked for (and cached by the agent) at the start of a long run, not at the end when signing is needed.

## When to use

- You are about to start a long build or release and want the passphrase prompt out of the way first. `push.py` and `release.py` call it for you.
- To check that your signing key and passphrase work, run it by hand.

## Usage

```bash
python3 scripts/prompt-pgp-passphrase.py
```

## Options

None.

## What it does

1. Prints a prompt banner and writes a small temporary text file containing `dummy` (`prompt_pgp_passphrase`).
2. Finds `gpg`: first on the `PATH`, then the program named by `git config gpg.program`, then `/opt/homebrew/bin/gpg`, `/usr/local/bin/gpg` and `/usr/bin/gpg` (`_resolve_gpg`).
3. Runs `gpg --yes --clearsign <tmpfile>` with a 60 second timeout. It is not run in batch mode, so gpg asks for the passphrase on the terminal.
4. Passes gpg's stderr through, dropping the harmless "error retrieving key fingerprint from card" line.
5. Deletes the temporary file and its `.asc` signature.
6. Prints a success line, or a warning that gpg exited non-zero.

## Output and exit codes

Status lines on stdout, gpg's remaining stderr on stderr.

| Code | Meaning |
|---|---|
| 0 | gpg signed the file, or the step was skipped harmlessly (gpg not found, timeout, or another setup error). |
| 1 | gpg ran and exited non-zero, for example a wrong passphrase. |

## Gotchas

- It is best-effort. `push.py` and `release.py` treat a non-zero exit as a warning only and carry on.
- A missing `gpg` or a timeout still exits 0, so exit 0 does not prove the passphrase was accepted.
- It needs an interactive terminal for the passphrase prompt.

## See also

- `push.py`, `release.py`
- Docs site: Developer Info, Script: prompt-pgp-passphrase (`04.02.04.ScriptPromptPgpPassphrase.md` in juneau-docs)
