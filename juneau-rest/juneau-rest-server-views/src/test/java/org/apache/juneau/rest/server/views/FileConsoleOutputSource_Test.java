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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.apache.juneau.rest.server.views.ConsoleOutputSource.*;
import org.apache.juneau.rest.server.views.FileConsoleOutputSource.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class FileConsoleOutputSource_Test extends TestBase {

	private static final Status RUNNING = new Status("RUNNING", Style.ACCENT, false, Instant.parse("2026-10-07T13:00:00Z"), null);
	private static final Status DONE = new Status("DONE", Style.SUCCESS, true, Instant.parse("2026-10-07T13:00:00Z"), 5000L);

	@TempDir Path dir;
	Path f;
	AtomicReference<Status> status;

	@BeforeEach void setUp() {
		f = dir.resolve("run.log");
		status = new AtomicReference<>(RUNNING);
	}

	private FileConsoleOutputSource source() {
		return FileConsoleOutputSource.create(f).status(status::get).build();
	}

	private void write(String s) throws IOException {
		Files.writeString(f, s, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	private void lines(int from, int to) throws IOException {
		var sb = new StringBuilder();
		for (var i = from; i <= to; i++)
			sb.append("line ").append(i).append('\n');
		write(sb.toString());
	}

	private static List<Long> ns(ConsoleOutputPage p) {
		return p.lines.stream().map(l -> l.n).collect(Collectors.toList());
	}

	private static List<String> texts(ConsoleOutputPage p) {
		return p.lines.stream().map(l -> l.text).collect(Collectors.toList());
	}

	private static String epochOf(String token) {
		return token.substring(0, token.indexOf('.'));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a - growth and the partial last line
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_growth() throws Exception {
		lines(1, 10);
		var src = source();
		var p1 = src.page(null, 100).validate();
		assertEquals(LongStream.rangeClosed(1, 10).boxed().collect(Collectors.toList()), ns(p1));
		assertBean(p1, "more,terminal,hasEarlier", "false,false,false");
		lines(11, 15);
		var p2 = src.page(p1.next, 100).validate();
		assertList(ns(p2), 11L, 12L, 13L, 14L, 15L);
		assertBean(p2.lines.get(0), "text", "line 11");
		assertBean(p2, "hasEarlier,before", "true," + p1.next);
	}

	@Test void a02_partialLineHeld() throws Exception {
		lines(1, 3);
		var src = source();
		var p1 = src.page(null, 100);
		write("abc");
		var p2 = src.page(p1.next, 100).validate();
		assertEquals(0, p2.lines.size());
		assertEquals(p1.next, p2.next);
		assertBean(p2, "more,terminal", "false,false");
	}

	@Test void a03_partialLineFlushedWhenTerminal() throws Exception {
		lines(1, 3);
		write("abc");
		var src = source();
		var p1 = src.page(null, 100);
		assertEquals(3, p1.lines.size());
		status.set(DONE);
		var p2 = src.page(p1.next, 100).validate();
		assertList(texts(p2), "abc");
		assertList(ns(p2), 4L);
		assertBean(p2, "more,terminal,state,durationMs", "false,true,DONE,5000");
		var size = Files.size(f);
		assertEquals(epochOf(p1.next) + "." + size + ".5", p2.next);
		var p3 = src.page(p2.next, 100).validate();
		assertEquals(0, p3.lines.size());
		assertBean(p3, "terminal,next", "true," + p2.next);
		var back = src.before(p2.next, 10).validate();
		assertList(ns(back), 1L, 2L, 3L, 4L);
	}

	@Test void a04_terminalOnlyOnLastPage() throws Exception {
		lines(1, 5000);
		status.set(DONE);
		var src = source();
		var p1 = src.page(null, 2000).validate();
		assertBean(p1, "more,terminal", "true,false");
		assertNull(p1.durationMs);
		var p2 = src.page(p1.next, 2000).validate();
		assertBean(p2, "more,terminal", "true,false");
		var p3 = src.page(p2.next, 2000).validate();
		assertEquals(1000, p3.lines.size());
		assertBean(p3, "more,terminal", "false,true");
		var capped = src.page(null, 50_000).validate();
		assertEquals(FileConsoleOutputSource.MAX_PAGE_LINES, capped.lines.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - truncation, rotation and rewrites give 410
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_truncate() throws Exception {
		lines(1, 50);
		var src = source();
		var p = src.page(null, 100);
		Files.writeString(f, "new 1\n", StandardOpenOption.TRUNCATE_EXISTING);
		assertThrows(UnknownTokenException.class, () -> src.page(p.next, 100));
		var t = src.tail(10).validate();
		assertList(texts(t), "new 1");
		assertNotEquals(epochOf(p.next), epochOf(t.next));
	}

	@Test void b02_rotate() throws Exception {
		lines(1, 50);
		assumeTrue(Files.readAttributes(f, BasicFileAttributes.class).fileKey() != null, "filesystem has no file keys");
		var src = source();
		var p = src.page(null, 100);
		Files.move(f, dir.resolve("run.log.1"));
		lines(1, 50);
		assertThrows(UnknownTokenException.class, () -> src.page(p.next, 100));
	}

	@Test void b03_rewriteInPlace() throws Exception {
		lines(1, 50);
		var src = source();
		var p = src.page(null, 100);
		try (var ch = FileChannel.open(f, StandardOpenOption.WRITE)) {
			ch.write(ByteBuffer.wrap("#".repeat(64).getBytes(UTF_8)), 0);
		}
		assertThrows(UnknownTokenException.class, () -> src.page(p.next, 100));
	}

	@Test void b04_customIndex() throws Exception {
		lines(1, 10_000);
		var src = FileConsoleOutputSource.create(f).status(status::get)
			.index(FileLineIndex.create(f).stride(2).maxEntries(8).build()).build();
		String token = null;
		var seen = 0L;
		while (seen < 10_000) {
			var p = src.page(token, 777).validate();
			for (var l : p.lines)
				assertEquals(++seen, l.n.longValue());
			token = p.next;
		}
		var back = src.before(token, 5).validate();
		assertList(ns(back), 9996L, 9997L, 9998L, 9999L, 10_000L);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - decoding
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_malformedUtf8() throws Exception {
		Files.write(f, new byte[] {'a', (byte)0xC3, '(', 'b', '\n', (byte)0xFF, '\n', 'z', '\n'});
		var src = source();
		var p = src.page(null, 100).validate();
		assertList(texts(p), "a\uFFFD(b", "\uFFFD", "z");
		var first = src.page(null, 1);
		assertEquals(epochOf(first.next) + ".5.2", first.next);
		var second = src.page(first.next, 1);
		assertEquals(epochOf(first.next) + ".7.3", second.next);
	}

	@Test void c02_bomAndCrlf() throws Exception {
		Files.write(f, new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF, 'x', '\r', '\n', 'y', '\r', '\n'});
		assertList(texts(source().page(null, 10)), "x", "y");
	}

	@Test void c03_hugeLine() throws Exception {
		var huge = "a" + "é".repeat(524_288);
		write(huge + "\nnext\n");
		var bytes = huge.getBytes(UTF_8);
		var decoded = FileConsoleOutputSource.decode(bytes, bytes.length, false);
		assertTrue(decoded.startsWith("aé"));
		assertTrue(decoded.endsWith("… [truncated, line was 1048577 bytes]"), decoded.substring(decoded.length() - 50));
		assertFalse(decoded.contains("\uFFFD"));
		assertEquals(1 + 131_071 + "… [truncated, line was 1048577 bytes]".length(), decoded.length());
		var src = source();
		var p = src.page(null, 10).validate();
		assertEquals(2, p.lines.size());
		assertTrue(p.lines.get(0).text.contains("… [truncated"), "cut line carries a truncation suffix");
		assertBean(p.lines.get(1), "n,text", "2,next");
		assertEquals(epochOf(p.next) + "." + Files.size(f) + ".3", p.next);
	}

	@Test void c04_ansiOnByDefaultAndOff() throws Exception {
		write("\u001b[31mred\u001b[0m\n");
		var on = source().page(null, 10).lines.get(0);
		assertEquals(Style.ERROR, on.frags.get(0).style);
		var off = FileConsoleOutputSource.create(f).ansi(false).build().page(null, 10).lines.get(0);
		assertEquals("\uFFFD[31mred\uFFFD[0m", off.text);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d - tokens
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_forgedTokens() throws Exception {
		lines(1, 10);
		var src = source();
		var e = epochOf(src.page(null, 1).next);
		var other = "zzzzzz".equals(e) ? "yyyyyy" : "zzzzzz";
		var line3 = src.page(src.page(null, 2).next, 1).before;
		var off3 = line3.split("\\.")[1];
		for (var bad : List.of(other + ".0.1", e + ".3.2", e + ".999999.2", e + "." + off3 + ".2", e + ".x.1", e + ".0.0",
				e + ".-1.1", e + ".01.1", "abc", "", e + ".0", e + ".0.1.2")) {
			assertThrows(UnknownTokenException.class, () -> src.page(bad, 10), bad);
			assertThrows(UnknownTokenException.class, () -> src.before(bad, 10), bad);
		}
		assertThrows(UnknownTokenException.class, () -> src.before(null, 10));
		assertDoesNotThrow(() -> src.page(e + "." + off3 + ".3", 10));
	}

	@Test void d02_endTokenRequiresTerminal() throws Exception {
		lines(1, 2);
		write("tail");
		var src = source();
		var e = epochOf(src.page(null, 1).next);
		var endToken = e + "." + Files.size(f) + ".4";
		assertThrows(UnknownTokenException.class, () -> src.page(endToken, 10));
		status.set(DONE);
		assertDoesNotThrow(() -> src.page(endToken, 10));
	}

	@Test void d03_emptyAndMissing() throws Exception {
		var src = source();
		var p = src.page(null, 10).validate();
		assertEquals(0, p.lines.size());
		assertTrue(p.next.matches("^[0-9a-z]{6}\\.0\\.1$"), p.next);
		assertBean(p, "more,hasEarlier", "false,false");
		var t = src.tail(10).validate();
		assertEquals(p.next, t.next);
		Files.createFile(f);
		var q = source().page(null, 10).validate();
		assertEquals(0, q.lines.size());
		assertTrue(q.next.endsWith(".0.1"));
		try (var s = source().stream()) {
			assertEquals(0, s.count());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// e - tail, before and budgets
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_tailBeforeContiguity() throws Exception {
		lines(1, 12_345);
		var src = source();
		var forward = new ArrayList<ConsoleOutputLine>();
		String token = null;
		do {
			var p = src.page(token, 2000);
			forward.addAll(p.lines);
			token = p.next;
			if (! p.more)
				break;
		} while (true);
		var t = src.tail(5000).validate();
		assertEquals(5000, t.lines.size());
		assertEquals(7346L, t.lines.get(0).n);
		assertEquals(token, t.next);
		var collected = new ArrayList<ConsoleOutputLine>(t.lines);
		var before = t.before;
		while (before != null) {
			var b = src.before(before, 2000).validate();
			collected.addAll(0, b.lines);
			before = b.before;
		}
		assertEquals(forward.size(), collected.size());
		for (var i = 0; i < forward.size(); i++)
			assertEquals(forward.get(i).toContractMap(), collected.get(i).toContractMap(), "i=" + i);
	}

	@Test void e02_charBudget() throws Exception {
		var big = "x".repeat(65_536);
		var sb = new StringBuilder();
		for (var i = 0; i < 200; i++)
			sb.append(big).append('\n');
		write(sb.toString());
		var src = FileConsoleOutputSource.create(f).ansi(false).build();
		var perPage = ConsoleOutputPage.MAX_PAGE_CHARS / 65_536;
		var t = src.tail(5000).validate();
		assertEquals(perPage, t.lines.size());
		assertEquals(200L, t.lines.get(perPage - 1).n);
		assertTrue(t.hasEarlier);
		var fwd = src.page(null, 2000).validate();
		assertEquals(perPage, fwd.lines.size());
		assertTrue(fwd.more);
		var b = src.before(t.before, 2000).validate();
		assertEquals(200 - perPage, b.lines.size());
		assertFalse(b.hasEarlier);
	}

	@Test void e03_tailIncludesTerminalPartial() throws Exception {
		lines(1, 5);
		write("end");
		status.set(DONE);
		var t = source().tail(2).validate();
		assertList(texts(t), "line 5", "end");
		assertBean(t, "terminal,more", "true,false");
	}

	//------------------------------------------------------------------------------------------------------------------
	// f - concurrency, decorator, stream
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_concurrentAppendAndRead() throws Exception {
		Files.createFile(f);
		var src = source();
		var pool = Executors.newFixedThreadPool(5);
		try {
			var writer = pool.submit(() -> {
				for (var i = 1; i <= 100_000; i += 500)
					lines(i, i + 499);
				return null;
			});
			var readers = new ArrayList<Future<?>>();
			for (var r = 0; r < 4; r++)
				readers.add(pool.submit(() -> {
					String token = null;
					long last = 0;
					while (last < 100_000) {
						var p = src.page(token, 2000).validate();
						for (var l : p.lines) {
							assertEquals(++last, l.n.longValue());
							assertEquals("line " + last, l.text);
						}
						token = p.next;
					}
					return null;
				}));
			writer.get(120, TimeUnit.SECONDS);
			for (var fu : readers)
				fu.get(120, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void f02_decorator() throws Exception {
		write("##run step\nplain\n");
		var src = FileConsoleOutputSource.create(f)
			.decorate((raw, line) -> raw.startsWith("##run ") ? line.marker(true) : line).build();
		var p = src.page(null, 10).validate();
		assertEquals(Boolean.TRUE, p.lines.get(0).ui.marker);
		assertNull(p.lines.get(1).ui);
		var bad = FileConsoleOutputSource.create(f).decorate((raw, line) -> line.color("red")).build();
		assertThrowsWithMessage(IllegalStateException.class, "decorator produced an invalid line 1", () -> bad.page(null, 10));
		var nul = FileConsoleOutputSource.create(f).decorate((raw, line) -> null).build();
		assertThrowsWithMessage(IllegalStateException.class, "decorator returned null for line 1", () -> nul.page(null, 10));
		var renumber = FileConsoleOutputSource.create(f).decorate((raw, line) -> line.n(99)).build();
		assertThrowsWithMessage(IllegalStateException.class, "decorator changed n of line 1", () -> renumber.page(null, 10));
	}

	@Test void f03_streamMatchesPagesAndCloses() throws Exception {
		lines(1, 4321);
		write("partial");
		status.set(DONE);
		var src = source();
		var forward = new ArrayList<Map<String,Object>>();
		String token = null;
		while (true) {
			var p = src.page(token, 1000);
			p.lines.forEach(l -> forward.add(l.toContractMap()));
			token = p.next;
			if (p.terminal)
				break;
		}
		var before = FileConsoleOutputSource.openReaders();
		List<Map<String,Object>> streamed;
		try (var s = src.stream()) {
			streamed = s.map(ConsoleOutputLine::toContractMap).collect(Collectors.toList());
			assertEquals(before + 1, FileConsoleOutputSource.openReaders());
		}
		assertEquals(before, FileConsoleOutputSource.openReaders());
		assertEquals(forward, streamed);
		assertEquals("partial", streamed.get(streamed.size() - 1).get("text"));
	}

	@Test void f04_pagesCloseTheirChannel() throws Exception {
		lines(1, 100);
		var src = source();
		var before = FileConsoleOutputSource.openReaders();
		var p = src.page(null, 10);
		src.tail(10);
		src.before(p.next, 5);
		assertEquals(before, FileConsoleOutputSource.openReaders());
	}

	//------------------------------------------------------------------------------------------------------------------
	// g - builder
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_builder() {
		assertThrows(NullPointerException.class, () -> FileConsoleOutputSource.create(null));
		assertThrowsWithMessage(IllegalArgumentException.class, "index is for a different file",
			() -> FileConsoleOutputSource.create(f).index(FileLineIndex.of(dir.resolve("other.log"))).build());
		var p = FileConsoleOutputSource.create(f).build().page(null, 1).validate();
		assertBean(p, "state,terminal", "RUNNING,false");
	}
}
