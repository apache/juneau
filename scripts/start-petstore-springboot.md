# start-petstore-springboot.py

Starts the Juneau petstore sample application on Spring Boot, through the Spring Boot Maven plugin.

## When to use

- You want the Spring Boot flavor of the petstore running locally on its default port, 5000.
- For the Jetty flavor, use `start-petstore-jetty.py`.

## Usage

```bash
python3 scripts/start-petstore-springboot.py
```

Stop the server with Ctrl+C.

## Options

None.

## What it does

1. Finds the project root by walking up from `scripts/` to the first directory with a `pom.xml` (`find_project_root`).
2. Requires `juneau-petstore/juneau-petstore-springboot` under it (`main`).
3. If that module has no `target/classes`, runs `mvn clean compile` there first.
4. Runs `mvn spring-boot:run` in the module with its output attached to your terminal. The main class is `org.apache.juneau.petstore.springboot.App`.

## Output and exit codes

Colored status lines, then the Maven and Spring Boot output. The banner shows `http://localhost:5000` as the default address.

| Code | Meaning |
|---|---|
| 0 | Stopped with Ctrl+C. |
| 1 | The project root or module directory was not found, the build failed, or the launch raised an error. |
| other | The exit code of `mvn spring-boot:run` when it ends on its own. |

## Gotchas

- Unlike the Jetty script, there is no `--port` option and no check that the port is free. The port comes from the application's own configuration.
- The build step runs only when `target/classes` is missing. Stale classes are not rebuilt.
- It needs `mvn` on the `PATH`.

## See also

- `start-petstore-jetty.py`
- Docs site: Developer Info, Script: start-petstore-springboot (`04.02.13.ScriptStartPetstoreSpringboot.md` in juneau-docs)
