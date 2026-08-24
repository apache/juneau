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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link MenuItem} factory matrix: link / safe require a unique id; {@link MenuItem#divider()} is id-exempt (S3).
 */
class MenuItem_Test extends TestBase {

	@Test void a01_link_ok() {
		var m = MenuItem.link("profile", "Profile", "/me").icon("person");
		assertEquals("person", m.icon);
		m.validate();
	}

	@Test void a02_safe_ok() {
		MenuItem.safe("out", "Sign out", "sign-out").validate();
	}

	@Test void a03_divider_isExempt() {
		var d = MenuItem.divider();
		assertTrue(d.isDivider());
		assertNull(d.id);
		d.validate();   // no id, no label - still valid
	}

	@Test void a04_link_blankId_rejected() {
		var m = MenuItem.link("  ", "L", "/x");
		assertThrows(IllegalArgumentException.class, m::validate);
	}

	@Test void a05_safeItem_withHref_rejected() {
		var m = MenuItem.safe("x", "X", "do-x");
		m.href = "/x";
		assertThrows(IllegalArgumentException.class, m::validate);
	}

	@Test void a06_link_badHref_rejected() {
		for (var bad : new String[]{"http://x", "//x", "../x", "javascript:1"}) {
			var m = MenuItem.link("x", "X", bad);
			assertThrows(IllegalArgumentException.class, m::validate, () -> "expected reject for " + bad);
		}
	}

	@Test void a07_safe_badToken_rejected() {
		for (var bad : new String[]{"Sign-Out", "sign_out", "", "1x"}) {
			var m = MenuItem.safe("x", "X", bad);
			assertThrows(IllegalArgumentException.class, m::validate, () -> "expected reject for '" + bad + "'");
		}
	}

	@Test void a08_neitherLinkNorSafe_rejected() {
		var m = new MenuItem();
		m.id = "x";
		m.label = "X";
		assertThrows(IllegalArgumentException.class, m::validate);
	}
}
