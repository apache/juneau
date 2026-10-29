# release.py

Drives an Apache Juneau release from a terminal: build and stage a release candidate, open the vote, then (once the vote passes) promote it.

## When to use

- You are the release manager cutting a release candidate, resuming after the vote, or dropping a candidate.
- To see where a release stands without changing anything: `--list-steps`.
- To start a release: `--init-params` writes a params template, you edit it, `--check` verifies every precondition, then `--version x.y.z` (or `--params FILE`) runs it. A new release asks nothing: it reads the params file and takes secrets from the environment, the keychain or `gh`.
- To find out, before starting, whether a release would get through its preconditions: `--check` (read-only).
- To rehearse: `--dry-run` runs read-only commands and prints every mutation.
- To run the Python tests for this script, use pytest (see `tests/README.md`); to push repository changes, use `push.md`.

## Usage

```bash
python3 scripts/release.py --version 10.0.0      # start a new release from <home>/params/10.0.0.yml
python3 scripts/release.py --check --version 9.2.1   # preflight only: print the table, change nothing
python3 scripts/release.py --list-steps          # steps by phase, finished ones marked
python3 scripts/release.py --resume              # continue after the last finished step
python3 scripts/release.py --start-step vote-tally
python3 scripts/release.py --skip-step javadoc-verify
python3 scripts/release.py --revert              # undo the current release candidate
python3 scripts/release.py --dry-run
python3 scripts/release.py --init-params --version 9.2.1   # write ~/.juneau-release-manager/params/9.2.1.yml
python3 scripts/release.py --params ~/.juneau-release-manager/params/9.2.1.yml
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--resume` | none | off | Continue the active run after its last finished step. Mutually exclusive with `--start-step`. |
| `--start-step` | `NAME` (one of the step names) | none | Start, or restart, at this step. Refused when the release is already public and the step is before `nexus-release`, when a promote step is chosen before the vote has passed, and when a vote step is chosen before `vote-gate` has passed. |
| `--skip-step` | `NAME` (repeatable) | none | Skip a step. Only skippable steps are accepted (currently `javadoc-verify`). |
| `--list-steps` | none | off | Print the steps by phase (Release candidate, Vote, Promote), with finished ones marked, and exit. |
| `--version` | `x.y.z` | none | With `--list-steps`, which run's marks to show; on a new release or `--check`, selects `<home>/params/<version>.yml` and must match the version in the file. Must look like `x.y.z`. |
| `--revert` | none | off | Undo the current release candidate. Cannot be combined with `--resume`, `--start-step` or `--skip-step`. |
| `--dry-run` | none | off | Run read-only commands; print every mutation instead of running it. State is kept in memory, and `run.json` is never written. |
| `--params` | `FILE` | none | The YAML params file a new release (or `--check`) reads. Needs PyYAML. With `--version`, the version must match the file. Cannot be combined with `--resume`, `--revert`, `--start-step` or `--list-steps`. Without it, `<home>/params/<version>.yml` is used when `--version` is given. A new release with neither a file nor `--version` exits 2 and points to `--init-params`. With `--init-params`, the path the template is written to. |
| `--check` | none | off | Run the whole preflight (groups `tools`, `params`, `secrets-present`, `secrets-valid`, `repo`, `notes`, `remote`) for a new release, print one table with a reason per check and a summary line, and exit. Changes nothing: no `run.json`, no attempt directory. Exit 0 when every check passes, 2 when any failed, 1 when none failed but one could not be run. Cannot be combined with `--resume`, `--revert`, `--start-step`, `--init-params`, `--dry-run`, `--list-steps` or `--skip-step` (exit 2). |
| `--init-params` | none | off | Write a commented params template (prefilled from `git config user.email` and `gpg --list-secret-keys`) to `<home>/params/<version>.yml`, or to `--params FILE`, and exit 0. Needs `--version`. Never overwrites an existing file (exit 2). Cannot be combined with `--resume`, `--revert`, `--start-step`, `--list-steps` or `--dry-run`. |
| `--console` | `full`, `board`, `none` | toolkit default | Console view; the flag beats `JUNEAU_TERMINAL_CONSOLE`. |
| `--home` | `DIR` | `$JUNEAU_RELEASE_HOME`, else `~/.juneau-release-manager` | State directory. |

`--attempt-dir` also exists but is hidden from the help (used by tests).

## Params file

A params file holds the release details and your non-secret identity; it never holds a secret. Keys: `branch`, `version`, `developmentVersion`, `rc`, `summary`, `highlights`, `knownIssues`, `acknowledgements`, `apache.availid`, `apache.email`, `gpg.keyid`, and, after the vote closes, `vote:` (`plus1`, `zero`, `minus1`, `binding`). `--init-params` writes `vote:` as a commented block at the end of the template; leave it commented out until the vote is over. Unknown keys, a key whose name looks like a secret (`password`, `passphrase`, `token`, `secret`, at any depth) and a leftover `TODO` placeholder are errors (exit 2); an invalid file never falls back to the form.

