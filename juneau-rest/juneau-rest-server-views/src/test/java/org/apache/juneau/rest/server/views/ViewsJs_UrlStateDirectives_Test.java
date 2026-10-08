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

/** Node behavioral checks for the {@code ?state=} directive registry, the {@code window}/{@code ids} built-ins, and their live wire. */
class ViewsJs_UrlStateDirectives_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("urlstate-directives.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.URLSTATE_JS_RESOURCE);
		assumeTrue(r != null, "node (or urlstate-directives.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void d01_registerDirectiveRejectsReservedAndMalformedNames() {
		var r = report();
		assertEquals(true, r.get("registerReserved"));
		assertEquals(true, r.get("registerBadName"));
		assertEquals(true, r.get("registerOk"));
	}

	@Test void d02_registerDirectiveRejectsDuplicateNames() {
		assertEquals(true, report().get("registerDuplicate"));
	}

	@Test void d03_registeredDirectiveRoundTripsThroughExt() {
		var r = report();
		assertEquals("abc", r.get("extRoundTrip"));
		assertEquals("probe(abc)", r.get("encoded"));
	}

	@Test void d04_unregisteredDirectiveStillDropped() {
		assertNull(report().get("stillDroppedUnregistered"));
	}

	@Test void d05_isEmptyStateChecksExt() {
		var r = report();
		assertEquals(false, r.get("notEmptyBecauseOfExtOnly"));
		assertEquals(true, r.get("emptyWithNoExt"));
		assertEquals(true, r.get("emptyWithBlankExt"));
	}

	@Test void d06_codecThrowOnDecodeIsEJS68AndDirectiveDropped() {
		var r = report();
		assertEquals("E-JS-68", r.get("throwingCodecLogged"));
		assertNull(r.get("throwingCodecValue"));
		assertEquals("a", r.get("throwingCodecKeepsTab"));
	}

	@Test void d07_encodeArgPercentEncodesTheReservedChars() {
		assertEquals("a%3Bb%28c%29d%2Ce%3Df%25", report().get("encodeArgSample"));
	}

	@Test void d08_windowBuiltin_roundTripsStartAndEnd() {
		var r = report();
		assertEquals("2026-10-01T00:00:00Z", r.get("windowStart"));
		assertEquals("2026-10-02T00:00:00Z", r.get("windowEnd"));
	}

	@Test void d09_windowBuiltin_toParamsRenamesByConfig() {
		var r = report();
		assertEquals("2026-10-01T00:00:00Z", r.get("windowParamFrom"));
		assertEquals("2026-10-02T00:00:00Z", r.get("windowParamTo"));
		assertEquals("{\"from\":\"x\"}", r.get("windowOpenEnded"));
	}

	@Test void d10_idsBuiltin_trimsDedupesAndCaps() {
		var r = report();
		assertEquals("a,b", r.get("idsNormalized"));
		assertEquals(true, r.get("idsCappedAtMax"));
		assertEquals("Showing the first 2 ids", r.get("idsCapMessage"));
		assertNull(r.get("idsNoMessageWhenNotCapped"));
		assertEquals("{\"idList\":\"a,b\"}", r.get("idsParam"));
	}

	@Test void d11_idsBuiltin_parseThenSerializeIsIdempotent() {
		var r = report();
		assertEquals(true, r.get("idsIdempotent"));
		assertEquals(true, r.get("idsSpecialCharsRoundTrip"));
	}

	@Test void e01_liveWire_seedsRequestParamsFromTheAddressBar() {
		var r = report();
		assertEquals("[\"p\",\"q\"]", r.get("seededFromUrl"));
		assertEquals("{\"idList\":\"p,q\"}", r.get("requestParamsSeeded"));
		assertEquals(1, ((Number)r.get("popstateListener")).intValue());
	}

	@Test void e02_liveWire_setDirectiveUpdatesBarParamsAndRefetches() {
		var r = report();
		assertEquals(true, r.get("setDirectiveOk"));
		assertEquals(1, ((Number)r.get("setDirectiveReloaded")).intValue());
		var bar = (String)r.get("addressBar");
		assertTrue(bar.contains("window(start=2026-10-01T00:00:00Z;end=2026-10-02T00:00:00Z)"), bar);
		assertTrue(bar.contains("ids(p,q)"), bar);
		var params = (String)r.get("requestParamsAfterSet");
		assertTrue(params.contains("\"from\":\"2026-10-01T00:00:00Z\"") && params.contains("\"to\":\"2026-10-02T00:00:00Z\""), params);
		assertEquals("{\"idList\":\"p,q\"}", r.get("requestParamsAfterClear"));
		assertFalse(((String)r.get("addressBarAfterClear")).contains("window("));
		assertEquals(true, r.get("setDirectiveUnknown"));
	}

	@Test void e03_liveWire_popstateReappliesAndRefetches() {
		var r = report();
		assertEquals("[\"z\"]", r.get("popstateIds"));
		assertEquals(1, ((Number)r.get("popstateReloaded")).intValue());
	}
}
