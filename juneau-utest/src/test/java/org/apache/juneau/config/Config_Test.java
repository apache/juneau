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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.mod.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"rawtypes", // Raw types used for generic testing scenarios
	"unchecked" // Cast from Object to List/Map in config tests
})
class Config_Test extends TestBase {

	private Config.Builder cb = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg");

	private Config init(String...lines) {
		MemoryStore.DEFAULT.update("Test.cfg", lines);
		return cb.build().rollback();
	}

	//====================================================================================================
	//	public String get(String key)
	//====================================================================================================
	@Test void get() {
		var c = init("a=1", "[S]", "b=2");

		assertEquals("1", c.get("a").get());
		assertEquals("1", c.get("a").get());
		assertEquals("2", c.get("S/b").get());
		assertNull(c.get("b").orElse(null));
		assertNull(c.get("S/c").orElse(null));
		assertNull(c.get("T/d").orElse(null));
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'key' cannot be null.", ()->c.get(null));
		c.close();
	}

	//====================================================================================================
	//	public Config set(String key, String value)
	//====================================================================================================
	@Test void set1() throws Exception {
		var c = init("a1=1", "[S]", "b1=1");

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

		assertEquals("a1 = 2|a2 = 3|a3 = 4|[S]|b1 = 5|b2 = 6|[T]|c1 = 7|", pipedLines(c));
	}

	//====================================================================================================
	//	public Config set(String key, Object value)
	//====================================================================================================
	@Test void set2() throws Exception {
		var c = init("a1=1", "[S]", "b1=1");

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

		assertEquals("a1 = 2|a2 = 3|a3 = 4|[S]|b1 = 5|b2 = 6|[T]|c1 = 7|", pipedLines(c));
	}

