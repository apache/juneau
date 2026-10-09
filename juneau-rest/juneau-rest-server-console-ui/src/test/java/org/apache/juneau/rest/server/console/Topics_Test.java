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
package org.apache.juneau.rest.server.console;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

class Topics_Test extends TestBase {

	private static final String E40 = "invalid topic '%s': expected family[:key] (see the topic syntax)";

	@Test void a01_frameworkBuilders() {
		assertEquals("card:changes", Topics.card("changes"));
		assertEquals("selection:changes", Topics.selection("changes"));
		assertEquals("filter:changes", Topics.filter("changes"));
		assertEquals("redraw:changes", Topics.redraw("changes"));
		assertEquals("detail:changes", Topics.detail("changes"));
		assertEquals("bulk:changes", Topics.bulk("changes"));
		assertEquals("cmd:tasks", Topics.cmd("tasks"));
		assertEquals("probe:ssc-probe-row", Topics.probe("ssc-probe-row"));
		assertEquals("job:j-91", Topics.job("j-91"));
		assertEquals("badge:pending", Topics.badge("pending"));
		assertEquals("bridge:ops", Topics.bridge("ops"));
	}

	@Test void a02_badKey_isE40() {
		var e = assertThrows(IllegalArgumentException.class, () -> Topics.selection("a b"));
		assertEquals(String.format(E40, "selection:a b"), e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> Topics.job(null));
		assertEquals(String.format(E40, "job:null"), e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> Topics.cmd("x".repeat(129)));
		assertEquals(String.format(E40, "cmd:" + "x".repeat(129)), e.getMessage());
	}

	@Test void a03_custom() {
		assertEquals("ssc.focus", Topics.custom("ssc", "focus"));
		assertEquals("ssc.alert:east", Topics.custom("ssc", "alert", "east"));
		assertEquals("ssc.alert:*", Topics.custom("ssc", "alert", "*"));
		var e = assertThrows(IllegalArgumentException.class, () -> Topics.custom("", "focus"));
		assertEquals("custom topic family 'focus' must be namespaced (e.g. 'app.focus')", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> Topics.custom("SSC", "focus"));
		assertEquals(String.format(E40, "SSC.focus"), e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> Topics.custom("ssc", "alert", "a b"));
		assertEquals(String.format(E40, "ssc.alert:a b"), e.getMessage());
	}

	@Test void a04_frameworkFamilies() {
		assertEquals(List.of("card", "selection", "filter", "redraw", "detail", "bulk", "cmd", "probe", "job", "badge", "bridge"),
			List.copyOf(Topics.FRAMEWORK_FAMILIES));
		assertThrows(UnsupportedOperationException.class, () -> Topics.FRAMEWORK_FAMILIES.add("x"));
	}

	@Test void a05_frameworkFamilies_matchJuneauBusJs() throws Exception {
		var js = Files.readString(Path.of(System.getProperty("basedir", "."),
			"../juneau-rest-server-views/src/main/resources/org/apache/juneau/views/juneau-bus.js"), UTF_8);
		var m = Pattern.compile("FRAMEWORK_FAMILIES\\s*[:=]\\s*(?:Object\\.freeze\\()?\\[([^\\]]*)\\]").matcher(js);
		assertTrue(m.find(), "FRAMEWORK_FAMILIES array not found in juneau-bus.js");
		var jsFamilies = new LinkedHashSet<String>();
		for (var part : m.group(1).split(","))
			if (! part.isBlank())
				jsFamilies.add(part.trim().replaceAll("^['\"]|['\"]$", ""));
		assertEquals(Topics.FRAMEWORK_FAMILIES, jsFamilies);
	}

	@Test void a06_patternsMatchSchema() throws Exception {
		try (var in = Topics.class.getResourceAsStream("/org/apache/juneau/console/juneau-page.schema.json")) {
			var defs = JsonMap.ofString(new String(in.readAllBytes(), UTF_8)).getMap("$defs");
			assertEquals(defs.getMap("topic").getString("pattern"), Topics.TOPIC.pattern());
			assertEquals(defs.getMap("topicDecl").getMap("properties").getMap("topic").getString("pattern"), Topics.TOPIC_DECL.pattern());
			assertEquals(defs.getMap("publication").getMap("properties").getMap("topic").getString("pattern"), Topics.PUBLICATION.pattern());
			assertEquals(defs.getMap("subscription").getMap("properties").getMap("as").getString("pattern"), Topics.ROLE.pattern());
		}
	}
}
