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
package org.apache.juneau.ini;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.svl.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class ConfigMgrTest {

	private static File tempDir;

	@BeforeClass
	public static void setup() {
		tempDir = new File(System.getProperty("java.io.tmpdir"), StringUtils.generateUUID(12));
		FileUtils.mkdirs(tempDir, true);
	}

	@AfterClass
	public static void teardown() {
		FileUtils.delete(tempDir);
	}

	//====================================================================================================
	// get(String path)
	// get(String path, boolean create)
	//====================================================================================================
	@Test
	public void testGet() throws Exception {
		File f;
		ConfigMgr cm = new ConfigMgr(false, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, Charset.defaultCharset(), new String[]{tempDir.getAbsolutePath()});

		ConfigFile cf = cm.get("TestGet.cfg", true);
		cf.put("Test/A", "a");

		f = new File(tempDir, "TestGet.cfg");
		assertTrue(f.exists());

		cf.save();
		assertTextEquals("[Test]|A = a|", IOUtils.read(f));

		cf = cm.get("TestGet.cfg");
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf);

		ConfigFile cf2 = cm.get(tempDir.getAbsolutePath() + "/TestGet.cfg");
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf2);
		assertTrue(cf == cf2);  // Relative and absolute paths must resolve to same config file.

		try { cm.get("TestGet2.cfg"); fail(); } catch (FileNotFoundException e) {}
		try { cm.get(tempDir.getAbsolutePath() + "TestGet2.cfg"); fail(); } catch (FileNotFoundException e) {}

		cm.get(tempDir.getAbsolutePath() + "TestGet2.cfg", true);

		ConfigMgr cm2 = new ConfigMgr(false, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, Charset.defaultCharset(), new String[]{tempDir.getAbsolutePath()});
		cf = cm2.get("TestGet.cfg");
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf);

		cm2 = new ConfigMgr(false, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, Charset.defaultCharset(), null);
		try { cf = cm2.get("TestGet.cfg"); fail(); } catch (FileNotFoundException e) {}

		String NL = System.getProperty("line.separator");
		cf = cm2.create(new StringReader(("[Test]"+NL+"A = a"+NL)));
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf);

		ConfigMgr cm3 = new ConfigMgr(false, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, IOUtils.UTF8, new String[]{tempDir.getAbsolutePath()});
		cf = cm3.get("TestGet.cfg");
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf);

		cm.deleteAll();
		cm2.deleteAll();
		cm3.deleteAll();
	}

	//====================================================================================================
	// loadIfModified()
	//====================================================================================================
	@Test
	public void testLoadIfModified() throws Exception {
		ConfigMgr cm = new ConfigMgr(false, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, Charset.defaultCharset(), new String[]{tempDir.getAbsolutePath()});
		File f;
		ConfigFile cf = cm.get("TestGet.cfg", true);
		cf.put("Test/A", "a");

		f = new File(tempDir, "TestGet.cfg");
		String NL = System.getProperty("line.separator");
		IOUtils.write(f, new StringReader("[Test]"+NL+"A = b"+NL));
		FileUtils.modifyTimestamp(f);

		cm.loadIfModified();
		assertEquals("b", cf.getString("Test/A"));
		cm.loadIfModified();
		assertEquals("b", cf.getString("Test/A"));

		// Config file with no backing file.
		cf = cm.create();
		cf.put("Test/B", "b");
		cm.loadIfModified();
		cf.loadIfModified();
		assertEquals("b", cf.getString("Test/B"));

		cm.deleteAll();
	}

	//====================================================================================================
	// read only
	//====================================================================================================
	@Test
	public void testReadOnly() throws Exception {
		ConfigMgr cm = new ConfigMgr(true, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, Charset.defaultCharset(), new String[]{tempDir.getAbsolutePath()});
		ConfigFile cf = cm.get("TestGet.cfg", true);

		// All these should fail.
		try { cf.loadIfModified(); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.load(); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.load(new StringReader("")); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.put("A","b"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.put("A","b",true); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.put("A","b"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.put("A","b",true); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.removeString("A"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.addLines("A","b=c"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.addHeaderComments("A", "b=c"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.clearHeaderComments("A"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.addSection("A"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.setSection("A",null); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.removeSection("A"); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.save(); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.merge(cf); fail(); } catch (UnsupportedOperationException e) {}
		try { cf.addListener(new ConfigFileListener(){}); fail(); } catch (UnsupportedOperationException e) {}

		// All these should succeed.
		cf.getObject(String.class, "A");
		cf.getObject(String.class, "A", "a");
		cf.getString("A");
		cf.getString("A","a");
		cf.getObject(String.class, "A");
		cf.getObject(String.class, "A", "a");
		cf.getObject(String[].class, "A");
		cf.getStringArray("A");
		cf.getStringArray("A", null);
		cf.getInt("A");
		cf.getInt("A", 0);
		cf.getBoolean("A");
		cf.getBoolean("A", true);
		cf.containsNonEmptyValue("A");
		cf.getSectionMap("A");
		cf.serializeTo(new StringWriter());
		cf.serializeTo(new StringWriter(), ConfigFileFormat.INI);
		cf.getResolving(VarResolver.DEFAULT);
		cf.toWritable();
	}

	//====================================================================================================
	// main(String[] args)
	//====================================================================================================
	@Test
	public void testMain() throws Exception {
		System.setProperty("exit.2", "0");
		ConfigMgr cm = new ConfigMgr(false, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, Charset.defaultCharset(), new String[]{tempDir.getAbsolutePath()});

		ConfigFile cf = cm.get("Test.cfg", true)
			.addLines(null, "# c1", "\t# c2", " c3 ", "  ", "x1=1", "x2=true", "x3=null")
			.addLines("s1", "#c4", "k1=1", "#c5 foo=bar", "k2 = true", "k3  = \tnull");
		cf.save();

		File configFile = new File(tempDir, "Test.cfg");
		File envFile = new File(tempDir, "Test.bat");

		ConfigMgr.main(new String[]{"createBatchEnvFile", "-configFile", configFile.getAbsolutePath(), "-envFile", envFile.getAbsolutePath()});
		String expected = "rem c1|rem c2|rem c3||set x1 = 1|set x2 = true|set x3 = null|rem c4|set s1_k1 = 1|rem c5 foo=bar|set s1_k2 = true|set s1_k3 = null|";
		String actual = IOUtils.read(envFile);
		assertTextEquals(expected, actual);

		ConfigMgr.main(new String[]{"createShellEnvFile", "-configFile", configFile.getAbsolutePath(), "-envFile", envFile.getAbsolutePath()});
		expected = "# c1|# c2|# c3||export x1=\"1\"|export x2=\"true\"|export x3=\"null\"|# c4|export s1_k1=\"1\"|# c5 foo=bar|export s1_k2=\"true\"|export s1_k3=\"null\"|";
		actual = IOUtils.read(envFile);
		assertTextEquals(expected, actual);

		ConfigMgr.main(new String[]{"setVals", "-configFile", configFile.getAbsolutePath(), "-vals", "x1=2", "s1/k1=2", "s2/k1=3"});
		FileUtils.modifyTimestamp(configFile);
		cf.loadIfModified();
		assertObjectEquals("{'default':{x1:'2',x2:'true',x3:'null'},s1:{k1:'2',k2:'true',k3:'null'},s2:{k1:'3'}}", cf);

		ConfigMgr.main(new String[]{});
	}
}