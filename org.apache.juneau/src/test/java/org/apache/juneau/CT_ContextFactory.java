/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.xml.*;
import org.junit.*;


@SuppressWarnings({"rawtypes"})
public class CT_ContextFactory {

	//====================================================================================================
	// testSimpleProperties()
	//====================================================================================================
	@Test
	public void testSimpleProperties() {
		ContextFactory f = ContextFactory.create();

		f.setProperty("A.f1", "1");
		f.setProperty("A.f2", "2");

		assertObjectEquals("{'A.f1':'1','A.f2':'2'}", f.getPropertyMap("A").asMap());

		f.setProperty("B.f3", "3");
		f.setProperty("A.f1", String.class);
		f.setProperty("A.f2", 4);

		assertObjectEquals("{'A.f1':'java.lang.String','A.f2':4}", f.getPropertyMap("A").asMap());

		f.setProperty("A.f2", null);
		f.setProperty("A.f2", null);
		assertObjectEquals("{'A.f1':'java.lang.String'}", f.getPropertyMap("A").asMap());

		try {
			f.setProperty(null, null);
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Invalid property name specified: 'null'", e.getMessage());
		}

		try {
			f.addToProperty("A.f1", "foo");
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Cannot add value 'foo' (java.lang.String) to property 'A.f1' (SIMPLE).", e.getMessage());
		}

		try {
			f.removeFromProperty("A.f1", "foo");
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Cannot remove value 'foo' (java.lang.String) from property 'A.f1' (SIMPLE).", e.getMessage());
		}

		try {
			f.putToProperty("A.f1", "foo", "bar");
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Cannot put value 'foo'(java.lang.String)->'bar'(java.lang.String) to property 'A.f1' (SIMPLE).", e.getMessage());
		}

		try {
			f.putToProperty("A.f1", "foo");
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Cannot put value 'foo' (java.lang.String) to property 'A.f1' (SIMPLE).", e.getMessage());
		}
	}

	//====================================================================================================
	// testSetProperties()
	//====================================================================================================
	@Test
	public void testSetProperties() {
		ContextFactory f = ContextFactory.create();
		String key = "A.f1.set";

		f.setProperty(key, Arrays.asList(2,3,1));
		assertObjectEquals("[1,2,3]", f.getProperty(key, int[].class, null));

		f.addToProperty(key, 0);
		f.addToProperty(key, new int[]{4,5});
		assertObjectEquals("[0,1,2,3,4,5]", f.getProperty(key, int[].class, null));
		f.addToProperty(key, new HashSet<String>(Arrays.asList("6","7")));
		assertObjectEquals("[0,1,2,3,4,5,6,7]", f.getProperty(key, int[].class, null));
		f.addToProperty(key, new int[]{4,5});
		assertObjectEquals("[0,1,2,3,4,5,6,7]", f.getProperty(key, int[].class, null));

		f.removeFromProperty(key, 4);
		f.removeFromProperty(key, new HashSet<String>(Arrays.asList("1")));
		f.removeFromProperty(key, new String[]{"2","9"});
		assertObjectEquals("[0,3,5,6,7]", f.getProperty(key, int[].class, null));
		assertObjectEquals("['0','3','5','6','7']", f.getProperty(key, String[].class, null));

		f.setProperty(key, Arrays.asList("foo","bar","baz"));
		assertObjectEquals("['bar','baz','foo']", f.getProperty(key, String[].class, null));

		f.setProperty(key, "[1,2,3]");
		assertObjectEquals("[1,2,3]", f.getProperty(key, int[].class, null));

		f.setProperty(key, "['1','2','3']");
		assertObjectEquals("[1,2,3]", f.getProperty(key, int[].class, null));

		try {
			f.putToProperty("A.f1.set", "foo");
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Cannot put value 'foo' (java.lang.String) to property 'A.f1.set' (SET).", e.getMessage());
		}

		try {
			f.putToProperty("A.f1.set", "foo", "bar");
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Cannot put value 'foo'(java.lang.String)->'bar'(java.lang.String) to property 'A.f1.set' (SET).", e.getMessage());
		}
	}

