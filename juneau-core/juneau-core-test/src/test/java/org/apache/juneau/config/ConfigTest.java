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
import static org.apache.juneau.config.ConfigMod.*;
import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.config.encode.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;
import org.junit.*;

public class ConfigTest {

	private ConfigBuilder cb = new ConfigBuilder().store(ConfigMemoryStore.DEFAULT).name("Test.cfg");
	
	private Config init(String...lines) {
		ConfigMemoryStore.DEFAULT.update("Test.cfg", lines);
		return cb.build().rollback();
	}

	//====================================================================================================
	//	public String get(String key) {
	//====================================================================================================
	@Test
	public void get() throws Exception {
		Config c = init("a=1", "[S]", "b=2");
		
		assertEquals("1", c.get("a"));
		assertEquals("1", c.get("a"));
		assertEquals("2", c.get("S/b"));
		assertNull(c.get("b"));
		assertNull(c.get("S/c"));
		assertNull(c.get("T/d"));
		
		try {
			assertNull(c.get(null));
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'key' cannot be null.", e.getLocalizedMessage());
		}
		c.close();
	}

	//====================================================================================================
	//	public Config set(String key, String value) {
	//====================================================================================================
	@Test
	public void set1() throws Exception {
		Config c = init("a1=1", "[S]", "b1=1");
		
		c.set("a1", "2");
		c.set("a2", "3");
		c.set("a3", "4");
		c.set("S/b1", "5");
		c.set("S/b2", "6");
		c.set("T/c1", "7");
		
		assertEquals("2", c.get("a1"));
		assertEquals("3", c.get("a2"));
		assertEquals("4", c.get("a3"));
		assertEquals("5", c.get("S/b1"));
		assertEquals("6", c.get("S/b2"));
		assertEquals("7", c.get("T/c1"));
		
		c.commit();
		
		assertEquals("2", c.get("a1"));
		assertEquals("3", c.get("a2"));
		assertEquals("4", c.get("a3"));
		assertEquals("5", c.get("S/b1"));
		assertEquals("6", c.get("S/b2"));
		assertEquals("7", c.get("T/c1"));

		c = cb.build();
		
		assertEquals("2", c.get("a1"));
		assertEquals("3", c.get("a2"));
		assertEquals("4", c.get("a3"));
		assertEquals("5", c.get("S/b1"));
		assertEquals("6", c.get("S/b2"));
		assertEquals("7", c.get("T/c1"));
		
		assertTextEquals("a1 = 2|a2 = 3|a3 = 4|[S]|b1 = 5|b2 = 6|[T]|c1 = 7|", c.toString());
	}

	//====================================================================================================
	//	public Config set(String key, Object value) throws SerializeException {
	//====================================================================================================
	@Test
	public void set2() throws Exception {
		Config c = init("a1=1", "[S]", "b1=1");
		
		c.set("a1", 2);
		c.set("a2", 3);
		c.set("a3", 4);
		c.set("S/b1", 5);
		c.set("S/b2", 6);
		c.set("T/c1", 7);
		
		assertEquals("2", c.get("a1"));
		assertEquals("3", c.get("a2"));
		assertEquals("4", c.get("a3"));
		assertEquals("5", c.get("S/b1"));
		assertEquals("6", c.get("S/b2"));
		assertEquals("7", c.get("T/c1"));
		
		c.commit();
		
		assertEquals("2", c.get("a1"));
		assertEquals("3", c.get("a2"));
		assertEquals("4", c.get("a3"));
		assertEquals("5", c.get("S/b1"));
		assertEquals("6", c.get("S/b2"));
		assertEquals("7", c.get("T/c1"));

		c = cb.build();
		
		assertEquals("2", c.get("a1"));
		assertEquals("3", c.get("a2"));
		assertEquals("4", c.get("a3"));
		assertEquals("5", c.get("S/b1"));
		assertEquals("6", c.get("S/b2"));
		assertEquals("7", c.get("T/c1"));
		
		assertTextEquals("a1 = 2|a2 = 3|a3 = 4|[S]|b1 = 5|b2 = 6|[T]|c1 = 7|", c.toString());
	}
	
	//====================================================================================================
	//	public Config set(String key, Object value, Serializer serializer) throws SerializeException {
	//====================================================================================================
	@Test
	public void set3() throws Exception {
		Config c = init("a1=1", "[S]", "b1=1");
		
		ABean b = new ABean().init();
		c.set("a1", b, UonSerializer.DEFAULT);
		c.set("a2", b, UonSerializer.DEFAULT);
		c.set("a3", b, UonSerializer.DEFAULT);
		c.set("S/b1", b, UonSerializer.DEFAULT);
		c.set("S/b2", b, UonSerializer.DEFAULT);
		c.set("T/c1", b, UonSerializer.DEFAULT);
		
		assertEquals("(foo=bar)", c.get("a1"));
		assertEquals("(foo=bar)", c.get("a2"));
		assertEquals("(foo=bar)", c.get("a3"));
		assertEquals("(foo=bar)", c.get("S/b1"));
		assertEquals("(foo=bar)", c.get("S/b2"));
		assertEquals("(foo=bar)", c.get("T/c1"));
	}
	
	//====================================================================================================
	//	public Config set(String key, Object value, Serializer serializer, ConfigMod[] modifiers, String comment, List<String> preLines) throws SerializeException {
	//====================================================================================================
	@Test
	public void set4() throws Exception {
		Config c = init("a1=1", "[S]", "b1=1");
		
		ABean b = new ABean().init();
		c.set("a1", b, UonSerializer.DEFAULT, ENCODED, "comment", Arrays.asList("#c1","#c2"));
		c.set("a2", b, UonSerializer.DEFAULT, ENCODED, "comment", Arrays.asList("#c1","#c2"));
		c.set("a3", b, UonSerializer.DEFAULT, ENCODED, "comment", Arrays.asList("#c1","#c2"));
		c.set("S/b1", b, UonSerializer.DEFAULT, ENCODED, "comment", Arrays.asList("#c1","#c2"));
		c.set("S/b2", b, UonSerializer.DEFAULT, ENCODED, "comment", Arrays.asList("#c1","#c2"));
		c.set("T/c1", b, UonSerializer.DEFAULT, ENCODED, "comment", Arrays.asList("#c1","#c2"));
		
		assertTextEquals("#c1|#c2|a1* = {RhMWWFIFVksf} # comment|#c1|#c2|a2* = {RhMWWFIFVksf} # comment|#c1|#c2|a3* = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1* = {RhMWWFIFVksf} # comment|#c1|#c2|b2* = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1* = {RhMWWFIFVksf} # comment|", c.toString());
		c.commit();
		assertTextEquals("#c1|#c2|a1* = {RhMWWFIFVksf} # comment|#c1|#c2|a2* = {RhMWWFIFVksf} # comment|#c1|#c2|a3* = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1* = {RhMWWFIFVksf} # comment|#c1|#c2|b2* = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1* = {RhMWWFIFVksf} # comment|", c.toString());
		c = cb.build();
		assertTextEquals("#c1|#c2|a1* = {RhMWWFIFVksf} # comment|#c1|#c2|a2* = {RhMWWFIFVksf} # comment|#c1|#c2|a3* = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1* = {RhMWWFIFVksf} # comment|#c1|#c2|b2* = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1* = {RhMWWFIFVksf} # comment|", c.toString());

		assertEquals("(foo=bar)", c.get("a1"));
		assertEquals("(foo=bar)", c.get("a2"));
		assertEquals("(foo=bar)", c.get("a3"));
		assertEquals("(foo=bar)", c.get("S/b1"));
		assertEquals("(foo=bar)", c.get("S/b2"));
		assertEquals("(foo=bar)", c.get("T/c1"));
	}
	
