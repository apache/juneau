# Juneau Release Manager

> **Maintainer dev-tooling — NOT an Apache Juneau release artifact.**
>
> This branch (`juneau-release-manager`, formerly `release-manager`) is
> project-owned release-automation tooling for Apache Juneau maintainers. It is
> **not** an official Apache Juneau release artifact: it does **not** ride the
> release train, is **not** part of the signed source/binary distribution or the
> release assembly, is **not** published to Maven Central, and carries **no** ASF
> release guarantees. It lives on an orphan branch of `apache/juneau` (mirroring
> the `docs` branch) with no history from `master` and no CI/deploy pipeline.

## What it is

A Juneau REST + FreeMarker View web application (with macOS Keychain credential
storage) that automates the Apache Juneau release process — preflight checks,
build/verify, staging, vote orchestration, and promotion.

## Dependency status

This tooling currently depends on `10.0.0-SNAPSHOT` of several Juneau modules
(`juneau-rest-server-springboot`, `juneau-rest-server-view-freemarker`,
`juneau-secret-macos-keychain`). The `<juneau.version>` property will be pinned to
`10.0.0` once Juneau 10.0 GA is published. The project's own version
(`1.0.0-SNAPSHOT`) is intentionally unreleased.

## Getting a working copy (committers)

This branch mirrors the `docs`-branch workflow — obtain a dedicated checkout of
just this orphan branch, sibling to your `master` checkout:

```bash
# Dedicated single-branch clone:
git clone -b juneau-release-manager --single-branch https://github.com/apache/juneau.git juneau-release-manager

# ...or, from an existing apache/juneau checkout, add a worktree:
git worktree add ../juneau-release-manager juneau-release-manager
```

## Building / running

```bash
mvn clean verify        # build + run tests + apache-rat license check
./start.sh              # launch the web app
./stop.sh               # stop it
```

## Release CLI

`scripts/release.py` drives a release (release candidate, vote, promote) from the terminal. It asks nothing at the
start: a params file holds the release details, and secrets come from the keychain or the environment. The full option
table, exit codes and step list are in [`scripts/release.md`](scripts/release.md).

### 1. Prerequisites

- The tools (git, mvn, java, svn, svnmucc, gpg, gh, unzip) and PyYAML (`python3 -m pip install pyyaml`). The `tools`
  group of `--check` (step 4) lists each one with its minimum version and an install hint.
- `juneau-terminal` 10.0.0-SNAPSHOT in `~/.m2`: build Juneau first (`mvn install` in `~/git/apache/juneau`, which is
  also where the release is cut).

### 2. One-time secret setup

Secrets never go in the params file. Store each once; the first two use your Apache id as the account, the token uses
`default`. Each command prompts for the value (`-w` with no argument), so it never reaches a command line or your
shell history:

```bash
security add-generic-password -a <apache id> -s jrm.apache.password -w
security add-generic-password -a <apache id> -s jrm.gpg.passphrase -w
security add-generic-password -a default -s jrm.github.token -w
```

Or export `JRM_APACHE_PASSWORD`, `JRM_GPG_PASSPHRASE` and `JRM_GITHUB_TOKEN` (they win over the keychain), or run
`gh auth login` instead of storing a token (needs the `repo` scope).

### 3. Write the params file

```bash
python3 scripts/release.py --init-params --version x.y.z
```

This writes `~/.juneau-release-manager/params/x.y.z.yml`, prefilled from `git config user.email` and your GPG keys.
Edit it: replace every `TODO`. `summary` is required; `highlights`, `knownIssues` and `acknowledgements` can be filled
in or deleted. Check `branch`, `developmentVersion`, `rc`, the Apache id and email, and the GPG key id. A leftover
`TODO` blocks the run (exit 2).

### 4. Check everything first

```bash
python3 scripts/release.py --check --version x.y.z
```

Read-only: it tests the tools, the params, that each secret exists and is accepted, the branch, RC tag, pom version
and release notes, and that no Nexus staging repository or dist/dev RC directory is left over. Each line is `PASS`,
`FAIL` or `???` (could not be checked); the exit code is 0 (all pass), 2 (a check failed) or 1 (none failed, but one
could not run). A real run does the same checks first and refuses to start unless they all pass.

### 5. Start the release

```bash
python3 scripts/release.py --version x.y.z
```

