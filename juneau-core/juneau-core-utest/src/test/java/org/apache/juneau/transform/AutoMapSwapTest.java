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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"unchecked","rawtypes"})
public class AutoMapSwapTest {

	private static final Map<String,String> STRINGMAP = AMap.of("foo","bar");
	private static final OMap OMAP = OMap.of("foo","bar");

	private static PojoSwap find(Class<?> c) {
		return AutoMapSwap.find(BeanContext.DEFAULT, ClassInfo.of(c));
	}

	private static PojoSwap find(BeanContext bc, Class<?> c) {
		return AutoMapSwap.find(bc, ClassInfo.of(c));
	}

	private static BeanContext bc(Class<?> applyAnnotations) {
		return BeanContext.DEFAULT.builder().applyAnnotations(applyAnnotations).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Swap methods
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}
	public static class A02 {
		public OMap toOMap() {
			return OMAP;
		}
	}
	public static class A03 {
		public OMap toOMap() throws SerializeException {
			throw new SerializeException("foo");
		}
	}
	public static class A04 {
		public OMap toOMap() {
			throw new RuntimeException("foo");
		}
	}

	@Test
	public void a01_swap_toMap() throws Exception {
		assertObjectEquals("{foo:'bar'}", find(A01.class).swap(null, new A01()));
	}

	@Test
	public void a02_swap_toOMap() throws Exception {
		assertObjectEquals("{foo:'bar'}", find(A02.class).swap(null, new A02()));
	}

	@Test(expected = SerializeException.class)
	public void a03_swap_serializeException() throws Exception {
		find(A03.class).swap(null, null);
	}

