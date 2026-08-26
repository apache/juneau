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
import org.junit.jupiter.api.*;

/**
 * {@link CardContent} bean contract: the raw-markup {@link CardBody}.  This widgets test must never import
 * {@code ViewDef} (no widgets&rarr;views), mirroring {@code CardFieldList_Test}'s equivalent check.
 */
class CardContent_Test extends TestBase {

	@Test void a01_builder_roundTrip() {
		var c = CardContent.create().content("<p>Hello, <b>world</b>.</p>");
		assertEquals("<p>Hello, <b>world</b>.</p>", c.content);
		c.validate();
	}

	@Test void a02_nullContent_rejected() {
		var c = CardContent.create();
		var e = assertThrows(IllegalArgumentException.class, c::validate);
		assertTrue(e.getMessage().contains("content"), e::getMessage);
	}

	@Test void a03_blankContent_isAccepted() {
		// Mirrors the Tab/Subtab raw-content contract: a blank (non-null) body is not an error, only an absent one.
		var c = CardContent.create().content("  ");
		c.validate();
		assertEquals("  ", c.content);
	}

	@Test void a04_source_doesNotImportViewsModule() throws Exception {
		var root = Path.of("").toAbsolutePath();
		var src = root;
		if (!Files.isDirectory(src.resolve("src/main/java")))
			src = root.resolve("juneau-rest/juneau-rest-server-widgets");
		var f = src.resolve("src/main/java/org/apache/juneau/rest/server/widgets/CardContent.java");
		for (var line : Files.readAllLines(f))
			if (line.stripLeading().startsWith("import "))
				assertFalse(line.contains("rest.server.views"), () -> "CardContent must not import any views type: " + line);
	}
}
