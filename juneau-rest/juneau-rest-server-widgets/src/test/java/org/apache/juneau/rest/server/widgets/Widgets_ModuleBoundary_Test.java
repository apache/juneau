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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The mechanical guards on this module's boundaries &mdash; the layering rules a reviewer can miss and the compiler
 * cannot state.  These were previously spread across individual bean tests; they are consolidated here so there is
 * one place that answers "what may this module depend on".
 *
 * <h5 class='section'>The rules</h5>
 * <ul>
 * 	<li>The widgets/views edge is one-way: views composes the widget beans, and widgets never reaches back.  Shared
 * 		behaviour the two modules both need lives further down the stack instead ({@code juneau-commons},
 * 		{@code juneau-rest-server}), never in a widgets&rarr;views dependency.
 * 	<li>The beans stay independent of the REST serving framework and of the HTML5 bean DOM: they are data plus
 * 		{@code validate()}, and the markup that renders them is emitted elsewhere.  {@link WidgetsMixin} is the single
 * 		exception, because a mixin is by definition a serving-framework type.
 * </ul>
 */
class Widgets_ModuleBoundary_Test extends TestBase {

	/** The package this module must never reference, assembled from segments so this guard's own source is not a hit. */
	private static final String VIEWS_PACKAGE = String.join(".", "org", "apache", "juneau", "rest", "server", "views");

	/** The one source file in this module allowed to reference the REST serving framework. */
	private static final String MIXIN_FILE = "WidgetsMixin.java";

	/** Resolves a source root relative to the module basedir (Surefire's working directory). */
	private static Path sourceRoot(String relative) {
		var p = Path.of(relative);
		assertTrue(Files.isDirectory(p), () -> "Source root not found at '" + p.toAbsolutePath() + "'; the test expects the module basedir as its working directory.");
		return p;
	}

	private static List<Path> javaFilesUnder(String relative) throws IOException {
		try (var s = Files.walk(sourceRoot(relative))) {
			return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
		}
	}

