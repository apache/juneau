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
package org.apache.juneau.http.header;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import org.apache.juneau.http.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HeaderBeanMeta_Test {

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
			this.value = stringify(value);
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
			this.value = stringify(value);
		}
	}

	@Header(name="A5")
	public static class A5 {}

	public static class A6 {
		public A6(String name, Object value) {
			throw new RuntimeException("oops");
		}
	}

	@Test
	public void a01_basic() {
		HeaderBeanMeta<A1> a1 = HeaderBeanMeta.of(A1.class);
		assertTrue(a1 == HeaderBeanMeta.of(A1.class));
		assertObject(a1.construct("X", "foo")).isJson("{name:'X',value:'foo'}");
		assertThrown(()->a1.construct("foo")).asMessage().isContains("Constructor for type "+TNAME+"$A1 requires a name as the first argument.");
		assertString(a1.getSchema().getName()).isNull();

		HeaderBeanMeta<A2> a2 = HeaderBeanMeta.of(A2.class);
		assertObject(a2.construct("X", "foo")).isJson("{name:'X',value:'foo'}");
		assertThrown(()->a2.construct("foo")).asMessage().isContains("Constructor for type "+TNAME+"$A2 requires a name as the first argument.");
		assertString(a2.getSchema().getName()).isNull();

		HeaderBeanMeta<A3> a3 = HeaderBeanMeta.of(A3.class);
		assertObject(a3.construct("X", "foo")).isJson("{value:'foo'}");
		assertObject(a3.construct("foo")).isJson("{value:'foo'}");
		assertString(a3.getSchema().getName()).is("A3");

		HeaderBeanMeta<A4> a4 = HeaderBeanMeta.of(A4.class);
		assertObject(a4.construct("X", "foo")).isJson("{value:'foo'}");
		assertObject(a4.construct("foo")).isJson("{value:'foo'}");
		assertString(a4.getSchema().getName()).is("A4");

		HeaderBeanMeta<A5> a5 = HeaderBeanMeta.of(A5.class);
		assertThrown(()->a5.construct("foo")).asMessage().isContains("Constructor for type "+TNAME+"$A5 could not be found.");
		assertString(a5.getSchema().getName()).is("A5");

		HeaderBeanMeta<A6> a6 = HeaderBeanMeta.of(A6.class);
		assertThrown(()->a6.construct("X", "foo")).asMessages().isContains("oops");
	}
}
