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
package org.apache.juneau.commons.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.jar.*;
import java.util.jar.Attributes.Name;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ManifestFile_Test extends TestBase {

	private static Manifest mainManifest() {
		var m = new Manifest();
		var attrs = m.getMainAttributes();
		attrs.put(Name.MANIFEST_VERSION, "1.0");
		attrs.put(new Name("Bundle-Name"), "Test Bundle");
		attrs.put(new Name("Bundle-Version"), "1.0.0");
		return m;
	}

	private static Manifest manifestWithSections() {
		var m = mainManifest();
		var sectionAttrs = new Attributes();
		sectionAttrs.put(new Name("Section-Key"), "Section-Value");
		sectionAttrs.put(new Name("Other-Key"), "Other-Value");
		m.getEntries().put("my-section", sectionAttrs);
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - Main attribute access
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_basic() {
		var x = new ManifestFile(mainManifest());
		assertEquals("1.0", x.get("Manifest-Version").orElse(null));
		assertEquals("Test Bundle", x.get("Bundle-Name").orElse(null));
		assertEquals("1.0.0", x.get("Bundle-Version").orElse(null));
	}

	@Test void a02_missingKeyReturnsEmpty() {
		var x = new ManifestFile(mainManifest());
		assertTrue(x.get("Nonexistent").isEmpty());
	}

	@Test void a03_asMapHasMainAttrsOnly() {
		var x = new ManifestFile(manifestWithSections());
		var map = x.asMap();
		assertEquals(3, map.size());
		assertTrue(map.containsKey("Manifest-Version"));
		assertTrue(map.containsKey("Bundle-Name"));
		assertTrue(map.containsKey("Bundle-Version"));
	}

	@Test void a04_asMapUnmodifiable() {
		var x = new ManifestFile(mainManifest());
		assertThrows(UnsupportedOperationException.class, () -> x.asMap().put("k", "v"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - Section access (new capability)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_sectionGet() {
		var x = new ManifestFile(manifestWithSections());
		assertEquals("Section-Value", x.get("my-section", "Section-Key").orElse(null));
		assertEquals("Other-Value", x.get("my-section", "Other-Key").orElse(null));
	}

	@Test void b02_sectionMissingReturnsEmpty() {
		var x = new ManifestFile(manifestWithSections());
		assertTrue(x.get("nonexistent-section", "Section-Key").isEmpty());
		assertTrue(x.get("my-section", "nonexistent-key").isEmpty());
	}

	@Test void b03_sectionsList() {
		var x = new ManifestFile(manifestWithSections());
		assertEquals(1, x.sections().size());
		assertTrue(x.sections().contains("my-section"));
	}

	@Test void b04_asMapForSection() {
		var x = new ManifestFile(manifestWithSections());
		var map = x.asMap("my-section");
		assertEquals(2, map.size());
		assertEquals("Section-Value", map.get("Section-Key"));
	}

	@Test void b05_asMapForUnknownSectionEmpty() {
		var x = new ManifestFile(mainManifest());
		assertTrue(x.asMap("nonexistent").isEmpty());
	}

	@Test void b06_sectionMapUnmodifiable() {
		var x = new ManifestFile(manifestWithSections());
		assertThrows(UnsupportedOperationException.class, () -> x.asMap("my-section").put("k", "v"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - Constructor variants
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_inputStreamConstructor() throws Exception {
		var content = "Manifest-Version: 1.0\r\nBundle-Name: Stream Bundle\r\n\r\n";
		var x = new ManifestFile(new ByteArrayInputStream(content.getBytes()));
		assertEquals("1.0", x.get("Manifest-Version").orElse(null));
		assertEquals("Stream Bundle", x.get("Bundle-Name").orElse(null));
	}

	@Test void c02_readerConstructor() throws Exception {
		var content = "Manifest-Version: 1.0\r\nBundle-Name: Reader Bundle\r\n\r\n";
		var x = new ManifestFile(new StringReader(content));
		assertEquals("1.0", x.get("Manifest-Version").orElse(null));
		assertEquals("Reader Bundle", x.get("Bundle-Name").orElse(null));
	}

	@Test void c03_classConstructorNonJar() throws Exception {
		// Tests inside the project run from class folders, not jars; constructor should yield empty manifest.
		var x = new ManifestFile(ManifestFile_Test.class);
		assertTrue(x.asMap().isEmpty());
		assertTrue(x.sections().isEmpty());
	}

	@Test void c04_toString() {
		var x = new ManifestFile(mainManifest());
		var s = x.toString();
		assertTrue(s.contains("Manifest-Version: 1.0"));
		assertTrue(s.contains("Bundle-Name: Test Bundle"));
	}
}
