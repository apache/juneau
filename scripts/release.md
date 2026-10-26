# release.py

Walks through the Apache Juneau release steps: checks, build, `release:prepare` and `release:perform`, binary artifacts, SVN upload and distribution check.

## When to use

- It is deprecated. At startup it prints a warning that Juneau releases now run from the separate release manager project, and that this script will be removed after the next release.
- Use it only to continue or revert a release that was started with it.

## Usage

```bash
python3 scripts/release.py --list-steps
python3 scripts/release.py
python3 scripts/release.py --resume
python3 scripts/release.py --start-step run_release_prepare --skip-step clean_maven_repo
python3 scripts/release.py --revert
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--list-steps` | none | off | List the step names in order and exit. |
| `--start-step` | step name | first step | Start at this step, skipping the ones before it. Mutually exclusive with `--resume`. |
| `--resume` | none | off | Start after the last step recorded in the checkpoint file. |
| `--skip-step` | step name | none | Skip this step. May be repeated. |
| `--revert` | none | off | Revert a release: delete the git tag, reset the Maven versions and remove the RC from SVN. |
| `--console` | `full`, `board`, `none` | `full` | Maven output on the console. Also `JUNEAU_TERMINAL_CONSOLE`; the flag wins. |
| `--detail` | `summary`, `actionable`, `modules`, `all` | `actionable` | Console detail. Also `JUNEAU_TERMINAL_DETAIL`. |

## What it does

The steps, in order (`RELEASE_STEPS`):

1. `check_prerequisites`: requires `wget`, `gpg`, `svn`, `git`, `java` and `mvn` on the `PATH`.
2. `check_java_version` and `check_maven_version`: Java 17 or newer, Maven 3 or newer.
3. `clean_maven_repo`: moves `~/.m2/repository` aside and deletes it in the background, only on the first run for a version.
4. `make_git_folder`: recreates the staging directory (default `~/tmp/dist-release-juneau`), deleting any existing one.
5. `clone_juneau` and `configure_git`: clones the project from the Apache git server into staging and sets the committer name and email.
6. `run_clean_verify`: `mvn clean verify`.
7. `run_deploy`: `mvn deploy` with MD5, SHA-1 and SHA-512 checksums.
8. `run_release_prepare`: requires a `-SNAPSHOT` version, then `mvn release:prepare` with the release version, tag and next development version.
9. `run_git_diff`: shows `git diff <tag>` in a pager so you can review what `release:prepare` changed.
10. `run_release_perform`: `mvn release:perform`, opens the Nexus staging page, and asks for the staging repository name after you close it by hand.
11. `create_binary_artifacts`: checks out the SVN dev area, downloads the source and binary zips from the staging repository, renames them to `apache-juneau-<version>-src.zip` and `-bin.zip`, and commits them under the release candidate name.
12. `verify_distribution`: checks the six expected files exist and are not empty, opens the SVN page, and prints a draft vote email.

Before the steps it prompts for the release candidate number, staging directory, Apache username and email, git branch and `JAVA_HOME`, with the last answers as defaults. `--revert` runs four steps: pull, delete the tag, reset the versions with `release:update-versions`, and remove the RC directories from SVN (`revert_release`).

## Output and exit codes

A configuration banner, then a row per step with the full output written to `~/.juneau-release-logs/<release>-<timestamp>.log`. After a failure it prints the command that resumes the run.

| Code | Meaning |
|---|---|
| 0 | Every selected step finished. |
| 1 | A step failed, or a prerequisite is missing. |
| 2 | An invalid option or step name. |
| 130 | Cancelled with Ctrl-C or Ctrl-D at a prompt, or the revert confirmation was declined. |

## Gotchas

- The checkpoint is in `~/.juneau-release-state.json` and the per-version answers in `release-history-<version>.json` beside the script. A corrupt checkpoint stops the run so a release never resumes from a guess.
- `release:perform` uploads to the public Apache staging repository, and the SVN steps commit to the public dev distribution area. Do not click Release in Nexus until the vote has passed.
- `make_git_folder` deletes the whole staging directory on every run that includes it. Use `--start-step` or `--resume` to avoid redoing it.
- `--revert` deletes the remote tag with `git push origin :<tag>` and can commit SVN deletions after asking.
- Steps need GPG and SVN credentials. The script primes the gpg agent first with `prompt-pgp-passphrase.py`.
- `TERMINAL_MARKERS` is ignored, with a warning.

## See also

- `prompt-pgp-passphrase.py`
- `current-release.py`
- `maven-version.py`
- Docs site: Developer Info, Script: release (`04.02.03.ScriptRelease.md` in juneau-docs)
