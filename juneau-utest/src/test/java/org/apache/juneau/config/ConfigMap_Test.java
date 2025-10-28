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
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

class ConfigMap_Test extends TestBase {

	static final String ENCODED = "*";
	static final String BASE64 = "^";

	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read non-existent files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a01_nonExistentConfig() throws Exception {
		var s = MemoryStore.create().build();
		var cm = s.getMap("A.cfg");
		assertEquals("", cm.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read blank files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a02_blankConfig() throws Exception {

		var s = initStore("A.cfg", "");
		var cm = s.getMap("A.cfg");
		assertEquals("", cm.toString());

		s.update("A.cfg", "   \n   \n   ");
		s.getMap("A.cfg");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a03_simpleOneLine() throws Exception {
		var s = initStore("A.cfg",
			"foo=bar"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("foo=bar|", pipedLines(cm));

		assertEquals("", StringUtils.join(cm.getPreLines(""), '|'));
		assertEquals("", StringUtils.join(cm.getEntry("", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("foo=bar|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file with leading comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a04_simpleOneLineWithComments() throws Exception {
		var s = initStore("A.cfg",
			"#comment",
			"foo=bar"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("#comment|foo=bar|", pipedLines(cm));

		assertEquals("", StringUtils.join(cm.getPreLines(""), '|'));
		assertEquals("#comment", StringUtils.join(cm.getEntry("", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("#comment|foo=bar|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple section.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a05_simpleSection() throws Exception {
		var s = initStore("A.cfg",
			"[MySection]",
			"foo=bar"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("[MySection]|foo=bar|", pipedLines(cm));

		assertEquals("", StringUtils.join(cm.getPreLines(""), '|'));
		assertEquals("", StringUtils.join(cm.getPreLines("MySection"), '|'));
		assertEquals("", StringUtils.join(cm.getEntry("MySection", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("MySection", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("[MySection]|foo=bar|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Non-existent values should not throw exceptions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a06_nonExistentValues() throws Exception {
		var s = initStore("A.cfg",
			"[MySection]",
			"foo=bar"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("[MySection]|foo=bar|", pipedLines(cm));

		assertEquals("", StringUtils.join(cm.getPreLines(""), '|'));

		assertNull(cm.getPreLines("XXX"));

		assertNull(cm.getEntry("XXX", "yyy"));
		assertNull(cm.getEntry("MySection", "yyy"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleSectionWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a07_simpleSectionWithComments() throws Exception {
		var s = initStore("A.cfg",
			"#S1",
			"[S1]",
			"#k1",
			"k1=v1",
			"#S2",
			"[S2]",
			"#k2",
			"k2=v2"
		);
		var cm = s.getMap("A.cfg");
		assertEquals("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|", pipedLines(cm));

		assertEquals("", StringUtils.join(cm.getPreLines(""), '|'));
		assertEquals("#S1", StringUtils.join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", StringUtils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("#S2", StringUtils.join(cm.getPreLines("S2"), '|'));
		assertEquals("#k2", StringUtils.join(cm.getEntry("S2", "k2").getPreLines(), '|'));

		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2", cm.getEntry("S2", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a08_simpleAndDefaultSectionsWithComments() throws Exception {
		var s = initStore("A.cfg",
			"#D",
			"",
			"#k",
			"k=v",
			"#S1",
			"[S1]",
			"#k1",
			"k1=v1"
		);
		var cm = s.getMap("A.cfg");
		assertEquals("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|", pipedLines(cm));

		assertEquals("#D", StringUtils.join(cm.getPreLines(""), '|'));
		assertEquals("#k", StringUtils.join(cm.getEntry("", "k").getPreLines(), '|'));
		assertEquals("#S1", StringUtils.join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", StringUtils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));

		assertEquals("v", cm.getEntry("", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithCommentsAndExtraSpaces
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a09_simpleAndDefaultSectionsWithCommentsAndExtraSpaces() throws Exception {
		var s = initStore("A.cfg",
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
		var cm = s.getMap("A.cfg");
		assertEquals("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|", pipedLines(cm));

		assertEquals("#Da|#Db", StringUtils.join(cm.getPreLines(""), '|'));
		assertEquals("#ka||#kb|", StringUtils.join(cm.getEntry("", "k").getPreLines(), '|'));
		assertEquals("|#S1a||#S1b|", StringUtils.join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1a||#k1b|", StringUtils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));

		assertEquals("v", cm.getEntry("", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Error conditions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a10_malformedSectionHeaders() {

		var test = a(
			"[]", "[  ]",
			"[/]", "[[]", "[]]", "[\\]",
			"[foo/bar]", "[foo[bar]", "[foo]bar]", "[foo\\bar]",
			"[]", "[ ]", "[\t]"
		);

		for (var t : test) {
			var s = initStore("A.cfg", t);
			assertThrowsWithMessage(Exception.class, "Invalid section name", ()->s.getMap("A.cfg"));
		}
	}

	@Test void a01_duplicateSectionNames() {
		var s = initStore("A.cfg", "[S1]", "[S1]");
		assertThrowsWithMessage(ConfigException.class, "Duplicate section found in configuration:  [S1]", ()->s.getMap("A.cfg"));
	}

	@Test void a02_duplicateEntryNames() {
		var s = initStore("A.cfg", "[S1]", "foo=v1", "foo=v2");
		assertThrowsWithMessage(ConfigException.class, "Duplicate entry found in section [S1] of configuration:  foo", ()->s.getMap("A.cfg"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lines can be split up.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a03_multipleLines() throws Exception {
		var s = initStore("A.cfg",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("", StringUtils.join(cm.getEntry("", "k1").getPreLines(), '|'));
		assertEquals("", StringUtils.join(cm.getEntry("", "k2").getPreLines(), '|'));

		assertEquals("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", pipedLines(cm));

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", pipedLines(cm));
	}

	@Test void a04_multipleLinesWithSpacesAndComments() throws Exception {
		var s = initStore("A.cfg",
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
		var cm = s.getMap("A.cfg");

		assertEquals("|#k1|", StringUtils.join(cm.getEntry("", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", StringUtils.join(cm.getEntry("", "k2").getPreLines(), '|'));

		assertEquals("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", pipedLines(cm));

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", pipedLines(cm));
	}

	@Test void a05_multipleLinesInSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("", StringUtils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("", StringUtils.join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertEquals("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", pipedLines(cm));

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", pipedLines(cm));
	}

	@Test void a06_multipleLinesInSectionWithSpacesAndPrelines() throws Exception {
		var s = initStore("A.cfg",
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
		var cm = s.getMap("A.cfg");

		assertEquals("|#S1|", StringUtils.join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1|", StringUtils.join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", StringUtils.join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertEquals("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", pipedLines(cm));

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertEquals("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Entry lines can have trailing comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a07_entriesWithComments() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = foo # comment"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("[S1]|k1 = foo # comment|", pipedLines(cm));
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("comment", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, "newcomment", null);
		assertEquals("[S1]|k1 = foo # newcomment|", pipedLines(cm));
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("newcomment", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, "", null);
		assertEquals("[S1]|k1 = foo|", pipedLines(cm));
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, null, null);
		assertEquals("[S1]|k1 = foo|", pipedLines(cm));
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("", cm.getEntry("S1", "k1").getComment());
	}

	@Test void a08_entriesWithOddComments() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = foo#",
			"k2 = foo # "
		);
		var cm = s.getMap("A.cfg");
		assertEquals("[S1]|k1 = foo#|k2 = foo # |", pipedLines(cm));
		assertEquals("", cm.getEntry("S1", "k1").getComment());
		assertEquals("", cm.getEntry("S1", "k2").getComment());
	}

	@Test void a09_entriesWithEscapedComments() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = foo\\#bar",
			"k2 = foo \\# bar",
			"k3 = foo \\# bar # real-comment"
		);
		var cm = s.getMap("A.cfg");
		assertEquals("[S1]|k1 = foo\\#bar|k2 = foo \\# bar|k3 = foo \\# bar # real-comment|", pipedLines(cm));

		assertEquals(null, cm.getEntry("S1", "k1").getComment());
		assertEquals(null, cm.getEntry("S1", "k2").getComment());
		assertEquals("real-comment", cm.getEntry("S1", "k3").getComment());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test setting entries.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a10_settingEntries() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1a",
			"k2 = v2a"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v1b", null, null, null);
		cm.setEntry("S1", "k2", null, null, null, null);
		cm.setEntry("S1", "k3", "v3b", null, null, null);

		assertEquals("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|", pipedLines(cm));

		cm.commit();
		assertEquals("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|", pipedLines(cm));

		// Round trip.
		cm = s.getMap("A.cfg");
		assertEquals("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|", pipedLines(cm));
	}

	@Test void a11_settingEntriesWithPreLines() throws Exception {
		var s = initStore("A.cfg",
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
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v1b", null, null, null);
		cm.setEntry("S1", "k2", null, null, null, null);
		cm.setEntry("S1", "k3", "v3b", null, null, null);
		cm.setEntry("S1", "k4", "v4b", null, null, Arrays.asList("","#k4",""));

		assertEquals("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|", pipedLines(cm));

		cm.commit();
		assertEquals("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|", pipedLines(s.read("A.cfg")));

		// Round trip.
		cm = s.getMap("A.cfg");
		assertEquals("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|", pipedLines(cm));
	}

	@Test void a12_settingEntriesWithNewlines() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		cm.setEntry("", "k", "v1\nv2\nv3", null, null, null);
		cm.setEntry("S1", "k1", "v1\nv2\nv3", null, null, null);

		assertEquals("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", pipedLines(cm));

		assertEquals("v1\nv2\nv3", cm.getEntry("", "k").getValue());
		assertEquals("v1\nv2\nv3", cm.getEntry("S1", "k1").getValue());
		cm.commit();
		assertEquals("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", pipedLines(cm));

		// Round trip.
		cm = s.getMap("A.cfg");
		assertEquals("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", pipedLines(cm));
	}

	@Test void a13_settingEntriesWithNewlinesAndSpaces() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		cm.setEntry("", "k", "v1 \n v2 \n v3", null, null, null);
		cm.setEntry("S1", "k1", "v1\t\n\tv2\t\n\tv3", null, null, null);

		assertEquals("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", pipedLines(cm));

		assertEquals("v1 \n v2 \n v3", cm.getEntry("", "k").getValue());
		assertEquals("v1\t\n\tv2\t\n\tv3", cm.getEntry("S1", "k1").getValue());
		cm.commit();
		assertEquals("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", pipedLines(cm));

		// Round trip.
		cm = s.getMap("A.cfg");
		assertEquals("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setSection()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a14_setSectionOnExistingSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setSection("S1", Arrays.asList("#S1"));
		assertEquals("#S1|[S1]|k1 = v1|", pipedLines(cm));
		cm.setSection("S1", Collections.<String>emptyList());
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
		cm.setSection("S1", null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
	}

	@Test void a15_setSectionOnDefaultSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setSection("", Arrays.asList("#D"));
		assertEquals("#D||[S1]|k1 = v1|", pipedLines(cm));
		cm.setSection("", Collections.<String>emptyList());
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
		cm.setSection("", null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
	}

	@Test void a16_setSectionOnNewSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setSection("S2", Arrays.asList("#S2"));
		assertEquals("[S1]|k1 = v1|#S2|[S2]|", pipedLines(cm));
		cm.setSection("S3", Collections.<String>emptyList());
		assertEquals("[S1]|k1 = v1|#S2|[S2]|[S3]|", pipedLines(cm));
		cm.setSection("S4", null);
		assertEquals("[S1]|k1 = v1|#S2|[S2]|[S3]|[S4]|", pipedLines(cm));
	}

	@Test void a17_setSectionBadNames() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		var test = a(
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		);

		for (var t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid section name", ()->cm.setSection(t, null));
		}
	}

	@Test void a18_setSectionOkNames() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		// These are all okay characters to use in section names.
		String validChars = "~`!@#$%^&*()_-+={}|:;\"\'<,>.?";

		for (var c : validChars.toCharArray()) {
			var test = ""+c;
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
	@Test void a19_removeSectionOnExistingSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		var cm = s.getMap("A.cfg");

		cm.removeSection("S1");
		assertEquals("[S2]|k2 = v2|", pipedLines(cm));
	}

	@Test void a20_removeSectionOnNonExistingSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		var cm = s.getMap("A.cfg");

		cm.removeSection("S3");
		assertEquals("[S1]|k1 = v1|[S2]|k2 = v2|", pipedLines(cm));

		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid section name: 'null'", ()->cm.removeSection(null));
	}

	@Test void a21_removeDefaultSection() throws Exception {
		var s = initStore("A.cfg",
			"k = v",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		var cm = s.getMap("A.cfg");

		cm.removeSection("");
		assertEquals("[S1]|k1 = v1|[S2]|k2 = v2|", pipedLines(cm));
	}

	@Test void a22_removeDefaultSectionWithComments() throws Exception {
		var s = initStore("A.cfg",
			"#D",
			"",
			"#k",
			"k = v",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		var cm = s.getMap("A.cfg");

		cm.removeSection("");
		assertEquals("[S1]|k1 = v1|[S2]|k2 = v2|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setPreLines()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a23_setPrelinesOnExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, Arrays.asList("#k1"));
		assertEquals("[S1]|#k1|k1 = v1|", pipedLines(cm));
		cm.setEntry("S1", "k1", null, null, null, Collections.<String>emptyList());
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
		cm.setEntry("S1", "k1", null, null, null, null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
	}

	@Test void a24_setPrelinesOnExistingEntryWithAtrributes() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"#k1a",
			"k1 = v1 # comment"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, Arrays.asList("#k1b"));
		assertEquals("[S1]|#k1b|k1 = v1 # comment|", pipedLines(cm));
	}

	@Test void a25_setPrelinesOnNonExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", null, null, null, Arrays.asList("#k2"));
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
		cm.setEntry("S1", "k2", null, null, null, Collections.<String>emptyList());
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
		cm.setEntry("S1", "k2", null, null, null, null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));

		cm.setEntry("S2", "k2", null, null, null, Arrays.asList("#k2"));
		assertEquals("[S1]|k1 = v1|[S2]|", pipedLines(cm));
		cm.setEntry("S2", "k2", null, null, null, Collections.<String>emptyList());
		assertEquals("[S1]|k1 = v1|[S2]|", pipedLines(cm));
		cm.setEntry("S2", "k2", null, null, null, null);
		assertEquals("[S1]|k1 = v1|[S2]|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a26_setValueOnExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertEquals("[S1]|k1 = v2|", pipedLines(cm));
	}

	@Test void a27_setValueOnExistingEntryWithAttributes() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertEquals("[S1]|#k1|k1 = v2 # comment|", pipedLines(cm));
	}

	@Test void a28_setValueToNullOnExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
	}

	@Test void a29_setValueOnNonExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", "v2", null, null, null);
		assertEquals("[S1]|k1 = v1|k2 = v2|", pipedLines(cm));
		cm.setEntry("S1", "k2", null, null, null, null);
		assertEquals("[S1]|k1 = v1|k2 = v2|", pipedLines(cm));
		cm.setEntry("S1", "k2", null, null, null, null);
		assertEquals("[S1]|k1 = v1|k2 = v2|", pipedLines(cm));
	}

	@Test void a30_setValueOnNonExistingEntryOnNonExistentSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S2", "k2", "v2", null, null, null);
		assertEquals("[S1]|k1 = v1|[S2]|k2 = v2|", pipedLines(cm));
	}

	@Test void a31_setValueInvalidSectionNames() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		var test = a(
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		);

		for (var t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid section name:", ()->cm.setEntry(t, "k1", "foo", null, null, null));
		}
	}

	@Test void a32_setValueInvalidKeyNames() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		var test = a(
			"", " ", "\t",
			"foo=bar", "=",
			"foo/bar", "/",
			"foo[bar", "]",
			"foo]bar", "]",
			"foo\\bar", "\\",
			"foo#bar", "#",
			null
		);

		for (var t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid key name", ()->cm.setEntry("S1", t, "foo", null, null, null));
		}
	}

	@Test void a33_setValueWithCommentChars() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		// If value has # in it, it should get escaped.
		cm.setEntry("S1", "k1", "v1 # foo", null, null, null);
		assertEquals("[S1]|k1 = v1 \\u0023 foo|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setComment()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a34_setCommentOnExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, "c1", null);
		assertEquals("[S1]|k1 = v1 # c1|", pipedLines(cm));

		cm.setEntry("S1", "k1", null, null, "", null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
		cm.commit();
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));

		cm.setEntry("S1", "k1", null, null, null, null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
	}

	@Test void a35_setCommentOnExistingEntryWithAttributes() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"#k1a",
			"k1 = v1 # c1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, "c2", null);
		assertEquals("[S1]|#k1a|k1 = v1 # c2|", pipedLines(cm));
	}

	@Test void a36_setCommentOnNonExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", null, null, "foo", null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
		cm.setEntry("S1", "k2", null, null, null, null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));

		cm.setEntry("S2", "k2", null, null, "foo", null);
		assertEquals("[S1]|k1 = v1|[S2]|", pipedLines(cm));
		cm.setEntry("S2", "k2", null, null, null, null);
		assertEquals("[S1]|k1 = v1|[S2]|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a37_setEntryOnExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertEquals("[S1]|k1 = v2|", pipedLines(cm));

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertEquals("[S1]|#k1a|k1<*> = v3 # c3|", pipedLines(cm));

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertEquals("[S1]|#k1b|k1<^> = v4 # c4|", pipedLines(cm));
	}

	@Test void a38_setEntryOnExistingEntryWithAttributes() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertEquals("[S1]|#k1|k1 = v2 # comment|", pipedLines(cm));

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertEquals("[S1]|#k1a|k1<*> = v3 # c3|", pipedLines(cm));

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertEquals("[S1]|#k1b|k1<^> = v4 # c4|", pipedLines(cm));
	}

	@Test void a39_setEntryToNullOnExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, null);
		assertEquals("[S1]|k1 = v1|", pipedLines(cm));
	}

	@Test void a40_setEntryOnNonExistingEntry() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", "v2", null, null, null);
		assertEquals("[S1]|k1 = v1|k2 = v2|", pipedLines(cm));
		cm.setEntry("S1", "k2", null, null, null, null);
		assertEquals("[S1]|k1 = v1|k2 = v2|", pipedLines(cm));
		cm.setEntry("S1", "k2", "", null, null, null);
		assertEquals("[S1]|k1 = v1|k2 = |", pipedLines(cm));
	}

	@Test void a41_setEntryOnNonExistingEntryOnNonExistentSection() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		cm.setEntry("S2", "k2", "v2", null, null, null);
		assertEquals("[S1]|k1 = v1|[S2]|k2 = v2|", pipedLines(cm));
	}

	@Test void a42_setEntryInvalidSectionNames() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		var test = a(
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		);

		for (var t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid section name", ()->cm.setEntry(t, "k1", "foo", null, null, null));
		}
	}

	@Test void a43_setEntryInvalidKeyNames() throws Exception {
		var s = initStore("A.cfg");
		var cm = s.getMap("A.cfg");

		var test = a(
			"", " ", "\t",
			"foo=bar", "=",
			"foo/bar", "/",
			"foo[bar", "]",
			"foo]bar", "]",
			"foo\\bar", "\\",
			"foo#bar", "#",
			null
		);

		for (var t : test) {
			assertThrowsWithMessage(Exception.class, "Invalid key name", ()->cm.setEntry("S1", t, "foo", null, null, null));
		}
	}

	@Test void a44_setEntryWithCommentChars() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		var cm = s.getMap("A.cfg");

		// If value has # in it, it should get escaped.
		cm.setEntry("S1", "k1", "v1 # foo", null, null, null);
		assertEquals("[S1]|k1 = v1 \\u0023 foo|", pipedLines(cm));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Modifiers
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a45_modifiers() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1<^> = v1",
			"k2<*> = v2",
			"k3<*^> = v3"
		);
		var cm = s.getMap("A.cfg");

		assertEquals("[S1]|k1<^> = v1|k2<*> = v2|k3<*^> = v3|", pipedLines(cm));
		assertEquals("^", cm.getEntry("S1", "k1").getModifiers());
		assertEquals("*", cm.getEntry("S1", "k2").getModifiers());
		assertEquals("*^", cm.getEntry("S1", "k3").getModifiers());

		cm.setEntry("S1", "k1", "v1", "#$%&*+^@~", null, null);
		assertEquals("[S1]|k1<#$%&*+^@~> = v1|k2<*> = v2|k3<*^> = v3|", pipedLines(cm));
	}

	@Test void a46_invalidModifier() throws Exception {
		var s = initStore("A.cfg",
			"[S1]",
			"k1^ = v1",
			"k2* = v2",
			"k3*^ = v3"
		);
		var cm = s.getMap("A.cfg");

		// This is okay.
		assertDoesNotThrow(()->cm.setEntry("S1", "k1", "v1", "", null, null));
	}

	private static ConfigStore initStore(String name, String...contents) {
		return MemoryStore.create().build().update(name, contents);
	}
}