	/** Collects every source file under the given roots whose text matches, reported as a path list. */
	private static List<String> offenders(Predicate<String> matches, String... roots) throws IOException {
		var out = new ArrayList<String>();
		for (var root : roots)
			for (var f : javaFilesUnder(root))
				if (matches.test(Files.readString(f)))
					out.add(f.toString());
		return out;
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The widgets -> views edge does not exist, in either the sources or the classpath
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_noSourceFileInThisModuleReferencesTheViewsPackage() throws IOException {
		// Any reference, not just an import: a fully-qualified name in a signature or a javadoc link is the same
		// coupling, and would break the day views is not on the classpath.
		var o = offenders(t -> t.contains(VIEWS_PACKAGE), "src/main/java", "src/test/java");
		assertTrue(o.isEmpty(), () -> "These files reference '" + VIEWS_PACKAGE + "', which this module must not depend on: " + o);
	}

	@Test void a02_theViewsPackageIsNotEvenOnThisModulesClasspath() {
		// Belt to a01's braces: an import cannot be introduced by accident if the type is not resolvable here.
		assertThrows(ClassNotFoundException.class, () -> Class.forName(VIEWS_PACKAGE + ".ViewsMixin"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The beans stay data-only; the mixin is the module's only serving-framework type
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_onlyTheMixinReferencesTheRestServingFramework() throws IOException {
		// This module does depend on juneau-rest-server, but solely so it can declare a mixin.  Letting that reach
		// the beans would make the shared widget contracts unusable outside a REST server.
		var framework = Pattern.compile("org\\.apache\\.juneau\\.rest\\.server\\.(?!widgets)");
		var o = offenders(t -> framework.matcher(t).find(), "src/main/java");
		o.removeIf(p -> p.endsWith(MIXIN_FILE));
		assertTrue(o.isEmpty(), () -> "Only " + MIXIN_FILE + " may reference the REST serving framework; these also do: " + o);
	}

	@Test void b02_beansDoNotReferenceTheHtml5BeanDom() throws IOException {
		// The markup that renders these beans is emitted by the module that owns the emitters, not here.
		var o = offenders(t -> t.contains("org.apache.juneau.bean.html5."), "src/main/java");
		assertTrue(o.isEmpty(), () -> "Widget beans must not depend on the HTML5 bean DOM: " + o);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) The mixin's declared surface matches what this module actually ships
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_contractVersionReExports_areAliasesOfTheBeanConstants_notBakedLiterals() throws IOException {
		// These fields are compile-time constants, so a baked literal is indistinguishable from an alias at runtime -
		// and would silently stop tracking the bean when the bean's wire contract is revised.  The assignment shape
		// is therefore checked in the source.
		var src = mixinSource();
		assertTrue(src.contains("CARDS_CONTRACT_VERSION = CardFieldList.CONTRACT_VERSION"), src);
		assertTrue(src.contains("CALENDAR_CONTRACT_VERSION = CalendarDef.CONTRACT_VERSION"), src);
		assertTrue(src.contains("HEADER_CONTRACT_VERSION = AppHeaderDef.CONTRACT_VERSION"), src);
		assertTrue(src.contains("BAR_CONTRACT_VERSION = BarSlot.CONTRACT_VERSION"), src);
	}

	/**
	 * The handler and the bytes arrive together.  This module now ships the four relocated widget runtime assets, so
	 * the mixin must declare a serving endpoint for each: a declared mount with no accessor would answer 404, and an
	 * accessor with no bytes would answer an empty 200.  Pinning the resource set as well as the endpoint count is
	 * what keeps the two halves from drifting apart in either direction.
	 */
	@Test void c02_theMixinDeclaresOneServingEndpointForEachAssetItShips() throws IOException {
		var dir = Path.of("src/main/resources/org/apache/juneau/widgets");
		assertTrue(Files.isDirectory(dir), () -> "This module must ship its widget assets at " + dir);
		try (var s = Files.list(dir)) {
			assertEquals(
				List.of("juneau-calendar.css", "juneau-calendar.js", "juneau-cards.js", "juneau-chrome.js"),
				s.map(x -> x.getFileName().toString()).sorted().toList());
		}
		// Anchored to the start of a line so a javadoc mention of the annotation is not a hit.
		var m = Pattern.compile("(?m)^\\s*@RestGet").matcher(mixinSource());
		var endpoints = 0;
		while (m.find())
			endpoints++;
		assertEquals(4, endpoints, "One serving endpoint per shipped asset, no more and no fewer.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) The dialog beans live here, not next door
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_dialogBeans_resolveFromThisModulesPackage() {
		// The declarative dialog contracts are general widget contracts, so they belong to this module.  Resolving
		// them by name (rather than referencing the types) is what makes this a genuine relocation guard: a stale
		// import elsewhere cannot satisfy it.
		for (var n : new String[]{"ModalDef", "FormDef", "FormDef$Input", "FormDef$Section", "ModalDef$Field"})
			assertDoesNotThrow(() -> Class.forName("org.apache.juneau.rest.server.widgets." + n), n);
	}

	@Test void d02_dialogBeans_areGoneFromTheViewsPackage() {
		// No shim: a deprecated forwarding class left behind in views would re-create the views/widgets tangle in
		// reverse, so the old coordinates must resolve to nothing at all.
		for (var n : new String[]{"ModalDef", "FormDef"})
			assertThrows(ClassNotFoundException.class, () -> Class.forName(VIEWS_PACKAGE + "." + n), n);
	}

	@Test void d03_dialogBeans_carryNoDanglingLinkToAViewsType() throws IOException {
		// A {@link} from here to a table/row-action type would not resolve and would fail the javadoc gate.  a01
		// already forbids the fully-qualified package; these are the simple names those links used, which have to
		// have become prose or {@code} instead.
		var link = Pattern.compile("\\{@link\\s+(RowAction|IdempotencyKey|ActionResult)\\b");
		var o = offenders(t -> link.matcher(t).find(), "src/main/java", "src/test/java");
		assertTrue(o.isEmpty(), () -> "These files {@link} a views type by simple name, which cannot resolve here: " + o);
	}

	private static String mixinSource() throws IOException {
		return Files.readString(sourceRoot("src/main/java").resolve("org/apache/juneau/rest/server/widgets/" + MIXIN_FILE));
	}
}
