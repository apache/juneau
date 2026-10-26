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

import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class TerminalAccess_Test extends TestBase {

	private static final TerminalCaller CALLER = new TerminalCaller() {
		@Override public String principalName() { return "alice"; }
		@Override public Object attribute(String name) { return null; }
	};

	@Test void a01_lambdaIsWatchOnly() {
		TerminalAccess a = (caller, id) -> true;
		assertEquals(Optional.empty(), a.inputSink(CALLER, "t1"));
		assertTrue(a.canWatch(CALLER, "t1"));
	}

	@Test void a02_overrideYieldsSink() throws Exception {
		var written = new ArrayList<byte[]>();
		TerminalSink sink = new TerminalSink() {
			@Override public boolean acceptsInput() { return true; }
			@Override public boolean write(byte[] bytes) { return written.add(bytes); }
		};
		TerminalAccess a = new TerminalAccess() {
			@Override public boolean canWatch(TerminalCaller caller, String terminalId) { return "t1".equals(terminalId); }
			@Override public Optional<TerminalSink> inputSink(TerminalCaller caller, String terminalId) {
				return "t1".equals(terminalId) ? Optional.of(sink) : Optional.empty();
			}
		};
		assertTrue(a.inputSink(CALLER, "t1").orElseThrow().write(new byte[]{'x'}));
		assertSize(1, written);
		assertTrue(a.inputSink(CALLER, "t2").isEmpty());
		assertFalse(a.canWatch(CALLER, "t2"));
	}
}
