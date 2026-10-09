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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class BridgeDecl_Test extends TestBase {

	@Test void a01_sse_specExample() {
		var b = BridgeDecl.sse("ops", "servlet:/juneau-bus/session").downstream("ops.jobs", Topics.cmd("jobs"));
		assertEquals("{id:'ops',transport:'sse',session:'servlet:/juneau-bus/session',downstream:['ops.jobs','cmd:jobs']}",
			Json5.DEFAULT.write(b.toMap()));
	}

	@Test void a02_websocket_withUpstream_andMaxAttempts() {
		var b = BridgeDecl.websocket("ops", "/rest/ops/juneau-bus/session")
			.downstream("ops.jobs", "job:*").upstream("ops.cancel-all").maxAttempts(5);
		assertEquals("{id:'ops',transport:'websocket',session:'/rest/ops/juneau-bus/session',downstream:['ops.jobs','job:*'],upstream:['ops.cancel-all'],maxAttempts:5}",
			Json5.DEFAULT.write(b.toMap()));
	}

	@Test void a03_downstreamAndUpstreamAppend() {
		var b = BridgeDecl.websocket("ops", "context:/bus/session").downstream("ops.a").downstream("ops.b").upstream("ops.c").upstream("ops.d");
		assertEquals(List.of("ops.a", "ops.b"), List.copyOf(b.toMap().getList("downstream")));
		assertEquals(List.of("ops.c", "ops.d"), List.copyOf(b.toMap().getList("upstream")));
	}

	@Test void b01_badId() {
		var e = assertThrows(IllegalArgumentException.class, () -> BridgeDecl.sse("1ops", "/s"));
		assertEquals("bridge '1ops': id must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$", e.getMessage());
	}

	@Test void b02_crossOriginOrBadSession() {
		for (var s : new String[] {"https://elsewhere.example/s", "//elsewhere.example/s", "javascript:alert(1)", "session", "", "/\\evil"}) {
			var e = assertThrows(IllegalArgumentException.class, () -> BridgeDecl.sse("ops", s), s);
			assertEquals("bridge 'ops': session '" + s + "' must be a same-origin path or a servlet:/context: URI", e.getMessage());
		}
	}

	@Test void b03_upstreamOnSse() {
		var e = assertThrows(IllegalArgumentException.class, () -> BridgeDecl.sse("ops", "/s").upstream("ops.x"));
		assertEquals("bridge 'ops': upstream is allowed only on a websocket bridge", e.getMessage());
	}

	@Test void b04_bothDirections() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> BridgeDecl.websocket("ops", "/s").downstream("ops.x").upstream("ops.x").toMap());
		assertEquals("bridge 'ops': topic 'ops.x' is both downstream and upstream", e.getMessage());
	}

	@Test void b05_maxAttempts() {
		var e = assertThrows(IllegalArgumentException.class, () -> BridgeDecl.sse("ops", "/s").maxAttempts(0));
		assertEquals("bridge 'ops': maxAttempts must be >= 1; got 0", e.getMessage());
	}

	@Test void b06_noTopics() {
		var e = assertThrows(IllegalArgumentException.class, () -> BridgeDecl.sse("ops", "/s").toMap());
		assertEquals("bridge 'ops': carries no topics; add downstream(...) or upstream(...)", e.getMessage());
	}

	@Test void b07_badTopicSyntax_isE40() {
		var e = assertThrows(IllegalArgumentException.class, () -> BridgeDecl.sse("ops", "/s").downstream("Ops.Jobs"));
		assertEquals("invalid topic 'Ops.Jobs': expected family[:key] (see the topic syntax)", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> BridgeDecl.websocket("ops", "/s").upstream("ops.x:*"));
		assertEquals("invalid topic 'ops.x:*': expected family[:key] (see the topic syntax)", e.getMessage());
	}
}
