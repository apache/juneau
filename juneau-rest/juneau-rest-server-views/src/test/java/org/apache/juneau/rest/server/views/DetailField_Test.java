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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link DetailField} factory, fluent setters, and {@link DetailField.Format} wire tokens.
 */
class DetailField_Test extends TestBase {

	@Test void a01_of_setsData() {
		var f = DetailField.of("body");
		assertEquals("body", f.data);
		assertNull(f.title);
		assertNull(f.format);
	}

	@Test void a02_of_blankRejected() {
		assertThrows(IllegalArgumentException.class, () -> DetailField.of(""));
		assertThrows(IllegalArgumentException.class, () -> DetailField.of(null));
	}

	@Test void a03_fluentFormatAndTitle() {
		var f = DetailField.of("body").title("SKILL.md").format(DetailField.Format.MARKDOWN);
		assertEquals("SKILL.md", f.title);
		assertEquals(DetailField.Format.MARKDOWN, f.format);
		assertEquals("markdown", f.format.wire());
		assertEquals("text", DetailField.Format.TEXT.wire());
	}

	@Test void a04_renderAndHref_nullByDefault() {
		var f = DetailField.of("cpu");
		assertNull(f.render);
		assertNull(f.href);
		f.render("tag:status").href("/x/{id}");
		assertEquals("tag", f.render.id);
		assertEquals("status", f.render.meta.get("field"));
		assertEquals("/x/{id}", f.href);
		f.render(Render.of("progress")).href(null);
		assertEquals("progress", f.render.id);
		assertNull(f.href);
	}

	@Test void a05_actions_nullByDefault_andFluentlySettable() {
		var f = DetailField.of("ticketId");
		assertNull(f.actions);
		var bar = ActionBar.create().items(ActionRef.of("ticket-create"), ActionRef.of("ticket-assign"));
		assertSame(f, f.actions(bar));
		assertSame(bar, f.actions);
		assertSize(2, f.actions.items);
		// Unsettable, like every other optional on this bean.
		f.actions(null);
		assertNull(f.actions);
	}

	/**
	 * A field may carry a value-shaping option and a bar at the same time.  The bar is an additional host on the
	 * field, not an alternative to its value, so nothing here is mutually exclusive.
	 */
	@Test void a06_actions_composesWithTheValueOptions() {
		var f = DetailField.of("ticketId").title("Linked ticket").href("/tickets/{ticketId}")
			.render("linked")
			.span(FieldSpan.FULL)
			.actions(ActionBar.create().items(ActionRef.of("ticket-create")));
		assertEquals("Linked ticket", f.title);
		assertEquals("linked", f.render.id);
		assertEquals("/tickets/{ticketId}", f.href);
		assertEquals(FieldSpan.FULL, f.span);
		assertSize(1, f.actions.items);
	}
}
