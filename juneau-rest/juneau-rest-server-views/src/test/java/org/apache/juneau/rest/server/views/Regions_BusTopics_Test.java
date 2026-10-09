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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for a region's view of the PAGE bus ({@code juneau-bus.js}) through {@code ctx.publish} /
 * {@code ctx.subscribe}: the region key as owner, self-echo, the "message" re-populate and its
 * {@code messageSignal}, the per-invocation sweep, teardown, the separation from the region bus, and E-JS-46.
 *
 * <p>
 * The behavioral half runs the real runtimes under a DOM shim (see {@code src/test/js/regions-bus-topics.cjs}),
 * with {@code juneau-bus.js} loaded first.
 */
class Regions_BusTopics_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWithBus("regions-bus-topics.cjs");
		assumeTrue(r != null, "node not available or regions-bus-topics.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	private static int num(Map<?,?> r, String key) {
		return ((Number)r.get(key)).intValue();
	}

	/**
	 * GC-B1: the framework message builders and {@code emitFramework} are gone (their payloads are the page bus's
	 * {@code selection:}/{@code detail:}/{@code redraw:} topics now), the page bus is reached only through the owner
	 * handle, and a page-bus delivery never raises the region bus's fan-out depth.
	 */
	@Test void a01_sourceShape() throws Exception {
		var body = RegionsHarness.regionsJs();
		for (var gone : List.of("emitFramework", "selectionChangedMessage", "detailToggledMessage", "tableRedrewMessage"))
			assertFalse(body.contains(gone), () -> gone + " must be deleted (GC-B1)");
		assertTrue(body.contains("NS.bus.owner(region.key)"), "the owner of a region's page-bus traffic is its key");
		assertEquals(2, body.split("bus\\.fanOutDepth\\+\\+", -1).length - 1,
			"only the region bus's own fan-out and replay raise bus.fanOutDepth");
		assertTrue(body.contains("pageBusDepth++"), body);
	}

	@Test void b01_publishCarriesTheRegionKey() {
		var r = report();
		assertEquals("map/picker", r.get("t01_key"));
		assertEquals(true, r.get("t01_returned"), r::toString);
		assertEquals(List.of(Map.of("p", Map.of("region", "east"), "from", "map/picker")), r.get("t01_got"), r::toString);
		assertEquals(Map.of("region", "east"), r.get("t01_retained"), r::toString);
		assertAllTrue(r, "t01_noErrors");
	}

	@Test void b02_aRegionDoesNotHearItselfUnlessEcho() {
		var r = report();
		assertEquals("2", r.get("t02_a"), "a hears c but not itself");
		assertEquals("1,2", r.get("t02_b"), r::toString);
		assertEquals("1,2", r.get("t02_c"), "{echo:true} hears its own publish");
	}

	/** Decision (i): the "message" reason and messageSignal survive removal of the region bus because of this test. */
	@Test void b03_aSubscribeHandlerRefreshIsAMessageRepopulate() {
		var r = report();
		assertEquals(List.of("initial", "message", "message"), r.get("t03_reasons"), r::toString);
		assertEquals(List.of(false, false), r.get("t03_liveAtRun"), "messageSignal is live while its populate runs");
		assertEquals("-,1,2", r.get("t03_probeSeen"), "the populate reads the newest value with bus.get");
		assertEquals("juneau:superseded", r.get("t03_firstAbortReason"),
			"the next delivery supersedes the previous messageSignal before the re-populate tears it down");
		assertAllTrue(r, "t03_initialSignalNull", "t03_latestLive", "t03_quiet");
	}

	@Test void b04_theDeliveryDepthUnwinds() {
		var r = report();
		assertEquals(List.of("initial", "refresh"), r.get("t04_reasons"), "a refresh after the delivery is a refresh");
		assertEquals(1, num(r, "t04_e45"), r::toString);
		assertAllTrue(r, "t04_e45NamesOwner");
	}

	@Test void b05_subscriptionsAreSweptOnRepopulate() {
		var r = report();
		assertEquals(1, num(r, "t05_deliveries"), r::toString);
		assertEquals(1, num(r, "t05_subscribers"), r::toString);
	}

	@Test void b06_teardownDisposesTheOwner() {
		var r = report();
		assertEquals("grid1", r.get("t06_ownerBefore"));
		assertEquals("r1", r.get("t06_idsBefore"));
		assertTrue(r.containsKey("t06_ownerAfter"), r::toString);
		assertNull(r.get("t06_ownerAfter"), "the claim is released");
		assertEquals(false, r.get("t06_lateReturned"));
		assertAllTrue(r, "t06_heardNothing", "t06_valueGone", "t06_lateWarned", "t06_lateSubscribeIsNoop",
			"t06_stillNothing");
	}

	@Test void b07_theTwoBusesAreSeparate() {
		var r = report();
		assertEquals(List.of("legacy"), r.get("t07_onGot"), "ctx.publish never reaches ctx.on");
		assertEquals(List.of("app.note"), r.get("t07_historyTopics"), "ctx.emit never reaches the page bus");
	}

	@Test void b08_missingBusIsEJs46() {
		var r = report();
		var msg = "E-JS-46: page wires topics but juneau-bus.js is not loaded";
		assertEquals(msg, r.get("t08_publish"));
		assertEquals(msg, r.get("t08_subscribe"));
		assertEquals(2, num(r, "t08_logged"), r::toString);
		assertAllTrue(r, "t08_plainQuiet", "t08_namesRegion", "t08_busAbsent");
	}
}
