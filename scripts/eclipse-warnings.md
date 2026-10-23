# eclipse-warnings.py

Reproduces Eclipse's compiler warnings for a module without a running Eclipse, by compiling its sources with the `ecj` compiler under the module's own preferences.

## When to use

- You need the same warnings Eclipse's Problems view would show, for one module or all of them, and no Eclipse session is available.
- You want a before-and-after check of a warning fix.

## Usage

```bash
python3 scripts/eclipse-warnings.py juneau-rest/juneau-rest-server
python3 scripts/eclipse-warnings.py --all
```

Run from the repository root.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `module-path` | path (positional) | none | Module directory relative to the repository root. Exactly one argument is required. |
| `--all` | none | off | Check every module that `apply-eclipse-prefs.py` would update, source and test modules. Used in place of `module-path`. |

## What it does

1. Requires exactly one argument; otherwise prints the usage text and exits 1 (`main`).
2. Locates the `ecj` 3.45.0 jar, `~/.m2/repository/org/eclipse/jdt/ecj/3.45.0/ecj-3.45.0.jar` unless `ECJ_JAR` is set, and exits 2 if it is missing (`find_ecj_jar`).
3. For `--all`, loads `apply-eclipse-prefs.py` and uses its module discovery (`load_apply_prefs_module`).
4. For each module, runs `mvn -q dependency:build-classpath` from inside the module directory to get its runtime and test classpath (`build_classpath`, 180 second limit).
5. Skips the module if it has no `.settings/org.eclipse.jdt.core.prefs` or no Java sources (`run_ecj`).
6. Runs `java -jar ecj.jar -properties <module prefs> -enableJavadoc -warn:+allJavadoc,invalidJavadoc,javadoc -cp <classpath> -d <temp dir> <sources>` over `src/main/java` and `src/test/java` (300 second limit), with the module's `target/classes` and `target/test-classes` appended to the classpath.
7. Prints ecj's output and its final `n problems (e errors, w warnings)` line (`summarize`).

## Output and exit codes

A banner per module, ecj's raw diagnostics, and a `>>> <module>: <summary>` line.

| Code | Meaning |
|---|---|
| 0 | No module reported problems. |
| 1 | A usage error, or at least one module's summary was not `0 errors, 0 warnings`. |
| 2 | The `ecj` jar was not found. |

## Gotchas

- `ECJ_JAR` overrides the jar location. `JAVA_HOME` is preferred over the first `java` on the `PATH`.
- The classpath uses installed jars from `~/.m2` for other reactor modules, not their current sources. A type that is new or renamed in an upstream module shows as "cannot be resolved" until that module is installed again.
- Javadoc diagnostics come from flags the script always passes, because the tracked preference files set no Javadoc options.
- Lines such as "invalid Class-Path header in manifest of jar file" come from third-party jars and can be ignored.
- It needs `mvn` and `java` on the `PATH`. A slow module can hit the time limits and raise an uncaught timeout.

## See also

- `apply-eclipse-prefs.py`
