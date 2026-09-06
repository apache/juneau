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
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * WORK-J0522d, design test <b>35</b> (R11, §13): <b>the populator name is never interpolated into a selector or into
 * markup</b>.
 *
 * <p>
 * The populator name is the one piece of a region descriptor that arrives as a <i>string off a DOM attribute</i>
 * ({@code data-juneau-region-populate}) and is then used to look something up. That combination is what makes it
 * worth a test of its own: a name concatenated into a {@code querySelector} argument is a selector-injection, and a
 * name concatenated into markup is an XSS. It must only ever be a <b>key</b>.
 *
 * <p>
 * Server-side the name is additionally allowlisted at startup (design test 37, already covered by
 * {@code RegionDef_Test.b03}), so on a page Juneau emitted the name is a known-good token. This test covers the
 * client side, which cannot assume that: the runtime reads the attribute off whatever DOM it is handed.
 *
 * <h5 class='section'>The three places the name legitimately goes, established by reading every use</h5>
 * <ol>
 * 	<li>{@code registry[name]} &mdash; an own-property lookup, guarded by {@code Object.hasOwn}.
 * 	<li>An <b>error message string</b>, when no populator is registered under it. This is the interesting one: the
 * 		name genuinely does reach the DOM here, and the test asserts it arrives as <b>text</b>. The path is
 * 		{@code resolvePopulator} &rarr; {@code setRegionState(region,"error",message)} &rarr;
 * 		{@code renderAsyncStatus} &rarr; {@code status.textContent = message}.
 * 	<li>A {@code console.error} argument, which is not a DOM sink.
 * </ol>
 *
 * <p>
 * The absence of any HTML sink anywhere in {@code juneau-regions.js} is enforced separately and more broadly by
 * {@link RawContentSink_SecurityScan_Test}'s shipped-JS scan, which this item added the region runtime to; that
 * scan is the universal, and this class pins the specific flow.
 */
class Regions_PopulatorNameSafety_Test extends TestBase {

