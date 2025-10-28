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

import static org.apache.juneau.TestUtils.*;
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
class AutoObjectSwapTest extends TestBase {

	private static final Map<String,String> STRINGMAP = map("foo","bar");
	private static final JsonMap JSONMAP = JsonMap.of("foo","bar");

	private static ObjectSwap find(Class<?> c) {
		return AutoObjectSwap.find(BeanContext.DEFAULT, ClassInfo.of(c));
	}

	private static ObjectSwap find(BeanContext bc, Class<?> c) {
		return AutoObjectSwap.find(bc, ClassInfo.of(c));
	}

	private static BeanContext bc(Class<?> applyAnnotations) {
		return BeanContext.DEFAULT.copy().applyAnnotations(applyAnnotations).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Swap methods
	//------------------------------------------------------------------------------------------------------------------
	//SWAP_METHOD_NAMES = newUnmodifiableHashSet("swap", "toObject"),

	public static class A01 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}
	public static class A02 {
		public JsonMap toObject() {
			return JSONMAP;
		}
	}
	public static class A03 {
		public JsonMap toObject() throws SerializeException {
			throw new SerializeException("foo");
		}
	}
	public static class A04 {
		public JsonMap toObject() {
			throw new RuntimeException("foo");
		}
	}

	@Test void a01_swap_swap() throws Exception {
		assertJson("{foo:'bar'}", find(A01.class).swap(null, new A01()));
	}

	@Test void a02_swap_toObject() throws Exception {
		assertJson("{foo:'bar'}", find(A02.class).swap(null, new A02()));
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
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		public static B01 unswap(Map<String,String> o) {
			assertJson("{foo:'bar'}", o);
			return new B01();
		}
	}
	public static class B02 {
		public JsonMap swap() {
			return JSONMAP;
		}
		public static B02 create(JsonMap o) {
			assertBean(o, "foo", "bar");
			return new B02();
		}
	}
	public static class B03 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		public static B03 fromObject(Map<String,String> o) {
			assertBean(o, "foo", "bar");
			return new B03();
		}
	}
	public static class B04 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}

	@Test void b01_unswap_unswap() throws Exception {
		assertNotNull(find(B01.class).unswap(null, STRINGMAP, null));
	}

	@Test void b02_unswap_create() throws Exception {
		assertNotNull(find(B02.class).unswap(null, JSONMAP, null));
	}

	@Test void b03_unswap_fromObject() throws Exception {
		assertNotNull(find(B03.class).unswap(null, STRINGMAP, null));
	}

	@Test void b04_unswap_noMethod() {
		assertThrows(ParseException.class, ()->find(B04.class).unswap(null, STRINGMAP, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap constructor
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 {
		public C01() {}
		public C01(Map<String,String> o) {
			assertBean(o, "foo", "bar");
		}
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}

	@Test void c01_unswap_constructor() throws Exception {
		assertNotNull(find(C01.class).unswap(null, STRINGMAP, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore class
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore(on="D01c")
	private static class D01Config {}

	@BeanIgnore
	public static class D01 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}
	public static class D01c {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}
	public static class D02 {
		public class D02A {
			public Map<String,String> swap() {
				return STRINGMAP;
			}
		}
	}

	@Test void d01_ignoreClass_beanIgnore() {
		assertNull(find(D01.class));
	}

	@Test void d01c_ignoreClass_beanIgnore_usingConfig() {
		assertNull(find(bc(D01Config.class), D01c.class));
	}

	@Test void d02_ignoreClass_memberClass() {
		assertNull(find(D02.D02A.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore swap method
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore(on="E01c.swap")
	private static class E01Config {}

	public static class E01 {
		@BeanIgnore
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}
	public static class E01c {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}
	public static class E02 {
		@Deprecated
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}
	public static class E04 {
		public Map<String,String> swap(Map<String,String> foo) {
			return STRINGMAP;
		}
	}
	public static class E05 {
		public static Map<String,String> swap() {
			return STRINGMAP;
		}
	}

	@Test void e01_ignoreSwapMethod_beanIgnore() {
		assertNull(find(E01.class));
	}

	@Test void e01c_ignoreSwapMethod_beanIgnore_usingConfig() {
		assertNull(find(bc(E01Config.class), E01c.class));
	}

	@Test void e02_ignoreSwapMethod_deprecated() {
		assertNull(find(E02.class));
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

	@BeanIgnore(on="F01c.create(java.util.Map)")
	private static class F01Config {}

	public static class F01 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		@BeanIgnore
		public static F01 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F01c {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		public static F01 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F02 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		@Deprecated
		public static F02 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F03 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		public static Object create(Map<String,String> o) {
			return null;
		}
	}
	public static class F04 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		public static F04 create(List<String> o) {
			return null;
		}
	}
	public static class F05 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		public F05 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F06 {
		public Map<String,String> swap() {
			return STRINGMAP;
		}
		public static F06 createx(Map<String,String> o) {
			return null;
		}
	}

	@Test void f01_ignoreUnswapMethod_beanIgnore() {
		assertThrows(ParseException.class, ()->find(F01.class).unswap(null, null, null));
	}

	@Test void f01c_ignoreUnswapMethod_beanIgnore_usingConfig() {
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

	@BeanIgnore(on="G01c(Map)")
	private static class G01Config {}

	public static class G01 {
		@BeanIgnore
		public G01(Map<String,String> o) {}
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}

	public static class G01c {
		public G01c(Map<String,String> o) {}
		public Map<String,String> swap() {
			return STRINGMAP;
		}
	}

	public static class G02 {
		@Deprecated
		public G02(Map<String,String> o) {}
		public Map<String,String> swap() {
			return STRINGMAP;
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