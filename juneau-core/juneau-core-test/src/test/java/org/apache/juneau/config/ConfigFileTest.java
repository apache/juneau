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
import static org.junit.Assert.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.FileUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;
import org.junit.*;

public class ConfigFileTest {

	private ConfigFileBuilder configFileBuilder = new ConfigFileBuilder();

	private File getFreshFile() {
		String tempDir = System.getProperty("java.io.tmpdir");
		File f = new File(tempDir, "Test.cfg");
		if (f.exists())
			f.delete();
		f.deleteOnExit();
		return f;
	}

	//====================================================================================================
	// testCommentsOnly
	//====================================================================================================
	@Test
	public void testCommentsOnly() throws Exception {

		File f = getFreshFile();

		ConfigFile cf = configFileBuilder.build(f)
			.addLines(null, "# c1", "\t# c2", " c3 ");
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		String expected = "# c1|\t# c2| c3 |";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		cf.save();
		cf = configFileBuilder.build(f);
		expected = "# c1|\t# c2| c3 |";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

	}

	//====================================================================================================
	// testBasics
	//====================================================================================================
	@Test
	public void testBasics() throws Exception {

		File f = getFreshFile();

		ConfigFile cf = configFileBuilder.build(f)
			.addLines(null, "# c1", "\t# c2", " c3 ", "x1=1", "x2=true", "x3=null")
			.addLines("s1", "#c4", "k1=1", "#c5 foo=bar", "k2 = true", "k3  = \tnull");
		ConfigFile cfw = cf.getResolving().getResolving();

		String expected = "# c1|\t# c2| c3 |x1 = 1|x2 = true|x3 = null|[s1]|#c4|k1 = 1|#c5 foo=bar|k2 = true|k3 = null|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		cf.save();
		cf = configFileBuilder.build(f);
		cfw = cf.getResolving(VarResolver.DEFAULT);
		assertEquals(1, cf.getInt("x1"));
		assertEquals(true, cf.getBoolean("x2"));
		assertEquals("null", cf.getString("x3"));
		assertEquals(1, cf.getInt("s1/k1"));
		assertEquals(true, cf.getBoolean("s1/k2"));
		assertEquals("null", cf.getString("s1/k3"));

		assertEquals(-1, cf.getInt("X1/k1", -1));
		assertEquals(true, cf.getBoolean("X1/k2", true));
		assertEquals("null", cf.getString("X1/k3", "null"));

		assertEquals(1, cfw.getInt("x1"));
		assertEquals(true, cfw.getBoolean("x2"));
		assertEquals("null", cfw.getString("x3"));
		assertEquals(1, cfw.getInt("s1/k1"));
		assertEquals(true, cfw.getBoolean("s1/k2"));
		assertEquals("null", cfw.getString("s1/k3"));

		assertEquals(-1, cfw.getInt("X1/k1", -1));
		assertEquals(true, cfw.getBoolean("X1/k2", true));
		assertEquals("null", cfw.getString("X1/k3", "null"));

		cf.put("x1", 2);
		cf.put("x2", false);
		cf.put("x3", "foo");
		cf.put("s1/k1", 2);
		cf.put("s1/k2", false);
		cf.put("s1/k3", "bar");

		expected = "# c1|\t# c2| c3 |x1 = 2|x2 = false|x3 = foo|[s1]|#c4|k1 = 2|#c5 foo=bar|k2 = false|k3 = bar|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		cfw.put("x1", 3);
		cfw.put("x2", true);
		cfw.put("x3", "bar");
		cfw.put("s1/k1", 4);
		cfw.put("s1/k2", true);
		cfw.put("s1/k3", "baz");

		expected = "# c1|\t# c2| c3 |x1 = 3|x2 = true|x3 = bar|[s1]|#c4|k1 = 4|#c5 foo=bar|k2 = true|k3 = baz|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		// Null strings.
		cf.put("x1", (String)null);
		expected = "# c1|\t# c2| c3 |x1 = |x2 = true|x3 = bar|[s1]|#c4|k1 = 4|#c5 foo=bar|k2 = true|k3 = baz|";
		assertTextEquals(expected, cf);
	}

	@Test
	public void testSerialization() throws Exception {

		ConfigFile cf = configFileBuilder.build();
		cf.put("x1", 1);

		String expected = "{'default':{x1:'1'}}";
		assertObjectEquals(expected, cf);
		cf = cf.getResolving(VarResolver.DEFAULT);
		assertObjectEquals(expected, cf);
	}

	//====================================================================================================
	// testHeaderComments
	//====================================================================================================
	@Test
	public void testHeaderComments() throws Exception {
		ConfigFile[] cff = {
			configFileBuilder.build(getFreshFile()).addLines(null, "x").addLines("s1", "#c3", "#c4").addHeaderComments("s1", "#c1", "#c2"),
			configFileBuilder.build(getFreshFile()).addLines(null, "x").addLines("s1", "#c3", "#c4").addHeaderComments("s1", "#c1", "#c2").getResolving()
		};

		for (ConfigFile cf : cff) {

			String expected = "x|#c1|#c2|[s1]|#c3|#c4|";
			assertTextEquals(expected, cf);

			cf.save();
			cf.load();

			cf.clearHeaderComments("s1");
			expected = "x|[s1]|#c3|#c4|";
			assertTextEquals(expected, cf);

			cf.clearHeaderComments("x1");

			cf.addHeaderComments("s1", "#c5", "c6");
			expected = "x|#c5|#c6|[s1]|#c3|#c4|";
			assertTextEquals(expected, cf);
		}
	}

	//====================================================================================================
	// testRemoveEntries
	//====================================================================================================
	@Test
	public void testRemoveEntries() throws Exception {

		File f = getFreshFile();

		ConfigFile cf = configFileBuilder.build(f)
			.addLines(null, "x1=1")
			.addLines("s1", "x2=2");
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		String expected = "x1 = 1|[s1]|x2 = 2|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		cf.save();
		cf.load();

		cf.removeString("x1");
		expected = "[s1]|x2 = 2|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		cfw.save();
		cfw.load();

		cf.removeString("s1/x2");
		expected = "[s1]|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		cf.removeSection("s1");
		assertEquals("", cf.toString());
		assertEquals("", cfw.toString());
	}

	//====================================================================================================
	// testPut
	//====================================================================================================
	@Test
	public void testPut() throws Exception {

		ConfigFile cf = configFileBuilder.build();
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		cf.addSection(null);
		cf.put("x1", "1");
		cf.addHeaderComments(null, "#h1");
		cf.addLines(null, "#c1", "c2");
		cf.addSection("s1");
		cf.put("s1/x2", "1");
		cf.addHeaderComments("s1","#h2");
		cf.addLines("s1", "#c3", "c4");

		String expected = "#h1|x1 = 1|#c1|c2|#h2|[s1]|x2 = 1|#c3|c4|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);

		cfw.addSection(null);
		cfw.put("x2", "2");
		cfw.addHeaderComments(null, "#h2");
		cfw.addLines(null, "#c2", "c3");
		cfw.addSection("s2");
		cfw.put("s2/x3", "2");
		cfw.addHeaderComments("s2","#h3");
		cfw.addLines("s2", "#c4", "c5");

		expected = "#h1|#h2|x1 = 1|#c1|c2|x2 = 2|#c2|c3|#h2|[s1]|x2 = 1|#c3|c4|#h3|[s2]|x3 = 2|#c4|c5|";
		assertTextEquals(expected, cf);
		assertTextEquals(expected, cfw);
	}

	//====================================================================================================
	// testExampleInConfigFile - Example in ConfigFile
	//====================================================================================================
	@Test
	public void testExampleInConfigFile() throws Exception {

		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "# Default section", "key1 = 1", "key2 = true", "key3 = [1,2,3]", "key4 = http://foo", "")
			.addHeaderComments("section1", "# Section 1")
			.addLines("section1", "key1 = 2", "key2 = false", "key3 = [4,5,6]", "key4 = http://bar");
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		assertEquals(1, cf.getInt("key1"));
		assertEquals(true, cf.getBoolean("key2"));
		assertEquals(3, cf.getObject("key3", int[].class)[2]);
		assertEquals(6, cf.getObjectWithDefault("xkey3", new int[]{4,5,6}, int[].class)[2]);
		assertEquals(6, cf.getObjectWithDefault("X/key3", new int[]{4,5,6}, int[].class)[2]);
		assertEquals(new URL("http://foo").toString(), cf.getObject("key4", URL.class).toString());

		assertEquals(1, cfw.getInt("key1"));
		assertEquals(true, cfw.getBoolean("key2"));
		assertEquals(3, cfw.getObject("key3", int[].class)[2]);
		assertEquals(6, cfw.getObjectWithDefault("xkey3", new int[]{4,5,6}, int[].class)[2]);
		assertEquals(6, cfw.getObjectWithDefault("X/key3", new int[]{4,5,6}, int[].class)[2]);
		assertEquals(new URL("http://foo").toString(), cfw.getObject("key4", URL.class).toString());

		assertEquals(2, cf.getInt("section1/key1"));
		assertEquals(false, cf.getBoolean("section1/key2"));
		assertEquals(6, cf.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cf.getObject("section1/key4", URL.class).toString());

		assertEquals(2, cfw.getInt("section1/key1"));
		assertEquals(false, cfw.getBoolean("section1/key2"));
		assertEquals(6, cfw.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cfw.getObject("section1/key4", URL.class).toString());

