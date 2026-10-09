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
 * Tests for {@code subscribes=} / {@code publishes=} on {@code <@card>}: attribute and JSON5-body forms, R-10 at {@code </@console>} on FTL and mixed pages, and E-B7.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class CardDirective_Bus_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/wiring") public View r0() { return FreemarkerView.of("admin/bus-card-wiring.ftlh"); }
		@RestGet(path="/json5") public View r1() { return FreemarkerView.of("admin/bus-card-json5.ftlh"); }
		@RestGet(path="/both") public View r2() { return FreemarkerView.of("admin/bus-card-both.ftlh"); }
		@RestGet(path="/badkey") public View r3() { return FreemarkerView.of("admin/bus-card-badkey.ftlh"); }
		@RestGet(path="/badtopic") public View r4() { return FreemarkerView.of("admin/bus-card-badtopic.ftlh"); }
		@RestGet(path="/unwired") public View r5() { return FreemarkerView.of("admin/bus-card-unwired.ftlh"); }
		@RestGet(path="/noid") public View r6() { return FreemarkerView.of("admin/bus-card-noid.ftlh"); }
		@RestGet(path="/nested") public View x0() { return FreemarkerView.of("admin/bus-card-nested.ftlh"); }
		@RestGet(path="/retainhash") public View x1() { return FreemarkerView.of("admin/bus-card-retain-hash.ftlh"); }
		@RestGet(path="/retainstring") public View x2() { return FreemarkerView.of("admin/bus-card-retain-string.ftlh"); }
		@RestGet(path="/subnotopic") public View x3() { return FreemarkerView.of("admin/bus-card-sub-notopic.ftlh"); }
		@RestGet(path="/pubnotopic") public View x4() { return FreemarkerView.of("admin/bus-card-pub-notopic.ftlh"); }
		@RestGet(path="/whenemptybad") public View x5() { return FreemarkerView.of("admin/bus-card-whenempty-bad.ftlh"); }
		@RestGet(path="/bodypubs") public View x6() { return FreemarkerView.of("admin/bus-card-body-publishes.ftlh"); }
		@RestGet(path="/attrsubbodypub") public View x7() { return FreemarkerView.of("admin/bus-card-attr-sub-body-pub.ftlh"); }
		@RestGet(path="/bothpubs") public View x8() { return FreemarkerView.of("admin/bus-card-both-publishes.ftlh"); }
		@RestGet(path="/runviewwired") public View x9() { return FreemarkerView.of("admin/bus-card-runview-wired.ftlh"); }
		@RestGet(path="/topicbridgeonce") public View x10() { return FreemarkerView.of("admin/bus-page-topic-bridge-once.ftlh"); }
		@RestGet(path="/badge-refreshes") public View badgeRefreshes(RestRequest req) {
			return PageSpec.create().template("admin/bus-badge-refreshes.ftlh")
				.html("focus", "<p>Focus</p>", c -> c.publishes(TopicDecl.of("ssc.focus").retain(true)))
				.html("detail", "<p>Detail</p>", c -> c.subscribes(Subscription.to("ssc.focus").as("refresh")))
				.view(req);
		}
		@RestGet(path="/mixed") public View mixed(RestRequest req) {
			return PageSpec.create().template("admin/bus-mixed.ftlh")
				.html("focus", "<p>Focus</p>", c -> c.publishes(TopicDecl.of("ssc.focus").retain(true)))
				.view(req);
		}
		@RestGet(path="/mixed-unwired") public View mixedUnwired(RestRequest req) {
			return PageSpec.create().template("admin/bus-mixed-plain.ftlh")
				.html("lonely", "<p>Lonely</p>", c -> c.subscribes(Subscription.to("ssc.nobody").as("refresh")))
				.view(req);
		}
		@RestGet(path="/ref-wiring") public View refWiring(RestRequest req) {
			return PageSpec.create().template("admin/bus-card-ref-wiring.ftlh").html("focus", "<p>Focus</p>").view(req);
		}
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

	@Test void a01_attributes_shorthandAndHash() throws Exception {
		var a = PageContractAssert.assertPage(ok("/wiring"))
			.publishes("focus", "ssc.focus")
			.publishes("focus", "ssc.hot")
			.subscribes("detail", "ssc.focus", "refresh")
			.subscribes("hot", "ssc.hot", "refresh")
			.hasNoWiringErrors();
		assertEquals("[{topic:'ssc.focus',retain:true},{topic:'ssc.hot',retain:false}]",
			Json5.DEFAULT.write(a.card("focus").getList("publishes")));
		assertEquals("[{topic:'ssc.hot',as:'refresh',whenEmpty:'keep',emptyText:'Nothing hot.'}]",
			Json5.DEFAULT.write(a.card("hot").getList("subscribes")));
	}

	@Test void a02_json5Body_promotedToBaseKey() throws Exception {
		var a = PageContractAssert.assertPage(ok("/json5")).subscribes("detail", "ssc.focus", "refresh").hasNoWiringErrors();
		assertEquals(1, a.card("detail").getList("subscribes").size());
	}

	@Test void b01_bothAttributeAndBody_isE23() throws Exception {
		fails("/both", "body key 'subscribes' is reserved; set it as an attribute.");
	}

	@Test void b02_unknownHashKey() throws Exception {
		fails("/badkey", "<@card id='a'> subscribes entry has unknown key 'rol'; allowed: topic, as, map, whenEmpty, emptyText");
	}

	@Test void b03_badTopic_isE40() throws Exception {
		fails("/badtopic", "invalid topic 'selection:': expected family[:key] (see the topic syntax)");
	}

	@Test void b04_wiringErrors_failTheRender_R10() throws Exception {
		fails("/unwired", "card 'a' subscribes to 'ssc.nobody' but nothing publishes it");
	}

	@Test void b05_wiringWithoutId() throws Exception {
		fails("/noid", "<@card> with subscribes= or publishes= needs id=.");
	}

	@Test void a03_mixedPage_specPublishes_ftlSubscribes() throws Exception {
		PageContractAssert.assertPage(ok("/mixed"))
			.publishes("focus", "ssc.focus")
			.subscribes("detail", "ssc.focus", "refresh")
			.hasNoWiringErrors();
	}

	@Test void b06_mixedPage_specCardWiring_checkedAtConsoleClose() throws Exception {
		fails("/mixed-unwired", "card 'lonely' subscribes to 'ssc.nobody' but nothing publishes it");
	}

	@Test void b07_refWithWiring_isEB7() throws Exception {
		fails("/ref-wiring", "<@card ref='focus'> must not also set 'subscribes' or a body.");
	}

	@Test void b08_bothAttributeAndBody_exactText() throws Exception {
		fails("/both", "<@card id='detail'> body key 'subscribes' is reserved; set it as an attribute.");
		fails("/bothpubs", "<@card id='detail'> body key 'publishes' is reserved; set it as an attribute.");
	}

	@Test void b09_nestedWiredCard_hitsNestingErrorFirst() throws Exception {
		fails("/nested", "<@card> cannot be nested inside another <@card>.");
	}

	@Test void a04_publishesHash_withRetain() throws Exception {
		var a = PageContractAssert.assertPage(ok("/retainhash")).publishes("focus", "ssc.focus").publishes("focus", "ssc.hot")
			.subscribes("detail", "ssc.focus", "refresh").subscribes("detail", "ssc.hot", "refresh").hasNoWiringErrors();
		assertEquals("[{topic:'ssc.focus',retain:true},{topic:'ssc.hot',retain:false}]",
			Json5.DEFAULT.write(a.card("focus").getList("publishes")));
	}

	@Test void b10_nonBooleanRetain_namesTheCard() throws Exception {
		fails("/retainstring", "<@card id='focus'> publishes entry retain must be a boolean (true or false); got 'true'");
	}

	@Test void b11_hashWithoutTopic_isAClearError() throws Exception {
		fails("/subnotopic", "<@card id='a'> subscribes entry requires a topic");
		fails("/pubnotopic", "<@card id='a'> publishes entry requires a topic");
	}

	@Test void b12_badWhenEmpty() throws Exception {
		fails("/whenemptybad", "<@card id='a'> whenEmpty 'maybe' must be clear or keep");
	}

	@Test void a05_bodyOnlyPublishes() throws Exception {
		var a = PageContractAssert.assertPage(ok("/bodypubs")).publishes("focus", "ssc.focus")
			.subscribes("detail", "ssc.focus", "refresh").hasNoWiringErrors();
		assertEquals("[{topic:'ssc.focus',retain:true}]", Json5.DEFAULT.write(a.card("focus").getList("publishes")));
	}

	@Test void a06_attributeSubscribes_withBodyPublishes() throws Exception {
		PageContractAssert.assertPage(ok("/attrsubbodypub"))
			.subscribes("focus", "ssc.other", "refresh").publishes("focus", "ssc.focus")
			.subscribes("detail", "ssc.focus", "refresh").hasNoWiringErrors();
	}

	@Test void a07_runViewBody_acceptsSubscribes() throws Exception {
		PageContractAssert.assertPage(ok("/runviewwired")).subscribes("run", "ssc.focus", "refresh").hasNoWiringErrors();
	}

	@Test void a08_topicAndBridgeInPage_capturedOnce() throws Exception {
		var c = PageContractAssert.assertPage(ok("/topicbridgeonce")).hasBridge("ops").declaresTopic("ops.jobs").contract();
		assertEquals(1, c.getList("topics").size());
		assertEquals(1, c.getList("bridges").size());
	}

	@Test void a09_badgeRefreshes_cardWiredThroughSubscribes_passesBothChecks() throws Exception {
		var a = PageContractAssert.assertPage(ok("/badge-refreshes")).subscribes("detail", "ssc.focus", "refresh").hasNoWiringErrors();
		assertTrue(Json5.DEFAULT.write(a.contract()).contains("refreshes:['detail']"), () -> Json5.DEFAULT.write(a.contract()));
	}
}