	//====================================================================================================
	// testListProperties()
	//====================================================================================================
	@Test
	public void testListProperties() {
		ContextFactory f = ContextFactory.create();
		String key = "A.f1.list";

		f.setProperty(key, Arrays.asList(2,3,1));
		assertObjectEquals("[2,3,1]", f.getProperty(key, int[].class, null));

		f.addToProperty(key, 0);
		f.addToProperty(key, new int[]{4,5});
		assertObjectEquals("[4,5,0,2,3,1]", f.getProperty(key, int[].class, null));
		f.addToProperty(key, new TreeSet<String>(Arrays.asList("6","7")));
		assertObjectEquals("[6,7,4,5,0,2,3,1]", f.getProperty(key, int[].class, null));
		f.addToProperty(key, new int[]{4,5});
		assertObjectEquals("[4,5,6,7,0,2,3,1]", f.getProperty(key, int[].class, null));

		f.removeFromProperty(key, 4);
		f.removeFromProperty(key, new HashSet<String>(Arrays.asList("1")));
		f.removeFromProperty(key, new String[]{"2","9"});
		assertObjectEquals("[5,6,7,0,3]", f.getProperty(key, int[].class, null));
		assertObjectEquals("['5','6','7','0','3']", f.getProperty(key, String[].class, null));

		f.setProperty(key, Arrays.asList("foo","bar","baz"));
		assertObjectEquals("['foo','bar','baz']", f.getProperty(key, String[].class, null));
	}

	//====================================================================================================
	// testMapProperties()
	//====================================================================================================
	@SuppressWarnings("serial")
	@Test
	public void testMapProperties() {
		ContextFactory f = ContextFactory.create();
		String key = "A.f1.map";

		f.setProperty(key, new HashMap<String,String>(){{put("1","1");put("3","3");put("2","2");}});
		assertObjectEquals("{'1':1,'2':2,'3':3}", f.getMap(key, Integer.class, Integer.class, null));

		f.setProperty(key, "{'1':1,'2':2,'3':3}");
		assertObjectEquals("{'1':1,'2':2,'3':3}", f.getMap(key, Integer.class, Integer.class, null));

		f.putToProperty(key, "{'3':4,'4':5,'5':6}");
		assertObjectEquals("{'1':1,'2':2,'3':4,'4':5,'5':6}", f.getMap(key, Integer.class, Integer.class, null));
	}

	//====================================================================================================
	// Hash code and comparison
	//====================================================================================================
	@SuppressWarnings({ "serial" })
	@Test
	public void testHashCodes() throws Exception {
		ContextFactory f1 = ContextFactory.create();
		f1.setProperty("A.a", 1);
		f1.setProperty("A.b", true);
		f1.setProperty("A.c", String.class);
		f1.setProperty("A.d.set", new Object[]{1, true, String.class});
		f1.setProperty("A.e.map", new HashMap<Object,Object>(){{put(true,true);put(1,1);put(String.class,String.class);}});

		ContextFactory f2 = ContextFactory.create();
		f2.setProperty("A.e.map", new HashMap<Object,Object>(){{put("1","1");put("true","true");put("java.lang.String","java.lang.String");}});
		f2.setProperty("A.d.set", new Object[]{"true","1","java.lang.String"});
		f2.setProperty("A.c", "java.lang.String");
		f2.setProperty("A.b", "true");
		f2.setProperty("A.a", "1");

		ContextFactory.PropertyMap p1 = f1.getPropertyMap("A");
		ContextFactory.PropertyMap p2 = f2.getPropertyMap("A");
		assertEquals(p1.hashCode(), p2.hashCode());
	}

	@SuppressWarnings("unchecked")
	private static class ConversionTest {
		ContextFactory config = ContextFactory.create();
		String pName;
		Object in;

		private ConversionTest(String pName, Object in) {
			this.pName = pName;
			this.in = in;
		}

		private ConversionTest test(Class c, String expected) {
			try {
				config.setProperty(pName, in);
				assertObjectEquals(expected, config.getProperty(pName, c, null));
			} catch (Exception x) {
				assertEquals(expected.toString(), x.getLocalizedMessage());
			}
			return this;
		}

		private ConversionTest testMap(Class k, Class v, String expected) {
			try {
				config.setProperty(pName, in);
				assertObjectEquals(expected, config.getMap(pName, k, v, null));
			} catch (Exception x) {
				assertEquals(expected, x.getLocalizedMessage());
			}
			return this;
		}
	}

