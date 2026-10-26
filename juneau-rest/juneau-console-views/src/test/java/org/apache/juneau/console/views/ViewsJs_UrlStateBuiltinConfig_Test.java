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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Node check that the {@code window} and {@code ids} built-ins are configured from a view's own {@code urlState}
 * entry, with nothing registered by hand, end to end from {@link TableSpec} output to the live request.
 */
class ViewsJs_UrlStateBuiltinConfig_Test extends TestBase {

	private static Map<?,?> report() {
		var urlState = TableSpec.create("x")
			.urlState("ids", Map.of("param", "pid", "max", 2))
			.urlState("window", Map.of("params", Map.of("start", "from", "end", "to")))
			.toCardBody().get("urlState");
		var r = NodeHarness.report(Map.of("URLSTATE_VIEW_JSON", Json.of(urlState)), "urlstate-builtin-config.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.URLSTATE_JS_RESOURCE);
		assumeTrue(r != null, "node (or urlstate-builtin-config.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void a01_builtinsAreRegisteredOnlyByTheViewConfig() {
		var r = report();
		assertEquals("false|false", r.get("registeredBefore"));
		assertEquals("true|true", r.get("registeredAfter"));
	}

	@Test void a02_windowConfigRenamesTheRequestParams() {
		var r = report();
		assertEquals("2026-10-01T00:00:00Z", r.get("windowStart"));
		assertEquals("2026-10-02T00:00:00Z", r.get("windowEnd"));
		var params = (String)r.get("requestParams");
		assertTrue(params.contains("\"from\":\"2026-10-01T00:00:00Z\"") && params.contains("\"to\":\"2026-10-02T00:00:00Z\""), params);
	}

	@Test void a03_idsConfigRenamesTheParamAndCapsAtMax() {
		var r = report();
		assertEquals("[\"a\",\"b\"]", r.get("seededIds"));
		assertEquals(true, r.get("idsTruncated"));
		assertEquals("Showing the first 2 ids", r.get("idsCapMessage"));
		var params = (String)r.get("requestParams");
		assertTrue(params.contains("\"pid\":\"a,b\""), params);
	}
}
