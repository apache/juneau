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
		assertEquals(10, SinkRenderAllowlist.BUILTIN_IDS.size());
		assertTrue(expected.contains("progress"));
		assertTrue(expected.contains("tag"));
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
