# juneau-run

Build tooling, not a library dependency. It ships `juneau_run.py`, a stdlib-only Python script that wraps
Maven, pytest and Playwright runs: it prints run-protocol v1 markers (`##run`) when `RUN_MARKERS=1`, routes the
full and condensed output to the console and log files, and parses tool output into sub-steps and test counts.
The wire format is in [docs/run-protocol-v1.md](docs/run-protocol-v1.md).

The module is `pom` packaging. `mvn install` attaches the script as `org.apache.juneau:juneau-run:<version>:py`
and runs the unit tests (`python3 -m unittest discover -s src/test/python`). `-DskipTests` skips them.

## Using it from a push.py

Paste this into the adopting script. It loads the script at the Juneau version the repo already pins, and returns
`None` (after one stderr warning) when it can't, so the adopter then runs its commands as it does today.

```python
def load_juneau_run(version, cache_dir):
    """Returns the juneau_run module, or None (with one warning) if it can't be loaded."""
    import importlib.util, os, subprocess, sys
    path = os.environ.get("JUNEAU_RUN_PY")
    try:
        if not path:
            path = os.path.join(cache_dir, "juneau-run.py")
            if version.endswith("-SNAPSHOT") or not os.path.exists(path):
                subprocess.run(["mvn", "-q", "-nsu", "dependency:copy",
                    f"-Dartifact=org.apache.juneau:juneau-run:{version}:py",
                    f"-DoutputDirectory={cache_dir}", "-Dmdep.stripVersion=true"],
                    check=True, stdout=subprocess.DEVNULL)
        spec = importlib.util.spec_from_file_location("juneau_run", path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        sys.modules["juneau_run"] = mod
        return mod
    except Exception as e:
        print(f"warning: juneau-run unavailable ({e}); running commands without it", file=sys.stderr)
        return None
```

- `cache_dir` is the adopter's choice, for example `target/juneau-run/` under its repo.
- For a `-SNAPSHOT` pin the copy runs on every call, so a freshly installed tree is picked up. The copy is local.
- `JUNEAU_RUN_PY=/path/to/juneau_run.py` skips Maven entirely.

Typical use:

```python
juneau_run = load_juneau_run("10.0.0-SNAPSHOT", "target/juneau-run")
if juneau_run:
    result = juneau_run.run_tool(["mvn", "test", "-T1C"], "maven", "tests", "Tests")
```

Adopters whose stdout is captured by a consumer must use `console="full"` so the consumer's raw log stays
complete. Quiet Maven calls (`-q`) should use the `generic` parser.

## In this repository

`scripts/test.py` and `scripts/push.py` load the script straight from `juneau-run/src/main/python/` (no Maven involved).
- With `RUN_MARKERS=1` they print run-protocol markers: `push.py` owns the run (`run`, `done`, its own steps); `test.py` and anything it
  runs contribute steps only, because `run()` exports `JUNEAU_RUN_ACTIVE=1`.
- `test.py --console full|condensed|none --full-log PATH --condensed-log PATH` routes Maven output (the same settings can come from
  `JUNEAU_RUN_CONSOLE`, `JUNEAU_RUN_FULL_LOG`, `JUNEAU_RUN_CONDENSED_LOG`; an argument wins; relative paths resolve under `$RUN_ARTIFACTS`).
- `push.py --test-only` runs the build and test gates only (no identity check, commit, push or docs follow-up). It is not the same flag as
  `test.py --test-only`, which means "tests without the build".
- With markers off and no routing, both scripts behave exactly as before.

## Regenerating the test fixtures

The Maven fixtures in `src/test/python/fixtures/` are recorded output, not hand-written:
- `src/test/python/record-maven-fixtures.sh <scratch-dir> <fixtures-dir>` builds a throwaway 3-module project and records the five
  `maven-*-failure/success.log` files. Local paths are replaced with `/work/demo` and `/home/user`.
- `maven-juneau-T1C.log` is a trimmed slice of a real `mvn test -T1C` build of this repository (start of the run, a few Surefire
  class lines, the skipped-test summary and the Reactor Summary), with `/Users/<name>` replaced by `/home/user`.

They are plain `.log` files, which RAT excludes. If a Maven upgrade changes its output, re-record, then fix the parser tests that fail.