Secrets come from the environment (`JRM_APACHE_PASSWORD`, `JRM_GPG_PASSPHRASE`, `JRM_GITHUB_TOKEN`; an empty value counts as unset), then on macOS from the keychain (`security find-generic-password -s jrm.apache.password -a <availid> -w`, likewise `jrm.gpg.passphrase`; the GitHub token is `-s jrm.github.token -a default`), then for the GitHub token from `gh auth token`. Nothing is ever asked for: a new release runs the preflight first, and any secret that is missing or rejected is listed together with every other problem before anything is created (exit 2). The Apache email defaults to `<availid>@apache.org`. A dry run skips the secret checks and reads no secret store.

The file is copied to `<home>/runs/<version>/params.yml` as a record (a copy failure is only a warning), and the Apache id, email and GPG key id are saved in `run.json` as `identity`, so `--resume` does not need the file. `--resume` still needs no params file to continue, except that `vote-tally` reads the `vote:` block from the file recorded in `run.json` as `paramsPath` (else `<home>/params/<version>.yml`).

## Preflight

`jrm_release/preflight.py` holds one list of checks in seven groups. A new release runs all of them before it creates `run.json`; `--check` runs the same list and only prints it; `--resume` runs `tools`, `secrets-present` and `secrets-valid`. Every check runs (nothing short-circuits) and a check that raises counts as could-not-run. A real run refuses to start on any failure and on any check that could not run, and lists all of them.

| Group | Checks |
|---|---|
| `tools` | each tool and its minimum version (`git`, `mvn`, `java`, `svn`, `svnmucc`, `gpg`, `gh`, `unzip`), PyYAML, Nexus reachable, dist/dev reachable, gpg-agent cache TTL (`default-cache-ttl` >= 1800 and `max-cache-ttl` >= 7200 from `gpgconf --list-options gpg-agent`, so the passphrase survives from `gpg-unlock` to `release-perform`; `python3 scripts/gpg-agent-ttl.py` fixes a failure) |
| `params` | the params file loads and validates, `--version` matches it, the details resolve, the `vote:` block (if any) has the right types |
| `secrets-present` | each secret has a source (env, keychain, `gh`); only the source is reported, never a value |
| `secrets-valid` | the Apache password is accepted by Nexus (HTTP 401/403 is a failure), the GPG passphrase signs a throwaway blob (passphrase on stdin), the GPG key has not expired, the GitHub token works and has the `repo` scope |
| `repo` | the branch exists on origin, the RC tag is absent, the branch tip's pom is `<version>-SNAPSHOT`, the GPG key is in the published KEYS file |
| `notes` | `RELEASE-NOTES.txt` at the branch tip has a `Release Notes - Juneau - Version <version>` heading |
| `remote` | no open Nexus staging repository, no RC directory on dist/dev |

Under `--dry-run` the secret checks report `skipped (dry run)`. The branch tip is read by a depth-1 fetch into a throwaway bare repository, which is removed afterwards; the Juneau checkout is not touched.

## What it does

1. Parses and validates the arguments (a bad combination exits 2). `--list-steps` prints the table and exits 0. `--check` runs the preflight, prints the table and exits.
2. Looks for the active run under `<home>/runs/<version>/run.json` (the newest one with details that is not yet `released`). `--resume`, `--revert` and `--start-step` with no active run exit 2; starting a new release while one is in progress also exits 2 and tells you to use `--resume` or `--revert`.
3. Loads the `juneau_terminal_scripts` toolkit through `bootstrap.py` (needs `juneau-terminal` 10.0.0-SNAPSHOT in `~/.m2`; build Juneau first).
4. Creates an attempt directory `<home>/attempts/<YYYYMMDD-HHMMSS>-<NNN>/` with `full.log` (a dry run uses `attempts/dry-run`).
5. For a new release, reads the params file (`--params`, else `<home>/params/<version>.yml`; none is an error) and runs the whole preflight before anything is created or asked. Every problem is listed together. A resume runs the `tools` and secrets groups. Skipped for `--revert`.
6. Starts a fresh `run.json` from the resolved details and records the params file.
7. Takes a non-blocking lock (`.lock` in the run directory) and runs the steps in order, checkpointing `run.json` after each. A second `release.py` on the same release exits 1 with the holder's pid.
8. The 25 steps fall in three phases: release candidate (prerequisites through `vote-gate`), vote (`vote-tally`, result email) and promote (`nexus-release` through `finalize`). The run never asks a question. Where it needs you it pauses (exit 0), prints what to do and the command to continue, and records the pause in `run.json`; see Pauses.
9. `--revert` runs five rows instead: drop the Nexus staging repository, remove the RC from dist/dev, delete the RC tag, restore the poms the release plugin changed (then verify the root version), record the revert. The command is the confirmation: there is no typed confirmation on the CLI (the Release page still types `RC<n>` before it drops an RC). Each row checks remote state first, so a rerun is harmless.

