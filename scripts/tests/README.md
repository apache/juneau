# Script tests

Tests for the Python scripts in `scripts/`. Run them from the repository root:

```
python3 -m pytest -q scripts/tests
```

`conftest.py` puts `scripts/` on `sys.path` and provides the `load_script` helper for scripts with dashes in their names.

`test_script_docs.py` is the script-docs checker. Every entry point under `scripts/` (a `*.sh` file, a `*.py` file with a top-level `if __name__ == "__main__":`, or an extensionless file with a shell or Python shebang) needs a `<name>.md` next to it with the standard sections (When to use, Usage, Options, What it does, Output and exit codes, Gotchas, See also), and every flag the script declares must appear in the doc's Options section. Exceptions go in `scripts/script-docs-exclude.txt`, one pattern per line with a mandatory `# reason`.

`test_check_fences.py` covers `check-code-fences.py`, `check-java-fences.py` and `code_fences.py`. Its Java test needs a JDK (`~/jdk/default/bin/java` or `java` on the path) and is skipped without one.

`test_check_topic_template.py`, `test_apply_renames.py` and `test_renumber_topics.py` cover `check-topic-template.py`, `apply-renames.py` and `renumber-topics.py`. They build small docs trees under pytest's `tmp_path` and pass them with `--docs-dir`, so they never touch the live pages.

`test_check_guide_snippets.py` covers `check-guide-snippets.py` (pointer resolution, region markers, normalization, the guide-page pointer rule).

`test_verify_docs.py` covers `verify-docs.py` check 16 (the Guides section) and the guide-link cases of check 11.
