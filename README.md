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

`scripts/release.py` drives a release (RC, vote, promote) from the terminal. Smoke-test it with a dry run, which
runs read-only commands, prints every mutation and writes no `runs/`:

```bash
JUNEAU_RUN_PY=~/git/apache/juneau/juneau-run/src/main/python/juneau_run.py \
  uv run --native-tls python scripts/release.py --dry-run --version 9.2.1
```

- **Prerequisites run first**, before any prompt (branch, details, credentials), on a new run and on `--resume` alike,
  dry run included. They check git, mvn (3.6+), java (17+), svn and svnmucc (1.10+), gpg (2.1+), gh and unzip on PATH,
  plus credential-free reachability: Nexus, and an anonymous `svn ls` of dist/dev. Every failure is listed at once with
  an install hint (for example `brew install subversion`), then release.py exits before asking anything. The
  `prechecks` step that follows keeps only what depends on your answers (branch exists, RC tag absent).
- The GPG key id prompt is prefilled from `gpg --list-secret-keys --with-colons` (read-only): the unexpired,
  signing-capable key whose uid matches your Apache email (or `<availid>@apache.org`). Several matches become a
  choice; none leaves the plain prompt. Outside a dry run it warns if the key is missing from the published KEYS file.
- The release branch is asked first. With `--version`, its default is an existing `origin/juneau-X-branch` or
  `origin/X.Y.x`, else `master`.
- `--version` prefills the Version in the details form and derives the next development version from it.
- Leave the development version blank to get the next patch; a bare `x.y.z` gets `-SNAPSHOT` appended.
- **Release diff review** is automatic: it checks the release commit (`<rc tag>^..<rc tag>`) and the plugin's following
  "prepare for next development iteration" commit (the tag's first descendant on the branch). Clean means every changed
  file is a reactor `pom.xml` with only the X-SNAPSHOT -> X version (and `juneau.version`/SCM tag) changes, no reactor
  pom was skipped, and the follow-up commit reverses them to the next SNAPSHOT; the step then passes with a one-line
  summary and no prompt. Anything else prints just the offending hunks and asks the usual question (`no` fails).
- A dry run logs to `~/.juneau-release-manager/attempts/dry-run/full.log` (the banner's `tail -f` target).

## License

Apache License 2.0. See `LICENSE`.
