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
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.mod.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.junit.*;

public class ConfigTest {

	private Config.Builder cb = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg");

	private Config init(String...lines) {
		MemoryStore.DEFAULT.update("Test.cfg", lines);
		return cb.build().rollback();
	}

	//====================================================================================================
	//	public String get(String key)
	//====================================================================================================
	@Test
	public void get() {
		Config c = init("a=1", "[S]", "b=2");

		assertEquals("1", c.get("a").get());
		assertEquals("1", c.get("a").get());
		assertEquals("2", c.get("S/b").get());
		assertNull(c.get("b").orElse(null));
		assertNull(c.get("S/c").orElse(null));
		assertNull(c.get("T/d").orElse(null));
		assertThrows(IllegalArgumentException.class, ()->c.get(null), "Argument 'key' cannot be null.");
		c.close();
	}

	//====================================================================================================
	//	public Config set(String key, String value)
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

		assertEquals("2", c.get("a1").get());
		assertEquals("3", c.get("a2").get());
		assertEquals("4", c.get("a3").get());
		assertEquals("5", c.get("S/b1").get());
		assertEquals("6", c.get("S/b2").get());
		assertEquals("7", c.get("T/c1").get());

		c.commit();

		assertEquals("2", c.get("a1").get());
		assertEquals("3", c.get("a2").get());
		assertEquals("4", c.get("a3").get());
		assertEquals("5", c.get("S/b1").get());
		assertEquals("6", c.get("S/b2").get());
		assertEquals("7", c.get("T/c1").get());

		c = cb.build();

		assertEquals("2", c.get("a1").get());
		assertEquals("3", c.get("a2").get());
		assertEquals("4", c.get("a3").get());
		assertEquals("5", c.get("S/b1").get());
		assertEquals("6", c.get("S/b2").get());
		assertEquals("7", c.get("T/c1").get());

