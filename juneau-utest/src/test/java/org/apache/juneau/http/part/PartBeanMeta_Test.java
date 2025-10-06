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
package org.apache.juneau.http.part;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Tests: {@link PartBeanMeta}
 */
class PartBeanMeta_Test extends TestBase {

	private static final String TNAME = PartBeanMeta_Test.class.getName();

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
			this.value = Utils.s(value);
		}
	}

	@Query(name="A3")
	public static class A3 {
		public String value;

		public A3(String value) {
			this.value = value;
		}
	}

	@FormData("A4")
	public static class A4 {
		public String value;

		public A4(Object value) {
			this.value = Utils.s(value);
		}
	}

	@Path(name="A5")
	public static class A5 {}

	public static class A6 {
		public A6(String name, Object value) {
			throw new RuntimeException("oops");
		}
	}

	@Header(name="A7")
	public static class A7 {}

	@Test void a01_basic() {
		var a1 = PartBeanMeta.of(A1.class);
		assertSame(a1, PartBeanMeta.of(A1.class));
		assertBean(a1.construct("X", "foo"), "name,value", "X,foo");
		assertThrowsWithMessage(Exception.class, "Constructor for type "+TNAME+"$A1 requires a name as the first argument.", ()->a1.construct("foo"));
		assertNull(a1.getSchema().getName());

		var a2 = PartBeanMeta.of(A2.class);
		assertBean(a2.construct("X", "foo"), "name,value", "X,foo");
		assertThrowsWithMessage(Exception.class, "Constructor for type "+TNAME+"$A2 requires a name as the first argument.", ()->a2.construct("foo"));
		assertNull(a2.getSchema().getName());

		var a3 = PartBeanMeta.of(A3.class);
		assertBean(a3.construct("X", "foo"), "value", "foo");
		assertBean(a3.construct("foo"), "value", "foo");
		assertEquals("A3", a3.getSchema().getName());

		var a4 = PartBeanMeta.of(A4.class);
		assertBean(a4.construct("X", "foo"), "value", "foo");
		assertBean(a4.construct("foo"), "value", "foo");
		assertEquals("A4", a4.getSchema().getName());

		var a5 = PartBeanMeta.of(A5.class);
		assertThrowsWithMessage(Exception.class, "Constructor for type "+TNAME+"$A5 could not be found.", ()->a5.construct("foo"));
		assertEquals("A5", a5.getSchema().getName());

		var a6 = PartBeanMeta.of(A6.class);
		assertThrowsWithMessage(Exception.class, "oops", ()->a6.construct("X", "foo"));

		var a7 = PartBeanMeta.of(A7.class);
		assertThrowsWithMessage(Exception.class, "Constructor for type "+TNAME+"$A7 could not be found.", ()->a7.construct("foo"));
		assertEquals("A7", a7.getSchema().getName());
	}
}