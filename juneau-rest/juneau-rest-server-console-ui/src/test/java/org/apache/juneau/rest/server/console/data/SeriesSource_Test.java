/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.console.data;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

class SeriesSource_Test extends TestBase {

	/** Counts flush() calls, so the flush cadence is visible. */
	static final class Sink extends ByteArrayOutputStream {
		int flushes;
		@Override public void flush() { flushes++; }
		String text() { return new String(toByteArray(), UTF_8); }
	}

	static SeriesSource.Row row(Object...kv) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < kv.length; i += 2)
			m.put((String)kv[i], kv[i + 1]);
		return SeriesSource.Row.of(m);
	}

	@Test void a01_csvHeaderAndQuoting() {
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, s).columns("a", "b").write(Stream.of(
			row("a", "plain", "b", "x,y"),
			row("a", "say \"hi\"", "b", "two\nlines"),
			row("a", "#hash", "b", "ok")));
		assertEquals("a,b\nplain,\"x,y\"\n\"say \"\"hi\"\"\",\"two\nlines\"\n\"#hash\",ok\n", s.text());
	}

	@Test void a02_csvNullVersusEmpty() {
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, s).columns("a", "b").write(Stream.of(row("a", null, "b", "")));
		assertEquals("a,b\n,\"\"\n", s.text());
	}

	@Test void a03_valueTypes() {
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, s).columns("t", "d", "n", "b").write(Stream.of(
			row("t", Instant.parse("2026-10-11T01:02:03Z"), "d", LocalDate.of(2026, 10, 11), "n", 1.5, "b", true)));
		assertEquals("t,d,n,b\n2026-10-11T01:02:03Z,2026-10-11T00:00:00Z,1.5,true\n", s.text());
	}

	@Test void a04_capWritesSentinelOnlyWhenMoreRowsExist() {
		var pulled = new AtomicInteger();
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, s).columns("v").cap(3)
			.write(IntStream.range(0, 1_000_000).peek(i -> pulled.incrementAndGet()).mapToObj(i -> row("v", i)));
		assertEquals("v\n0\n1\n2\n#truncated,3\n", s.text());
		assertEquals(4, pulled.get(), "reads cap rows plus one look-ahead row, then stops");
	}

	@Test void a05_exactCapIsNotTruncated() {
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, s).columns("v").cap(2).write(Stream.of(row("v", 1), row("v", 2)));
		assertEquals("v\n1\n2\n", s.text());
	}

	@Test void a06_flushCadence() {
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, s).columns("v")
			.write(IntStream.range(0, 2500).mapToObj(i -> row("v", i)));
		assertEquals(3, s.flushes, "after row 1000, after row 2000, and at the end");
		var big = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, big).columns("v")
			.write(Stream.of(row("v", "x".repeat(70 * 1024)), row("v", "y")));
		assertEquals(2, big.flushes, "64K characters forces a flush before 1000 rows");
	}

	@Test void a07_ndjson() throws Exception {
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.NDJSON, s).columns("t", "n", "x").cap(1).write(Stream.of(
			row("t", Instant.ofEpochMilli(0), "n", Double.NaN, "x", "q\""),
			row("t", Instant.ofEpochMilli(1), "n", 2, "x", "r")));
		var lines = s.text().split("\n");
		assertEquals(2, lines.length);
		var first = JsonMap.ofString(lines[0]);
		assertEquals("1970-01-01T00:00:00Z", first.getString("t"));
		assertNull(first.get("n"), "non-finite numbers are written as null");
		assertEquals("q\"", first.getString("x"));
		assertEquals(1, JsonMap.ofString(lines[1]).getInt("$truncated"));
	}

	@Test void a08_errors() {
		var s = new Sink();
		var src = new SeriesSource(SeriesSource.Format.CSV, s);
		var e1 = assertThrows(IllegalStateException.class, () -> src.write(Stream.empty()));
		assertEquals("SeriesSource.write: columns(...) not set", e1.getMessage());
		assertThrows(IllegalArgumentException.class, src::columns);
		assertThrows(IllegalArgumentException.class, () -> src.cap(0));
	}

	@Test void a09_dateSubclassAndLocalDateTime() {
		var d = new Date(0) {
			private static final long serialVersionUID = 1L;
			@Override public Instant toInstant() { throw new UnsupportedOperationException(); }
		};
		var s = new Sink();
		new SeriesSource(SeriesSource.Format.CSV, s).columns("t", "l")
			.write(Stream.of(row("t", d, "l", LocalDateTime.of(2026, 10, 11, 1, 2, 3))));
		assertEquals("t,l\n1970-01-01T00:00:00Z,2026-10-11T01:02:03Z\n", s.text());
	}
}
