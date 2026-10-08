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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CardRequirements_Test extends TestBase {

	private static final ToolkitPackRegistry PACKS = new ToolkitPackRegistry();

	@Test void a01_builtInMappings() {
		var r = CardRequirements.create().build(PACKS);
		assertList(r.forType("datatables"), "datatables-glue");
		assertEmpty(r.forType("console-output"));
		assertEmpty(r.forType("html"));
	}

	@Test void a02_add_appendsToType() {
		var r = CardRequirements.create().add("datatables", "datatables-buttons").add("html", "calendar").build(PACKS);
		assertList(r.forType("datatables"), "datatables-glue", "datatables-buttons");
		assertList(r.forType("html"), "calendar");
	}

	@Test void a03_unknownPackInMapping_failsAtBuild() {
		var b = CardRequirements.create().add("html", "ghost");
		var e = assertThrows(IllegalArgumentException.class, () -> b.build(PACKS));
		assertString("Unknown toolkit pack 'ghost' (cardRequires for type 'html').", e.getMessage());
	}

	@Test void a04_forCard_typeThenRequires_deduplicated() {
		var r = CardRequirements.create().build(PACKS);
		assertList(r.forCard("datatables", "rel", List.of("datatables-buttons", "datatables-glue")), "datatables-glue", "datatables-buttons");
		assertEmpty(r.forCard("html", "box", List.of()));
	}

	@Test void a05_forCard_unknownRequires_namesCard() {
		var r = CardRequirements.create().build(PACKS);
		var e = assertThrows(IllegalArgumentException.class, () -> r.forCard("html", "box", List.of("ghost")));
		assertString("Unknown toolkit pack 'ghost' (<@card id='box'> requires=).", e.getMessage());
	}
}
