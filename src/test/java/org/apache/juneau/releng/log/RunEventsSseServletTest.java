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

package org.apache.juneau.releng.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class RunEventsSseServletTest {

	@Test
	void a01_streamsPublishedEventsAsSseData() throws Exception {
		var subscribed = new CountDownLatch(1);
		var bc = new EventBroadcaster() {
			@Override
			public AutoCloseable subscribe(Consumer<String> sink) {
				var subscription = super.subscribe(sink);
				subscribed.countDown();
				return subscription;
			}
		};
		var out = new StringWriter();
		var pw = new PrintWriter(out, true) {
			@Override
			public boolean checkError() {
				return out.toString().contains("\"ended\""); // the client "disconnects" after the ended event
			}
		};
		var req = mock(HttpServletRequest.class);
		when(req.getPathInfo()).thenReturn("/runs");
		var resp = mock(HttpServletResponse.class);
		when(resp.getWriter()).thenReturn(pw);
		var t = new Thread(() -> new RunEventsSseServlet(bc).doGet(req, resp));
		t.start();
		assertTrue(subscribed.await(5, TimeUnit.SECONDS), "servlet never subscribed");
		bc.publish("process", Map.of("state", "ended"));
		t.join(TimeUnit.SECONDS.toMillis(5));
		assertFalse(t.isAlive());
		assertTrue(out.toString().startsWith("data: {\"type\":\"process\""), out.toString());
		verify(resp).setContentType("text/event-stream");
		verify(resp).flushBuffer();
		assertEquals(0, bc.subscribers(), "unsubscribed on disconnect");
	}

	@Test
	void a02_otherPathsAre404() {
		var req = mock(HttpServletRequest.class);
		when(req.getPathInfo()).thenReturn("/9.2.1/state");
		var resp = mock(HttpServletResponse.class);
		new RunEventsSseServlet(new EventBroadcaster()).doGet(req, resp);
		verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
	}
}
