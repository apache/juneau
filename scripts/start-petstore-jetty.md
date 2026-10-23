# start-petstore-jetty.py

Starts the Juneau petstore sample application on the embedded Jetty microservice, with plain `java` and a cached Maven classpath.

## When to use

- You want the Jetty flavor of the petstore running locally, on port 10000 or another port you choose.
- For the Spring Boot flavor, use `start-petstore-springboot.py`.

## Usage

```bash
python3 scripts/start-petstore-jetty.py
python3 scripts/start-petstore-jetty.py --port 10100
```

Stop the server with Ctrl+C.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--port` | N (integer, 1-65535) | 10000 | Port to listen on. Passed to the application as the `petstore.port` system property; the checked-in configuration file is not changed. Out-of-range values are rejected by the argument parser. |

## What it does

1. Parses `--port` (`parse_args`).
2. Checks whether something already listens on `localhost:<port>` (`port_in_use`). If so, prints the port and, when `lsof` can find it, the holder process, and exits 2 before building or launching anything (`port_busy_message`).
3. Finds the project root and requires `juneau-petstore/juneau-petstore-jetty` under it.
4. If the module has no `target/classes`, runs `mvn clean compile` there.
5. Resolves the runtime classpath with `mvn -q dependency:build-classpath`, caching it in `target/petstore-classpath.txt` and refreshing the cache when the module's `pom.xml` is newer (`resolve_classpath`).
6. Runs `java -Dpetstore.port=<port> -cp <classes>:<classpath> org.apache.juneau.petstore.jetty.App` in the module directory, using `$JAVA_HOME/bin/java` when `JAVA_HOME` is set (`launch_command`).

## Output and exit codes

Colored status lines, then the application's output. The banner shows `http://localhost:<port>`.

| Code | Meaning |
|---|---|
| 0 | Stopped with Ctrl+C. |
| 1 | The project root or module directory was not found, the build or classpath step failed, or the launch raised an error. |
| 2 | The port is already in use. |
| other | The exit code of the `java` process when it ends on its own. |

## Gotchas

- The port is strict. A busy port is never skipped or shared, so a request can never be answered by a stale server.
- The script uses plain `java` rather than `exec:java` because the Maven exec class loader cannot load some petstore classes.
- The classes build only runs when `target/classes` is missing. Stale classes are not rebuilt.
- `lsof` is optional; without it the busy-port message just suggests the command to run.

## See also

- `start-petstore-springboot.py`
- Docs site: Developer Info, Script: start-petstore-jetty (`04.02.12.ScriptStartPetstoreJetty.md` in juneau-docs)
