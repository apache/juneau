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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests that line breaks in SSE field values cannot introduce additional fields or events.
 */
class SseSerializer_LineBreaks_Test extends TestBase {

	private static String write(SseEvent e) throws Exception {
		return SseSerializer.DEFAULT.write(e);
	}

	@Test void a01_eventAndIdLineBreaksRemoved() throws Exception {
		var e = new SseEvent().setEvent("a\r\nb\nc\rd").setData("x").setId("1\n2\r3");
		assertEquals("event: abcd\ndata: x\nid: 123\n\n", write(e));
	}

	@Test void a02_dataSplitOnAllLineBreaks() throws Exception {
		var e = new SseEvent().setData("a\rb\r\nc\nd");
		assertEquals("data: a\ndata: b\ndata: c\ndata: d\n\n", write(e));
	}
}
