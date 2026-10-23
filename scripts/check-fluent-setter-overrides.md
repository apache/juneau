# check-fluent-setter-overrides.py

Reports subclasses that do not override an inherited fluent setter, so the narrowed return type is lost in method chains.

## When to use

- You changed a builder or bean hierarchy and want to see which subclasses still return the parent type from an inherited setter.
- It is informational only; it never fails the build.

## Usage

```bash
python3 scripts/check-fluent-setter-overrides.py
```

## Options

None.

## What it does

1. Walks the repository root for `.java` files, skipping dot-directories, `target`, `node_modules`, `build`, `dist`, any directory whose path has a `test` or `juneau-integration-tests` component, and files ending in `Test.java` or `Tests.java` (`find_java_files`).
2. For each file, uses regular expressions to find top-level `public class X extends Y` declarations (`extract_class_info`).
3. In each class body, records the public methods that return the class's own type and contain `return this` within the next 500 characters (the fluent setters), and the methods marked `@Override`.
4. Groups the classes by name and, for each class with a parent found in the scan, looks for parent setters that the child neither overrides nor redefines (`check_missing_overrides`). Setters on a parent that carry `@BeanProp` are skipped.
5. Prints the findings grouped by child class.

## Output and exit codes

Progress lines with the file, class and setter counts, then either `MISSING OVERRIDES (n found)` with each child class, its file and the missing methods with their parent, or a line saying all setters are overridden.

| Code | Meaning |
|---|---|
| 0 | Always, with or without findings. |

## Gotchas

- The analysis is regular-expression based, not a Java parser. Inner classes are ignored, generics on `extends` are stripped, and a parent is matched by simple name, so name collisions across packages can give false results.
- Because any path component named `test` skips a directory, a checkout that lives under a directory called `test` scans nothing.
- It scans the whole tree and takes a while.

## See also

- Docs site: Developer Info, Script: check-fluent-setter-overrides (`04.02.09.ScriptCheckFluentSetterOverrides.md` in juneau-docs)
