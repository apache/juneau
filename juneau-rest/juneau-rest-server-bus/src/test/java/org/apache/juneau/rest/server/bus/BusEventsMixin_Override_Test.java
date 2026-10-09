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
package org.apache.juneau.rest.server.bus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * An application guard on the session POST: an override of {@link BusEventsMixin#openBusSession} that calls
 * {@code BusEventsMixin.super.openBusSession}.
 *
 * <p>
 * Pins that re-declaring {@code @RestPost(path=SESSION_PATH)} on the override registers the route once, and that an
 * override without the annotation still inherits it.
 */
@SuppressWarnings({
	"resource" // The shared bus and MockRestClients live for the whole class; the bus is closed in @AfterAll.
})
class BusEventsMixin_Override_Test extends TestBase {

	static final ServerBus BUS = ServerBus.create(BusPolicy.create().downstream("ops.jobs", true).build());
	static final AtomicInteger CALLS = new AtomicInteger();
	static final String BODY = "{\"v\":1,\"bridge\":\"ops\",\"transport\":\"sse\",\"downstream\":[\"ops.jobs\"]}";

	/** Lists the Java methods behind every registered operation, as {@code DeclaringClass.method}. */
	static List<String> ops(RestRequest req) {
		return req.getContext().getRestOperations().getOpContexts().stream()
			.map(o -> o.getJavaMethod().getDeclaringClass().getSimpleName() + "." + o.getJavaMethod().getName())
			.toList();
	}

	@Rest
	public static class Guarded extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;

		@Override public ServerBus serverBus() { return BUS; }

		@Override
		@RestPost(path=SESSION_PATH)
		public JsonMap openBusSession(RestRequest req, RestResponse res) {
			CALLS.incrementAndGet();
			if (! "yes".equals(req.getHttpServletRequest().getHeader("X-App-Guard"))) {
				res.setStatus(403);
				return JsonMap.of("code", "app:guard", "message", "refused by the application guard");
			}
			return BusEventsMixin.super.openBusSession(req, res);
		}

		@RestGet("/ops")
		public List<String> listOps(RestRequest req) {
			return ops(req);
		}
	}

	@Rest
	public static class Plain extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;

		@Override public ServerBus serverBus() { return BUS; }

		@Override
		public JsonMap openBusSession(RestRequest req, RestResponse res) {
			CALLS.incrementAndGet();
			return BusEventsMixin.super.openBusSession(req, res);
		}

		@RestGet("/ops")
		public List<String> listOps(RestRequest req) {
			return ops(req);
		}
	}

	private static final MockRestClient GUARDED = MockRestClient.buildLax(Guarded.class);
	private static final MockRestClient PLAIN = MockRestClient.buildLax(Plain.class);

	@AfterAll static void closeBus() {
		BUS.close();
	}

	@BeforeEach void reset() {
		CALLS.set(0);
	}

	@SuppressWarnings("unchecked")
	private static List<String> sessionOps(MockRestClient c) throws Exception {
		var text = c.get("/ops").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		return ((List<String>) Json.to(text, List.class)).stream().filter(x -> x.endsWith(".openBusSession")).toList();
	}

	@Test void a01_aReDeclaredRestPostRegistersOnce() throws Exception {
		assertEquals(List.of("Guarded.openBusSession"), sessionOps(GUARDED));
	}

	@Test void a02_theGuardRunsOncePerRequest() throws Exception {
		GUARDED.post(BusEventsMixin.SESSION_PATH).contentString(BODY).contentType("application/json")
			.header("Accept", "application/json").run().assertStatus(403);
		assertEquals(1, CALLS.get());

		var text = GUARDED.post(BusEventsMixin.SESSION_PATH).contentString(BODY).contentType("application/json")
			.header("Accept", "application/json").header("X-App-Guard", "yes").run().assertStatus(200).getContent().asString();
		assertEquals(2, CALLS.get());
		assertTrue(text.contains("sessionId"), text);
	}

	@Test void b01_anUnannotatedOverrideInheritsTheRoute() throws Exception {
		assertEquals(List.of("Plain.openBusSession"), sessionOps(PLAIN));
		PLAIN.post(BusEventsMixin.SESSION_PATH).contentString(BODY).contentType("application/json")
			.header("Accept", "application/json").run().assertStatus(200);
		assertEquals(1, CALLS.get());
	}
}
