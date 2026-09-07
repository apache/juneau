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

/**
 * The row-id containment a row-detail region depends on: <b>{@code expandDetailRow} stamps the row's id onto the
 * detail panel</b>.
 *
 * <h5 class='section'>The gap</h5>
 * <p>
 * DataTables inserts a child row as a <b>sibling</b> {@code <tr>} of the row it belongs to, never as a descendant of
 * it. The detail panel &mdash; and every region container cloned into it &mdash; therefore sits <i>outside</i> the
 * {@code <tr>} carrying {@code data-juneau-row-id}. Since {@code juneau-regions.js}'s {@code readIds} resolves every
 * enclosing identity with {@code el.closest("[data-juneau-row-id]")}, a panel region would read a null row id, and
 * {@code resolveDeclaredUrl} would then refuse the {@code {id}} substitution outright &mdash; or, for an author's own
 * populate doing its own interpolation, build a literal {@code /.../null/...} path and collect a 400 from the
 * server's path-param resolver. Both surface to a user as an empty or broken region body.
 *
 * <h5 class='section'>Why behavioral rather than a source-shape pin</h5>
 * <p>
 * The claim is not "a {@code setAttribute} call exists" &mdash; it is that a region inside the panel resolves the
 * <b>right</b> id and fetches the <b>right</b> url. That spans two runtimes ({@code expandDetailRow} in
 * {@code juneau-views.js} writes the stamp; {@code readIds}/{@code resolveDeclaredUrl} in {@code juneau-regions.js}
 * read it), so only running both against one DOM proves they agree. The harness drives the real production path:
 * {@code initDetailsExpander}'s delegated click &rarr; {@code toggleDetailRow} &rarr; {@code expandDetailRow} &rarr;
 * {@code NS.regions.enrolIn(panel)}.
 *
 * <h5 class='section'>The claims are paired with controls</h5>
 * <p>
 * "The region resolved the right id" is a claim a <b>broken harness</b> can satisfy by accident &mdash; most simply
 * by building a fixture where the panel was never a sibling in the first place, making the stamp a redundant
 * restatement of something {@code closest} would have found anyway. So the harness also proves the containment gap is
 * real ({@code gap_*}), and re-runs the identical enrolment against a panel whose stamp has been removed
 * ({@code unstamped_*}) to show it degrades exactly as described. The pair is the evidence; either half alone is not.
 *
 * <p>
 * Runs whenever {@code node} is on {@code PATH} and is skipped (not failed) otherwise, so it exercises on any
 * developer or CI machine with node without becoming a hard toolchain dependency.
 */
class ViewsJs_DetailRegionRowId_Test extends TestBase {

	private static final String HARNESS = "detail-region-row-id.cjs";

	private static Map<?,?> report() {
		var r = RegionsHarness.report(HARNESS);
		assumeTrue(r != null, "node not available or " + HARNESS + " not found - behavioral layer skipped");
		assertEquals(true, r.get("hasFixture"), () -> "the harness never built a detail panel: " + r);
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	/**
	 * The stamp itself, and the read that depends on it: the panel carries the row's id, and a region cloned into the
	 * panel resolves that id the way every consumer resolves it &mdash; by {@code closest(...)}.
	 */
	@Test void a01_panelCarriesTheRowIdAndItsRegionResolvesIt() {
		assertAllTrue(report(), "panelCarriesRowId", "regionResolvesRowIdByClosest");
	}

	/**
	 * End to end, and the part a user would have seen break: the region's declared {@code {id}} url is fetched with
	 * the real id substituted in &mdash; never with a literal {@code {id}} still in the path, and never as
	 * {@code /.../null/...}.
	 */
	@Test void a02_regionFetchesTheSubstitutedUrl() {
		assertAllTrue(report(),
			"regionFetchedSubstitutedUrl", "regionNeverFetchedNull", "regionNeverFetchedLiteralTemplate");
	}

	/**
	 * The fixture really did drive the production expand path rather than a stub of it: the panel issued its OWN
	 * detail GET, substituted, which only {@code expandDetailRow} does.
	 */
	@Test void a03_theFixtureDroveTheRealExpandPath() {
		assertAllTrue(report(), "panelFetchedItsOwnDetailUrl");
	}

	/**
	 * CONTROL 1 &mdash; the containment gap is real. The panel is genuinely not inside the row's {@code <tr>}, which
	 * is what makes the stamp load-bearing instead of redundant. Were this to start failing, {@code a01} would be
	 * passing for a reason that has nothing to do with the stamp.
	 */
	@Test void a04_control_panelIsNotADescendantOfItsRow() {
		assertAllTrue(report(), "gap_panelIsNotInsideTheRowTr", "gap_panelIsASiblingSubtree");
	}

	/**
	 * CONTROL 2 &mdash; removing the stamp degrades the same enrolment: the region resolves no row id at all, and the
	 * declared fetch it would have issued is refused rather than sent. This is the observable difference the stamp
	 * makes, and without it a harness whose region never fetched at all would look identical to a working one.
	 */
	@Test void a05_control_withoutTheStampTheRegionResolvesNothing() {
		assertAllTrue(report(), "unstamped_resolvesNoRowId", "unstamped_neverFetchedSubstitutedUrlTwice");
	}
}