## Pauses

A pause ends the attempt with exit 0 and status `paused`. `run.json` records `pause` (`step`, `message`, `at`); `--resume` clears it and continues. An email step writes the mail to `<home>/runs/<version>/emails/<kind>.eml.txt`, prints the same text, records `emailPending: "<kind>"` and pauses.

| Pause | What you do | How you continue |
|---|---|---|
| `propose`, `vote`, `result`, `announce` email | Send the mail from `emails/<kind>.eml.txt` | `release.py --resume` |
| `vote-gate` | Wait for the vote to close | `release.py --resume` |
| `vote-tally`, vote still open | Wait for the deadline in `voteDeadline` | `release.py --resume` after it |
| `vote-tally`, no valid `vote:` block | Fill in `vote:` in the params file | `release.py --resume` |
| `compose-announce-email`, release not on the mirrors | Wait | `release.py --resume` later; each resume waits a fresh 30 minutes |

Details:

- A plain `--resume` after an unsent mail records it as sent, so send it first. `--start-step` at an email step writes the mail again.
- The Javadoc check and the follow-up checklist are files: `javadoc-report.txt` and `followup.md` in the run directory. A release diff that is not clean fails the step (`release-diff.txt` lists it): run `--revert` and bump `rc`.
- There is no confirmation before the release goes to Maven Central; the passed vote is the gate.
- A dry run shows each mail and then goes on; it writes no mail file and no `run.json`, and a step that would pause is reported as skipped.

## Output and exit codes

A board view with one row per step. State lives under the home directory: `runs/<version>/run.json` (the checkpoint) and `attempts/<stamp>-<NNN>/full.log` (the full log, whose `tail -f` line the banner prints). When `JUNEAU_TERMINAL_FULL_LOG` is already set on entry (as `watch-run.sh` does, tailing that file in its lower pane), the session writes there instead and `attempts/<stamp>-<NNN>/full.log` becomes a symlink to it; if the link cannot be made, a warning is printed and the run logs to `full.log` as usual. Secrets are masked in output and never put on a command line or in `run.json`.

| Code | Meaning |
|---|---|
| 0 | Finished, or paused (an email to send, the vote gate, the tally; see Pauses). |
| 1 | A step failed, another run holds the lock, `run.json` is corrupt, the toolkit could not be loaded, or (preflight, `--check`) no check failed but one could not be run. |
| 2 | Usage error, no params file or an invalid one, any failed preflight check (including a missing or rejected secret), or the requested action does not fit the active run. |
| 130 | Cancelled with Ctrl-C. |

## Board

Each step is a row named `NN Name`, with NN the `--start-step` number zero-padded (`05 Build verify`, `16 Vote wait (72h)`); the Setup row is unnumbered. A command a step runs is a child of that step's row, not a row of its own: a step with one command shows that command as the step row.

The steps that pause the run (the four emails, the vote gate, the tally) have a divider after them, such as `send proposal email`. Each divider that is still ahead reads `run N stops here`, counted in plan order: the first is this run's stop (N is the attempt number), the next is run N+1, and so on. The run ends at its divider with exit 0, and `--resume` starts the next run after it. Dividers are drawn passed (`run ended`, no number) once crossed and dim while they are still ahead.

On a toolkit that offers them, the board also uses a count bar on steps that count files or artifacts, a reactor bar on the Maven rows (a module with no tests counts as pass or skip), and Juneau's push history to estimate unit-test progress. JRM reads `run.BOARD_FEATURES` (`dividers`, `history-alias`, `reactor-bar`, `count`) and skips any it lacks, so an older toolkit still runs the release with the plain board.

Push order: Juneau first (the toolkit), then JRM.

## Gotchas

- Everything from `nexus-release` on is irreversible (the release is public). After that, `--start-step` before it is refused; continue from `dist-promote`.
- A corrupt `run.json` is never guessed at: the script stops and asks you to inspect or delete it.
- `JUNEAU_RELEASE_HOME` moves the state directory; `--home` overrides it. `JRM_RELEASE_NO_BELL` silences the completion bell.
- Under `watch-run.sh` the banner's `tail -f` line names the file the wrapper passed in `JUNEAU_TERMINAL_FULL_LOG`, not an attempt-directory path; `attempts/.../full.log` is a symlink to it. The web Release page does not set that variable, so it is unaffected.
- The Juneau checkout is expected at `~/git/apache/juneau`.
- The Apache id, email and GPG key id come from the params file (or `run.json` on a resume); the password, passphrase and GitHub token from the environment, the keychain or `gh`. Nothing is asked for. The password reaches `svn` over stdin, and git gets credentials through the `askpass.py` helper reading `JRM_ASKPASS_USER` and `JRM_ASKPASS_PASS`.

## See also

- `jrm_release/README.md` (the step implementations)
- `push.md`, `tests/README.md`
