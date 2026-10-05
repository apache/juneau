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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.test.*;
import org.junit.jupiter.api.*;

/**
 * Free markup in a {@code <@page>} body becomes ordered {@code jc-seg-N} bare html cards between the authored cards
 * (spec §4.2), and {@code <@card>} without {@code id=} gets {@code jc-card-N} (P17).
 *
 * @since 10.0.0
 */
class PageCapture_Segments_Test extends TestBase {

	@Test void a01_segmentsKeepAuthoredOrder() {
		var a = assertPage(render("page-segments")).isValid().hasCardOrder("jc-seg-1", "a", "jc-seg-2", "b")
			.templateContains("jc-seg-1", "<p>s1</p>")
			.templateContains("jc-seg-2", "<p>s2</p>")
			.templateContains("a", "<p>A</p>");
		assertEquals(Boolean.TRUE, card(a, "jc-seg-1").get("bare"));
		assertNull(card(a, "a").get("bare"));
	}

	@Test void a02_whitespaceBetweenCardsIsNotASegment() {
		assertPage(render("page-cards-only")).isValid().hasCardOrder("a", "b");
	}

	@Test void a03_autoIds() {
		assertPage(render("page-autoid")).isValid().hasCardOrder("jc-card-1", "jc-seg-1", "jc-card-2")
			.templateContains("jc-card-1", "<p>one</p>")
			.templateContains("jc-card-2", "<p>two</p>");
	}

	@Test void a04_title() {
		assertEquals("Totals", card(assertPage(render("page-card-title")).isValid(), "t").getString("title"));
	}

	@Test void a05_srcOnly() {
		var c = card(assertPage(render("page-card-src")).isValid(), "s");
		assertEquals("/frag/s.html", c.getString("src"));
		assertNull(c.get("template"));
	}

	@Test void a06_templateReference() {
		var a = assertPage(render("page-card-template-ref")).isValid();
		assertBeans(List.of(card(a, "again"), card(a, "body1")), "template", "body1", "body1");
		assertEquals(1, render("page-card-template-ref").split("<template data-card=", -1).length - 1);
	}

	static JsonMap card(PageContractAssert a, String id) {
		for (var o : a.contract().getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "'");
	}
}
