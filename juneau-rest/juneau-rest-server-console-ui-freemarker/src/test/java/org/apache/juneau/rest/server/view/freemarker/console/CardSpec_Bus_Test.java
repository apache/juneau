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
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.console.Subscription.*;
import org.junit.jupiter.api.*;

class CardSpec_Bus_Test extends TestBase {

	@Test void a01_subscribes_specExample() {
		var tasks = CardSpec.of("datatables", "tasks")
			.subscribes(Subscription.to(Topics.selection("changes")).as("params")
				.map("changeId", "ids.0").whenEmpty(WhenEmpty.CLEAR)
				.emptyText("Select a change to see its tasks."));
		assertEquals("{id:'tasks',type:'datatables',subscribes:[{topic:'selection:changes',as:'params',map:{changeId:'ids.0'},whenEmpty:'clear',emptyText:'Select a change to see its tasks.'}]}",
			Json5.DEFAULT.write(tasks.toMap()));
	}

	@Test void a02_repeatableAndAppending_baseKeysBeforeBody() {
		var c = CardSpec.of("kpi", "focus").title("Focus")
			.publishes(TopicDecl.of("ssc.focus").retain(true))
			.publishes(TopicDecl.of("ssc.hot").retain(false))
			.subscribes(Subscription.to("cmd:other").as("refresh"))
			.subscribes(Subscription.to("filter:changes").as("filter"))
			.body("value", 3);
		assertEquals("{id:'focus',type:'kpi',title:'Focus',publishes:[{topic:'ssc.focus',retain:true},{topic:'ssc.hot',retain:false}],subscribes:[{topic:'cmd:other',as:'refresh'},{topic:'filter:changes',as:'filter'}],value:3}",
			Json5.DEFAULT.write(c.toMap()));
	}

	@Test void a03_noWiring_noKeys() {
		assertEquals("{id:'x',type:'html'}", Json5.DEFAULT.write(CardSpec.html("x").toMap()));
	}

	@Test void b01_publishesRejectsPublisher() {
		var d = TopicDecl.of("ssc.focus").retain(true).publisher(TopicDecl.Publisher.SCRIPT);
		var e = assertThrows(IllegalArgumentException.class, () -> CardSpec.html("x").publishes(d));
		assertEquals("topic 'ssc.focus': publisher is page-level only; a card's publishes may not set it", e.getMessage());
	}

	@Test void b02_bodyCannotSetWiringKeys() {
		var e = assertThrows(IllegalArgumentException.class, () -> CardSpec.html("x").body("subscribes", java.util.List.of()));
		assertEquals("card 'x': 'subscribes' is a base key; use subscribes(...)", e.getMessage());
	}

	@Test void a04_copy_carriesWiring() {
		var c = CardSpec.html("x").subscribes(Subscription.to("cmd:x").as("refresh")).publishes(TopicDecl.of("ssc.focus").retain(true));
		var copy = c.copy();
		assertEquals(Json5.DEFAULT.write(c.toMap()), Json5.DEFAULT.write(copy.toMap()));
		var before = Json5.DEFAULT.write(c.toMap());
		copy.subscribes(Subscription.to("cmd:y").as("refresh")).publishes(TopicDecl.of("ssc.other").retain(false));
		assertEquals(before, Json5.DEFAULT.write(c.toMap()));
		assertNotEquals(before, Json5.DEFAULT.write(copy.toMap()));
	}

	@Test void b03_wiringRejectsNonMapEntries() {
		var e = assertThrows(IllegalArgumentException.class, () -> CardSpec.html("x").wiring(java.util.List.of("oops"), null));
		assertTrue(e.getMessage().contains("must be objects"), e.getMessage());
	}
}
