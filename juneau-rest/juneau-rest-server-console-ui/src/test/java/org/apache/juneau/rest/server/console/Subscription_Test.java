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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.Subscription.*;
import org.junit.jupiter.api.*;

class Subscription_Test extends TestBase {

	@Test void a01_masterDetail_specExample() {
		var s = Subscription.to(Topics.selection("changes")).as("params")
			.map("changeId", "ids.0").whenEmpty(WhenEmpty.CLEAR).emptyText("Select a change to see its tasks.");
		assertEquals("{topic:'selection:changes',as:'params',map:{changeId:'ids.0'},whenEmpty:'clear',emptyText:'Select a change to see its tasks.'}",
			Json5.DEFAULT.write(s.toMap()));
	}

	@Test void a02_minimal_omitsOptionalKeys() {
		assertEquals("{topic:'filter:changes',as:'filter'}",
			Json5.DEFAULT.write(Subscription.to(Topics.filter("changes")).as("filter").toMap()));
	}

	@Test void a03_mapIsRepeatable_andKeepsOrder() {
		var s = Subscription.to("app.region-picked").as("filter").map("columns.region", "region").map("columns.zone", "zone.0");
		assertEquals("{topic:'app.region-picked',as:'filter',map:{'columns.region':'region','columns.zone':'zone.0'}}",
			Json5.DEFAULT.write(s.toMap()));
	}

	@Test void a04_keep() {
		assertEquals("keep", Subscription.to("cmd:x").as("refresh").whenEmpty(WhenEmpty.KEEP).toMap().getString("whenEmpty"));
	}

	@Test void b01_badTopic_isE40() {
		var e = assertThrows(IllegalArgumentException.class, () -> Subscription.to("selection:"));
		assertEquals("invalid topic 'selection:': expected family[:key] (see the topic syntax)", e.getMessage());
		// Subscriptions are concrete: no wildcard.
		e = assertThrows(IllegalArgumentException.class, () -> Subscription.to("ssc.alert:*"));
		assertEquals("invalid topic 'ssc.alert:*': expected family[:key] (see the topic syntax)", e.getMessage());
		assertThrows(IllegalArgumentException.class, () -> Subscription.to(null));
	}

	@Test void b02_badRole() {
		var e = assertThrows(IllegalArgumentException.class, () -> Subscription.to("cmd:x").as("Refresh"));
		assertEquals("subscription to 'cmd:x': role 'Refresh' must match ^[a-z][a-z0-9-]{0,31}$", e.getMessage());
	}

	@Test void b03_badMapPath() {
		var e = assertThrows(IllegalArgumentException.class, () -> Subscription.to("cmd:x").as("params").map("id", "ids..0"));
		assertEquals("subscription to 'cmd:x': map path 'ids..0' must be a dotted path such as 'ids.0'", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> Subscription.to("cmd:x").as("params").map("", "ids.0"));
		assertEquals("subscription to 'cmd:x': map target '' must be a dotted name", e.getMessage());
	}

	@Test void b04_toMapWithoutRole() {
		var e = assertThrows(IllegalArgumentException.class, () -> Subscription.to("cmd:x").toMap());
		assertEquals("subscription to 'cmd:x' needs as(role)", e.getMessage());
	}
}
