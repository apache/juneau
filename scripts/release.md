# release.py

Drives an Apache Juneau release from a terminal: build and stage a release candidate, open the vote, then (once the vote passes) promote it.

## When to use

- You are the release manager cutting a release candidate, resuming after the vote, or dropping a candidate.
- To see where a release stands without changing anything: `--list-steps`.
- To rehearse: `--dry-run` runs read-only commands and prints every mutation.
- To run the Python tests for this script, use pytest (see `tests/README.md`); to push repository changes, use `push.md`.

## Usage

```bash
python3 scripts/release.py                       # start a new release (asks for the details)
python3 scripts/release.py --version 10.0.0      # start a new release, prefilling the details
python3 scripts/release.py --list-steps          # steps by phase, finished ones marked
python3 scripts/release.py --resume              # continue after the last finished step
python3 scripts/release.py --start-step vote-tally
python3 scripts/release.py --skip-step javadoc-verify
python3 scripts/release.py --revert              # undo the current release candidate
python3 scripts/release.py --dry-run
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--resume` | none | off | Continue the active run after its last finished step. Mutually exclusive with `--start-step`. |
| `--start-step` | `NAME` (one of the step names) | none | Start, or restart, at this step. Refused when the release is already public and the step is before `nexus-release`, when a promote step is chosen before the vote has passed, and when a vote step is chosen before `vote-gate` has passed. |
| `--skip-step` | `NAME` (repeatable) | none | Skip a step. Only skippable steps are accepted (currently `javadoc-verify`). |
| `--list-steps` | none | off | Print the steps by phase (Release candidate, Vote, Promote), with finished ones marked, and exit. |
| `--version` | `x.y.z` | none | With `--list-steps`, which run's marks to show; on a new release, prefills the details. Must look like `x.y.z`. |
| `--revert` | none | off | Undo the current release candidate. Cannot be combined with `--resume`, `--start-step` or `--skip-step`. |
| `--dry-run` | none | off | Run read-only commands; print every mutation instead of running it. State is kept in memory, and `run.json` is never written. |
| `--console` | `full`, `board`, `none` | toolkit default | Console view; the flag beats `JUNEAU_RUN_CONSOLE`. |
| `--home` | `DIR` | `$JUNEAU_RELEASE_HOME`, else `~/.juneau-release-manager` | State directory. |

`--attempt-dir` also exists but is hidden from the help (used by tests).

## What it does

1. Parses and validates the arguments (a bad combination exits 2). `--list-steps` prints the table and exits 0.
2. Looks for the active run under `<home>/runs/<version>/run.json` (the newest one with details that is not yet `released`). `--resume`, `--revert` and `--start-step` with no active run exit 2; starting a new release while one is in progress also exits 2 and tells you to use `--resume` or `--revert`.
3. Loads the `juneau_scripts` toolkit through `bootstrap.py` (needs `juneau-run` 10.0.0-SNAPSHOT in `~/.m2`; build Juneau first).
4. Creates an attempt directory `<home>/attempts/<YYYYMMDD-HHMMSS>-<NNN>/` with `full.log` (a dry run uses `attempts/dry-run`).
5. Runs the prerequisite checks before asking anything (tools and minimum versions: `git`, `mvn` 3.6, `java` 17, `svn` and `svnmucc` 1.10, `gpg` 2.1, `gh` 2.28, `unzip`). Every failure is listed together; nothing was asked or changed. Skipped for `--revert`.
6. For a new release, asks for the release details (version, development version, RC number, summary, highlights, known issues, acknowledgements) and starts a fresh `run.json`.
7. Takes a non-blocking lock (`.lock` in the run directory) and runs the steps in order, checkpointing `run.json` after each. A second `release.py` on the same release exits 1 with the holder's pid.
8. The 25 steps fall in three phases: release candidate (prerequisites through `vote-gate`), vote (`vote-tally`, result email) and promote (`nexus-release` through `finalize`). The run pauses at `vote-gate`; after the vote closes, run `release.py --resume`.
9. `--revert` runs six rows instead: confirm, drop the Nexus staging repository, remove the RC from dist/dev, delete the RC tag, reset the pom versions, record the revert. Each row checks remote state first, so a rerun is harmless.

## Output and exit codes

A board view with one row per step. State lives under the home directory: `runs/<version>/run.json` (the checkpoint) and `attempts/<stamp>-<NNN>/full.log` (the full log, whose `tail -f` line the banner prints). Secrets are masked in output and never put on a command line or in `run.json`.

| Code | Meaning |
|---|---|
| 0 | Finished, or paused (for example at the vote gate). |
| 1 | A step or prerequisite failed, another run holds the lock, `run.json` is corrupt, or the toolkit could not be loaded. |
| 2 | Usage error, or the requested action does not fit the active run. |
| 130 | Cancelled with Ctrl-C. |

## Gotchas

- Everything from `nexus-release` on is irreversible (the release is public). After that, `--start-step` before it is refused; continue from `dist-promote`.
- A corrupt `run.json` is never guessed at: the script stops and asks you to inspect or delete it.
- `JUNEAU_RELEASE_HOME` moves the state directory; `--home` overrides it. `JRM_RELEASE_NO_BELL` silences the completion bell.
- The Juneau checkout is expected at `~/git/apache/juneau`.
- The Apache id, password, GPG key id and passphrase, and GitHub token are asked for as needed. The password reaches `svn` over stdin, and git gets credentials through the `askpass.py` helper reading `JRM_ASKPASS_USER` and `JRM_ASKPASS_PASS`.

## See also

- `jrm_release/README.md` (the step implementations)
- `push.md`, `tests/README.md`
