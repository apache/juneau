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

import static java.nio.file.StandardOpenOption.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.RunViewSource.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class FileRunViewSource_Test extends TestBase {

	@TempDir Path dir;

	private Path f;

	@BeforeEach void setup() {
		f = dir.resolve("run.jsonl");
	}

	private void write(String...lines) throws Exception {
		var sb = new StringBuilder();
		for (var l : lines)
			sb.append(l).append('\n');
		Files.writeString(f, sb.toString(), CREATE, APPEND);
	}

	private FileRunViewSource src() {
		return FileRunViewSource.create(f).build();
	}

	private static List<Long> seqs(RunViewPage p) {
		return p.events().stream().map(RunEvent::seq).toList();
	}

	private static String note(int i) {
		return "{\"ev\":\"note\",\"level\":\"info\",\"text\":\"n" + i + "\"}";
	}

	@Test void a01_seqIsTheLineNumber() throws Exception {
		write("{\"ev\":\"step\",\"id\":\"a\",\"title\":\"A\"}", "{\"ev\":\"end\",\"id\":\"a\",\"status\":\"ok\"}");
		var p = src().page(null, 10).validate();
		assertEquals(List.of(1L, 2L), seqs(p));
		assertTrue(p.next().endsWith(".2"));
	}

	@Test void a02_seqMemberInFileIsOverwritten() throws Exception {
		write("{\"ev\":\"done\",\"status\":\"ok\",\"seq\":99}");
		assertEquals(List.of(1L), seqs(src().page(null, 10)));
	}

	@Test void b01_partialTrailingLineWaitsForNewline() throws Exception {
		Files.writeString(f, "{\"ev\":\"done\",\"status\":\"ok\"}", CREATE, APPEND);
		var s = src();
		assertTrue(s.page(null, 10).events().isEmpty());
		Files.writeString(f, "\n", APPEND);
		assertEquals(1, s.page(null, 10).events().size());
	}

	@Test void b02_badLinesAreSkippedWithSeqGap() throws Exception {
		write("not json", "{\"ev\":\"bogus\"}", "{\"ev\":\"done\",\"status\":\"ok\"}");
		var p = src().page(null, 10).validate();
		assertEquals(List.of(3L), seqs(p));
		assertTrue(p.next().endsWith(".3"));
	}

	@Test void b03_overlongLineSkipped() throws Exception {
		write("{\"ev\":\"note\",\"level\":\"info\",\"text\":\"" + "x".repeat(70_000) + "\"}", note(2));
		assertEquals(List.of(2L), seqs(src().page(null, 10)));
	}

	@Test void b04_blankLineAndBomAndCr() throws Exception {
		Files.writeString(f, "﻿" + note(1) + "\r\n\n" + note(3) + "\n", CREATE, APPEND);
		assertEquals(List.of(1L, 3L), seqs(src().page(null, 10)));
	}

	@Test void c01_paging() throws Exception {
		var lines = new String[25];
		for (var i = 0; i < 25; i++)
			lines[i] = note(i);
		write(lines);
		var s = src();
		var p1 = s.page(null, 10).validate();
		var p2 = s.page(p1.next(), 10).validate();
		var p3 = s.page(p2.next(), 10).validate();
		assertEquals(List.of(10, 10, 5), List.of(p1.events().size(), p2.events().size(), p3.events().size()));
		assertEquals(List.of(true, true, false), List.of(p1.more(), p2.more(), p3.more()));
		assertEquals(25L, p3.events().get(4).seq());
	}

	@Test void c02_tokenResumes() throws Exception {
		write(note(1), note(2));
		var s = src();
		var p = s.page(null, 10);
		write(note(3));
		assertEquals(List.of(3L), seqs(s.page(p.next(), 10)));
	}

	@Test void c03_tokenKeepsWorkingAsFileGrowsPastFingerprint() throws Exception {
		write(note(1));
		var s = src();
		var p = s.page(null, 10);
		var token = p.next();
		for (var i = 2; i < 40; i++) {
			write(note(i));
			s.page(null, 1);
		}
		var q = s.page(token, 100);
		assertEquals(2L, q.events().get(0).seq());
		assertEquals(39L, q.events().get(q.events().size() - 1).seq());
	}

	@Test void d01_truncationIsStale() throws Exception {
		write(note(1), note(2), note(3));
		var s = src();
		var t = s.page(null, 10).next();
		Files.writeString(f, "{\"ev\":\"done\",\"status\":\"ok\"}\n", TRUNCATE_EXISTING);
		assertThrows(UnknownTokenException.class, () -> s.page(t, 10));
	}

	@Test void d02_replacementIsStale() throws Exception {
		write(note(1), note(2), note(3));
		var s = src();
		var t = s.page(null, 10).next();
		Files.delete(f);
		write("{\"ev\":\"done\",\"status\":\"ok\"}", "{\"ev\":\"done\",\"status\":\"fail\"}", "{\"ev\":\"done\",\"status\":\"ok\"}", "{\"ev\":\"done\",\"status\":\"ok\"}");
		assertThrows(UnknownTokenException.class, () -> s.page(t, 10));
	}

	@Test void d03_tokenBeyondIndexedLinesIsStale() throws Exception {
		write(note(1));
		var s = src();
		var t = s.page(null, 10).next();
		var epoch = t.substring(0, 6);
		assertThrows(UnknownTokenException.class, () -> s.page(epoch + ".5", 10));
	}

	@Test void d04_malformedToken() throws Exception {
		write(note(1));
		var s = src();
		for (var bad : List.of("zzz", "abcdef.x", "a b", "abcdef.01", ""))
			assertThrows(UnknownTokenException.class, () -> s.page(bad, 10), bad);
	}

	@Test void e01_terminalIsReadBeforeTheFile() throws Exception {
		var flag = new AtomicBoolean();
		var s = FileRunViewSource.create(f).terminal(flag::get).build();
		write(note(1));
		assertFalse(s.page(null, 10).terminal());
		flag.set(true);
		assertTrue(s.page(null, 10).terminal());
	}

	@Test void e02_terminalOnlyAtEnd() throws Exception {
		var s = FileRunViewSource.create(f).terminal(() -> true).build();
		write(note(1), note(2), note(3));
		var p1 = s.page(null, 2);
		assertTrue(p1.more());
		assertFalse(p1.terminal());
		assertTrue(s.page(p1.next(), 2).terminal());
	}

	@Test void f01_missingFileIsEmptyFirstPage() {
		var p = src().page(null, 10).validate();
		assertTrue(p.events().isEmpty());
		assertTrue(p.next().matches("^[0-9a-z]{6}\\.0$"), p.next());
	}

	@Test void f02_missingThenCreated() throws Exception {
		var s = src();
		var t = s.page(null, 10).next();
		write(note(1));
		// The file generation changed under the token, so the token is stale; a fresh first page works.
		assertThrows(UnknownTokenException.class, () -> s.page(t, 10));
		assertEquals(List.of(1L), seqs(s.page(null, 10)));
	}

	@Test void g01_indexMustMatchFile() {
		var other = FileLineIndex.of(dir.resolve("other.jsonl"));
		assertThrows(IllegalArgumentException.class, () -> FileRunViewSource.create(f).index(other).build());
	}
}
