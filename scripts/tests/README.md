# Script tests

Tests for the Python scripts in `scripts/`. Run them from the repository root:

```
python3 -m pytest -q scripts/tests
```

`conftest.py` puts `scripts/` on `sys.path` and provides the `load_script` helper for scripts with dashes in their names.

`test_script_docs.py` is the script-docs checker. Every entry point under `scripts/` (a `*.sh` file, a `*.py` file with a top-level `if __name__ == "__main__":`, or an extensionless file with a shell or Python shebang) needs a `<name>.md` next to it with the standard sections (When to use, Usage, Options, What it does, Output and exit codes, Gotchas, See also), and every flag the script declares must appear in the doc's Options section. Exceptions go in `scripts/script-docs-exclude.txt`, one pattern per line with a mandatory `# reason`.
