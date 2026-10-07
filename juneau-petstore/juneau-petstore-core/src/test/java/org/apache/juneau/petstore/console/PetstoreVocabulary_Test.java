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
package org.apache.juneau.petstore.console;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Petstore is an Apache-tree sample, so its console vocabulary must stay generic.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreVocabulary_Test extends TestBase {

	/** Terms that must never appear.  {@code uip} is matched as a whole word (it occurs inside ordinary words). */
	static final List<Pattern> DENIED = Stream.of(
			"\\buip\\b", "slds-", "salesforce", "lightning", "sfdc", "einstein", "hyperforce",
			"salesforce sans", "trailhead", "chatter", "sandbox support console")
		.map(Pattern::compile).toList();

	private static Path moduleRoot() {
		for (var p : List.of(Path.of(""), Path.of("juneau-petstore/juneau-petstore-core")))
			if (Files.isDirectory(p.resolve("src/main/java/org/apache/juneau/petstore/console")))
				return p;
		var basedir = System.getProperty("basedir");
		return basedir == null ? null : Path.of(basedir);
	}

	static List<Path> scannedFiles() throws Exception {
		var root = moduleRoot();
		assertNotNull(root, "could not locate juneau-petstore-core; the vocabulary gate would scan nothing and 'pass'");
		var out = new ArrayList<Path>();
		for (var dir : List.of("src/main/java/org/apache/juneau/petstore/console", "src/main/resources/org/apache/juneau/petstore/console")) {
			var d = root.resolve(dir);
			if (Files.isDirectory(d))
				try (var s = Files.walk(d)) {
					s.filter(Files::isRegularFile).filter(p -> p.toString().matches(".*\\.(java|ftlh|js|css)$")).forEach(out::add);
				}
		}
		return out;
	}

	static List<String> hits(String text) {
		var lower = text.toLowerCase(Locale.ROOT);
		return DENIED.stream().filter(p -> p.matcher(lower).find()).map(Pattern::pattern).toList();
	}

	@Test void a01_sourcesCarryNoDeniedTerms() throws Exception {
		var bad = new ArrayList<String>();
		for (var f : scannedFiles())
			for (var h : hits(Files.readString(f, UTF_8)))
				bad.add(String.format("Petstore vocabulary gate: '%s' contains disallowed term '%s'", f, h));
		assertEmpty(bad);
	}

	@Test void a02_servedPagesCarryNoDeniedTerms() throws Exception {
		var c = client();
		for (var path : PetstorePages_ContractTest.PAGE_PATHS)
			assertEmpty(hits(page(c, path)));
	}

	@Test void a03_theGateIsNotVacuous() throws Exception {
		assertTrue(scannedFiles().size() >= 5, "scanned too few files: " + scannedFiles());
		assertList(hits("A UIP panel with slds-button"), "\\buip\\b", "slds-");
		assertEmpty(hits("building equipment"));
		assertTrue(DENIED.size() >= 8, "the denylist was emptied out; it is the gate");
	}
}
