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
 * Tests for the {@code <@topic>} directive: lowering to the contract and the E-45/E-46/E-50 rejections.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class TopicDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/topic") public View r0() { return FreemarkerView.of("admin/bus-topic.ftlh"); }
		@RestGet(path="/outside") public View r1() { return FreemarkerView.of("admin/bus-topic-outside.ftlh"); }
		@RestGet(path="/conflict") public View r2() { return FreemarkerView.of("admin/bus-topic-retain-conflict.ftlh"); }
		@RestGet(path="/framework") public View r3() { return FreemarkerView.of("admin/bus-topic-framework.ftlh"); }
		@RestGet(path="/noretain") public View r4() { return FreemarkerView.of("admin/bus-topic-noretain.ftlh"); }
		@RestGet(path="/nopublisher") public View r5() { return FreemarkerView.of("admin/bus-topic-nopublisher.ftlh"); }
		@RestGet(path="/badpublisher") public View r6() { return FreemarkerView.of("admin/bus-topic-badpublisher.ftlh"); }
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

	@Test void a01_topic_lowersToContract() throws Exception {
		var a = PageContractAssert.assertPage(ok("/topic"));
		a.declaresTopic("app.region-picked").subscribes("region", "app.region-picked", "refresh").hasNoWiringErrors();
		var t = (Map<?,?>)a.contract().getList("topics").get(0);
		assertEquals("{topic:'app.region-picked',retain:true,publisher:'script'}", Json5.DEFAULT.write(t));
	}

	@Test void b01_outsideConsoleOrPage_isE50() throws Exception {
		fails("/outside", "<@topic> must be inside <@console> or <@page>");
	}

	@Test void b02_retainConflict_isE46() throws Exception {
		fails("/conflict", "topic 'ops.jobs' is declared with retain=false here and retain=true at <@topic>");
	}

	@Test void b03_frameworkFamily_isE45() throws Exception {
		fails("/framework", "'selection' is a framework family; framework topics are implicit and may not be declared");
	}

	@Test void b04_retainRequired() throws Exception {
		fails("/noretain", "<@topic name='app.region-picked'> needs retain=true or retain=false");
	}

	@Test void b05_publisherRequired() throws Exception {
		fails("/nopublisher", "topic 'app.region-picked': a page-level topic needs publisher(SCRIPT|SERVER|RIBBON)");
	}

	@Test void b06_badPublisher() throws Exception {
		fails("/badpublisher", "<@topic name='app.region-picked'> publisher='browser' must be script, server or ribbon");
	}
}
