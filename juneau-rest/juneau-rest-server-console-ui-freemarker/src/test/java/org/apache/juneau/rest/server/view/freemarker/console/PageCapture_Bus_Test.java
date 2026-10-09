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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.console.TopicDecl.*;
import org.junit.jupiter.api.*;

class PageCapture_Bus_Test extends TestBase {

	private static JsonMap contract(PageCapture cap) throws Exception {
		return JsonMap.ofString(cap.toContractJson());
	}

	@Test void a01_noTopicsOrBridges_noKeys() throws Exception {
		var c = contract(new PageCapture());
		assertFalse(c.containsKey("topics"));
		assertFalse(c.containsKey("bridges"));
	}

	@Test void a02_topicsAndBridges_inDeclarationOrder() throws Exception {
		var cap = new PageCapture();
		cap.addTopic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER).toMap(), "PageSpec.topic");
		cap.addTopic(TopicDecl.of("app.region-picked").retain(true).publisher(Publisher.SCRIPT).toMap(), "<@topic>");
		cap.addBridge(BridgeDecl.sse("ops", "/rest/ops/juneau-bus/session").downstream("ops.jobs").toMap());
		var c = contract(cap);
		assertEquals("[{topic:'ops.jobs',retain:true,publisher:'server'},{topic:'app.region-picked',retain:true,publisher:'script'}]",
			org.apache.juneau.marshall.marshaller.Json5.DEFAULT.write(c.getList("topics")));
		var b = (JsonMap)c.getList("bridges").get(0);
		assertEquals("ops", b.getString("id"));
		assertEquals("/rest/ops/juneau-bus/session", b.getString("session"));
	}

	@Test void a04_resolvedSession_isWhatTheContractCarries() throws Exception {
		var cap = new PageCapture();
		var m = BridgeDecl.sse("ops", "servlet:/juneau-bus/session").downstream("ops.jobs").toMap();
		m.put("session", "/app/juneau-bus/session");
		cap.addBridge(m);
		assertEquals("/app/juneau-bus/session", ((JsonMap)contract(cap).getList("bridges").get(0)).getString("session"));
	}

	@Test void a03_sameTopicSameRetain_isUnion() throws Exception {
		var cap = new PageCapture();
		cap.addTopic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER).toMap(), "PageSpec.topic");
		cap.addTopic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER).toMap(), "<@topic>");
		assertEquals(1, contract(cap).getList("topics").size());
	}

	@Test void b01_retainConflict_isE46() {
		var cap = new PageCapture();
		cap.addTopic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER).toMap(), "PageSpec.topic");
		var e = assertThrows(IllegalArgumentException.class,
			() -> cap.addTopic(TopicDecl.of("ops.jobs").retain(false).publisher(Publisher.SERVER).toMap(), "<@topic>"));
		assertEquals("topic 'ops.jobs' is declared with retain=false here and retain=true at PageSpec.topic", e.getMessage());
	}

	@Test void b03_publisherConflict_isE46() {
		var cap = new PageCapture();
		cap.addTopic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER).toMap(), "PageSpec.topic");
		var e = assertThrows(IllegalArgumentException.class,
			() -> cap.addTopic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SCRIPT).toMap(), "<@topic>"));
		assertEquals("topic 'ops.jobs' is declared with publisher=script here and publisher=server at PageSpec.topic", e.getMessage());
	}

	@Test void b02_duplicateBridge_isE51() {
		var cap = new PageCapture();
		cap.addBridge(BridgeDecl.sse("ops", "/s").downstream("ops.a").toMap());
		var e = assertThrows(IllegalArgumentException.class, () -> cap.addBridge(BridgeDecl.sse("ops", "/t").downstream("ops.b").toMap()));
		assertEquals("bridge 'ops': duplicate id", e.getMessage());
	}

	@Test void c01_checkWiring_reportsFirstProblem() {
		var cap = new PageCapture();
		cap.addTopic(TopicDecl.of("ops.jobs").retain(true).publisher(Publisher.SERVER).toMap(), "PageSpec.topic");
		// R-11: a server topic that no bridge carries.
		var e = assertThrows(IllegalArgumentException.class, () -> cap.checkWiring(CardTypeRegistry.standard()));
		assertEquals("topic 'ops.jobs' is declared publisher=server but no bridge carries it downstream", e.getMessage());
	}
}
