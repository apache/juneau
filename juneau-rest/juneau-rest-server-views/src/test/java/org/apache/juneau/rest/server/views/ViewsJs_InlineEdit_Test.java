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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** Node behavioral checks for the {@code edit} renderer and the inline cell editor it drives. */
class ViewsJs_InlineEdit_Test extends TestBase {

	private static Map<?,?> report;

	private static Map<?,?> report() {
		if (report == null)
			report = NodeHarness.report("edit-renderer.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(report != null, "node (or edit-renderer.cjs) not available - behavioral layer skipped");
		return report;
	}

	private static Map<?,?> map(String key) {
		return (Map<?,?>)report().get(key);
	}

	@Test void a01_rendererMarkupIsEscapedAndCarriesTheWiring() {
		assertEquals(Boolean.TRUE, report().get("registered"));
		for (var e : map("markup").entrySet())
			assertEquals(Boolean.TRUE, e.getValue(), () -> "markup." + e.getKey());
		assertEquals(Boolean.TRUE, report().get("noActionPlain"));
		assertEquals(Boolean.TRUE, report().get("hostileActionEscaped"));
		assertEquals(Boolean.TRUE, report().get("wrapClass"));
	}

	@Test void b01_visibleWhenHidesTheMissingActionAndTheFailingRow() {
		assertEquals(Boolean.TRUE, report().get("unknownActionHides"));
		assertEquals(Boolean.TRUE, report().get("visibleWhenHides"));
	}

	@Test void b02_enabledWhenDisablesWithTheReasonAndNeverOpens() {
		var d = map("enabledWhenDisables");
		assertEquals(Boolean.TRUE, d.get("disabled"));
		assertEquals("Closed rows cannot be reassigned", d.get("title"));
		assertEquals(Boolean.TRUE, report().get("disabledNoEditor"));
	}

	@Test void c01_enterCommitsTheEditedColumnThroughTheAction() {
		assertEquals(Boolean.TRUE, report().get("opened"));
		assertEquals("old", report().get("prefill"));
		var c = map("commit");
		assertEquals(1, ((Number)c.get("fetches")).intValue());
		assertEquals("/x/7", c.get("url"));
		assertEquals("assign", c.get("action"));
		assertEquals("7", c.get("targetId"));
		assertEquals(Map.of("owner", "new"), c.get("fields"));
		assertEquals(Boolean.TRUE, report().get("closedAfterSuccess"));
		assertEquals("new", report().get("shownValue"));
		assertEquals(1, ((Number)report().get("reloadedAfterSuccess")).intValue());
	}

	@Test void c02_escapeAndUnchangedValueSendNothing() {
		var e = map("escape");
		assertEquals(0, ((Number)e.get("fetches")).intValue());
		assertEquals(Boolean.TRUE, e.get("closed"));
		assertEquals("old", e.get("restored"));
		assertEquals(Boolean.TRUE, e.get("pencilBack"));
		var u = map("unchanged");
		assertEquals(0, ((Number)u.get("fetches")).intValue());
		assertEquals(Boolean.TRUE, u.get("closed"));
	}

	@Test void c03_blurCommits() {
		var b = map("blurCommit");
		assertEquals(1, ((Number)b.get("fetches")).intValue());
		assertEquals(Map.of("owner", "blurred"), b.get("fields"));
	}

	@Test void d01_failureKeepsTheEditorOpenWithTheErrorInline() {
		var f = map("failure");
		assertEquals(Boolean.TRUE, f.get("stillOpen"));
		assertEquals("Not allowed", f.get("error"));
		assertEquals(Boolean.TRUE, f.get("errorVisible"));
		assertEquals("alert", f.get("role"));
		assertEquals(Boolean.TRUE, f.get("enabled"));
		assertEquals(0, ((Number)f.get("reloads")).intValue());
		assertEquals(1, ((Number)report().get("failureBlurNoResubmit")).intValue(), "blur after a failure must not resubmit");
	}

	@Test void d02_transportRefusalAlsoSurfacesInline() {
		var r = map("refusal");
		assertEquals(Boolean.TRUE, r.get("stillOpen"));
		assertEquals(Boolean.TRUE, r.get("hasMessage"));
	}

	@Test void e01_selectInputListsTheOptionsAndPrefills() {
		var s = map("select");
		assertEquals("SELECT", s.get("tag"));
		assertEquals(List.of("x", "y", "z"), s.get("options"));
		assertEquals("y", s.get("value"));
	}

	@Test void e02_aNestedTablesPencilIsNotTheParentsToHandle() {
		assertEquals(Boolean.TRUE, report().get("nestedIgnored"));
	}
}
