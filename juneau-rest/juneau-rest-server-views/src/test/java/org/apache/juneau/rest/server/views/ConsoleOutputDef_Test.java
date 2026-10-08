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
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class ConsoleOutputDef_Test extends TestBase {

	private static void assertJson(String expected, Object actual) {
		assertEquals(Json.to(expected.replace('\'', '"'), Map.class), Json.to(Json.of(actual), Map.class));
	}

	@Test void a01_minimalRegion() {
		var r = ConsoleOutputDef.create("build-log").linesUrl("/jobs/7/lines").toRegion();
		assertBean(r, "id,type,populate,dataUrl", "build-log,card-body,console-output,<null>");
		assertJson("{'linesUrl':'/jobs/7/lines'}", r.params);
		assertDoesNotThrow(r::validate);
	}

	@Test void a02_fullParamsInTableOrder() {
		var r = ConsoleOutputDef.create("build-log").linesUrl("/j/lines").downloadUrl("/j/download").refreshMs(3000)
			.tail(100).earlierLimit(50).rows(30).compact(true).anchorPrefix("L").showTime(true).markers("hide").title("Build output")
			.toRegion();
		assertEquals(List.of("linesUrl", "downloadUrl", "refreshMs", "tail", "earlierLimit", "rows", "compact", "anchorPrefix",
			"showTime", "markers", "title"), new ArrayList<>(r.params.keySet()));
		assertJson("{'linesUrl':'/j/lines','downloadUrl':'/j/download','refreshMs':3000,'tail':100,'earlierLimit':50,'rows':30,"
			+ "'compact':true,'anchorPrefix':'L','showTime':true,'markers':'hide','title':'Build output'}", r.params);
	}

	@Test void a03_refreshClamp() {
		assertEquals(1000L, ConsoleOutputDef.create("a").linesUrl("/x").refreshMs(10).toRegion().params.get("refreshMs"));
		assertThrowsWithMessage(IllegalArgumentException.class, "refreshMs must be > 0",
			() -> ConsoleOutputDef.create("a").linesUrl("/x").refreshMs(0).validate());
	}

	@Test void a04_allowlistOptIn() {
		var r = ConsoleOutputDef.create("a").linesUrl("/x").toRegion();
		assertList(r.allowedPopulators, "console-output");
	}

	@Test void a05_forMixin() {
		var d = ConsoleOutputDef.forMixin("groom-log", "/petstore", "{id}").type(RegionDef.TYPE_ROW_DETAIL);
		assertJson("{'linesUrl':'/petstore/juneau-console-output/{id}/lines','downloadUrl':'/petstore/juneau-console-output/{id}/download'}",
			d.toRegion().params);
		var c = ConsoleOutputDef.forMixin("job", "/petstore/", "7f3a9c");
		assertEquals("/petstore/juneau-console-output/7f3a9c/lines", c.toRegion().params.get("linesUrl"));
		assertThrowsWithMessage(IllegalArgumentException.class, "logIdTemplate",
			() -> ConsoleOutputDef.forMixin("job", "/p", "a.b"));
		assertEquals("/juneau-console-output", ConsoleOutputDef.MIXIN_PREFIX);
	}

	@Test void b01_validation() {
		assertThrowsWithMessage(IllegalArgumentException.class, "id must not be blank", () -> ConsoleOutputDef.create(" ").linesUrl("/x").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "linesUrl is required", () -> ConsoleOutputDef.create("a").validate());
		for (var bad : List.of("http://e.x/l", "//e.x/l", "javascript:x", "/a/../b"))
			assertThrowsWithMessage(IllegalArgumentException.class, "linesUrl must be a same-origin path", () -> ConsoleOutputDef.create("a").linesUrl(bad).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "downloadUrl must be a same-origin path",
			() -> ConsoleOutputDef.create("a").linesUrl("/x").downloadUrl("https://e.x/").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "type must be one of",
			() -> ConsoleOutputDef.create("a").linesUrl("/x").type("popup").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "{id} is only allowed on a row-detail",
			() -> ConsoleOutputDef.create("a").linesUrl("/r/{id}/lines").validate());
		assertDoesNotThrow(() -> ConsoleOutputDef.create("a").linesUrl("/r/{id}/lines").type(RegionDef.TYPE_ROW_DETAIL).validate());
	}

	@Test void b02_ranges() {
		var ok = ConsoleOutputDef.create("a").linesUrl("/x");
		assertDoesNotThrow(() -> ok.tail(0).tail(10_000).earlierLimit(1).earlierLimit(10_000).rows(3).rows(200).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "tail must be 0..10000", () -> ConsoleOutputDef.create("a").linesUrl("/x").tail(10_001).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "tail must be 0..10000", () -> ConsoleOutputDef.create("a").linesUrl("/x").tail(-1).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "earlierLimit must be 1..10000", () -> ConsoleOutputDef.create("a").linesUrl("/x").earlierLimit(0).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "rows must be 3..200", () -> ConsoleOutputDef.create("a").linesUrl("/x").rows(2).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "rows must be 3..200", () -> ConsoleOutputDef.create("a").linesUrl("/x").rows(201).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "anchorPrefix", () -> ConsoleOutputDef.create("a").linesUrl("/x").anchorPrefix("9L").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "markers must be one of dim|hide|show", () -> ConsoleOutputDef.create("a").linesUrl("/x").markers("blink").validate());
	}

	@Test void c01_fromMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("linesUrl", "/runs/1/lines");
		m.put("refreshMs", 2000);
		m.put("rows", 20);
		m.put("tail", 5000L);
		m.put("anchorPrefix", "raw-L");
		m.put("markers", "dim");
		m.put("compact", false);
		var d = ConsoleOutputDef.fromMap("run-output", m).validate();
		assertJson("{'linesUrl':'/runs/1/lines','refreshMs':2000,'tail':5000,'rows':20,'compact':false,'anchorPrefix':'raw-L','markers':'dim'}", d.toMap());
	}

	@Test void c02_fromMapRejectsUnknownAndWrongTypes() {
		assertThrowsWithMessage(IllegalArgumentException.class, "unknown key 'colour'",
			() -> ConsoleOutputDef.fromMap("a", Map.of("linesUrl", "/x", "colour", "red")));
		assertThrowsWithMessage(IllegalArgumentException.class, "rows must be an integer",
			() -> ConsoleOutputDef.fromMap("a", Map.of("linesUrl", "/x", "rows", "20")));
		assertThrowsWithMessage(IllegalArgumentException.class, "rows must be an integer",
			() -> ConsoleOutputDef.fromMap("a", Map.of("linesUrl", "/x", "rows", 2.5)));
		assertThrowsWithMessage(IllegalArgumentException.class, "compact must be a boolean",
			() -> ConsoleOutputDef.fromMap("a", Map.of("linesUrl", "/x", "compact", "true")));
		assertThrowsWithMessage(IllegalArgumentException.class, "linesUrl must be a string",
			() -> ConsoleOutputDef.fromMap("a", Map.of("linesUrl", 5)));
	}

	@Test void c03_keys() {
		assertList(ConsoleOutputDef.KEYS, "linesUrl", "downloadUrl", "refreshMs", "tail", "earlierLimit", "rows", "compact",
			"anchorPrefix", "showTime", "markers", "title");
	}
}
