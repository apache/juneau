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
package org.apache.juneau.svl;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class ResolvingJsonMapTest extends TestBase {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test void a01_basic() {
		var vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		var m = new ResolvingJsonMap(vr.createSession());

		m.put("foo", "$X{a}");
		assertEquals("1", m.get("foo"));

		m.put("foo", new String[]{"$X{a}"});
		assertList(m.get("foo"), "1");

		m.put("foo", list("$X{a}"));
		assertList(m.get("foo"), "1");

		m.put("foo", map("k1","$X{a}"));
		assertMap(m, "foo{k1}", "{1}");
	}

	public static class XVar extends MapVar {
		public XVar() {
			super("X", JsonMap.of("a", 1, "b", 2, "c", 3));
		}
	}

	//====================================================================================================
	// testNulls
	//====================================================================================================
	@Test void a02_nulls() {
		var vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		var m = new ResolvingJsonMap(vr.createSession());

		m.put("foo", null);
		assertNull(m.get("foo"));

		m.put("foo", new String[]{null});
		assertList(m.get("foo"), (Object)null);

		m.put("foo", list((String)null));
		assertList(m.get("foo"), (Object)null);

		m.put("foo", map("k1",null));
		assertMap(m, "foo{k1}", "{<null>}");
	}

	//====================================================================================================
	// testNonStrings
	//====================================================================================================
	@Test void a03_nonStrings() {
		var vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		var m = new ResolvingJsonMap(vr.createSession());

		m.put("foo", FooEnum.ONE);
		assertString("ONE", m.get("foo"));
		m.put("foo", new Object[]{FooEnum.ONE});
		assertList(m.get("foo"), "ONE");

		m.put("foo", list(FooEnum.ONE));
		assertList(m.get("foo"), "ONE");

		m.put("foo", map(FooEnum.ONE,FooEnum.ONE));
		assertBean(m, "foo", "{ONE=ONE}");
	}

	public enum FooEnum {
		ONE
	}

	//====================================================================================================
	// testInner - Test inner maps
	//====================================================================================================
	@Test void a04_inner() {
		var vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		var m = new ResolvingJsonMap(vr.createSession());
		var m2 = new JsonMap();
		var m3 = new JsonMap();
		m.inner(m2);
		m2.inner(m3);

		m3.put("foo", "$X{a}");
		assertEquals("1", m.get("foo"));

		m3.put("foo", new String[]{"$X{a}"});
		assertList(m.get("foo"), "1");

		m3.put("foo", list("$X{a}"));
		assertList(m.get("foo"), "1");

		m3.put("foo", map("k1","$X{a}"));
		assertMap(m, "foo{k1}", "{1}");
	}

	//====================================================================================================
	// testFluentSetters - Test fluent setter overrides
	//====================================================================================================
	@Test void a05_fluentSetters() {
		var vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		var m = new ResolvingJsonMap(vr.createSession());

		// Test inner() returns same instance for fluent chaining
		Map<String,Object> innerMap = new HashMap<>();
		innerMap.put("test", "$X{a}");
		assertSame(m, m.inner(innerMap));
		assertEquals("1", m.get("test"));

		// Test session() returns same instance
		BeanSession session = BeanContext.DEFAULT.getSession();
		assertSame(m, m.session(session));

		// Test append(String, Object) returns same instance
		assertSame(m, m.append("key1", "$X{b}"));
		assertEquals("2", m.get("key1"));

		// Test append(Map) returns same instance
		Map<String,Object> appendMap = new HashMap<>();
		appendMap.put("key2", "$X{c}");
		assertSame(m, m.append(appendMap));
		assertEquals("3", m.get("key2"));

		// Test appendIf() returns same instance
		assertSame(m, m.appendIf(true, "key3", "value3"));
		assertEquals("value3", m.get("key3"));
		assertSame(m, m.appendIf(false, "key4", "value4"));
		assertNull(m.get("key4"));

		// Test filtered() returns same instance
		assertSame(m, m.filtered(x -> x != null));

		// Test keepAll() returns same instance
		assertSame(m, m.keepAll("key1", "key2"));

		// Test setBeanSession() returns same instance
		assertSame(m, m.setBeanSession(session));

		// Test modifiable() returns a new instance when unmodifiable
		assertSame(m, m.modifiable());

		// Test unmodifiable() returns same instance
		assertSame(m, m.unmodifiable());
	}

	@Test void a06_fluentChaining() {
		var vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		// Test multiple fluent calls can be chained
		var m = new ResolvingJsonMap(vr.createSession())
			.append("key1", "$X{a}")
			.append("key2", "$X{b}")
			.appendIf(true, "key3", "$X{c}");

		assertEquals("1", m.get("key1"));
		assertEquals("2", m.get("key2"));
		assertEquals("3", m.get("key3"));
	}
}