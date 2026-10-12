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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * A row action with {@link RowAction#confirm(String) confirm} (or {@link RowAction#confirmRenderer(String)
 * confirmRenderer}) prompts before it submits even without {@code present(DIALOG)}, from every surface that
 * dispatches a row action: the row-action menu, a cell pill, a detail-panel ActionBar button and
 * {@code JuneauViews.rowActions.run}.  Cancel sends nothing; Confirm sends exactly one request.  An action with no
 * confirm, or with {@link RowAction#noConfirm() noConfirm}, still submits directly.
 *
 * <p>Behavioral Node harness ({@code confirm-without-dialog.cjs}); skipped when {@code node} is not on {@code PATH}.
 */
class ViewsJs_ConfirmWithoutDialog_Test extends TestBase {

	private static final List<String> SURFACES = List.of("menu", "pill", "detail", "run");

	private static Map<?,?> report;

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var views = Files.createTempFile("juneau-views-", ".js");
		var renders = Files.createTempFile("juneau-renders-", ".js");
		var stdout = Files.createTempFile("confirm-without-dialog-stdout-", ".json");
		var stderr = Files.createTempFile("confirm-without-dialog-stderr-", ".txt");
		try {
			Files.writeString(views, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(renders, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			var p = new ProcessBuilder(List.of("node", harness.toString(), renders.toString(), views.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("confirm-without-dialog.cjs did not finish within 30s; stderr:\n" + Files.readString(stderr, UTF_8));
			}
			if (p.exitValue() != 0)
				fail("confirm-without-dialog.cjs exited " + p.exitValue() + "; stderr:\n" + Files.readString(stderr, UTF_8));
			report = Json.to(Files.readString(stdout, UTF_8), Map.class);
		} finally {
			for (var f : List.of(views, renders, stdout, stderr))
				Files.deleteIfExists(f);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		var candidates = new ArrayList<Path>();
		if (basedir != null)
			candidates.add(Path.of(basedir, "src/test/js/confirm-without-dialog.cjs"));
		candidates.add(Path.of("src/test/js/confirm-without-dialog.cjs"));
		candidates.add(Path.of("juneau-rest/juneau-rest-server-views/src/test/js/confirm-without-dialog.cjs"));
		return candidates.stream().filter(Files::isRegularFile).map(p -> p.toAbsolutePath().normalize()).findFirst().orElse(null);
	}

	private static Map<?,?> surface(String name) {
		assumeTrue(report != null, "node not available or confirm-without-dialog.cjs not found - behavioral layer skipped");
		return (Map<?,?>) report.get(name);
	}

	@Test void a01_cancelSendsNothing() {
		for (var s : SURFACES)
			assertBean(surface(s), "cancel{fetchesAtClick,title,cancelled,fetchesAfterCancel,closed}", "{0,Delete 'Rex'?,true,0,true}");
	}

	@Test void a02_confirmSendsExactlyOneRequest() {
		for (var s : SURFACES)
			assertBean(surface(s), "confirm{fetchesAtClick,title,confirmed,fetches,url,method,action}", "{0,Delete 'Rex'?,true,1,/x/7,DELETE,delete}");
	}

	@Test void a03_noConfirmStillSubmitsDirectly() {
		for (var s : SURFACES)
			assertBean(surface(s), "plain{fetches,dialogs},noConfirm{fetches,dialogs}", "{1,0},{1,0}");
	}

	@Test void a04_aNamedConfirmRendererGatesTheWrite() {
		assertBean(surface("renderer"), "no{asked,fetches},yes{asked,fetches},titles", "{1,0},{2,1},[Delete,Delete]");
	}
}
