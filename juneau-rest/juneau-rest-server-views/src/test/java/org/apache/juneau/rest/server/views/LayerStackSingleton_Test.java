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
 * Mechanical guard for the toolkit's <b>one</b> popup-layer stack: exactly one runtime file
 * ({@code juneau-views.js}) may <b>define</b> {@code pushLayer}/{@code popLayer} or escalate layer z-indexes, and
 * every other runtime file may only be a <b>client</b> of it.
 *
 * <h5 class='section'>Defines vs calls &mdash; the distinction this guard is built on:</h5>
 * <p>
 * "One stack" is a statement about <b>ownership</b>, not about references.  {@code juneau-chrome.js} and
 * {@code juneau-calendar.js} are legitimate, deliberate clients: they resolve
 * {@code window.JuneauViews.init.pushLayer} and call it, and a guard that merely searched for the identifier would
 * fail them for doing exactly the right thing.  So this guard looks only for <b>definition</b> forms &mdash; a
 * {@code function pushLayer(...)} declaration or a {@code pushLayer = ...} binding &mdash; and separately proves the
 * known clients still call through a resolved stack object, so the guard can never pass just because nothing
 * references layers any more.
 *
 * <p>
 * Comments are stripped before matching, because both client files legitimately <i>discuss</i>
 * {@code pushLayer}/{@code popLayer} in prose that explains why they do not define one.
 *
 * <h5 class='section'>Why a test and not a review promise:</h5>
 * <p>
 * A second stack does not announce itself: it looks like a local {@code openMenu()} that appends to the body and
 * picks its own z-index, and it only misbehaves later, when Escape unwinds the wrong surface or two popups fight
 * over stacking.  Catching it needs a gate that runs on every build.
 */
class LayerStackSingleton_Test extends TestBase {

	/** The one runtime file allowed to own the stack. */
	private static final String OWNER = ViewsMixin.VIEWS_JS_RESOURCE;

	/** Every runtime JS resource the module serves. */
	private static final List<String> RUNTIME_JS = List.of(
		ViewsMixin.VIEWS_JS_RESOURCE,
		ViewsMixin.RENDERS_JS_RESOURCE,
		ViewsMixin.RIBBON_JS_RESOURCE,
		ViewsMixin.ICONS_JS_RESOURCE,
		ViewsMixin.PAGES_JS_RESOURCE,
		ViewsMixin.CONFIG_JS_RESOURCE,
		ViewsMixin.CARDS_JS_RESOURCE,
		ViewsMixin.CALENDAR_JS_RESOURCE,
		ViewsMixin.CHROME_JS_RESOURCE
	);

	private static String read(String resource) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Strips block and line comments so prose ABOUT the stack is never mistaken for code. */
	private static String code(String resource) throws IOException {
		return read(resource).replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
	}

	/** {@code function NAME(} or {@code NAME =} (a binding) - but never {@code NAME ===} / {@code NAME ==}. */
	private static Pattern defines(String name) {
		return Pattern.compile("\\bfunction\\s+" + name + "\\s*\\(|\\b" + name + "\\s*=(?!=)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// The guard.
	//------------------------------------------------------------------------------------------------------------------

	/** No runtime file other than {@code juneau-views.js} may DEFINE {@code pushLayer} / {@code popLayer}. */
	@Test void a01_onlyViewsJsDefinesTheLayerStack() throws Exception {
		for (var res : RUNTIME_JS) {
			if (OWNER.equals(res)) continue;
			var body = code(res);
			for (var fn : List.of("pushLayer", "popLayer")) {
				var m = defines(fn).matcher(body);
				assertFalse(m.find(),
					() -> res + " DEFINES " + fn + " - the toolkit has exactly one layer stack, in juneau-views.js."
						+ " Resolve window.JuneauViews.init." + fn + " and call it instead. Offending text: "
						+ body.substring(Math.max(0, m.start() - 60), Math.min(body.length(), m.end() + 60)));
			}
		}
	}

	/** The owner really does define both halves - otherwise {@code a01} could pass vacuously. */
	@Test void a02_viewsJsIsTheOwner() throws Exception {
		var body = code(OWNER);
		assertTrue(defines("pushLayer").matcher(body).find(), "juneau-views.js must define pushLayer");
		assertTrue(defines("popLayer").matcher(body).find(), "juneau-views.js must define popLayer");
		assertTrue(body.contains("popupLayerStack"), "juneau-views.js must own the popupLayerStack array");
	}

	/**
	 * No runtime file other than the owner may run a competing z-index escalator: the per-layer inline z-index is
	 * computed from {@code --jc-dialog-z} + {@code --jc-layer-step} in exactly one place, and a second file doing its
	 * own stacking arithmetic is the same bug as a second stack, just spelled differently.
	 */
	@Test void a03_onlyViewsJsEscalatesLayerZIndex() throws Exception {
		for (var res : RUNTIME_JS) {
			if (OWNER.equals(res)) continue;
			var body = code(res);
			for (var token : List.of("--jc-dialog-z", "--jc-layer-step"))
				assertFalse(body.contains(token),
					() -> res + " reads " + token + " - layer z-index escalation belongs to juneau-views.js alone");
			assertFalse(body.contains("style.zIndex") || body.contains("zIndex ="),
				() -> res + " stamps its own zIndex - let pushLayer assign the per-depth z-index");
		}
	}

	/**
	 * The clients are still clients.  This is the half that proves {@code a01} is testing "defines", not "mentions":
	 * both files reference the stack heavily and must keep passing.
	 */
	@Test void a04_knownClientsStillCallTheSharedStack() throws Exception {
		for (var res : List.of(ViewsMixin.CHROME_JS_RESOURCE, ViewsMixin.CALENDAR_JS_RESOURCE)) {
			var body = code(res);
			assertTrue(body.contains(".pushLayer("),
				() -> res + " no longer calls the shared pushLayer - it must remain a client, not grow its own stack");
			assertTrue(body.contains(".popLayer("),
				() -> res + " no longer calls the shared popLayer");
			// A client reaches the stack THROUGH the published namespace rather than re-implementing it.
			assertTrue(body.contains("JuneauViews"), () -> res + " must resolve the stack via window.JuneauViews");
		}
	}

	/**
	 * Proves the guard itself discriminates, against synthetic sources rather than by editing a runtime file: the
	 * definition forms a rogue second stack would take must MATCH, and every shape a legitimate client takes must
	 * NOT.  Without this, {@code a01} could silently degrade into a pattern that matches nothing at all.
	 */
	@Test void a05_theGuardDistinguishesDefiningFromCalling() {
		var p = defines("pushLayer");
		for (var rogue : List.of(
			"function pushLayer(el, opts) { }",
			"function  pushLayer (el) { }",
			"const pushLayer = function (el) { };",
			"pushLayer = (el) => { };",
			"var pushLayer=function(){};"
		))
			assertTrue(p.matcher(rogue).find(), () -> "the guard must catch a second stack definition: " + rogue);

		for (var client : List.of(
			"views.pushLayer(menu, { kind: \"menu\" });",
			"stack.pushLayer(pop, {});",
			"return typeof views.pushLayer === \"function\" ? views : null;",
			"pushLayer: pushLayer,",
			"if (views.pushLayer == null) return;"
		))
			assertFalse(p.matcher(client).find(), () -> "the guard must not fail a legitimate client: " + client);
	}
}
