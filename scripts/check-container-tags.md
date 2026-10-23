# check-container-tags.py

Fails when a test class that boots a Spring Boot, Jetty or similar container is missing its container marker annotation.

## When to use

- You added a test that starts a container and want to confirm it carries the right marker.
- A build gate reports an untagged container test. `push.py` runs this script before the tests.

## Usage

```bash
python3 scripts/check-container-tags.py
```

## Options

None.

## What it does

1. Finds every `src/test/java` directory under the repository root, skipping any path with a `target` component (`discover_test_roots`).
2. Reads each `*Test.java` file in those trees (`scan_counted`).
3. Decides that a file needs a tag when its source matches a Spring hint (`@SpringBootTest`, `@AutoConfigureMockMvc`, `SpringExtension`, `SpringApplication.run(`) or a Jetty hint (`JettyMicroservice.create/builder(`, `Microservice.create/builder(`, a `new org.eclipse.jetty...Server(`).
4. Counts the file as tagged when it contains any of the markers `@SpringbootTest`, `@JettyMicroserviceTest`, `@TomcatMicroserviceTest` or `@MicroserviceTest`, plain or fully qualified.
5. Lists the files that need a tag and have none.

## Output and exit codes

On success: `Container tag guard passed (<n> container tests tagged).` On failure, a list of offending paths relative to the root and a reminder of the four annotations.

| Code | Meaning |
|---|---|
| 0 | Every container-booting test has a marker. |
| 1 | At least one container-booting test has no marker. |

## Gotchas

- Detection is textual. A hint in a comment or string counts, and a marker in a comment counts as a tag.
- Only files named `*Test.java` are scanned.
- The Spring hints look for `@SpringBootTest` (capital B) while the marker annotation is `@SpringbootTest` (lower b); the two are different names.

## See also

- `check-bom-completeness.py`
- `push.py`, which runs this check before the tests
