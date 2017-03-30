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

import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class ConfigFileBuilderTest {

	private static File tempDir;
	private static String[] TEMP_DIR;

	@BeforeClass
	public static void setup() {
		tempDir = new File(System.getProperty("java.io.tmpdir"), StringUtils.generateUUID(12));
		FileUtils.mkdirs(tempDir, true);
		TEMP_DIR = new String[]{tempDir.getAbsolutePath()};
	}

	@AfterClass
	public static void teardown() {
		FileUtils.delete(tempDir);
	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGet() throws Exception {
		File f;
		ConfigFileBuilder b1 = new ConfigFileBuilder().paths(TEMP_DIR).createIfNotExists();

		ConfigFile cf = b1.build("TestGet.cfg");
		cf.put("Test/A", "a");

		f = new File(tempDir, "TestGet.cfg");
		assertTrue(f.exists());

		cf.save();
		assertTextEquals("[Test]|A = a|", IOUtils.read(f));

		cf = b1.build("TestGet.cfg");
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf);

		String NL = System.getProperty("line.separator");
		cf = b1.build(new StringReader(("[Test]"+NL+"A = a"+NL)));
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf);

		b1.charset(IOUtils.UTF8);
		cf = b1.build("TestGet.cfg");
		assertObjectEquals("{'default':{},Test:{A:'a'}}", cf);
	}
	
	/**
	 * Retrieving config file should fail if the file doesn't exist and createIfNotExist == false.
	 */
	@Test
	public void testFailOnNonExistentFiles() throws Exception {
		ConfigFileBuilder b = new ConfigFileBuilder().paths(new String[]{tempDir.getAbsolutePath()});
		try { b.build("TestGet2.cfg"); fail(); } catch (FileNotFoundException e) {}
		try { b.build(tempDir.getAbsolutePath() + "TestGet2.cfg"); fail(); } catch (FileNotFoundException e) {}

		b = new ConfigFileBuilder().paths().createIfNotExists();
		try { b.build("TestGet.cfg"); fail(); } catch (FileNotFoundException e) {}
	}	

	
	//====================================================================================================
	// loadIfModified()
	//====================================================================================================
	@Test
	public void testLoadIfModified() throws Exception {
		ConfigFileBuilder b = new ConfigFileBuilder().paths(TEMP_DIR).createIfNotExists();
		File f;
		ConfigFile cf = b.build("TestGet.cfg");
		cf.put("Test/A", "a");

		f = new File(tempDir, "TestGet.cfg");
		String NL = System.getProperty("line.separator");
		IOUtils.write(f, new StringReader("[Test]"+NL+"A = b"+NL));
		FileUtils.modifyTimestamp(f);

		cf.loadIfModified();
		assertEquals("b", cf.getString("Test/A"));
		cf.loadIfModified();
		assertEquals("b", cf.getString("Test/A"));

		// Config file with no backing file.
		cf = b.build();
		cf.put("Test/B", "b");
		cf.loadIfModified();
		cf.loadIfModified();
		assertEquals("b", cf.getString("Test/B"));
	}

	//====================================================================================================
	// read only
	//====================================================================================================
	@Test
	public void testReadOnly() throws Exception {
		ConfigFileBuilder cm = new ConfigFileBuilder().paths(TEMP_DIR).createIfNotExists().readOnly();
		ConfigFile cf = cm.build("TestGet.cfg");

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
		ConfigFileBuilder cm = new ConfigFileBuilder().paths(TEMP_DIR).createIfNotExists();

		ConfigFile cf = cm.build("Test.cfg")
			.addLines(null, "# c1", "\t# c2", " c3 ", "  ", "x1=1", "x2=true", "x3=null")
			.addLines("s1", "#c4", "k1=1", "#c5 foo=bar", "k2 = true", "k3  = \tnull");
		cf.save();

		File configFile = new File(tempDir, "Test.cfg");
		File envFile = new File(tempDir, "Test.bat");

		ConfigFileBuilder.main(new String[]{"createBatchEnvFile", "-configFile", configFile.getAbsolutePath(), "-envFile", envFile.getAbsolutePath()});
		String expected = "rem c1|rem c2|rem c3||set x1 = 1|set x2 = true|set x3 = null|rem c4|set s1_k1 = 1|rem c5 foo=bar|set s1_k2 = true|set s1_k3 = null|";
		String actual = IOUtils.read(envFile);
		assertTextEquals(expected, actual);

		ConfigFileBuilder.main(new String[]{"createShellEnvFile", "-configFile", configFile.getAbsolutePath(), "-envFile", envFile.getAbsolutePath()});
		expected = "# c1|# c2|# c3||export x1=\"1\"|export x2=\"true\"|export x3=\"null\"|# c4|export s1_k1=\"1\"|# c5 foo=bar|export s1_k2=\"true\"|export s1_k3=\"null\"|";
		actual = IOUtils.read(envFile);
		assertTextEquals(expected, actual);

		ConfigFileBuilder.main(new String[]{"setVals", "-configFile", configFile.getAbsolutePath(), "-vals", "x1=2", "s1/k1=2", "s2/k1=3"});
		FileUtils.modifyTimestamp(configFile);
		cf.loadIfModified();
		assertObjectEquals("{'default':{x1:'2',x2:'true',x3:'null'},s1:{k1:'2',k2:'true',k3:'null'},s2:{k1:'3'}}", cf);

		ConfigFileBuilder.main(new String[]{});
	}
}