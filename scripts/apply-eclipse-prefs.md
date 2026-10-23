# apply-eclipse-prefs.py

Copies the tracked Eclipse JDT preference files from `scripts/eclipse-preferences/` into the `.settings/` directory of every Maven module.

## When to use

- You added a module to the reactor, made a fresh checkout, or edited the canonical `.prefs` files.
- Never hand-edit a module's `.settings/` files. Edit the canonical files and run this script again.
- `eclipse-warnings.py` reads the copied preferences, so run this first if it reports them missing.

## Usage

```bash
python3 scripts/apply-eclipse-prefs.py
python3 scripts/apply-eclipse-prefs.py /path/to/another/checkout
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `root-dir` | path (optional positional) | repository root | Root of the checkout whose modules should be updated. Only `sys.argv[1]` is read. |

## What it does

1. Takes the root from the first argument, or the parent of `scripts/` (`main`).
2. Scans the reactor groups `juneau-bean`, `juneau-core`, `juneau-examples`, `juneau-microservice`, `juneau-petstore`, `juneau-rest`, `juneau-sc` and `juneau-shaded` (one level down), plus `juneau-integration-tests` and `juneau-secret-macos-keychain`, for directories with a `pom.xml` (`discover_projects`).
3. Skips poms that declare `<packaging>pom</packaging>` (`is_aggregator_pom`).
4. Classifies a module as a test module when it is named `juneau-integration-tests` or ends in `-ftest`; everything else is a source module (`is_test_module`).
5. Creates `.settings/` if needed and copies `org.eclipse.jdt.core.prefs` and `org.eclipse.jdt.ui.prefs` from `eclipse-preferences/source-prefs/` or `eclipse-preferences/test-prefs/` (`apply_preferences`).
6. Prints a line per module and a summary.

## Output and exit codes

`Preferences applied to <module>` per module, then a summary with total, updated and failed counts.

| Code | Meaning |
|---|---|
| 0 | Every module was updated. |
| 1 | At least one module failed, or a preferences directory or file was missing. |

## Gotchas

- The `.settings/` directories are ignored by git. The copies are local to your machine; only the files under `scripts/eclipse-preferences/` are tracked.
- Source modules get warnings on resource leaks and unused parameters; test modules get those relaxed.
- A new top-level reactor group is not discovered until it is added to `REACTOR_GROUPS` in the script. New modules inside existing groups are found automatically.
- Existing preference files in `.settings/` are overwritten.

## See also

- `eclipse-warnings.py`
- Docs site: Developer Info, Script: apply-eclipse-prefs (`04.02.07.ScriptApplyEclipsePrefs.md` in juneau-docs)
