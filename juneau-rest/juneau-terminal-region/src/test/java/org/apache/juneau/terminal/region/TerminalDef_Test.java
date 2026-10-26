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
package org.apache.juneau.terminal.region;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class TerminalDef_Test extends TestBase {

	@Test void a01_forMixin() {
		assertEquals("/runs/juneau-terminal/r42/bytes", TerminalDef.forMixin("t", "/runs/", "r42").bytesUrl);
		assertEquals("/juneau-terminal/r42/bytes", TerminalDef.forMixin("t", null, "r42").bytesUrl);
		assertThrowsWithMessage(IllegalArgumentException.class, "terminalId must match", () -> TerminalDef.forMixin("t", "", "a/b"));
	}

	@Test void a02_toMapOrderClampAndOmitsUnset() {
		var d = TerminalDef.create("t").inputId("t1").input(true).tailBytes(10).title("T").refreshMs(5).eventsUrl("/e").bytesUrl("/b").terminal(Map.of("convertEol", true)).validate();
		assertEquals(List.of("bytesUrl", "eventsUrl", "refreshMs", "title", "tailBytes", "input", "inputId", "terminal"), new ArrayList<>(d.toMap().keySet()));
		assertEquals(1000L, d.toMap().get("refreshMs"));
		assertEquals(Map.of("bytesUrl", "/b"), TerminalDef.create("t").bytesUrl("/b").validate().toMap());
	}

	@Test void a03_fromMap() {
		var d = TerminalDef.fromMap("t", Map.of("bytesUrl", "/b", "tailBytes", 4096.0, "refreshMs", 2000));
		assertEquals(4096L, d.tailBytes);
		assertEquals(2000L, d.refreshMs);
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' unknown key 'rows'; allowed: [bytesUrl, eventsUrl, refreshMs, title, tailBytes, input, inputId, terminal].",
			() -> TerminalDef.fromMap("t", Map.of("rows", 1)));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' bytesUrl must be a string.", () -> TerminalDef.fromMap("t", Map.of("bytesUrl", 1)));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' tailBytes must be an integer.", () -> TerminalDef.fromMap("t", Map.of("tailBytes", 1.5)));
	}

	@Test void a04_validate() {
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' bytesUrl is required.", () -> TerminalDef.create("t").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "bytesUrl must be a same-origin path", () -> TerminalDef.create("t").bytesUrl("https://x/b").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "eventsUrl must be a same-origin path", () -> TerminalDef.create("t").bytesUrl("/b").eventsUrl("//x/e").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "bytesUrl must not contain a '{' placeholder", () -> TerminalDef.create("t").bytesUrl("/t/{id}/bytes").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "refreshMs must be > 0", () -> TerminalDef.create("t").bytesUrl("/b").refreshMs(0).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "tailBytes must be 1..1073741824; got 0.", () -> TerminalDef.create("t").bytesUrl("/b").tailBytes(0).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "tailBytes must be 1..1073741824", () -> TerminalDef.create("t").bytesUrl("/b").tailBytes(TerminalDef.MAX_TAIL_BYTES + 1).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef id must not be blank.", () -> TerminalDef.create(" ").bytesUrl("/b").validate());
		assertNotNull(TerminalDef.create("t").bytesUrl("/b").tailBytes(TerminalDef.MAX_TAIL_BYTES).validate());
	}

	@Test void a05_inputNeedsInputId() {
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' input needs inputId.",
			() -> TerminalDef.create("t").bytesUrl("/b").input(true).validate());
		assertNotNull(TerminalDef.create("t").bytesUrl("/b").input(false).validate());
		assertNotNull(TerminalDef.create("t").bytesUrl("/b").input(true).inputId("t1").validate());
	}

	@Test void a06_inputIdChecked() {
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' inputId must match [A-Za-z0-9_-]{1,128}; got '../x'.",
			() -> TerminalDef.create("t").bytesUrl("/b").inputId("../x").validate());
	}

	@Test void a07_forMixinSetsInputId() {
		var d = TerminalDef.forMixin("card", "/runs", "t1");
		assertEquals("t1", d.inputId);
		assertNull(d.input);
		assertFalse(d.toMap().containsKey("input"));
	}

	@Test void a08_inputMustBeBoolean() {
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' input must be a boolean.",
			() -> TerminalDef.fromMap("t", Map.of("input", "yes")));
		assertEquals(Boolean.TRUE, TerminalDef.fromMap("t", Map.of("input", true, "inputId", "t1")).input);
	}

	@Test void a09_terminalOptionsRoundTrip() {
		var d = TerminalDef.fromMap("t", Map.of("bytesUrl", "/b", "terminal", Map.of("convertEol", true, "scrollback", 500, "fontFamily", "monospace"))).validate();
		assertEquals(Boolean.TRUE, d.terminal.get("convertEol"));
		assertEquals(500L, d.terminal.get("scrollback"));
		assertEquals("monospace", d.terminal.get("fontFamily"));
		assertEquals(List.of("convertEol", "scrollback", "fontFamily"), new ArrayList<>(((Map<?,?>)d.toMap().get("terminal")).keySet()));
		var built = TerminalDef.create("t").bytesUrl("/b").terminal(Map.of("convertEol", false)).validate();
		assertEquals(Map.of("convertEol", false), built.toMap().get("terminal"));
		assertEquals(List.of("bytesUrl", "terminal"), new ArrayList<>(built.toMap().keySet()));
		assertFalse(TerminalDef.create("t").bytesUrl("/b").validate().toMap().containsKey("terminal"));
	}

	@Test void a10_terminalOptionsStrict() {
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal must be an object.",
			() -> TerminalDef.fromMap("t", Map.of("terminal", "x")));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal unknown key 'rows'; allowed: [convertEol, scrollback, fontFamily].",
			() -> TerminalDef.fromMap("t", Map.of("terminal", Map.of("rows", 3))));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.convertEol must be a boolean.",
			() -> TerminalDef.fromMap("t", Map.of("terminal", Map.of("convertEol", "yes"))));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.scrollback must be an integer.",
			() -> TerminalDef.fromMap("t", Map.of("terminal", Map.of("scrollback", 1.5))));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.fontFamily must be a non-empty string.",
			() -> TerminalDef.fromMap("t", Map.of("terminal", Map.of("fontFamily", ""))));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.fontFamily must be a non-empty string.",
			() -> TerminalDef.fromMap("t", Map.of("terminal", Map.of("fontFamily", 3))));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.scrollback must be an integer.",
			() -> TerminalDef.fromMap("t", Map.of("terminal", Map.of("scrollback", true))));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.scrollback must be an integer.",
			() -> TerminalDef.fromMap("t", Map.of("terminal", Map.of("scrollback", 1e300))));
	}

	@Test void a11_terminalScrollbackRange() {
		assertNotNull(TerminalDef.create("t").bytesUrl("/b").terminal(Map.of("scrollback", 0)).validate());
		assertNotNull(TerminalDef.create("t").bytesUrl("/b").terminal(Map.of("scrollback", 1_000_000)).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.scrollback must be 0..1000000; got -1.",
			() -> TerminalDef.create("t").bytesUrl("/b").terminal(Map.of("scrollback", -1)).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.scrollback must be 0..1000000; got 1000001.",
			() -> TerminalDef.create("t").bytesUrl("/b").terminal(Map.of("scrollback", 1_000_001)).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' terminal.convertEol must be a boolean.",
			() -> TerminalDef.create("t").bytesUrl("/b").terminal(Map.of("convertEol", 1)).validate());
	}
}
