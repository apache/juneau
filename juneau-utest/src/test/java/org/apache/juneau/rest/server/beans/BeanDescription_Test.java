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
package org.apache.juneau.rest.server.beans;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BeanDescription} and {@link BeanDescription.BeanPropertyDescription}.
 */
class BeanDescription_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test beans.
	//-----------------------------------------------------------------------------------------------------------------

	public static class SimpleBean {
		public String name;
		public int age;
	}

	public static class MultiPropBean {
		public String s;
		public Integer i;
		public Boolean b;
		public List<String> l;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a. of(Class) factory + basic content.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_of_returnsInstance() {
		var d = BeanDescription.of(SimpleBean.class);
		assertNotNull(d);
		assertEquals(SimpleBean.class.getName(), d.type);
		assertNotNull(d.properties);
	}

	@Test void a02_constructor_returnsInstance() {
		var d = new BeanDescription(SimpleBean.class);
		assertEquals(SimpleBean.class.getName(), d.type);
		assertEquals(2, d.properties.length);
	}

	@Test void a03_propertyNames_arePopulated() {
		var d = BeanDescription.of(SimpleBean.class);
		var names = new HashSet<String>();
		for (var p : d.properties)
			names.add(p.name);
		assertTrue(names.contains("name"));
		assertTrue(names.contains("age"));
	}

	@Test void a04_propertyTypes_arePopulated() {
		var d = BeanDescription.of(MultiPropBean.class);
		assertEquals(4, d.properties.length);
		for (var p : d.properties) {
			assertNotNull(p.name);
			assertNotNull(p.type);
			assertFalse(p.type.isEmpty());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. Non-bean failure path.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_nonBean_throwsRex() {
		// String has no bean properties - getBeanMeta should return null.
		var ex = assertThrows(RuntimeException.class, () -> BeanDescription.of(String.class));
		assertTrue(ex.getMessage().contains("not a valid bean"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. BeanPropertyDescription serialization.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_serializesAsJson() throws Exception {
		var d = BeanDescription.of(SimpleBean.class);
		var s = JsonSerializer.DEFAULT.toString(d);
		assertNotNull(s);
		assertTrue(s.contains(SimpleBean.class.getName()));
		assertTrue(s.contains("name"));
		assertTrue(s.contains("age"));
	}

	@Test void c02_beanPropertyDescription_directConstruction() {
		// Cover the public constructor by building one through BeanDescription, then confirm fields are accessible.
		var d = BeanDescription.of(SimpleBean.class);
		var p = d.properties[0];
		assertNotNull(p.name);
		assertNotNull(p.type);
	}
}
