# capture-hero-samples.py

Regenerates `src/data/hero-samples.json`, the per-format outputs behind the format dropdown on the home-page hero (`src/pages/index.tsx`).

## When to use

- A serializer's default output changed.
- A format was added to or removed from the badge block on `pages/topics/03.00.JuneauMarshall.md`.

## Usage

```bash
python3 scripts/capture-hero-samples.py
python3 scripts/capture-hero-samples.py --classpath-file /tmp/hero-cp.txt
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--juneau-root` | PATH | `$JUNEAU_ROOT`, then `~/git/apache/juneau` | Juneau checkout used to build the classpath. |
| `--classpath-file` | FILE | none | Use this classpath instead of running `mvn dependency:build-classpath` (through the Maven lock wrapper) in juneau-marshall-rdf. |

## What it does

It reads the format badges from the JuneauMarshall page (the order and the 26 core + RDF split), runs each facade's `of(new Person("Ada", 36))` through a small single-file Java program against the `10.0.0-SNAPSHOT` jars in `~/.m2`, and writes one entry per format. Binary formats are stored as a hex dump (truncated). SSE only serializes `SseEvent`, so its call wraps the Json output in an event.

## Output and exit codes

- Writes `src/data/hero-samples.json` and prints `wrote N samples to src/data/hero-samples.json`; exit 0.
- Exits non-zero, writing nothing, when the badge labels and the script's facade table disagree, when the Maven classpath build or the Java program fails (its stderr is printed), or when fewer samples than badges were captured.

## Gotchas

- Requires `~/jdk/default` (Java 17) and installed SNAPSHOT jars; the Maven step runs offline (`-o`).
- A new badge on the JuneauMarshall page needs a matching `FACADES` entry in the script.

## See also

- `src/pages/index.tsx` (the hero format dropdown)
- `pages/topics/03.00.JuneauMarshall.md` (the badge block)
