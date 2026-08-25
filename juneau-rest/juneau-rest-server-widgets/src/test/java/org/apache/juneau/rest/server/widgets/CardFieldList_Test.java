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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.junit.jupiter.api.*;

/**
 * {@link CardFieldList} bean contract, endpoint safety, and the Java-side poll clamp.  The clamp floor comes from
 * {@link SafePathTemplate#MIN_POLL_INTERVAL_MS} in {@code juneau-commons} &mdash; this widgets test must never
 * import {@code ViewDef} (no widgets&rarr;views).
 */
class CardFieldList_Test extends TestBase {

	private static CardFieldList base() {
		return CardFieldList.create().fields(CardField.of("k", "L", "v"));
	}

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", CardFieldList.CONTRACT_VERSION);
	}

	@Test void a02_builder_roundTrip_defaultsColumnsToTwo() {
		var b = CardFieldList.create();
		assertEquals(2, b.columns);
		var b2 = base().columns(3);
		assertEquals(3, b2.columns);
		b2.validate();
	}

	@Test void a03_columnsBelowOne_rejected() {
		var b = base().columns(0);
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a04_emptyFields_rejected() {
		var b1 = CardFieldList.create();
		assertThrows(IllegalArgumentException.class, b1::validate);
		var b2 = CardFieldList.create().fields();
		assertThrows(IllegalArgumentException.class, b2::validate);
	}

	@Test void a05_blankDataKey_rejected() {
		var b = CardFieldList.create().fields(CardField.of("  ", "L"));
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a06_duplicateDataKey_rejected() {
		var b = CardFieldList.create().fields(CardField.of("k", "A"), CardField.of("k", "B"));
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a07_pollWithoutEndpoint_rejected() {
		var b = base().pollIntervalMs(10_000);
		assertThrows(IllegalArgumentException.class, b::validate);
	}

	@Test void a08_unsafeEndpoint_rejected() {
		var b1 = base().refresh("http://evil/x");
		assertThrows(IllegalArgumentException.class, b1::validate);
		var b2 = base().refresh("//evil/x");
		assertThrows(IllegalArgumentException.class, b2::validate);
		var b3 = base().refresh("../x");
		assertThrows(IllegalArgumentException.class, b3::validate);
		var b4 = base().refresh("javascript:alert(1)");
		assertThrows(IllegalArgumentException.class, b4::validate);
	}

	@Test void a09_templatedEndpoint_rejected() {
		var b1 = base().refresh("/cards/{id}");
		assertThrows(IllegalArgumentException.class, b1::validate);
		var b2 = base().refresh("/x/{anything}");
		assertThrows(IllegalArgumentException.class, b2::validate);
	}

	@Test void a10_safeNonTemplatedEndpoint_passes() {
		var b = base().refresh("/data/summary");
		b.validate();
		assertEquals("/data/summary", b.refreshEndpoint);
	}

	@Test void a11_pollBelowFloor_isClampedInJava() {
		var b = base().refresh("/data/summary").pollIntervalMs(1_000);
		b.validate();
		assertEquals((int) SafePathTemplate.MIN_POLL_INTERVAL_MS, b.pollIntervalMs);
		assertEquals(5_000, b.pollIntervalMs);
	}

	@Test void a12_pollAboveFloor_isHonored() {
		var b = base().refresh("/data/summary").pollIntervalMs(30_000);
		b.validate();
		assertEquals(30_000, b.pollIntervalMs);
	}

	@Test void a13_pollNull_clears() {
		var b = base().refresh("/data/summary").pollIntervalMs(30_000).pollIntervalMs(null);
		assertNull(b.pollIntervalMs);
		b.validate();
		assertNull(b.pollIntervalMs);
	}

	@Test void a14_source_doesNotImportViewDef() throws Exception {
		var root = Path.of("").toAbsolutePath();
		var src = root;
		if (!Files.isDirectory(src.resolve("src/main/java")))
			src = root.resolve("juneau-rest/juneau-rest-server-widgets");
		var f = src.resolve("src/main/java/org/apache/juneau/rest/server/widgets/CardFieldList.java");
		// Assert no widgets->views compile dependency: no import line pulls in a views-module type.  (A Javadoc
		// mention of ViewDef.poll() as the shared numeric floor is fine; an import is not.)
		for (var line : Files.readAllLines(f))
			if (line.stripLeading().startsWith("import "))
				assertFalse(line.contains("rest.server.views"), () -> "CardFieldList must not import any views type: " + line);
	}
}
