#!/usr/bin/env python3
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
"""Regenerates src/data/hero-samples.json, the per-format outputs on the home-page hero.

See capture-hero-samples.md.
"""
import argparse
import base64
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

DOCS = Path(__file__).resolve().parent.parent
MARSHALL_PAGE = DOCS / 'pages/topics/03.00.JuneauMarshall.md'
OUT = DOCS / 'src/data/hero-samples.json'
BADGE_RE = re.compile(r'<a href="[^"]+"><span className="badge\b[^>]*>([^<]+)</span></a>')
CORE_PKG, RDF_PKG = 'org.apache.juneau.marshall.marshaller', 'org.apache.juneau.marshall.jena.marshaller'

# Badge label -> (facade class, highlight language for the Output block).
FACADES = {
    'JSON': ('Json', 'json'), 'JSON5': ('Json5', 'json'), 'JSONL': ('Jsonl', 'json'), 'JSON5L': ('Json5l', 'json'),
    'JCS': ('Jcs', 'json'), 'XML': ('Xml', 'xml'), 'HTML': ('Html', 'html'), 'YAML': ('Yaml', 'yaml'),
    'TOML': ('Toml', 'toml'), 'HOCON': ('Hocon', 'text'), 'HJSON': ('Hjson', 'text'), 'INI': ('Ini', 'ini'),
    'CBOR': ('Cbor', 'text'), 'BSON': ('Bson', 'text'), 'MessagePack': ('MsgPack', 'text'),
    'Protobuf': ('Protobuf', 'text'), 'Prototext': ('Prototext', 'text'), 'UON': ('Uon', 'text'),
    'URL-Encoding': ('UrlEncoding', 'text'), 'OpenAPI': ('OpenApi', 'text'), 'CSV': ('Csv', 'text'),
    'Parquet': ('Parquet', 'text'), 'Markdown': ('Markdown', 'markdown'), 'MarkdownDoc': ('MarkdownDoc', 'markdown'),
    'SSE': ('Sse', 'text'), 'PlainText': ('PlainText', 'text'),
    'RDF/XML': ('RdfXml', 'xml'), 'RDF/XML-ABBREV': ('RdfXmlAbbrev', 'xml'), 'N-Triple': ('NTriple', 'text'),
    'N-Quads': ('NQuads', 'text'), 'N3': ('N3', 'text'), 'Turtle': ('Turtle', 'text'), 'TriG': ('TriG', 'text'),
    'TriX': ('TriX', 'xml'), 'JSON-LD': ('JsonLd', 'json'), 'RDF/JSON': ('RdfJson', 'json'),
    'RDF/Proto': ('RdfProto', 'text'), 'RDF/Thrift': ('RdfThrift', 'text'),
}
CORE_COUNT = 26
# Formats whose facade cannot take the bare Person; the shown call differs.
CALL_OVERRIDES = {'SSE': 'Sse.of(new SseEvent("person", Json.of(new Person("Ada", 36))))'}

JAVA = '''
import java.lang.reflect.Method;
import java.util.Base64;

public class HeroCapture {
	public record Person(String name, int age) {}
	public static void main(String[] a) throws Exception {
		for (String spec : a) {
			String[] p = spec.split(":");
			Method m = Class.forName(p[0]).getMethod("of", Object.class);
			Person person = new Person("Ada", 36);
			// Sse only serializes SseEvent objects, so it gets the Json of the person as the event data.
			Object arg = p[1].equals("SSE")
				? new org.apache.juneau.marshall.sse.SseEvent("person", org.apache.juneau.marshall.marshaller.Json.of(person))
				: person;
			Object o = m.invoke(null, arg);
			byte[] b = o instanceof byte[] ? (byte[]) o : ((String) o).getBytes("UTF-8");
			System.out.println(p[1] + "\\t" + (o instanceof byte[] ? "B" : "T") + "\\t" + Base64.getEncoder().encodeToString(b));
		}
	}
}
'''


def classpath(args):
    if args.classpath_file:
        return Path(args.classpath_file).read_text().strip()
    out = Path(tempfile.gettempdir(), 'hero-cp.txt')
    lock = Path.home() / 'agents/skills/juneau-worktrees/juneau-mvn-lock.sh'
    rdf = Path(args.juneau_root, 'juneau-core/juneau-marshall-rdf')
    env = dict(os.environ, JAVA_HOME=str(Path.home() / 'jdk/default'))
    subprocess.run([str(lock), 'mvn', '-o', '-q', 'dependency:build-classpath', f'-Dmdep.outputFile={out}'],
                   cwd=rdf, env=env, check=True)
    return out.read_text().strip()


def hexdump(b, limit=96):
    s = ' '.join(f'{x:02x}' for x in b[:limit])
    return s + (' ...' if len(b) > limit else '')


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--juneau-root', default=os.environ.get('JUNEAU_ROOT', str(Path.home() / 'git/apache/juneau')))
    ap.add_argument('--classpath-file', help='file holding the classpath; default: build it with Maven')
    args = ap.parse_args()
    labels = BADGE_RE.findall(MARSHALL_PAGE.read_text(encoding='utf-8'))
    unknown = [l for l in labels if l not in FACADES]
    if unknown or len(labels) != len(FACADES):
        sys.exit(f'badge labels and FACADES disagree: unknown={unknown}, badges={len(labels)}, facades={len(FACADES)}')
    cp = classpath(args)
    jars = [str(j) for j in Path.home().glob('.m2/repository/org/apache/juneau/juneau-marshall*/10.0.0-SNAPSHOT/*-10.0.0-SNAPSHOT.jar')]
    cp = ':'.join([cp] + jars)
    specs = [f"{(CORE_PKG if i < CORE_COUNT else RDF_PKG)}.{FACADES[l][0]}:{l}" for i, l in enumerate(labels)]
    with tempfile.TemporaryDirectory() as d:
        src = Path(d, 'HeroCapture.java')
        src.write_text(JAVA)
        java = Path.home() / 'jdk/default/bin/java'
        res = subprocess.run([str(java), '-cp', cp, str(src), *specs], capture_output=True, text=True, cwd=d)
    if res.returncode:
        sys.exit(res.stderr)
    samples = []
    for i, line in enumerate(res.stdout.splitlines()):
        label, kind, b64 = line.split('\t')
        raw = base64.b64decode(b64)
        cls, lang = FACADES[label]
        binary = kind == 'B'
        samples.append({
            'id': cls, 'label': label, 'group': 'core' if i < CORE_COUNT else 'rdf', 'call': CALL_OVERRIDES.get(label, f'{cls}.of(new Person("Ada", 36))'),
            'language': 'text' if binary else lang, 'binary': binary,
            'output': hexdump(raw) if binary else raw.decode('utf-8').rstrip('\n')})
    if len(samples) != len(labels):
        sys.exit(f'captured {len(samples)} of {len(labels)} formats')
    OUT.write_text(json.dumps(samples, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'wrote {len(samples)} samples to {OUT.relative_to(DOCS)}')


if __name__ == '__main__':
    main()
