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
package org.apache.juneau.rest.server.terminal;

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
		var d = TerminalDef.create("t").tailBytes(10).title("T").refreshMs(5).eventsUrl("/e").bytesUrl("/b").validate();
		assertEquals(List.of("bytesUrl", "eventsUrl", "refreshMs", "title", "tailBytes"), new ArrayList<>(d.toMap().keySet()));
		assertEquals(1000L, d.toMap().get("refreshMs"));
		assertEquals(Map.of("bytesUrl", "/b"), TerminalDef.create("t").bytesUrl("/b").validate().toMap());
	}

	@Test void a03_fromMap() {
		var d = TerminalDef.fromMap("t", Map.of("bytesUrl", "/b", "tailBytes", 4096.0, "refreshMs", 2000));
		assertEquals(4096L, d.tailBytes);
		assertEquals(2000L, d.refreshMs);
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalDef 't' unknown key 'rows'; allowed: [bytesUrl, eventsUrl, refreshMs, title, tailBytes].",
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
}
