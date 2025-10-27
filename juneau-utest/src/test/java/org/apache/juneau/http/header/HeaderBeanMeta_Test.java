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
package org.apache.juneau.http.header;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

class HeaderBeanMeta_Test extends TestBase {

	private static final String TNAME = HeaderBeanMeta_Test.class.getName();

	public static class A1 {
		public String name, value;

		public A1(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}

	public static class A2 {
		public String name, value;

		public A2(String name, Object value) {
			this.name = name;
			this.value = s(value);
		}
	}

	@Header(name="A3")
	public static class A3 {
		public String value;

		public A3(String value) {
			this.value = value;
		}
	}

	@Header("A4")
	public static class A4 {
		public String value;

		public A4(Object value) {
			this.value = s(value);
		}
	}

	@Header(name="A5")
	public static class A5 {}

	public static class A6 {
		public A6(String name, Object value) {
			throw new RuntimeException("oops");
		}
	}

	@Test void a01_basic() {
		var a1 = HeaderBeanMeta.of(A1.class);
		assertSame(a1, HeaderBeanMeta.of(A1.class));
		assertBean(a1.construct("X", "foo"), "name,value", "X,foo");
		assertThrowsWithMessage(Exception.class, "Constructor for type "+TNAME+"$A1 requires a name as the first argument.", ()->a1.construct("foo"));
		assertNull(a1.getSchema().getName());

		var a2 = HeaderBeanMeta.of(A2.class);
		assertBean(a2.construct("X", "foo"), "name,value", "X,foo");
		assertThrowsWithMessage(Exception.class, "Constructor for type "+TNAME+"$A2 requires a name as the first argument.", ()->a2.construct("foo"));
		assertNull(a2.getSchema().getName());

		var a3 = HeaderBeanMeta.of(A3.class);
		assertBean(a3.construct("X", "foo"), "value", "foo");
		assertBean(a3.construct("foo"), "value", "foo");
		assertEquals("A3", a3.getSchema().getName());

		var a4 = HeaderBeanMeta.of(A4.class);
		assertBean(a4.construct("X", "foo"), "value", "foo");
		assertBean(a4.construct("foo"), "value", "foo");
		assertEquals("A4", a4.getSchema().getName());

		var a5 = HeaderBeanMeta.of(A5.class);
		assertThrowsWithMessage(Exception.class, "Constructor for type "+TNAME+"$A5 could not be found.", ()->a5.construct("foo"));
		assertEquals("A5", a5.getSchema().getName());

		var a6 = HeaderBeanMeta.of(A6.class);
		assertThrowsWithMessage(Exception.class, "oops", ()->a6.construct("X", "foo"));
	}
}