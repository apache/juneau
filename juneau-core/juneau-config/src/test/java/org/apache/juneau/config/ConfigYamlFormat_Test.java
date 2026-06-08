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
package org.apache.juneau.config;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.config.format.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1130", // Test methods use the project-standard broad 'throws Exception' signature; narrowing each to the specific checked type (IOException) is high-churn/low-value.
	"resource"    // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ConfigYamlFormat_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// End-to-end behavior through Config / MemoryStore.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_readNestedPathValues() throws Exception {
		var s = MemoryStore.create().build();
		s.update("A.yaml",
			"# top",
			"foo:",
			"  bar:",
			"    k1: v1",
			"    k2: v2 # note"
		);
		var c = Config.create().store(s).name("A.yaml").build();

		assertEquals("v1", c.getString("foo/bar/k1"));
		assertEquals("v2", c.getString("foo/bar/k2"));
	}

	@Test void a02_writeYamlRoundTrip() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("B.yaml").build();

		c.set("foo/bar/k1", "v1");
		c.set("foo/bar/k2", "v2");
		c.commit();

		assertEquals("foo:\n  bar:\n    k1: v1\n    k2: v2\n", s.read("B.yaml"));
	}

	@Test void a03_defaultSectionMixedWithNestedSection() throws Exception {
		var s = MemoryStore.create().build();
		s.update("C.yaml",
			"k0: v0",
			"foo:",
			"  k1: v1"
		);
		var c = Config.create().store(s).name("C.yaml").build();

		assertEquals("v0", c.getString("k0"));
		assertEquals("v1", c.getString("foo/k1"));
	}

	@Test void a04_yamlDocumentMarkerIgnored() throws Exception {
		var s = MemoryStore.create().build();
		s.update("D.yaml",
			"---",
			"foo:",
			"  k1: v1"
		);
		var c = Config.create().store(s).name("D.yaml").build();

		assertEquals("v1", c.getString("foo/k1"));
	}

	@Test void a05_importsLineIgnored() throws Exception {
		var s = MemoryStore.create().build();
		s.update("E.yaml",
			"_imports: [other]",
			"foo:",
			"  k1: v1"
		);
		var c = Config.create().store(s).name("E.yaml").build();

		assertEquals("v1", c.getString("foo/k1"));
	}

	@Test void a06_singleQuotedValueParsed() throws Exception {
		var s = MemoryStore.create().build();
		s.update("F.yaml",
			"foo:",
			"  k1: 'has: colon'"
		);
		var c = Config.create().store(s).name("F.yaml").build();

		assertEquals("has: colon", c.getString("foo/k1"));
	}

	@Test void a07_doubleQuotedValueParsed() throws Exception {
		var s = MemoryStore.create().build();
		s.update("G.yaml",
			"foo:",
			"  k1: \"hash: # value\""
		);
		var c = Config.create().store(s).name("G.yaml").build();

		// The "#" splits inline-comments since it's prefixed by a space.
		assertEquals("\"hash:", c.getString("foo/k1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Direct unit tests on YamlConfigFormat (drive branch coverage on helpers).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_idIsYaml() {
		assertEquals("yaml", YamlConfigFormat.INSTANCE.id());
	}

	@Test void b02_toInternalNullReturnsEmpty() throws Exception {
		assertEquals("", YamlConfigFormat.INSTANCE.toInternal(null));
	}

	@Test void b03_toInternalEmptyReturnsEmpty() throws Exception {
		assertEquals("", YamlConfigFormat.INSTANCE.toInternal(""));
	}

	@Test void b04_toInternalDefaultSectionOnly() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("k1: v1\n");
		assertEquals("k1 = v1\n", ini);
	}

	@Test void b05_toInternalNestedSection() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("""
			foo:
			  bar:
			    k1: v1
			""");
		assertEquals("[foo/bar]\nk1 = v1\n", ini);
	}

	@Test void b06_toInternalLeavesBlanksAndComments() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("""
			# header

			foo:
			  k1: v1
			""");
		// Comments and blanks are preserved verbatim alongside the new INI section header.
		assertTrue(ini.contains("# header"));
		assertTrue(ini.contains("[foo]"));
		assertTrue(ini.contains("k1 = v1"));
	}

	@Test void b07_toInternalUnindentReducesStack() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("""
			a:
			  b:
			    k1: v1
			c:
			  k2: v2
			""");
		assertTrue(ini.contains("[a/b]"));
		assertTrue(ini.contains("[c]"));
		assertTrue(ini.contains("k1 = v1"));
		assertTrue(ini.contains("k2 = v2"));
	}

	@Test void b08_toInternalInlineComment() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("""
			foo:
			  k1: v1 # inline
			""");
		assertEquals("[foo]\nk1 = v1 # inline\n", ini);
	}

	@Test void b09_toInternalLineMissingColonThrows() {
		assertThrows(IOException.class, () -> YamlConfigFormat.INSTANCE.toInternal("not-a-yaml-line\n"));
	}

	@Test void b10_toInternalLineWithLeadingColonThrows() {
		assertThrows(IOException.class, () -> YamlConfigFormat.INSTANCE.toInternal(": value\n"));
	}

	@Test void b11_toInternalSingleQuotedValue() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("k: 'q'\n");
		assertEquals("k = q\n", ini);
	}

	@Test void b12_toInternalDoubleQuotedValue() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("k: \"q\"\n");
		assertEquals("k = q\n", ini);
	}

	@Test void b13_toInternalShortQuotedValuePassesThrough() throws Exception {
		var ini = YamlConfigFormat.INSTANCE.toInternal("k: \"\n");
		assertEquals("k = \"\n", ini);
	}

	@Test void b14_fromInternalEmptyMapReturnsEmpty() throws Exception {
		var s = MemoryStore.create().build();
		var map = s.getMap("Empty.yaml", YamlConfigFormat.INSTANCE);
		assertEquals("", YamlConfigFormat.INSTANCE.fromInternal(map));
	}

	@Test void b15_fromInternalEmptyValueGetsQuoted() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("H.yaml").yaml().build();
		c.set("foo/k1", "");
		c.commit();

		assertTrue(s.read("H.yaml").contains("k1: \"\""));
	}

	@Test void b16_fromInternalQuotesValueWithSpecialChars() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("I.yaml").yaml().build();
		c.set("foo/k1", "has: colon");
		c.commit();

		assertTrue(s.read("I.yaml").contains("k1: \"has: colon\""));
	}

	@Test void b17_fromInternalEscapesDoubleQuotes() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("J.yaml").yaml().build();
		c.set("foo/k1", "has # hash with \" quote");
		c.commit();

		assertTrue(s.read("J.yaml").contains("\\\""));
	}

	@Test void b18_fromInternalTrailingSpaceTriggersQuoting() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("K.yaml").yaml().build();
		c.set("foo/k1", "trailing ");
		c.commit();

		assertTrue(s.read("K.yaml").contains("\"trailing \""));
	}

	@Test void b19_fromInternalLeadingSpaceTriggersQuoting() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("L.yaml").yaml().build();
		c.set("foo/k1", " leading");
		c.commit();

		assertTrue(s.read("L.yaml").contains("\" leading\""));
	}

	@Test void b20_fromInternalSharedPrefixReusedAcrossSections() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("M.yaml").yaml().build();
		c.set("a/b/k1", "v1");
		c.set("a/c/k2", "v2");
		c.commit();

		var written = s.read("M.yaml");
		// 'a' should appear exactly once since the prefix is shared between 'a/b' and 'a/c'.
		var firstA = written.indexOf("a:");
		var lastA = written.lastIndexOf("a:");
		assertTrue(firstA != -1 && firstA == lastA);
	}

	@Test void b21_yamlRoundTripPreservesValues() throws Exception {
		var s = MemoryStore.create().build();
		var c = Config.create().store(s).name("N.yaml").yaml().build();
		c.set("alpha/beta/k1", "v1");
		c.set("alpha/k2", "v2");
		c.commit();

		var c2 = Config.create().store(s).name("N.yaml").yaml().build();
		assertEquals("v1", c2.getString("alpha/beta/k1"));
		assertEquals("v2", c2.getString("alpha/k2"));
	}

	@Test void b22_roundTripPreservesInlineComment() throws Exception {
		var s = MemoryStore.create().build();
		s.update("RT.yaml",
			"foo:",
			"  k1: v1 # original"
		);
		var c = Config.create().store(s).name("RT.yaml").build();
		// Force re-serialization by updating a different key and committing.
		c.set("foo/k2", "v2");
		c.commit();

		var written = s.read("RT.yaml");
		assertTrue(written.contains("# original"), () -> "Expected comment to survive round-trip, got:\n" + written);
	}

	@Test void b23_toInternalAllBlankLineHandled() throws Exception {
		// A line of only spaces exercises the leadingSpaces "i < length false" branch.
		var ini = YamlConfigFormat.INSTANCE.toInternal("""
			foo:
			\s\s\s\s
			  k1: v1
			""");
		assertTrue(ini.contains("k1 = v1"));
	}

	@Test void b24_toInternalUnclosedQuotePassesThrough() throws Exception {
		// Value starts with a quote but doesn't end with one — unquote should not strip it.
		var ini = YamlConfigFormat.INSTANCE.toInternal("k: \"unmatched\n");
		assertEquals("k = \"unmatched\n", ini);
	}

	@Test void b24a_toInternalUnclosedSingleQuotePassesThrough() throws Exception {
		// Single-quote variant of b24 to hit the second clause of the unquote check.
		var ini = YamlConfigFormat.INSTANCE.toInternal("k: 'unmatched\n");
		assertEquals("k = 'unmatched\n", ini);
	}

	@Test void b24b_toInternalEmptyLineAtEnd() throws Exception {
		// Trailing empty line exercises the leadingSpaces empty-string path.
		var ini = YamlConfigFormat.INSTANCE.toInternal("foo:\n  k1: v1\n\n");
		assertTrue(ini.contains("k1 = v1"));
	}

	@Test void b25_configToMapMatchesNestedYamlInput() throws Exception {
		var s = MemoryStore.create().build();
		s.update("O.yaml",
			"foo:",
			"  bar:",
			"    k1: v1",
			"  k2: v2"
		);
		var c = Config.create().store(s).name("O.yaml").build();

		assertJson("{'':{},'foo/bar':{k1:'v1'},foo:{k2:'v2'}}", c.toMap());
	}
}
