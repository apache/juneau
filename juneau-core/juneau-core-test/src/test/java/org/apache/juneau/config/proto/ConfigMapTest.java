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
package org.apache.juneau.config.proto;

import static org.junit.Assert.*;

import java.util.*;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.config.store.*;
import org.junit.*;

public class ConfigMapTest {
	
	final static String ENCODED = "*";
	final static String BASE64 = "^";
	
	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read non-existent files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testNonExistentConfig() throws Exception {
		Store s = MemoryStore.create().build();		
		ConfigMap cm = s.getMap("Foo");
		assertEquals("", cm.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should be able to read blank files without errors.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testBlankConfig() throws Exception {
		
		Store s = initStore("Foo", "");		
		ConfigMap cm = s.getMap("Foo");
		assertEquals("", cm.toString());
		
		s.update("Foo", "   \n   \n   ");
		cm = s.getMap("Foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleOneLine() throws Exception {
		Store s = initStore("Foo", 
			"foo=bar"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertTextEquals("foo=bar|", cm);
		
		assertEquals("", join(cm.getPreLines("default"), '|'));
		assertEquals("", join(cm.getEntry("default", "foo").getPreLines(), '|'));
		
		assertEquals("bar", cm.getEntry("default", "foo").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("foo=bar|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple one-line file with leading comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleOneLineWithComments() throws Exception {
		Store s = initStore("Foo", 
			"#comment",
			"foo=bar"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertTextEquals("#comment|foo=bar|", cm);

		assertEquals("", join(cm.getPreLines("default"), '|'));
		assertEquals("#comment", join(cm.getEntry("default", "foo").getPreLines(), '|'));

		assertEquals("bar", cm.getEntry("default", "foo").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("#comment|foo=bar|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Simple section.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleSection() throws Exception {
		Store s = initStore("Foo", 
			"[MySection]",
			"foo=bar"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertTextEquals("[MySection]|foo=bar|", cm);
		
		assertEquals("", join(cm.getPreLines("default"), '|'));
		assertEquals("", join(cm.getPreLines("MySection"), '|'));
		assertEquals("", join(cm.getEntry("MySection", "foo").getPreLines(), '|'));
		
		assertEquals("bar", cm.getEntry("MySection", "foo").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("[MySection]|foo=bar|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Non-existent values should not throw exceptions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testNonExistentValues() throws Exception {
		Store s = initStore("Foo", 
			"[MySection]",
			"foo=bar"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertTextEquals("[MySection]|foo=bar|", cm);
		
		assertEquals("", join(cm.getPreLines("default"), '|'));
		
		assertNull(cm.getPreLines("XXX"));

		assertNull(cm.getEntry("XXX", "yyy"));
		assertNull(cm.getEntry("MySection", "yyy"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleSectionWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleSectionWithComments() throws Exception {
		Store s = initStore("Foo", 
			"#S1",
			"[S1]",
			"#k1",
			"k1=v1",
			"#S2",
			"[S2]",
			"#k2",
			"k2=v2"
		);		
		ConfigMap cm = s.getMap("Foo");
		assertTextEquals("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|", cm);
		
		assertEquals("", join(cm.getPreLines("default"), '|'));
		assertEquals("#S1", join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("#S2", join(cm.getPreLines("S2"), '|'));
		assertEquals("#k2", join(cm.getEntry("S2", "k2").getPreLines(), '|'));
		
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2", cm.getEntry("S2", "k2").getValue());

		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("#S1|[S1]|#k1|k1=v1|#S2|[S2]|#k2|k2=v2|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithComments
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleAndDefaultSectionsWithComments() throws Exception {
		Store s = initStore("Foo", 
			"#D",
			"",
			"#k",
			"k=v",
			"#S1",
			"[S1]",
			"#k1",
			"k1=v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		assertTextEquals("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|", cm);

		assertEquals("#D", join(cm.getPreLines("default"), '|'));
		assertEquals("#k", join(cm.getEntry("default", "k").getPreLines(), '|'));
		assertEquals("#S1", join(cm.getPreLines("S1"), '|'));
		assertEquals("#k1", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		
		assertEquals("v", cm.getEntry("default", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("#D||#k|k=v|#S1|[S1]|#k1|k1=v1|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// testSimpleAndDefaultSectionsWithCommentsAndExtraSpaces
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSimpleAndDefaultSectionsWithCommentsAndExtraSpaces() throws Exception {
		Store s = initStore("Foo", 
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
		ConfigMap cm = s.getMap("Foo");
		assertTextEquals("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|", cm);

		assertEquals("#Da|#Db", join(cm.getPreLines("default"), '|'));
		assertEquals("#ka||#kb|", join(cm.getEntry("default", "k").getPreLines(), '|'));
		assertEquals("|#S1a||#S1b|", join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1a||#k1b|", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		
		assertEquals("v", cm.getEntry("default", "k").getValue());
		assertEquals("v1", cm.getEntry("S1", "k1").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("#Da|#Db||#ka||#kb||k=v||#S1a||#S1b||[S1]||#k1a||#k1b||k1=v1|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Error conditions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testMalformedSectionHeaders() throws Exception {
		
		String[] test = {
			"[default]", "[ default ]", " [ default ] ", "\t[\tdefault\t]\t",
			"[/]", "[[]", "[]]", "[\\]", 
			"[foo/bar]", "[foo[bar]", "[foo]bar]", "[foo\\bar]", 
			"[]", "[ ]", "[\t]", " [] ",
		};
		
		for (String t : test) {
			Store s = initStore("Foo", t);		
			try {
				s.getMap("Foo");
				fail("Exception expected.");
			} catch (ConfigException e) {
				assertTrue(e.getLocalizedMessage().startsWith("Invalid section name"));
			}
		}
	}
	
	@Test
	public void testDuplicateSectionNames() throws Exception {
		Store s = initStore("Foo", "[S1]", "[S1]");		
		try {
			s.getMap("Foo");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Duplicate section found in configuration:  [S1]", e.getLocalizedMessage());
		}
	}	

	@Test
	public void testDuplicateEntryNames() throws Exception {
		Store s = initStore("Foo", "[S1]", "foo=v1", "foo=v2");		
		try {
			s.getMap("Foo");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Duplicate entry found in section [S1] of configuration:  foo", e.getLocalizedMessage());
		}
	}	
	
	//-----------------------------------------------------------------------------------------------------------------
	// Lines can be split up.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testMultipleLines() throws Exception {
		Store s = initStore("Foo", 
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertEquals("", join(cm.getEntry("default", "k1").getPreLines(), '|'));
		assertEquals("", join(cm.getEntry("default", "k2").getPreLines(), '|'));

		assertTextEquals("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("default", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("default", "k2").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);
	}

	@Test
	public void testMultipleLinesWithSpacesAndComments() throws Exception {
		Store s = initStore("Foo", 
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
		ConfigMap cm = s.getMap("Foo");
		
		assertEquals("|#k1|", join(cm.getEntry("default", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", join(cm.getEntry("default", "k2").getPreLines(), '|'));

		assertTextEquals("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("default", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("default", "k2").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("|#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);
	}

	@Test
	public void testMultipleLinesInSection() throws Exception {
		Store s = initStore("Foo", 
			"[S1]",
			"k1 = v1a,",
			"\tv1b,",
			"\tv1c",
			"k2 = v2a,",
			"\tv2b,",
			"\tv2c"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertEquals("", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("", join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertTextEquals("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("[S1]|k1 = v1a,|\tv1b,|\tv1c|k2 = v2a,|\tv2b,|\tv2c|", cm);
	}

	@Test
	public void testMultipleLinesInSectionWithSpacesAndPrelines() throws Exception {
		Store s = initStore("Foo",
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
		ConfigMap cm = s.getMap("Foo");
		
		assertEquals("|#S1|", join(cm.getPreLines("S1"), '|'));
		assertEquals("|#k1|", join(cm.getEntry("S1", "k1").getPreLines(), '|'));
		assertEquals("|#k2|", join(cm.getEntry("S1", "k2").getPreLines(), '|'));

		assertTextEquals("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);

		assertEquals("v1a,\nv1b,\nv1c", cm.getEntry("S1", "k1").getValue());
		assertEquals("v2a,\nv2b,\nv2c", cm.getEntry("S1", "k2").getValue());
		
		// Round trip.
		s.update("Foo", cm.toString());
		cm = s.getMap("Foo");
		assertTextEquals("|#S1||[S1]||#k1||k1 = v1a,|	v1b,|	v1c||#k2||k2 = v2a,|	v2b,|	v2c|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Entry lines can have trailing comments.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testEntriesWithComments() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = foo # comment"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertTextEquals("[S1]|k1 = foo # comment|", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("comment", cm.getEntry("S1", "k1").getComment());
		
		cm.setComment("S1", "k1", "newcomment");
		assertTextEquals("[S1]|k1 = foo # newcomment|", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("newcomment", cm.getEntry("S1", "k1").getComment());
		
		cm.setComment("S1", "k1", "");
		assertTextEquals("[S1]|k1 = foo # |", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals("", cm.getEntry("S1", "k1").getComment());
		
		cm.setComment("S1", "k1", null);
		assertTextEquals("[S1]|k1 = foo|", cm);
		assertEquals("foo", cm.getEntry("S1", "k1").getValue());
		assertEquals(null, cm.getEntry("S1", "k1").getComment());
	}
	
	@Test
	public void testEntriesWithOddComments() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = foo#",
			"k2 = foo # "
		);		
		ConfigMap cm = s.getMap("Foo");
		assertTextEquals("[S1]|k1 = foo#|k2 = foo # |", cm);
		assertEquals("", cm.getEntry("S1", "k1").getComment());
		assertEquals("", cm.getEntry("S1", "k2").getComment());
	}
	
	@Test
	public void testEntriesWithEscapedComments() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = foo\\#bar",
			"k2 = foo \\# bar",
			"k3 = foo \\# bar # real-comment"
		);		
		ConfigMap cm = s.getMap("Foo");
		assertTextEquals("[S1]|k1 = foo\\#bar|k2 = foo \\# bar|k3 = foo \\# bar # real-comment|", cm);
		
		assertEquals(null, cm.getEntry("S1", "k1").getComment());
		assertEquals(null, cm.getEntry("S1", "k2").getComment());
		assertEquals("real-comment", cm.getEntry("S1", "k3").getComment());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test setting entries.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSettingEntries() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1a",
			"k2 = v2a"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("S1", "k1", "v1b");
		cm.setValue("S1", "k2", null);
		cm.setValue("S1", "k3", "v3b");
		
		assertTextEquals("[S1]|k1 = v1b|k3 = v3b|", cm);
		
		cm.save();
		assertTextEquals("[S1]|k1 = v1b|k3 = v3b|", s.read("Foo"));
		
		// Round trip.
		cm = s.getMap("Foo");
		assertTextEquals("[S1]|k1 = v1b|k3 = v3b|", cm);
	}
	
	@Test
	public void testSettingEntriesWithPreLines() throws Exception {
		Store s = initStore("Foo",
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
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("S1", "k1", "v1b");
		cm.setValue("S1", "k2", null);
		cm.setValue("S1", "k3", "v3b");
		cm.setEntry("S1", "k4", "v4b", null, null, Arrays.asList("","#k4",""));
		
		assertTextEquals("|#S1||[S1]||#k1||k1 = v1b|k3 = v3b||#k4||k4 = v4b|", cm);
		
		cm.save();
		assertTextEquals("|#S1||[S1]||#k1||k1 = v1b|k3 = v3b||#k4||k4 = v4b|", s.read("Foo"));
		
		// Round trip.
		cm = s.getMap("Foo");
		assertTextEquals("|#S1||[S1]||#k1||k1 = v1b|k3 = v3b||#k4||k4 = v4b|", cm);
	}
	
	@Test
	public void testSettingEntriesWithNewlines() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("default", "k", "v1\nv2\nv3");
		cm.setValue("S1", "k1", "v1\nv2\nv3");
		
		assertTextEquals("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", cm);
		
		assertEquals("v1\nv2\nv3", cm.getEntry("default", "k").getValue());
		assertEquals("v1\nv2\nv3", cm.getEntry("S1", "k1").getValue());
		cm.save();
		assertTextEquals("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", cm);
		
		// Round trip.
		cm = s.getMap("Foo");
		assertTextEquals("k = v1|	v2|	v3|[S1]|k1 = v1|	v2|	v3|", cm);
	}
	
	@Test
	public void testSettingEntriesWithNewlinesAndSpaces() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("default", "k", "v1 \n v2 \n v3");
		cm.setValue("S1", "k1", "v1\t\n\tv2\t\n\tv3");
		
		assertTextEquals("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", cm);
		
		assertEquals("v1 \n v2 \n v3", cm.getEntry("default", "k").getValue());
		assertEquals("v1\t\n\tv2\t\n\tv3", cm.getEntry("S1", "k1").getValue());
		cm.save();
		assertTextEquals("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", cm);
		
		// Round trip.
		cm = s.getMap("Foo");
		assertTextEquals("k = v1 |	 v2 |	 v3|[S1]|k1 = v1	|		v2	|		v3|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setSection()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetSectionOnExistingSection() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setSection("S1", Arrays.asList("#S1"));
		assertTextEquals("#S1|[S1]|k1 = v1|", cm);
		cm.setSection("S1", Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setSection("S1", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}
	
	@Test
	public void testSetSectionOnDefaultSection() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setSection("default", Arrays.asList("#D"));
		assertTextEquals("#D||[S1]|k1 = v1|", cm);
		cm.setSection("default", Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setSection("default", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}

	@Test
	public void testSetSectionOnNewSection() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setSection("S2", Arrays.asList("#S2"));
		assertTextEquals("[S1]|k1 = v1|#S2|[S2]|", cm);
		cm.setSection("S3", Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|#S2|[S2]|[S3]|", cm);
		cm.setSection("S4", null);
		assertTextEquals("[S1]|k1 = v1|#S2|[S2]|[S3]|[S4]|", cm);
	}

	@Test
	public void testSetSectionBadNames() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");
		
		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar", 
			"", " ",
			null
		};
		
		for (String t : test) {
			try {
				cm.setSection(t, null);
				fail("Exception expected.");
			} catch (ConfigException e) {
				assertTrue(e.getLocalizedMessage().startsWith("Invalid section name"));
			}
		}
	}
	
	@Test
	public void testSetSectionOkNames() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");

		// These are all okay characters to use in section names.
		String validChars = "~`!@#$%^&*()_-+={}|:;\"\'<,>.?";
		
		for (char c : validChars.toCharArray()) {
			String test = ""+c;
			cm.setSection(test, Arrays.asList("test"));
			cm.save();
			assertEquals("test", cm.getPreLines(test).get(0));
			
			test = "foo"+c+"bar";
			cm.setSection(test, Arrays.asList("test"));
			cm.save();
			assertEquals("test", cm.getPreLines(test).get(0));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// removeSection()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSectionOnExistingSection() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"
			
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.removeSection("S1");
		assertTextEquals("[S2]|k2 = v2|", cm);
	}
	
	@Test
	public void testRemoveSectionOnNonExistingSection() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"
			
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.removeSection("S3");
		cm.removeSection("");
		cm.removeSection(null);
		assertTextEquals("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}

	@Test
	public void testRemoveDefaultSection() throws Exception {
		Store s = initStore("Foo",
			"k = v",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"
			
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.removeSection("default");
		assertTextEquals("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}
	
	@Test
	public void testRemoveDefaultSectionWithComments() throws Exception {
		Store s = initStore("Foo",
			"#D",
			"",
			"#k",
			"k = v",
			"[S1]",
			"k1 = v1",
			"[S2]",
			"k2 = v2"
			
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.removeSection("default");
		assertTextEquals("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// setPreLines()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetPrelinesOnExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setPreLines("S1", "k1", Arrays.asList("#k1"));
		assertTextEquals("[S1]|#k1|k1 = v1|", cm);
		cm.setPreLines("S1", "k1", Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines("S1", "k1", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}
	
	@Test
	public void testSetPrelinesOnExistingEntryWithAtrributes() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"#k1a",
			"k1 = v1 # comment"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setPreLines("S1", "k1", Arrays.asList("#k1b"));
		assertTextEquals("[S1]|#k1b|k1 = v1 # comment|", cm);
	}

	@Test
	public void testSetPrelinesOnNonExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setPreLines("S1", "k2", Arrays.asList("#k2"));
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines("S1", "k2", Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines("S1", "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
		
		cm.setPreLines("S2", "k2", Arrays.asList("#k2"));
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines("S2", "k2", Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines("S2", "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);

		cm.setPreLines("S1", null, Arrays.asList("#k2"));
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines("S1", null, Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines("S1", null, null);
		assertTextEquals("[S1]|k1 = v1|", cm);

		cm.setPreLines(null, "k2", Arrays.asList("#k2"));
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines(null, "k2", Collections.<String>emptyList());
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setPreLines(null, "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetValueOnExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("S1", "k1", "v2");
		assertTextEquals("[S1]|k1 = v2|", cm);
	}
	
	@Test
	public void testSetValueOnExistingEntryWithAttributes() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("S1", "k1", "v2");
		assertTextEquals("[S1]|#k1|k1 = v2 # comment|", cm);
	}

	@Test
	public void testSetValueToNullOnExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("S1", "k1", null);
		assertTextEquals("[S1]|", cm);
	}

	@Test
	public void testSetValueOnNonExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("S1", "k2", "v2");
		assertTextEquals("[S1]|k1 = v1|k2 = v2|", cm);
		cm.setValue("S1", "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setValue("S1", "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}
	
	@Test
	public void testSetValueOnNonExistingEntryOnNonExistentSection() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setValue("S2", "k2", "v2");
		assertTextEquals("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}

	@Test
	public void testSetValueInvalidSectionNames() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");
		
		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar", 
			"", " ",
			null
		};
		
		for (String t : test) {
			try {
				cm.setValue(t, "k1", "foo");
				fail("Exception expected.");
			} catch (ConfigException e) {
				assertTrue(e.getLocalizedMessage().startsWith("Invalid section name"));
			}
		}
	}

	@Test
	public void testSetValueInvalidKeyNames() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");
		
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
			try {
				cm.setValue("S1", t, "foo");
				fail("Exception expected.");
			} catch (ConfigException e) {
				assertTrue(e.getLocalizedMessage().startsWith("Invalid key name"));
			}
		}
	}
	
	@Test
	public void testSetValueWithCommentChars() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		// If value has # in it, it should get escaped.
		cm.setValue("S1", "k1", "v1 # foo");
		assertTextEquals("[S1]|k1 = v1 \\# foo|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// setComment()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetCommentOnExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setComment("S1", "k1", "c1");
		assertTextEquals("[S1]|k1 = v1 # c1|", cm);

		cm.setComment("S1", "k1", "");
		assertTextEquals("[S1]|k1 = v1 # |", cm);
		cm.save();
		assertTextEquals("[S1]|k1 = v1 # |", cm);
		
		cm.setComment("S1", "k1", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}
	
	@Test
	public void testSetCommentOnExistingEntryWithAttributes() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"#k1a",
			"k1 = v1 # c1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setComment("S1", "k1", "c2");
		assertTextEquals("[S1]|#k1a|k1 = v1 # c2|", cm);
	}

	@Test
	public void testSetCommentOnNonExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setComment("S1", "k2", "foo");
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setComment("S1", "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
		
		cm.setComment("S2", "k2", "foo");
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setComment("S2", "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);

		cm.setComment("S1", null, "foo");
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setComment("S1", null, null);
		assertTextEquals("[S1]|k1 = v1|", cm);

		cm.setComment(null, "k2", "foo");
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setComment(null, "k2", null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// setValue()
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testSetEntryOnExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertTextEquals("[S1]|k1 = v2|", cm);

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertTextEquals("[S1]|#k1a|k1* = v3 # c3|", cm);

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertTextEquals("[S1]|#k1b|k1^ = v4 # c4|", cm);
	}
	
	@Test
	public void testSetEntryOnExistingEntryWithAttributes() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"#k1",
			"k1 = v1 # comment"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setEntry("S1", "k1", "v2", null, null, null);
		assertTextEquals("[S1]|k1 = v2|", cm);

		cm.setEntry("S1", "k1", "v3", ENCODED, "c3", Arrays.asList("#k1a"));
		assertTextEquals("[S1]|#k1a|k1* = v3 # c3|", cm);

		cm.setEntry("S1", "k1", "v4", BASE64, "c4", Arrays.asList("#k1b"));
		assertTextEquals("[S1]|#k1b|k1^ = v4 # c4|", cm);
	}

	@Test
	public void testSetEntryToNullOnExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setEntry("S1", "k1", null, null, null, null);
		assertTextEquals("[S1]|", cm);

		cm.setEntry("S1", "k1", null, ENCODED, "c3", Arrays.asList("#k1a"));
		assertTextEquals("[S1]|", cm);

		cm.setEntry("S1", "k1", null, BASE64, "c4", Arrays.asList("#k1b"));
		assertTextEquals("[S1]|", cm);
	}
	
	@Test
	public void testSetEntryOnNonExistingEntry() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setEntry("S1", "k2", "v2", null, null, null);
		assertTextEquals("[S1]|k1 = v1|k2 = v2|", cm);
		cm.setEntry("S1", "k2", null, null, null, null);
		assertTextEquals("[S1]|k1 = v1|", cm);
		cm.setEntry("S1", "k2", null, null, null, null);
		assertTextEquals("[S1]|k1 = v1|", cm);
	}
	
	@Test
	public void testSetEntryOnNonExistingEntryOnNonExistentSection() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		cm.setEntry("S2", "k2", "v2", null, null, null);
		assertTextEquals("[S1]|k1 = v1|[S2]|k2 = v2|", cm);
	}

	@Test
	public void testSetEntryInvalidSectionNames() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");
		
		String[] test = {
			"/", "[", "]",
			"foo/bar", "foo[bar", "foo]bar", 
			"", " ",
			null
		};
		
		for (String t : test) {
			try {
				cm.setEntry(t, "k1", "foo", null, null, null);
				fail("Exception expected.");
			} catch (ConfigException e) {
				assertTrue(e.getLocalizedMessage().startsWith("Invalid section name"));
			}
		}
	}

	@Test
	public void testSetEntryInvalidKeyNames() throws Exception {
		Store s = initStore("Foo");		
		ConfigMap cm = s.getMap("Foo");
		
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
			try {
				cm.setEntry("S1", t, "foo", null, null, null);
				fail("Exception expected.");
			} catch (ConfigException e) {
				assertTrue(e.getLocalizedMessage().startsWith("Invalid key name"));
			}
		}
	}
	
	@Test
	public void testSetEntryWithCommentChars() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1 = v1"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		// If value has # in it, it should get escaped.
		cm.setEntry("S1", "k1", "v1 # foo", null, null, null);
		assertTextEquals("[S1]|k1 = v1 \\# foo|", cm);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Modifiers
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testModifiers() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1^ = v1",
			"k2* = v2",
			"k3*^ = v3"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		assertTextEquals("[S1]|k1^ = v1|k2* = v2|k3*^ = v3|", cm);
		assertTrue(cm.getEntry("S1", "k1").hasModifier('^'));
		assertFalse(cm.getEntry("S1", "k1").hasModifier('*'));
		assertFalse(cm.getEntry("S1", "k2").hasModifier('^'));
		assertTrue(cm.getEntry("S1", "k2").hasModifier('*'));
		assertTrue(cm.getEntry("S1", "k3").hasModifier('^'));
		assertTrue(cm.getEntry("S1", "k3").hasModifier('*'));
		
		cm.setEntry("S1", "k1", "v1", "#$%&*+^@~", null, null);
		assertTextEquals("[S1]|k1#$%&*+^@~ = v1|k2* = v2|k3*^ = v3|", cm);
	}
	
	@Test
	public void testInvalidModifier() throws Exception {
		Store s = initStore("Foo",
			"[S1]",
			"k1^ = v1",
			"k2* = v2",
			"k3*^ = v3"
		);		
		ConfigMap cm = s.getMap("Foo");
		
		// This is okay.
		cm.setEntry("S1", "k1", "v1", "", null, null);

		try {
			cm.setEntry("S1", "k1", "v1", "X", null, null);
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Invalid modifiers: X", e.getLocalizedMessage());
		}

		try {
			cm.setEntry("S1", "k1", "v1", " ", null, null);
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Invalid modifiers:  ", e.getLocalizedMessage());
		}
	}

	private static Store initStore(String name, String...contents) {
		return MemoryStore.create().build().update(name, contents);
	}
}
