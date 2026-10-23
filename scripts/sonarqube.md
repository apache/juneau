# sonarqube.py

Shows SonarCloud findings (rule, severity, line, message) for a source file, a package directory, a Maven module, or the whole repository.

## When to use

- You want the live Sonar findings for the code you are changing, with filters by severity, rule or type.
- You want a repository-wide summary by module and rule, or a pass/fail gate on matching issues (`--fail-on-issues`).
- You want findings for a branch or pull request analysis instead of the default branch.

## Usage

```bash
python3 scripts/sonarqube.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/
python3 scripts/sonarqube.py juneau-core/juneau-marshall --severity BLOCKER,CRITICAL
python3 scripts/sonarqube.py path/to/File.java --with-suppress-hint
python3 scripts/sonarqube.py --all --run
python3 scripts/sonarqube.py --all --rule java:S3776 --detail
python3 scripts/sonarqube.py --all --pr 1234 --run --fail-on-issues
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `path` | file, directory or module (positional) | none | A `.java` file, a package directory under `src/main/java` or `src/test/java`, or a module root. Absolute or relative to the repository root. Omitting it selects whole-repository mode. |
| `--all` | none | implied without a path | Whole-repository mode: totals, a per-module table and the top rules. |
| `--run`, `-r` | none | off | Fetch the issues from SonarCloud again and overwrite the local cache. |
| `--severity` | SEV[,SEV...] | all | Filter by severity: `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`, `INFO`. Also accepted as `--severity=...`. |
| `--rule` | rule id | all | Filter by rule id, for example `java:S3776`. May be repeated. Also accepted as `--rule=...`. |
| `--type` | TYPE[,TYPE...] | all | Filter by type: `CODE_SMELL`, `BUG`, `VULNERABILITY`, `SECURITY_HOTSPOT`. Also accepted as `--type=...`. |
| `--branch` | name | `master` | SonarCloud branch to query. Cannot be combined with `--pr`. |
| `--pr` | id | none | SonarCloud pull-request analysis to query. Cannot be combined with `--branch`. |
| `--with-suppress-hint` | none | off | Append a `@SuppressWarnings` hint to each finding. |
| `--detail` | none | off | Whole-repository mode only: also print the per-file findings after the summary. |
| `--max` | N (integer) | 200 | Cap on the number of findings printed. |
| `--fail-on-issues` | none | off | Exit 2 if any issue remains after the filters. Output is unchanged. |
| `--help`, `-h` | none | off | Print the usage text and exit 0. Also printed when no argument is given. |

## What it does

1. Parses the options by hand and rejects unknown ones, a second positional, `--branch` together with `--pr`, and bad severity or type values (`parse_args`).
2. Reads the cache in `target/.sonar-issues.json` unless `--run` is given (`load_cache`).
3. With no cache, or with `--run`, fetches all issues for project `apache_juneau` in organization `apache` from the SonarCloud web API, 500 per page, up to a 5000 issue cap (`fetch_all_issues`).
4. For a non-default branch or pull request, first checks that SonarCloud has an analysis for it (`scope_analyzed`). If not, it warns and exits 3.
5. Applies the severity, rule and type filters, and the path filter (`filter_issues`, `filter_issues_all`).
6. Prints the findings for the path (`report`), or the totals, module table and top rules (`report_whole_repo`).

## Output and exit codes

Findings grouped by file, each with severity, rule, line and message. Counts by severity are summarized. A line says whether the cache was used and which branch or pull request it holds.

| Code | Meaning |
|---|---|
| 0 | Normal, or clean with `--fail-on-issues`, or help was shown. |
| 1 | An error: bad option or value, missing path, network or authorization failure. |
| 2 | `--fail-on-issues` was given and matching issues remain. |
| 3 | The requested branch or pull request has no SonarCloud analysis yet. It is a skip, not a pass. |

## Gotchas

- The cache holds one scope. If it was fetched for a different branch or pull request, the script prints a notice; use `--run` to refresh.
- It queries the public project anonymously. If `SONAR_TOKEN` is set, it is sent as a bearer token to raise the rate limit and is never printed. A 429 response suggests setting it.
- Findings come from the last CI analysis, so they lag local edits. A new branch or pull request has nothing to report until CI has analyzed it.
- `--detail` has an effect only in whole-repository mode.
- `--max` limits what is printed, not what `--fail-on-issues` counts.
- Network access to `sonarcloud.io` is required whenever the cache is missing or `--run` is used.

## See also

- `coverage.py`, the sibling tool this one mirrors
- `view-sonar-category.py`, the legacy offline viewer
