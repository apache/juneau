# jrm_release

The step implementations behind `scripts/release.py`: one module per concern, imported by `release.py`, not run directly.

`release.py` owns the command line, the run lock and the session; this package owns what each step does. Steps are functions of one argument, `fn(ctx)`, returning an optional summary string, and are registered by name in a `FUNCS` map per phase. For usage see `../release.md`.

## Modules

| Module | Role |
|---|---|
| `inventory.py` | Constants (repository, Nexus and dist URLs, mailing lists), the table of 25 steps with their phase and whether they are skippable, and version helpers. |
| `ctx.py` | The `Ctx` object every step receives (arguments, home, runner, Nexus, SVN, run file, credentials) and `StepError`. |
| `runfile.py` | The `run.json` checkpoint (atomic writes, in-memory under `--dry-run`, refusal to read a corrupt file) and attempt directories. |
| `runner.py` | The one place a command runs; under `--dry-run` a mutating command is printed, never executed. Masks secrets. |
| `preflight.py` | The one registry of preconditions (tools, params, secrets present and valid, repo, release notes, remote state), run before a new release, by `--check`, and (tools and secrets only) on `--resume`. Results are `pass`, `fail` or `cant`; the Prerequisites and Release checks rows replay them. |
| `prereqs.py` | An adapter that shows the preflight `tools` results as the Prerequisites row. |
| `details.py` | Defaults and validation for the release details (pom defaults, branch default); the form is gone. |
| `rc.py`, `vote.py`, `promote.py` | The release-candidate, vote and promote phase steps. |
| `revert.py` | The `--revert` rows. |
| `diffcheck.py` | A pure checker for the release plugin's two commits; callers pass diff text in. |
| `emails.py` | Vote, result and announce email text and the send flow. |
| `nexus.py`, `svn.py`, `gpgkeys.py` | Nexus staging REST, `svn` and `svnmucc`, and parsing of `gpg --list-secret-keys`. |
| `params.py` | The params file: loading and validation (PyYAML, `safe_load` only), the `--init-params` template, and the `runs/<version>/params.yml` record copy. |
| `keychain.py` | Read-only macOS keychain and `gh auth token` lookups through `Runner.quiet` (never logged, 10 s timeout). |
| `askpass.py` | The `GIT_ASKPASS` helper: answers git's prompts from `JRM_ASKPASS_USER` and `JRM_ASKPASS_PASS` so no secret is on a command line. |

## Conventions

- Every external command goes through `Runner`, so `--dry-run` and secret masking apply uniformly.
- A step that fails raises `StepError`; its message is the row's failure text.
- A secret is resolved by `Ctx.cred`: cache, environment, keychain, `gh auth token` (GitHub token), then a prompt (or `StepError` under `--params`). Secrets are never written to `run.json`, the params record or a log; only the `identity` allow-list (Apache id, email, GPG key id) is persisted.
- Tests use fakes only (see `../tests/README.md`).

## See also

- `../release.md`
