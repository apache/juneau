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
package org.apache.juneau.marshall.sse;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-targeted tests for {@link SseParserSession#doRead}'s target-type dispatch, which is not
 * otherwise exercised from this module (the broader SSE round-trip suite lives in
 * juneau-integration-tests).
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; casts to List<SseEvent> below.
})
class SseParserSession_Test {

	private static final String WIRE = "event: progress\ndata: step 1\nid: 42\nretry: 1000\n\n";

	@Test void a02_objectType_returnsListOfEvents() throws Exception {
		Object v = SseParser.DEFAULT.read(WIRE, Object.class);
		assertTrue(v instanceof List, "Expected List, got: " + v);
	}

	@Test void a03_arrayType_returnsArrayOfEvents() throws Exception {
		var v = SseParser.DEFAULT.read(WIRE, SseEvent[].class);
		assertEquals(1, v.length);
		assertBean(v[0], "event,data,id,retry", "progress,step 1,42,1000");
	}

	@Test void a04_singleSseEventType_returnsFirstEvent() throws Exception {
		var v = SseParser.DEFAULT.read(WIRE, SseEvent.class);
		assertBean(v, "event,data,id,retry", "progress,step 1,42,1000");
	}

	@Test void a05_singleSseEventType_noEvents_returnsNull() throws Exception {
		var v = SseParser.DEFAULT.read("", SseEvent.class);
		assertNull(v);
	}

	@Test void a06_listType_returnsListOfEvents() throws Exception {
		// List is itself a Collection subtype, so this actually takes doRead's earlier
		// "type.isCollectionOrArray()" branch (not the dedicated "List.class.isAssignableFrom(...)"
		// check further down, which is unreachable dead code -- see the HTT marker at that line).
		List<SseEvent> v = (List<SseEvent>) SseParser.DEFAULT.read(WIRE, List.class, SseEvent.class);
		assertEquals(1, v.size());
		assertBean(v.get(0), "event,data,id,retry", "progress,step 1,42,1000");
	}

	@Test void a07_unsupportedTargetType_throwsParseException() {
		assertThrows(ParseException.class, () -> SseParser.DEFAULT.read(WIRE, String.class));
	}

	@Test void a08_isRecordStreaming_false() {
		var session = SseParser.DEFAULT.createSession().build();
		assertFalse(session.isRecordStreaming());
	}

	@Test void a09_readRecords_delegatesToArrayReader() throws Exception {
		var session = SseParser.DEFAULT.createSession().build();
		try (var records = session.readRecords(WIRE)) {
			assertNotNull(records);
		}
	}
}
