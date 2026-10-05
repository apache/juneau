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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * activeNav resolution (spec §4.3): tab paths at depths 1-4, the {@code selected=} override, E-6, E-7, and the empty
 * fallback.
 *
 * @since 10.0.0
 */
class PageCapture_ActiveNav_Test extends TestBase {

	@Test void a01_depth1() { assertPage(render("page-tab-slo")).isValid().hasActiveNav("slo"); }
	@Test void a02_depth2() { assertPage(render("page-tab-setup")).isValid().hasActiveNav("home", "setup"); }
	@Test void a03_depth3() { assertPage(render("page-tab-alpha")).isValid().hasActiveNav("home", "setup", "alpha"); }
	@Test void a04_depth4_trimmed() { assertPage(render("page-tab-beta")).isValid().hasActiveNav("home", "setup", "alpha", "beta"); }

	@Test void a05_noTabIsEmpty() {
		assertPage(render("page-plain")).isValid().hasNoActiveNav();
	}

	@Test void a06_selectedWinsOverTab() {
		assertPage(renderSelected("page-tab-slo")).isValid().hasActiveNav("home", "about");
	}

	@Test void a07_selectedWithoutPage() {
		assertPage(render("nav-selected")).isValid().hasActiveNav("a", "b");
	}

	@Test void a08_doubleSelect_E6() {
		assertError("bad-double-select", "<@node id='b'> selected=true but 'a' is already selected.");
	}

	@Test void a09_tabToHiddenNode_E7() {
		assertError("page-tab-secret", "<@page tab='secret'> does not match a visible <@node> path; known paths: "
			+ "'home, home/about, home/setup, home/setup/alpha, home/setup/alpha/beta, slo'.");
	}

	@Test void a10_tabToUnknownChild_E7() {
		assertError("page-tab-unknown", "<@page tab='home/nope'> does not match a visible <@node> path;");
	}

	@Test void a11_hiddenNodeIsNotInTree() {
		var nav = assertPage(render("page-plain")).contract().getList("nav").toString();
		org.junit.jupiter.api.Assertions.assertFalse(nav.contains("secret"), nav);
	}
}
