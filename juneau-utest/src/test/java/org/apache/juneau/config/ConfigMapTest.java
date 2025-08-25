// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.config;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

class ConfigMapTest extends SimpleTestBase {

	static final String ENCODED = "*";
	static final String BASE64 = "^";

	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read non-existent files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testNonExistentConfig() throws Exception {
		ConfigStore s = MemoryStore.create().build();
		ConfigMap cm = s.getMap("A.cfg");
		assertEquals("", cm.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read blank files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testBlankConfig() throws Exception {

		ConfigStore s = initStore("A.cfg", "");
		ConfigMap cm = s.getMap("A.cfg");
		assertEquals("", cm.toString());

		s.update("A.cfg", "   \n   \n   ");
		s.getMap("A.cfg");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSimpleOneLine() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertLines("foo=bar|", cm);

		assertEquals("", Utils.join(cm.getPreLines(""), '|'));
		assertEquals("", Utils.join(cm.getEntry("", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("foo=bar|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file with leading comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSimpleOneLineWithComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#comment",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertLines("#comment|foo=bar|", cm);

		assertEquals("", Utils.join(cm.getPreLines(""), '|'));
		assertEquals("#comment", Utils.join(cm.getEntry("", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("#comment|foo=bar|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple section.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSimpleSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[MySection]",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertLines("[MySection]|foo=bar|", cm);

		assertEquals("", Utils.join(cm.getPreLines(""), '|'));
		assertEquals("", Utils.join(cm.getPreLines("MySection"), '|'));
		assertEquals("", Utils.join(cm.getEntry("MySection", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("MySection", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("[MySection]|foo=bar|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Non-existent values should not throw exceptions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testNonExistentValues() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[MySection]",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertLines("[MySection]|foo=bar|", cm);

		assertEquals("", Utils.join(cm.getPreLines(""), '|'));

		assertNull(cm.getPreLines("XXX"));

		assertNull(cm.getEntry("XXX", "yyy"));
		assertNull(cm.getEntry("MySection", "yyy"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleSectionWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSimpleSectionWithComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#S1",
			"[S1]",
			"#k1",
			"k1=v1",
			"#S2",
			"[S2]",
			"#k2",
			"k2=v2"
		);
		ConfigMap cm = s.getMap("A.cfg");
		assertLines("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|", cm);

		assertEquals("", Utils.join(cm.getPreLines(""), '|'));
		assertEquals("#S1", Utils.join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", Utils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("#S2", Utils.join(cm.getPreLines("S2"), '|'));
		assertEquals("#k2", Utils.join(cm.getEntry("S2", "k2").getPreLines(), '|'));

		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2", cm.getEntry("S2", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSimpleAndDefaultSectionsWithComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#D",
			"",
			"#k",
			"k=v",
			"#S1",
			"[S1]",
			"#k1",
			"k1=v1"
		);
		ConfigMap cm = s.getMap("A.cfg");
		assertLines("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|", cm);

		assertEquals("#D", Utils.join(cm.getPreLines(""), '|'));
		assertEquals("#k", Utils.join(cm.getEntry("", "k").getPreLines(), '|'));
		assertEquals("#S1", Utils.join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", Utils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));

		assertEquals("v", cm.getEntry("", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithCommentsAndExtraSpaces
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSimpleAndDefaultSectionsWithCommentsAndExtraSpaces() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#Da",
			"#Db",
			"",
			"#ka",
			"",
			"#kb",
			"",
			"k=v",
			"",
			"#S1a",
			"",
			"#S1b",
			"",
			"[S1]",
			"",
			"#k1a",
			"",
			"#k1b",
			"",
			"k1=v1"
		);
		ConfigMap cm = s.getMap("A.cfg");
		assertLines("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|", cm);

		assertEquals("#Da|#Db", Utils.join(cm.getPreLines(""), '|'));
		assertEquals("#ka||#kb|", Utils.join(cm.getEntry("", "k").getPreLines(), '|'));
		assertEquals("|#S1a||#S1b|", Utils.join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1a||#k1b|", Utils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));

		assertEquals("v", cm.getEntry("", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Error conditions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testMalformedSectionHeaders() {

		String[] test = {
			"[]", "[  ]",
			"[/]", "[[]", "[]]", "[\\]",
			"[foo/bar]", "[foo[bar]", "[foo]bar]", "[foo\\bar]",
			"[]", "[ ]", "[\t]",
		};

		for (String t : test) {
			ConfigStore s = initStore("A.cfg", t);
			assertThrowsWithMessage(Exception.class, "Invalid section name", ()->s.getMap("A.cfg"));
		}
	}

	@Test void testDuplicateSectionNames() {
		ConfigStore s = initStore("A.cfg", "[S1]", "[S1]");
		assertThrowsWithMessage(ConfigException.class, "Duplicate section found in configuration:  [S1]", ()->s.getMap("A.cfg"));
	}

	@Test void testDuplicateEntryNames() {
		ConfigStore s = initStore("A.cfg", "[S1]", "foo=v1", "foo=v2");
		assertThrowsWithMessage(ConfigException.class, "Duplicate entry found in section [S1] of configuration:  foo", ()->s.getMap("A.cfg"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lines can be split up.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testMultipleLines() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertEquals("", Utils.join(cm.getEntry("", "k1").getPreLines(), '|'));
		assertEquals("", Utils.join(cm.getEntry("", "k2").getPreLines(), '|'));

		assertLines("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);
	}

	@Test void testMultipleLinesWithSpacesAndComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"",
			"#k1",
			"",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"",
			"#k2",
			"",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertEquals("|#k1|", Utils.join(cm.getEntry("", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", Utils.join(cm.getEntry("", "k2").getPreLines(), '|'));

		assertLines("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);
	}

	@Test void testMultipleLinesInSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertEquals("", Utils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("", Utils.join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertLines("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);
	}

	@Test void testMultipleLinesInSectionWithSpacesAndPrelines() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"",
			"#S1",
			"",
			"[S1]",
			"",
			"#k1",
			"",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"",
			"#k2",
			"",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertEquals("|#S1|", Utils.join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1|", Utils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", Utils.join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertLines("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertLines("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Entry lines can have trailing comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testEntriesWithComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = foo # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertLines("[S1]|k1 = foo # comment|", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("comment", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, "newcomment", null);
		assertLines("[S1]|k1 = foo # newcomment|", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("newcomment", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, "", null);
		assertLines("[S1]|k1 = foo|", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, null, null);
		assertLines("[S1]|k1 = foo|", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("", cm.getEntry("S1", "k1").getComment());
	}

	@Test void testEntriesWithOddComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = foo#",
			"k2 = foo # "
		);
		ConfigMap cm = s.getMap("A.cfg");
		assertLines("[S1]|k1 = foo#|k2 = foo # |", cm);
		assertEquals("", cm.getEntry("S1", "k1").getComment());
		assertEquals("", cm.getEntry("S1", "k2").getComment());
	}

	@Test void testEntriesWithEscapedComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = foo\\#bar",
			"k2 = foo \\# bar",
			"k3 = foo \\# bar # real-comment"
		);
		ConfigMap cm = s.getMap("A.cfg");
		assertLines("[S1]|k1 = foo\\#bar|k2 = foo \\# bar|k3 = foo \\# bar # real-comment|", cm);

		assertEquals(null, cm.getEntry("S1", "k1").getComment());
		assertEquals(null, cm.getEntry("S1", "k2").getComment());
		assertEquals("real-comment", cm.getEntry("S1", "k3").getComment());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test setting entries.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSettingEntries() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1a",
			"k2 = v2a"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v1b", null, null, null);
		cm.setEntry("S1", "k2", null, null, null, null);
		cm.setEntry("S1", "k3", "v3b", null, null, null);

		assertLines("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|", cm);

		cm.commit();
		assertLines("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|", cm);

		// Round trip.
		cm = s.getMap("A.cfg");
		assertLines("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|", cm);
	}

	@Test void testSettingEntriesWithPreLines() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"",
			"#S1",
			"",
			"[S1]",
			"",
			"#k1",
			"",
			"k1 = v1a",
			"",
			"#k2",
			"",
			"k2 = v2a"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v1b", null, null, null);
		cm.setEntry("S1", "k2", null, null, null, null);
		cm.setEntry("S1", "k3", "v3b", null, null, null);
		cm.setEntry("S1", "k4", "v4b", null, null, Arrays.asList("","#k4",""));

		assertLines("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|", cm);

		cm.commit();
		assertLines("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|", s.read("A.cfg"));

		// Round trip.
		cm = s.getMap("A.cfg");
		assertLines("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|", cm);
	}

	@Test void testSettingEntriesWithNewlines() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("", "k", "v1\nv2\nv3", null, null, null);
		cm.setEntry("S1", "k1", "v1\nv2\nv3", null, null, null);

		assertLines("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", cm);

		assertEquals("v1\nv2\nv3", cm.getEntry("", "k").getValue());
		assertEquals("v1\nv2\nv3", cm.getEntry("S1", "k1").getValue());
		cm.commit();
		assertLines("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", cm);

		// Round trip.
		cm = s.getMap("A.cfg");
		assertLines("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", cm);
	}

	@Test void testSettingEntriesWithNewlinesAndSpaces() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("", "k", "v1 \n v2 \n v3", null, null, null);
		cm.setEntry("S1", "k1", "v1\t\n\tv2\t\n\tv3", null, null, null);

		assertLines("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", cm);

		assertEquals("v1 \n v2 \n v3", cm.getEntry("", "k").getValue());
		assertEquals("v1\t\n\tv2\t\n\tv3", cm.getEntry("S1", "k1").getValue());
		cm.commit();
		assertLines("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", cm);

		// Round trip.
		cm = s.getMap("A.cfg");
		assertLines("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setSection()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSetSectionOnExistingSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setSection("S1", Arrays.asList("#S1"));
		assertLines("#S1|[S1]|k1 = v1|", cm);
		cm.setSection("S1", Collections.<String>emptyList());
		assertLines("[S1]|k1 = v1|", cm);
		cm.setSection("S1", null);
		assertLines("[S1]|k1 = v1|", cm);
	}

	@Test void testSetSectionOnDefaultSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setSection("", Arrays.asList("#D"));
		assertLines("#D||[S1]|k1 = v1|", cm);
		cm.setSection("", Collections.<String>emptyList());
		assertLines("[S1]|k1 = v1|", cm);
		cm.setSection("", null);
		assertLines("[S1]|k1 = v1|", cm);
	}

	@Test void testSetSectionOnNewSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setSection("S2", Arrays.asList("#S2"));
		assertLines("[S1]|k1 = v1|#S2|[S2]|", cm);
		cm.setSection("S3", Collections.<String>emptyList());
		assertLines("[S1]|k1 = v1|#S2|[S2]|[S3]|", cm);
		cm.setSection("S4", null);
		assertLines("[S1]|k1 = v1|#S2|[S2]|[S3]|[S4]|", cm);
	}

	@Test void testSetSectionBadNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		};

		for (String t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid section name", ()->cm.setSection(t, null));
		}
	}

	@Test void testSetSectionOkNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		// These are all okay characters to use in section names.
		String validChars = "~`!@#$%^&*()_-+={}|:;\"\'<,>.?";

		for (char c : validChars.toCharArray()) {
			String test = ""+c;
			cm.setSection(test, Arrays.asList("test"));
			cm.commit();
			assertEquals("test", cm.getPreLines(test).get(0));

			test = "foo"+c+"bar";
			cm.setSection(test, Arrays.asList("test"));
			cm.commit();
			assertEquals("test", cm.getPreLines(test).get(0));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// removeSection()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testRemoveSectionOnExistingSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.removeSection("S1");
		assertLines("[S2]|k2 = v2|", cm);
	}

	@Test void testRemoveSectionOnNonExistingSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.removeSection("S3");
		assertLines("[S1]|k1 = v1|[S2]|k2 = v2|", cm);

		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid section name: 'null'", ()->cm.removeSection(null));
	}

	@Test void testRemoveDefaultSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"k = v",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.removeSection("");
		assertLines("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}

	@Test void testRemoveDefaultSectionWithComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#D",
			"",
			"#k",
			"k = v",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.removeSection("");
		assertLines("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setPreLines()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSetPrelinesOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, Arrays.asList("#k1"));
		assertLines("[S1]|#k1|k1 = v1|", cm);
		cm.setEntry("S1", "k1", null, null, null, Collections.<String>emptyList());
		assertLines("[S1]|k1 = v1|", cm);
		cm.setEntry("S1", "k1", null, null, null, null);
		assertLines("[S1]|k1 = v1|", cm);
	}

	@Test void testSetPrelinesOnExistingEntryWithAtrributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1a",
			"k1 = v1 # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, Arrays.asList("#k1b"));
		assertLines("[S1]|#k1b|k1 = v1 # comment|", cm);
	}

	@Test void testSetPrelinesOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", null, null, null, Arrays.asList("#k2"));
		assertLines("[S1]|k1 = v1|", cm);
		cm.setEntry("S1", "k2", null, null, null, Collections.<String>emptyList());
		assertLines("[S1]|k1 = v1|", cm);
		cm.setEntry("S1", "k2", null, null, null, null);
		assertLines("[S1]|k1 = v1|", cm);

		cm.setEntry("S2", "k2", null, null, null, Arrays.asList("#k2"));
		assertLines("[S1]|k1 = v1|[S2]|", cm);
		cm.setEntry("S2", "k2", null, null, null, Collections.<String>emptyList());
		assertLines("[S1]|k1 = v1|[S2]|", cm);
		cm.setEntry("S2", "k2", null, null, null, null);
		assertLines("[S1]|k1 = v1|[S2]|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSetValueOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertLines("[S1]|k1 = v2|", cm);
	}

	@Test void testSetValueOnExistingEntryWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertLines("[S1]|#k1|k1 = v2 # comment|", cm);
	}

	@Test void testSetValueToNullOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, null);
		assertLines("[S1]|k1 = v1|", cm);
	}

	@Test void testSetValueOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", "v2", null, null, null);
		assertLines("[S1]|k1 = v1|k2 = v2|", cm);
		cm.setEntry("S1", "k2", null, null, null, null);
		assertLines("[S1]|k1 = v1|k2 = v2|", cm);
		cm.setEntry("S1", "k2", null, null, null, null);
		assertLines("[S1]|k1 = v1|k2 = v2|", cm);
	}

	@Test void testSetValueOnNonExistingEntryOnNonExistentSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S2", "k2", "v2", null, null, null);
		assertLines("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}

	@Test void testSetValueInvalidSectionNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		};

		for (String t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid section name:", ()->cm.setEntry(t, "k1", "foo", null, null, null));
		}
	}

	@Test void testSetValueInvalidKeyNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"", " ", "\t",
			"foo=bar", "=",
			"foo/bar", "/",
			"foo[bar", "]",
			"foo]bar", "]",
			"foo\\bar", "\\",
			"foo#bar", "#",
			null
		};

		for (String t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid key name", ()->cm.setEntry("S1", t, "foo", null, null, null));
		}
	}

	@Test void testSetValueWithCommentChars() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		// If value has # in it, it should get escaped.
		cm.setEntry("S1", "k1", "v1 # foo", null, null, null);
		assertLines("[S1]|k1 = v1 \\u0023 foo|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setComment()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSetCommentOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, "c1", null);
		assertLines("[S1]|k1 = v1 # c1|", cm);

		cm.setEntry("S1", "k1", null, null, "", null);
		assertLines("[S1]|k1 = v1|", cm);
		cm.commit();
		assertLines("[S1]|k1 = v1|", cm);

		cm.setEntry("S1", "k1", null, null, null, null);
		assertLines("[S1]|k1 = v1|", cm);
	}

	@Test void testSetCommentOnExistingEntryWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1a",
			"k1 = v1 # c1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, "c2", null);
		assertLines("[S1]|#k1a|k1 = v1 # c2|", cm);
	}

	@Test void testSetCommentOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", null, null, "foo", null);
		assertLines("[S1]|k1 = v1|", cm);
		cm.setEntry("S1", "k2", null, null, null, null);
		assertLines("[S1]|k1 = v1|", cm);

		cm.setEntry("S2", "k2", null, null, "foo", null);
		assertLines("[S1]|k1 = v1|[S2]|", cm);
		cm.setEntry("S2", "k2", null, null, null, null);
		assertLines("[S1]|k1 = v1|[S2]|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testSetEntryOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertLines("[S1]|k1 = v2|", cm);

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertLines("[S1]|#k1a|k1<*> = v3 # c3|", cm);

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertLines("[S1]|#k1b|k1<^> = v4 # c4|", cm);
	}

	@Test void testSetEntryOnExistingEntryWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertLines("[S1]|#k1|k1 = v2 # comment|", cm);

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertLines("[S1]|#k1a|k1<*> = v3 # c3|", cm);

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertLines("[S1]|#k1b|k1<^> = v4 # c4|", cm);
	}

	@Test void testSetEntryToNullOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, null);
		assertLines("[S1]|k1 = v1|", cm);
	}

	@Test void testSetEntryOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", "v2", null, null, null);
		assertLines("[S1]|k1 = v1|k2 = v2|", cm);
		cm.setEntry("S1", "k2", null, null, null, null);
		assertLines("[S1]|k1 = v1|k2 = v2|", cm);
		cm.setEntry("S1", "k2", "", null, null, null);
		assertLines("[S1]|k1 = v1|k2 = |", cm);
	}

	@Test void testSetEntryOnNonExistingEntryOnNonExistentSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S2", "k2", "v2", null, null, null);
		assertLines("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}

	@Test void testSetEntryInvalidSectionNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		};

		for (String t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid section name", ()->cm.setEntry(t, "k1", "foo", null, null, null));
		}
	}

	@Test void testSetEntryInvalidKeyNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"", " ", "\t",
			"foo=bar", "=",
			"foo/bar", "/",
			"foo[bar", "]",
			"foo]bar", "]",
			"foo\\bar", "\\",
			"foo#bar", "#",
			null
		};

		for (String t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid key name", ()->cm.setEntry("S1", t, "foo", null, null, null));
		}
	}

	@Test void testSetEntryWithCommentChars() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		// If value has # in it, it should get escaped.
		cm.setEntry("S1", "k1", "v1 # foo", null, null, null);
		assertLines("[S1]|k1 = v1 \\u0023 foo|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Modifiers
	//-----------------------------------------------------------------------------------------------------------------
	@Test void testModifiers() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1<^> = v1",
			"k2<*> = v2",
			"k3<*^> = v3"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertLines("[S1]|k1<^> = v1|k2<*> = v2|k3<*^> = v3|", cm);
		assertEquals("^", cm.getEntry("S1", "k1").getModifiers());
		assertEquals("*", cm.getEntry("S1", "k2").getModifiers());
		assertEquals("*^", cm.getEntry("S1", "k3").getModifiers());

		cm.setEntry("S1", "k1", "v1", "#$%&*+^@~", null, null);
		assertLines("[S1]|k1<#$%&*+^@~> = v1|k2<*> = v2|k3<*^> = v3|", cm);
	}

	@Test void testInvalidModifier() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1^ = v1",
			"k2* = v2",
			"k3*^ = v3"
		);
		ConfigMap cm = s.getMap("A.cfg");

		// This is okay.
		TestUtils.assertNotThrown(()->cm.setEntry("S1", "k1", "v1", "", null, null));
	}

	private static ConfigStore initStore(String name, String...contents) {
		return MemoryStore.create().build().update(name, contents);
	}
}