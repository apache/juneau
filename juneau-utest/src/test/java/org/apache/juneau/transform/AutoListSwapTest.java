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
package org.apache.juneau.transform;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"rawtypes"})
class AutoListSwapTest extends TestBase {

	private static final List<String> STRINGLIST = l("foo");
	private static final JsonList JSONLIST = JsonList.ofJsonOrCdl("foo");

	private static ObjectSwap find(Class<?> c) {
		return AutoListSwap.find(BeanContext.DEFAULT, ClassInfo.of(c));
	}

	private static ObjectSwap find(BeanContext bc, Class<?> c) {
		return AutoListSwap.find(bc, ClassInfo.of(c));
	}

	private static BeanContext bc(Class<?> applyAnnotations) {
		return BeanContext.DEFAULT.copy().applyAnnotations(applyAnnotations).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Swap methods
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 {
		public List<String> toList() {
			return STRINGLIST;
		}
	}
	public static class A02 {
		public JsonList toJsonList() {
			return JSONLIST;
		}
	}
	public static class A03 {
		public JsonList toJsonList() throws SerializeException {
			throw new SerializeException("foo");
		}
	}
	public static class A04 {
		public JsonList toJsonList() {
			throw new RuntimeException("foo");
		}
	}

	@Test void a01_swap_toList() throws Exception {
		assertString("[foo]", find(A01.class).swap(null, new A01()));
	}

	@Test void a02_swap_toJsonList() throws Exception {
		assertString("[foo]", find(A02.class).swap(null, new A02()));
	}

	@Test void a03_swap_serializeException() {
		assertThrows(SerializeException.class, ()->find(A03.class).swap(null, null));
	}

	@Test void a04_swap_runtimeException() {
		assertThrows(SerializeException.class, ()->find(A04.class).swap(null, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap methods
	//------------------------------------------------------------------------------------------------------------------

	public static class B01 {
		public List<String> toList() {
			return STRINGLIST;
		}
		public static B01 fromList(List<String> o) {
			assertList(o, "foo");
			return new B01();
		}
	}
	public static class B02 {
		public JsonList toJsonList() {
			return JSONLIST;
		}
		public static B02 fromJsonList(JsonList o) {
			assertList(o, "foo");
			return new B02();
		}
	}
	public static class B03 {
		public List<String> toList() {
			return STRINGLIST;
		}
		public static B03 create(List<String> o) {
			assertList(o, "foo");
			return new B03();
		}
	}
	public static class B04 {
		public List<String> toList() {
			return STRINGLIST;
		}
	}

	@Test void b01_unswap_fromList() throws Exception {
		assertNotNull(find(B01.class).unswap(null, STRINGLIST, null));
	}

	@Test void b02_unswap_fromJsonList() throws Exception {
		assertNotNull(find(B02.class).unswap(null, JSONLIST, null));
	}

	@Test void b03_unswap_create() throws Exception {
		assertNotNull(find(B03.class).unswap(null, STRINGLIST, null));
	}

	@Test void b04_unswap_noMethod() {
		assertThrows(ParseException.class, ()->find(B04.class).unswap(null, STRINGLIST, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap constructor
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 {
		public C01() {}
		public C01(List<String> o) {
			assertList(o, "foo");
		}
		public List<String> toList() {
			return STRINGLIST;
		}
	}

	@Test void c01_unswap_constructor() throws Exception {
		assertNotNull(find(C01.class).unswap(null, STRINGLIST, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore class
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore
	public static class D01 {
		public List<String> toList() {
			return STRINGLIST;
		}
	}
	public static class D02 {
		public class D02A {
			public List<String> toList() {
				return STRINGLIST;
			}
		}
	}

	@Test void d01_ignoreClass_beanIgnore() {
		assertNull(find(D01.class));
	}

	@Test void d02_ignoreClass_memberClass() {
		assertNull(find(D02.D02A.class));
	}

	@BeanIgnore(on="D01c")
	private static class D01cConfig {}

	public static class D01c {
		public List<String> toList() {
			return STRINGLIST;
		}
	}
	public static class D02c {
		public class D02Ac {
			public List<String> toList() {
				return STRINGLIST;
			}
		}
	}

	@Test void d03_ignoreClass_beanIgnore_usingConfig() {
		assertNull(find(bc(D01cConfig.class), D01c.class));
	}

	@Test void d04_ignoreClass_memberClass_usingConfig() {
		assertNull(find(bc(D01cConfig.class), D02c.D02Ac.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore swap method
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore(on="E01c.toList")
	private static class E01Config {}

	public static class E01 {
		@BeanIgnore
		public List<String> toList() {
			return STRINGLIST;
		}
	}
	public static class E01c {
		public List<String> toList() {
			return STRINGLIST;
		}
	}
	public static class E02 {
		@Deprecated
		public List<String> toList() {
			return STRINGLIST;
		}
	}
	public static class E03 {
		public Object toList() {
			return STRINGLIST;
		}
	}
	public static class E04 {
		public List<String> toList(List<String> foo) {
			return STRINGLIST;
		}
	}
	public static class E05 {
		public static List<String> toList() {
			return STRINGLIST;
		}
	}

	@Test void e01_ignoreSwapMethod_beanIgnore() {
		assertNull(find(E01.class));
	}

	@Test void e01c_ignoreSwapMethod_beanIgnore_usingConfig() {
		assertNull(find(BeanContext.DEFAULT.copy().applyAnnotations(E01Config.class).build(), E01c.class));
	}

	@Test void e02_ignoreSwapMethod_deprecated() {
		assertNull(find(E02.class));
	}

	@Test void e03_ignoreSwapMethod_wrongReturnType() {
		assertNull(find(E03.class));
	}

	@Test void e04_ignoreSwapMethod_wrongParameters() {
		assertNull(find(E04.class));
	}

	@Test void e05_ignoreSwapMethod_notStatic() {
		assertNull(find(E05.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore unswap method
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore(on="F01c.create")
	private static class F01Config {}

	public static class F01 {
		public List<String> toList() {
			return STRINGLIST;
		}
		@BeanIgnore
		public static F01 create(List<String> o) {
			return null;
		}
	}
	public static class F01c {
		public List<String> toList() {
			return STRINGLIST;
		}
		public static F01c create(List<String> o) {
			return null;
		}
	}
	public static class F02 {
		public List<String> toList() {
			return STRINGLIST;
		}
		@Deprecated
		public static F02 create(List<String> o) {
			return null;
		}
	}
	public static class F03 {
		public List<String> toList() {
			return STRINGLIST;
		}
		public static Object create(List<String> o) {
			return null;
		}
	}
	public static class F04 {
		public List<String> toList() {
			return STRINGLIST;
		}
		public static F04 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F05 {
		public List<String> toList() {
			return STRINGLIST;
		}
		public F05 create(List<String> o) {
			return null;
		}
	}
	public static class F06 {
		public List<String> toList() {
			return STRINGLIST;
		}
		public static F06 createx(List<String> o) {
			return null;
		}
	}

	@Test void f01_ignoreUnswapMethod_beanIgnore() {
		assertThrows(ParseException.class, ()->find(F01.class).unswap(null, null, null));
	}

	@Test void f01c_ignoreUnswapMethod_beanIgnore_applyConfig() {
		assertThrows(ParseException.class, ()->find(bc(F01Config.class), F01c.class).unswap(null, null, null));
	}

	@Test void f02_ignoreUnswapMethod_deprecated() {
		assertThrows(ParseException.class, ()->find(F02.class).unswap(null, null, null));
	}

	@Test void f03_ignoreUnswapMethod_wrongReturnType() {
		assertThrows(ParseException.class, ()->find(F03.class).unswap(null, null, null));
	}

	@Test void f04_ignoreUnswapMethod_wrongParameters() {
		assertThrows(ParseException.class, ()->find(F04.class).unswap(null, null, null));
	}

	@Test void f05_ignoreUnswapMethod_notStatic() {
		assertThrows(ParseException.class, ()->find(F05.class).unswap(null, null, null));
	}

	@Test void f06_ignoreUnswapMethod_wrongName() {
		assertThrows(ParseException.class, ()->find(F06.class).unswap(null, null, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore constructor
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore(on="G01c(List)")
	private static class G01Config {}

	public static class G01 {
		@BeanIgnore
		public G01(List<String> o) {}
		public List<String> toList() {
			return STRINGLIST;
		}
	}

	public static class G01c {
		public G01c(List<String> o) {}
		public List<String> toList() {
			return STRINGLIST;
		}
	}

	public static class G02 {
		@Deprecated
		public G02(List<String> o) {}
		public List<String> toList() {
			return STRINGLIST;
		}
	}

	@Test void g01_ignoreUnswapConstructor_beanIgnore() {
		assertThrows(ParseException.class, ()->find(G01.class).unswap(null, null, null));
	}

	@Test void g01c_ignoreUnswapConstructor_beanIgnore_usingConfig() {
		assertThrows(ParseException.class, ()->find(bc(G01Config.class), G01c.class).unswap(null, null, null));
	}

	@Test void g02_ignoreUnswapConstructor_deprecated() {
		assertThrows(ParseException.class, ()->find(G02.class).unswap(null, null, null));
	}
}