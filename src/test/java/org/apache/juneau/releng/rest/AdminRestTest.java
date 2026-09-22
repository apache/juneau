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

package org.apache.juneau.releng.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.juneau.commons.utils.IoUtils;
import org.apache.juneau.releng.RootRest;
import org.apache.juneau.rest.server.Rest;
import org.junit.jupiter.api.Test;

/**
 * Admin Page Tab removal: the standalone {@code AdminRest} pair (redirect + Releases-pair page) has been retired in
 * favor of the Releases tab alone. Guards the three surfaces that carried it: {@link RootRest}'s declared children,
 * the {@code admin.ftlh} template, and {@code base.ftlh}'s nav markup/asset wiring.
 */
class AdminRestTest {

	@Test
	void a01_rootRestNoLongerMountsAnAdminChild() {
		var children = RootRest.class.getAnnotation(Rest.class).children();
		var names = Arrays.stream(children).map(Class::getSimpleName).toList();
		assertFalse(names.contains("AdminRest"), "RootRest must not mount an Admin child: " + names);
	}

	@Test
	void a02_adminTemplateNoLongerShips() throws IOException {
		try (var in = AdminRestTest.class.getResourceAsStream("/templates/admin.ftlh")) {
			assertNull(in, "admin.ftlh must not ship on the classpath");
		}
	}

	@Test
	void b01_baseTemplateNoLongerWiresTheAdminNavLinkOrActiveTabBranches() throws IOException {
		var base = readClasspathResource("/templates/base.ftlh");
		assertFalse(base.contains("href=\"/rest/admin\""), "Admin nav link must be gone: " + base);
		// The chrome is now a thin <@console> shell: no activeTab if-blocks and no <#macro content>/<@content> at all.
		assertFalse(base.contains("activeTab"), "activeTab conditional wiring must be gone: " + base);
		assertFalse(base.contains("<@content"), "The content macro must be gone (replaced by <@main/>): " + base);
		assertFalse(base.contains("<#macro content"), "The content macro must be gone: " + base);
		assertTrue(base.contains("<@main/>"), "Chrome must emit the captured page body via <@main/>: " + base);
		assertTrue(base.contains("href=\"/rest/setup\""), "Missing Setup nav link: " + base);
		assertTrue(base.contains("href=\"/rest/releases\""), "Missing Releases nav link: " + base);
		assertTrue(base.contains("href=\"/rest/runs\""), "Missing New Release nav link: " + base);
	}

	@Test
	void b02_baseTemplateAuthorsExactlyThreeNavNodesOnce() throws IOException {
		var base = readClasspathResource("/templates/base.ftlh");
		assertTrue(base.contains("<@navigation>"), "Chrome must author the nav tree once as <@navigation>: " + base);
		var count = base.split("<@node ", -1).length - 1;
		assertEquals(3, count, "Expected exactly 3 <@node> nav entries (Setup, Releases, New Release): " + base);
	}

	@Test
	void b03_releasesDetailScriptOptsIntoMaterialIconPack() throws IOException {
		// With the views toolkit pack, base.ftlh no longer hardcodes juneau-icons.js or its pack attribute — the pack
		// loop loads it generically (default "original"). The Material opt-in (Juneau WORK-J0545) now lives in the
		// Releases page's init script, which selects it via JuneauViews.icons.pack() before the mount.
		var base = readClasspathResource("/templates/base.ftlh");
		assertFalse(base.contains("data-juneau-icon-pack"), "base.ftlh must not hardcode the icon pack anymore: " + base);
		var script = readClasspathResource("/static/js/releases-detail.js");
		assertTrue(script.contains("icons.pack('material')"),
			"releases-detail.js must select the Material icon sprite: " + script);
	}

	private static String readClasspathResource(String path) throws IOException {
		try (var in = AdminRestTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path + " not found on the test classpath");
			return new String(IoUtils.readBytes(in), StandardCharsets.UTF_8);
		}
	}
}
