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
package org.apache.juneau.transform;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"unchecked","rawtypes"})
public class AutoStringSwapTest {

	private static PojoSwap find(Class<?> c) {
		return AutoStringSwap.find(ClassInfo.of(c));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap methods
	//------------------------------------------------------------------------------------------------------------------

	public static class B01 {
		public static B01 fromString(String o) {
			assertEquals("foo", o);
			return new B01();
		}
	}
	public static class B02 {
		public static B02 fromValue(String o) {
			assertEquals("foo", o);
			return new B02();
		}
	}
	public static class B03 {
		public static B03 valueOf(String o) {
			assertEquals("foo", o);
			return new B03();
		}
	}
	public static class B04 {
		public static B04 parse(String o) {
			assertEquals("foo", o);
			return new B04();
		}
	}
	public static class B05 {
		public static B05 parseString(String o) {
			assertEquals("foo", o);
			return new B05();
		}
	}
	public static class B06 {
		public static B06 forName(String o) {
			assertEquals("foo", o);
			return new B06();
		}
	}
	public static class B07 {
		public static B07 forString(String o) {
			assertEquals("foo", o);
			return new B07();
		}
	}

	@Test
	public void b01_unswap_fromString() throws Exception {
		assertNotNull(find(B01.class).unswap(null, "foo", null));
	}

	@Test
	public void b02_unswap_fromValue() throws Exception {
		assertNotNull(find(B02.class).unswap(null, "foo", null));
	}

	@Test
	public void b03_unswap_valueOf() throws Exception {
		assertNotNull(find(B03.class).unswap(null, "foo", null));
	}

	@Test
	public void b04_unswap_parse() throws Exception {
		assertNotNull(find(B04.class).unswap(null, "foo", null));
	}

	@Test
	public void b05_unswap_parseString() throws Exception {
		assertNotNull(find(B05.class).unswap(null, "foo", null));
	}

	@Test
	public void b06_unswap_forName() throws Exception {
		assertNotNull(find(B06.class).unswap(null, "foo", null));
	}

	@Test
	public void b07_unswap_forString() throws Exception {
		assertNotNull(find(B07.class).unswap(null, "foo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap constructor
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 {
		public C01() {}
		public C01(String o) {
			assertEquals("foo", o);
		}
	}
	public static class C02 {
		public C02() {}
		public C02(String o) {
			throw new RuntimeException("foo");
		}
	}
	public static class C03 {
		public C03() {}
		public C03(String o) throws ParseException {
			throw new ParseException("foo");
		}
	}

	@Test
	public void c01_unswapConstructor() throws Exception {
		assertNotNull(find(C01.class).unswap(null, "foo", null));
	}

	@Test(expected = ParseException.class)
	public void c02_unswapConstructor_runtimeException() throws Exception {
		assertNotNull(find(C02.class).unswap(null, "foo", null));
	}

	@Test(expected = ParseException.class)
	public void c03_unswapConstructor_parseException() throws Exception {
		assertNotNull(find(C03.class).unswap(null, "foo", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore class
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore
	public static class D01 {
		public static D01 fromString(String s) {
			return new D01();
		}
	}
	public static class D02 {
		public class D02A {
			public D02A fromString(String s) {
				return new D02A();
			}
		}
	}

	@Test
	public void d01_ignoreClass_beanIgnore() throws Exception {
		assertNull(find(D01.class));
	}

	@Test
	public void d02_ignoreClass_memberClass() throws Exception {
		assertNull(find(D02.D02A.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore unswap method
	//------------------------------------------------------------------------------------------------------------------

	public static class F01 {
		@BeanIgnore
		public static F01 fromString(String o) {
			return null;
		}
	}
	public static class F02 {
		@Deprecated
		public static F02 fromString(String o) {
			return null;
		}
	}
	public static class F03 {
		public static Object fromString(String o) {
			return null;
		}
	}
	public static class F04 {
		public static F04 fromString(Map<String,String> o) {
			return null;
		}
	}
	public static class F05 {
		public F05 fromString(String o) {
			return null;
		}
	}
	public static class F06 {
		public static F06 createx(String o) {
			return null;
		}
	}

	@Test
	public void f01_ignoreUnswapMethod_beanIgnore() throws Exception {
		assertNull(find(F01.class));
	}

	@Test
	public void f02_ignoreUnswapMethod_deprecated() throws Exception {
		assertNull(find(F02.class));
	}

	@Test
	public void f03_ignoreUnswapMethod_wrongReturnType() throws Exception {
		assertNull(find(F03.class));
	}

	@Test
	public void f04_ignoreUnswapMethod_wrongParameters() throws Exception {
		assertNull(find(F04.class));
	}

	@Test
	public void f05_ignoreUnswapMethod_notStatic() throws Exception {
		assertNull(find(F05.class));
	}

	@Test
	public void f06_ignoreUnswapMethod_wrongName() throws Exception {
		assertNull(find(F06.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore constructor
	//------------------------------------------------------------------------------------------------------------------

	public static class G01 {
		@BeanIgnore
		public G01(String o) {}
	}

	public static class G02 {
		@Deprecated
		public G02(String o) {}
	}

	@Test
	public void g01_ignoreUnswapConstructor_beanIgnore() throws Exception {
		assertNull(find(G01.class));
	}

	@Test
	public void g02_ignoreUnswapConstructor_deprecated() throws Exception {
		assertNull(find(G02.class));
	}
}
