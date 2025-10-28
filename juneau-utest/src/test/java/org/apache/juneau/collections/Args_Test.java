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
package org.apache.juneau.collections;

import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Args_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// test - Basic tests
	//-----------------------------------------------------------------------------------------------------------------
	@Test void basic() {
		var a = new Args(a());

		// Empty args
		assertNull(a.getArg(0));
		assertNull(a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(a("foo"));
		assertEquals("foo", a.getArg(0));
		assertNull(a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(a("foo", "bar bar"));
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(a("foo", "bar bar", "-foo"));
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertTrue(a.containsKey("foo"));

		a = new Args(a("foo", "bar bar", "-foo", "bar bar"));
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertEquals("bar bar", a.getArg("foo"));
		assertEquals(1, a.getArgs("foo").size());
		assertEquals("bar bar", a.getArgs("foo").get(0));
		assertTrue(a.containsKey("foo"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// test - Fluent setters
	//-----------------------------------------------------------------------------------------------------------------
	@Test void fluentSetters() {
		Args a = new Args(a("main1"));

		// Test inner() returns same instance for fluent chaining
		Map<String,Object> innerMap = new HashMap<>();
		innerMap.put("test", "value");
		assertSame(a, a.inner(innerMap));

		// Test session() returns same instance
		BeanSession session = BeanContext.DEFAULT.getSession();
		assertSame(a, a.session(session));

		// Test append(String, Object) returns same instance
		assertSame(a, a.append("key1", "value1"));
		assertEquals("value1", a.get("key1"));

		// Test append(Map) returns same instance
		Map<String,Object> appendMap = new HashMap<>();
		appendMap.put("key2", "value2");
		assertSame(a, a.append(appendMap));
		assertEquals("value2", a.get("key2"));

		// Test appendIf() returns same instance
		assertSame(a, a.appendIf(true, "key3", "value3"));
		assertEquals("value3", a.get("key3"));
		assertSame(a, a.appendIf(false, "key4", "value4"));
		assertNull(a.get("key4"));

		// Test filtered() returns same instance
		assertSame(a, a.filtered(x -> x != null));

		// Test keepAll() returns same instance
		assertSame(a, a.keepAll("key1", "key2"));

		// Test setBeanSession() returns same instance
		assertSame(a, a.setBeanSession(session));

		// Test modifiable() returns same instance
		assertSame(a, a.modifiable());

		// Test unmodifiable() returns same instance
		assertSame(a, a.unmodifiable());
	}

	@Test void fluentChaining() {
		// Test multiple fluent calls can be chained
		Args a = new Args(a("main1"))
			.append("key1", "value1")
			.append("key2", "value2")
			.appendIf(true, "key3", "value3");

		assertEquals("value1", a.get("key1"));
		assertEquals("value2", a.get("key2"));
		assertEquals("value3", a.get("key3"));
	}
}