	//====================================================================================================
	// Conversions on simple properties
	//====================================================================================================
	@Test
	@SuppressWarnings({ "serial" })
	public void testConversionsOnSimpleProperties() throws Exception {
		String pName = "A.a";

		//--------------------------------------------------------------------------------
		// boolean
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, true)
			.test(boolean.class, "true")
			.test(int.class, "1")
			.test(String.class, "'true'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Boolean' to type 'java.lang.Class'.  Value=true.")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Boolean' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=true.")
			.test(String[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Boolean' to type 'java.lang.String[]'.  Value=true.")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Boolean' to type 'java.lang.Class[]'.  Value=true.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Boolean' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=true.")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Boolean' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=true.")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Boolean' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=true.")
		;

		//--------------------------------------------------------------------------------
		// int
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, 123)
			.test(boolean.class, "true")
			.test(int.class, "123")
			.test(String.class, "'123'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Integer' to type 'java.lang.Class'.  Value=123.")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Integer' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=123.")
			.test(String[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Integer' to type 'java.lang.String[]'.  Value=123.")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Integer' to type 'java.lang.Class[]'.  Value=123.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Integer' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=123.")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Integer' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=123.")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Integer' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=123.")
		;

		//--------------------------------------------------------------------------------
		// Class
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, String.class)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class' to type 'int'.  Value='java.lang.String'.")
			.test(String.class, "'java.lang.String'")
			.test(Class.class, "'java.lang.String'")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value='java.lang.String'.")
			.test(String[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class' to type 'java.lang.String[]'.  Value='java.lang.String'.")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class' to type 'java.lang.Class[]'.  Value='java.lang.String'.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value='java.lang.String'.")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value='java.lang.String'.")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value='java.lang.String'.")
		;

		//--------------------------------------------------------------------------------
		// String
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, "foo")
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'int'.  Value='foo'.")
			.test(String.class, "'foo'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'java.lang.Class'.  Value='foo'.")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value='foo'.")
			.test(String[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'java.lang.String[]'.  Value='foo'.")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'java.lang.Class[]'.  Value='foo'.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value='foo'.")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value='foo'.")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value='foo'.")
		;
		new ConversionTest(pName, "java.lang.String")
			.test(Class.class, "'java.lang.String'")
		;
		new ConversionTest(pName, "true")
			.test(boolean.class, "true")
		;
		new ConversionTest(pName, "ONE")
			.test(TestEnum.class, "'ONE'")
		;
		new ConversionTest(pName, "123")
			.test(int.class, "123")
		;