		cf = configFileBuilder.build(getFreshFile())
			.addLines(null, "# Default section")
			.addHeaderComments("section1", "# Section 1");
		cfw = cf.getResolving(VarResolver.DEFAULT);

		cf.put("key1", 1);
		cf.put("key2", true);
		cf.put("key3", new int[]{1,2,3});
		cf.put("key4", new URL("http://foo"));
		cf.put("section1/key1", 2);
		cf.put("section1/key2", false);
		cf.put("section1/key3", new int[]{4,5,6});
		cf.put("section1/key4", new URL("http://bar"));

		cf.save();
		cf.load();

		assertEquals(1, cf.getInt("key1"));
		assertEquals(true, cf.getBoolean("key2"));
		assertEquals(3, cf.getObject("key3", int[].class)[2]);
		assertEquals(new URL("http://foo").toString(), cf.getObject("key4", URL.class).toString());

		assertEquals(1, cfw.getInt("key1"));
		assertEquals(true, cfw.getBoolean("key2"));
		assertEquals(3, cfw.getObject("key3", int[].class)[2]);
		assertEquals(new URL("http://foo").toString(), cfw.getObject("key4", URL.class).toString());

		assertEquals(2, cf.getInt("section1/key1"));
		assertEquals(false, cf.getBoolean("section1/key2"));
		assertEquals(6, cf.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cf.getObject("section1/key4", URL.class).toString());

		assertEquals(2, cfw.getInt("section1/key1"));
		assertEquals(false, cfw.getBoolean("section1/key2"));
		assertEquals(6, cfw.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cfw.getObject("section1/key4", URL.class).toString());

		cfw.put("key1", 2);
		cfw.put("key2", false);
		cfw.put("key3", new int[]{4,5,6});
		cfw.put("key4", new URL("http://bar"));
		cfw.put("section1/key1", 3);
		cfw.put("section1/key2", true);
		cfw.put("section1/key3", new int[]{7,8,9});
		cfw.put("section1/key4", new URL("http://baz"));

		cfw.save();
		cfw.load();

		assertEquals(2, cf.getInt("key1"));
		assertEquals(false, cf.getBoolean("key2"));
		assertEquals(6, cf.getObject("key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cf.getObject("key4", URL.class).toString());

		assertEquals(2, cfw.getInt("key1"));
		assertEquals(false, cfw.getBoolean("key2"));
		assertEquals(6, cfw.getObject("key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cfw.getObject("key4", URL.class).toString());

		assertEquals(3, cf.getInt("section1/key1"));
		assertEquals(true, cf.getBoolean("section1/key2"));
		assertEquals(9, cf.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://baz").toString(), cf.getObject("section1/key4", URL.class).toString());

		assertEquals(3, cfw.getInt("section1/key1"));
		assertEquals(true, cfw.getBoolean("section1/key2"));
		assertEquals(9, cfw.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://baz").toString(), cfw.getObject("section1/key4", URL.class).toString());
	}

	//====================================================================================================
	// testEnum
	//====================================================================================================
	@Test
	public void testEnum() throws Exception {
		ConfigFile cf = configFileBuilder.build(getFreshFile())
			.addLines(null, "key1 = MINUTES");
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		assertEquals(TimeUnit.MINUTES, cf.getObject("key1", TimeUnit.class));
		assertEquals(TimeUnit.MINUTES, cfw.getObject("key1", TimeUnit.class));

		cf.save();
		cf.load();

		assertEquals(TimeUnit.MINUTES, cf.getObject("key1", TimeUnit.class));
		assertEquals(TimeUnit.MINUTES, cfw.getObject("key1", TimeUnit.class));
	}

	//====================================================================================================
	// testBatchFileGeneration
	//====================================================================================================
	@Test
	public void testBatchFileGeneration() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "# c1", "\t# c2", " c3 ", "", "  ", "x1=1", "x2=true", "x3=null")
			.addHeaderComments(null, "header null", "", null)
			.addLines("s1", "#c4", "k1=1", "#c5 foo=bar", "k2 = true", "k3  = \tnull")
			.addHeaderComments("s1", "header s1", "", null);
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		String expected = "rem header null|||rem c1|rem c2|rem c3|||set x1 = 1|set x2 = true|set x3 = null|rem header s1|||rem c4|set s1_k1 = 1|rem c5 foo=bar|set s1_k2 = true|set s1_k3 = null|";

		StringWriter sw = new StringWriter();
		cf.serializeTo(new PrintWriter(sw), ConfigFileFormat.BATCH);
		assertTextEquals(expected, sw);

		sw = new StringWriter();
		cfw.serializeTo(new PrintWriter(sw), ConfigFileFormat.BATCH);
		assertTextEquals(expected, sw);
	}

	//====================================================================================================
	// testShellScriptGeneration
	//====================================================================================================
	@Test
	public void testShellScriptGeneration() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "# c1", "\t# c2", " c3 ", "", " ", "x1=1", "x2=true", "x3=null")
			.addHeaderComments(null, "header null", "", null)
			.addLines("s1", "#c4", "k1=1", "#c5 foo=bar", "k2 = true", "k3  = \tnull")
			.addHeaderComments("s1", "header s1", "", null);
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		String expected = "# header null|||# c1|# c2|# c3|||export x1=\"1\"|export x2=\"true\"|export x3=\"null\"|# header s1|||# c4|export s1_k1=\"1\"|# c5 foo=bar|export s1_k2=\"true\"|export s1_k3=\"null\"|";

		StringWriter sw = new StringWriter();
		cf.serializeTo(new PrintWriter(sw), ConfigFileFormat.SHELL);
		assertTextEquals(expected, sw);

		sw = new StringWriter();
		cfw.serializeTo(new PrintWriter(sw), ConfigFileFormat.SHELL);
		assertTextEquals(expected, sw);
	}

	//====================================================================================================
	// testEncodedValues
	//====================================================================================================
	@Test
	public void testEncodedValues() throws Exception {
		File f = getFreshFile();

		ConfigFile cf = configFileBuilder.build(f)
			.addLines("s1", "", "foo* = mypassword")
			.getResolving(VarResolver.DEFAULT);
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		assertEquals("mypassword", cf.getString("s1/foo"));
		assertEquals("mypassword", cfw.getString("s1/foo"));
		assertTrue(cf.isEncoded("s1/foo"));
		assertTrue(cfw.isEncoded("s1/foo"));
		cf.save();
		String expected = "[s1]||foo* = {AwwJVhwUQFZEMg==}|";
		String actual = read(new FileReader(f));
		assertTextEquals(expected, actual);
		cf.load();
		assertEquals("mypassword", cf.getString("s1/foo"));
		assertEquals("mypassword", cfw.getString("s1/foo"));

		write(f, new StringReader("[s1]\nfoo* = mypassword2\n"));
		modifyTimestamp(f);
		cf.loadIfModified();
		assertTrue(cf.isEncoded("s1/foo"));
		assertEquals("mypassword2", cf.getString("s1/foo"));
		cf.put("s1/foo", "mypassword");
		assertTrue(cf.isEncoded("s1/foo"));

		// INI output should be encoded
		StringWriter sw = new StringWriter();
		cf.serializeTo(new PrintWriter(sw), ConfigFileFormat.INI);
		expected = "[s1]|foo* = {AwwJVhwUQFZEMg==}|";
		assertTextEquals(expected, sw);

		// BATCH output should not be encoded
		sw = new StringWriter();
		cf.serializeTo(new PrintWriter(sw), ConfigFileFormat.BATCH);
		expected = "set s1_foo = mypassword|";
		assertTextEquals(expected, sw);

		// SHELL output should not be encoded
		sw = new StringWriter();
		cf.serializeTo(new PrintWriter(sw), ConfigFileFormat.SHELL);
		expected = "export s1_foo=\"mypassword\"|";
		assertTextEquals(expected, sw);
	}

	//====================================================================================================
	// testVariables
	//====================================================================================================
	@Test
	public void testVariables() throws Exception {

		ConfigFile cf = configFileBuilder.build()
			.addLines("s1",
				"f1 = $S{foo}",
				"f2 = $S{foo,bar}",
				"f3 = $S{$S{baz,bing},bar}"
		);
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		System.getProperties().remove("foo");
		System.getProperties().remove("bar");
		System.getProperties().remove("baz");
		System.getProperties().remove("bing");

		assertEquals("$S{foo}", cf.getString("s1/f1"));
		assertEquals("$S{foo,bar}", cf.getString("s1/f2"));
		assertEquals("$S{$S{baz,bing},bar}", cf.getString("s1/f3"));
		assertEquals("", cfw.getString("s1/f1"));
		assertEquals("bar", cfw.getString("s1/f2"));
		assertEquals("bar", cfw.getString("s1/f3"));

		System.setProperty("foo", "123");
		assertEquals("$S{foo}", cf.getString("s1/f1"));
		assertEquals("$S{foo,bar}", cf.getString("s1/f2"));
		assertEquals("$S{$S{baz,bing},bar}", cf.getString("s1/f3"));
		assertEquals("123", cfw.getString("s1/f1"));
		assertEquals("123", cfw.getString("s1/f2"));
		assertEquals("bar", cfw.getString("s1/f3"));

		System.setProperty("foo", "$S{bar}");
		System.setProperty("bar", "baz");
		assertEquals("$S{foo}", cf.getString("s1/f1"));
		assertEquals("$S{foo,bar}", cf.getString("s1/f2"));
		assertEquals("$S{$S{baz,bing},bar}", cf.getString("s1/f3"));
		assertEquals("baz", cfw.getString("s1/f1"));
		assertEquals("baz", cfw.getString("s1/f2"));
		assertEquals("bar", cfw.getString("s1/f3"));

		System.setProperty("bing", "$S{foo}");
		assertEquals("$S{$S{baz,bing},bar}", cf.getString("s1/f3"));
		assertEquals("baz", cfw.getString("s1/f3"));

		System.setProperty("baz", "foo");
		System.setProperty("foo", "123");
		assertEquals("$S{$S{baz,bing},bar}", cf.getString("s1/f3"));
		assertEquals("123", cfw.getString("s1/f3"));
	}

	//====================================================================================================
	// testXorEncoder
	//====================================================================================================
	@Test
	public void testXorEncoder() throws Exception {
		testXor("foo");
		testXor("");
		testXor("123");
		testXor("€");  // 3-byte UTF-8 character
		testXor("𤭢"); // 4-byte UTF-8 character
	}

	private void testXor(String in) {
		XorEncoder e = new XorEncoder();
		String s = e.encode("", in);
		String s2 = e.decode("", s);
		assertEquals(in, s2);
	}

	//====================================================================================================
	// testMultiLines
	//====================================================================================================
	@Test
	public void testMultiLines() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines("s1",
				"f1 = x \ny \n  z"
		);
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		assertEquals("x \ny \n  z", cf.getString("s1/f1"));
		assertEquals("x \ny \n  z", cfw.getString("s1/f1"));

		StringWriter sw = new StringWriter();
		cf.serializeTo(sw);
		String expected = "[s1]|f1 = x |\ty |\t  z|";
		assertTextEquals(expected, sw);

		sw = new StringWriter();
		cfw.serializeTo(sw);
		assertTextEquals(expected, sw);
	}

