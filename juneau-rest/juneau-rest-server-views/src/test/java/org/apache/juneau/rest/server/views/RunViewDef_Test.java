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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class RunViewDef_Test extends TestBase {

	private static List<String> keysOf(Map<String,Object> m) {
		return new ArrayList<>(m.keySet());
	}

	@Test void a01_defaultsToMap() {
		var d = RunViewDef.create("run").eventsUrl("/juneau-run-view/42/events");
		assertEquals(List.of("eventsUrl", "refreshMs", "poll", "compact"), keysOf(d.toMap()));
		assertEquals(2000L, d.toMap().get("refreshMs"));
		assertEquals(true, d.toMap().get("poll"));
		assertEquals(false, d.toMap().get("compact"));
	}

	@Test void a02_refreshClampedToFloor() {
		assertEquals(1000L, RunViewDef.create("r").eventsUrl("/x").refreshMs(10).toMap().get("refreshMs"));
	}

	@Test void a03_allKeysInOrder() {
		var d = RunViewDef.create("r").eventsUrl("/x").title("T").rawHref("#raw-L{line}");
		assertEquals(RunViewDef.KEYS, keysOf(d.toMap()));
	}

	@Test void b01_eventsUrlMustBeSameOriginPath() {
		assertThrowsWithMessage(IllegalArgumentException.class, "eventsUrl", () -> RunViewDef.create("r").eventsUrl("https://evil.example/x").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "eventsUrl", () -> RunViewDef.create("r").eventsUrl("/a/../b").validate());
	}

	@Test void b02_idTemplateOnlyOnRowDetail() {
		assertThrowsWithMessage(IllegalArgumentException.class, "{id}", () -> RunViewDef.create("r").eventsUrl("/runs/{id}/events").validate());
		assertDoesNotThrow(() -> RunViewDef.create("r").type(RegionDef.TYPE_ROW_DETAIL).eventsUrl("/runs/{id}/events").validate());
	}

	@Test void b03_eventsUrlRequiredUnlessPollFalse() {
		assertThrowsWithMessage(IllegalArgumentException.class, "eventsUrl is required", () -> RunViewDef.create("r").validate());
		assertDoesNotThrow(() -> RunViewDef.create("r").poll(false).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "eventsUrl", () -> RunViewDef.create("r").poll(false).eventsUrl("//evil").validate());
	}

	@Test void b04_rawHrefTemplate() {
		assertDoesNotThrow(() -> RunViewDef.create("r").eventsUrl("/x").rawHref("#raw-L{line}").validate());
		assertDoesNotThrow(() -> RunViewDef.create("r").eventsUrl("/x").rawHref("#term-O{offset}").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "rawHref", () -> RunViewDef.create("r").eventsUrl("/x").rawHref("#raw-L").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "exactly one {line} or one {offset}", () -> RunViewDef.create("r").eventsUrl("/x").rawHref("#a{line}{offset}").validate());
	}

	@Test void b05_typeAndIdAndRefresh() {
		assertThrowsWithMessage(IllegalArgumentException.class, "type", () -> RunViewDef.create("r").eventsUrl("/x").type("bogus").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "id", () -> RunViewDef.create(" ").eventsUrl("/x").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "refreshMs", () -> RunViewDef.create("r").eventsUrl("/x").refreshMs(0).validate());
	}

	@Test void c01_fromMapRejectsUnknownKey() {
		assertThrowsWithMessage(IllegalArgumentException.class, "unknown", () -> RunViewDef.fromMap("r", Map.of("bogus", 1)));
		assertThrowsWithMessage(IllegalArgumentException.class, "unknown", () -> RunViewDef.fromMap("r", Map.of("fwLabels", "x")));
	}

	@Test void c02_roundTrip() {
		var d = RunViewDef.create("r").eventsUrl("/x").refreshMs(5000).poll(true).compact(true).title("T").rawHref("#raw-L{line}");
		assertEquals(d.toMap(), RunViewDef.fromMap("r", d.toMap()).toMap());
	}

	@Test void c03_fromMapTypeChecks() {
		assertThrowsWithMessage(IllegalArgumentException.class, "must be a string", () -> RunViewDef.fromMap("r", Map.of("eventsUrl", 1)));
		assertThrowsWithMessage(IllegalArgumentException.class, "must be an integer", () -> RunViewDef.fromMap("r", Map.of("refreshMs", "x")));
		assertThrowsWithMessage(IllegalArgumentException.class, "must be a boolean", () -> RunViewDef.fromMap("r", Map.of("poll", "x")));
		assertEquals(3000L, RunViewDef.fromMap("r", Map.of("refreshMs", 3000.0)).refreshMs);
	}

	@Test void d01_forMixin() {
		assertEquals("/app/juneau-run-view/{id}/events", RunViewDef.forMixin("r", "/app", "{id}").toMap().get("eventsUrl"));
		assertEquals("/app/juneau-run-view/r1/events", RunViewDef.forMixin("r", "/app/", "r1").toMap().get("eventsUrl"));
		assertThrowsWithMessage(IllegalArgumentException.class, "runIdTemplate", () -> RunViewDef.forMixin("r", "/app", "a b"));
	}

	@Test void d02_forMixinMatchesRoutes() {
		var m = RunViewDef.forMixin("x", "/petstore", "job-1").toMap();
		assertEquals("/petstore" + RunViewMixin.EVENTS_PATH.replace("{runId}", "job-1"), m.get("eventsUrl"));
	}

	@Test void d03_toRegionValidates() {
		var r = RunViewDef.create("run").eventsUrl("/x").title("T").rawHref("#raw-L{line}").toRegion();
		assertBean(r, "id,populate", "run,run-view");
		assertEquals(List.of(RunViewDef.POPULATOR), List.copyOf(r.allowedPopulators));
	}
}