		assertString(c).asReplaceAll("\\r?\\n", "|").is("a1 = 2|a2 = 3|a3 = 4|[S]|b1 = 5|b2 = 6|[T]|c1 = 7|");
	}

	//====================================================================================================
	//	public Config set(String key, Object value)
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

		assertEquals("2", c.get("a1").get());
		assertEquals("3", c.get("a2").get());
		assertEquals("4", c.get("a3").get());
		assertEquals("5", c.get("S/b1").get());
		assertEquals("6", c.get("S/b2").get());
		assertEquals("7", c.get("T/c1").get());

		c.commit();

		assertEquals("2", c.get("a1").get());
		assertEquals("3", c.get("a2").get());
		assertEquals("4", c.get("a3").get());
		assertEquals("5", c.get("S/b1").get());
		assertEquals("6", c.get("S/b2").get());
		assertEquals("7", c.get("T/c1").get());

		c = cb.build();

		assertEquals("2", c.get("a1").get());
		assertEquals("3", c.get("a2").get());
		assertEquals("4", c.get("a3").get());
		assertEquals("5", c.get("S/b1").get());
		assertEquals("6", c.get("S/b2").get());
		assertEquals("7", c.get("T/c1").get());

		assertString(c).asReplaceAll("\\r?\\n", "|").is("a1 = 2|a2 = 3|a3 = 4|[S]|b1 = 5|b2 = 6|[T]|c1 = 7|");
	}

	//====================================================================================================
	//	public Config set(String key, Object value, Serializer serializer)
	//====================================================================================================
	@Test
	public void set3() {
		Config c = init("a1=1", "[S]", "b1=1");

		ABean b = new ABean().init();
		c.set("a1", b, UonSerializer.DEFAULT);
		c.set("a2", b, UonSerializer.DEFAULT);
		c.set("a3", b, UonSerializer.DEFAULT);
		c.set("S/b1", b, UonSerializer.DEFAULT);
		c.set("S/b2", b, UonSerializer.DEFAULT);
		c.set("T/c1", b, UonSerializer.DEFAULT);

		assertEquals("(foo=bar)", c.get("a1").get());
		assertEquals("(foo=bar)", c.get("a2").get());
		assertEquals("(foo=bar)", c.get("a3").get());
		assertEquals("(foo=bar)", c.get("S/b1").get());
		assertEquals("(foo=bar)", c.get("S/b2").get());
		assertEquals("(foo=bar)", c.get("T/c1").get());
	}

	//====================================================================================================
	//	public Config set(String key, Object value, Serializer serializer, ConfigMod[] modifiers, String comment, List<String> preLines)
	//====================================================================================================
	@Test
	public void set4() throws Exception {
		Config c = init("a1=1", "[S]", "b1=1");

		ABean b = new ABean().init();
		c.set("a1", b, UonSerializer.DEFAULT, "*", "comment", Arrays.asList("#c1","#c2"));
		c.set("a2", b, UonSerializer.DEFAULT, "*", "comment", Arrays.asList("#c1","#c2"));
		c.set("a3", b, UonSerializer.DEFAULT, "*", "comment", Arrays.asList("#c1","#c2"));
		c.set("S/b1", b, UonSerializer.DEFAULT, "*", "comment", Arrays.asList("#c1","#c2"));
		c.set("S/b2", b, UonSerializer.DEFAULT, "*", "comment", Arrays.asList("#c1","#c2"));
		c.set("T/c1", b, UonSerializer.DEFAULT, "*", "comment", Arrays.asList("#c1","#c2"));

		assertString(c).asReplaceAll("\\r?\\n", "|").is("#c1|#c2|a1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a2<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a3<*> = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|b2<*> = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1<*> = {RhMWWFIFVksf} # comment|");
		c.commit();
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#c1|#c2|a1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a2<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a3<*> = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|b2<*> = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1<*> = {RhMWWFIFVksf} # comment|");
		c = cb.build();
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#c1|#c2|a1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a2<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a3<*> = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|b2<*> = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1<*> = {RhMWWFIFVksf} # comment|");

		assertEquals("(foo=bar)", c.get("a1").get());
		assertEquals("(foo=bar)", c.get("a2").get());
		assertEquals("(foo=bar)", c.get("a3").get());
		assertEquals("(foo=bar)", c.get("S/b1").get());
		assertEquals("(foo=bar)", c.get("S/b2").get());
		assertEquals("(foo=bar)", c.get("T/c1").get());
	}

	//====================================================================================================
	//	public Config remove(String key)
	//====================================================================================================
	@Test
	public void remove() throws Exception {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1");

		c.remove("a1");
		c.remove("a2");
		c.remove("a3");
		c.remove("S/b1");
		c.remove("T/c1");

		assertString(c).asReplaceAll("\\r?\\n", "|").is("[S]|");
		c.commit();
		assertString(c).asReplaceAll("\\r?\\n", "|").is("[S]|");
		c = cb.build();
		assertString(c).asReplaceAll("\\r?\\n", "|").is("[S]|");
	}

	//====================================================================================================
	//	public String getString1(String key)
	//====================================================================================================
	@Test
	public void xgetString1() {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");

		assertEquals("1", c.get("a1").as(String.class).orElse(null));
		assertEquals("2", c.get("a2").as(String.class).orElse(null));
		assertEquals(null, c.get("a3").as(String.class).orElse(null));
		assertEquals("1", c.get("S/b1").as(String.class).orElse(null));
		assertEquals("", c.get("S/b2").as(String.class).orElse(null));
		assertEquals(null, c.get("S/b3").as(String.class).orElse(null));
		assertEquals(null, c.get("T/c1").as(String.class).orElse(null));
	}

	//====================================================================================================
	//	public String getString(String key, String def)
	//====================================================================================================
	@Test
	public void getString2() {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertEquals("1", c.get("a1").orElse("foo"));
		assertEquals("2", c.get("a2").orElse("foo"));
		assertEquals("foo", c.get("a3").orElse("foo"));
		assertEquals("1", c.get("S/b1").orElse("foo"));
		assertEquals("", c.get("S/b2").orElse("foo"));
		assertEquals("foo", c.get("S/b3").orElse("foo"));
		assertEquals("foo", c.get("T/c1").orElse("foo"));
	}

	//====================================================================================================
	//	public String[] getStringArray(String key)
	//====================================================================================================
	@Test
	public void getStringArray1() {
		Config c = init("a1=1,2", "a2= 2 , 3 ", "[S]", "b1=1", "b2=");
		assertObject(c.get("a1").as(String[].class).orElse(null)).asJson().is("['1','2']");
		assertObject(c.get("a2").as(String[].class).orElse(null)).asJson().is("['2','3']");
		assertNull(c.get("a3").as(String[].class).orElse(null));
		assertObject(c.get("S/b1").as(String[].class).orElse(null)).asJson().is("['1']");
		assertObject(c.get("S/b2").as(String[].class).orElse(null)).asJson().is("[]");
		assertNull(c.get("S/b3").as(String[].class).orElse(null));
		assertNull(c.get("T/c1").as(String[].class).orElse(null));
	}

	//====================================================================================================
	//	public String[] getStringArray(String key, String[] def)
	//====================================================================================================
	@Test
	public void getStringArray2() {
		Config c = init("a1=1,2", "a2= 2 , 3 ", "[S]", "b1=1", "b2=");
		assertObject(c.get("a1").asStringArray().orElse(new String[] {"foo"})).asJson().is("['1','2']");
		assertObject(c.get("a2").asStringArray().orElse(new String[] {"foo"})).asJson().is("['2','3']");
		assertObject(c.get("a3").asStringArray().orElse(new String[] {"foo"})).asJson().is("['foo']");
		assertObject(c.get("S/b1").asStringArray().orElse(new String[] {"foo"})).asJson().is("['1']");
		assertObject(c.get("S/b2").asStringArray().orElse(new String[] {"foo"})).asJson().is("[]");
		assertObject(c.get("S/b3").asStringArray().orElse(new String[] {"foo"})).asJson().is("['foo']");
		assertObject(c.get("T/c1").asStringArray().orElse(new String[] {"foo"})).asJson().is("['foo']");
	}

	//====================================================================================================
	//	public int getInt(String key)
	//====================================================================================================
	@Test
	public void getInt1() {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertEquals(1, c.get("a1").asInteger().orElse(0));
		assertEquals(2, c.get("a2").asInteger().orElse(0));
		assertEquals(0, c.get("a3").asInteger().orElse(0));
		assertEquals(1, c.get("S/b1").asInteger().orElse(0));
		assertEquals(0, c.get("S/b2").asInteger().orElse(0));
		assertEquals(0, c.get("S/b3").asInteger().orElse(0));
		assertEquals(0, c.get("T/c1").asInteger().orElse(0));
	}

	@Test
	public void getInt1BadValues() {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		assertThrown(()->c.get("a1").asInteger().orElse(0)).isExists();
		assertThrown(()->c.get("a2").asInteger().orElse(0)).isExists();
		assertThrown(()->c.get("a3").asInteger().orElse(0)).isExists();
		assertThrown(()->c.get("a4").asInteger().orElse(0)).isExists();
	}

	//====================================================================================================
	//	public int getInt2(String key, int def)
	//====================================================================================================
	@Test
	public void getInt2() {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertEquals(1, c.get("a1").asInteger().orElse(-1));
		assertEquals(2, c.get("a2").asInteger().orElse(-1));
		assertEquals(-1, c.get("a3").asInteger().orElse(-1));
		assertEquals(1, c.get("S/b1").asInteger().orElse(-1));
		assertEquals(-1, c.get("S/b2").asInteger().orElse(-1));
		assertEquals(-1, c.get("S/b3").asInteger().orElse(-1));
		assertEquals(-1, c.get("T/c1").asInteger().orElse(-1));
	}

	@Test
	public void getInt2BadValues() {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		assertThrown(()->c.get("a1").asInteger().orElse(-1)).isExists();
		assertThrown(()->c.get("a2").asInteger().orElse(-1)).isExists();
		assertThrown(()->c.get("a3").asInteger().orElse(-1)).isExists();
		assertThrown(()->c.get("a4").asInteger().orElse(-1)).isExists();
	}

	//====================================================================================================
	//	public boolean getBoolean(String key)
	//====================================================================================================
	@Test
	public void getBoolean1() {
		Config c = init("a1=true", "a2=false", "[S]", "b1=TRUE", "b2=");
		assertEquals(true, c.get("a1").asBoolean().orElse(false));
		assertEquals(false, c.get("a2").asBoolean().orElse(false));
		assertEquals(false, c.get("a3").asBoolean().orElse(false));
		assertEquals(true, c.get("S/b1").asBoolean().orElse(false));
		assertEquals(false, c.get("S/b2").asBoolean().orElse(false));
		assertEquals(false, c.get("S/b3").asBoolean().orElse(false));
		assertEquals(false, c.get("T/c1").asBoolean().orElse(false));
	}

	@Test
	public void getBoolean1BadValues() {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=T");
		assertEquals(false, c.get("a1").asBoolean().orElse(false));
		assertEquals(false, c.get("a2").asBoolean().orElse(false));
		assertEquals(false, c.get("a3").asBoolean().orElse(false));
		assertEquals(false, c.get("a4").asBoolean().orElse(false));
	}

	//====================================================================================================
	//	public boolean getBoolean(String key, boolean def)
	//====================================================================================================
	@Test
	public void getBoolean2() {
		Config c = init("a1=true", "a2=false", "[S]", "b1=TRUE", "b2=");
		assertEquals(true, c.get("a1").asBoolean().orElse(true));
		assertEquals(false, c.get("a2").asBoolean().orElse(true));
		assertEquals(true, c.get("a3").asBoolean().orElse(true));
		assertEquals(true, c.get("S/b1").asBoolean().orElse(true));
		assertEquals(true, c.get("S/b2").asBoolean().orElse(true));
		assertEquals(true, c.get("S/b3").asBoolean().orElse(true));
		assertEquals(true, c.get("T/c1").asBoolean().orElse(true));
	}

	@Test
	public void getBoolean2BadValues() {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=T");
		assertEquals(false, c.get("a1").asBoolean().orElse(true));
		assertEquals(false, c.get("a2").asBoolean().orElse(true));
		assertEquals(false, c.get("a3").asBoolean().orElse(true));
		assertEquals(false, c.get("a4").asBoolean().orElse(true));
	}

	//====================================================================================================
	//	public long getLong(String key)
	//====================================================================================================
	@Test
	public void getLong1() {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertLong(c.get("a1").asLong().orElse(0L)).is(1L);
		assertLong(c.get("a2").asLong().orElse(0L)).is(2L);
		assertLong(c.get("a3").asLong().orElse(0L)).is(0L);
		assertLong(c.get("S/b1").asLong().orElse(0L)).is(1L);
		assertLong(c.get("S/b2").asLong().orElse(0L)).is(0L);
		assertLong(c.get("S/b3").asLong().orElse(0L)).is(0L);
		assertLong(c.get("T/c1").asLong().orElse(0L)).is(0L);
	}

	@Test
	public void getLong1BadValues() {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		assertThrown(()->c.get("a1").as(long.class)).isExists();
		assertThrown(()->c.get("a2").as(long.class)).isExists();
		assertThrown(()->c.get("a3").as(long.class)).isExists();
		assertThrown(()->c.get("a4").as(long.class)).isExists();
	}

	//====================================================================================================
	//	public long getLong(String key, long def)
	//====================================================================================================
	@Test
	public void getLong2() {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertLong(c.get("a1").asLong().orElse(Long.MAX_VALUE)).is(1L);
		assertLong(c.get("a2").asLong().orElse(Long.MAX_VALUE)).is(2L);
		assertLong(c.get("a3").asLong().orElse(Long.MAX_VALUE)).is(Long.MAX_VALUE);
		assertLong(c.get("S/b1").asLong().orElse(Long.MAX_VALUE)).is(1L);
		assertLong(c.get("S/b2").asLong().orElse(Long.MAX_VALUE)).is(Long.MAX_VALUE);
		assertLong(c.get("S/b3").asLong().orElse(Long.MAX_VALUE)).is(Long.MAX_VALUE);
		assertLong(c.get("T/c1").asLong().orElse(Long.MAX_VALUE)).is(Long.MAX_VALUE);
	}

	@Test
	public void getLong2BadValues() {
		Config c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");

		assertThrown(()->c.get("a1").asLong().orElse(-1L)).isType(NumberFormatException.class);
		assertThrown(()->c.get("a2").asLong().orElse(-1L)).isType(NumberFormatException.class);
		assertThrown(()->c.get("a3").asLong().orElse(-1L)).isType(NumberFormatException.class);
		assertThrown(()->c.get("a4").asLong().orElse(-1L)).isType(NumberFormatException.class);
	}

	//====================================================================================================
	//	public boolean getBytes(String key)
	//====================================================================================================
	@Test
	public void getBytes1() {
		Config c = init("a1=Zm9v", "a2=Zm", "\t9v", "a3=");

		assertObject(c.get("a1").as(byte[].class)).asJson().is("[102,111,111]");
		assertObject(c.get("a2").as(byte[].class)).asJson().is("[102,111,111]");
		assertObject(c.get("a3").as(byte[].class)).asJson().is("[]");
		assertFalse(c.get("a4").as(byte[].class).isPresent());
	}

	//====================================================================================================
	//	public boolean getBytes(String key, byte[] def)
	//====================================================================================================
	@Test
	public void getBytes2() {
		Config c = init("a1=Zm9v", "a2=Zm", "\t9v", "a3=");

		assertObject(c.get("a1").asBytes().orElse(new byte[] {1})).asJson().is("[102,111,111]");
		assertObject(c.get("a2").asBytes().orElse(new byte[] {1})).asJson().is("[102,111,111]");
		assertObject(c.get("a3").asBytes().orElse(new byte[] {1})).asJson().is("[]");
		assertObject(c.get("a4").asBytes().orElse(new byte[] {1})).asJson().is("[1]");
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test
	public void getObject1() {
		Config c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
			);

		Map<String,Integer> a1 = (Map<String,Integer>) c.get("a1").as(Map.class, String.class, Integer.class).get();
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		List<Map<String,Integer>> a2a = (List<Map<String,Integer>>) c.get("a2").as(List.class, Map.class, String.class, Integer.class).get();
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(a2a.get(0).keySet().iterator().next()).isType(String.class);
		assertObject(a2a.get(0).values().iterator().next()).isType(Integer.class);

		List<ABean> a2b = (List<ABean>) c.get("a2").as(List.class, ABean.class).get();
		assertObject(a2b).asJson().is("[{foo:'123'}]");
		assertObject(a2b.get(0)).isType(ABean.class);

		Map<String,Integer> a3 = (Map<String,Integer>) c.get("a3").as(Map.class, String.class, Integer.class).orElse(null);
		assertNull(a3);

		Map<String,Integer> a4a = (Map<String,Integer>) c.get("a4").as(Map.class, String.class, Integer.class).get();
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		ABean a4b = c.get("a4").as(ABean.class).get();
		assertObject(a4b).asJson().is("{foo:'123'}");
		assertObject(a4b).isType(ABean.class);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Parser parser, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test
	public void getObject2() {
		Config c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
			);

		Map<String,Integer> a1 = (Map<String,Integer>) c.get("a1").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).get();
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		List<Map<String,Integer>> a2a = (List<Map<String,Integer>>) c.get("a2").as(UonParser.DEFAULT, List.class, Map.class, String.class, Integer.class).get();
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(a2a.get(0).keySet().iterator().next()).isType(String.class);
		assertObject(a2a.get(0).values().iterator().next()).isType(Integer.class);

		List<ABean> a2b = (List<ABean>) c.get("a2").as(UonParser.DEFAULT, List.class, ABean.class).get();
		assertObject(a2b).asJson().is("[{foo:'123'}]");
		assertObject(a2b.get(0)).isType(ABean.class);

		Map<String,Integer> a3 = (Map<String,Integer>) c.get("a3").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(null);
		assertNull(a3);

		Map<String,Integer> a4a = (Map<String,Integer>) c.get("a4").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).get();
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		ABean a4b = c.get("a4").as(UonParser.DEFAULT, ABean.class).get();
		assertObject(a4b).asJson().is("{foo:'123'}");
		assertObject(a4b).isType(ABean.class);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Class<T> type) throws ParseException
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void getObject3() {
		Config c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
			);

		Map a1 = c.get("a1").as(Map.class).get();
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		List a2a = c.get("a2").as(List.class).get();
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(((Map)a2a.get(0)).keySet().iterator().next()).isType(String.class);
		assertObject(((Map)a2a.get(0)).values().iterator().next()).isType(Integer.class);

		Map a3 = c.get("a3").as(Map.class).orElse(null);
		assertNull(a3);

		Map a4a = c.get("a4").as(Map.class).get();
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		ABean a4b = c.get("a4").as(ABean.class).orElse(null);
		assertObject(a4b).asJson().is("{foo:'123'}");
		assertObject(a4b).isType(ABean.class);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Parser parser, Class<T> type) throws ParseException
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void getObject4() {
		Config c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);

		Map a1 = c.get("a1").as(UonParser.DEFAULT, Map.class).get();
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		List a2a = c.get("a2").as(UonParser.DEFAULT, List.class).get();
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(((Map)a2a.get(0)).keySet().iterator().next()).isType(String.class);
		assertObject(((Map)a2a.get(0)).values().iterator().next()).isType(Integer.class);

		Map a3 = c.get("a3").as(UonParser.DEFAULT, Map.class).orElse(null);
		assertNull(a3);

		Map a4a = c.get("a4").as(UonParser.DEFAULT, Map.class).get();
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		ABean a4b = c.get("a4").as(UonParser.DEFAULT, ABean.class).get();
		assertObject(a4b).asJson().is("{foo:'123'}");
		assertObject(a4b).isType(ABean.class);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, T def, Class<T> type) throws ParseException
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void getObjectWithDefault1() {
		Config c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
		);

		Map a1 = c.get("a1").as(Map.class).orElseGet(JsonMap::new);
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		Map a1b = c.get("a1b").as(Map.class).orElseGet(JsonMap::new);
		assertObject(a1b).asJson().is("{}");

		List a2a = c.get("a2").as(List.class).orElseGet(JsonList::new);
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(((Map)a2a.get(0)).keySet().iterator().next()).isType(String.class);
		assertObject(((Map)a2a.get(0)).values().iterator().next()).isType(Integer.class);

		List a2b = c.get("a2b").as(List.class).orElseGet(JsonList::new);
		assertObject(a2b).asJson().is("[]");

		Map a3 = c.get("a3").as(Map.class).orElseGet(JsonMap::new);
		assertObject(a3).asJson().is("{}");

		Map a4a = c.get("a4").as(Map.class).orElseGet(JsonMap::new);
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		Map a4b = c.get("a4b").as(Map.class).orElseGet(JsonMap::new);
		assertObject(a4b).asJson().is("{}");

		ABean a4c = c.get("a4c").as(ABean.class).orElse(new ABean().init());
		assertObject(a4c).asJson().is("{foo:'bar'}");
		assertObject(a4c).isType(ABean.class);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, Parser parser, T def, Class<T> type) throws ParseException
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void getObjectWithDefault2() {
		Config c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);

		Map a1 = c.get("a1").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		Map a1b = c.get("a1b").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertObject(a1b).asJson().is("{}");

		List a2a = c.get("a2").as(UonParser.DEFAULT, List.class).orElseGet(JsonList::new);
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(((Map)a2a.get(0)).keySet().iterator().next()).isType(String.class);
		assertObject(((Map)a2a.get(0)).values().iterator().next()).isType(Integer.class);

		List a2b = c.get("a2b").as(UonParser.DEFAULT, List.class).orElseGet(JsonList::new);
		assertObject(a2b).asJson().is("[]");

		Map a3 = c.get("a3").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertObject(a3).asJson().is("{}");

		Map a4a = c.get("a4").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		Map a4b = c.get("a4b").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertObject(a4b).asJson().is("{}");

		ABean a4c = c.get("a4c").as(UonParser.DEFAULT, ABean.class).orElse(new ABean().init());
		assertObject(a4c).asJson().is("{foo:'bar'}");
		assertObject(a4c).isType(ABean.class);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, T def, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test
	public void getObjectWithDefault3() {
		Config c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
		);

		Map<String,Integer> a1 = (Map<String,Integer>) c.get("a1").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		Map<String,Integer> a1b = (Map<String,Integer>) c.get("a1b").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a1b).asJson().is("{}");

		List<Map<String,Integer>> a2a = (List<Map<String,Integer>>) c.get("a2").as(List.class, Map.class, String.class, Integer.class).orElse(new ArrayList<>());
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(a2a.get(0).keySet().iterator().next()).isType(String.class);
		assertObject(a2a.get(0).values().iterator().next()).isType(Integer.class);

		List<ABean> a2b = (List<ABean>) c.get("a2b").as(List.class, ABean.class).orElse(new ArrayList<>());
		assertObject(a2b).asJson().is("[]");

		Map<String,Object> a3 = (Map<String,Object>) c.get("a3").as(Map.class, String.class, Object.class).orElse(new JsonMap());
		assertObject(a3).asJson().is("{}");

		Map<String,Integer> a4a = (Map<String,Integer>) c.get("a4").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		Map<String,Integer> a4b = (Map<String,Integer>) c.get("a4b").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a4b).asJson().is("{}");

		ABean a4c = c.get("a4c").as(ABean.class).orElse(new ABean().init());
		assertObject(a4c).asJson().is("{foo:'bar'}");
		assertObject(a4c).isType(ABean.class);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, Parser parser, T def, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test
	public void getObjectWithDefault4() {
		Config c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);

		Map<String,Integer> a1 = (Map<String,Integer>) c.get("a1").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a1).asJson().is("{foo:123}");
		assertObject(a1.keySet().iterator().next()).isType(String.class);
		assertObject(a1.values().iterator().next()).isType(Integer.class);

		Map<String,Integer> a1b = (Map<String,Integer>) c.get("a1b").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a1b).asJson().is("{}");

		List<Map<String,Integer>> a2a = (List<Map<String,Integer>>) c.get("a2").as(UonParser.DEFAULT, List.class, Map.class, String.class, Integer.class).orElse(new ArrayList<>());
		assertObject(a2a).asJson().is("[{foo:123}]");
		assertObject(a2a.get(0).keySet().iterator().next()).isType(String.class);
		assertObject(a2a.get(0).values().iterator().next()).isType(Integer.class);

		List<ABean> a2b = (List<ABean>) c.get("a2b").as(UonParser.DEFAULT, List.class, ABean.class).orElse(new ArrayList<>());
		assertObject(a2b).asJson().is("[]");

		Map<String,Object> a3 = (Map<String,Object>) c.get("a3").as(UonParser.DEFAULT,Map.class, String.class, Object.class).orElse( new JsonMap());
		assertObject(a3).asJson().is("{}");

		Map<String,Integer> a4a = (Map<String,Integer>) c.get("a4").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a4a).asJson().is("{foo:123}");
		assertObject(a4a.keySet().iterator().next()).isType(String.class);
		assertObject(a4a.values().iterator().next()).isType(Integer.class);

		Map<String,Integer> a4b = (Map<String,Integer>) c.get("a4b").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertObject(a4b).asJson().is("{}");

		ABean a4c = c.get("a4c").as(UonParser.DEFAULT, ABean.class).orElse(new ABean().init());
		assertObject(a4c).asJson().is("{foo:'bar'}");
		assertObject(a4c).isType(ABean.class);
	}

	//====================================================================================================
	//	public Set<String> getKeys(String section)
	//====================================================================================================
	@Test
	public void getKeys() {
		Config c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");

		assertObject(c.getKeys("")).asJson().is("['a1','a2']");
		assertObject(c.getKeys("")).asJson().is("['a1','a2']");
		assertObject(c.getKeys("S")).asJson().is("['b1','b2']");
		assertTrue(c.getKeys("T").isEmpty());

		assertThrows(IllegalArgumentException.class, ()->c.getKeys(null), "Argument 'section' cannot be null.");
	}

	//====================================================================================================
	//	public Config writeProperties(String section, Object bean, boolean ignoreUnknownProperties)
	//====================================================================================================
	@Test
	public void writeProperties() {
		ABean a = new ABean().init();
		BBean b = new BBean().init();

		Config c = init("foo=qux", "[S]", "foo=baz", "bar=baz");
		c.getSection("S").writeToBean(a, true);
		assertObject(a).asJson().is("{foo:'baz'}");
		c.getSection("S").writeToBean(b, true);
		assertObject(b).asJson().is("{foo:'baz'}");
		assertThrows(ParseException.class, ()->c.getSection("S").writeToBean(a, false), "Unknown property 'bar' encountered in configuration section 'S'.");
		assertThrows(ParseException.class, ()->c.getSection("S").writeToBean(b, false), "Unknown property 'bar' encountered in configuration section 'S'.");
		c.getSection("").writeToBean(b, true);
		assertObject(b).asJson().is("{foo:'qux'}");
		c.getSection("").writeToBean(a, true);
		assertObject(a).asJson().is("{foo:'qux'}");
		c.getSection(null).writeToBean(a, true);
		assertObject(a).asJson().is("{foo:'qux'}");
	}

	//====================================================================================================
	//	public <T> T getSectionAsBean(String sectionName, Class<T>c)
	//====================================================================================================
	@Test
	public void getSectionAsBean1() {
		Config c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");

		ABean a = null;
		BBean b = null;

		a = c.getSection("").asBean(ABean.class).get();
		assertObject(a).asJson().is("{foo:'qux'}");
		a = c.getSection("").asBean(ABean.class).get();
		assertObject(a).asJson().is("{foo:'qux'}");
		a = c.getSection(null).asBean(ABean.class).get();
		assertObject(a).asJson().is("{foo:'qux'}");
		a = c.getSection("S").asBean(ABean.class).get();
		assertObject(a).asJson().is("{foo:'baz'}");

		b = c.getSection("").asBean(BBean.class).get();
		assertObject(b).asJson().is("{foo:'qux'}");
		b = c.getSection("").asBean(BBean.class).get();
		assertObject(b).asJson().is("{foo:'qux'}");
		b = c.getSection("S").asBean(BBean.class).get();
		assertObject(b).asJson().is("{foo:'baz'}");

		assertThrows(ParseException.class, ()->c.getSection("T").asBean(ABean.class).get(), "Unknown property 'bar' encountered in configuration section 'T'.");
		assertThrows(ParseException.class, ()->c.getSection("T").asBean(BBean.class).get(), "Unknown property 'bar' encountered in configuration section 'T'.");
	}

	//====================================================================================================
	//	public <T> T getSectionAsBean(String section, Class<T> c, boolean ignoreUnknownProperties)
	//====================================================================================================
	@Test
	public void getSectionAsBean2() {
		Config c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");

		ABean a = null;
		BBean b = null;

		a = c.getSection("T").asBean(ABean.class, true).get();
		assertObject(a).asJson().is("{foo:'qux'}");
		b = c.getSection("T").asBean(BBean.class, true).get();
		assertObject(b).asJson().is("{foo:'qux'}");

		assertThrows(ParseException.class, ()->c.getSection("T").asBean(ABean.class, false).get(), "Unknown property 'bar' encountered in configuration section 'T'.");
		assertThrows(ParseException.class, ()->c.getSection("T").asBean(BBean.class, false).get(), "Unknown property 'bar' encountered in configuration section 'T'.");
	}

	//====================================================================================================
	//	public JsonMap getSectionAsMap(String section)
	//====================================================================================================
	@Test
	public void getSectionAsMap() {
		Config c = init("a=1", "[S]", "b=2", "[T]");

		assertObject(c.getSection("").asMap().get()).asJson().is("{a:'1'}");
		assertObject(c.getSection("").asMap().get()).asJson().is("{a:'1'}");
		assertObject(c.getSection(null).asMap().get()).asJson().is("{a:'1'}");
		assertObject(c.getSection("S").asMap().get()).asJson().is("{b:'2'}");
		assertObject(c.getSection("T").asMap().get()).asJson().is("{}");
		assertFalse(c.getSection("U").asMap().isPresent());
	}

	//====================================================================================================
	//	public <T> T getSectionAsInterface(final String sectionName, final Class<T> c)
	//====================================================================================================
	@Test
	public void getSectionAsInterface() {
		Config c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");
		AInterface a = null;

		a = c.getSection("").asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		a = c.getSection("").asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		a = c.getSection(null).asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		a = c.getSection("S").asInterface(AInterface.class).get();
		assertEquals("baz", a.getFoo());

		a = c.getSection("T").asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		assertThrows(IllegalArgumentException.class, ()->c.getSection("T").asInterface(ABean.class).get(), "Class 'org.apache.juneau.config.ConfigTest$ABean' passed to toInterface() is not an interface.");
	}

	public interface AInterface {
		String getFoo();
	}

	//====================================================================================================
	//	public boolean exists(String key)
	//====================================================================================================
	@Test
	public void exists() {
		Config c = init("a=1", "[S]", "b=2", "c=", "[T]");

		assertTrue(c.exists("a"));
		assertFalse(c.exists("b"));
		assertTrue(c.exists("S/b"));
		assertFalse(c.exists("S/c"));
		assertFalse(c.exists("T/d"));
		assertFalse(c.exists("U/e"));
	}

	//====================================================================================================
	//	public Config setSection(String name, List<String> preLines)
	//====================================================================================================
	@Test
	public void setSection1() {
		Config c = init();

		c.setSection("", Arrays.asList("#C1", "#C2"));
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C1|#C2||");

		c.setSection("", Arrays.asList("#C3", "#C4"));
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4||");

		c.setSection("S1", Arrays.asList("", "#C5", "#C6"));
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4|||#C5|#C6|[S1]|");

		c.setSection("S1", null);
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4|||#C5|#C6|[S1]|");

		c.setSection("S1", Collections.<String>emptyList());
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4||[S1]|");

		assertThrows(IllegalArgumentException.class, ()->c.setSection(null, Arrays.asList("", "#C5", "#C6")), "Argument 'section' cannot be null.");
	}

	//====================================================================================================
	//	public Config setSection(String name, List<String> preLines, Map<String,Object> contents)
	//====================================================================================================
	@Test
	public void setSection2() {
		Config c = init();
		JsonMap m = JsonMap.of("a", "b");

		c.setSection("", Arrays.asList("#C1", "#C2"), m);
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C1|#C2||a = b|");

		c.setSection("", Arrays.asList("#C3", "#C4"), m);
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4||a = b|");

		c.setSection("S1", Arrays.asList("", "#C5", "#C6"), m);
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4||a = b||#C5|#C6|[S1]|a = b|");

		c.setSection("S1", null, m);
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4||a = b||#C5|#C6|[S1]|a = b|");

		c.setSection("S1", Collections.<String>emptyList(), m);
		assertString(c).asReplaceAll("\\r?\\n", "|").is("#C3|#C4||a = b|[S1]|a = b|");

		assertThrows(IllegalArgumentException.class, ()->c.setSection(null, Arrays.asList("", "#C5", "#C6"), m), "Argument 'section' cannot be null.");
	}

	//====================================================================================================
	//	public Config removeSection(String name)
	//====================================================================================================
	@Test
	public void removeSection() {
		Config c = init("a=1", "[S]", "b=2", "c=", "[T]");

		c.removeSection("S");
		c.removeSection("T");

		assertString(c).asReplaceAll("\\r?\\n", "|").is("a=1|");

		c.removeSection("");
		assertString(c).asReplaceAll("\\r?\\n", "|").is("");
	}

	//====================================================================================================
	//	public Writer writeTo(Writer w)
	//====================================================================================================
	@Test
	public void writeTo() throws Exception {
		Config c = init("a=1", "[S]", "b=2", "c=", "[T]");

		assertString(c.writeTo(new StringWriter())).asReplaceAll("\\r?\\n", "|").is("a=1|[S]|b=2|c=|[T]|");
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

		assertEquals(1, cf.get("key1").asInteger().get());
		assertTrue(cf.get("key2").asBoolean().get());
		assertEquals(3, cf.get("key3").as(int[].class).get()[2]);
		assertEquals(6, cf.get("xkey3").as(int[].class).orElse(new int[]{4,5,6})[2]);
		assertEquals(6, cf.get("X/key3").as(int[].class).orElse(new int[]{4,5,6})[2]);
		assertEquals(url("http://foo").toString(), cf.get("key4").as(URL.class).get().toString());

		assertEquals(2, cf.get("section1/key1").asInteger().get());
		assertFalse(cf.get("section1/key2").asBoolean().get());
		assertEquals(6, cf.get("section1/key3").as(int[].class).get()[2]);
		assertEquals(url("http://bar").toString(), cf.get("section1/key4").as(URL.class).get().toString());

		cf = init(
			"# Default section",
			"[section1]", "# Section 1"
		);

		cf.set("key1", 1);
		cf.set("key2", true);
		cf.set("key3", new int[]{1,2,3});
		cf.set("key4", url("http://foo"));
		cf.set("section1/key1", 2);
		cf.set("section1/key2", false);
		cf.set("section1/key3", new int[]{4,5,6});
		cf.set("section1/key4", url("http://bar"));

		cf.commit();

		assertEquals(1, cf.get("key1").asInteger().get());
		assertTrue(cf.get("key2").asBoolean().get());
		assertEquals(3, cf.get("key3").as(int[].class).get()[2]);
		assertEquals(url("http://foo").toString(), cf.get("key4").as(URL.class).get().toString());

		assertEquals(2, cf.get("section1/key1").asInteger().get());
		assertFalse(cf.get("section1/key2").asBoolean().get());
		assertEquals(6, cf.get("section1/key3").as(int[].class).get()[2]);
		assertEquals(url("http://bar").toString(), cf.get("section1/key4").as(URL.class).get().toString());
	}

	//====================================================================================================
	// testEnum
	//====================================================================================================
	@Test
	public void testEnum() throws Exception {
		Config cf = init(
			"key1 = MINUTES"
		);
		assertEquals(TimeUnit.MINUTES, cf.get("key1").as(TimeUnit.class).get());

		cf.commit();

		assertEquals(TimeUnit.MINUTES, cf.get("key1").as(TimeUnit.class).get());
	}

	//====================================================================================================
	// testEncodedValues
	//====================================================================================================
	@Test
	public void testEncodedValues() throws Exception {
		Config cf = init(
			"[s1]", "", "foo<*> = "
		);

		cf.set("s1/foo", "mypassword");

		assertEquals("mypassword", cf.get("s1/foo").get());

		cf.commit();

		assertString(cf).asReplaceAll("\\r?\\n", "|").is("[s1]||foo<*> = {AwwJVhwUQFZEMg==}|");

		assertEquals("mypassword", cf.get("s1/foo").get());

		cf.load(reader("[s1]\nfoo<*> = mypassword2\n"), true);

		assertEquals("mypassword2", cf.get("s1/foo").get());

		cf.set("s1/foo", "mypassword");

		// INI output should be encoded
		StringWriter sw = new StringWriter();
		cf.writeTo(new PrintWriter(sw));
		assertString(sw).asReplaceAll("\\r?\\n", "|").is("[s1]|foo<*> = {AwwJVhwUQFZEMg==}|");
	}

	//====================================================================================================
	// public Config encodeEntries()
	//====================================================================================================
	@Test
	public void testEncodeEntries() throws Exception {
		Config cf = init(
			"[s1]", "", "foo<*> = mypassword"
		);

		cf.applyMods();
		cf.commit();
		assertString(MemoryStore.DEFAULT.read("Test.cfg")).asReplaceAll("\\r?\\n", "|").is("[s1]||foo<*> = {AwwJVhwUQFZEMg==}|");
	}

	//====================================================================================================
	// testVariables
	//====================================================================================================
	@Test
	public void testVariables() {

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

		assertEquals("", cf.get("s1/f1").get());
		assertEquals("bar", cf.get("s1/f2").get());
		assertEquals("bar", cf.get("s1/f3").get());

		System.setProperty("foo", "123");
		assertEquals("123", cf.get("s1/f1").get());
		assertEquals("123", cf.get("s1/f2").get());
		assertEquals("bar", cf.get("s1/f3").get());

		System.setProperty("foo", "$S{bar}");
		System.setProperty("bar", "baz");
		assertEquals("baz", cf.get("s1/f1").get());
		assertEquals("baz", cf.get("s1/f2").get());
		assertEquals("bar", cf.get("s1/f3").get());

		System.setProperty("bing", "$S{foo}");
		assertEquals("baz", cf.get("s1/f3").get());

		System.setProperty("baz", "foo");
		System.setProperty("foo", "123");
		assertEquals("123", cf.get("s1/f3").get());
	}

	//====================================================================================================
	// testXorEncoder
	//====================================================================================================
	@Test
	public void testXorEncoder() {
		testXor("foo");
		testXor("");
		testXor("123");
		testXor("€");  // 3-byte UTF-8 character
		testXor("𤭢"); // 4-byte UTF-8 character
	}

	private void testXor(String in) {
		XorEncodeMod e = new XorEncodeMod();
		String s = e.apply(in);
		String s2 = e.remove(s);
		assertEquals(in, s2);
	}

	//====================================================================================================
	// testHex
	//====================================================================================================
	@Test
	public void testHex() throws Exception {
		Config cf = init().copy().binaryFormat(BinaryFormat.HEX).build();

		cf.set("foo", "bar".getBytes("UTF-8"));
		assertEquals("626172", cf.get("foo").getValue());
		assertObject(cf.get("foo").asBytes().get()).asJson().is("[98,97,114]");
	}

	//====================================================================================================
	// testSpacedHex
	//====================================================================================================
	@Test
	public void testSpacedHex() throws Exception {
		Config cf = init().copy().binaryFormat(BinaryFormat.SPACED_HEX).build();

		cf.set("foo", "bar".getBytes("UTF-8"));
		assertEquals("62 61 72", cf.get("foo").getValue());
		assertObject(cf.get("foo").asBytes().get()).asJson().is("[98,97,114]");
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

		assertEquals("x \ny \nz", cf.get("s1/f1").get());

		StringWriter sw = new StringWriter();
		cf.writeTo(sw);
		assertString(sw).asReplaceAll("\\r?\\n", "|").is("[s1]|f1 = x |\ty |\tz|");
	}

	//====================================================================================================
	// testNumberShortcuts
	//====================================================================================================
	@Test
	public void testNumberShortcuts() {
		Config cf = init(
			"[s1]",
			"f1 = 1M",
			"f2 = 1K",
			"f3 = 1 M",
			"f4 = 1 K"
		);
		assertEquals(1048576, cf.get("s1/f1").asInteger().get());
		assertEquals(1024, cf.get("s1/f2").asInteger().get());
		assertEquals(1048576, cf.get("s1/f3").asInteger().get());
		assertEquals(1024, cf.get("s1/f4").asInteger().get());
	}

	//====================================================================================================
	// testListeners
	//====================================================================================================
	@Test
	public void testListeners() throws Exception {
		Config cf = init();

		final Set<String> changes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		cf.addListener(events -> {
			for (ConfigEvent ce : events) {
				String key = (ce.getSection().isEmpty() ? "" : (ce.getSection() + '/')) + ce.getKey();
				if (ce.getType() == ConfigEventType.REMOVE_ENTRY) {
					changes.add("REMOVE_ENTRY(" + key + ")");
				} else if (ce.getType() == ConfigEventType.REMOVE_SECTION) {
					changes.add("REMOVE_SECTION(" + ce.getSection() + ")");
				} else if (ce.getType() == ConfigEventType.SET_SECTION) {
					changes.add("SET_SECTION(" + ce.getSection() + ")");
				} else {
					changes.add(key + '=' + ce.getValue());
				}
			}
		});

		// No changes until save.
		changes.clear();
		cf.set("a1", 3);
		cf.set("a3", 3);
		cf.set("B/b1", 3);
		cf.set("B/b3", 3);
		assertObject(changes).asJson().is("[]");
		cf.commit();
		assertObject(changes).asJson().is("['a1=3','a3=3','B/b1=3','B/b3=3']");

		// Rollback.
		changes.clear();
		cf.set("a1", 3);
		cf.set("a3", 3);
		cf.set("B/b1", 3);
		cf.set("B/b3", 3);
		assertObject(changes).asJson().is("[]");
		cf.rollback();
		cf.commit();
		assertObject(changes).asJson().is("[]");

		// Overwrite
		changes.clear();
		cf.set("a1", "2");
		cf.set("B/b1", "2");
		cf.set("a2", "2");
		cf.set("B/b2", "2");
		cf.set("C/c1", "2");
		cf.set("C/c2", "2");
		cf.commit();
		assertObject(changes).asJson().is("['a1=2','a2=2','B/b1=2','B/b2=2','C/c1=2','C/c2=2']");

		// Encoded
		changes.clear();
		cf.set("a4", "4", null, "*", null, null);
		cf.set("B/b4", "4", null, "*", null, null);
		cf.commit();
		assertObject(changes).asJson().is("['a4={Wg==}','B/b4={Wg==}']");

		// Encoded overwrite
		changes.clear();
		cf.set("a4", "5");
		cf.set("B/b4", "5");
		cf.commit();
		assertObject(changes).asJson().is("['a4={Ww==}','B/b4={Ww==}']");

		// Remove entries
		changes.clear();
		cf.remove("a4");
		cf.remove("ax");
		cf.remove("B/b4");
		cf.remove("B/bx");
		cf.remove("X/bx");
		cf.commit();
		assertObject(changes).asJson().is("['REMOVE_ENTRY(a4)','REMOVE_ENTRY(B/b4)']");

		// Add section
		// Shouldn't trigger listener.
		changes.clear();
		cf.setSection("D", Arrays.asList("#comment"));
		cf.commit();
		assertObject(changes).asJson().is("[]");

		// Add section with contents
		changes.clear();
		cf.setSection("E", null, map("e1","1","e2","2"));
		cf.commit();
		assertObject(changes).asJson().is("['E/e1=1','E/e2=2']");

		// Remove section
		changes.clear();
		cf.removeSection("E");
		cf.commit();
		assertObject(changes).asJson().is("['REMOVE_ENTRY(E/e1)','REMOVE_ENTRY(E/e2)']");

		// Remove non-existent section
		changes.clear();
		cf.removeSection("E");
		cf.commit();
		assertObject(changes).asJson().is("[]");
	}

	//====================================================================================================
	// getObjectArray(Class c, String key)
	// getObjectArray(Class c, String key, T[] def)
	//====================================================================================================
	@Test
	public void testGetObjectArray() {
		Config cf = init("[A]", "a1=[1,2,3]");
		assertObject(cf.get("A/a1").as(Integer[].class).get()).asJson().is("[1,2,3]");
		assertObject(cf.get("A/a2").as(Integer[].class).orElse(new Integer[]{4,5,6})).asJson().is("[4,5,6]");
		assertObject(cf.get("B/a1").as(Integer[].class).orElse(new Integer[]{7,8,9})).asJson().is("[7,8,9]");
		assertFalse(cf.get("B/a1").as(Integer[].class).isPresent());

		cf = init("[A]", "a1 = [1 ,\n\t2 ,\n\t3] ");
		assertObject(cf.get("A/a1").as(Integer[].class).get()).asJson().is("[1,2,3]");

		// We cannot cast primitive arrays to Object[], so the following throws exceptions.
		assertObject(cf.get("A/a1").as(int[].class).get()).asJson().is("[1,2,3]");
		assertEquals("int", cf.get("A/a1").as(int[].class).get().getClass().getComponentType().getSimpleName());
		assertFalse(cf.get("B/a1").as(int[].class).isPresent());
		assertEquals("int", cf.get("B/a1").as(int[].class).orElse(new int[0]).getClass().getComponentType().getSimpleName());
		assertFalse(cf.get("A/a2").as(int[].class).isPresent());
		assertEquals("int", cf.get("A/a2").as(int[].class).orElse(new int[0]).getClass().getComponentType().getSimpleName());

		assertObject(cf.get("A/a1").as(int[].class).orElse(new int[]{4})).asJson().is("[1,2,3]");
		assertEquals("int", cf.get("A/a1").as(int[].class).orElse(new int[]{4}).getClass().getComponentType().getSimpleName());
		assertObject(cf.get("B/a1").as(int[].class).orElse(new int[]{4})).asJson().is("[4]");
		assertEquals("int", cf.get("B/a1").as(int[].class).orElse(new int[]{4}).getClass().getComponentType().getSimpleName());
		assertObject(cf.get("A/a2").as(int[].class).orElse(new int[]{4})).asJson().is("[4]");
		assertEquals("int", cf.get("A/a2").as(int[].class).orElse(new int[]{4}).getClass().getComponentType().getSimpleName());

		System.setProperty("X", "[4,5,6]");
		cf = init("x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "[A]", "a1=[1,2,3]");
		assertObject(cf.get("x1").as(int[].class).orElse(new int[]{9})).asJson().is("[1,2,3]");
		assertObject(cf.get("x2").as(int[].class).orElse(new int[]{9})).asJson().is("[4,5,6]");
		assertObject(cf.get("x3").as(int[].class).orElse(new int[]{9})).asJson().is("[]");
		System.clearProperty("X");
	}

	//====================================================================================================
	// getStringArray(String key)
	// getStringArray(String key, String[] def)
	//====================================================================================================
	@Test
	public void testGetStringArray() {
		Config cf = init("[A]", "a1=1,2,3");
		assertObject(cf.get("A/a1").asStringArray().get()).asJson().is("['1','2','3']");
		assertObject(cf.get("A/a2").asStringArray().orElse(new String[]{"4","5","6"})).asJson().is("['4','5','6']");
		assertObject(cf.get("B/a1").asStringArray().orElse(new String[]{"7","8","9"})).asJson().is("['7','8','9']");
		assertNull(cf.get("B/a1").asStringArray().orElse(null));

		cf = init("[A]", "a1 = 1 ,\n\t2 ,\n\t3 ");
		assertObject(cf.get("A/a1").asStringArray().get()).asJson().is("['1','2','3']");

		System.setProperty("X", "4,5,6");
		cf = init(null, "x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "x4=$S{Y,$S{X}}", "[A]", "a1=1,2,3");
		assertObject(cf.get("x1").asStringArray().orElse(new String[]{"9"})).asJson().is("['1','2','3']");
		assertObject(cf.get("x2").asStringArray().orElse(new String[]{"9"})).asJson().is("['4','5','6']");
		assertObject(cf.get("x9").asStringArray().orElse(new String[]{"9"})).asJson().is("['9']");

		System.clearProperty("X");
	}

	//====================================================================================================
	// getSectionMap(String name)
	//====================================================================================================
	@Test
	public void testGetSectionMap() {
		Config cf = init("[A]", "a1=1", "", "[D]", "d1=$C{A/a1}","d2=$S{X}");

		assertObject(cf.getSection("A").asMap().get()).asJson().is("{a1:'1'}");
		assertFalse(cf.getSection("B").asMap().isPresent());
		assertObject(cf.getSection("C").asMap().orElse(null)).asJson().is("null");

		JsonMap m = cf.getSection("A").asMap().get();
		assertObject(m).asJson().is("{a1:'1'}");

		System.setProperty("X", "x");
		m = cf.getSection("D").asMap().get();
		assertObject(m).asJson().is("{d1:'1',d2:'x'}");
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
		assertString(sw).asReplaceAll("\\r?\\n", "|").is("a = b|");
	}


	//====================================================================================================
	// Test resolving with override
	//====================================================================================================
	@Test
	public void testResolvingWithOverride() {
		Config cf = init();
		cf.set("a", "$A{X}");
		cf.set("b", "$B{X}");
		cf.set("c", "$A{$B{X}}");
		cf.set("d", "$B{$A{X}}");
		cf.set("e", "$D{X}");

		VarResolver vr = VarResolver.create().defaultVars().vars(ALVar.class, BLVar.class).build();

		cf = cf.resolving(vr.createSession());

		assertEquals("aXa", cf.get("a").get());
		assertEquals("bXb", cf.get("b").get());
		assertEquals("abXba", cf.get("c").get());
		assertEquals("baXab", cf.get("d").get());
		assertEquals("$D{X}", cf.get("e").get());

		// Create new resolver that addx $C and overrides $A
		VarResolver vr2 = vr.copy().vars(AUVar.class, DUVar.class).build();

		// true == augment by adding existing as parent to the new resolver
		cf = cf.resolving(vr2.createSession());
		assertEquals("AXA", cf.get("a").get());
		assertEquals("bXb", cf.get("b").get());
		assertEquals("AbXbA", cf.get("c").get());
		assertEquals("bAXAb", cf.get("d").get());
		assertEquals("DXD", cf.get("e").get());
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

		assertString(cf).asReplaceAll("\\r?\\n", "|").is("a = a,\\u0023b,=c|[A]|a = a,\\u0023b,=c|");
		cf.commit();
		assertString(cf).asReplaceAll("\\r?\\n", "|").is("a = a,\\u0023b,=c|[A]|a = a,\\u0023b,=c|");

		assertEquals("a,#b,=c", cf.get("a").get());
		assertEquals("a,#b,=c", cf.get("A/a").get());

		cf.set("a", "a,#b,=c", null, null, "comment#comment", null);
		assertString(cf).asReplaceAll("\\r?\\n", "|").is("a = a,\\u0023b,=c # comment#comment|[A]|a = a,\\u0023b,=c|");
		assertEquals("a,#b,=c", cf.get("a").get());
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

	@Test
	public void testGetCandidateSystemDefaultConfigNames() {

		System.setProperty("juneau.configFile", "foo.txt");
		assertObject(Config.getCandidateSystemDefaultConfigNames()).asJson().is("['foo.txt']");

		System.clearProperty("juneau.configFile");
		assertObject(Config.getCandidateSystemDefaultConfigNames()).asJson().is("['test.cfg','juneau.cfg','default.cfg','application.cfg','app.cfg','settings.cfg','application.properties']");
	}

	//====================================================================================================
	//	setSystemProperties
	//====================================================================================================

	@Test
	public void setSystemProperties() {
		Config c = init("a=1", "[S]", "b=2");
		c.setSystemProperties();
		assertEquals("1", System.getProperty("a"));
		assertEquals("2", System.getProperty("S/b"));
	}
}