	private static String regionsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.REGIONS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.REGIONS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Strips {@code //} and block comments so prose about the name is neither a hit nor a miss. */
	private static String stripComments(String s) {
		var out = new StringBuilder(s.length());
		var i = 0;
		while (i < s.length()) {
			var c = s.charAt(i);
			if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
				while (i < s.length() && s.charAt(i) != '\n') i++;
			} else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
				var end = s.indexOf("*/", i + 2);
				i = end < 0 ? s.length() : end + 2;
				out.append('\n');
			} else {
				out.append(c);
				i++;
			}
		}
		return out.toString();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) ANTI-VACUITY: the identifiers this test reasons about must still exist and still be spelled this way.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_theAttributeAndTheNameBindingStillExist() throws Exception {
		var js = stripComments(regionsJs());
		assertTrue(js.contains("REGION_POPULATE_ATTR = \"data-juneau-region-populate\""),
			"the populator-name attribute constant moved or was renamed; every pin below is about that flow");
		assertTrue(js.contains("populateName: el.getAttribute(REGION_POPULATE_ATTR)"),
			"the one read of the populator name off the DOM moved; this test is pinned to that read");
		assertTrue(js.contains("region.populateName"),
			"the descriptor field this test tracks is gone");
	}

	@Test void a02_theRuntimeUsesSelectorsAtAll_soTheSelectorClaimIsNotVacuous() throws Exception {
		var js = stripComments(regionsJs());
		assertTrue(js.contains("querySelector"),
			"the region runtime no longer uses querySelector at all, so b01's 'no name in a selector' claim would "
				+ "hold trivially and stop being evidence of anything");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) R11: never a selector.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Every {@code querySelector}/{@code querySelectorAll}/{@code closest}/{@code matches} argument in the region
	 * runtime must be a <b>constant</b> selector &mdash; a string literal, or a literal built from the module's own
	 * attribute-name constants. What must never appear is the populator name, in any of its spellings.
	 */
	@Test void b01_noSelectorArgumentMentionsThePopulatorName() throws Exception {
		var js = stripComments(regionsJs());
		var p = Pattern.compile("(?:querySelectorAll|querySelector|closest|matches)\\s*\\(([^;]{0,400}?)\\)\\s*[;.,)\\]]");
		var m = p.matcher(js);
		var offenders = new ArrayList<String>();
		var checked = 0;
		while (m.find()) {
			checked++;
			var arg = m.group(1);
			if (arg.contains("populateName") || arg.contains("populateFn")
					|| arg.contains("REGION_POPULATE_ATTR") && arg.contains("+"))
				offenders.add(arg.trim());
		}
		assertTrue(checked > 0, "the selector scan matched nothing, so it proves nothing");
		assertTrue(offenders.isEmpty(),
			() -> "the populator name reached a selector argument - a name off a DOM attribute concatenated into a "
				+ "selector is a selector-injection.  The name must only ever be a registry KEY:\n  "
				+ String.join("\n  ", offenders));
	}

	@Test void b02_theNameIsResolvedByOwnPropertyLookupOnly() throws Exception {
		var js = stripComments(regionsJs());
		// Object.hasOwn is what stops a name like "toString" / "constructor" / "valueOf" resolving to something off
		// Object.prototype and being CALLED as a populator.  A bare `registry[name]` truthiness check would.
		assertTrue(js.contains("Object.hasOwn(registry, name) ? registry[name] : null"),
			"populator resolution must stay an own-property lookup: a bare registry[name] check would let a name "
				+ "like 'toString' or 'constructor' resolve off Object.prototype and get invoked as a populator");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) R11: never markup.  The name DOES reach the DOM on the unknown-name path, so pin that it arrives as text.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_theUnknownNameMessageIsRoutedThroughTheTextOnlyStatusRenderer() throws Exception {
		var js = stripComments(regionsJs());
		assertTrue(js.contains("no populator is registered under the name '\" + name + \"'."),
			"the unknown-populator message changed shape; c02 pins how it is painted, so this is the flow it pins");
		assertTrue(js.contains("setRegionState(region, \"error\", message)"),
			"the unknown-name message must go out through setRegionState, whose only paint route is "
				+ "renderAsyncStatus");
		assertTrue(js.contains("if (typeof render === \"function\") render(region.el, \"error\", message)"),
			"setRegionState must keep delegating the message to renderAsyncStatus rather than touching the DOM "
				+ "itself - that delegation is what makes c02's textContent pin cover this flow");
	}

	@Test void c02_theStatusRendererPaintsMessagesAsTextNotMarkup() throws Exception {
		var js = stripComments(viewsJs());
		var from = js.substring(js.indexOf("function renderAsyncStatus"));
		final var body = from.substring(0, from.indexOf("\n\tfunction ", 1));
		assertTrue(body.contains("status.textContent = message"),
			() -> "renderAsyncStatus must paint the error message with textContent.  This is the single sink the "
				+ "populator name reaches, so if it ever becomes innerHTML the name becomes an XSS vector:\n" + body);
		assertFalse(body.contains("innerHTML"),
			() -> "renderAsyncStatus must contain no innerHTML at all:\n" + body);
		assertTrue(body.contains("document.createTextNode(message"),
			() -> "the loading branch must also append the message as a TEXT NODE:\n" + body);
	}

	@Test void c03_theRegionRuntimeHasNoHtmlSinkAtAll() throws Exception {
		// The broad universal, asserted here too because it is the property that makes the name-to-markup question
		// unanswerable-by-construction rather than merely audited.  RawContentSink_SecurityScan_Test's shipped-JS
		// scan enforces the same thing across every shipped asset now that this item added the region runtime to it.
		var r = RawContentSinkScanner.scanJsHtmlSinks("juneau-regions.js", regionsJs());
		assertTrue(r.violations().isEmpty(),
			() -> "the region runtime must contain no innerHTML / .html( / outerHTML / insertAdjacentHTML / "
				+ "document.write sink whatsoever:\n  " + String.join("\n  ", r.violations()));
	}
}
