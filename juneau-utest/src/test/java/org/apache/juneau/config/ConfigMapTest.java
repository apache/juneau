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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.config.internal.*;
import org.apache.juneau.config.store.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ConfigMapTest {

	final static String ENCODED = "*";
	final static String BASE64 = "^";

	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read non-existent files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testNonExistentConfig() throws Exception {
		ConfigStore s = MemoryStore.create().build();
		ConfigMap cm = s.getMap("A.cfg");
		assertEquals("", cm.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read blank files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testBlankConfig() throws Exception {

		ConfigStore s = initStore("A.cfg", "");
		ConfigMap cm = s.getMap("A.cfg");
		assertEquals("", cm.toString());

		s.update("A.cfg", "   \n   \n   ");
		cm = s.getMap("A.cfg");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleOneLine() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("foo=bar|");

		assertEquals("", join(cm.getPreLines(""), '|'));
		assertEquals("", join(cm.getEntry("", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("foo=bar|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file with leading comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleOneLineWithComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"#comment",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#comment|foo=bar|");

		assertEquals("", join(cm.getPreLines(""), '|'));
		assertEquals("#comment", join(cm.getEntry("", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#comment|foo=bar|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple section.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[MySection]",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[MySection]|foo=bar|");

		assertEquals("", join(cm.getPreLines(""), '|'));
		assertEquals("", join(cm.getPreLines("MySection"), '|'));
		assertEquals("", join(cm.getEntry("MySection", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("MySection", "foo").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[MySection]|foo=bar|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Non-existent values should not throw exceptions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testNonExistentValues() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[MySection]",
			"foo=bar"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[MySection]|foo=bar|");

		assertEquals("", join(cm.getPreLines(""), '|'));

		assertNull(cm.getPreLines("XXX"));

		assertNull(cm.getEntry("XXX", "yyy"));
		assertNull(cm.getEntry("MySection", "yyy"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleSectionWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleSectionWithComments() throws Exception {
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
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|");

		assertEquals("", join(cm.getPreLines(""), '|'));
		assertEquals("#S1", join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("#S2", join(cm.getPreLines("S2"), '|'));
		assertEquals("#k2", join(cm.getEntry("S2", "k2").getPreLines(), '|'));

		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2", cm.getEntry("S2", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleAndDefaultSectionsWithComments() throws Exception {
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
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|");

		assertEquals("#D", join(cm.getPreLines(""), '|'));
		assertEquals("#k", join(cm.getEntry("", "k").getPreLines(), '|'));
		assertEquals("#S1", join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", join(cm.getEntry("S1", "k1").getPreLines(), '|'));

		assertEquals("v", cm.getEntry("", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithCommentsAndExtraSpaces
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleAndDefaultSectionsWithCommentsAndExtraSpaces() throws Exception {
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
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|");

		assertEquals("#Da|#Db", join(cm.getPreLines(""), '|'));
		assertEquals("#ka||#kb|", join(cm.getEntry("", "k").getPreLines(), '|'));
		assertEquals("|#S1a||#S1b|", join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1a||#k1b|", join(cm.getEntry("S1", "k1").getPreLines(), '|'));

		assertEquals("v", cm.getEntry("", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Error conditions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testMalformedSectionHeaders() throws Exception {

		String[] test = {
			"[]", "[  ]",
			"[/]", "[[]", "[]]", "[\\]",
			"[foo/bar]", "[foo[bar]", "[foo]bar]", "[foo\\bar]",
			"[]", "[ ]", "[\t]",
		};

		for (String t : test) {
			ConfigStore s = initStore("A.cfg", t);
			assertThrown(()->s.getMap("A.cfg")).asMessage().isContains("Invalid section name");
		}
	}

	@Test
	public void testDuplicateSectionNames() throws Exception {
		ConfigStore s = initStore("A.cfg", "[S1]", "[S1]");
		assertThrown(()->s.getMap("A.cfg")).asMessage().is("Duplicate section found in configuration:  [S1]");
	}

	@Test
	public void testDuplicateEntryNames() throws Exception {
		ConfigStore s = initStore("A.cfg", "[S1]", "foo=v1", "foo=v2");
		assertThrown(()->s.getMap("A.cfg")).asMessage().is("Duplicate entry found in section [S1] of configuration:  foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lines can be split up.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testMultipleLines() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertEquals("", join(cm.getEntry("", "k1").getPreLines(), '|'));
		assertEquals("", join(cm.getEntry("", "k2").getPreLines(), '|'));

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|");

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|");
	}

	@Test
	public void testMultipleLinesWithSpacesAndComments() throws Exception {
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

		assertEquals("|#k1|", join(cm.getEntry("", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", join(cm.getEntry("", "k2").getPreLines(), '|'));

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|");

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|");
	}

	@Test
	public void testMultipleLinesInSection() throws Exception {
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

		assertEquals("", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("", join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|");

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|");
	}

	@Test
	public void testMultipleLinesInSectionWithSpacesAndPrelines() throws Exception {
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

		assertEquals("|#S1|", join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1|", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|");

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());

		// Round trip.
		s.update("A.cfg", cm.toString());
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Entry lines can have trailing comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testEntriesWithComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = foo # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = foo # comment|");
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("comment", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, "newcomment", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = foo # newcomment|");
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("newcomment", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, "", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = foo|");
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("", cm.getEntry("S1", "k1").getComment());

		cm.setEntry("S1", "k1", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = foo|");
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("", cm.getEntry("S1", "k1").getComment());
	}

	@Test
	public void testEntriesWithOddComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = foo#",
			"k2 = foo # "
		);
		ConfigMap cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = foo#|k2 = foo # |");
		assertEquals("", cm.getEntry("S1", "k1").getComment());
		assertEquals("", cm.getEntry("S1", "k2").getComment());
	}

	@Test
	public void testEntriesWithEscapedComments() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = foo\\#bar",
			"k2 = foo \\# bar",
			"k3 = foo \\# bar # real-comment"
		);
		ConfigMap cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = foo\\#bar|k2 = foo \\# bar|k3 = foo \\# bar # real-comment|");

		assertEquals(null, cm.getEntry("S1", "k1").getComment());
		assertEquals(null, cm.getEntry("S1", "k2").getComment());
		assertEquals("real-comment", cm.getEntry("S1", "k3").getComment());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test setting entries.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSettingEntries() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1a",
			"k2 = v2a"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v1b", null, null, null);
		cm.setEntry("S1", "k2", null, null, null, null);
		cm.setEntry("S1", "k3", "v3b", null, null, null);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|");

		cm.commit();
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|");

		// Round trip.
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1b|k2 = v2a|k3 = v3b|");
	}

	@Test
	public void testSettingEntriesWithPreLines() throws Exception {
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

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|");

		cm.commit();
		assertString(s.read("A.cfg")).asReplaceAll("\\r?\\n", "|").is("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|");

		// Round trip.
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("|#S1||[S1]||#k1||k1 = v1b||#k2||k2 = v2a|k3 = v3b||#k4||k4 = v4b|");
	}

	@Test
	public void testSettingEntriesWithNewlines() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("", "k", "v1\nv2\nv3", null, null, null);
		cm.setEntry("S1", "k1", "v1\nv2\nv3", null, null, null);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|");

		assertEquals("v1\nv2\nv3", cm.getEntry("", "k").getValue());
		assertEquals("v1\nv2\nv3", cm.getEntry("S1", "k1").getValue());
		cm.commit();
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|");

		// Round trip.
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|");
	}

	@Test
	public void testSettingEntriesWithNewlinesAndSpaces() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("", "k", "v1 \n v2 \n v3", null, null, null);
		cm.setEntry("S1", "k1", "v1\t\n\tv2\t\n\tv3", null, null, null);

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|");

		assertEquals("v1 \n v2 \n v3", cm.getEntry("", "k").getValue());
		assertEquals("v1\t\n\tv2\t\n\tv3", cm.getEntry("S1", "k1").getValue());
		cm.commit();
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|");

		// Round trip.
		cm = s.getMap("A.cfg");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setSection()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetSectionOnExistingSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setSection("S1", Arrays.asList("#S1"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#S1|[S1]|k1 = v1|");
		cm.setSection("S1", Collections.<String>emptyList());
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
		cm.setSection("S1", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
	}

	@Test
	public void testSetSectionOnDefaultSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setSection("", Arrays.asList("#D"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("#D||[S1]|k1 = v1|");
		cm.setSection("", Collections.<String>emptyList());
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
		cm.setSection("", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
	}

	@Test
	public void testSetSectionOnNewSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setSection("S2", Arrays.asList("#S2"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|#S2|[S2]|");
		cm.setSection("S3", Collections.<String>emptyList());
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|#S2|[S2]|[S3]|");
		cm.setSection("S4", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|#S2|[S2]|[S3]|[S4]|");
	}

	@Test
	public void testSetSectionBadNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		};

		for (String t : test) {
			assertThrown(()->cm.setSection(t, null)).asMessage().isContains("Invalid section name");
		}
	}

	@Test
	public void testSetSectionOkNames() throws Exception {
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
	@Test
	public void testRemoveSectionOnExistingSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.removeSection("S1");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S2]|k2 = v2|");
	}

	@Test
	public void testRemoveSectionOnNonExistingSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.removeSection("S3");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|k2 = v2|");

		assertThrown(()->cm.removeSection(null)).asMessage().is("Invalid section name: 'null'");
	}

	@Test
	public void testRemoveDefaultSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"k = v",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"

		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.removeSection("");
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|k2 = v2|");
	}

	@Test
	public void testRemoveDefaultSectionWithComments() throws Exception {
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
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|k2 = v2|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setPreLines()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetPrelinesOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, Arrays.asList("#k1"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1|k1 = v1|");
		cm.setEntry("S1", "k1", null, null, null, Collections.<String>emptyList());
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
		cm.setEntry("S1", "k1", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
	}

	@Test
	public void testSetPrelinesOnExistingEntryWithAtrributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1a",
			"k1 = v1 # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, Arrays.asList("#k1b"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1b|k1 = v1 # comment|");
	}

	@Test
	public void testSetPrelinesOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", null, null, null, Arrays.asList("#k2"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
		cm.setEntry("S1", "k2", null, null, null, Collections.<String>emptyList());
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
		cm.setEntry("S1", "k2", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");

		cm.setEntry("S2", "k2", null, null, null, Arrays.asList("#k2"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|");
		cm.setEntry("S2", "k2", null, null, null, Collections.<String>emptyList());
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|");
		cm.setEntry("S2", "k2", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetValueOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v2|");
	}

	@Test
	public void testSetValueOnExistingEntryWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1|k1 = v2 # comment|");
	}

	@Test
	public void testSetValueToNullOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
	}

	@Test
	public void testSetValueOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|k2 = v2|");
		cm.setEntry("S1", "k2", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|k2 = v2|");
		cm.setEntry("S1", "k2", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|k2 = v2|");
	}

	@Test
	public void testSetValueOnNonExistingEntryOnNonExistentSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S2", "k2", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|k2 = v2|");
	}

	@Test
	public void testSetValueInvalidSectionNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		};

		for (String t : test) {
			assertThrown(()->cm.setEntry(t, "k1", "foo", null, null, null)).asMessage().isContains("Invalid section name:");
		}
	}

	@Test
	public void testSetValueInvalidKeyNames() throws Exception {
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
			assertThrown(()->cm.setEntry("S1", t, "foo", null, null, null)).asMessage().isContains("Invalid key name");
		}
	}

	@Test
	public void testSetValueWithCommentChars() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		// If value has # in it, it should get escaped.
		cm.setEntry("S1", "k1", "v1 # foo", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1 \\u0023 foo|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setComment()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetCommentOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, "c1", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1 # c1|");

		cm.setEntry("S1", "k1", null, null, "", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
		cm.commit();
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");

		cm.setEntry("S1", "k1", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
	}

	@Test
	public void testSetCommentOnExistingEntryWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1a",
			"k1 = v1 # c1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, "c2", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1a|k1 = v1 # c2|");
	}

	@Test
	public void testSetCommentOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", null, null, "foo", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
		cm.setEntry("S1", "k2", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");

		cm.setEntry("S2", "k2", null, null, "foo", null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|");
		cm.setEntry("S2", "k2", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetEntryOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v2|");

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1a|k1<*> = v3 # c3|");

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1b|k1<^> = v4 # c4|");
	}

	@Test
	public void testSetEntryOnExistingEntryWithAttributes() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1|k1 = v2 # comment|");

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1a|k1<*> = v3 # c3|");

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|#k1b|k1<^> = v4 # c4|");
	}

	@Test
	public void testSetEntryToNullOnExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k1", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|");
	}

	@Test
	public void testSetEntryOnNonExistingEntry() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S1", "k2", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|k2 = v2|");
		cm.setEntry("S1", "k2", null, null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|k2 = v2|");
		cm.setEntry("S1", "k2", "", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|k2 = |");
	}

	@Test
	public void testSetEntryOnNonExistingEntryOnNonExistentSection() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		cm.setEntry("S2", "k2", "v2", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1|[S2]|k2 = v2|");
	}

	@Test
	public void testSetEntryInvalidSectionNames() throws Exception {
		ConfigStore s = initStore("A.cfg");
		ConfigMap cm = s.getMap("A.cfg");

		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar",
			" ",
			null
		};

		for (String t : test) {
			assertThrown(()->cm.setEntry(t, "k1", "foo", null, null, null)).asMessage().isContains("Invalid section name");
		}
	}

	@Test
	public void testSetEntryInvalidKeyNames() throws Exception {
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
			assertThrown(()->cm.setEntry("S1", t, "foo", null, null, null)).asMessage().isContains("Invalid key name");
		}
	}

	@Test
	public void testSetEntryWithCommentChars() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1 = v1"
		);
		ConfigMap cm = s.getMap("A.cfg");

		// If value has # in it, it should get escaped.
		cm.setEntry("S1", "k1", "v1 # foo", null, null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1 = v1 \\u0023 foo|");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Modifiers
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testModifiers() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1<^> = v1",
			"k2<*> = v2",
			"k3<*^> = v3"
		);
		ConfigMap cm = s.getMap("A.cfg");

		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1<^> = v1|k2<*> = v2|k3<*^> = v3|");
		assertEquals("^", cm.getEntry("S1", "k1").getModifiers());
		assertEquals("*", cm.getEntry("S1", "k2").getModifiers());
		assertEquals("*^", cm.getEntry("S1", "k3").getModifiers());

		cm.setEntry("S1", "k1", "v1", "#$%&*+^@~", null, null);
		assertString(cm).asReplaceAll("\\r?\\n", "|").is("[S1]|k1<#$%&*+^@~> = v1|k2<*> = v2|k3<*^> = v3|");
	}

	@Test
	public void testInvalidModifier() throws Exception {
		ConfigStore s = initStore("A.cfg",
			"[S1]",
			"k1^ = v1",
			"k2* = v2",
			"k3*^ = v3"
		);
		ConfigMap cm = s.getMap("A.cfg");

		// This is okay.
		cm.setEntry("S1", "k1", "v1", "", null, null);
	}

	private static ConfigStore initStore(String name, String...contents) {
		return MemoryStore.create().build().update(name, contents);
	}
}
