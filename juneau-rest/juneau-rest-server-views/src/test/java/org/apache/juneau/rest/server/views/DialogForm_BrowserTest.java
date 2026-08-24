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
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The dialog-form half of the module's <b>JavaScript-execution harness</b>: runs the REAL served {@code juneau-views.js}
 * in a real headless browser and asserts the complex-dialog-form behavior as a user would experience it &mdash; the
 * six typed inputs paint (with a keyboard-operable {@code role=switch} toggle and a native {@code select}), the modal
 * traps focus and wraps Tab within itself, a confirm on an invalid form is blocked and focuses the first invalid
 * control, a valid confirm submits a body carrying the checkbox/toggle as explicit boolean strings, and the nested
 * {@code type=action} button opens a SECOND stacked dialog without closing the first.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape + Node harness tests):</h5>
 * <p>
 * {@link ViewsJs_DialogForm_Test} and {@link ViewsJs_DialogValidation_Test} prove the shipped script contains the
 * painter and validation logic and behave correctly under a DOM shim; only a real browser proves the focus trap,
 * keyboard Tab wrapping, and real click-driven submit/stacking actually work.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code dialog-form-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is
 * needed.
 */
@EnabledIfSystemProperty(named=DialogForm_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class DialogForm_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("dialog-form-browser.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("dialog-form.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("dialog-form-stdout.json");
		var stderr = dir.resolve("dialog-form-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> sub(String key) {
		return (Map<String,Object>) report.get(key);
	}

	@Test void a01_runtimeLoadedWithoutErrors() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void b01_sixTypedInputsPaintWithKeyboardOperableToggleAndSelect() {
		var p = sub("paint");
		assertEquals(Boolean.TRUE, p.get("formVisible"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("hasTextarea"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("hasText"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("hasCheckbox"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("toggleRoleSwitch"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("toggleCheckedFromToken"), () -> report.toString());
		assertEquals(2L, ((Number) p.get("selectOptionCount")).longValue(), () -> report.toString());
		assertEquals("warning", p.get("selectPrefill"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("actionEnabled"), () -> report.toString());
	}

	@Test void b02_modalTrapsFocusAndTabWrapsWithinTheDialog() {
		var p = sub("paint");
		assertEquals(Boolean.TRUE, p.get("focusTrappedIntoDialog"), () -> "focus did not move into the dialog: " + report);
		assertEquals(Boolean.TRUE, p.get("tabWrapsToFirst"), () -> "Tab from the last control did not wrap to the first: " + report);
		assertEquals(Boolean.TRUE, p.get("tabKeepsFocusInDialog"), () -> "Tab escaped the trapping layer: " + report);
	}

	@Test void c01_confirmOnInvalidFormIsBlockedAndFocusesFirstInvalid() {
		var i = sub("invalid");
		assertEquals(Boolean.TRUE, i.get("submitBlocked"), () -> "an invalid form was submitted: " + report);
		assertEquals(Boolean.TRUE, i.get("notesAriaInvalid"), () -> report.toString());
		assertEquals(Boolean.TRUE, i.get("focusOnFirstInvalid"), () -> "confirm did not focus the first invalid control: " + report);
		assertEquals(Boolean.TRUE, i.get("dialogStillOpen"), () -> "the dialog closed on a blocked confirm: " + report);
	}

	@Test void d01_validConfirmSubmitsBooleanStringFieldValues() {
		var s = sub("submit");
		assertEquals(Boolean.TRUE, s.get("issued"), () -> "a valid form did not submit: " + report);
		var body = String.valueOf(s.get("body"));
		assertTrue(body.contains("\"fields\""), () -> "fields object missing from submit body: " + report);
		assertTrue(body.contains("looks fine"), () -> "textarea value missing: " + report);
		// Checkbox checked -> "true", toggle unchecked -> explicit "false" (decision 9: never omitted).
		assertTrue(body.contains("\"agree\":\"true\""), () -> "checkbox boolean string missing: " + report);
		assertTrue(body.contains("\"notify\":\"false\""), () -> "toggle boolean string missing: " + report);
	}

	@Test void e01_nestedActionButtonOpensSecondStackedDialog() {
		var n = sub("nested");
		assertEquals(1L, ((Number) n.get("before")).longValue(), () -> report.toString());
		assertEquals(2L, ((Number) n.get("after")).longValue(), () -> "the type=action button did not open a second dialog: " + report);
		assertEquals(2L, ((Number) n.get("twoBackdrops")).longValue(), () -> "the first dialog was not kept open: " + report);
	}
}
