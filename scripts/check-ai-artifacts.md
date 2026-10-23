# check-ai-artifacts.py

Read-only check that the generated AI artifacts (`static/llms.txt`, `static/ai/`) are fresh and that a few release-related facts agree across the docs and the Juneau checkout.

## When to use

- After editing pages or release notes, to confirm the AI artifacts were regenerated.
- As part of a full build: `build-docs.py` runs it in its Links stage.
- To regenerate the artifacts rather than check them, use `generate-ai-artifacts.py`.

## Usage

```bash
python3 scripts/check-ai-artifacts.py
python3 scripts/check-ai-artifacts.py --juneau-root ~/git/apache/juneau
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--docs-root` | directory | parent of the `scripts/` folder | The docs checkout to check. |
| `--juneau-root` | directory | `../juneau` next to the docs root | Juneau source checkout. It must contain `scripts/current-release.py`. Pass it explicitly for worktrees or other layouts; the `JUNEAU_ROOT` variable is not consulted by this script. |

## What it does

1. Resolves both roots; exits 1 right away if the Juneau checkout has no `scripts/current-release.py`.
2. Gets the current release by running that script in the Juneau checkout (`resolve_current_release`).
3. Runs each check and collects every error (`main`):
   - freshness: runs `generate-ai-artifacts.py --check` and fails if it does (`check_ai_artifact_freshness`);
   - `static/ai/manifest.json` exists and has `schema_version`, `version`, `generated_at`, `source_commit` and `record_count` (`check_manifest_shape`);
   - `static/llms.txt` exists and starts with `# Apache Juneau` (`check_llms_txt_shape`);
   - the Maven `<version>` in `README.md` equals the current release (`check_readme_version`);
   - every javadocs or apidocs `pathname:///` link in `docusaurus.config.ts` is the same URL (`check_javadocs_link_consistency`);
   - `pages/release-notes/<release>.md` exists, and so does `RELEASE-NOTES.txt` in the Juneau checkout (`check_release_note_source_presence`).
4. Prints the failures, or a success line.

## Output and exit codes

Stdout: `All AI documentation checks passed.`, or `AI documentation checks failed:` followed by one `- ` line per error. Nothing is written.

| Code | Meaning |
|---|---|
| 0 | All checks passed. |
| 1 | At least one check failed, or the Juneau checkout was not found. |

## Gotchas

- Missing `README.md`, `docusaurus.config.ts` or an unreadable manifest raise an exception rather than a tidy failure line.
- The freshness check runs another script in a subprocess with the current Python interpreter and relative path `scripts/generate-ai-artifacts.py`, so it expects a normal docs checkout layout.
- A failure of the release lookup (`current-release.py` exits non-zero) raises `RuntimeError` with its stderr.

## See also

- `generate-ai-artifacts.py`, `build-docs.py`
- Docs site: Developer Info, Script: AI artifacts (`pages/developer-info/04.03.07.ScriptAiArtifacts.md`)
