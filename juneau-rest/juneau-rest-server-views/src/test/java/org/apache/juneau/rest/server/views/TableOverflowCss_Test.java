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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins the TODO-445n table-overflow-discipline CSS contract in {@code juneau-views.css}: the DT2 "Approach D"
 * scoped overflow box, the {@code min-width:0} constraint chain (INV-3), and the DT1 "Approach B" wrap rule.
 *
 * <p>
 * The jsdom Node harness cannot measure {@code scrollWidth}; the browser suite proves the actual scroll behavior.
 * This CSS-presence guard is the always-on floor - it fails if the overflow box or the min-width chain is dropped.
 */
class TableOverflowCss_Test extends TestBase {

	private static String css() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_CSS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/**
	 * Strips {@code &#47;* ... *&#47;} comments, then collapses runs of whitespace, so selector/declaration matching
	 * is layout-insensitive AND sees only real rules - not prose that merely mentions a selector (e.g. the
	 * "an unscoped .dt-layout-cell { overflow }" caveat comment above the scoped rule).
	 */
	private static String flat() throws IOException {
		return css().replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("\\s+", " ");
	}

	/** Approach D (DT2): the scroll box is the flex {@code .dt-layout-cell}, scoped to the table's layout row. */
	@Test void a01_dt2ScrollBoxIsScopedLayoutCell() throws Exception {
		var c = flat();
		assertTrue(c.contains(".dt-layout-row.dt-layout-table > .dt-layout-cell"),
			"the overflow box must be the SCOPED .dt-layout-row.dt-layout-table > .dt-layout-cell (N-P4-S2)");
	}

	/** The overflow selector must be scoped - an unscoped {@code .dt-layout-cell { overflow }} would scroll toolbar cells. */
	@Test void a02_noUnscopedLayoutCellOverflow() throws Exception {
		var c = flat();
		// There must be no bare ".dt-layout-cell {" rule that declares overflow (only the scoped child selector).
		// A match preceded by a ">" child combinator is the SCOPED box (".dt-layout-table > .dt-layout-cell"),
		// not a bare rule - the substring ".dt-layout-cell {" is the tail of that scoped selector, so skip it.
		var idx = c.indexOf(".dt-layout-cell {");
		while (idx >= 0) {
			var j = idx - 1;
			while (j >= 0 && c.charAt(j) == ' ') j--;
			var scoped = j >= 0 && c.charAt(j) == '>';
			if (! scoped) {
				var end = c.indexOf('}', idx);
				var rule = c.substring(idx, end < 0 ? c.length() : end);
				assertFalse(rule.contains("overflow"),
					"an unscoped .dt-layout-cell overflow rule would scroll toolbar cells (N-P4-S2): " + rule);
			}
			idx = c.indexOf(".dt-layout-cell {", idx + 1);
		}
	}

	/** Approach B (DT1 wrap) box carries overflow-x + min-width:0. */
	@Test void a03_dt1WrapBox() throws Exception {
		var c = flat();
		assertTrue(c.contains(".juneau-view-table-scroll"), "missing the DT1 wrap rule .juneau-view-table-scroll");
	}

	/** INV-3: the min-width:0 constraint chain reaches the DataTables wrapper + the DT2 layout rows/cell. */
	@Test void a04_minWidthChain() throws Exception {
		var c = flat();
		assertTrue(c.contains(".dataTables_wrapper") || c.contains(".dt-container"),
			"min-width:0 chain must reach the DataTables wrapper (.dataTables_wrapper / .dt-container)");
		// The scroll box and the constraint links declare min-width:0 so the box, not the card, absorbs overflow.
		assertTrue(c.contains("overflow-x: auto") || c.contains("overflow-x:auto"),
			"the scroll box must declare overflow-x:auto");
	}

	/** Neutral - the overflow discipline adds no color/token (the file's colours-in-chrome split, Decision #6). */
	@Test void a05_noNewToken() throws Exception {
		var c = css();
		assertFalse(c.contains("--jc-table-scroll"), "the overflow discipline must not introduce a new --jc-* token");
	}
}