	//====================================================================================================
	// testNumberShortcuts
	//====================================================================================================
	@Test
	public void testNumberShortcuts() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines("s1",
				"f1 = 1M",
				"f2 = 1K",
				"f3 = 1 M",
				"f4 = 1 K"
		);
		ConfigFile cfw = cf.getResolving(VarResolver.DEFAULT);

		assertEquals(1048576, cf.getInt("s1/f1"));
		assertEquals(1024, cf.getInt("s1/f2"));
		assertEquals(1048576, cf.getInt("s1/f3"));
		assertEquals(1024, cf.getInt("s1/f4"));

		assertEquals(1048576, cfw.getInt("s1/f1"));
		assertEquals(1024, cfw.getInt("s1/f2"));
		assertEquals(1048576, cfw.getInt("s1/f3"));
		assertEquals(1024, cfw.getInt("s1/f4"));
	}

	//====================================================================================================
	// testListeners
	//====================================================================================================
	@Test
	public void testListeners() throws Exception {
		ConfigFile[] cff = {
			configFileBuilder.build(createTempFile("ConfigFileTest.cfg")).addLines(null, "a1=1").addLines("B", "b1=1"),
			configFileBuilder.build(createTempFile("ConfigFileTest.cfg")).addLines(null, "a1=1").addLines("B", "b1=1").getResolving(VarResolver.DEFAULT)
		};

		for (ConfigFile cf : cff) {
			final Set<String> changes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			final int[] count = new int[]{0};

			cf.addListener(
				new ConfigFileListener() {
					@Override /* ConfigFileListener */
					public void onLoad(ConfigFile f) {
						super.onLoad(f);
						changes.add("<load>");
						count[0]++;
					}
					@Override /* ConfigFileListener */
					public void onSave(ConfigFile f) {
						super.onSave(f);
						changes.add("<save>");
						count[0]++;
					}
					@Override /* ConfigFileListener */
					public void onChange(ConfigFile f, Set<String> ss) {
						super.onChange(f, ss);
						for (String sss : ss)
							changes.add(sss + '=' + f.getString(sss));
						count[0]++;
					}
				}
			);

			// ConfigFile.addLines(section,lines)
			changes.clear();
			count[0] = 0;
			cf.addLines(null, "a1=3", "a3=3").addLines("B", "b1=3","b3=3");
			assertObjectEquals("['a1=3','a3=3','B/b1=3','B/b3=3']", changes);
			assertEquals(2, count[0]);

			// ConfigFile.put(key,value,encoded)
			changes.clear();
			count[0] = 0;
			cf.put("a1", "2");
			cf.put("B/b1", "2");
			cf.put("a2", "2");
			cf.put("B/b2", "2");
			cf.put("C/c1", "2");
			cf.put("C/c2", "2");
			assertObjectEquals("['a1=2','a2=2','B/b1=2','B/b2=2','C/c1=2','C/c2=2']", changes);
			assertEquals(6, count[0]);

			// put(key,value,encoded)
			changes.clear();
			count[0] = 0;
			cf.put("a4", "4", true);
			cf.put("B/b4", "4", true);
			assertObjectEquals("['a4=4','B/b4=4']", changes);
			assertEquals(2, count[0]);

			// put(key,value)
			changes.clear();
			count[0] = 0;
			cf.put("a5", "5");
			cf.put("B/b5", "5");
			assertObjectEquals("['a5=5','B/b5=5']", changes);
			assertEquals(2, count[0]);

			// put(key,value,encoded)
			changes.clear();
			count[0] = 0;
			cf.put("a6", "6", true);
			cf.put("B/b6", "6", true);
			assertObjectEquals("['a6=6','B/b6=6']", changes);
			assertEquals(2, count[0]);

			// removeString(key)
			changes.clear();
			count[0] = 0;
			cf.removeString("a6");
			cf.removeString("B/b6");
			cf.removeString("B/bx");
			cf.removeString("X/bx");
			assertObjectEquals("['a6=null','B/b6=null']", changes);
			assertEquals(2, count[0]);

			// addSection(name)
			changes.clear();
			count[0] = 0;
			cf.addSection("D");
			assertObjectEquals("[]", changes);
			assertEquals(0, count[0]);

			// setSection(name,contents)
			changes.clear();
			count[0] = 0;
			cf.setSection("E", new AMap<String,String>().append("e1", "1").append("e2", "2"));
			assertObjectEquals("['E/e1=1','E/e2=2']", changes);
			assertEquals(1, count[0]);
			cf.removeSection("E");

			// removeSection(name)
			changes.clear();
			count[0] = 0;
			cf.removeSection("B");
			assertObjectEquals("['B/b1=null','B/b2=null','B/b3=null','B/b4=null','B/b5=null']", changes);
			assertEquals(1, count[0]);

			// removeSection(name)
			changes.clear();
			count[0] = 0;
			cf.removeSection("B");
			assertObjectEquals("[]", changes);
			assertEquals(0, count[0]);

			// put(key) {
			changes.clear();
			count[0] = 0;
			Section c = cf.get("C");
			cf.put("B", c);
			assertObjectEquals("['C/c1=2','C/c2=2']", changes);
			assertEquals(1, count[0]);

			// remove(key)
			cf.remove("B");
			cf.addLines("B", "b1=1","b2=2");
			changes.clear();
			count[0] = 0;
			cf.remove("B");
			assertObjectEquals("['B/b1=null','B/b2=null']", changes);
			assertEquals(1, count[0]);

			// putAll(map)
			ConfigFile cf2 = configFileBuilder.build();
			cf2.addLines("D", "d1=1","d2=1").addLines("E", "e1=1","e2=2");
			changes.clear();
			count[0] = 0;
			cf.putAll(cf2);
			assertObjectEquals("['a1=null','a2=null','a3=null','a4=null','a5=null','D/d1=1','D/d2=1','E/e1=1','E/e2=2']", changes);
			assertEquals(1, count[0]);

			// clear()
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			cf.clear();
			assertObjectEquals("['a1=null','a2=null','B/b1=null','B/b2=null']", changes);
			assertEquals(1, count[0]);

			// entrySet()
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			for (Iterator<Map.Entry<String,Section>> i = cf.entrySet().iterator(); i.hasNext();) {
				i.next();
				i.remove();
			}
			assertObjectEquals("['a1=null','a2=null','B/b1=null','B/b2=null']", changes);
			assertEquals(2, count[0]);

			// keySet()
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			for (Iterator<String> i = cf.keySet().iterator(); i.hasNext();) {
				i.next();
				i.remove();
			}
			assertObjectEquals("['a1=null','a2=null','B/b1=null','B/b2=null']", changes);
			assertEquals(2, count[0]);

			// values()
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			for (Iterator<Section> i = cf.values().iterator(); i.hasNext();) {
				i.next();
				i.remove();
			}
			assertObjectEquals("['a1=null','a2=null','B/b1=null','B/b2=null']", changes);
			assertEquals(2, count[0]);

			// ConfigFile.merge()
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1", "a3=1").addLines("B", "b1=1", "b2=1", "b3=1").addLines("C", "c1=1", "c2=1", "c3=1");
			cf2.clear();
			cf2.addLines(null, "a2=1", "a3=2", "a4=2").addLines("B", "b2=1", "b3=2", "b4=2").addLines("D", "d2=1", "d3=2", "d4=2");
			changes.clear();
			count[0] = 0;
			cf.merge(cf2);
			assertObjectEquals("['a1=null','a3=2','a4=2','B/b1=null','B/b3=2','B/b4=2','C/c1=null','C/c2=null','C/c3=null','D/d2=1','D/d3=2','D/d4=2']", changes);
			assertEquals(1, count[0]);

			// ConfigFile.save()
			// ConfigFile.load()
			changes.clear();
			count[0] = 0;
			cf.save();
			cf.load();
			assertObjectEquals("['<load>','<save>']", changes);
			assertEquals(2, count[0]);

			// Section.clear()
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			cf.get("default").clear();
			cf.get("B").clear();
			assertObjectEquals("['a1=null','a2=null','B/b1=null','B/b2=null']", changes);
			assertEquals(2, count[0]);

			// Section.put(key,value)
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			cf.get("default").put("a1", "2");
			cf.get("default").put("a3", "2");
			cf.get("B").put("b1", "2");
			cf.get("B").put("b3", "2");
			assertObjectEquals("['a1=2','a3=2','B/b1=2','B/b3=2']", changes);
			assertEquals(4, count[0]);

			// Section put(key,value,encoded)
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			cf.get("default").put("a1", "2", true);
			cf.get("default").put("a3", "2", true);
			cf.get("B").put("b1", "2", true);
			cf.get("B").put("b3", "2", true);
			assertObjectEquals("['a1=2','a3=2','B/b1=2','B/b3=2']", changes);
			assertEquals(4, count[0]);

			// Section.putAll(map)
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			cf.get("default").putAll(new AMap<String,String>().append("a1","1").append("a2","2").append("a3","2"));
			cf.get("B").putAll(new AMap<String,String>().append("b1","1").append("b2","2").append("b3","2"));
			assertObjectEquals("['a2=2','a3=2','B/b2=2','B/b3=2']", changes);
			assertEquals(2, count[0]);

			// Section.remove(key)
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			cf.get("default").remove("a1");
			cf.get("default").remove("ax");
			cf.get("B").remove("b1");
			cf.get("B").remove("bx");
			assertObjectEquals("['a1=null','B/b1=null']", changes);
			assertEquals(2, count[0]);

			// Section.entrySet()
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			Section n = cf.get("default");
			for (Iterator<Map.Entry<String,String>> i = n.entrySet().iterator(); i.hasNext();) {
				i.next();
				i.remove();
			}
			n = cf.get("B");
			for (Iterator<Map.Entry<String,String>> i = n.entrySet().iterator(); i.hasNext();) {
				i.next();
				i.remove();
			}
			assertObjectEquals("['a1=null','a2=null','B/b1=null','B/b2=null']", changes);
			assertEquals(4, count[0]);

			// Section.keySet()
			cf.clear();
			cf.addLines("default", "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			changes.clear();
			count[0] = 0;
			for (Iterator<String> i = cf.get("default").keySet().iterator(); i.hasNext();) {
				i.next();
				i.remove();
			}
			for (Iterator<String> i = cf.get("B").keySet().iterator(); i.hasNext();) {
				i.next();
				i.remove();
			}
			assertObjectEquals("['a1=null','a2=null','B/b1=null','B/b2=null']", changes);
			assertEquals(4, count[0]);

			// Section.values()
			cf.clear();
			cf.addLines(null, "a1=1", "a2=1").addLines("B", "b1=1", "b2=1");
			try {
				Iterator<String> i = cf.get("default").values().iterator();
				i.next();
				i.remove();
				fail("Exception expected");
			} catch (UnsupportedOperationException e) {}
			try {
				Iterator<String> i = cf.get("B").values().iterator();
				i.next();
				i.remove();
				fail("Exception expected");
			} catch (UnsupportedOperationException e) {}
		}
	}

	//====================================================================================================
	// testEntryListener
	//====================================================================================================
	@Test
	public void testEntryListener() throws Exception {
		ConfigFile[] cff = {
			configFileBuilder.build().addLines(null, "a1=1").addLines("B", "b1=1"),
			configFileBuilder.build().addLines(null, "a1=1").addLines("B", "b1=1").getResolving(VarResolver.DEFAULT)
		};

		for (ConfigFile cf : cff) {
			final Set<String> changes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			final int[] count = new int[]{0};

			cf.addListener(
				new EntryListener("a1") {
					@Override /* EntryListener */
					public void onChange(ConfigFile f) {
						super.onChange(f);
						changes.add("a1=" + f.getString("a1"));
						count[0]++;
					}
				}
			);

			cf.addListener(
				new EntryListener("B/b1") {
					@Override /* EntryListener */
					public void onChange(ConfigFile f) {
						changes.add("B/b1=" + f.getString("B/b1"));
						count[0]++;
					}
				}
			);

			cf.put("a1", "2");
			cf.put("a2", "2");
			cf.put("B/b1", "2");
			cf.put("B/b2", "2");
			assertObjectEquals("['a1=2','B/b1=2']", changes);
			assertEquals(2, count[0]);
		}
	}

	//====================================================================================================
	// testSectionListener
	//====================================================================================================
	@Test
	public void testSectionListener() throws Exception {
		ConfigFile[] cff = {
			configFileBuilder.build().addLines(null, "a1=1").addLines("B", "b1=1"),
			configFileBuilder.build().addLines(null, "a1=1").addLines("B", "b1=1").getResolving(VarResolver.DEFAULT)
		};

		for (ConfigFile cf : cff) {

			final Set<String> changes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			final int[] count = new int[]{0};

			cf.addListener(
				new SectionListener(null) {
					@Override /* SectionListener */
					public void onChange(ConfigFile f) {
						super.onChange(f);
						changes.add("x=" + f.getString("a1"));
						count[0]++;
					}
				}
			);

			cf.addListener(
				new SectionListener("") {
					@Override /* SectionListener */
					public void onChange(ConfigFile f) {
						changes.add("y=" + f.getString("a1"));
						count[0]++;
					}
				}
			);

			cf.addListener(
				new SectionListener("B") {
					@Override /* SectionListener */
					public void onChange(ConfigFile f) {
						changes.add("z=" + f.getString("B/b1"));
						count[0]++;
					}
				}
			);

			cf.put("a1", "2");
			cf.put("a2", "2");
			cf.put("B/b1", "2");
			cf.put("B/b2", "2");
			assertObjectEquals("['x=2','y=2','z=2']", changes);
			assertEquals(6, count[0]);
		}
	}

	//====================================================================================================
	// testMerge
	//====================================================================================================
	@Test
	public void testMerge() throws Exception {
		ConfigFile cf1 = configFileBuilder.build()
			.addLines(null, "# comment a1", "a1=1")
			.addLines("B", "# comment b1", "b1=1").addHeaderComments("B", "# comment B1")
			.addLines("C", "# comment c1", "bc=1").addHeaderComments("C", "# comment C1");
		ConfigFile cf2 = configFileBuilder.build()
			.addLines(null, "# comment a2", "a2=2")
			.addLines("B", "# comment b2", "b2=2").addHeaderComments("B", "# comment B2")
			.addLines("D", "# comment d2", "d2=2").addHeaderComments("D", "# comment D2");
		cf1.merge(cf2);

		String expected = "# comment a2|a2 = 2|# comment B2|[B]|# comment b2|b2 = 2|# comment D2|[D]|# comment d2|d2 = 2|";
		assertTextEquals(expected, cf1);

		cf1 = configFileBuilder.build()
			.addLines(null, "# comment a1", "a1=1")
			.addLines("B", "# comment b1", "b1=1").addHeaderComments("B", "# comment B1")
			.addLines("C", "# comment c1", "bc=1").addHeaderComments("C", "# comment C1").getResolving(VarResolver.DEFAULT);
		cf2 = configFileBuilder.build()
			.addLines(null, "# comment a2", "a2=2")
			.addLines("B", "# comment b2", "b2=2").addHeaderComments("B", "# comment B2")
			.addLines("D", "# comment d2", "d2=2").addHeaderComments("D", "# comment D2").getResolving(VarResolver.DEFAULT);

		cf1.merge(cf2);
		assertTextEquals(expected, cf1);
	}

	//====================================================================================================
	// testDefaultSection
	// Default section should be keyed by either null, "", or "default"
	//====================================================================================================
	@Test
	public void testDefaultSection() throws Exception {
		ConfigFile[] cff = {
			configFileBuilder.build().addLines(null, "a1=1").addLines("", "a2=2").addLines("default", "a3=3"),
			configFileBuilder.build().addLines(null, "a1=1").addLines("", "a2=2").addLines("default", "a3=3").getResolving(VarResolver.DEFAULT)
		};

		for (ConfigFile cf : cff) {
			assertObjectEquals("{'default':{a1:'1',a2:'2',a3:'3'}}", cf);
			assertTextEquals("a1 = 1|a2 = 2|a3 = 3|", cf);

			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.get(null));
			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.get(""));
			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.get("default"));

			assertEquals("1", cf.getString("a1"));
			assertEquals("1", cf.getString("default/a1"));

			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.getSectionMap(null));
			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.getSectionMap(""));
			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.getSectionMap("default"));

			cf.put(null, cf.get(null));
			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.get(null));
			cf.put("", cf.get(null));
			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.get(null));
			cf.put("default", cf.get(null));
			assertObjectEquals("{a1:'1',a2:'2',a3:'3'}", cf.get(null));
		}
	}

	//====================================================================================================
	// ConfigFileImpl(File)
	// ConfigFileImpl()
	//====================================================================================================
	@Test
	public void testAlternateConstructors() throws Exception {
		ConfigFile cf = new ConfigFileImpl();
		cf.put("A", "a");
		try { cf.save(); fail(); } catch (UnsupportedOperationException e) {}
		cf.loadIfModified();
		assertEquals("a", cf.getString("A"));
	}

	//====================================================================================================
	// containsKey(Object)
	// containsValue(Object)
	// size()
	//====================================================================================================
	@Test
	public void testContains() throws Exception {
		ConfigFile cf = configFileBuilder.build().addLines(null, "a1=1").addLines("", "a2=2").addLines("default", "a3=3").addLines("A", "a4=4");
		ConfigFile cfw = cf.getResolving();

		assertTrue(cf.containsKey(null));
		assertTrue(cf.containsKey(""));
		assertTrue(cf.containsKey("default"));
		assertTrue(cf.containsKey("A"));
		assertFalse(cf.containsKey("x"));

		assertTrue(cfw.containsKey(null));
		assertTrue(cfw.containsKey(""));
		assertTrue(cfw.containsKey("default"));
		assertTrue(cfw.containsKey("A"));
		assertFalse(cfw.containsKey("x"));

		Section s = cf.get(null);
		assertTrue(cf.containsValue(s));
		assertTrue(cfw.containsValue(s));
		s = cf.get("A");
		assertTrue(cf.containsValue(s));
		assertTrue(cfw.containsValue(s));

		assertFalse(cf.isEmpty());
		assertFalse(cfw.isEmpty());

		cf.clear();
		assertTrue(cf.isEmpty());
		assertTrue(cfw.isEmpty());
		assertFalse(cf.containsKey(null));
		assertFalse(cfw.containsKey(null));
		assertEquals(0, cf.size());
		assertEquals(0, cfw.size());
		assertEquals(0, cf.keySet().size());
		assertEquals(0, cfw.keySet().size());
		assertEquals(0, cf.entrySet().size());
		assertEquals(0, cfw.entrySet().size());
		assertEquals(0, cf.values().size());
		assertEquals(0, cfw.values().size());
	}

	//====================================================================================================
	// getObjectArray(Class c, String key)
	// getObjectArray(Class c, String key, T[] def)
	//====================================================================================================
	@Test
	public void testGetObjectArray() throws Exception {
		ConfigFile cf = configFileBuilder.build().addLines("A", "a1=[1,2,3]");
		ConfigFile cfw = cf.getResolving();
		assertObjectEquals("[1,2,3]", cf.getObject("A/a1", Integer[].class));
		assertObjectEquals("[1,2,3]", cfw.getObject("A/a1", Integer[].class));
		assertObjectEquals("[4,5,6]", cf.getObjectWithDefault("A/a2", new Integer[]{4,5,6}, Integer[].class));
		assertObjectEquals("[4,5,6]", cfw.getObjectWithDefault("A/a2", new Integer[]{4,5,6}, Integer[].class));
		assertObjectEquals("[7,8,9]", cf.getObjectWithDefault("B/a1", new Integer[]{7,8,9}, Integer[].class));
		assertObjectEquals("[7,8,9]", cfw.getObjectWithDefault("B/a1", new Integer[]{7,8,9}, Integer[].class));
		assertNull(cf.getObject("B/a1", Integer[].class));
		assertNull(cfw.getObject("B/a1", Integer[].class));

		cf = configFileBuilder.build().addLines("A", "a1 = [1 ,\n\t2 ,\n\t3] ");
		assertObjectEquals("[1,2,3]", cf.getObject("A/a1", Integer[].class));
		assertObjectEquals("[1,2,3]", cfw.getObject("A/a1", Integer[].class));

		// We cannot cast primitive arrays to Object[], so the following throws exceptions.
		assertObjectEquals("[1,2,3]", cf.getObject("A/a1", int[].class));
		assertEquals("int", cf.getObject("A/a1", int[].class).getClass().getComponentType().getSimpleName());
		assertNull(cf.getObject("B/a1", int[].class));
		assertEquals("int", cf.getObjectWithDefault("B/a1", new int[0], int[].class).getClass().getComponentType().getSimpleName());
		assertNull(cf.getObject("A/a2", int[].class));
		assertEquals("int", cf.getObjectWithDefault("A/a2", new int[0], int[].class).getClass().getComponentType().getSimpleName());

		assertObjectEquals("[1,2,3]", cf.getObjectWithDefault("A/a1", new int[]{4}, int[].class));
		assertEquals("int", cf.getObjectWithDefault("A/a1", new int[]{4}, int[].class).getClass().getComponentType().getSimpleName());
		assertObjectEquals("[4]", cf.getObjectWithDefault("B/a1", new int[]{4}, int[].class));
		assertEquals("int", cf.getObjectWithDefault("B/a1", new int[]{4}, int[].class).getClass().getComponentType().getSimpleName());
		assertObjectEquals("[4]", cf.getObjectWithDefault("A/a2", new int[]{4}, int[].class));
		assertEquals("int", cf.getObjectWithDefault("A/a2", new int[]{4}, int[].class).getClass().getComponentType().getSimpleName());

		System.setProperty("X", "[4,5,6]");
		cf = configFileBuilder.build().addLines(null, "x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}").addLines("A", "a1=[1,2,3]").getResolving();
		assertObjectEquals("[1,2,3]", cf.getObjectWithDefault("x1", new int[]{9}, int[].class));
		assertObjectEquals("[4,5,6]", cf.getObjectWithDefault("x2", new int[]{9}, int[].class));
		assertObjectEquals("[9]", cf.getObjectWithDefault("x3", new int[]{9}, int[].class));
		System.clearProperty("X");
	}

	//====================================================================================================
	// getStringArray(String key)
	// getStringArray(String key, String[] def)
	//====================================================================================================
	@Test
	public void testGetStringArray() throws Exception {
		ConfigFile cf = configFileBuilder.build().addLines("A", "a1=1,2,3");
		ConfigFile cfw = cf.getResolving();
		assertObjectEquals("['1','2','3']", cf.getStringArray("A/a1"));
		assertObjectEquals("['1','2','3']", cfw.getStringArray("A/a1"));
		assertObjectEquals("['4','5','6']", cf.getStringArray("A/a2", new String[]{"4","5","6"}));
		assertObjectEquals("['4','5','6']", cfw.getStringArray("A/a2", new String[]{"4","5","6"}));
		assertObjectEquals("['7','8','9']", cf.getStringArray("B/a1", new String[]{"7","8","9"}));
		assertObjectEquals("['7','8','9']", cfw.getStringArray("B/a1", new String[]{"7","8","9"}));
		assertObjectEquals("[]", cf.getStringArray("B/a1"));
		assertObjectEquals("[]", cfw.getStringArray("B/a1"));

		cf = configFileBuilder.build().addLines("A", "a1 = 1 ,\n\t2 ,\n\t3 ");
		assertObjectEquals("['1','2','3']", cf.getStringArray("A/a1"));
		assertObjectEquals("['1','2','3']", cfw.getStringArray("A/a1"));

		System.setProperty("X", "4,5,6");
		cf = configFileBuilder.build().addLines(null, "x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "x4=$S{Y,$S{X}}").addLines("A", "a1=1,2,3").getResolving();
		assertObjectEquals("['1','2','3']", cf.getStringArray("x1", new String[]{"9"}));
		assertObjectEquals("['4','5','6']", cf.getStringArray("x2", new String[]{"9"}));
		assertObjectEquals("['9']", cf.getStringArray("x3", new String[]{"9"}));

		// TODO - Doesn't work yet.
		// assertObjectEquals("['4','5','6']", cf.getStringArray("x4", new String[]{"9"}));
		System.clearProperty("X");
	}

	//====================================================================================================
	// getSectionMap(String name)
	//====================================================================================================
	@Test
	public void testGetSectionMap() throws Exception {
		ConfigFile cf = configFileBuilder.build().addLines("A", "a1=1", "").addLines("D", "d1=$C{A/a1}","d2=$S{X}");

		assertObjectEquals("{a1:'1'}", cf.getSectionMap("A"));
		assertNull(cf.getSectionMap("B"));
		assertObjectEquals("null", cf.getSectionMap("C"));

		ObjectMap m = cf.getSectionMap("A");
		assertObjectEquals("{a1:'1'}", m);

		m = cf.getSectionMap("D");
		assertObjectEquals("{d1:'$C{A/a1}',d2:'$S{X}'}", m);

		cf = cf.getResolving();

		System.setProperty("X", "x");
		m = cf.getSectionMap("D");
		assertObjectEquals("{d1:'1',d2:'x'}", m);
		System.clearProperty("X");
	}

	//====================================================================================================
	// load(Reader)
	//====================================================================================================
	@Test
	public void testLoadFromReader() throws Exception {
		ConfigFile[] cff = {
			configFileBuilder.build().addLines(null, "a1=1"),
			configFileBuilder.build().addLines(null, "a1=1").getResolving(VarResolver.DEFAULT)
		};

		for (ConfigFile cf : cff) {
			cf.load(new StringReader("[B]\nb1=1"));
			assertObjectEquals("{'default':{},B:{b1:'1'}}", cf);
		}
	}


	//====================================================================================================
	// toWritable()
	//====================================================================================================
	@Test
	public void testToWritable() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "a=b");
		ConfigFile cfw = cf.getResolving();

		StringWriter sw = new StringWriter();
		cf.toWritable().writeTo(sw);
		assertTextEquals("a = b|", sw);

		sw = new StringWriter();
		cfw.toWritable().writeTo(sw);
		assertTextEquals("a = b|", sw);

		assertEquals("text/plain", cf.toWritable().getMediaType().toString());
		assertEquals("text/plain", cfw.toWritable().getMediaType().toString());
	}

	//====================================================================================================
	// containsNonEmptyKey()
	//====================================================================================================
	@Test
	public void testContainsNonEmptyKey() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "a=b","c=");
		ConfigFile cfw = cf.getResolving();

		assertTrue(cf.containsNonEmptyValue("a"));
		assertFalse(cf.containsNonEmptyValue("c"));
		assertFalse(cf.containsNonEmptyValue("d"));

		assertTrue(cfw.containsNonEmptyValue("a"));
		assertFalse(cfw.containsNonEmptyValue("c"));
		assertFalse(cfw.containsNonEmptyValue("d"));

		cf.addLines("A", "a1=$S{X}", "a2=$S{Y,$S{X}}");
		assertFalse(cfw.containsNonEmptyValue("A/a1"));
		assertFalse(cfw.containsNonEmptyValue("A/a2"));

		System.setProperty("X", "x");
		assertTrue(cfw.containsNonEmptyValue("A/a1"));
		assertTrue(cfw.containsNonEmptyValue("A/a2"));
		System.clearProperty("X");
	}

	//====================================================================================================
	// getSectionKeys(String sectionName)
	//====================================================================================================
	@Test
	public void testGetSectionKeys() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "x1=1", "x2=")
			.addLines("A", "a1=1", "a2=");

		assertObjectEquals("['x1','x2']", cf.getSectionKeys(null));
		assertObjectEquals("['x1','x2']", cf.getSectionKeys(""));
		assertObjectEquals("['x1','x2']", cf.getSectionKeys("default"));
		assertObjectEquals("['a1','a2']", cf.getSectionKeys("A"));
		assertNull(cf.getSectionKeys("B"));

		cf = cf.getResolving();

		assertObjectEquals("['x1','x2']", cf.getSectionKeys(null));
		assertObjectEquals("['x1','x2']", cf.getSectionKeys(""));
		assertObjectEquals("['x1','x2']", cf.getSectionKeys("default"));
		assertObjectEquals("['a1','a2']", cf.getSectionKeys("A"));
		assertNull(cf.getSectionKeys("B"));
	}

	//====================================================================================================
	// addLines(String section, String...lines)
	//====================================================================================================
	@Test
	public void testAddLines() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1");

		cf.addLines(null, "# comment1", "x1=2", "x2=1", "foobar");
		cf.addLines("A", "# comment2", "a1=2", "a2=1", "foobar");
		cf.addLines("B", "# comment3", "b1=2", "b2=1", "foobar");
		cf.addLines("C", (String[])null);  // Should be OK.
		cf.addLines("C", (String)null);  // Should be OK.
		assertObjectEquals("{'default':{x1:'2',x2:'1'},A:{a1:'2',a2:'1'},B:{b1:'2',b2:'1'},C:{}}", cf);
		assertTextEquals("# comment1|x1 = 2|x2 = 1|foobar|[A]|# comment2|a1 = 2|a2 = 1|foobar|[B]|# comment3|b1 = 2|b2 = 1|foobar|[C]||", cf);

		cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1")
			.getResolving();

		cf.addLines(null, "# comment1", "x1=2", "x2=1", "foobar");
		cf.addLines("A", "# comment2", "a1=2", "a2=1", "foobar");
		cf.addLines("B", "# comment3", "b1=2", "b2=1", "foobar");
		cf.addLines("C", (String[])null);  // Should be OK.
		cf.addLines("C", (String)null);  // Should be OK.
		assertObjectEquals("{'default':{x1:'2',x2:'1'},A:{a1:'2',a2:'1'},B:{b1:'2',b2:'1'},C:{}}", cf);
		assertTextEquals("# comment1|x1 = 2|x2 = 1|foobar|[A]|# comment2|a1 = 2|a2 = 1|foobar|[B]|# comment3|b1 = 2|b2 = 1|foobar|[C]||", cf);

		cf = configFileBuilder.build()
			.addLines(null, "x1=$C{A/a2}")
			.addLines("A", "a1=1")
			.getResolving();
		assertObjectEquals("{'default':{x1:'$C{A/a2}'},A:{a1:'1'}}", cf);
		assertTextEquals("x1 = $C{A/a2}|[A]|a1 = 1|", cf);

		assertEquals("", cf.getString("x1"));

		cf.addLines("A", "a2=2");

		assertEquals("2", cf.getString("x1"));
	}


	//====================================================================================================
	// addHeaderComments(String section, String...headerComments)
	// clearHeaderComments(String section)
	//====================================================================================================
	@Test
	public void testAddHeaderComments() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1");

		cf.addHeaderComments(null, "# h1");
		cf.addHeaderComments("", "# h2");
		cf.addHeaderComments("default", "# h3");
		cf.addHeaderComments("A", "# h4");
		cf.addHeaderComments("B", "# h5");
		cf.addHeaderComments("C", (String[])null);
		cf.addHeaderComments("C", (String)null);

		assertTextEquals("# h1|# h2|# h3|x1 = 1|# h4|[A]|a1 = 1|# h5|[B]|#|[C]|", cf);

		cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1");

		cf.addHeaderComments(null, "h1");
		cf.addHeaderComments("", "h2");
		cf.addHeaderComments("default", "h3");
		cf.addHeaderComments("A", "h4");
		cf.addHeaderComments("B", "h5");
		cf.addHeaderComments("C", (String[])null);
		cf.addHeaderComments("C", (String)null);

		assertTextEquals("#h1|#h2|#h3|x1 = 1|#h4|[A]|a1 = 1|#h5|[B]|#|[C]|", cf);

		cf.clearHeaderComments(null).clearHeaderComments("A").clearHeaderComments("B").clearHeaderComments("C");
		assertTextEquals("x1 = 1|[A]|a1 = 1|[B]|[C]|", cf);

		cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1")
			.getResolving();

		cf.addHeaderComments(null, "h1");
		cf.addHeaderComments("", "h2");
		cf.addHeaderComments("default", "h3");
		cf.addHeaderComments("A", "h4");
		cf.addHeaderComments("B", "h5");
		cf.addHeaderComments("C", (String[])null);
		cf.addHeaderComments("C", (String)null);

		assertTextEquals("#h1|#h2|#h3|x1 = 1|#h4|[A]|a1 = 1|#h5|[B]|#|[C]|", cf);

		cf.clearHeaderComments(null).clearHeaderComments("A").clearHeaderComments("B").clearHeaderComments("C");
		assertTextEquals("x1 = 1|[A]|a1 = 1|[B]|[C]|", cf);
	}

	//====================================================================================================
	// getString(String key)
	// getString(String key, String def)
	//====================================================================================================
	@Test
	public void testGetString() throws Exception {
		System.setProperty("S1", "1");
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "x1=1", "x2=$C{A/a2}", "x3=$S{S1,2}", "x4=$S{S2,3}")
			.addLines("A", "a1=1", "a2=$C{A/a1}", "a3=$S{S1,2}", "a4=$S{S2,3}");

		assertEquals("1", cf.getString("x1"));
		assertEquals("1", cf.getString("x1", "x"));
		assertEquals("$C{A/a2}", cf.getString("x2"));
		assertEquals("$C{A/a2}", cf.getString("x2", "x"));
		assertEquals("$S{S1,2}", cf.getString("x3"));
		assertEquals("$S{S1,2}", cf.getString("x3", "x"));
		assertEquals("$S{S2,3}", cf.getString("x4"));
		assertEquals("$S{S2,3}", cf.getString("x4", "x"));
		assertNull(cf.getString("x5"));
		assertEquals("x", cf.getString("x5", "x"));

		assertEquals("1", cf.getString("A/a1"));
		assertEquals("1", cf.getString("A/a1", "x"));
		assertEquals("$C{A/a1}", cf.getString("A/a2"));
		assertEquals("$C{A/a1}", cf.getString("A/a2", "x"));
		assertEquals("$S{S1,2}", cf.getString("A/a3"));
		assertEquals("$S{S1,2}", cf.getString("A/a3", "x"));
		assertEquals("$S{S2,3}", cf.getString("A/a4"));
		assertEquals("$S{S2,3}", cf.getString("A/a4", "x"));
		assertNull(cf.getString("A/a5"));
		assertEquals("x", cf.getString("A/a5", "x"));

		assertNull(cf.getString("B/b1"));
		assertEquals("x", cf.getString("B/b1", "x"));

		cf = cf.getResolving();

		assertEquals("1", cf.getString("x1"));
		assertEquals("1", cf.getString("x1", "x"));
		assertEquals("1", cf.getString("x2"));
		assertEquals("1", cf.getString("x2", "x"));
		assertEquals("1", cf.getString("x3"));
		assertEquals("1", cf.getString("x3", "x"));
		assertEquals("3", cf.getString("x4"));
		assertEquals("3", cf.getString("x4", "x"));
		assertNull(cf.getString("x5"));
		assertEquals("x", cf.getString("x5", "x"));

		assertEquals("1", cf.getString("A/a1"));
		assertEquals("1", cf.getString("A/a1", "x"));
		assertEquals("1", cf.getString("A/a2"));
		assertEquals("1", cf.getString("A/a2", "x"));
		assertEquals("1", cf.getString("A/a3"));
		assertEquals("1", cf.getString("A/a3", "x"));
		assertEquals("3", cf.getString("A/a4"));
		assertEquals("3", cf.getString("A/a4", "x"));
		assertNull(cf.getString("A/a5"));
		assertEquals("x", cf.getString("A/a5", "x"));

		assertNull(cf.getString("B/b1"));
		assertEquals("x", cf.getString("B/b1", "x"));

		System.clearProperty("S1");
	}

	//====================================================================================================
	// put(String key, Object value)
	//====================================================================================================
	@Test
	public void testPutString() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1");

		cf.put("x1", 2);
		cf.put("x2", 3);
		cf.put("A/a1", 2);
		cf.put("A/a2", 3);
		cf.put("B/b1", 2);

		assertObjectEquals("{'default':{x1:'2',x2:'3'},A:{a1:'2',a2:'3'},B:{b1:'2'}}", cf);
		assertTextEquals("x1 = 2|x2 = 3|[A]|a1 = 2|a2 = 3|[B]|b1 = 2|", cf);

		cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1")
			.getResolving();

		cf.put("x1", 2);
		cf.put("x2", 3);
		cf.put("A/a1", 2);
		cf.put("A/a2", 3);
		cf.put("B/b1", 2);

		assertObjectEquals("{'default':{x1:'2',x2:'3'},A:{a1:'2',a2:'3'},B:{b1:'2'}}", cf);
		assertTextEquals("x1 = 2|x2 = 3|[A]|a1 = 2|a2 = 3|[B]|b1 = 2|", cf);

		cf.put("x1", 9);
		cf.put("x2", "$C{x1}");
		cf.put("A/a1", "$C{x1}");
		cf.put("A/a2", "$C{x1}");
		cf.put("B/b1", "$C{x1}");

		assertObjectEquals("{'default':{x1:'9',x2:'$C{x1}'},A:{a1:'$C{x1}',a2:'$C{x1}'},B:{b1:'$C{x1}'}}", cf);
		assertTextEquals("x1 = 9|x2 = $C{x1}|[A]|a1 = $C{x1}|a2 = $C{x1}|[B]|b1 = $C{x1}|", cf);

		assertEquals("9", cf.getString("x1"));
		assertEquals("9", cf.getString("x2"));
		assertEquals("9", cf.getString("A/a1"));
		assertEquals("9", cf.getString("A/a2"));
		assertEquals("9", cf.getString("B/b1"));
	}

	//====================================================================================================
	// put(String key, Object value, boolean encoded)
	//====================================================================================================
	@Test
	public void testPutStringEncoded() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1");

		cf.put("x1", 2, true);
		cf.put("x2", 3, true);
		cf.put("A/a1", 2, true);
		cf.put("A/a2", 3, true);
		cf.put("B/b1", 2, true);

		assertObjectEquals("{'default':{x1:'2',x2:'3'},A:{a1:'2',a2:'3'},B:{b1:'2'}}", cf);
		assertTextEquals("x1* = {XA==}|x2* = {XQ==}|[A]|a1* = {XA==}|a2* = {XQ==}|[B]|b1* = {XA==}|", cf);

		cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1")
			.getResolving();

		cf.put("x1", 2, true);
		cf.put("x2", 3, true);
		cf.put("A/a1", 2, true);
		cf.put("A/a2", 3, true);
		cf.put("B/b1", 2, true);

		assertObjectEquals("{'default':{x1:'2',x2:'3'},A:{a1:'2',a2:'3'},B:{b1:'2'}}", cf);
		assertTextEquals("x1* = {XA==}|x2* = {XQ==}|[A]|a1* = {XA==}|a2* = {XQ==}|[B]|b1* = {XA==}|", cf);

		cf.put("x1", 9, true);
		cf.put("x2", "$C{x1}", true);
		cf.put("A/a1", "$C{x1}", true);
		cf.put("A/a2", "$C{x1}", true);
		cf.put("B/b1", "$C{x1}", true);

		assertObjectEquals("{'default':{x1:'9',x2:'$C{x1}'},A:{a1:'$C{x1}',a2:'$C{x1}'},B:{b1:'$C{x1}'}}", cf);
		assertTextEquals("x1* = {Vw==}|x2* = {SjYCT14a}|[A]|a1* = {SjYCT14a}|a2* = {SjYCT14a}|[B]|b1* = {SjYCT14a}|", cf);

		assertEquals("9", cf.getString("x1"));
		assertEquals("9", cf.getString("x2"));
		assertEquals("9", cf.getString("A/a1"));
		assertEquals("9", cf.getString("A/a2"));
		assertEquals("9", cf.getString("B/b1"));
	}

	//====================================================================================================
	// removeString(String key)
	//====================================================================================================
	@Test
	public void testRemoveString() throws Exception {
		ConfigFile cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1");

		cf.removeString("x1");
		cf.removeString("x2");
		cf.removeString("A/a1");
		cf.removeString("A/a2");

		assertObjectEquals("{'default':{},A:{}}", cf);
		assertTextEquals("[A]|", cf);

		cf = configFileBuilder.build()
			.addLines(null, "x1=1")
			.addLines("A", "a1=1")
			.getResolving();

		cf.removeString("x1");
		cf.removeString("x2");
		cf.removeString("A/a1");
		cf.removeString("A/a2");

		assertObjectEquals("{'default':{},A:{}}", cf);
		assertTextEquals("[A]|", cf);
	}

	//====================================================================================================
	// getObject(Class c, String key)
	// getObject(Class c, String key, T def)
	//====================================================================================================
	@Test
	public void testGetObject() throws Exception {
		ConfigFile cf = configFileBuilder.build().addLines("A", "a1=[1,2,3]", "a2=1", "a3=true", "a4=1.2", "a5=[1.2,3.4]");
		ConfigFile cfw = cf.getResolving();

		assertObjectEquals("['1','2','3']", cf.getObject("A/a1", String[].class));
		assertObjectEquals("'[1,2,3]'", cf.getObject("A/a1", String.class));
		assertObjectEquals("'foobar'", cf.getObjectWithDefault("X/a1", "foobar", String.class));
		assertObjectEquals("1", cf.getObject("A/a2", int.class));
		assertObjectEquals("1", cf.getObject("A/a2", Integer.class));
		assertObjectEquals("true", cf.getObject("A/a3", boolean.class));
		assertObjectEquals("true", cf.getObject("A/a3", Boolean.class));
		assertObjectEquals("1.2", cf.getObject("A/a4", Float.class));
		assertObjectEquals("[1.2,3.4]", cf.getObject("A/a5", Float[].class));
		assertObjectEquals("1.2", cf.getObject("A/a4", float.class));
		assertObjectEquals("[1.2,3.4]", cf.getObject("A/a5", float[].class));
		assertNull(cf.getObject("B/a4", String.class));

		assertObjectEquals("['1','2','3']", cfw.getObject("A/a1", String[].class));
		assertObjectEquals("'[1,2,3]'", cfw.getObject("A/a1", String.class));
		assertObjectEquals("'foobar'", cfw.getObjectWithDefault("X/a1", "foobar", String.class));
		assertObjectEquals("1", cfw.getObject("A/a2", int.class));
		assertObjectEquals("1", cfw.getObject("A/a2", Integer.class));
		assertObjectEquals("true", cfw.getObject("A/a3", boolean.class));
		assertObjectEquals("true", cfw.getObject("A/a3", Boolean.class));
		assertObjectEquals("1.2", cfw.getObject("A/a4", Float.class));
		assertObjectEquals("[1.2,3.4]", cfw.getObject("A/a5", Float[].class));
		assertObjectEquals("1.2", cfw.getObject("A/a4", float.class));
		assertObjectEquals("[1.2,3.4]", cfw.getObject("A/a5", float[].class));
		assertNull(cfw.getObject("B/a4", String.class));
	}

	//====================================================================================================
	// getInt(String key)
	// getInt(String key, int def)
	//====================================================================================================
	@Test
	public void testGetInt() throws Exception {
		System.setProperty("X", "1");
		ConfigFile cf = configFileBuilder.build().addLines(null, "x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "x4=$S{Y,2}").addLines("A", "a1=1");

		try {
			cf.getInt("x1");
			fail();
		} catch (NumberFormatException e) {}
		try {
			cf.getInt("x2");
			fail();
		} catch (NumberFormatException e) {}
		try {
			cf.getInt("x3");
			fail();
		} catch (NumberFormatException e) {}
		try {
			cf.getInt("x4");
			fail();
		} catch (NumberFormatException e) {}
		assertEquals(1, cf.getInt("A/a1"));

		cf = cf.getResolving();

		assertEquals(1, cf.getInt("x1"));
		assertEquals(1, cf.getInt("x2"));
		assertEquals(0, cf.getInt("x3"));
		assertEquals(9, cf.getInt("x3", 9));
		assertEquals(2, cf.getInt("x4"));
		assertEquals(9, cf.getInt("x5", 9));
		assertEquals(1, cf.getInt("A/a1"));
		assertEquals(9, cf.getInt("A/a2", 9));
		assertEquals(9, cf.getInt("B/b1", 9));

		System.clearProperty("X");
	}

	//====================================================================================================
	// getBoolean(String key)
	// getBoolean(String key, boolean def)
	//====================================================================================================
	@Test
	public void testGetBoolean() throws Exception {
		System.setProperty("X", "true");
		ConfigFile cf = configFileBuilder.build().addLines(null, "x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "x4=$S{Y,true}").addLines("A", "a1=true");

		assertFalse(cf.getBoolean("x1"));
		assertFalse(cf.getBoolean("x2"));
		assertFalse(cf.getBoolean("x3"));
		assertFalse(cf.getBoolean("x4"));
		assertTrue(cf.getBoolean("A/a1"));

		cf = cf.getResolving();

		assertTrue(cf.getBoolean("x1"));
		assertTrue(cf.getBoolean("x2"));
		assertFalse(cf.getBoolean("x3"));
		assertTrue(cf.getBoolean("x3", true));
		assertTrue(cf.getBoolean("x4"));
		assertTrue(cf.getBoolean("x5", true));
		assertTrue(cf.getBoolean("A/a1"));
		assertTrue(cf.getBoolean("A/a2", true));
		assertTrue(cf.getBoolean("B/b1", true));

		System.clearProperty("X");
	}

	//====================================================================================================
	// getSectionAsBean(String,Class)
	//====================================================================================================
	@Test
	public void testGetSectionAsBean() throws Exception {
		ConfigFile cf = configFileBuilder.build();
		cf.put("A/a", "1");
		cf.put("A/b", "2");

		A a = cf.getSectionAsBean("A", A.class);
		assertObjectEquals("{a:1,b:2}", a);

		cf.put("A/c", "3");
		try { cf.getSectionAsBean("A",A.class);} catch (ParseException e) {}
		a = cf.getSectionAsBean("A", A.class, true);
		assertObjectEquals("{a:1,b:2}", a);

		System.setProperty("X", "3");
		cf.put("A/a", "$S{X}");
		cf.put("A/b", "$S{Y,$S{X}}");
		cf = cf.getResolving();
		a = cf.getSectionAsBean("A", A.class, true);
		assertObjectEquals("{a:3,b:3}", a);
		System.clearProperty("X");
	}

	public static class A {
		public int a;
		private int b;
		public int getB() {
			return b;
		}
		public void setB(int b) {
			this.b = b;
		}
	}

	//====================================================================================================
	// writeProperties(...)
	//====================================================================================================
	@Test
	public void testWriteProperties() throws Exception {
		ConfigFile cf = configFileBuilder.build();
		cf.put("B/a", "1");

		B b = new B();
		cf.writeProperties("B", b, false);
		assertObjectEquals("{a:1}", b);

		cf.put("B/b", "2");
		try { cf.writeProperties("B", b, false);} catch (ParseException e) {}
		cf.writeProperties("B", b, true);
		assertObjectEquals("{a:1}", b);
		cf.removeString("B/b");

		cf.put("B/c", "2");
		try { cf.writeProperties("B", b, false);} catch (ParseException e) {}
		cf.writeProperties("B", b, true);
		assertObjectEquals("{a:1}", b);
		cf.removeString("B/c");

		cf.put("B/c2", "2");
		try { cf.writeProperties("B", b, false);} catch (ParseException e) {}
		cf.writeProperties("B", b, true);
		assertObjectEquals("{a:1}", b);
		cf.removeString("B/c2");

		cf.put("B/d", "2");
		try { cf.writeProperties("B", b, false);} catch (ParseException e) {}
		cf.writeProperties("B", b, true);
		assertObjectEquals("{a:1}", b);
		cf.removeString("B/d");

		cf.put("B/e", "2");
		cf.writeProperties("B", b, false);
		assertObjectEquals("{a:1,e:2}", b);

		cf.put("B/f", "foobar");
		try {
			cf.writeProperties("B", b, false, int.class, Integer.class);
		} catch (ParseException e) {
			assertTrue(e.getLocalizedMessage().startsWith("Invalid"));
		}
		assertObjectEquals("{a:1,e:2}", b);

		cf.removeString("B/f");
		System.setProperty("X", "3");
		cf.put("B/a", "$S{X}");
		cf.put("B/e", "$S{Y,$S{X}}");
		cf = cf.getResolving();
		cf.writeProperties("B", b, true);
		assertObjectEquals("{a:3,e:3}", b);

		System.clearProperty("X");
	}

	@Bean(sort=true)
	public static class B {
		private int a;
		private Integer e;
		public int getA() {
			return a;
		}
		public void setA(int a) {
			this.a = a;
		}
		public static void setB(String b) {
			throw new RuntimeException("Should not be called.");
		}
		protected void setC(String c) {
			throw new RuntimeException("Should not be called.");
		}
		protected static void setC2(String c2) {
			throw new RuntimeException("Should not be called.");
		}
		public void setD(String d, String x) {
			throw new RuntimeException("Should not be called.");
		}
		public Integer getE() {
			return e;
		}
		public void setE(Integer e) {
			this.e = e;
		}
		public String getF() {
			return null;
		}
		public void setF(String f) {
			throw new RuntimeException("Should not be called.");
		}
	}

	//====================================================================================================
	// Bad input
	//====================================================================================================
	@Test
	public void testBadInput() throws Exception {
		ConfigFile[] cff = {
			configFileBuilder.build().addLines("A", "a1=1", ""),
			configFileBuilder.build().addLines("A", "a1=1", "").getResolving()
		};

		for (ConfigFile cf : cff) {
			try {
				cf.load(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'r' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getString(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getString(null, null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.put(null, (Object)null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.put(null, null, true);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.removeString(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getObject(null, Object.class);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getObject("", null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'c' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getStringArray(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getStringArray(null, null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getInt(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getInt(null, -1);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getBoolean(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getBoolean(null, true);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.put(null, (String)null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.put(null, null, true);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.writeProperties(null, null, true);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'bean' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getSectionAsBean(null, null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'c' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getSectionAsBean(null, null, true);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'c' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.containsNonEmptyValue(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.isEncoded(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.addListener(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'listener' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.merge(null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'cf' cannot be null.", e.getLocalizedMessage());
			}
			try {
				cf.getResolving((VarResolver)null);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals("Field 'vr' cannot be null.", e.getLocalizedMessage());
			}
		}
	}

	//====================================================================================================
	// Config to and from JSON
	//====================================================================================================
	@Test
	public void testSerializedAsJson() throws Exception {
		ConfigFile cf = configFileBuilder.build();
		cf.put("a", "1");
		cf.put("B/a", "2");

		String json = JsonSerializer.DEFAULT_LAX.toString(cf);
		assertEquals("{'default':{a:'1'},B:{a:'2'}}", json);

		cf = JsonParser.DEFAULT.parse(json, ConfigFileImpl.class);
		assertObjectEquals("{'default':{a:'1'},B:{a:'2'}}", cf);

	}

	//====================================================================================================
	// Test resolving with override
	//====================================================================================================
	@Test
	public void testResolvingWithOverride() throws Exception {
		ConfigFile cf = configFileBuilder.build();
		cf.put("a", "$A{X}");
		cf.put("b", "$B{X}");
		cf.put("c", "$A{$B{X}}");
		cf.put("d", "$B{$A{X}}");
		cf.put("e", "$D{X}");

		VarResolver vr = new VarResolverBuilder().vars(ALVar.class, BLVar.class).build();

		cf = cf.getResolving(vr);

		assertEquals("aXa", cf.getString("a"));
		assertEquals("bXb", cf.getString("b"));
		assertEquals("abXba", cf.getString("c"));
		assertEquals("baXab", cf.getString("d"));
		assertEquals("$D{X}", cf.getString("e"));

		// Create new resolver that addx $C and overrides $A
		VarResolver vr2 = vr.builder().vars(AUVar.class, DUVar.class).build();

		// true == augment by adding existing as parent to the new resolver
		cf = cf.getResolving(vr2);
		assertEquals("AXA", cf.getString("a"));
		assertEquals("bXb", cf.getString("b"));
		assertEquals("AbXbA", cf.getString("c"));
		assertEquals("bAXAb", cf.getString("d"));
		assertEquals("DXD", cf.getString("e"));
	}

	public static class ALVar extends SimpleVar {
		public ALVar() {
			super("A");
		}
		@Override
		public String resolve(VarResolverSession session, String key) {
			return 'a' + key + 'a';
		}
	}

	public static class AUVar extends SimpleVar {
		public AUVar() {
			super("A");
		}
		@Override
		public String resolve(VarResolverSession session, String key) {
			return 'A' + key + 'A';
		}
	}

	public static class BLVar extends SimpleVar {
		public BLVar() {
			super("B");
		}
		@Override
		public String resolve(VarResolverSession session, String key) {
			return 'b' + key + 'b';
		}
	}

	public static class DUVar extends SimpleVar {
		public DUVar() {
			super("D");
		}
		@Override
		public String resolve(VarResolverSession session, String key) {
			return 'D' + key + 'D';
		}
	}

	//====================================================================================================
	// Test multiline values.
	//====================================================================================================
	@Test
	public void testMultilineValues() throws Exception {
		File f = getFreshFile();

		ConfigFile cf = configFileBuilder.build(f);
		cf.put("a", "a,\nb,\nc");
		cf.put("A/a", "a,\nb,\nc");

		assertTextEquals("a = a,|\tb,|\tc|[A]|a = a,|\tb,|\tc|", cf);
		cf.save();
		assertTextEquals("a = a,|\tb,|\tc|[A]|a = a,|\tb,|\tc|", read(f));

		cf.load();
		assertEquals("a,\nb,\nc", cf.getString("a"));
		assertEquals("a,\nb,\nc", cf.getString("A/a"));

		assertObjectEquals("['a','b','c']", cf.getStringArray("a", null));
		assertObjectEquals("['a','b','c']", cf.getStringArray("A/a", null));
	}

	//====================================================================================================
	// Test special character encoding.
	//====================================================================================================
	@Test
	public void testSpecialCharacterEncoding() throws Exception {
		File f = getFreshFile();

		ConfigFile cf = configFileBuilder.build(f);
		cf.put("a", "a,#b,=c");
		cf.put("A/a", "a,#b,=c");

		assertTextEquals("a = a,\\u0023b,\\u003Dc|[A]|a = a,\\u0023b,\\u003Dc|", cf);
		cf.save();
		assertTextEquals("a = a,\\u0023b,\\u003Dc|[A]|a = a,\\u0023b,\\u003Dc|", read(f));

		cf.load();
		assertEquals("a,#b,=c", cf.getString("a"));
		assertEquals("a,#b,=c", cf.getString("A/a"));
	}
}