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
package org.apache.juneau.utils;

import static org.apache.juneau.reflect.Mutaters.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MutatersTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		private String f;
		public A(String f) {
			this.f = f;
		}
		public A(int f) {
			this.f = String.valueOf(f);
		}
		public A(Integer f) {
			this.f = String.valueOf(f);
		}
	}
	@Test
	public void stringConstructor() {
		assertEquals("foo", get(String.class, A.class).mutate("foo").f);
	}
	@Test
	public void intConstructor() {
		assertEquals("1", get(int.class, A.class).mutate(1).f);
	}
	@Test
	public void integerConstructor() {
		assertEquals("2", get(Integer.class, A.class).mutate(2).f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// fromString methods.
	//-----------------------------------------------------------------------------------------------------------------

	public static class D1 {
		private String f;
		public static D1 create(String f) {
			D1 d = new D1(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_create() {
		assertEquals("foo", get(String.class, D1.class).mutate("foo").f);
	}

	public static class D2 {
		private String f;
		public static D2 fromString(String f) {
			D2 d = new D2(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_fromString() {
		assertEquals("foo", get(String.class, D2.class).mutate("foo").f);
	}

	public static class D3 {
		private String f;
		public static D3 fromValue(String f) {
			D3 d = new D3(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_fromValue() {
		assertEquals("foo", get(String.class, D3.class).mutate("foo").f);
	}

	public static class D4 {
		private String f;
		public static D4 valueOf(String f) {
			D4 d = new D4(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_valueOf() {
		assertEquals("foo", get(String.class, D4.class).mutate("foo").f);
	}

	public static class D5 {
		private String f;
		public static D5 parse(String f) {
			D5 d = new D5(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_parse() {
		assertEquals("foo", get(String.class, D5.class).mutate("foo").f);
	}

	public static class D6 {
		private String f;
		public static D6 parseString(String f) {
			D6 d = new D6(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_parseString() {
		assertEquals("foo", get(String.class, D6.class).mutate("foo").f);
	}

	public static class D7 {
		private String f;
		public static D7 forName(String f) {
			D7 d = new D7(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_forName() {
		assertEquals("foo", get(String.class, D7.class).mutate("foo").f);
	}

	public static class D8 {
		private String f;
		public static D8 forString(String f) {
			D8 d = new D8(); d.f = f; return d;
		}
	}
	@Test
	public void fromString_forString() {
		assertEquals("foo", get(String.class, D8.class).mutate("foo").f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// fromX methods.
	//-----------------------------------------------------------------------------------------------------------------

	public static class X {}

	public static class E1 {
		private String f;
		public static E1 create(X x) {
			E1 e = new E1(); e.f = "ok"; return e;
		}
	}
	@Test
	public void fromX_create() {
		assertEquals("ok", get(X.class, E1.class).mutate(new X()).f);
	}

	public static class E2 {
		private String f;
		public static E2 fromX(X x) {
			E2 e = new E2(); e.f = "ok"; return e;
		}
	}
	@Test
	public void fromX_fromX() {
		assertEquals("ok", get(X.class, E2.class).mutate(new X()).f);
	}
}
