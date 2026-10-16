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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.runreport.*;
import org.apache.juneau.rest.server.terminal.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Runs the shared same-origin path corpus ({@code $VIEWS/src/test/resources/same-origin-path-corpus.json}) through
 * the Java callers of {@link SafePathTemplate#isSameOriginPath(String)} that are visible from this module.  The JS half,
 * {@code same-origin-path.cjs}, runs the same rows through the shipped JS guards, so the two sides cannot drift apart
 * unnoticed.
 *
 * <p>
 * Covered elsewhere / exempt:
 * <ul>
 * 	<li>{@code RunViewChecks.isNoteHref} deliberately accepts an absolute http(s) URL, so the corpus verdict does not apply;
 * 		its vectors live in {@code RunViewChecks_Test}.  Its non-absolute branch is {@code isSafeLineHref}, covered by a04.
 * 	<li>{@code RunViewChecks.isRawHrefTemplate} requires exactly one {@code {offset}} placeholder, so the
 * 		corpus verdict does not apply; its vectors live in {@code RunViewChecks_Test}.  It delegates to {@code isSafeLineHref}.
 * 	<li>Covered transitively through {@code RegionDef.isSafeDetailEndpoint}: {@code RowDetail} ({id} templates),
 * 		{@code RunViewDef} eventsUrl and {@code RegionDef} dataUrl.
 * </ul>
 */
class SameOriginPath_Corpus_Test extends TestBase {

	private static final Path CORPUS = Path.of(System.getProperty("basedir", "."),
		"../juneau-rest-server-views/src/test/resources/same-origin-path-corpus.json");

	/** The corpus holds this many rows; adding or dropping one must be a deliberate edit of this number and of the JS test. */
	static final int CASE_COUNT = 45;

	private record Row(String name, String input, boolean accepted, boolean crossHost, Map<String,Object> guards) {}

	private static List<Row> rows() throws Exception {
		var corpus = JsonMap.ofString(Files.readString(CORPUS, UTF_8));
		var out = new ArrayList<Row>();
		for (var o : corpus.getList("cases")) {
			var c = (JsonMap)o;
			out.add(new Row(c.getString("name"), c.getString("input"), c.getBoolean("accepted"), c.getBoolean("crossHost"),
				c.containsKey("guards") ? c.getMap("guards") : Map.of()));
		}
		return out;
	}

	private static boolean throwsIae(Runnable r) {
		try {
			r.run();
			return false;
		} catch (IllegalArgumentException e) {
			return true;
		}
	}

	private static Map<String,Object> body(Object... kv) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < kv.length; i += 2)
			m.put((String)kv[i], kv[i + 1]);
		return m;
	}

	private static Map<String,Function<String,Boolean>> predicates() {
		var m = new LinkedHashMap<String,Function<String,Boolean>>();
		m.put("SafePathTemplate.isSameOriginPath", SafePathTemplate::isSameOriginPath);
		m.put("SafePathTemplate.isSafeDocumentUrl", SafePathTemplate::isSafeDocumentUrl);
		m.put("RunViewChecks.isSameOriginPath", RunViewChecks::isSameOriginPath);
		m.put("RegionDef.isSafeDetailEndpoint", RegionDef::isSafeDetailEndpoint);
		// Each caller below throws exactly when the shared core rejects, so "accepted" is "did not throw".
		m.put("ChartDef.onClickHref", s -> ! throwsIae(() -> ChartDef.donut("c").onClickHref(s)));
		m.put("ChartCardType onClick.href", s -> ! throwsIae(() -> new ChartCardType().toFragment(CardSource.create("chart", "c")
			.bodyMap(body("kind", "donut", "label", "a", "value", "b", "data", List.of(), "onClick", Map.of("href", s))).build())));
		m.put("JobLogCardType submit.endpoint", s -> ! throwsIae(() -> new JobLogCardType().toFragment(CardSource.create("job-log", "j")
			.bodyMap(body("submit", Map.of("endpoint", s))).build())));
		m.put("BadgeDef.src", s -> ! throwsIae(() -> BadgeDef.create("b").src(s).validate()));
		m.put("BadgeDef.href", s -> ! throwsIae(() -> BadgeDef.create("b").src("/p").href(s).validate()));
		m.put("MenuItem.href", s -> ! throwsIae(() -> MenuItem.link("m", "L", s).validate()));
		m.put("BarSlot.refreshUrl", s -> ! throwsIae(() -> BarSlot.create("s").widgets(BarText.of("w", "t")).refreshUrl(s).validate()));
		return m;
	}

	@Test void a01_caseCount() throws Exception {
		assertEquals(CASE_COUNT, rows().size());
	}

	@Test void a02_labelsAreConsistent() throws Exception {
		for (var r : rows())
			if (r.accepted())
				assertFalse(r.crossHost(), () -> r.name() + ": the shared core must never accept a cross-host input");
	}

	@TestFactory Stream<DynamicTest> a03_everyJavaCallerAgreesWithTheCorpus() throws Exception {
		var preds = predicates();
		return rows().stream().flatMap(r -> preds.entrySet().stream().map(e -> DynamicTest.dynamicTest(r.name() + " " + e.getKey(),
			() -> assertEquals(r.accepted(), e.getValue().apply(r.input()), () -> e.getKey() + " on " + r.name()))));
	}

	@TestFactory Stream<DynamicTest> a04_lineHrefAgrees() throws Exception {
		return rows().stream().map(r -> DynamicTest.dynamicTest(r.name(), () -> {
			var expected = r.guards().containsKey("isSafeLineHref") ? (Boolean)r.guards().get("isSafeLineHref") : r.accepted();
			assertEquals(expected, RunViewChecks.isSafeLineHref(r.input()), r.name());
			if (r.crossHost())
				assertFalse(RunViewChecks.isSafeLineHref(r.input()), r.name());
		}));
	}

	@TestFactory Stream<DynamicTest> a05_cardSrcIsValidatedForEveryCardType() throws Exception {
		var registry = CardTypeRegistry.standard();
		assertTrue(registry.isRegistered("html"));
		assertTrue(registry.isRegistered("chart"));
		assertTrue(registry.isRegistered("datatables"));
		var cards = new LinkedHashMap<String,Function<String,CardSource>>();
		cards.put("html", s -> CardSource.create("html", "h").src(s).build());
		cards.put("chart", s -> CardSource.create("chart", "c").src(s).bodyMap(body("kind", "donut", "label", "a", "value", "b")).build());
		cards.put("datatables", s -> CardSource.create("datatables", "d").src(s).build());
		return rows().stream().flatMap(r -> cards.entrySet().stream().map(e -> DynamicTest.dynamicTest(r.name() + " " + e.getKey(), () -> {
			var threw = throwsIae(() -> registry.toCard(e.getValue().apply(r.input())));
			if (r.accepted())
				assertFalse(threw, () -> e.getKey() + " card src '" + r.name() + "' must pass the page-build check");
			else {
				var ex = assertThrows(IllegalArgumentException.class, () -> registry.toCard(e.getValue().apply(r.input())));
				assertTrue(ex.getMessage().contains("'" + e.getValue().apply(r.input()).id() + "'"), () -> "names the card id: " + ex.getMessage());
				assertTrue(ex.getMessage().contains("src"), ex::getMessage);
			}
		})));
	}

	/** The real production path of an html card: PageCapture.addCard, which {@code <@card>} and PageSpec.html both reach. */
	@TestFactory Stream<DynamicTest> a06_htmlCardSrcIsCheckedWhereTheCardIsAdded() throws Exception {
		return rows().stream().map(r -> DynamicTest.dynamicTest(r.name(), () -> {
			var add = (org.junit.jupiter.api.function.Executable)() -> new PageCapture().addCard(CardSpec.html("h").src(r.input()), null);
			if (r.accepted())
				assertDoesNotThrow(add, r.name());
			else {
				var ex = assertThrows(freemarker.template.TemplateModelException.class, add, r.name());
				assertTrue(ex.getMessage().contains("<@card id='h'>"), ex::getMessage);
			}
		}));
	}

	/**
	 * CalendarEvent.href treats a blank href as "no href", so the blank corpus rows do not apply to it.
	 * CalendarDef.endpoint must also carry {year} and {month}; they are appended as a path suffix, which cannot
	 * change the verdict of a row unless that row ends in whitespace, a control character or a slash (a trailing slash would
	 * make the suffix a "//" prefix or a new segment), so those rows are skipped.
	 */
	@TestFactory Stream<DynamicTest> a07_calendarCallersAgreeWithTheCorpus() throws Exception {
		return rows().stream().flatMap(r -> {
			var tests = new ArrayList<DynamicTest>();
			if (! r.input().isBlank())
				tests.add(DynamicTest.dynamicTest(r.name() + " CalendarEvent.href", () -> {
					var e = CalendarEvent.create().id("e").title("t").start("2026-01-15").href(r.input());
					assertEquals(r.accepted(), ! throwsIae(() -> e.validate(Set.of(), null, null)), r.name());
				}));
			if (! r.input().isEmpty() && r.input().charAt(r.input().length() - 1) > ' ' && ! r.input().endsWith("/"))
				tests.add(DynamicTest.dynamicTest(r.name() + " CalendarDef.endpoint", () -> {
					var d = CalendarDef.create().id("c").endpoint(r.input() + "/{year}/{month}");
					assertEquals(r.accepted(), ! throwsIae(d::validate), r.name());
				}));
			return tests.stream();
		});
	}

	/** TerminalDef layers "no {} placeholder" on the shared rule, so a templated row is accepted by the core and refused here. */
	@TestFactory Stream<DynamicTest> a08_terminalUrlsAgreeWithTheCorpus() throws Exception {
		return rows().stream().flatMap(r -> {
			var expected = r.accepted() && ! r.input().contains("{");
			return Stream.of(
				DynamicTest.dynamicTest(r.name() + " TerminalDef.bytesUrl", () ->
					assertEquals(expected, ! throwsIae(() -> TerminalDef.create("t").bytesUrl(r.input()).validate()), r.name())),
				DynamicTest.dynamicTest(r.name() + " TerminalDef.eventsUrl", () ->
					assertEquals(expected, ! throwsIae(() -> TerminalDef.create("t").bytesUrl("/b").eventsUrl(r.input()).validate()), r.name())));
		});
	}
}
