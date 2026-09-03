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

import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Java id-allowlist for fill sinks, plus sync against the JS frozen-builtin id set.
 */
class SinkRenderAllowlist_Test extends TestBase {

	@Test void a01_builtinIdsAccepted() {
		for (var id : SinkRenderAllowlist.BUILTIN_IDS)
			SinkRenderAllowlist.assertAllowed(id, null);
	}

	@Test void a02_unknownRejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> SinkRenderAllowlist.assertAllowed("evil", null));
		assertTrue(e.getMessage().contains("evil"), e::getMessage);
	}

	@Test void a03_customRejectedWithoutOptIn_acceptedWith() {
		assertThrows(IllegalArgumentException.class,
			() -> SinkRenderAllowlist.assertAllowed("spark", null));
		SinkRenderAllowlist.assertAllowed("spark", java.util.Set.of("spark"));
	}

	@Test void a04_blankRejected() {
		assertThrows(IllegalArgumentException.class, () -> SinkRenderAllowlist.assertAllowed("", null));
		assertThrows(IllegalArgumentException.class, () -> SinkRenderAllowlist.assertAllowed(null, null));
	}

	@Test void a05_popoverSubset() {
		SinkRenderAllowlist.assertPopoverAllowed("date");
		assertThrows(IllegalArgumentException.class, () -> SinkRenderAllowlist.assertPopoverAllowed("tag"));
		assertThrows(IllegalArgumentException.class, () -> SinkRenderAllowlist.assertPopoverAllowed("progress"));
	}

	@Test void a06_javaIdsMatchJsFrozenSet() throws Exception {
		String body;
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in);
			body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		for (var id : SinkRenderAllowlist.BUILTIN_IDS)
			assertTrue(body.contains("\"" + id + "\""), () -> "missing frozen id " + id);
		assertTrue(body.contains("frozenBuiltinIds"), body);
		var expected = String.join(",", SinkRenderAllowlist.BUILTIN_IDS.stream().sorted().toList());
		// The JS array is not sorted the same way as Set.of iteration; pin membership via the snapshot list.
		assertTrue(body.contains("\"progress\""), body);
		// Lockstep count: 12 built-in fill-sink ids, the eleventh being "pill" and the twelfth "code" (WORK-J0508).
		assertEquals(12, SinkRenderAllowlist.BUILTIN_IDS.size());
		assertTrue(expected.contains("progress"));
		assertTrue(expected.contains("tag"));
		// Bidirectional lockstep against the two source arrays that together drive frozenBuiltinIds: the snapshot
		// list plus the hand-registered sink variants (`pill`).  Catches an id added on one side only, in either
		// direction, without needing Node.
		assertEquals(expected, jsFrozenIds(body));
	}

	/** The sorted union of the JS {@code BUILTIN_RENDER_IDS} and {@code SINK_VARIANT_RENDER_IDS} literals. */
	private static String jsFrozenIds(String rendersJs) {
		var ids = new TreeSet<String>();
		for (var name : List.of("BUILTIN_RENDER_IDS = [", "SINK_VARIANT_RENDER_IDS = [")) {
			var start = rendersJs.indexOf(name);
			assertTrue(start > 0, () -> "missing " + name);
			start += name.length();
			var literal = rendersJs.substring(start, rendersJs.indexOf(']', start));
			for (var part : literal.split(","))
				if (part.contains("\""))
					ids.add(part.substring(part.indexOf('"') + 1, part.lastIndexOf('"')));
		}
		return String.join(",", ids);
	}

	@Test void a08_pillIsABuiltinFillSink_withADisplayOnlySinkRenderer() {
		// "pill" is now a fill-sink built-in; the count moved 10 -> 11 with this addition and nothing else.
		assertEquals(12, SinkRenderAllowlist.BUILTIN_IDS.size());
		assertTrue(SinkRenderAllowlist.BUILTIN_IDS.contains("pill"), "pill must be a fill-sink built-in");
		SinkRenderAllowlist.assertAllowed("pill", null);
		// Still not popover text - a pill is a chip, not a text-shaped built-in.
		assertThrows(IllegalArgumentException.class, () -> SinkRenderAllowlist.assertPopoverAllowed("pill"));
		assertFalse(SinkRenderAllowlist.POPOVER_TEXT_IDS.contains("pill"));
	}

	@Test void a09_codeIsABuiltinFillSink_minimalMonospaceSourceRenderer() {
		// "code" (WORK-J0508, Foundry WORK-P0063 row-detail-subtabs follow-up) is a fill-sink built-in; the count
		// moved 11 -> 12 with this addition and nothing else.
		assertEquals(12, SinkRenderAllowlist.BUILTIN_IDS.size());
		assertTrue(SinkRenderAllowlist.BUILTIN_IDS.contains("code"), "code must be a fill-sink built-in");
		SinkRenderAllowlist.assertAllowed("code", null);
		// Not popover text - the popover surface is a small text-shaped bubble, not a place for a source block.
		assertThrows(IllegalArgumentException.class, () -> SinkRenderAllowlist.assertPopoverAllowed("code"));
		assertFalse(SinkRenderAllowlist.POPOVER_TEXT_IDS.contains("code"));
	}

	@Test void a07_servingPath_detailFieldUnknownIdFailsViewTableOf() {
		var v = ViewDef.create("x").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("/d/{id}")
				.sections(DetailSection.create("s", "S")
					.fields(DetailField.of("cpu").render("nope"))))
			.build();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
	}
}
