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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.console.TopicDecl.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

class PageSpec_Bus_Test extends TestBase {

	private static final CardDirectiveModel MODEL = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()), CardTypeRegistry.standard());

	private static PageSpec jobsPage() {
		return PageSpec.create()
			.topic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER))
			.bridge(BridgeDecl.sse("ops", "/rest/ops/juneau-bus/session").downstream("ops.jobs"))
			.html("jobs", "<p>jobs</p>", c -> c.subscribes(Subscription.to("ops.jobs").as("refresh")));
	}

	@Test void a01_topicAndBridge_specExample_wiresClean() {
		assertDoesNotThrow(() -> jobsPage().checkWiringEarly(CardTypeRegistry.standard(), null));
	}

	@Test void a02_applyTo_replaysTopicsAndBridges_andCopiesWiring() throws Exception {
		var cap = new PageCapture();
		jobsPage().applyTo(cap, MODEL);
		var c = JsonMap.ofString(cap.toContractJson());
		assertEquals("[{topic:'ops.jobs',retain:true,publisher:'server'}]", Json5.DEFAULT.write(c.getList("topics")));
		var b = (JsonMap)c.getList("bridges").get(0);
		assertEquals("ops", b.getString("id"));
		assertEquals("/rest/ops/juneau-bus/session", b.getString("session"));
		assertEquals("[{topic:'ops.jobs',as:'refresh'}]",
			Json5.DEFAULT.write(cap.unplacedCards().get(0).toMap().get("subscribes")));
	}

	@Test void a03_liftedTable_keepsWiring() throws Exception {
		var cap = new PageCapture();
		PageSpec.create()
			.table(TableSpec.create("tasks").dataUrl("/rest/tasks/data").columns(Column.create("pod")),
				c -> c.subscribes(Subscription.to(Topics.selection("changes")).as("params").map("changeId", "ids.0")))
			.applyTo(cap, MODEL);
		assertEquals("[{topic:'selection:changes',as:'params',map:{changeId:'ids.0'}}]",
			Json5.DEFAULT.write(cap.unplacedCards().get(0).toMap().get("subscribes")));
	}

	@Test void b01_retainConflict_isE46() {
		var spec = PageSpec.create().topic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER));
		var e = assertThrows(IllegalArgumentException.class,
			() -> spec.topic(TopicDecl.of("ops.jobs").retain(false).publisher(Publisher.SERVER)));
		assertEquals("topic 'ops.jobs' is declared with retain=false here and retain=true at PageSpec.topic", e.getMessage());
	}

	@Test void b03_publisherConflict_isE46() {
		var spec = PageSpec.create().topic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER));
		var e = assertThrows(IllegalArgumentException.class,
			() -> spec.topic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SCRIPT)));
		assertEquals("topic 'ops.jobs' is declared with publisher=script here and publisher=server at PageSpec.topic", e.getMessage());
	}

	@Test void b04_servletSession_withoutRequest_failsClearly() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> PageSpec.resolvedBridge(BridgeDecl.sse("ops", "servlet:/juneau-bus/session").downstream("ops.jobs"), null));
		assertEquals("BridgeDecl 'ops' session 'servlet:/juneau-bus/session' needs a request to resolve.", e.getMessage());
	}

	@Test void b02_duplicateBridge_isE51() {
		var spec = PageSpec.create().bridge(BridgeDecl.sse("ops", "/s").downstream("ops.a"));
		var e = assertThrows(IllegalArgumentException.class, () -> spec.bridge(BridgeDecl.sse("ops", "/t").downstream("ops.b")));
		assertEquals("bridge 'ops': duplicate id", e.getMessage());
	}

	@Test void c01_earlyR10_whenNoPageTemplate() {
		var spec = PageSpec.create().html("b", "<p/>", c -> c.subscribes(Subscription.to("ssc.focus").as("refresh")));
		var e = assertThrows(IllegalArgumentException.class, () -> spec.checkWiringEarly(CardTypeRegistry.standard(), null));
		assertTrue(e.getMessage().startsWith("card 'b' subscribes to 'ssc.focus' but nothing publishes it"), e.getMessage());
	}

	@Test void c02_earlyR10_seesTableRibbonTarget() {
		var spec = PageSpec.create().table(TableSpec.create("changes").dataUrl("/rest/changes/data").columns(Column.create("pod"))
			.ribbon(RibbonItem.refresh().title("Refresh tasks").target("nope")));
		var e = assertThrows(IllegalArgumentException.class, () -> spec.checkWiringEarly(CardTypeRegistry.standard(), null));
		assertEquals("topic 'cmd:nope' names card 'nope', which is not on this page", e.getMessage());
	}

	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	@Test void c04_servletSession_resolvesBeforeEarlyCheck() throws Exception {
		MockRestClient.buildLax(Host.class).get("/x").run();
		var req = Host.CAPTURED.get();
		var spec = PageSpec.create()
			.topic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER))
			.bridge(BridgeDecl.sse("ops", "servlet:/juneau-bus/session").downstream("ops.jobs"))
			.html("jobs", "<p>jobs</p>", c -> c.subscribes(Subscription.to("ops.jobs").as("refresh")));
		assertDoesNotThrow(() -> spec.checkWiringEarly(CardTypeRegistry.standard(), req));
		var session = (String)PageSpec.resolvedBridge(BridgeDecl.sse("ops", "servlet:/juneau-bus/session").downstream("ops.jobs"), req).get("session");
		assertEquals(req.getUriResolver().resolve("servlet:/juneau-bus/session"), session);
		assertTrue(session.startsWith("/") && session.endsWith("/juneau-bus/session"), session);
	}

	@Test void c03_earlyR10_skippedWithPageTemplate() {
		var spec = PageSpec.create().template("page.ftlh")
			.html("b", "<p/>", c -> c.subscribes(Subscription.to("ssc.focus").as("refresh")));
		assertDoesNotThrow(() -> spec.checkWiringEarly(CardTypeRegistry.standard(), null));
	}
}
