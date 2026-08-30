# Juneau Release Manager

> **Maintainer dev-tooling — NOT an Apache Juneau release artifact.**
>
> This branch (`release-manager`) is project-owned release-automation tooling for
> Apache Juneau maintainers. It is **not** an official Apache Juneau release
> artifact: it does **not** ride the release train, is **not** part of the signed
> source/binary distribution or the release assembly, is **not** published to
> Maven Central, and carries **no** ASF release guarantees. It lives on an orphan
> branch of `apache/juneau` (mirroring the `docs` branch) with no history from
> `master` and no CI/deploy pipeline.

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
git clone -b release-manager --single-branch https://github.com/apache/juneau.git juneau-release-manager

# ...or, from an existing apache/juneau checkout, add a worktree:
git worktree add ../release-manager release-manager
```

## Building / running

```bash
mvn clean verify        # build + run tests + apache-rat license check
./start.sh              # launch the web app
./stop.sh               # stop it
```

## License

Apache License 2.0. See `LICENSE` (which also reproduces the MIT attribution
notices for the vendored jQuery 3.7.1 and DataTables 2.1.8 assets under
`src/main/resources/static/datatables/`).
