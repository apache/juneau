# view-sonar-category.py

Legacy viewer for a locally saved, categorized SonarQube issue export; it does not talk to SonarCloud.

## When to use

- You still have the categorized JSON file from an older offline triage and want to browse one category.
- For live findings, use `sonarqube.py` instead; it queries SonarCloud and supports rule and severity filters.

## Usage

```bash
python3 scripts/view-sonar-category.py
python3 scripts/view-sonar-category.py "<category-name>"
python3 scripts/view-sonar-category.py "<category-name>" --limit 50
```

With no argument it lists the categories. Use a name exactly as the listing prints it.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `category-name` | text (optional positional) | none | The category to show. Omitted: print every category with its issue count, largest first. |
| `--limit` | N (integer) | 20 | How many sample issues to print. Honored only as the second argument after the category name (`<category> --limit N`). |

## What it does

1. Loads `SonarQubeIssues.categorized.json` from a fixed path in one user's Downloads folder (`main`).
2. With no category argument, prints the numbered category list and exits 0.
3. Otherwise looks the category up; an unknown name prints the sorted list of names and exits 1.
4. Prints the issue total, the number of affected files, the first N issues (description, file and location, id), and the 20 files with the most issues.

## Output and exit codes

All output goes to stdout.

| Code | Meaning |
|---|---|
| 0 | The category list or a category report was printed. |
| 1 | The JSON file is missing, or the category name is not in it. |

## Gotchas

- The JSON path is hard-coded to one user's Downloads folder, so the script does nothing useful elsewhere. The file was written by a categorizing script that is no longer in the repository.
- `--limit` is read by position. `--limit 50` before the category name, or any other layout, is ignored and the default of 20 applies; a non-integer value raises an uncaught `ValueError`.

## See also

- `sonarqube.py`
