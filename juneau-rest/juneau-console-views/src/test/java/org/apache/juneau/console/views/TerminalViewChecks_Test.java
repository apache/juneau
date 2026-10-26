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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.terminal.report.*;
import org.junit.jupiter.api.*;

class TerminalViewChecks_Test extends TestBase {

	@SuppressWarnings({
		"unchecked" // The JSON fixture maps each group name to a list of rows.
	})
	static List<List<Object>> rows(String group) throws IOException {
		try (var in = TerminalViewChecks_Test.class.getResourceAsStream("terminal-view-vectors.json")) {
			assertNotNull(in, "terminal-view-vectors.json missing from the test classpath");
			var m = (Map<String,List<List<Object>>>)Json.to(new String(in.readAllBytes(), StandardCharsets.UTF_8), Map.class);
			return m.get(group);
		}
	}

	@Test void a01_runId() throws Exception {
		for (var row : rows("runId"))
			assertEquals(row.get(1), TerminalViewChecks.isRunId((String)row.get(0)), () -> "runId " + row);
	}

	@Test void a02_token() throws Exception {
		for (var row : rows("token"))
			assertEquals(row.get(1), TerminalViewChecks.isToken((String)row.get(0)), () -> "token " + row);
	}

	@Test void a03_stepId() throws Exception {
		for (var row : rows("stepId"))
			assertEquals(row.get(1), TerminalViewChecks.isStepId((String)row.get(0)), () -> "stepId " + row);
	}

	@Test void a04_fw() throws Exception {
		for (var row : rows("fw"))
			assertEquals(row.get(1), TerminalViewChecks.isFw((String)row.get(0)), () -> "fw " + row);
	}

	@Test void a05_noteHref() throws Exception {
		for (var row : rows("noteHref"))
			assertEquals(row.get(1), TerminalViewChecks.isNoteHref((String)row.get(0)), () -> "noteHref " + row);
	}

	@Test void a06_rawHrefTemplate() throws Exception {
		for (var row : rows("rawHref"))
			assertEquals(row.get(1), TerminalViewChecks.isRawHrefTemplate((String)row.get(0)), () -> "rawHref " + row);
	}

	@Test void a07_nulls() {
		assertFalse(TerminalViewChecks.isRunId(null));
		assertFalse(TerminalViewChecks.isToken(null));
		assertFalse(TerminalViewChecks.isStepId(null));
		assertFalse(TerminalViewChecks.isFw(null));
		assertFalse(TerminalViewChecks.isNoteHref(null));
		assertFalse(TerminalViewChecks.isRawHrefTemplate(null));
		assertEquals("", TerminalViewChecks.firstLine(null));
	}

	@Test void a08_firstLine() {
		assertEquals("one", TerminalViewChecks.firstLine("one\ntwo"));
		assertEquals("one", TerminalViewChecks.firstLine("  one  "));
	}
}
