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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class TerminalCardType_Test extends TestBase {

	private static IllegalArgumentException fail(String body) {
		var b = CardSource.create("terminal", "build");
		if (body != null)
			b.body(body);
		return assertThrows(IllegalArgumentException.class, () -> new TerminalCardType().toFragment(b.build()));
	}

	@Test void a01_minimal() {
		var frag = new TerminalCardType().toFragment(
			CardSource.create("terminal", "build").body("{contractVersion:'1', terminal:{bytesUrl:'/t/juneau-terminal/r1/bytes'}}").build());
		assertEquals(Map.of("terminal", Map.of("bytesUrl", "/t/juneau-terminal/r1/bytes")), frag);
	}

	@Test void a02_errors() {
		assertEquals("<@card id='build'> type='terminal' requires a JSON5 body { contractVersion: '1', terminal: {...} }.", fail(null).getMessage());
		assertEquals("<@card id='build'> type='terminal' requires contractVersion: '1'; got 'null'.", fail("{terminal:{}}").getMessage());
		assertEquals("<@card id='build'> type='terminal' unknown key 'output'; allowed: contractVersion, terminal.",
			fail("{contractVersion:'1', terminal:{}, output:{}}").getMessage());
		assertEquals("<@card id='build'> type='terminal' requires a terminal object.", fail("{contractVersion:'1'}").getMessage());
		assertEquals("TerminalDef 'build' bytesUrl is required.", fail("{contractVersion:'1', terminal:{}}").getMessage());
	}
}
