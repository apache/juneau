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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class ConsoleOutputCardType_Test extends TestBase {

	private static IllegalArgumentException fail(String body) {
		var b = CardSource.create("console-output", "log");
		if (body != null)
			b.body(body);
		return assertThrows(IllegalArgumentException.class, () -> new ConsoleOutputCardType().toFragment(b.build()));
	}

	@Test void a01_minimal() {
		var frag = new ConsoleOutputCardType().toFragment(
			CardSource.create("console-output", "log").body("{contractVersion:'1', output:{linesUrl:'/logs/1/lines'}}").build());
		assertEquals(java.util.Set.of("output"), frag.keySet());
	}

	@Test void a02_errors() {
		assertEquals("<@card id='log'> type='console-output' requires a JSON5 body { contractVersion: '1', output: {...} }.", fail(null).getMessage());
		assertEquals("<@card id='log'> type='console-output' requires contractVersion: '1'; got 'null'.", fail("{output:{}}").getMessage());
		assertEquals("<@card id='log'> type='console-output' unknown key 'poll'; allowed: contractVersion, output.",
			fail("{contractVersion:'1', output:{}, poll:1}").getMessage());
		assertEquals("<@card id='log'> type='console-output' requires an output object.", fail("{contractVersion:'1'}").getMessage());
	}
}