		//--------------------------------------------------------------------------------
		// enum
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, TestEnum.ONE)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum' to type 'int'.  Value='ONE'.")
			.test(String.class, "'ONE'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum' to type 'java.lang.Class'.  Value='ONE'.")
			.test(TestEnum.class, "'ONE'")
			.test(String[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum' to type 'java.lang.String[]'.  Value='ONE'.")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum' to type 'java.lang.Class[]'.  Value='ONE'.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value='ONE'.")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value='ONE'.")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value='ONE'.")
		;

		//--------------------------------------------------------------------------------
		// String[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new String[]{"foo","bar"})
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String[]' to type 'int'.  Value=['foo','bar'].")
			.test(String.class, "'[\\'foo\\',\\'bar\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String[]' to type 'java.lang.Class'.  Value=['foo','bar'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String[]' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['foo','bar'].")
			.test(String[].class, "['foo','bar']")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String[]' to type 'java.lang.Class[]'.  Value=['foo','bar'].")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String[]' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=['foo','bar'].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String[]' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['foo','bar'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.String[]' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['foo','bar'].")
		;
		new ConversionTest(pName, new String[]{"ONE","TWO"})
			.test(TestEnum[].class, "['ONE','TWO']")
		;
		new ConversionTest(pName, new String[]{"true","false"})
			.test(boolean[].class, "[true,false]")
		;
		new ConversionTest(pName, new String[]{"java.lang.String","java.lang.Integer"})
			.test(Class[].class, "['java.lang.String','java.lang.Integer']")
		;

		//--------------------------------------------------------------------------------
		// Class[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new Class[]{String.class,Integer.class})
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class[]' to type 'int'.  Value=['java.lang.String','java.lang.Integer'].")
			.test(String.class, "'[\\'java.lang.String\\',\\'java.lang.Integer\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class[]' to type 'java.lang.Class'.  Value=['java.lang.String','java.lang.Integer'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class[]' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['java.lang.String','java.lang.Integer'].")
			.test(String[].class, "['java.lang.String','java.lang.Integer']")
			.test(Class[].class, "['java.lang.String','java.lang.Integer']")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class[]' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=['java.lang.String','java.lang.Integer'].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class[]' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['java.lang.String','java.lang.Integer'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.lang.Class[]' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['java.lang.String','java.lang.Integer'].")
		;

		//--------------------------------------------------------------------------------
		// enum[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new TestEnum[]{TestEnum.ONE,TestEnum.TWO})
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum[]' to type 'int'.  Value=['ONE','TWO'].")
			.test(String.class, "'[\\'ONE\\',\\'TWO\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum[]' to type 'java.lang.Class'.  Value=['ONE','TWO'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum[]' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['ONE','TWO'].")
			.test(String[].class, "['ONE','TWO']")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum[]' to type 'java.lang.Class[]'.  Value=['ONE','TWO'].")
			.test(TestEnum[].class, "['ONE','TWO']")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum[]' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['ONE','TWO'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'org.apache.juneau.CT_ContextFactory$TestEnum[]' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['ONE','TWO'].")
		;

		//--------------------------------------------------------------------------------
		// Map<String,String>
		//--------------------------------------------------------------------------------
		LinkedHashMap<String,String> m1 = new LinkedHashMap<String,String>();
		m1.put("foo","bar");
		new ConversionTest(pName, m1)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'int'.  Value={foo:'bar'}.")
			.test(String.class, "'{foo:\\'bar\\'}'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'java.lang.Class'.  Value={foo:'bar'}.")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value={foo:'bar'}.")
			.test(String[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'java.lang.String[]'.  Value={foo:'bar'}.")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'java.lang.Class[]'.  Value={foo:'bar'}.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value={foo:'bar'}.")
			.testMap(String.class, String.class, "{foo:'bar'}")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value={foo:'bar'}.")
		;

		//--------------------------------------------------------------------------------
		// Map<Class,Class>
		//--------------------------------------------------------------------------------
		LinkedHashMap<Class,Class> m2 = new LinkedHashMap<Class,Class>();
		m2.put(String.class, Integer.class);
		new ConversionTest(pName, m2)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'int'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(String.class, "'{\\'java.lang.String\\':\\'java.lang.Integer\\'}'")
			.test(Class.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'java.lang.Class'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(TestEnum.class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(String[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'java.lang.String[]'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(Class[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'java.lang.Class[]'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a'.  Invalid data conversion from type 'java.util.LinkedHashMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.testMap(String.class, String.class, "{'java.lang.String':'java.lang.Integer'}")
			.testMap(Class.class, Class.class, "{'java.lang.String':'java.lang.Integer'}")
		;

		//--------------------------------------------------------------------------------
		// Namespace
		//--------------------------------------------------------------------------------
		final Namespace n = new Namespace("foo","bar");
		new ConversionTest(pName, n)
			.test(String.class, "'{name:\\'foo\\',uri:\\'bar\\'}'")
			.test(Namespace.class, "{name:'foo',uri:'bar'}");

		//--------------------------------------------------------------------------------
		// Namespace[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new Namespace[]{n})
			.test(String.class, "'[{name:\\'foo\\',uri:\\'bar\\'}]'")
			.test(Namespace[].class, "[{name:'foo',uri:'bar'}]");

		//--------------------------------------------------------------------------------
		// Map<Namespace,Namespace>
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new LinkedHashMap<Namespace,Namespace>(){{put(n,n);}})
			.testMap(Namespace.class, Namespace.class, "{'{name:\\'foo\\',uri:\\'bar\\'}':{name:'foo',uri:'bar'}}")
			.testMap(String.class, String.class, "{'{name:\\'foo\\',uri:\\'bar\\'}':'{name:\\'foo\\',uri:\\'bar\\'}'}");
	}

	//====================================================================================================
	// Conversions on set properties
	//====================================================================================================
	@Test
	@SuppressWarnings({ "serial" })
	public void testConversionsOnSetProperties() throws Exception {
		String pName = "A.a.set";

		//--------------------------------------------------------------------------------
		// boolean
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, true)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=[true].")
			.test(String.class, "'[true]'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=[true].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=[true].")
			.test(String[].class, "['true']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=[true].")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=[true].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=[true].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=[true].")
		;

		//--------------------------------------------------------------------------------
		// int
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, 123)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=[123].")
			.test(String.class, "'[123]'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=[123].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=[123].")
			.test(String[].class, "['123']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=[123].")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=[123].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=[123].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=[123].")
		;

		//--------------------------------------------------------------------------------
		// Class
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, String.class)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=['java.lang.String'].")
			.test(String.class, "'[\\'java.lang.String\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=['java.lang.String'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['java.lang.String'].")
			.test(String[].class, "['java.lang.String']")
			.test(Class[].class, "['java.lang.String']")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=['java.lang.String'].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['java.lang.String'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['java.lang.String'].")
		;

		//--------------------------------------------------------------------------------
		// String
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, "foo")
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=['foo'].")
			.test(String.class, "'[\\'foo\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=['foo'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['foo'].")
			.test(String[].class, "['foo']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=['foo'].")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=['foo'].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['foo'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['foo'].")
		;
		new ConversionTest(pName, Arrays.asList("java.lang.String"))
			.test(Class[].class, "['java.lang.String']")
		;
		new ConversionTest(pName, Arrays.asList("true"))
			.test(boolean[].class, "[true]")
		;
		new ConversionTest(pName, Arrays.asList("ONE"))
			.test(TestEnum[].class, "['ONE']")
		;
		new ConversionTest(pName, Arrays.asList("123"))
			.test(int[].class, "[123]")
		;

		//--------------------------------------------------------------------------------
		// enum
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, TestEnum.ONE)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=['ONE'].")
			.test(String.class, "'[\\'ONE\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=['ONE'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['ONE'].")
			.test(String[].class, "['ONE']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=['ONE'].")
			.test(TestEnum[].class, "['ONE']")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['ONE'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['ONE'].")
		;

		//--------------------------------------------------------------------------------
		// String[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new String[]{"foo","bar"})
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=['bar','foo'].")
			.test(String.class, "'[\\'bar\\',\\'foo\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=['bar','foo'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['bar','foo'].")
			.test(String[].class, "['bar','foo']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=['bar','foo'].")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=['bar','foo'].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['bar','foo'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['bar','foo'].")
		;
		new ConversionTest(pName, new String[]{"ONE","TWO"})
			.test(TestEnum[].class, "['ONE','TWO']")
		;
		new ConversionTest(pName, new String[]{"true","false"})
			.test(boolean[].class, "[false,true]")
		;
		new ConversionTest(pName, new String[]{"java.lang.String","java.lang.Integer"})
			.test(Class[].class, "['java.lang.Integer','java.lang.String']")
		;

		//--------------------------------------------------------------------------------
		// Class[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new Class[]{String.class,Integer.class})
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=['java.lang.Integer','java.lang.String'].")
			.test(String.class, "'[\\'java.lang.Integer\\',\\'java.lang.String\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=['java.lang.Integer','java.lang.String'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['java.lang.Integer','java.lang.String'].")
			.test(String[].class, "['java.lang.Integer','java.lang.String']")
			.test(Class[].class, "['java.lang.Integer','java.lang.String']")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=['java.lang.Integer','java.lang.String'].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['java.lang.Integer','java.lang.String'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['java.lang.Integer','java.lang.String'].")
		;

		//--------------------------------------------------------------------------------
		// enum[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new TestEnum[]{TestEnum.ONE,TestEnum.TWO})
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=['ONE','TWO'].")
			.test(String.class, "'[\\'ONE\\',\\'TWO\\']'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=['ONE','TWO'].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=['ONE','TWO'].")
			.test(String[].class, "['ONE','TWO']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=['ONE','TWO'].")
			.test(TestEnum[].class, "['ONE','TWO']")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=['ONE','TWO'].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=['ONE','TWO'].")
		;

		//--------------------------------------------------------------------------------
		// Map<String,String>
		//--------------------------------------------------------------------------------
		LinkedHashMap<String,String> m1 = new LinkedHashMap<String,String>();
		m1.put("foo","bar");
		new ConversionTest(pName, m1)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=[{foo:'bar'}].")
			.test(String.class, "'[{foo:\\'bar\\'}]'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=[{foo:'bar'}].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=[{foo:'bar'}].")
			.test(String[].class, "['{foo:\\'bar\\'}']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=[{foo:'bar'}].")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=[{foo:'bar'}].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=[{foo:'bar'}].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=[{foo:'bar'}].")
		;

		//--------------------------------------------------------------------------------
		// Map<Class,Class>
		//--------------------------------------------------------------------------------
		LinkedHashMap<Class,Class> m2 = new LinkedHashMap<Class,Class>();
		m2.put(String.class, Integer.class);
		new ConversionTest(pName, m2)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'int'.  Value=[{'java.lang.String':'java.lang.Integer'}].")
			.test(String.class, "'[{\\'java.lang.String\\':\\'java.lang.Integer\\'}]'")
			.test(Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class'.  Value=[{'java.lang.String':'java.lang.Integer'}].")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value=[{'java.lang.String':'java.lang.Integer'}].")
			.test(String[].class, "['{\\'java.lang.String\\':\\'java.lang.Integer\\'}']")
			.test(Class[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.lang.Class[]'.  Value=[{'java.lang.String':'java.lang.Integer'}].")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value=[{'java.lang.String':'java.lang.Integer'}].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=[{'java.lang.String':'java.lang.Integer'}].")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value=[{'java.lang.String':'java.lang.Integer'}].")
		;

		//--------------------------------------------------------------------------------
		// Namespace
		//--------------------------------------------------------------------------------
		final Namespace n = new Namespace("foo","bar");
		new ConversionTest(pName, Arrays.asList(n))
			.test(String.class, "'[{name:\\'foo\\',uri:\\'bar\\'}]'")
			.test(Namespace.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'org.apache.juneau.xml.Namespace'.  Value=[{name:'foo',uri:'bar'}].");

		//--------------------------------------------------------------------------------
		// Namespace[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new Namespace[]{n})
			.test(String.class, "'[{name:\\'foo\\',uri:\\'bar\\'}]'")
			.test(Namespace[].class, "[{name:'foo',uri:'bar'}]");

		//--------------------------------------------------------------------------------
		// Map<Namespace,Namespace>
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new LinkedHashMap<Namespace,Namespace>(){{put(n,n);}})
			.testMap(Namespace.class, Namespace.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<org.apache.juneau.xml.Namespace,org.apache.juneau.xml.Namespace>'.  Value=[{'{name:\\'foo\\',uri:\\'bar\\'}':{name:'foo',uri:'bar'}}].")
			.testMap(String.class, String.class, "Could not retrieve config property 'A.a.set'.  Invalid data conversion from type 'java.util.concurrent.ConcurrentSkipListSet' to type 'java.util.LinkedHashMap<java.lang.String,java.lang.String>'.  Value=[{'{name:\\'foo\\',uri:\\'bar\\'}':{name:'foo',uri:'bar'}}].");
	}


	//====================================================================================================
	// Conversions on map properties
	//====================================================================================================
	@Test
	@SuppressWarnings({ "serial" })
	public void testConversionsOnMapProperties() throws Exception {
		String pName = "A.a.map";

		//--------------------------------------------------------------------------------
		// boolean
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, true)
			.test(boolean.class, "Cannot put value true (java.lang.Boolean) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// int
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, 123)
			.test(int.class, "Cannot put value 123 (java.lang.Integer) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// Class
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, String.class)
			.test(Class.class, "Cannot put value 'java.lang.String' (java.lang.Class) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// String
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, "foo")
			.test(String.class, "Cannot put value 'foo' (java.lang.String) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// enum
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, TestEnum.ONE)
			.test(TestEnum.class, "Cannot put value 'ONE' (org.apache.juneau.CT_ContextFactory$TestEnum) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// String[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new String[]{"foo","bar"})
			.test(String[].class, "Cannot put value ['foo','bar'] (java.lang.String[]) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// Class[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new Class[]{String.class,Integer.class})
			.test(Class[].class, "Cannot put value ['java.lang.String','java.lang.Integer'] (java.lang.Class[]) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// enum[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new TestEnum[]{TestEnum.ONE,TestEnum.TWO})
			.test(TestEnum[].class, "Cannot put value ['ONE','TWO'] (org.apache.juneau.CT_ContextFactory$TestEnum[]) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// Map<String,String>
		//--------------------------------------------------------------------------------
		LinkedHashMap<String,String> m1 = new LinkedHashMap<String,String>();
		m1.put("foo","bar");
		new ConversionTest(pName, m1)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'int'.  Value={foo:'bar'}.")
			.test(String.class, "'{foo:\\'bar\\'}'")
			.test(Class.class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'java.lang.Class'.  Value={foo:'bar'}.")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value={foo:'bar'}.")
			.test(String[].class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'java.lang.String[]'.  Value={foo:'bar'}.")
			.test(Class[].class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'java.lang.Class[]'.  Value={foo:'bar'}.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value={foo:'bar'}.")
			.testMap(String.class, String.class, "{foo:'bar'}")
			.testMap(Class.class, Class.class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'java.util.LinkedHashMap<java.lang.Class,java.lang.Class>'.  Value={foo:'bar'}.")
		;

		//--------------------------------------------------------------------------------
		// Map<Class,Class>
		//--------------------------------------------------------------------------------
		LinkedHashMap<Class,Class> m2 = new LinkedHashMap<Class,Class>();
		m2.put(String.class, Integer.class);
		new ConversionTest(pName, m2)
			.test(boolean.class, "false")
			.test(int.class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'int'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(String.class, "'{\\'java.lang.String\\':\\'java.lang.Integer\\'}'")
			.test(Class.class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'java.lang.Class'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(TestEnum.class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(String[].class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'java.lang.String[]'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(Class[].class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'java.lang.Class[]'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.test(TestEnum[].class, "Could not retrieve config property 'A.a.map'.  Invalid data conversion from type 'java.util.Collections$SynchronizedMap' to type 'org.apache.juneau.CT_ContextFactory$TestEnum[]'.  Value={'java.lang.String':'java.lang.Integer'}.")
			.testMap(String.class, String.class, "{'java.lang.String':'java.lang.Integer'}")
			.testMap(Class.class, Class.class, "{'java.lang.String':'java.lang.Integer'}")
		;

		//--------------------------------------------------------------------------------
		// Namespace
		//--------------------------------------------------------------------------------
		final Namespace n = new Namespace("foo","bar");
		new ConversionTest(pName, Arrays.asList(n))
			.test(String.class, "Cannot put value [{name:'foo',uri:'bar'}] (java.util.Arrays$ArrayList) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// Namespace[]
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new Namespace[]{n})
			.test(String.class, "Cannot put value [{name:'foo',uri:'bar'}] (org.apache.juneau.xml.Namespace[]) to property 'A.a.map' (MAP).")
		;

		//--------------------------------------------------------------------------------
		// Map<Namespace,Namespace>
		//--------------------------------------------------------------------------------
		new ConversionTest(pName, new LinkedHashMap<Namespace,Namespace>(){{put(n,n);}})
			.testMap(Namespace.class, Namespace.class, "{'{name:\\'foo\\',uri:\\'bar\\'}':{name:'foo',uri:'bar'}}")
			.testMap(String.class, String.class, "{'{name:\\'foo\\',uri:\\'bar\\'}':'{name:\\'foo\\',uri:\\'bar\\'}'}");
	}

	public enum TestEnum {
		ONE,TWO,TREE;
	}

	//====================================================================================================
	// testSystemPropertyDefaults()
	//====================================================================================================
	@Test
	public void testSystemPropertyDefaults() {
		System.setProperty("Foo.f1", "true");
		System.setProperty("Foo.f2", "123");
		System.setProperty("Foo.f3", "TWO");

		ContextFactory f = ContextFactory.create();

		assertObjectEquals("true", f.getProperty("Foo.f1", boolean.class, false));
		assertObjectEquals("123", f.getProperty("Foo.f2", int.class, 0));
		assertObjectEquals("'TWO'", f.getProperty("Foo.f3", TestEnum.class, TestEnum.ONE));

		f.setProperty("Foo.f1", false);
		f.setProperty("Foo.f2", 456);
		f.setProperty("Foo.f3", TestEnum.TREE);

		assertObjectEquals("false", f.getProperty("Foo.f1", boolean.class, false));
		assertObjectEquals("456", f.getProperty("Foo.f2", int.class, 0));
		assertObjectEquals("'TREE'", f.getProperty("Foo.f3", TestEnum.class, TestEnum.ONE));
	}

}