To watch it in a tmux pane (the board on top, the raw log below), extract the launcher once and run through it:

```bash
python3 -B ~/.m2/repository/org/apache/juneau/juneau-terminal/10.0.0-SNAPSHOT/juneau-terminal-10.0.0-SNAPSHOT.zip --bin watch-run.sh
~/.cache/juneau-terminal/bin/10.0.0-SNAPSHOT/watch-run.sh -d <this repo> -t "release x.y.z" -- python3 scripts/release.py --version x.y.z
```

### 6. Where it stops

The 25 steps run unattended except at these points:

| When | What you do | Then |
|---|---|---|
| Propose email | Send the `[PROPOSE]` mail to dev@ (it opens in your mail client) and confirm | "Not yet" pauses; `--resume` after sending |
| Javadoc verify | Skim the generated javadoc and confirm (`--skip-step javadoc-verify` skips it) | A "no" fails the step |
| Release diff review | Only when the release commits hold more than the pom version bumps: read the printed hunks and answer | A clean diff passes silently; "no" fails |
| Vote email | Send the `[VOTE]` mail to dev@ | The run then pauses at the vote gate and exits 0 |
| Vote gate | Wait for the vote to close | `--resume` |
| Tally form | Enter the +1, 0 and -1 counts and the binding voters (three binding +1 and more +1 than -1 passes) | Asked to confirm if the vote is still open |
| Result email | Send the `[RESULT]` mail | A failed vote pauses here: `--revert` |
| Maven Central confirmation | Type the version to confirm. **Irreversible** | Anything else stops the run |
| Announce email | Send it to announce@; the run first waits for downloads.apache.org to carry the release and offers to stop and resume later if the mirrors are slow | |
| Follow-up checklist | Do the listed chores and confirm | "No" pauses; `--resume` |

### 7. Picking up

- `--resume` continues after any pause, and once the vote closes. It needs no params file; the identity is saved in
  the run.
- `--list-steps` shows the steps by phase with finished ones marked; `--start-step NAME` restarts at a step (refused
  when it would repeat an irreversible one, or skip the vote).
- `--revert` drops a release candidate (Nexus staging repository, dist/dev files, RC tag, pom versions): use it after a
  failed vote, or before an RC2. Bump `rc` in the params file, then start again.
- Ctrl-C stops cleanly (exit 130); `--resume` continues. A second `release.py` on the same release is refused while the
  first holds the lock.

### Smoke test and diff review

A dry run runs read-only commands, prints every mutation and writes no `runs/`; it needs the params file but no secrets:

```bash
python3 scripts/release.py --dry-run --version 9.2.1
```

Its log is `~/.juneau-release-manager/attempts/dry-run/full.log`. Real runs log to
`~/.juneau-release-manager/attempts/<stamp>-<n>/full.log` (under `watch-run.sh`, that link points at the file the
bottom pane tails).

**Release diff review** is automatic: it checks the release commit (`<rc tag>^..<rc tag>`) and the plugin's following
"prepare for next development iteration" commit. Clean means every changed file is a reactor `pom.xml` with only the
X-SNAPSHOT -> X version (and `juneau.version`/SCM tag) changes, no reactor pom was skipped, and the follow-up commit
reverses them to the next SNAPSHOT. Anything else prints just the offending hunks and asks.

## Release page

The **Release** page (`/rest/runs`) runs `scripts/release.py` for you and streams its terminal. It starts
`python3 scripts/release.py --console board --home <rm.home> --attempt-dir ...`, answers the script's prompts
through dialogs, and lists past attempts (real runs and dry runs) under **History**, where each can be replayed.

- **Properties.** `rm.home` (default `~/.juneau-release-manager`) is where `runs/` and attempt directories live.
  `rm.scripts.dir` (default `<working dir>/scripts`) holds `release.py`. `rm.juneau.terminal.script` is the
  `juneau_terminal.py` the script runs under; it defaults to the `juneau-terminal` script in `~/.m2`, and the `JUNEAU_TERMINAL_PY`
  environment variable overrides it.
- **Credentials.** Stored credentials answer the script's `credential.*` prompts automatically; anything not stored is
  asked in a dialog.
- **Stopping JRM.** Stopping JRM stops a live run. It then shows as "interrupted (JRM stopped)" and can be resumed.

## License

Apache License 2.0. See `LICENSE`.
