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

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Option snapshots of the {@code juneau-charts.js} pipeline (frame, helper, decorators, patch, render mode,
 * large-data defaults, {@code $fn}) against goldens under {@code src/test/resources/charts-goldens}.
 *
 * <p>
 * The goldens follow the console-ui {@code GoldenFiles} rule: a missing golden is written and the test fails, and
 * {@code -Djuneau.golden.update=true} rewrites them all.  Review the diff before re-running without it.  A golden file
 * with no case in {@code NAMES} fails the test (delete it).
 *
 * <p>
 * Besides the goldens the runner reports the warnings each case logged, the error each failing case threw, and a set of
 * boolean behaviour checks (the mode a {@code $fn} is built in, {@code build()} returning a promise, frozen tokens,
 * {@code __proto__} keys); each is asserted here.
 *
 * <p>
 * The whole JS layer is skipped (via {@code assumeTrue}, the {@link NodeHarness} convention) when {@code node} is not
 * installed, so on a node-less build these goldens guard nothing.
 */
class ConsoleChartsOptions_Test extends TestBase {

	private static final boolean UPDATE = Boolean.getBoolean("juneau.golden.update");

	// The golden names, in the runner's order.  A new case must be added here and to charts-options.cjs.
	private static final List<String> NAMES = List.of(
		"line", "bar-stacked", "bar-horizontal", "donut-center-total", "donut-center-literal", "pie", "heatmap-extent",
		"heatmap-scale", "scatter", "legacy-line", "raw-own-dataset", "raw-no-binding", "patch-by-id", "patch-no-ids-replaces",
		"option-over-helper", "render-mode-html-kept", "raw-boxplot", "render-mode-html-string-formatter", "render-mode-auto",
		"render-mode-empty", "render-mode-false", "render-mode-upper", "render-mode-array", "render-mode-base-option",
		"render-mode-media", "render-mode-options", "raw-own-dataset-transform", "y-format-integer", "y-format-unknown",
		"axis-type-unknown", "tone-numeric", "marker-extra-key", "scatter-symbol-size", "legacy-scatter-symbol-size",
		"bool-column", "donut-center-total-skips-null", "error-kind-unknown", "error-kind-constructor", "error-kind-to-string", "error-kind-proto",
		"error-fn-unknown", "error-decorator-unknown", "large-on", "large-explicit-wins", "large-off-below-threshold",
		"large-progressive-override", "large-off-at-threshold", "large-progressive-false", "large-scatter",
		"y-format", "scatter-size", "fn-in-option", "markers-values", "bands-gaps", "bands-values", "thresholds",
		"annotations-presets", "annotations-values", "decorators-order", "error-fn-option-to-content", "fns"
	);

	private static final String DOWNGRADED = "[juneau-charts] chart card 'c' tooltip renderMode '%s' needs a $fn formatter; using 'richText'";

	private static String downgraded(String mode) { return DOWNGRADED.formatted(mode); }

	// The warnings each case logs; every other case logs none.
	private static final Map<String,List<String>> WARNINGS = Map.of(
		"render-mode-html-string-formatter", List.of(downgraded("html")),
		"render-mode-auto", List.of(downgraded("auto")),
		"render-mode-empty", List.of(downgraded("")),
		"render-mode-false", List.of(downgraded("false")),
		"render-mode-upper", List.of(downgraded("HTML")),
		"render-mode-array", List.of(downgraded("html")),
		"render-mode-base-option", List.of(downgraded("html")),
		"render-mode-media", List.of(downgraded("html")),
		"render-mode-options", List.of(downgraded("auto"), downgraded("html"))
	);

	// The error each failing case throws (the registered-names list is replaced by "<list>" in the report).
	private static final Map<String,String> THROWN = Map.of(
		"error-kind-unknown", "E-JS-60: chart card 'c' has unknown kind 'nope'; registered: <list>",
		"error-kind-constructor", "E-JS-60: chart card 'c' has unknown kind 'constructor'; registered: <list>",
		"error-kind-to-string", "E-JS-60: chart card 'c' has unknown kind 'toString'; registered: <list>",
		"error-kind-proto", "E-JS-60: chart card 'c' has unknown kind '__proto__'; registered: <list>",
		"error-fn-unknown", "E-JS-90: 'c' references unknown $fn 'app.nope'; registered: <list>",
		"error-decorator-unknown", "E-JS-91: chart card 'c' uses unknown decorator 'app.nope'; registered: <list>",
		"error-fn-option-to-content", "E-JS-98: 'c' fn 'app.tip' failed: 'a $fn may not format optionToContent (ECharts renders its result as html)'"
	);

	private static Map<?,?> report() {
		// The fn outputs (number, datetime) depend on the locale and the time zone.
		var r = NodeHarness.report(Map.of("LC_ALL", "en_US.UTF-8", "TZ", "UTC"), "charts-options.cjs", ConsoleChromeMixin.CHARTS_JS_RESOURCE);
		assumeTrue(r != null, "node (or charts-options.cjs) not available - JS layer skipped");
		return r;
	}

	// Unlike GoldenFiles.assertGolden, every missing or updated golden is written before the one failure.
	@Test void a01_snapshots_matchGoldens() throws IOException {
		var goldens = (Map<?,?>)report().get("goldens");
		assertEquals(NAMES, new ArrayList<>(goldens.keySet()));
		var written = new ArrayList<String>();
		for (var name : NAMES) {
			var file = Path.of("src/test/resources/charts-goldens", name + ".json");
			var actual = (String)goldens.get(name);
			if (UPDATE || ! Files.exists(file)) {
				Files.createDirectories(file.getParent());
				Files.writeString(file, actual, StandardCharsets.UTF_8);
				written.add(file.toAbsolutePath().toString());
			} else {
				assertEquals(Files.readString(file, StandardCharsets.UTF_8), actual, () -> "Golden mismatch: " + file);
			}
		}
		// A golden whose case was removed or renamed must be deleted.
		try (var files = Files.list(Path.of("src/test/resources/charts-goldens"))) {
			var onDisk = files.map(f -> f.getFileName().toString()).sorted().toList();
			assertEquals(NAMES.stream().map(n -> n + ".json").sorted().toList(), onDisk);
		}
		// The legacy flat form draws the same option as the helper form.
		assertEquals(goldens.get("line"), goldens.get("legacy-line"));
		assertEquals(goldens.get("scatter-symbol-size"), goldens.get("legacy-scatter-symbol-size"));
		if (! written.isEmpty())
			fail("Goldens written: " + written + " - review them, then re-run without -Djuneau.golden.update.");
	}

	@Test void a02_warningsAndErrors_perCase() {
		var r = report();
		// The throwing-fn-blank case calls a fn that throws: it logs E-JS-98 once and formats as "".
		assertEquals(List.of("[juneau-charts] E-JS-98: 'c' fn 'app.boom' failed: 'boom'"), r.get("errors"));
		var caseWarnings = (Map<?,?>)r.get("caseWarnings");
		for (var name : NAMES)
			assertEquals(WARNINGS.getOrDefault(name, List.of()), caseWarnings.get(name), name);
		assertEquals(new TreeMap<>(THROWN), new TreeMap<>((Map<?,?>)r.get("thrown")));
	}

	@Test void a03_behaviourChecks() {
		var checks = (Map<?,?>)report().get("checks");
		assertFalse(checks.isEmpty());
		var failed = checks.entrySet().stream().filter(e -> ! Boolean.TRUE.equals(e.getValue())).map(e -> e.getKey()).toList();
		assertEquals(List.of(), failed);
	}
}