	//====================================================================================================
	//	public Config remove(String key) {
	//====================================================================================================
	@Test
	public void remove() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1");
		
		c.remove("a1");
		c.remove("a2");
		c.remove("a3");
		c.remove("S/b1");
		c.remove("T/c1");
		
		assertTextEquals("[S]|", c.toString());
		c.commit();
		assertTextEquals("[S]|", c.toString());
		c = cb.build();
		assertTextEquals("[S]|", c.toString());
	}

	//====================================================================================================
	//	public String getString1(String key) {
	//====================================================================================================
	@Test
	public void xgetString1() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		
		assertEquals("1", c.getString("a1"));
		assertEquals("2", c.getString("a2"));
		assertEquals(null, c.getString("a3"));
		assertEquals("1", c.getString("S/b1"));
		assertEquals("", c.getString("S/b2"));
		assertEquals(null, c.getString("S/b3"));
		assertEquals(null, c.getString("T/c1"));
	}

	//====================================================================================================
	//	public String getString(String key, String def) {
	//====================================================================================================
	@Test
	public void getString2() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		
		assertEquals("1", c.getString("a1", "foo"));
		assertEquals("2", c.getString("a2", "foo"));
		assertEquals("foo", c.getString("a3", "foo"));
		assertEquals("1", c.getString("S/b1", "foo"));
		assertEquals("foo", c.getString("S/b2", "foo"));
		assertEquals("foo", c.getString("S/b3", "foo"));
		assertEquals("foo", c.getString("T/c1", "foo"));
	}

	//====================================================================================================
	//	public String[] getStringArray(String key) {
	//====================================================================================================
	@Test
	public void getStringArray1() throws Exception {
		Config c = init("a1=1,2", "a2= 2 , 3 ", "[S]", "b1=1", "b2=");
		
		assertObjectEquals("['1','2']", c.getStringArray("a1"));
		assertObjectEquals("['2','3']", c.getStringArray("a2"));
		assertObjectEquals("[]", c.getStringArray("a3"));
		assertObjectEquals("['1']", c.getStringArray("S/b1"));
		assertObjectEquals("[]", c.getStringArray("S/b2"));
		assertObjectEquals("[]", c.getStringArray("S/b3"));
		assertObjectEquals("[]", c.getStringArray("T/c1"));
	}

	//====================================================================================================
	//	public String[] getStringArray(String key, String[] def) {
	//====================================================================================================
	@Test
	public void getStringArray2() throws Exception {
		Config c = init("a1=1,2", "a2= 2 , 3 ", "[S]", "b1=1", "b2=");
		
		assertObjectEquals("['1','2']", c.getStringArray("a1", new String[] {"foo"}));
		assertObjectEquals("['2','3']", c.getStringArray("a2", new String[] {"foo"}));
		assertObjectEquals("['foo']", c.getStringArray("a3", new String[] {"foo"}));
		assertObjectEquals("['1']", c.getStringArray("S/b1", new String[] {"foo"}));
		assertObjectEquals("['foo']", c.getStringArray("S/b2", new String[] {"foo"}));
		assertObjectEquals("['foo']", c.getStringArray("S/b3", new String[] {"foo"}));
		assertObjectEquals("['foo']", c.getStringArray("T/c1", new String[] {"foo"}));
	}

	//====================================================================================================
	//	public int getInt(String key) {
	//====================================================================================================
	@Test
	public void getInt1() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		
		assertEquals(1, c.getInt("a1"));
		assertEquals(2, c.getInt("a2"));
		assertEquals(0, c.getInt("a3"));
		assertEquals(1, c.getInt("S/b1"));
		assertEquals(0, c.getInt("S/b2"));
		assertEquals(0, c.getInt("S/b3"));
		assertEquals(0, c.getInt("T/c1"));
	}

	@Test
	public void getInt1BadValues() throws Exception {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		
		try {
			c.getInt("a1");
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getInt("a2");
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getInt("a3");
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getInt("a4");
			fail();
		} catch (NumberFormatException e) {}
	}

	//====================================================================================================
	//	public int getInt2(String key, int def) {
	//====================================================================================================
	@Test
	public void getInt2() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		
		assertEquals(1, c.getInt("a1", -1));
		assertEquals(2, c.getInt("a2", -1));
		assertEquals(-1, c.getInt("a3", -1));
		assertEquals(1, c.getInt("S/b1", -1));
		assertEquals(-1, c.getInt("S/b2", -1));
		assertEquals(-1, c.getInt("S/b3", -1));
		assertEquals(-1, c.getInt("T/c1", -1));
	}

	@Test
	public void getInt2BadValues() throws Exception {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		
		try {
			c.getInt("a1", -1);
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getInt("a2", -1);
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getInt("a3", -1);
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getInt("a4", -1);
			fail();
		} catch (NumberFormatException e) {}
	}

	//====================================================================================================
	//	public boolean getBoolean(String key) {
	//====================================================================================================
	@Test
	public void getBoolean1() throws Exception {
		Config c = init("a1=true", "a2=false", "[S]", "b1=TRUE", "b2=");
		
		assertEquals(true, c.getBoolean("a1"));
		assertEquals(false, c.getBoolean("a2"));
		assertEquals(false, c.getBoolean("a3"));
		assertEquals(true, c.getBoolean("S/b1"));
		assertEquals(false, c.getBoolean("S/b2"));
		assertEquals(false, c.getBoolean("S/b3"));
		assertEquals(false, c.getBoolean("T/c1"));
	}

	@Test
	public void getBoolean1BadValues() throws Exception {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=T");
		
		assertEquals(false, c.getBoolean("a1"));
		assertEquals(false, c.getBoolean("a2"));
		assertEquals(false, c.getBoolean("a3"));
		assertEquals(false, c.getBoolean("a4"));
	}

	//====================================================================================================
	//	public boolean getBoolean(String key, boolean def) {
	//====================================================================================================
	@Test
	public void getBoolean2() throws Exception {
		Config c = init("a1=true", "a2=false", "[S]", "b1=TRUE", "b2=");
		
		assertEquals(true, c.getBoolean("a1", true));
		assertEquals(false, c.getBoolean("a2", true));
		assertEquals(true, c.getBoolean("a3", true));
		assertEquals(true, c.getBoolean("S/b1", true));
		assertEquals(true, c.getBoolean("S/b2", true));
		assertEquals(true, c.getBoolean("S/b3", true));
		assertEquals(true, c.getBoolean("T/c1", true));
	}

	@Test
	public void getBoolean2BadValues() throws Exception {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=T");
		
		assertEquals(false, c.getBoolean("a1", true));
		assertEquals(false, c.getBoolean("a2", true));
		assertEquals(false, c.getBoolean("a3", true));
		assertEquals(false, c.getBoolean("a4", true));
	}

	//====================================================================================================
	//	public long getLong(String key) {
	//====================================================================================================
	@Test
	public void getLong1() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		
		assertEquals(1l, c.getLong("a1"));
		assertEquals(2l, c.getLong("a2"));
		assertEquals(0l, c.getLong("a3"));
		assertEquals(1l, c.getLong("S/b1"));
		assertEquals(0l, c.getLong("S/b2"));
		assertEquals(0l, c.getLong("S/b3"));
		assertEquals(0l, c.getLong("T/c1"));
	}

	@Test
	public void getLong1BadValues() throws Exception {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		
		try {
			c.getLong("a1");
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getLong("a2");
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getLong("a3");
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getLong("a4");
			fail();
		} catch (NumberFormatException e) {}
	}

	//====================================================================================================
	//	public long getLong(String key, long def) {
	//====================================================================================================
	@Test
	public void getLong2() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		
		assertEquals(1l, c.getLong("a1", Long.MAX_VALUE));
		assertEquals(2l, c.getLong("a2", Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.getLong("a3", Long.MAX_VALUE));
		assertEquals(1l, c.getLong("S/b1", Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.getLong("S/b2", Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.getLong("S/b3", Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.getLong("T/c1", Long.MAX_VALUE));
	}

	@Test
	public void getLong2BadValues() throws Exception {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		
		try {
			c.getLong("a1", -1l);
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getLong("a2", -1l);
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getLong("a3", -1l);
			fail();
		} catch (NumberFormatException e) {}
		try {
			c.getLong("a4", -1l);
			fail();
		} catch (NumberFormatException e) {}
	}

	//====================================================================================================
	//	public boolean getBytes(String key) {
	//====================================================================================================
	@Test
	public void getBytes1() throws Exception {
		Config c = init("a1=Zm9v", "a2=Zm", "\t9v", "a3=");
		
		assertObjectEquals("[102,111,111]", c.getBytes("a1"));
		assertObjectEquals("[102,111,111]", c.getBytes("a2"));
		assertObjectEquals("[]", c.getBytes("a3"));
		assertNull(null, c.getBytes("a4"));
	}

	//====================================================================================================
	//	public boolean getBytes(String key, byte[] def) {
	//====================================================================================================
	@Test
	public void getBytes2() throws Exception {
		Config c = init("a1=Zm9v", "a2=Zm", "\t9v", "a3=");
		
		assertObjectEquals("[102,111,111]", c.getBytes("a1", new byte[] {1}));
		assertObjectEquals("[102,111,111]", c.getBytes("a2", new byte[] {1}));
		assertObjectEquals("[1]", c.getBytes("a3", new byte[] {1}));
		assertObjectEquals("[1]", c.getBytes("a4", new byte[] {1}));
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Type type, Type...args) throws ParseException {
	//====================================================================================================
	@Test
	public void getObject1() throws Exception {
		Config c = init(
			"a1={foo:123}", 
			"a2=[{foo:123}]", 
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
			);
		
		Map<String,Integer> a1 = c.getObject("a1", Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		List<Map<String,Integer>> a2a = c.getObject("a2", List.class, Map.class, String.class, Integer.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, a2a.get(0).keySet().iterator().next());
		assertType(Integer.class, a2a.get(0).values().iterator().next());

		List<ABean> a2b = c.getObject("a2", List.class, ABean.class);
		assertObjectEquals("[{foo:'123'}]", a2b);
		assertType(ABean.class, a2b.get(0));
		
		Map<String,Integer> a3 = c.getObject("a3", Map.class, String.class, Integer.class);
		assertNull(a3);
		
		Map<String,Integer> a4a = c.getObject("a4", Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		ABean a4b = c.getObject("a4", ABean.class);
		assertObjectEquals("{foo:'123'}", a4b);
		assertType(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Parser parser, Type type, Type...args) throws ParseException {
	//====================================================================================================
	@Test
	public void getObject2() throws Exception {
		Config c = init(
			"a1=(foo=123)", 
			"a2=@((foo=123))", 
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
			);
		
		Map<String,Integer> a1 = c.getObject("a1", UonParser.DEFAULT, Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		List<Map<String,Integer>> a2a = c.getObject("a2", UonParser.DEFAULT, List.class, Map.class, String.class, Integer.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, a2a.get(0).keySet().iterator().next());
		assertType(Integer.class, a2a.get(0).values().iterator().next());

		List<ABean> a2b = c.getObject("a2", UonParser.DEFAULT, List.class, ABean.class);
		assertObjectEquals("[{foo:'123'}]", a2b);
		assertType(ABean.class, a2b.get(0));
		
		Map<String,Integer> a3 = c.getObject("a3", UonParser.DEFAULT, Map.class, String.class, Integer.class);
		assertNull(a3);
		
		Map<String,Integer> a4a = c.getObject("a4", UonParser.DEFAULT, Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		ABean a4b = c.getObject("a4", UonParser.DEFAULT, ABean.class);
		assertObjectEquals("{foo:'123'}", a4b);
		assertType(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Class<T> type) throws ParseException {
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void getObject3() throws Exception {
		Config c = init(
			"a1={foo:123}", 
			"a2=[{foo:123}]", 
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
			);
		
		Map a1 = c.getObject("a1", Map.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		List a2a = c.getObject("a2", List.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertType(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		Map a3 = c.getObject("a3", Map.class);
		assertNull(a3);
		
		Map a4a = c.getObject("a4", Map.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		ABean a4b = c.getObject("a4", ABean.class);
		assertObjectEquals("{foo:'123'}", a4b);
		assertType(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Parser parser, Class<T> type) throws ParseException {
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void getObject4() throws Exception {
		Config c = init(
			"a1=(foo=123)", 
			"a2=@((foo=123))", 
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);
		
		Map a1 = c.getObject("a1", UonParser.DEFAULT, Map.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		List a2a = c.getObject("a2", UonParser.DEFAULT, List.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertType(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		Map a3 = c.getObject("a3", UonParser.DEFAULT, Map.class);
		assertNull(a3);
		
		Map a4a = c.getObject("a4", UonParser.DEFAULT, Map.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		ABean a4b = c.getObject("a4", UonParser.DEFAULT, ABean.class);
		assertObjectEquals("{foo:'123'}", a4b);
		assertType(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, T def, Class<T> type) throws ParseException {
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void getObjectWithDefault1() throws Exception {
		Config c = init(
			"a1={foo:123}", 
			"a2=[{foo:123}]", 
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
		);
		
		Map a1 = c.getObjectWithDefault("a1", new ObjectMap(), Map.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		Map a1b = c.getObjectWithDefault("a1b", new ObjectMap(), Map.class);
		assertObjectEquals("{}", a1b);

		List a2a = c.getObjectWithDefault("a2", new ObjectList(), List.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertType(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		List a2b = c.getObjectWithDefault("a2b", new ObjectList(), List.class);
		assertObjectEquals("[]", a2b);

		Map a3 = c.getObjectWithDefault("a3", new ObjectMap(), Map.class);
		assertObjectEquals("{}", a3);
		
		Map a4a = c.getObjectWithDefault("a4", new ObjectMap(), Map.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		Map a4b = c.getObjectWithDefault("a4b", new ObjectMap(), Map.class);
		assertObjectEquals("{}", a4b);

		ABean a4c = c.getObjectWithDefault("a4c", new ABean().init(), ABean.class);
		assertObjectEquals("{foo:'bar'}", a4c);
		assertType(ABean.class, a4c);
	}

	//====================================================================================================
	@SuppressWarnings("rawtypes")
	//	public <T> T getObjectWithDefault(String key, Parser parser, T def, Class<T> type) throws ParseException {
	//====================================================================================================
	@Test
	public void getObjectWithDefault2() throws Exception {
		Config c = init(
			"a1=(foo=123)", 
			"a2=@((foo=123))", 
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);
		
		Map a1 = c.getObjectWithDefault("a1", UonParser.DEFAULT, new ObjectMap(), Map.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		Map a1b = c.getObjectWithDefault("a1b", UonParser.DEFAULT, new ObjectMap(), Map.class);
		assertObjectEquals("{}", a1b);

		List a2a = c.getObjectWithDefault("a2", UonParser.DEFAULT, new ObjectList(), List.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertType(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		List a2b = c.getObjectWithDefault("a2b", UonParser.DEFAULT, new ObjectList(), List.class);
		assertObjectEquals("[]", a2b);

		Map a3 = c.getObjectWithDefault("a3", UonParser.DEFAULT, new ObjectMap(), Map.class);
		assertObjectEquals("{}", a3);
		
		Map a4a = c.getObjectWithDefault("a4", UonParser.DEFAULT, new ObjectMap(), Map.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		Map a4b = c.getObjectWithDefault("a4b", UonParser.DEFAULT, new ObjectMap(), Map.class);
		assertObjectEquals("{}", a4b);

		ABean a4c = c.getObjectWithDefault("a4c", UonParser.DEFAULT, new ABean().init(), ABean.class);
		assertObjectEquals("{foo:'bar'}", a4c);
		assertType(ABean.class, a4c);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, T def, Type type, Type...args) throws ParseException {
	//====================================================================================================
	@Test
	public void getObjectWithDefault3() throws Exception {
		Config c = init(
			"a1={foo:123}", 
			"a2=[{foo:123}]", 
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
		);
		
		Map<String,Integer> a1 = c.getObjectWithDefault("a1", new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		Map<String,Integer> a1b = c.getObjectWithDefault("a1b", new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{}", a1b);

		List<Map<String,Integer>> a2a = c.getObjectWithDefault("a2", new ArrayList<Map<String,Integer>>(), List.class, Map.class, String.class, Integer.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, a2a.get(0).keySet().iterator().next());
		assertType(Integer.class, a2a.get(0).values().iterator().next());

		List<ABean> a2b = c.getObjectWithDefault("a2b", new ArrayList<ABean>(), List.class, ABean.class);
		assertObjectEquals("[]", a2b);

		Map<String,Object> a3 = c.getObjectWithDefault("a3", new ObjectMap(), Map.class, String.class, Object.class);
		assertObjectEquals("{}", a3);
		
		Map<String,Integer> a4a = c.getObjectWithDefault("a4", new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		Map<String,Integer> a4b = c.getObjectWithDefault("a4b", new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{}", a4b);

		ABean a4c = c.getObjectWithDefault("a4c", new ABean().init(), ABean.class);
		assertObjectEquals("{foo:'bar'}", a4c);
		assertType(ABean.class, a4c);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, Parser parser, T def, Type type, Type...args) throws ParseException {
	//====================================================================================================
	@Test
	public void getObjectWithDefault4() throws Exception {
		Config c = init(
			"a1=(foo=123)", 
			"a2=@((foo=123))", 
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);
		
		Map<String,Integer> a1 = c.getObjectWithDefault("a1", UonParser.DEFAULT, new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a1);
		assertType(String.class, a1.keySet().iterator().next());
		assertType(Integer.class, a1.values().iterator().next());
		
		Map<String,Integer> a1b = c.getObjectWithDefault("a1b", UonParser.DEFAULT, new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{}", a1b);

		List<Map<String,Integer>> a2a = c.getObjectWithDefault("a2", UonParser.DEFAULT, new ArrayList<Map<String,Integer>>(), List.class, Map.class, String.class, Integer.class);
		assertObjectEquals("[{foo:123}]", a2a);
		assertType(String.class, a2a.get(0).keySet().iterator().next());
		assertType(Integer.class, a2a.get(0).values().iterator().next());

		List<ABean> a2b = c.getObjectWithDefault("a2b", UonParser.DEFAULT, new ArrayList<ABean>(), List.class, ABean.class);
		assertObjectEquals("[]", a2b);

		Map<String,Object> a3 = c.getObjectWithDefault("a3", UonParser.DEFAULT, new ObjectMap(), Map.class, String.class, Object.class);
		assertObjectEquals("{}", a3);
		
		Map<String,Integer> a4a = c.getObjectWithDefault("a4", UonParser.DEFAULT, new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{foo:123}", a4a);
		assertType(String.class, a4a.keySet().iterator().next());
		assertType(Integer.class, a4a.values().iterator().next());
		
		Map<String,Integer> a4b = c.getObjectWithDefault("a4b", UonParser.DEFAULT, new HashMap<String,Integer>(), Map.class, String.class, Integer.class);
		assertObjectEquals("{}", a4b);

		ABean a4c = c.getObjectWithDefault("a4c", UonParser.DEFAULT, new ABean().init(), ABean.class);
		assertObjectEquals("{foo:'bar'}", a4c);
		assertType(ABean.class, a4c);
	}

	//====================================================================================================
	//	public Set<String> getKeys(String section) {
	//====================================================================================================
	@Test
	public void getKeys() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		
		assertObjectEquals("['a1','a2']", c.getKeys(""));
		assertObjectEquals("['a1','a2']", c.getKeys(""));
		assertObjectEquals("['b1','b2']", c.getKeys("S"));
		assertTrue(c.getKeys("T").isEmpty());
		
		try {
			c.getKeys(null);
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getMessage());
		}
	}

	//====================================================================================================
	//	public Config writeProperties(String section, Object bean, boolean ignoreUnknownProperties) throws ParseException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	//====================================================================================================
	@Test
	public void writeProperties() throws Exception {
		ABean a = new ABean().init();
		BBean b = new BBean().init();
		
		Config c = init("foo=qux", "[S]", "foo=baz", "bar=baz");
		c.writeProperties("S", a, true);
		assertObjectEquals("{foo:'baz'}", a);
		c.writeProperties("S", b, true);
		assertObjectEquals("{foo:'baz'}", b);

		try {
			c.writeProperties("S", a, false);
			fail();
		} catch (ParseException e) {
			assertEquals("Unknown property 'bar' encountered in configuration section 'S'.", e.getMessage());
		}

		try {
			c.writeProperties("S", b, false);
			fail();
		} catch (ParseException e) {
			assertEquals("Unknown property 'bar' encountered in configuration section 'S'.", e.getMessage());
		}

		try {
			c.writeProperties("T", a, false);
		} catch (IllegalArgumentException e) {
			assertEquals("Section 'T' not found in configuration.", e.getMessage());
		}

		c.writeProperties("", b, true);
		assertObjectEquals("{foo:'qux'}", b);
		c.writeProperties("", a, true);
		assertObjectEquals("{foo:'qux'}", a);

		try {
			c.writeProperties(null, a, true);
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getLocalizedMessage());
		}
	}

	//====================================================================================================
	//	public <T> T getSectionAsBean(String sectionName, Class<T>c) throws ParseException {
	//====================================================================================================
	@Test
	public void getSectionAsBean1() throws Exception {
		Config c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");
		
		ABean a = null;
		BBean b = null;
		
		a = c.getSectionAsBean("", ABean.class);
		assertObjectEquals("{foo:'qux'}", a);
		a = c.getSectionAsBean("", ABean.class);
		assertObjectEquals("{foo:'qux'}", a);
		a = c.getSectionAsBean("S", ABean.class);
		assertObjectEquals("{foo:'baz'}", a);
		
		b = c.getSectionAsBean("", BBean.class);
		assertObjectEquals("{foo:'qux'}", b);
		b = c.getSectionAsBean("", BBean.class);
		assertObjectEquals("{foo:'qux'}", b);
		b = c.getSectionAsBean("S", BBean.class);
		assertObjectEquals("{foo:'baz'}", b);

		try {
			a = c.getSectionAsBean("T", ABean.class);
			fail();
		} catch (ParseException e) {
			assertEquals("Unknown property 'bar' encountered in configuration section 'T'.", e.getMessage());
		}

		try {
			b = c.getSectionAsBean("T", BBean.class);
			fail();
		} catch (ParseException e) {
			assertEquals("Unknown property 'bar' encountered in configuration section 'T'.", e.getMessage());
		}

		try {
			a = c.getSectionAsBean("U", ABean.class);
		} catch (IllegalArgumentException e) {
			assertEquals("Section 'U' not found in configuration.", e.getMessage());
		}
		
		try {
			c.getSectionAsBean(null, ABean.class);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getMessage());
		}
		
		try {
			c.getSectionAsBean(null, BBean.class);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getMessage());
		}
	}

	//====================================================================================================
	//	public <T> T getSectionAsBean(String section, Class<T> c, boolean ignoreUnknownProperties) throws ParseException {
	//====================================================================================================
	@Test
	public void getSectionAsBean2() throws Exception {
		Config c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");
		
		ABean a = null;
		BBean b = null;
		
		a = c.getSectionAsBean("T", ABean.class, true);
		assertObjectEquals("{foo:'qux'}", a);
		b = c.getSectionAsBean("T", BBean.class, true);
		assertObjectEquals("{foo:'qux'}", b);

		try {
			a = c.getSectionAsBean("T", ABean.class, false);
			fail();
		} catch (ParseException e) {
			assertEquals("Unknown property 'bar' encountered in configuration section 'T'.", e.getMessage());
		}

		try {
			b = c.getSectionAsBean("T", BBean.class, false);
			fail();
		} catch (ParseException e) {
			assertEquals("Unknown property 'bar' encountered in configuration section 'T'.", e.getMessage());
		}

		try {
			a = c.getSectionAsBean("U", ABean.class, true);
		} catch (IllegalArgumentException e) {
			assertEquals("Section 'U' not found in configuration.", e.getMessage());
		}
	}

	//====================================================================================================
	//	public ObjectMap getSectionAsMap(String section) throws ParseException {
	//====================================================================================================
	@Test
	public void getSectionAsMap() throws Exception {
		Config c = init("a=1", "[S]", "b=2", "[T]");
		
		assertObjectEquals("{a:'1'}", c.getSectionAsMap(""));
		assertObjectEquals("{a:'1'}", c.getSectionAsMap(""));
		assertObjectEquals("{b:'2'}", c.getSectionAsMap("S"));
		assertObjectEquals("{}", c.getSectionAsMap("T"));
		assertNull(c.getSectionAsMap("U"));

		try {
			c.getSectionAsMap(null);
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getLocalizedMessage());
		}
	}

	//====================================================================================================
	//	public <T> T getSectionAsInterface(final String sectionName, final Class<T> c) {
	//====================================================================================================
	@Test
	public void getSectionAsInterface() throws Exception {
		Config c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");
		AInterface a = null;
		
		a = c.getSectionAsInterface("", AInterface.class);
		assertEquals("qux", a.getFoo());
	
		a = c.getSectionAsInterface("", AInterface.class);
		assertEquals("qux", a.getFoo());

		a = c.getSectionAsInterface("S", AInterface.class);
		assertEquals("baz", a.getFoo());

		a = c.getSectionAsInterface("T", AInterface.class);
		assertEquals("qux", a.getFoo());

		try {
			c.getSectionAsInterface("T", ABean.class);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Class 'org.apache.juneau.config.ConfigTest$ABean' passed to getSectionAsInterface() is not an interface.", e.getMessage());
		}
		
		try {
			c.getSectionAsInterface(null, AInterface.class);
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getLocalizedMessage());
		}
	}
	
	public static interface AInterface {
		String getFoo();
	}

	//====================================================================================================
	//	public boolean exists(String key) {
	//====================================================================================================
	@Test
	public void exists() throws Exception {
		Config c = init("a=1", "[S]", "b=2", "c=", "[T]");
		
		assertTrue(c.exists("a"));
		assertFalse(c.exists("b"));
		assertTrue(c.exists("S/b"));
		assertFalse(c.exists("S/c"));
		assertFalse(c.exists("T/d"));
		assertFalse(c.exists("U/e"));
	}

	//====================================================================================================
	//	public Config setSection(String name, List<String> preLines) {
	//====================================================================================================
	@Test
	public void setSection1() throws Exception {
		Config c = init();
		
		c.setSection("", Arrays.asList("#C1", "#C2"));
		assertTextEquals("#C1|#C2||", c);

		c.setSection("", Arrays.asList("#C3", "#C4"));
		assertTextEquals("#C3|#C4||", c);
		
		c.setSection("S1", Arrays.asList("", "#C5", "#C6"));
		assertTextEquals("#C3|#C4|||#C5|#C6|[S1]|", c);
		
		c.setSection("S1", null);
		assertTextEquals("#C3|#C4|||#C5|#C6|[S1]|", c);
		
		c.setSection("S1", Collections.<String>emptyList());
		assertTextEquals("#C3|#C4||[S1]|", c);

		try {
			c.setSection(null, Arrays.asList("", "#C5", "#C6"));
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getLocalizedMessage());
		}
	}

	//====================================================================================================
	//	public Config setSection(String name, List<String> preLines, Map<String,Object> contents) throws SerializeException {
	//====================================================================================================
	@Test
	public void setSection2() throws Exception {
		Config c = init();
		ObjectMap m = new ObjectMap().append("a", "b");
		
		c.setSection("", Arrays.asList("#C1", "#C2"), m);
		assertTextEquals("#C1|#C2||a = b|", c);

		c.setSection("", Arrays.asList("#C3", "#C4"), m);
		assertTextEquals("#C3|#C4||a = b|", c);
		
		c.setSection("S1", Arrays.asList("", "#C5", "#C6"), m);
		assertTextEquals("#C3|#C4||a = b||#C5|#C6|[S1]|a = b|", c);
		
		c.setSection("S1", null, m);
		assertTextEquals("#C3|#C4||a = b||#C5|#C6|[S1]|a = b|", c);
		
		c.setSection("S1", Collections.<String>emptyList(), m);
		assertTextEquals("#C3|#C4||a = b|[S1]|a = b|", c);

		try {
			c.setSection(null, Arrays.asList("", "#C5", "#C6"), m);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Field 'section' cannot be null.", e.getLocalizedMessage());
		}
	}
	
	//====================================================================================================
	//	public Config removeSection(String name) {
	//====================================================================================================
	@Test
	public void removeSection() throws Exception {
		Config c = init("a=1", "[S]", "b=2", "c=", "[T]");
		
		c.removeSection("S");
		c.removeSection("T");
		
		assertTextEquals("a=1|", c);
		
		c.removeSection("");
		assertTextEquals("", c);
	}

	//====================================================================================================
	//	public Writer writeTo(Writer w) throws IOException {
	//====================================================================================================
	@Test
	public void writeTo() throws Exception {
		Config c = init("a=1", "[S]", "b=2", "c=", "[T]");
		
		assertTextEquals("a=1|[S]|b=2|c=|[T]|", c.writeTo(new StringWriter()).toString());
	}

	//====================================================================================================
	// testExampleInConfig - Example in Config
	//====================================================================================================
	@Test
	public void testExampleInConfig() throws Exception {

		Config cf = init(
			"# Default section", "key1 = 1", "key2 = true", "key3 = [1,2,3]", "key4 = http://foo", "",
			"# section1", "# Section 1",
			"[section1]", "key1 = 2", "key2 = false", "key3 = [4,5,6]", "key4 = http://bar"
		);

		assertEquals(1, cf.getInt("key1"));
		assertEquals(true, cf.getBoolean("key2"));
		assertEquals(3, cf.getObject("key3", int[].class)[2]);
		assertEquals(6, cf.getObjectWithDefault("xkey3", new int[]{4,5,6}, int[].class)[2]);
		assertEquals(6, cf.getObjectWithDefault("X/key3", new int[]{4,5,6}, int[].class)[2]);
		assertEquals(new URL("http://foo").toString(), cf.getObject("key4", URL.class).toString());

		assertEquals(2, cf.getInt("section1/key1"));
		assertEquals(false, cf.getBoolean("section1/key2"));
		assertEquals(6, cf.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cf.getObject("section1/key4", URL.class).toString());

		cf = init(
			"# Default section",
			"[section1]", "# Section 1"
		);

		cf.set("key1", 1);
		cf.set("key2", true);
		cf.set("key3", new int[]{1,2,3});
		cf.set("key4", new URL("http://foo"));
		cf.set("section1/key1", 2);
		cf.set("section1/key2", false);
		cf.set("section1/key3", new int[]{4,5,6});
		cf.set("section1/key4", new URL("http://bar"));

		cf.commit();

		assertEquals(1, cf.getInt("key1"));
		assertEquals(true, cf.getBoolean("key2"));
		assertEquals(3, cf.getObject("key3", int[].class)[2]);
		assertEquals(new URL("http://foo").toString(), cf.getObject("key4", URL.class).toString());

		assertEquals(2, cf.getInt("section1/key1"));
		assertEquals(false, cf.getBoolean("section1/key2"));
		assertEquals(6, cf.getObject("section1/key3", int[].class)[2]);
		assertEquals(new URL("http://bar").toString(), cf.getObject("section1/key4", URL.class).toString());
	}

	//====================================================================================================
	// testEnum
	//====================================================================================================
	@Test
	public void testEnum() throws Exception {
		Config cf = init(
			"key1 = MINUTES"
		);
		assertEquals(TimeUnit.MINUTES, cf.getObject("key1", TimeUnit.class));

		cf.commit();

		assertEquals(TimeUnit.MINUTES, cf.getObject("key1", TimeUnit.class));
	}

	//====================================================================================================
	// testEncodedValues
	//====================================================================================================
	@Test
	public void testEncodedValues() throws Exception {
		Config cf = init(
			"[s1]", "", "foo* = "
		);

		cf.set("s1/foo", "mypassword");
		
		assertEquals("mypassword", cf.getString("s1/foo"));

		cf.commit();
		String expected = "[s1]||foo* = {AwwJVhwUQFZEMg==}|";

		assertTextEquals(expected, cf);

		assertEquals("mypassword", cf.getString("s1/foo"));

		cf.load(new StringReader("[s1]\nfoo* = mypassword2\n"), true);
		
		assertEquals("mypassword2", cf.getString("s1/foo"));

		cf.set("s1/foo", "mypassword");

		// INI output should be encoded
		StringWriter sw = new StringWriter();
		cf.writeTo(new PrintWriter(sw));
		expected = "[s1]|foo* = {AwwJVhwUQFZEMg==}|";
		assertTextEquals(expected, sw);
	}

	//====================================================================================================
	// public Config encodeEntries() {
	//====================================================================================================
	@Test
	public void testEncodeEntries() throws Exception {
		Config cf = init(
			"[s1]", "", "foo* = mypassword"
		);

		cf.encodeEntries();
		cf.commit();
		String expected = "[s1]||foo* = {AwwJVhwUQFZEMg==}|";
		assertTextEquals(expected, ConfigMemoryStore.DEFAULT.read("Test.cfg"));
	}

	//====================================================================================================
	// testVariables
	//====================================================================================================
	@Test
	public void testVariables() throws Exception {

		Config cf = init(
			"[s1]",
			"f1 = $S{foo}",
			"f2 = $S{foo,bar}",
			"f3 = $S{$S{baz,bing},bar}"
		);

		System.getProperties().remove("foo");
		System.getProperties().remove("bar");
		System.getProperties().remove("baz");
		System.getProperties().remove("bing");

		assertEquals("", cf.getString("s1/f1"));
		assertEquals("bar", cf.getString("s1/f2"));
		assertEquals("bar", cf.getString("s1/f3"));

		System.setProperty("foo", "123");
		assertEquals("123", cf.getString("s1/f1"));
		assertEquals("123", cf.getString("s1/f2"));
		assertEquals("bar", cf.getString("s1/f3"));

		System.setProperty("foo", "$S{bar}");
		System.setProperty("bar", "baz");
		assertEquals("baz", cf.getString("s1/f1"));
		assertEquals("baz", cf.getString("s1/f2"));
		assertEquals("bar", cf.getString("s1/f3"));

		System.setProperty("bing", "$S{foo}");
		assertEquals("baz", cf.getString("s1/f3"));

		System.setProperty("baz", "foo");
		System.setProperty("foo", "123");
		assertEquals("123", cf.getString("s1/f3"));
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
		ConfigXorEncoder e = new ConfigXorEncoder();
		String s = e.encode("", in);
		String s2 = e.decode("", s);
		assertEquals(in, s2);
	}

	//====================================================================================================
	// testHex
	//====================================================================================================
	@Test
	public void testHex() throws Exception {
		Config cf = init().builder().binaryFormat(BinaryFormat.HEX).build();
	
		cf.set("foo", "bar".getBytes("UTF-8"));
		assertEquals("626172", cf.get("foo"));
		assertObjectEquals("[98,97,114]", cf.getBytes("foo"));
	}

	//====================================================================================================
	// testSpacedHex
	//====================================================================================================
	@Test
	public void testSpacedHex() throws Exception {
		Config cf = init().builder().binaryFormat(BinaryFormat.SPACED_HEX).build();
	
		cf.set("foo", "bar".getBytes("UTF-8"));
		assertEquals("62 61 72", cf.get("foo"));
		assertObjectEquals("[98,97,114]", cf.getBytes("foo"));
	}

	//====================================================================================================
	// testMultiLines
	//====================================================================================================
	@Test
	public void testMultiLines() throws Exception {
		Config cf = init(
			"[s1]",
			"f1 = x \n\ty \n\tz"
		);

		assertEquals("x \ny \nz", cf.getString("s1/f1"));

		StringWriter sw = new StringWriter();
		cf.writeTo(sw);
		String expected = "[s1]|f1 = x |\ty |\tz|";
		assertTextEquals(expected, sw);
	}

	//====================================================================================================
	// testNumberShortcuts
	//====================================================================================================
	@Test
	public void testNumberShortcuts() throws Exception {
		Config cf = init(
			"[s1]",
			"f1 = 1M",
			"f2 = 1K",
			"f3 = 1 M",
			"f4 = 1 K"
		);
		assertEquals(1048576, cf.getInt("s1/f1"));
		assertEquals(1024, cf.getInt("s1/f2"));
		assertEquals(1048576, cf.getInt("s1/f3"));
		assertEquals(1024, cf.getInt("s1/f4"));
	}

	//====================================================================================================
	// testListeners
	//====================================================================================================
	@Test
	public void testListeners() throws Exception {
		Config cf = init();

		final Set<String> changes = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

		cf.addListener(
			new ConfigEventListener() {
				@Override
				public void onConfigChange(List<ConfigEvent> events) {
					for (ConfigEvent ce : events) {
						String key = (ce.getSection().equals("") ? "" : (ce.getSection() + '/')) + ce.getKey();
						if (ce.getType() == ConfigEventType.REMOVE_ENTRY) {
							changes.add("REMOVE_ENTRY("+key+")");
						} else if (ce.getType() == ConfigEventType.REMOVE_SECTION) {
							changes.add("REMOVE_SECTION("+ce.getSection()+")");
						} else if (ce.getType() == ConfigEventType.SET_SECTION) {
							changes.add("SET_SECTION("+ce.getSection()+")");
						} else {
							changes.add(key + '=' + ce.getValue());
						}
					}
				}
			}
		);

		// No changes until save.
		changes.clear();
		cf.set("a1", 3);
		cf.set("a3", 3);
		cf.set("B/b1", 3);
		cf.set("B/b3", 3);
		assertObjectEquals("[]", changes);
		cf.commit();
		assertObjectEquals("['a1=3','a3=3','B/b1=3','B/b3=3']", changes);

		// Rollback.
		changes.clear();
		cf.set("a1", 3);
		cf.set("a3", 3);
		cf.set("B/b1", 3);
		cf.set("B/b3", 3);
		assertObjectEquals("[]", changes);
		cf.rollback();
		cf.commit();
		assertObjectEquals("[]", changes);

		// Overwrite
		changes.clear();
		cf.set("a1", "2");
		cf.set("B/b1", "2");
		cf.set("a2", "2");
		cf.set("B/b2", "2");
		cf.set("C/c1", "2");
		cf.set("C/c2", "2");
		cf.commit();
		assertObjectEquals("['a1=2','a2=2','B/b1=2','B/b2=2','C/c1=2','C/c2=2']", changes);

		// Encoded
		changes.clear();
		cf.set("a4", "4", null, ConfigMod.ENCODED, null, null);
		cf.set("B/b4", "4", null, ConfigMod.ENCODED, null, null);
		cf.commit();
		assertObjectEquals("['a4={Wg==}','B/b4={Wg==}']", changes);
		
		// Encoded overwrite
		changes.clear();
		cf.set("a4", "5");
		cf.set("B/b4", "5");
		cf.commit();
		assertObjectEquals("['a4={Ww==}','B/b4={Ww==}']", changes);

		// Remove entries
		changes.clear();
		cf.remove("a4");
		cf.remove("ax");
		cf.remove("B/b4");
		cf.remove("B/bx");
		cf.remove("X/bx");
		cf.commit();
		assertObjectEquals("['REMOVE_ENTRY(a4)','REMOVE_ENTRY(B/b4)']", changes);

		// Add section
		// Shouldn't trigger listener.
		changes.clear();
		cf.setSection("D", Arrays.asList("#comment"));
		cf.commit();
		assertObjectEquals("[]", changes);

		// Add section with contents
		changes.clear();
		cf.setSection("E", null, new AMap<String,Object>().append("e1", "1").append("e2", "2"));
		cf.commit();
		assertObjectEquals("['E/e1=1','E/e2=2']", changes);

		// Remove section
		changes.clear();
		cf.removeSection("E");
		cf.commit();
		assertObjectEquals("['REMOVE_ENTRY(E/e1)','REMOVE_ENTRY(E/e2)']", changes);

		// Remove non-existent section
		changes.clear();
		cf.removeSection("E");
		cf.commit();
		assertObjectEquals("[]", changes);
	}

	//====================================================================================================
	// getObjectArray(Class c, String key)
	// getObjectArray(Class c, String key, T[] def)
	//====================================================================================================
	@Test
	public void testGetObjectArray() throws Exception {
		Config cf = init("[A]", "a1=[1,2,3]");
		assertObjectEquals("[1,2,3]", cf.getObject("A/a1", Integer[].class));
		assertObjectEquals("[4,5,6]", cf.getObjectWithDefault("A/a2", new Integer[]{4,5,6}, Integer[].class));
		assertObjectEquals("[7,8,9]", cf.getObjectWithDefault("B/a1", new Integer[]{7,8,9}, Integer[].class));
		assertNull(cf.getObject("B/a1", Integer[].class));

		cf = init("[A]", "a1 = [1 ,\n\t2 ,\n\t3] ");
		assertObjectEquals("[1,2,3]", cf.getObject("A/a1", Integer[].class));

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
		cf = init("x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "[A]", "a1=[1,2,3]");
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
		Config cf = init("[A]", "a1=1,2,3");
		assertObjectEquals("['1','2','3']", cf.getStringArray("A/a1"));
		assertObjectEquals("['4','5','6']", cf.getStringArray("A/a2", new String[]{"4","5","6"}));
		assertObjectEquals("['7','8','9']", cf.getStringArray("B/a1", new String[]{"7","8","9"}));
		assertObjectEquals("[]", cf.getStringArray("B/a1"));

		cf = init("[A]", "a1 = 1 ,\n\t2 ,\n\t3 ");
		assertObjectEquals("['1','2','3']", cf.getStringArray("A/a1"));

		System.setProperty("X", "4,5,6");
		cf = init(null, "x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "x4=$S{Y,$S{X}}", "[A]", "a1=1,2,3");
		assertObjectEquals("['1','2','3']", cf.getStringArray("x1", new String[]{"9"}));
		assertObjectEquals("['4','5','6']", cf.getStringArray("x2", new String[]{"9"}));
		assertObjectEquals("['9']", cf.getStringArray("x3", new String[]{"9"}));
		
		System.clearProperty("X");
	}

	//====================================================================================================
	// getSectionMap(String name)
	//====================================================================================================
	@Test
	public void testGetSectionMap() throws Exception {
		Config cf = init("[A]", "a1=1", "", "[D]", "d1=$C{A/a1}","d2=$S{X}");

		assertObjectEquals("{a1:'1'}", cf.getSectionAsMap("A"));
		assertNull(cf.getSectionAsMap("B"));
		assertObjectEquals("null", cf.getSectionAsMap("C"));

		ObjectMap m = cf.getSectionAsMap("A");
		assertObjectEquals("{a1:'1'}", m);

		System.setProperty("X", "x");
		m = cf.getSectionAsMap("D");
		assertObjectEquals("{d1:'1',d2:'x'}", m);
		System.clearProperty("X");
	}

	//====================================================================================================
	// toWritable()
	//====================================================================================================
	@Test
	public void testToWritable() throws Exception {
		Config cf = init("a = b");

		StringWriter sw = new StringWriter();
		cf.writeTo(sw);
		assertTextEquals("a = b|", sw);

		assertEquals("text/plain", cf.getMediaType().toString());
	}


	//====================================================================================================
	// Test resolving with override
	//====================================================================================================
	@Test
	public void testResolvingWithOverride() throws Exception {
		Config cf = init();
		cf.set("a", "$A{X}");
		cf.set("b", "$B{X}");
		cf.set("c", "$A{$B{X}}");
		cf.set("d", "$B{$A{X}}");
		cf.set("e", "$D{X}");

		VarResolver vr = new VarResolverBuilder().defaultVars().vars(ALVar.class, BLVar.class).build();

		cf = cf.resolving(vr.createSession());

		assertEquals("aXa", cf.getString("a"));
		assertEquals("bXb", cf.getString("b"));
		assertEquals("abXba", cf.getString("c"));
		assertEquals("baXab", cf.getString("d"));
		assertEquals("$D{X}", cf.getString("e"));

		// Create new resolver that addx $C and overrides $A
		VarResolver vr2 = vr.builder().vars(AUVar.class, DUVar.class).build();

		// true == augment by adding existing as parent to the new resolver
		cf = cf.resolving(vr2.createSession());
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
	// Ensure pound signs in values are encoded.
	//====================================================================================================
	@Test
	public void testPoundSignEscape() throws Exception {
		Config cf = init();
		cf.set("a", "a,#b,=c");
		cf.set("A/a", "a,#b,=c");

		assertTextEquals("a = a,\\u0023b,=c|[A]|a = a,\\u0023b,=c|", cf);
		cf.commit();
		assertTextEquals("a = a,\\u0023b,=c|[A]|a = a,\\u0023b,=c|", cf);

		assertEquals("a,#b,=c", cf.getString("a"));
		assertEquals("a,#b,=c", cf.getString("A/a"));
		
		cf.set("a", "a,#b,=c", null, (ConfigMod)null, "comment#comment", null);
		assertTextEquals("a = a,\\u0023b,=c # comment#comment|[A]|a = a,\\u0023b,=c|", cf);
		assertEquals("a,#b,=c", cf.getString("a"));
	}
	
	public static void main(String[] args) {
		System.err.println(Integer.parseInt("1_000"));
	}
	
	
	public static class ABean {
		public String foo;
		
		public ABean init() {
			foo = "bar";
			return this;
		}
	}

	public static class BBean {
		private String foo;
		
		public String getFoo() { return foo; }
		public BBean setFoo(String foo) {this.foo = foo; return this;}
		public BBean init() {
			foo = "bar";
			return this;
		}
	}
}