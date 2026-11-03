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

It reads the format badges from the JuneauMarshall page (the order and the 26 core + RDF split), runs each facade's `of(new Person("Ada", 36))` through a small single-file Java program against the `10.0.0-SNAPSHOT` jars in `~/.m2`, and writes one entry per format. Binary formats are stored as a hex dump (truncated). SSE only serializes `SseEvent`, so its call wraps the Json output in an event.

It stops with an error when the badge labels and the script's facade table disagree.

## Options

- `--juneau-root PATH`: Juneau checkout used to build the classpath (default `$JUNEAU_ROOT`, then `~/git/apache/juneau`).
- `--classpath-file FILE`: use this classpath instead of running `mvn dependency:build-classpath` (through the Maven lock wrapper) in juneau-marshall-rdf.

Requires `~/jdk/default` (Java 17) and installed SNAPSHOT jars.
