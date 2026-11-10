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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.w3c.dom.*;

/** The Lucide sprite and its provenance document describe the same glyphs. */
class LucideSprite_Provenance_Test extends TestBase {

	private static String read(String name) throws IOException {
		try (var in = LucideSprite_Provenance_Test.class.getResourceAsStream("/org/apache/juneau/views/" + name)) {
			assertNotNull(in, name);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static List<Element> symbols() throws Exception {
		var f = DocumentBuilderFactory.newInstance();
		f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(read("juneau-symbols-lucide.svg").getBytes(StandardCharsets.UTF_8)));
		var nl = doc.getElementsByTagName("symbol");
		var out = new ArrayList<Element>();
		for (var i = 0; i < nl.getLength(); i++)
			out.add((Element)nl.item(i));
		return out;
	}

	private static Set<String> provenanceStems() throws IOException {
		var stems = new TreeSet<String>();
		var m = Pattern.compile("^\\| ([a-z0-9_]+) \\|", Pattern.MULTILINE).matcher(read("juneau-symbols-lucide-provenance.md"));
		while (m.find())
			stems.add(m.group(1));
		stems.remove("stem");
		return stems;
	}

	@Test void a01_everySymbolHasAProvenanceRowAndViceVersa() throws Exception {
		var ids = new TreeSet<String>();
		for (var s : symbols()) {
			var id = s.getAttribute("id");
			assertTrue(id.startsWith("juneau-sym-"), id);
			ids.add(id.substring("juneau-sym-".length()));
		}
		assertFalse(ids.isEmpty());
		assertEquals(ids, provenanceStems());
	}

	@Test void a02_everySymbolIs24Square() throws Exception {
		for (var s : symbols())
			assertEquals("0 0 24 24", s.getAttribute("viewBox"), s.getAttribute("id"));
	}

	@Test void a03_noActiveContent() throws Exception {
		var svg = read("juneau-symbols-lucide.svg").toLowerCase();
		for (var bad : List.of("<script", "onload", "<image", "href=\"http", "<foreignobject"))
			assertFalse(svg.contains(bad), bad);
	}

	@Test void a04_docNamesTheTagAndLicenses() throws Exception {
		var md = read("juneau-symbols-lucide-provenance.md");
		assertTrue(Pattern.compile("v?+\\d++\\.\\d++\\.\\d++").matcher(md).find());
		assertTrue(md.contains("ISC"));
		assertTrue(md.contains("MIT"));
	}

	@Test void a05_noForbiddenContentInSpriteOrProvenance() throws Exception {
		var deny = Pattern.compile("\\b[WJ]-?\\d{4,}\\b|WORK-[A-Z]\\d+|\\bplan\\s+[A-Z]\\b|ribbon-preset|\\birs\\b|\\bgus\\b", Pattern.CASE_INSENSITIVE);
		for (var name : List.of("juneau-symbols-lucide.svg", "juneau-symbols-lucide-provenance.md")) {
			var text = read(name);
			DualHatGuard.assertClean(text);
			var m = deny.matcher(text);
			assertFalse(m.find(), () -> name + " carries forbidden content: " + m.group());
		}
	}
}