	//====================================================================================================
	//	public Config set(String key, Object value, Serializer serializer)
	//====================================================================================================
	@Test void set3() {
		var c = init("a1=1", "[S]", "b1=1");

		var b = new ABean().init();
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
	@Test void set4() throws Exception {
		var c = init("a1=1", "[S]", "b1=1");

		var b = new ABean().init();
		c.set("a1", b, UonSerializer.DEFAULT, "*", "comment", l("#c1","#c2"));
		c.set("a2", b, UonSerializer.DEFAULT, "*", "comment", l("#c1","#c2"));
		c.set("a3", b, UonSerializer.DEFAULT, "*", "comment", l("#c1","#c2"));
		c.set("S/b1", b, UonSerializer.DEFAULT, "*", "comment", l("#c1","#c2"));
		c.set("S/b2", b, UonSerializer.DEFAULT, "*", "comment", l("#c1","#c2"));
		c.set("T/c1", b, UonSerializer.DEFAULT, "*", "comment", l("#c1","#c2"));

		assertEquals("#c1|#c2|a1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a2<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a3<*> = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|b2<*> = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1<*> = {RhMWWFIFVksf} # comment|", pipedLines(c));
		c.commit();
		assertEquals("#c1|#c2|a1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a2<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a3<*> = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|b2<*> = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1<*> = {RhMWWFIFVksf} # comment|", pipedLines(c));
		c = cb.build();
		assertEquals("#c1|#c2|a1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a2<*> = {RhMWWFIFVksf} # comment|#c1|#c2|a3<*> = {RhMWWFIFVksf} # comment|[S]|#c1|#c2|b1<*> = {RhMWWFIFVksf} # comment|#c1|#c2|b2<*> = {RhMWWFIFVksf} # comment|[T]|#c1|#c2|c1<*> = {RhMWWFIFVksf} # comment|", pipedLines(c));

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
	@Test void remove() throws Exception {
		var c = init("a1=1", "a2=2", "[S]", "b1=1");

		c.remove("a1");
		c.remove("a2");
		c.remove("a3");
		c.remove("S/b1");
		c.remove("T/c1");

		assertEquals("[S]|", pipedLines(c));
		c.commit();
		assertEquals("[S]|", pipedLines(c));
		c = cb.build();
		assertEquals("[S]|", pipedLines(c));
	}

	//====================================================================================================
	//	public String getString1(String key)
	//====================================================================================================
	@Test void xgetString1() {
		var c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");

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
	@Test void getString2() {
		var c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
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
	@Test void getStringArray1() {
		var c = init("a1=1,2", "a2= 2 , 3 ", "[S]", "b1=1", "b2=");
		assertList(c.get("a1").as(String[].class).orElse(null), "1", "2");
		assertList(c.get("a2").as(String[].class).orElse(null), "2", "3");
		assertNull(c.get("a3").as(String[].class).orElse(null));
		assertList(c.get("S/b1").as(String[].class).orElse(null), "1");
		assertEmpty(c.get("S/b2").as(String[].class).orElse(null));
		assertNull(c.get("S/b3").as(String[].class).orElse(null));
		assertNull(c.get("T/c1").as(String[].class).orElse(null));
	}

	//====================================================================================================
	//	public String[] getStringArray(String key, String[] def)
	//====================================================================================================
	@Test void getStringArray2() {
		var c = init("a1=1,2", "a2= 2 , 3 ", "[S]", "b1=1", "b2=");
		assertList(c.get("a1").asStringArray().orElse(a("foo")), "1", "2");
		assertList(c.get("a2").asStringArray().orElse(a("foo")), "2", "3");
		assertList(c.get("a3").asStringArray().orElse(a("foo")), "foo");
		assertList(c.get("S/b1").asStringArray().orElse(a("foo")), "1");
		assertEmpty(c.get("S/b2").asStringArray().orElse(a("foo")));
		assertList(c.get("S/b3").asStringArray().orElse(a("foo")), "foo");
		assertList(c.get("T/c1").asStringArray().orElse(a("foo")), "foo");
	}

	//====================================================================================================
	//	public int getInt(String key)
	//====================================================================================================
	@Test void getInt1() {
		var c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertEquals(1, c.get("a1").asInteger().orElse(0));
		assertEquals(2, c.get("a2").asInteger().orElse(0));
		assertEquals(0, c.get("a3").asInteger().orElse(0));
		assertEquals(1, c.get("S/b1").asInteger().orElse(0));
		assertEquals(0, c.get("S/b2").asInteger().orElse(0));
		assertEquals(0, c.get("S/b3").asInteger().orElse(0));
		assertEquals(0, c.get("T/c1").asInteger().orElse(0));
	}

	@Test void getInt1BadValues() {
		var c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		assertThrows(Exception.class, ()->c.get("a1").asInteger().orElse(0));
		assertThrows(Exception.class, ()->c.get("a2").asInteger().orElse(0));
		assertThrows(Exception.class, ()->c.get("a3").asInteger().orElse(0));
		assertThrows(Exception.class, ()->c.get("a4").asInteger().orElse(0));
	}

	//====================================================================================================
	//	public int getInt2(String key, int def)
	//====================================================================================================
	@Test void getInt2() {
		var c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertEquals(1, c.get("a1").asInteger().orElse(-1));
		assertEquals(2, c.get("a2").asInteger().orElse(-1));
		assertEquals(-1, c.get("a3").asInteger().orElse(-1));
		assertEquals(1, c.get("S/b1").asInteger().orElse(-1));
		assertEquals(-1, c.get("S/b2").asInteger().orElse(-1));
		assertEquals(-1, c.get("S/b3").asInteger().orElse(-1));
		assertEquals(-1, c.get("T/c1").asInteger().orElse(-1));
	}

	@Test void getInt2BadValues() {
		var c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		assertThrows(Exception.class, ()->c.get("a1").asInteger().orElse(-1));
		assertThrows(Exception.class, ()->c.get("a2").asInteger().orElse(-1));
		assertThrows(Exception.class, ()->c.get("a3").asInteger().orElse(-1));
		assertThrows(Exception.class, ()->c.get("a4").asInteger().orElse(-1));
	}

	//====================================================================================================
	//	public boolean getBoolean(String key)
	//====================================================================================================
	@Test void getBoolean1() {
		var c = init("a1=true", "a2=false", "[S]", "b1=TRUE", "b2=");
		assertEquals(true, c.get("a1").asBoolean().orElse(false));
		assertEquals(false, c.get("a2").asBoolean().orElse(false));
		assertEquals(false, c.get("a3").asBoolean().orElse(false));
		assertEquals(true, c.get("S/b1").asBoolean().orElse(false));
		assertEquals(false, c.get("S/b2").asBoolean().orElse(false));
		assertEquals(false, c.get("S/b3").asBoolean().orElse(false));
		assertEquals(false, c.get("T/c1").asBoolean().orElse(false));
	}

	@Test void getBoolean1BadValues() {
		var c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=T");
		assertEquals(false, c.get("a1").asBoolean().orElse(false));
		assertEquals(false, c.get("a2").asBoolean().orElse(false));
		assertEquals(false, c.get("a3").asBoolean().orElse(false));
		assertEquals(false, c.get("a4").asBoolean().orElse(false));
	}

	//====================================================================================================
	//	public boolean getBoolean(String key, boolean def)
	//====================================================================================================
	@Test void getBoolean2() {
		var c = init("a1=true", "a2=false", "[S]", "b1=TRUE", "b2=");
		assertEquals(true, c.get("a1").asBoolean().orElse(true));
		assertEquals(false, c.get("a2").asBoolean().orElse(true));
		assertEquals(true, c.get("a3").asBoolean().orElse(true));
		assertEquals(true, c.get("S/b1").asBoolean().orElse(true));
		assertEquals(true, c.get("S/b2").asBoolean().orElse(true));
		assertEquals(true, c.get("S/b3").asBoolean().orElse(true));
		assertEquals(true, c.get("T/c1").asBoolean().orElse(true));
	}

	@Test void getBoolean2BadValues() {
		var c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=T");
		assertEquals(false, c.get("a1").asBoolean().orElse(true));
		assertEquals(false, c.get("a2").asBoolean().orElse(true));
		assertEquals(false, c.get("a3").asBoolean().orElse(true));
		assertEquals(false, c.get("a4").asBoolean().orElse(true));
	}

	//====================================================================================================
	//	public long getLong(String key)
	//====================================================================================================
	@Test void getLong1() {
		var c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertEquals(1L, c.get("a1").asLong().orElse(0L));
		assertEquals(2L, c.get("a2").asLong().orElse(0L));
		assertEquals(0L, c.get("a3").asLong().orElse(0L));
		assertEquals(1L, c.get("S/b1").asLong().orElse(0L));
		assertEquals(0L, c.get("S/b2").asLong().orElse(0L));
		assertEquals(0L, c.get("S/b3").asLong().orElse(0L));
		assertEquals(0L, c.get("T/c1").asLong().orElse(0L));
	}

	@Test void getLong1BadValues() {
		var c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");
		assertThrows(Exception.class, ()->c.get("a1").as(long.class));
		assertThrows(Exception.class, ()->c.get("a2").as(long.class));
		assertThrows(Exception.class, ()->c.get("a3").as(long.class));
		assertThrows(Exception.class, ()->c.get("a4").as(long.class));
	}

	//====================================================================================================
	//	public long getLong(String key, long def)
	//====================================================================================================
	@Test void getLong2() {
		var c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");
		assertEquals(1L, c.get("a1").asLong().orElse(Long.MAX_VALUE));
		assertEquals(2L, c.get("a2").asLong().orElse(Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.get("a3").asLong().orElse(Long.MAX_VALUE));
		assertEquals(1L, c.get("S/b1").asLong().orElse(Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.get("S/b2").asLong().orElse(Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.get("S/b3").asLong().orElse(Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE, c.get("T/c1").asLong().orElse(Long.MAX_VALUE));
	}

	@Test void getLong2BadValues() {
		var c = init("a1=foo", "a2=2.3", "a3=[1]", "a4=false");

		var a1Entry = c.get("a1");
		assertThrows(NumberFormatException.class, a1Entry::asLong);
		var a2Entry = c.get("a2");
		assertThrows(NumberFormatException.class, a2Entry::asLong);
		var a3Entry = c.get("a3");
		assertThrows(NumberFormatException.class, a3Entry::asLong);
		var a4Entry = c.get("a4");
		assertThrows(NumberFormatException.class, a4Entry::asLong);
	}

	//====================================================================================================
	//	public boolean getBytes(String key)
	//====================================================================================================
	@Test void getBytes1() {
		var c = init("a1=Zm9v", "a2=Zm", "\t9v", "a3=");

		assertList(c.get("a1").as(byte[].class).get(), (byte)102, (byte)111, (byte)111);
		assertList(c.get("a2").as(byte[].class).get(), (byte)102, (byte)111, (byte)111);
		assertEmpty(c.get("a3").as(byte[].class).get());
		assertFalse(c.get("a4").as(byte[].class).isPresent());
	}

	//====================================================================================================
	//	public boolean getBytes(String key, byte[] def)
	//====================================================================================================
	@Test void getBytes2() {
		var c = init("a1=Zm9v", "a2=Zm", "\t9v", "a3=");

		assertList(c.get("a1").asBytes().orElse(bytes(1)), (byte)102, (byte)111, (byte)111);
		assertList(c.get("a2").asBytes().orElse(bytes(1)), (byte)102, (byte)111, (byte)111);
		assertEmpty(c.get("a3").asBytes().orElse(bytes(1)));
		assertList(c.get("a4").asBytes().orElse(bytes(1)), (byte)1);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test void getObject1() {
		var c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
			);

		var a1 = (Map<String,Integer>) c.get("a1").as(Map.class, String.class, Integer.class).get();
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a2a = (List<Map<String,Integer>>) c.get("a2").as(List.class, Map.class, String.class, Integer.class).get();
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, a2a.get(0).keySet().iterator().next());
		assertInstanceOf(Integer.class, a2a.get(0).values().iterator().next());

		var a2b = (List<ABean>) c.get("a2").as(List.class, ABean.class).get();
		assertJson("[{foo:'123'}]", a2b);
		assertInstanceOf(ABean.class, a2b.get(0));

		var a3 = (Map<String,Integer>) c.get("a3").as(Map.class, String.class, Integer.class).orElse(null);
		assertNull(a3);

		var a4a = (Map<String,Integer>) c.get("a4").as(Map.class, String.class, Integer.class).get();
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = c.get("a4").as(ABean.class).get();
		assertJson("{foo:'123'}", a4b);
		assertInstanceOf(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Parser parser, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test void getObject2() {
		var c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
			);

		var a1 = (Map<String,Integer>) c.get("a1").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).get();
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a2a = (List<Map<String,Integer>>) c.get("a2").as(UonParser.DEFAULT, List.class, Map.class, String.class, Integer.class).get();
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, a2a.get(0).keySet().iterator().next());
		assertInstanceOf(Integer.class, a2a.get(0).values().iterator().next());

		var a2b = (List<ABean>) c.get("a2").as(UonParser.DEFAULT, List.class, ABean.class).get();
		assertJson("[{foo:'123'}]", a2b);
		assertInstanceOf(ABean.class, a2b.get(0));

		var a3 = (Map<String,Integer>) c.get("a3").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(null);
		assertNull(a3);

		var a4a = (Map<String,Integer>) c.get("a4").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).get();
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = c.get("a4").as(UonParser.DEFAULT, ABean.class).get();
		assertJson("{foo:'123'}", a4b);
		assertInstanceOf(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Class<T> type) throws ParseException
	//====================================================================================================
	@Test void getObject3() {
		var c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
			);

		var a1 = c.get("a1").as(Map.class).get();
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a2a = c.get("a2").as(List.class).get();
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertInstanceOf(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		var a3 = c.get("a3").as(Map.class).orElse(null);
		assertNull(a3);

		var a4a = c.get("a4").as(Map.class).get();
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = c.get("a4").as(ABean.class).orElse(null);
		assertJson("{foo:'123'}", a4b);
		assertInstanceOf(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObject(String key, Parser parser, Class<T> type) throws ParseException
	//====================================================================================================
	@Test void getObject4() {
		var c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);

		var a1 = c.get("a1").as(UonParser.DEFAULT, Map.class).get();
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a2a = c.get("a2").as(UonParser.DEFAULT, List.class).get();
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertInstanceOf(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		var a3 = c.get("a3").as(UonParser.DEFAULT, Map.class).orElse(null);
		assertNull(a3);

		var a4a = c.get("a4").as(UonParser.DEFAULT, Map.class).get();
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = c.get("a4").as(UonParser.DEFAULT, ABean.class).get();
		assertJson("{foo:'123'}", a4b);
		assertInstanceOf(ABean.class, a4b);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, T def, Class<T> type) throws ParseException
	//====================================================================================================
	@Test void getObjectWithDefault1() {
		var c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
		);

		var a1 = c.get("a1").as(Map.class).orElseGet(JsonMap::new);
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a1b = c.get("a1b").as(Map.class).orElseGet(JsonMap::new);
		assertJson("{}", a1b);

		var a2a = c.get("a2").as(List.class).orElseGet(JsonList::new);
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertInstanceOf(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		var a2b = c.get("a2b").as(List.class).orElseGet(JsonList::new);
		assertJson("[]", a2b);

		var a3 = c.get("a3").as(Map.class).orElseGet(JsonMap::new);
		assertJson("{}", a3);

		var a4a = c.get("a4").as(Map.class).orElseGet(JsonMap::new);
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = c.get("a4b").as(Map.class).orElseGet(JsonMap::new);
		assertJson("{}", a4b);

		var a4c = c.get("a4c").as(ABean.class).orElse(new ABean().init());
		assertJson("{foo:'bar'}", a4c);
		assertInstanceOf(ABean.class, a4c);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, Parser parser, T def, Class<T> type) throws ParseException
	//====================================================================================================
	@Test void getObjectWithDefault2() {
		var c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);

		var a1 = c.get("a1").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a1b = c.get("a1b").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertJson("{}", a1b);

		var a2a = c.get("a2").as(UonParser.DEFAULT, List.class).orElseGet(JsonList::new);
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, ((Map)a2a.get(0)).keySet().iterator().next());
		assertInstanceOf(Integer.class, ((Map)a2a.get(0)).values().iterator().next());

		var a2b = c.get("a2b").as(UonParser.DEFAULT, List.class).orElseGet(JsonList::new);
		assertJson("[]", a2b);

		var a3 = c.get("a3").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertJson("{}", a3);

		var a4a = c.get("a4").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = c.get("a4b").as(UonParser.DEFAULT, Map.class).orElseGet(JsonMap::new);
		assertJson("{}", a4b);

		var a4c = c.get("a4c").as(UonParser.DEFAULT, ABean.class).orElse(new ABean().init());
		assertJson("{foo:'bar'}", a4c);
		assertInstanceOf(ABean.class, a4c);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, T def, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test void getObjectWithDefault3() {
		var c = init(
			"a1={foo:123}",
			"a2=[{foo:123}]",
			"a3=",
			"a4=\t{",
			"\t foo : 123 /* comment */",
			"\t}"
		);

		var a1 = (Map<String,Integer>) c.get("a1").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a1b = (Map<String,Integer>) c.get("a1b").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{}", a1b);

		var a2a = (List<Map<String,Integer>>) c.get("a2").as(List.class, Map.class, String.class, Integer.class).orElse(list());
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, a2a.get(0).keySet().iterator().next());
		assertInstanceOf(Integer.class, a2a.get(0).values().iterator().next());

		var a2b = (List<ABean>) c.get("a2b").as(List.class, ABean.class).orElse(list());
		assertJson("[]", a2b);

		var a3 = (Map<String,Object>) c.get("a3").as(Map.class, String.class, Object.class).orElse(new JsonMap());
		assertJson("{}", a3);

		var a4a = (Map<String,Integer>) c.get("a4").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = (Map<String,Integer>) c.get("a4b").as(Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{}", a4b);

		var a4c = c.get("a4c").as(ABean.class).orElse(new ABean().init());
		assertJson("{foo:'bar'}", a4c);
		assertInstanceOf(ABean.class, a4c);
	}

	//====================================================================================================
	//	public <T> T getObjectWithDefault(String key, Parser parser, T def, Type type, Type...args) throws ParseException
	//====================================================================================================
	@Test void getObjectWithDefault4() {
		var c = init(
			"a1=(foo=123)",
			"a2=@((foo=123))",
			"a3=",
			"a4=\t(",
			"\t foo = 123",
			"\t)"
		);

		var a1 = (Map<String,Integer>) c.get("a1").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{foo:123}", a1);
		assertInstanceOf(String.class, a1.keySet().iterator().next());
		assertInstanceOf(Integer.class, a1.values().iterator().next());

		var a1b = (Map<String,Integer>) c.get("a1b").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{}", a1b);

		var a2a = (List<Map<String,Integer>>) c.get("a2").as(UonParser.DEFAULT, List.class, Map.class, String.class, Integer.class).orElse(list());
		assertJson("[{foo:123}]", a2a);
		assertInstanceOf(String.class, a2a.get(0).keySet().iterator().next());
		assertInstanceOf(Integer.class, a2a.get(0).values().iterator().next());

		var a2b = (List<ABean>) c.get("a2b").as(UonParser.DEFAULT, List.class, ABean.class).orElse(list());
		assertJson("[]", a2b);

		var a3 = (Map<String,Object>) c.get("a3").as(UonParser.DEFAULT,Map.class, String.class, Object.class).orElse( new JsonMap());
		assertJson("{}", a3);

		var a4a = (Map<String,Integer>) c.get("a4").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{foo:123}", a4a);
		assertInstanceOf(String.class, a4a.keySet().iterator().next());
		assertInstanceOf(Integer.class, a4a.values().iterator().next());

		var a4b = (Map<String,Integer>) c.get("a4b").as(UonParser.DEFAULT, Map.class, String.class, Integer.class).orElse(new HashMap<>());
		assertJson("{}", a4b);

		var a4c = c.get("a4c").as(UonParser.DEFAULT, ABean.class).orElse(new ABean().init());
		assertJson("{foo:'bar'}", a4c);
		assertInstanceOf(ABean.class, a4c);
	}

	//====================================================================================================
	//	public Set<String> getKeys(String section)
	//====================================================================================================
	@Test void getKeys() {
		var c = init("a1=1", "a2=2", "[S]", "b1=1", "b2=");

		assertJson("['a1','a2']", c.getKeys(""));
		assertJson("['a1','a2']", c.getKeys(""));
		assertJson("['b1','b2']", c.getKeys("S"));
		assertEmpty(c.getKeys("T"));

		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'section' cannot be null.", ()->c.getKeys(null));
	}

	//====================================================================================================
	//	public Config writeProperties(String section, Object bean, boolean ignoreUnknownProperties)
	//====================================================================================================
	@Test void writeProperties() {
		var a = new ABean().init();
		var b = new BBean().init();

		var c = init("foo=qux", "[S]", "foo=baz", "bar=baz");
		c.getSection("S").writeToBean(a, true);
		assertJson("{foo:'baz'}", a);
		c.getSection("S").writeToBean(b, true);
		assertJson("{foo:'baz'}", b);
		assertThrowsWithMessage(ParseException.class, "Unknown property 'bar' encountered in configuration section 'S'.", ()->c.getSection("S").writeToBean(a, false));
		assertThrowsWithMessage(ParseException.class, "Unknown property 'bar' encountered in configuration section 'S'.", ()->c.getSection("S").writeToBean(b, false));
		c.getSection("").writeToBean(b, true);
		assertJson("{foo:'qux'}", b);
		c.getSection("").writeToBean(a, true);
		assertJson("{foo:'qux'}", a);
		c.getSection(null).writeToBean(a, true);
		assertJson("{foo:'qux'}", a);
	}

	//====================================================================================================
	//	public <T> T getSectionAsBean(String sectionName, Class<T>c)
	//====================================================================================================
	@Test void getSectionAsBean1() {
		var c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");

		var a = c.getSection("").asBean(ABean.class).get();
		assertJson("{foo:'qux'}", a);
		a = c.getSection("").asBean(ABean.class).get();
		assertJson("{foo:'qux'}", a);
		a = c.getSection(null).asBean(ABean.class).get();
		assertJson("{foo:'qux'}", a);
		a = c.getSection("S").asBean(ABean.class).get();
		assertJson("{foo:'baz'}", a);

		var b = c.getSection("").asBean(BBean.class).get();
		assertJson("{foo:'qux'}", b);
		b = c.getSection("").asBean(BBean.class).get();
		assertJson("{foo:'qux'}", b);
		b = c.getSection("S").asBean(BBean.class).get();
		assertJson("{foo:'baz'}", b);

		assertThrowsWithMessage(ParseException.class, "Unknown property 'bar' encountered in configuration section 'T'.", ()->c.getSection("T").asBean(ABean.class).get());
		assertThrowsWithMessage(ParseException.class, "Unknown property 'bar' encountered in configuration section 'T'.", ()->c.getSection("T").asBean(BBean.class).get());
	}

	//====================================================================================================
	//	public <T> T getSectionAsBean(String section, Class<T> c, boolean ignoreUnknownProperties)
	//====================================================================================================
	@Test void getSectionAsBean2() {
		var c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");

		var a = c.getSection("T").asBean(ABean.class, true).get();
		assertJson("{foo:'qux'}", a);
		var b = c.getSection("T").asBean(BBean.class, true).get();
		assertJson("{foo:'qux'}", b);

		assertThrowsWithMessage(ParseException.class, "Unknown property 'bar' encountered in configuration section 'T'.", ()->c.getSection("T").asBean(ABean.class, false).get());
		assertThrowsWithMessage(ParseException.class, "Unknown property 'bar' encountered in configuration section 'T'.", ()->c.getSection("T").asBean(BBean.class, false).get());
	}

	//====================================================================================================
	//	public JsonMap getSectionAsMap(String section)
	//====================================================================================================
	@Test void getSectionAsMap() {
		var c = init("a=1", "[S]", "b=2", "[T]");

		assertJson("{a:'1'}", c.getSection("").asMap().get());
		assertJson("{a:'1'}", c.getSection("").asMap().get());
		assertJson("{a:'1'}", c.getSection(null).asMap().get());
		assertJson("{b:'2'}", c.getSection("S").asMap().get());
		assertJson("{}", c.getSection("T").asMap().get());
		assertFalse(c.getSection("U").asMap().isPresent());
	}

	//====================================================================================================
	//	public <T> T getSectionAsInterface(String sectionName, Class<T> c)
	//====================================================================================================
	@Test void getSectionAsInterface() {
		var c = init("foo=qux", "[S]", "foo=baz", "[T]", "foo=qux", "bar=qux");

		var a = c.getSection("").asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		a = c.getSection("").asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		a = c.getSection(null).asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		a = c.getSection("S").asInterface(AInterface.class).get();
		assertEquals("baz", a.getFoo());

		a = c.getSection("T").asInterface(AInterface.class).get();
		assertEquals("qux", a.getFoo());

		assertThrowsWithMessage(IllegalArgumentException.class, "Class 'org.apache.juneau.config.Config_Test$ABean' passed to toInterface() is not an interface.", ()->c.getSection("T").asInterface(ABean.class).get());
	}

	public interface AInterface {
		String getFoo();
	}

	//====================================================================================================
	//	public boolean exists(String key)
	//====================================================================================================
	@Test void exists() {
		var c = init("a=1", "[S]", "b=2", "c=", "[T]");

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
	@Test void setSection1() {
		var c = init();

		c.setSection("", l("#C1", "#C2"));
		assertEquals("#C1|#C2||", pipedLines(c));

		c.setSection("", l("#C3", "#C4"));
		assertEquals("#C3|#C4||", pipedLines(c));

		c.setSection("S1", l("", "#C5", "#C6"));
		assertEquals("#C3|#C4|||#C5|#C6|[S1]|", pipedLines(c));

		c.setSection("S1", null);
		assertEquals("#C3|#C4|||#C5|#C6|[S1]|", pipedLines(c));

		c.setSection("S1", Collections.<String>emptyList());
		assertEquals("#C3|#C4||[S1]|", pipedLines(c));

		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'section' cannot be null.", ()->c.setSection(null, l("", "#C5", "#C6")));
	}

	//====================================================================================================
	//	public Config setSection(String name, List<String> preLines, Map<String,Object> contents)
	//====================================================================================================
	@Test void setSection2() {
		var c = init();
		var m = JsonMap.of("a", "b");

		c.setSection("", l("#C1", "#C2"), m);
		assertEquals("#C1|#C2||a = b|", pipedLines(c));

		c.setSection("", l("#C3", "#C4"), m);
		assertEquals("#C3|#C4||a = b|", pipedLines(c));

		c.setSection("S1", l("", "#C5", "#C6"), m);
		assertEquals("#C3|#C4||a = b||#C5|#C6|[S1]|a = b|", pipedLines(c));

		c.setSection("S1", null, m);
		assertEquals("#C3|#C4||a = b||#C5|#C6|[S1]|a = b|", pipedLines(c));

		c.setSection("S1", Collections.<String>emptyList(), m);
		assertEquals("#C3|#C4||a = b|[S1]|a = b|", pipedLines(c));

		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'section' cannot be null.", ()->c.setSection(null, l("", "#C5", "#C6"), m));
	}

	//====================================================================================================
	//	public Config removeSection(String name)
	//====================================================================================================
	@Test void removeSection() {
		var c = init("a=1", "[S]", "b=2", "c=", "[T]");

		c.removeSection("S");
		c.removeSection("T");

		assertEquals("a=1|", pipedLines(c));

		c.removeSection("");
		assertEquals("", pipedLines(c));
	}

	//====================================================================================================
	//	public Writer writeTo(Writer w)
	//====================================================================================================
	@Test void writeTo() throws Exception {
		var c = init("a=1", "[S]", "b=2", "c=", "[T]");

		assertEquals("a=1|[S]|b=2|c=|[T]|", pipedLines(c.writeTo(new StringWriter())));
	}

	//====================================================================================================
	// testExampleInConfig - Example in Config
	//====================================================================================================
	@Test void a01_exampleInConfig() throws Exception {

		var cf = init(
			"# Default section", "key1 = 1", "key2 = true", "key3 = [1,2,3]", "key4 = http://foo", "",
			"# section1", "# Section 1",
			"[section1]", "key1 = 2", "key2 = false", "key3 = [4,5,6]", "key4 = http://bar"
		);

		assertEquals(1, cf.get("key1").asInteger().get());
		assertTrue(cf.get("key2").asBoolean().get());
		assertEquals(3, cf.get("key3").as(int[].class).get()[2]);
		assertEquals(6, cf.get("xkey3").as(int[].class).orElse(ints(4,5,6))[2]);
		assertEquals(6, cf.get("X/key3").as(int[].class).orElse(ints(4,5,6))[2]);
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
		cf.set("key3", ints(1,2,3));
		cf.set("key4", url("http://foo"));
		cf.set("section1/key1", 2);
		cf.set("section1/key2", false);
		cf.set("section1/key3", ints(4,5,6));
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
	@Test void a02_enum() throws Exception {
		var cf = init(
			"key1 = MINUTES"
		);
		assertEquals(TimeUnit.MINUTES, cf.get("key1").as(TimeUnit.class).get());

		cf.commit();

		assertEquals(TimeUnit.MINUTES, cf.get("key1").as(TimeUnit.class).get());
	}

	//====================================================================================================
	// testEncodedValues
	//====================================================================================================
	@Test void a03_encodedValues() throws Exception {
		var cf = init(
			"[s1]", "", "foo<*> = "
		);

		cf.set("s1/foo", "mypassword");

		assertEquals("mypassword", cf.get("s1/foo").get());

		cf.commit();

		assertEquals("[s1]||foo<*> = {AwwJVhwUQFZEMg==}|", pipedLines(cf));

		assertEquals("mypassword", cf.get("s1/foo").get());

		cf.load(reader("[s1]\nfoo<*> = mypassword2\n"), true);

		assertEquals("mypassword2", cf.get("s1/foo").get());

		cf.set("s1/foo", "mypassword");

		// INI output should be encoded
		var sw = new StringWriter();
		cf.writeTo(new PrintWriter(sw));
		assertEquals("[s1]|foo<*> = {AwwJVhwUQFZEMg==}|", pipedLines(sw));
	}

	//====================================================================================================
	// public Config encodeEntries()
	//====================================================================================================
	@Test void a04_encodeEntries() throws Exception {
		var cf = init(
			"[s1]", "", "foo<*> = mypassword"
		);

		cf.applyMods();
		cf.commit();
		assertEquals("[s1]||foo<*> = {AwwJVhwUQFZEMg==}|", pipedLines(MemoryStore.DEFAULT.read("Test.cfg")));
	}

	//====================================================================================================
	// testVariables
	//====================================================================================================
	@Test void a05_variables() {

		var cf = init(
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
	@Test void a06_xorEncoder() {
		testXor("foo");
		testXor("");
		testXor("123");
		testXor("€");  // 3-byte UTF-8 character
		testXor("𤭢"); // 4-byte UTF-8 character
	}

	private static void testXor(String in) {
		var e = new XorEncodeMod();
		var s = e.apply(in);
		var s2 = e.remove(s);
		assertEquals(in, s2);
	}

	//====================================================================================================
	// testHex
	//====================================================================================================
	@Test void a07_hex() throws Exception {
		var cf = init().copy().binaryFormat(BinaryFormat.HEX).build();

		cf.set("foo", "bar".getBytes("UTF-8"));
		assertEquals("626172", cf.get("foo").getValue());
		assertJson("[98,97,114]", cf.get("foo").asBytes().get());
	}

	//====================================================================================================
	// testSpacedHex
	//====================================================================================================
	@Test void a08_spacedHex() throws Exception {
		var cf = init().copy().binaryFormat(BinaryFormat.SPACED_HEX).build();

		cf.set("foo", "bar".getBytes("UTF-8"));
		assertEquals("62 61 72", cf.get("foo").getValue());
		assertJson("[98,97,114]", cf.get("foo").asBytes().get());
	}

	//====================================================================================================
	// testMultiLines
	//====================================================================================================
	@Test void a09_multiLines() throws Exception {
		var cf = init(
			"[s1]",
			"f1 = x \n\ty \n\tz"
		);

		assertEquals("x \ny \nz", cf.get("s1/f1").get());

		var sw = new StringWriter();
		cf.writeTo(sw);
		assertEquals("[s1]|f1 = x |\ty |\tz|", pipedLines(sw));
	}

	//====================================================================================================
	// testNumberShortcuts
	//====================================================================================================
	@Test void a10_numberShortcuts() {
		var cf = init(
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
	@Test void a11_listeners() throws Exception {
		var cf = init();

		final var changes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		cf.addListener(events -> {
			for (var ce : events) {
				var key = (ce.getSection().isEmpty() ? "" : (ce.getSection() + '/')) + ce.getKey();
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
		assertJson("[]", changes);
		cf.commit();
		assertJson("['a1=3','a3=3','B/b1=3','B/b3=3']", changes);

		// Rollback.
		changes.clear();
		cf.set("a1", 3);
		cf.set("a3", 3);
		cf.set("B/b1", 3);
		cf.set("B/b3", 3);
		assertJson("[]", changes);
		cf.rollback();
		cf.commit();
		assertJson("[]", changes);

		// Overwrite
		changes.clear();
		cf.set("a1", "2");
		cf.set("B/b1", "2");
		cf.set("a2", "2");
		cf.set("B/b2", "2");
		cf.set("C/c1", "2");
		cf.set("C/c2", "2");
		cf.commit();
		assertJson("['a1=2','a2=2','B/b1=2','B/b2=2','C/c1=2','C/c2=2']", changes);

		// Encoded
		changes.clear();
		cf.set("a4", "4", null, "*", null, null);
		cf.set("B/b4", "4", null, "*", null, null);
		cf.commit();
		assertJson("['a4={Wg==}','B/b4={Wg==}']", changes);

		// Encoded overwrite
		changes.clear();
		cf.set("a4", "5");
		cf.set("B/b4", "5");
		cf.commit();
		assertJson("['a4={Ww==}','B/b4={Ww==}']", changes);

		// Remove entries
		changes.clear();
		cf.remove("a4");
		cf.remove("ax");
		cf.remove("B/b4");
		cf.remove("B/bx");
		cf.remove("X/bx");
		cf.commit();
		assertJson("['REMOVE_ENTRY(a4)','REMOVE_ENTRY(B/b4)']", changes);

		// Add section
		// Shouldn't trigger listener.
		changes.clear();
		cf.setSection("D", l("#comment"));
		cf.commit();
		assertJson("[]", changes);

		// Add section with contents
		changes.clear();
		cf.setSection("E", null, m("e1","1","e2","2"));
		cf.commit();
		assertJson("['E/e1=1','E/e2=2']", changes);

		// Remove section
		changes.clear();
		cf.removeSection("E");
		cf.commit();
		assertJson("['REMOVE_ENTRY(E/e1)','REMOVE_ENTRY(E/e2)']", changes);

		// Remove non-existent section
		changes.clear();
		cf.removeSection("E");
		cf.commit();
		assertJson("[]", changes);
	}

	//====================================================================================================
	// getObjectArray(Class c, String key)
	// getObjectArray(Class c, String key, T[] def)
	//====================================================================================================
	@Test void a12_getObjectArray() {
		var cf = init("[A]", "a1=[1,2,3]");
		assertJson("[1,2,3]", cf.get("A/a1").as(Integer[].class).get());
		assertJson("[4,5,6]", cf.get("A/a2").as(Integer[].class).orElse(a(4,5,6)));
		assertJson("[7,8,9]", cf.get("B/a1").as(Integer[].class).orElse(a(7,8,9)));
		assertFalse(cf.get("B/a1").as(Integer[].class).isPresent());

		cf = init("[A]", "a1 = [1 ,\n\t2 ,\n\t3] ");
		assertJson("[1,2,3]", cf.get("A/a1").as(Integer[].class).get());

		// We cannot cast primitive arrays to Object[], so the following throws exceptions.
		assertJson("[1,2,3]", cf.get("A/a1").as(int[].class).get());
		assertEquals("int", cf.get("A/a1").as(int[].class).get().getClass().getComponentType().getSimpleName());
		assertFalse(cf.get("B/a1").as(int[].class).isPresent());
		assertEquals("int", cf.get("B/a1").as(int[].class).orElse(new int[0]).getClass().getComponentType().getSimpleName());
		assertFalse(cf.get("A/a2").as(int[].class).isPresent());
		assertEquals("int", cf.get("A/a2").as(int[].class).orElse(new int[0]).getClass().getComponentType().getSimpleName());

		assertJson("[1,2,3]", cf.get("A/a1").as(int[].class).orElse(ints(4)));
		assertEquals("int", cf.get("A/a1").as(int[].class).orElse(ints(4)).getClass().getComponentType().getSimpleName());
		assertJson("[4]", cf.get("B/a1").as(int[].class).orElse(ints(4)));
		assertEquals("int", cf.get("B/a1").as(int[].class).orElse(ints(4)).getClass().getComponentType().getSimpleName());
		assertJson("[4]", cf.get("A/a2").as(int[].class).orElse(ints(4)));
		assertEquals("int", cf.get("A/a2").as(int[].class).orElse(ints(4)).getClass().getComponentType().getSimpleName());

		System.setProperty("X", "[4,5,6]");
		cf = init("x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "[A]", "a1=[1,2,3]");
		assertJson("[1,2,3]", cf.get("x1").as(int[].class).orElse(ints(9)));
		assertJson("[4,5,6]", cf.get("x2").as(int[].class).orElse(ints(9)));
		assertJson("[]", cf.get("x3").as(int[].class).orElse(ints(9)));
		System.clearProperty("X");
	}

	//====================================================================================================
	// getStringArray(String key)
	// getStringArray(String key, String[] def)
	//====================================================================================================
	@Test void a13_getStringArray() {
		var cf = init("[A]", "a1=1,2,3");
		assertJson("['1','2','3']", cf.get("A/a1").asStringArray().get());
		assertJson("['4','5','6']", cf.get("A/a2").asStringArray().orElse(a("4","5","6")));
		assertJson("['7','8','9']", cf.get("B/a1").asStringArray().orElse(a("7","8","9")));
		assertNull(cf.get("B/a1").asStringArray().orElse(null));

		cf = init("[A]", "a1 = 1 ,\n\t2 ,\n\t3 ");
		assertJson("['1','2','3']", cf.get("A/a1").asStringArray().get());

		System.setProperty("X", "4,5,6");
		cf = init(null, "x1=$C{A/a1}", "x2=$S{X}", "x3=$S{Y}", "x4=$S{Y,$S{X}}", "[A]", "a1=1,2,3");
		assertJson("['1','2','3']", cf.get("x1").asStringArray().orElse(a("9")));
		assertJson("['4','5','6']", cf.get("x2").asStringArray().orElse(a("9")));
		assertJson("['9']", cf.get("x9").asStringArray().orElse(a("9")));

		System.clearProperty("X");
	}

	//====================================================================================================
	// getSectionMap(String name)
	//====================================================================================================
	@Test void a14_getSectionMap() {
		var cf = init("[A]", "a1=1", "", "[D]", "d1=$C{A/a1}","d2=$S{X}");

		assertJson("{a1:'1'}", cf.getSection("A").asMap().get());
		assertFalse(cf.getSection("B").asMap().isPresent());
		assertJson("null", cf.getSection("C").asMap().orElse(null));

		var m = cf.getSection("A").asMap().get();
		assertJson("{a1:'1'}", m);

		System.setProperty("X", "x");
		m = cf.getSection("D").asMap().get();
		assertJson("{d1:'1',d2:'x'}", m);
		System.clearProperty("X");
	}

	//====================================================================================================
	// toWritable()
	//====================================================================================================
	@Test void a15_toWritable() throws Exception {
		var cf = init("a = b");

		var sw = new StringWriter();
		cf.writeTo(sw);
		assertEquals("a = b|", pipedLines(sw));
	}

	//====================================================================================================
	// Test resolving with override
	//====================================================================================================
	@Test void a16_resolvingWithOverride() {
		var cf = init();
		cf.set("a", "$A{X}");
		cf.set("b", "$B{X}");
		cf.set("c", "$A{$B{X}}");
		cf.set("d", "$B{$A{X}}");
		cf.set("e", "$D{X}");

		var vr = VarResolver.create().defaultVars().vars(ALVar.class, BLVar.class).build();

		cf = cf.resolving(vr.createSession());

		assertEquals("aXa", cf.get("a").get());
		assertEquals("bXb", cf.get("b").get());
		assertEquals("abXba", cf.get("c").get());
		assertEquals("baXab", cf.get("d").get());
		assertEquals("$D{X}", cf.get("e").get());

		// Create new resolver that addx $C and overrides $A
		var vr2 = vr.copy().vars(AUVar.class, DUVar.class).build();

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
	@Test void a17_poundSignEscape() throws Exception {
		var cf = init();
		cf.set("a", "a,#b,=c");
		cf.set("A/a", "a,#b,=c");

		assertEquals("a = a,\\u0023b,=c|[A]|a = a,\\u0023b,=c|", pipedLines(cf));
		cf.commit();
		assertEquals("a = a,\\u0023b,=c|[A]|a = a,\\u0023b,=c|", pipedLines(cf));

		assertEquals("a,#b,=c", cf.get("a").get());
		assertEquals("a,#b,=c", cf.get("A/a").get());

		cf.set("a", "a,#b,=c", null, null, "comment#comment", null);
		assertEquals("a = a,\\u0023b,=c # comment#comment|[A]|a = a,\\u0023b,=c|", pipedLines(cf));
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
		public BBean setFoo(String v) {this.foo = v; return this;}
		public BBean init() {
			foo = "bar";
			return this;
		}
	}

	@Test void a18_getCandidateSystemDefaultConfigNames() {

		System.setProperty("juneau.configFile", "foo.txt");
		assertJson("['foo.txt']", Config.getCandidateSystemDefaultConfigNames());

		System.clearProperty("juneau.configFile");
		assertJson("['test.cfg','juneau.cfg','default.cfg','application.cfg','app.cfg','settings.cfg','application.properties']", Config.getCandidateSystemDefaultConfigNames());
	}

	//====================================================================================================
	//	setSystemProperties
	//====================================================================================================

	@Test void setSystemProperties() {
		var c = init("a=1", "[S]", "b=2");
		c.setSystemProperties();
		assertEquals("1", System.getProperty("a"));
		assertEquals("2", System.getProperty("S/b"));
	}

	//====================================================================================================
	// Section.asBean(Class, boolean) - section not present
	//====================================================================================================

	@Test void a19_sectionAsBean_sectionNotPresent() throws Exception {
		var c = init("foo=bar");
		assertFalse(c.getSection("NONEXISTENT").asBean(ABean.class, true).isPresent());
		assertFalse(c.getSection("NONEXISTENT").asBean(ABean.class, false).isPresent());
	}

	//====================================================================================================
	// Section.writeToBean - section not present throws
	//====================================================================================================

	@Test void a20_sectionWriteToBean_sectionNotPresent() {
		var c = init("foo=bar");
		assertThrowsWithMessage(IllegalArgumentException.class, "Section 'NONEXISTENT' not found in configuration.", ()->c.getSection("NONEXISTENT").writeToBean(new ABean(), true));
	}

	//====================================================================================================
	// Section.asInterface - unsupported method
	//====================================================================================================

	@Test void a21_sectionAsInterface_unsupportedMethod() {
		var c = init("[S]", "x=1");
		var iface = c.getSection("S").asInterface(A21_Interface.class).get();
		assertThrowsWithMessage(UnsupportedOperationException.class, "Unsupported interface method.", iface::doSomething);
	}

	public interface A21_Interface {
		String doSomething();
	}

	//====================================================================================================
	// Entry.asBytes with HEX and SPACED_HEX formats
	//====================================================================================================

	@Test void a22_entryAsBytes_hex() throws Exception {
		MemoryStore.DEFAULT.update("Test.cfg", "a=48656C6C6F");
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").binaryFormat(org.apache.juneau.BinaryFormat.HEX).build().rollback();
		var bytes = c.get("a").asBytes().get();
		assertEquals("Hello", new String(bytes));
	}

	@Test void a23_entryAsBytes_spacedHex() throws Exception {
		MemoryStore.DEFAULT.update("Test.cfg", "a=48 65 6C 6C 6F");
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").binaryFormat(org.apache.juneau.BinaryFormat.SPACED_HEX).build().rollback();
		var bytes = c.get("a").asBytes().get();
		assertEquals("Hello", new String(bytes));
	}

	@Test void a24_entryAsBytes_base64() throws Exception {
		MemoryStore.DEFAULT.update("Test.cfg", "a=SGVsbG8=");
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").binaryFormat(org.apache.juneau.BinaryFormat.BASE64).build().rollback();
		var bytes = c.get("a").asBytes().get();
		assertEquals("Hello", new String(bytes));
	}

	@Test void a25_entryAsBytes_withNewlines() throws Exception {
		MemoryStore.DEFAULT.update("Test.cfg", "a=SGVs\n\tbG8=");
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").binaryFormat(org.apache.juneau.BinaryFormat.BASE64).build().rollback();
		var bytes = c.get("a").asBytes().get();
		assertEquals("Hello", new String(bytes));
	}

	//====================================================================================================
	// Entry as(Parser, Type, args...) with simple types via parser
	//====================================================================================================

	@Test void a26_entryAs_simpleTypeViaParser() throws Exception {
		var c = init("myEnum=ONE");
		var result = c.get("myEnum").as(org.apache.juneau.uon.UonParser.DEFAULT, A26_MyEnum.class);
		assertEquals(A26_MyEnum.ONE, result.get());
	}

	public enum A26_MyEnum { ONE, TWO, THREE }

	//====================================================================================================
	// Entry as(Type, args) uses default parser
	//====================================================================================================

	@Test void a27_entryAs_typeWithArgs() throws Exception {
		var c = init("[S]", "list=['a','b','c']");
		var result = c.get("S/list").as(java.util.List.class, String.class);
		assertTrue(result.isPresent());
	}

	//====================================================================================================
	// Entry.asDouble() - non-empty branch
	//====================================================================================================

	@Test void a28_entryAsDouble() {
		var c = init("a=3.14", "b=");
		assertEquals(3.14, c.get("a").asDouble().get(), 0.001);
		assertFalse(c.get("b").asDouble().isPresent());
		assertFalse(c.get("x").asDouble().isPresent());
	}

	//====================================================================================================
	// Entry.asFloat() - non-empty branch
	//====================================================================================================

	@Test void a29_entryAsFloat() {
		var c = init("a=1.5", "b=");
		assertEquals(1.5f, c.get("a").asFloat().get(), 0.001f);
		assertFalse(c.get("b").asFloat().isPresent());
		assertFalse(c.get("x").asFloat().isPresent());
	}

	//====================================================================================================
	// Entry.asMap() - no-arg form delegates to asMap(parser); JSON parser wraps value without braces
	//====================================================================================================

	@Test void a30_entryAsMap_noBraces() throws Exception {
		var c = init("a=\"key\":\"value\"");
		var m = c.get("a").asMap().get();
		assertEquals("value", m.get("key"));
		assertFalse(c.get("x").asMap().isPresent());
	}

	//====================================================================================================
	// Entry.asList() - no-arg form delegates to asList(parser); JSON parser wraps value without brackets
	//====================================================================================================

	@Test void a31_entryAsList_noBrackets() throws Exception {
		var c = init("a=\"x\",\"y\"");
		var list = c.get("a").asList().get();
		assertEquals(2, list.size());
		assertEquals("x", list.get(0));
		assertEquals("y", list.get(1));
		assertFalse(c.get("x").asList().isPresent());
	}

	//====================================================================================================
	// Entry.asStringArray() - JSON path: when value starts with '[', parses as JSON array
	//====================================================================================================

	@Test void a32_entryAsStringArray_jsonPath() {
		var c = init("a=[\"x\",\"y\",\"z\"]");
		var arr = c.get("a").asStringArray().get();
		assertEquals(3, arr.length);
		assertEquals("x", arr[0]);
		assertEquals("y", arr[1]);
		assertEquals("z", arr[2]);
	}

	//====================================================================================================
	// Entry.get() - throws NullPointerException when entry is absent
	//====================================================================================================

	@Test void a33_entryGet_nullThrow() {
		var c = init("a=1");
		assertThrows(NullPointerException.class, () -> c.get("missing").get());
	}

	//====================================================================================================
	// Entry.getComment(), getKey(), getModifiers(), getPreLines(), getValue()
	//====================================================================================================

	@Test void a34_entryMetadataGetters() throws Exception {
		var c = init();
		c.set("a", "hello", null, "*", "my comment", l("#preline"));
		var entry = c.get("a");
		assertEquals("a", entry.getKey());
		assertEquals("my comment", entry.getComment());
		assertEquals("*", entry.getModifiers());
		assertEquals(1, entry.getPreLines().size());
		assertEquals("#preline", entry.getPreLines().get(0));
		assertNotNull(entry.getValue());
	}

	//====================================================================================================
	// Entry.isNotEmpty()
	//====================================================================================================

	@Test void a35_entryIsNotEmpty() {
		var c = init("a=hello", "b=");
		assertTrue(c.get("a").isNotEmpty());
		assertFalse(c.get("b").isNotEmpty());
		assertFalse(c.get("x").isNotEmpty());
	}

	//====================================================================================================
	// Entry.orElseGet(Supplier<String>)
	//====================================================================================================

	@Test void a36_entryOrElseGet() {
		var c = init("a=hello");
		assertEquals("hello", c.get("a").orElseGet(() -> "default"));
		assertEquals("default", c.get("missing").orElseGet(() -> "default"));
	}

	//====================================================================================================
	// Entry.toString() - returns null when entry is absent
	//====================================================================================================

	@Test void a37_entryToString_nullPath() {
		var c = init("a=1");
		assertNull(c.get("missing").toString());
	}

	//====================================================================================================
	// Entry.as(Parser, Type...) - dispatch for Long, JsonMap, and JsonList types
	//====================================================================================================

	@Test void a38_entryAs_typeDispatch() throws Exception {
		var c = init("a=123", "b={\"k\":\"v\"}", "d=[\"x\"]");
		assertEquals(123L, c.get("a").as(JsonParser.DEFAULT, Long.class).get());
		var m = c.get("b").as(JsonParser.DEFAULT, JsonMap.class).get();
		assertEquals("v", m.get("k"));
		var list = c.get("d").as(JsonParser.DEFAULT, JsonList.class).get();
		assertEquals("x", list.get(0));
	}

	//====================================================================================================
	// Entry.as(Json5Parser, URL.class) - triggers JSON string wrapping for non-array/non-object type
	//====================================================================================================

	@Test void a39_entryAs_jsonWrapping() throws Exception {
		var c = init("a=http://example.com");
		var u = c.get("a").as(Json5Parser.DEFAULT, URL.class).get();
		assertEquals("http://example.com", u.toString());
	}

	//====================================================================================================
	// Config.Builder.annotations(Annotation...)
	//====================================================================================================

	@Test void a40_builderAnnotationsVarargs() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").annotations().build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.Builder.annotations(List<Annotation>)
	//====================================================================================================

	@Test void a41_builderAnnotationsList() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").annotations(List.of()).build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.Builder.apply(AnnotationWorkList)
	//====================================================================================================

	@Test void a42_builderApply() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").apply(AnnotationWorkList.create()).build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.Builder.binaryLineLength(int)
	//====================================================================================================

	@Test void a43_builderBinaryLineLength() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").binaryLineLength(80).build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.Builder.cache(Cache...)
	//====================================================================================================

	@Test void a44_builderCache() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").cache(null).build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.Builder.debug() and debug(boolean)
	//====================================================================================================

	@Test void a45_builderDebug() {
		var c1 = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").debug().build().rollback();
		assertNotNull(c1);
		var c2 = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").debug(false).build().rollback();
		assertNotNull(c2);
	}

	//====================================================================================================
	// Config.Builder.memStore()
	//====================================================================================================

	@Test void a46_builderMemStore() {
		var c = Config.create().memStore().name("Test.cfg").build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.Builder.multiLineValuesOnSeparateLines()
	//====================================================================================================

	@Test void a47_builderMultiLineValues() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").multiLineValuesOnSeparateLines().build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.Builder.parser(ReaderParser)
	//====================================================================================================

	@Test void a48_builderParser() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").parser(JsonParser.DEFAULT).build().rollback();
		assertNotNull(c);
	}

	//====================================================================================================
	// Config.getName()
	//====================================================================================================

	@Test void a49_getName() {
		var c = init("a=1");
		assertEquals("Test.cfg", c.getName());
	}

	//====================================================================================================
	// Config.getString(String key)
	//====================================================================================================

	@Test void a50_getString() {
		var c = init("a=1", "[S]", "b=2");
		assertEquals("1", c.getString("a"));
		assertEquals("2", c.getString("S/b"));
		assertNull(c.getString("missing"));
	}

	//====================================================================================================
	// Config.load(Map<String,Map<String,Object>>)
	//====================================================================================================

	@Test void a51_load_map() throws Exception {
		var c = init();
		var data = new LinkedHashMap<String, Map<String, Object>>();
		data.put("S", JsonMap.of("k", "v"));
		c.load(data);
		assertEquals("v", c.get("S/k").get());
	}

	//====================================================================================================
	// Config.setSystemDefault(Config) / Config.getSystemDefault()
	//====================================================================================================

	@Test void a52_systemDefault() {
		var c = init("a=1");
		Config.setSystemDefault(c);
		assertSame(c, Config.getSystemDefault());
		Config.setSystemDefault(null);
	}

	//====================================================================================================
	// Config.setImport(String, String, List) - throws UnsupportedOperationException (not supported)
	//====================================================================================================

	@Test void a53_setImport() {
		var c = init();
		assertThrows(UnsupportedOperationException.class, () -> c.setImport("", "other.cfg", null));
	}

	//====================================================================================================
	// Binary serialization with binaryLineLength - splits encoded value with newlines
	//====================================================================================================

	@Test void a54_binaryLineLength_splitting() throws Exception {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").binaryLineLength(4).build().rollback();
		c.set("a", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
		assertTrue(c.get("a").orElse("").contains("\n"));
	}

	//====================================================================================================
	// multiLineValuesOnSeparateLines - serializes multi-line string values starting on new line
	//====================================================================================================

	@Test void a55_multiLineOnSeparateLines() throws Exception {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").multiLineValuesOnSeparateLines().build().rollback();
		c.set("a", "line1\nline2");
		assertTrue(c.get("a").orElse("").contains("line1"));
	}

	//====================================================================================================
	// Config.Builder.readOnly() - throws UnsupportedOperationException on write operations
	//====================================================================================================

	@Test void a56_readOnly() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").readOnly().build();
		assertThrows(UnsupportedOperationException.class, () -> c.set("a", "1"));
	}

	@Test void a57_builderCopyMethod() {
		var b = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg");
		var b2 = b.copy();
		var c = b2.build().rollback();
		assertNotNull(c);
	}

	@Test void a58_builderApplyAnnotationsClass() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").applyAnnotations(String.class).build().rollback();
		assertNotNull(c);
	}

	@Test void a59_builderApplyAnnotationsObject() {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").applyAnnotations("foo").build().rollback();
		assertNotNull(c);
	}

	@Test void a60_builderImpl() {
		var existing = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").build().rollback();
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").impl(existing).build();
		assertNotNull(c);
	}

	@Test void a61_removeImport() throws Exception {
		var c = init();
		assertThrows(UnsupportedOperationException.class, () -> c.removeImport("", "nonexistent.cfg"));
	}

	@Test void a62_set_nullValueNonExistentKey() throws Exception {
		var c = init();
		c.set("nonExistent", (String)null);
		assertNull(c.get("nonExistent").orElse(null));
	}

	@Test void a63_properties_toStringCoversProperties() throws Exception {
		var c = Config.create()
			.store(MemoryStore.DEFAULT)
			.name("Test.cfg")
			.binaryFormat(BinaryFormat.HEX)
			.binaryLineLength(80)
			.multiLineValuesOnSeparateLines()
			.readOnly()
			.build();
		assertNotNull(c.toString());
	}

	@Test void a64_serialize_null() throws Exception {
		var c = init("a=1");
		c.set("a", (Object)null);
		assertEquals("", c.get("a").orElse(null));
	}

	@Test void a65_multiLineValues_withObject() throws Exception {
		var c = Config.create()
			.store(MemoryStore.DEFAULT)
			.name("Test.cfg")
			.multiLineValuesOnSeparateLines()
			.build()
			.rollback();
		c.set("a", List.of("x", "y", "z"));
		assertNotNull(c.get("a").orElse(null));
	}

	@Test void a66_applyMods_nullBranches() {
		var c = init();
		assertEquals("x", c.applyMods(null, "x"));
		assertEquals(null, c.applyMods("x", null));
	}

	@Test void a67_removeMods_nullBranches() {
		var c = init();
		assertEquals("x", c.removeMods(null, "x"));
		assertEquals(null, c.removeMods("x", null));
	}

	@Test void a68_setSystemProperties() throws Exception {
		var c = init("sysProp=testValue", "[S]", "k=v");
		c.setSystemProperties();
		assertEquals("testValue", System.getProperty("sysProp"));
		System.clearProperty("sysProp");
	}

	@Test void a69_setSection_stringList() throws Exception {
		var c = init();
		c.setSection("NewSection", List.of("# A comment"));
		assertTrue(c.getSectionNames().contains("NewSection"));
	}

	@Test void a70_binaryLineLength_noSplit_shortValue() throws Exception {
		var c = Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").binaryLineLength(100).build().rollback();
		c.set("a", new byte[]{1, 2, 3});
		assertFalse(c.get("a").orElse("").contains("\n"));
	}

	@Test void a71_load_nullMap() throws Exception {
		var c = init("a=1");
		c.load(null);
		assertEquals("1", c.get("a").get());
	}

	@Test void a72_load_synchronous() throws Exception {
		var c = init("");
		c.load("[S1]\na=1", true);
		assertEquals("1", c.get("S1/a").get());
	}

	@Test void a73_load_withImport() throws Exception {
		var c = init("");
		c.load("<ImportCfg>\n[S1]\na=1", false);
		assertNotNull(c.toMap());
		assertNotNull(c.getSectionNames());
		assertNotNull(c.getKeys("S1"));
		assertTrue(c.getSection("S1") != null);
		assertNotNull(c.getSectionNames());
	}

	@Test void a74_removeSection_nonExistent() throws Exception {
		var c = init("[S1]\na=1");
		assertDoesNotThrow(() -> c.removeSection("NonExistentSection"));
		assertEquals("1", c.get("S1/a").get());
	}

	@Test void a75_configMap_load_null() throws Exception {
		var c = init("[S1]\na=1");
		MemoryStore.DEFAULT.update("Test.cfg", (String)null);
		assertNull(c.get("S1/a").orElse(null));
	}

	@Test void a76_configMap_invalidImportStartDigit() throws Exception {
		var c = init("");
		assertThrows(Exception.class, () -> c.load("<1invalid>", false));
	}

	@Test void a77_configMap_invalidImportWithSpace() throws Exception {
		var c = init("");
		assertThrows(Exception.class, () -> c.load("<a b>", false));
	}

	@Test void a78_configMap_emptyImportName() throws Exception {
		var c = init("");
		assertThrows(Exception.class, () -> c.load("<>", false));
	}

	@Test void a79_configMap_importWithExtraContent() throws Exception {
		var c = init("");
		assertThrows(Exception.class, () -> c.load("<validname> extraStuff", false));
	}

	@Test void a80_configMap_invalidSectionName() throws Exception {
		var c = init("");
		assertThrows(Exception.class, () -> c.load("[invalid-section!", false));
	}

	@Test void a81_configMap_findDiffs_importsAddedAndRemoved() throws Exception {
		var c = init("[S1]\na=1");
		c.load("<ImportCfg>\n[S1]\na=1", false);
		c.load("[S1]\na=1", false);
		assertEquals("1", c.get("S1/a").get());
	}

	@Test void a82_configMap_importListedTwice() throws Exception {
		var c = init("");
		c.load("<ImportCfg>\n<ImportCfg>\n[S1]\na=1", false);
		assertNotNull(c.getSectionNames());
	}

	@Test void a83_configMap_importLineNoClosingBracket() throws Exception {
		var c = init("");
		c.load("<noClosingBracket\n[S1]\na=1", false);
		assertEquals("1", c.get("S1/a").get());
	}

	@Test void a84_configMap_tabIndentedComment() throws Exception {
		var c = init("");
		c.load("[S1]\na=value1\n\t#this is a comment\nb=value2", false);
		assertEquals("value1", c.get("S1/a").get());
		assertEquals("value2", c.get("S1/b").get());
	}

	@Test void a85_configMap_importWithHashComment() throws Exception {
		var c = init("");
		c.load("<ImportCfg>#comment\n[S1]\na=1", false);
		assertEquals("1", c.get("S1/a").get());
	}

	@Test void a86_configMapEntry_keyWithModifierNoClose() throws Exception {
		var c = init("[S1]\nkey<notclosed=value");
		assertEquals("value", c.get("S1/key").get());
	}

	@Test void a87_configMapEntry_valueWithBackslash() throws Exception {
		var c = init("[S1]\nkey=normal");
		c.set("S1/key", "value\\with\\backslash");
		c.commit();
		var s = c.toString();
		assertTrue(s.contains("key"));
	}

	@Test void a88_configMapEntry_valueWithIsoControlChar() throws Exception {
		var c = init("[S1]\nkey=normal");
		c.set("S1/key", "value\u0001control");
		c.commit();
		var s = c.toString();
		assertTrue(s.contains("key"));
	}
}