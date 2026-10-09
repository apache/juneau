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
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.console.test.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@code <@bridge>} directive: lowering to the contract, session resolution and the E-50/E-51 rejections.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class BridgeDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/sse") public View r0() { return FreemarkerView.of("admin/bus-bridge.ftlh"); }
		@RestGet(path="/ws") public View r1() { return FreemarkerView.of("admin/bus-bridge-ws.ftlh"); }
		@RestGet(path="/crossorigin") public View r2() { return FreemarkerView.of("admin/bus-bridge-crossorigin.ftlh"); }
		@RestGet(path="/transport") public View r3() { return FreemarkerView.of("admin/bus-bridge-transport.ftlh"); }
		@RestGet(path="/maxattempts") public View r4() { return FreemarkerView.of("admin/bus-bridge-maxattempts.ftlh"); }
		@RestGet(path="/outside") public View r5() { return FreemarkerView.of("admin/bus-bridge-outside.ftlh"); }
	}

	static String ok(String path) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(200);
			return rsp.getContent().asString();
		}
	}

	static void fails(String path, String message) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains(message), () -> body);
		}
	}

	@Test void a01_sse_sessionResolved() throws Exception {
		var a = PageContractAssert.assertPage(ok("/sse")).hasBridge("ops").declaresTopic("ops.jobs").hasNoWiringErrors();
		var b = (org.apache.juneau.marshall.collections.JsonMap)a.contract().getList("bridges").get(0);
		assertEquals("sse", b.getString("transport"));
		assertEquals("[\"ops.jobs\"]", b.getList("downstream").toString());
		var session = b.getString("session");
		assertFalse(session.startsWith("servlet:"), session);
		assertTrue(session.startsWith("/") && session.endsWith("/juneau-bus/session"), session);
		assertFalse(b.containsKey("upstream"));
	}

	@Test void a02_websocket_listsAndMaxAttempts() throws Exception {
		var b = PageContractAssert.assertPage(ok("/ws")).hasNoWiringErrors().contract().getList("bridges").get(0);
		assertEquals("{id:'ops',transport:'websocket',session:'/rest/ops/juneau-bus/session',downstream:['ops.jobs'],upstream:['ops.cancel-all'],maxAttempts:5}",
			Json5.DEFAULT.write(b));
	}

	@Test void b01_crossOrigin_isE51() throws Exception {
		fails("/crossorigin", "bridge 'ops': session 'https://elsewhere.example/s' must be a same-origin path or a servlet:/context: URI");
	}

	@Test void b02_badTransport_isE51() throws Exception {
		fails("/transport", "bridge 'ops': transport 'poll' must be sse or websocket");
	}

	@Test void b03_badMaxAttempts_isE51() throws Exception {
		fails("/maxattempts", "bridge 'ops': maxAttempts must be an integer; got 'many'");
	}

	@Test void b04_outside_isE50() throws Exception {
		fails("/outside", "<@bridge> must be inside <@console> or <@page>");
	}
}