	@Test(expected = SerializeException.class)
	public void a04_swap_runtimeException() throws Exception {
		find(A04.class).swap(null, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap methods
	//------------------------------------------------------------------------------------------------------------------

	public static class B01 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		public static B01 fromMap(Map<String,String> o) {
			assertObjectEquals("{foo:'bar'}", o);
			return new B01();
		}
	}
	public static class B02 {
		public OMap toOMap() {
			return OMAP;
		}
		public static B02 fromOMap(OMap o) {
			assertObjectEquals("{foo:'bar'}", o);
			return new B02();
		}
	}
	public static class B03 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		public static B03 create(Map<String,String> o) {
			assertObjectEquals("{foo:'bar'}", o);
			return new B03();
		}
	}
	public static class B04 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}

	@Test
	public void b01_unswap_fromMap() throws Exception {
		assertNotNull(find(B01.class).unswap(null, STRINGMAP, null));
	}

	@Test
	public void b02_unswap_fromOMap() throws Exception {
		assertNotNull(find(B02.class).unswap(null, OMAP, null));
	}

	@Test
	public void b03_unswap_create() throws Exception {
		assertNotNull(find(B03.class).unswap(null, STRINGMAP, null));
	}

	@Test(expected = ParseException.class)
	public void b04_unswap_noMethod() throws Exception {
		find(B04.class).unswap(null, STRINGMAP, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap constructor
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 {
		public C01() {}
		public C01(Map<String,String> o) {
			assertObjectEquals("{foo:'bar'}", o);
		}
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}

	@Test
	public void c01_unswap_constructor() throws Exception {
		assertNotNull(find(C01.class).unswap(null, STRINGMAP, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore class
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore
	public static class D01 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}
	@BeanConfig(applyBeanIgnore=@BeanIgnore(on="D01c"))
	public static class D01c {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}
	public static class D02 {
		public class D02A {
			public Map<String,String> toMap() {
				return STRINGMAP;
			}
		}
	}

	@Test
	public void d01_ignoreClass_beanIgnore() throws Exception {
		assertNull(find(D01.class));
	}

	@Test
	public void d01c_ignoreClass_beanIgnore_usingConfig() throws Exception {
		assertNull(find(bc(D01c.class), D01c.class));
	}

	@Test
	public void d02_ignoreClass_memberClass() throws Exception {
		assertNull(find(D02.D02A.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore swap method
	//------------------------------------------------------------------------------------------------------------------

	public static class E01 {
		@BeanIgnore
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}
	@BeanConfig(applyBeanIgnore=@BeanIgnore(on="E01c.toMap"))
	public static class E01c {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}
	public static class E02 {
		@Deprecated
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}
	public static class E03 {
		public Object toMap() {
			return STRINGMAP;
		}
	}
	public static class E04 {
		public Map<String,String> toMap(Map<String,String> foo) {
			return STRINGMAP;
		}
	}
	public static class E05 {
		public static Map<String,String> toMap() {
			return STRINGMAP;
		}
	}

	@Test
	public void e01_ignoreSwapMethod_beanIgnore() throws Exception {
		assertNull(find(E01.class));
	}

	@Test
	public void e01c_ignoreSwapMethod_beanIgnore_usingConfig() throws Exception {
		assertNull(find(bc(E01c.class), E01c.class));
	}

	@Test
	public void e02_ignoreSwapMethod_deprecated() throws Exception {
		assertNull(find(E02.class));
	}

	@Test
	public void e03_ignoreSwapMethod_wrongReturnType() throws Exception {
		assertNull(find(E03.class));
	}

	@Test
	public void e04_ignoreSwapMethod_wrongParameters() throws Exception {
		assertNull(find(E04.class));
	}

	@Test
	public void e05_ignoreSwapMethod_notStatic() throws Exception {
		assertNull(find(E05.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore unswap method
	//------------------------------------------------------------------------------------------------------------------

	public static class F01 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		@BeanIgnore
		public static F01 create(Map<String,String> o) {
			return null;
		}
	}
	@BeanConfig(applyBeanIgnore=@BeanIgnore(on="F01c.create(Map)"))
	public static class F01c {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		public static F01c create(Map<String,String> o) {
			return null;
		}
	}
	public static class F02 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		@Deprecated
		public static F02 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F03 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		public static Object create(Map<String,String> o) {
			return null;
		}
	}
	public static class F04 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		public static F04 create(List<String> o) {
			return null;
		}
	}
	public static class F05 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		public F05 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F06 {
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
		public static F06 createx(Map<String,String> o) {
			return null;
		}
	}

	@Test(expected = ParseException.class)
	public void f01_ignoreUnswapMethod_beanIgnore() throws Exception {
		find(F01.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void f01c_ignoreUnswapMethod_beanIgnore_usingConfig() throws Exception {
		find(bc(F01c.class), F01c.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void f02_ignoreUnswapMethod_deprecated() throws Exception {
		find(F02.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void f03_ignoreUnswapMethod_wrongReturnType() throws Exception {
		find(F03.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void f04_ignoreUnswapMethod_wrongParameters() throws Exception {
		find(F04.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void f05_ignoreUnswapMethod_notStatic() throws Exception {
		find(F05.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void f06_ignoreUnswapMethod_wrongName() throws Exception {
		find(F06.class).unswap(null, null, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore constructor
	//------------------------------------------------------------------------------------------------------------------

	public static class G01 {
		@BeanIgnore
		public G01(Map<String,String> o) {}
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}

	@BeanConfig(applyBeanIgnore=@BeanIgnore(on="G01c(Map)"))
	public static class G01c {
		public G01c(Map<String,String> o) {}
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}

	public static class G02 {
		@Deprecated
		public G02(Map<String,String> o) {}
		public Map<String,String> toMap() {
			return STRINGMAP;
		}
	}

	@Test(expected = ParseException.class)
	public void g01_ignoreUnswapConstructor_beanIgnore() throws Exception {
		find(G01.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void g01c_ignoreUnswapConstructor_beanIgnore_usingConfig() throws Exception {
		find(bc(G01c.class), G01c.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void g02_ignoreUnswapConstructor_deprecated() throws Exception {
		find(G02.class).unswap(null, null, null);
	}
}
