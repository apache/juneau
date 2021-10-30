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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings({"unchecked","rawtypes"})
public class AutoNumberSwapTest {

	private static ObjectSwap find(Class<?> c) {
		return AutoNumberSwap.find(BeanContext.DEFAULT, ClassInfo.of(c));
	}

	private static ObjectSwap find(BeanContext bc, Class<?> c) {
		return AutoNumberSwap.find(bc, ClassInfo.of(c));
	}

	private static BeanContext bc(Class<?> applyAnnotations) {
		return BeanContext.DEFAULT.copy().applyAnnotations(applyAnnotations).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Swap methods
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 {
		public Number toNumber() {
			return 1;
		}
	}
	public static class A02 {
		public Integer toInteger() {
			return 1;
		}
	}
	public static class A03 {
		public int toInt() {
			return 1;
		}
	}
	public static class A04 {
		public Long toLong() {
			return 1l;
		}
	}
	public static class A05 {
		public long toLong() {
			return 1l;
		}
	}
	public static class A06 {
		public Float toFloat() {
			return 1f;
		}
	}
	public static class A07 {
		public float toFloat() {
			return 1f;
		}
	}
	public static class A08 {
		public Double toDouble() {
			return 1d;
		}
	}
	public static class A09 {
		public double toDouble() {
			return 1d;
		}
	}
	public static class A10 {
		public Short toShort() {
			return 1;
		}
	}
	public static class A11 {
		public short toShort() {
			return 1;
		}
	}
	public static class A12 {
		public Byte toByte() {
			return 1;
		}
	}
	public static class A13 {
		public byte toByte() {
			return 1;
		}
	}
	public static class A14 {
		public Integer toInteger() throws SerializeException {
			throw new SerializeException("foo");
		}
	}
	public static class A15 {
		public Integer toInteger() {
			throw new RuntimeException("foo");
		}
	}

	@Test
	public void a01_swap_toNumber() throws Exception {
		assertObject(find(A01.class).swap(null, new A01())).asJson().is("1");
	}

	@Test
	public void a02_swap_toInteger() throws Exception {
		assertObject(find(A02.class).swap(null, new A02())).asJson().is("1");
	}

	@Test
	public void a03_swap_toIntPrimitive() throws Exception {
		assertObject(find(A03.class).swap(null, new A03())).asJson().is("1");
	}

	@Test
	public void a04_swap_toLong() throws Exception {
		assertObject(find(A04.class).swap(null, new A04())).asJson().is("1");
	}

	@Test
	public void a05_swap_toLongPrimitive() throws Exception {
		assertObject(find(A05.class).swap(null, new A05())).asJson().is("1");
	}

	@Test
	public void a06_swap_toFloat() throws Exception {
		assertObject(find(A06.class).swap(null, new A06())).asJson().is("1.0");
	}

	@Test
	public void a07_swap_toFloatPrimitive() throws Exception {
		assertObject(find(A07.class).swap(null, new A07())).asJson().is("1.0");
	}

	@Test
	public void a08_swap_toDouble() throws Exception {
		assertObject(find(A08.class).swap(null, new A08())).asJson().is("1.0");
	}

	@Test
	public void a09_swap_toDoublePrimitive() throws Exception {
		assertObject(find(A09.class).swap(null, new A09())).asJson().is("1.0");
	}

	@Test
	public void a10_swap_toShort() throws Exception {
		assertObject(find(A10.class).swap(null, new A10())).asJson().is("1");
	}

	@Test
	public void a11_swap_toShortPrimitive() throws Exception {
		assertObject(find(A11.class).swap(null, new A11())).asJson().is("1");
	}

	@Test
	public void a12_swap_toByte() throws Exception {
		assertObject(find(A12.class).swap(null, new A12())).asJson().is("1");
	}

	@Test
	public void a13_swap_toBytePrimitive() throws Exception {
		assertObject(find(A13.class).swap(null, new A13())).asJson().is("1");
	}

	@Test(expected = SerializeException.class)
	public void a14_swap_serializeException() throws Exception {
		find(A14.class).swap(null, null);
	}

	@Test(expected = SerializeException.class)
	public void a15_swap_runtimeException() throws Exception {
		find(A15.class).swap(null, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap methods
	//------------------------------------------------------------------------------------------------------------------
//	UNSWAP_METHOD_NAMES = newUnmodifiableHashSet("fromInteger", "fromInt", "fromLong", "fromFloat", "fromDouble", "fromShort", "fromByte", "create", "valueOf");

	public static class B01 {
		public Integer toInteger() {
			return 1;
		}
		public static B01 fromInteger(Integer o) {
			assertObject(o).asJson().is("1");
			return new B01();
		}
	}
	public static class B02 {
		public int toInt() {
			return 1;
		}
		public static B02 fromInt(int o) {
			assertObject((Object) o).asJson().is("1");
			return new B02();
		}
	}
	public static class B03 {
		public Long toLong() {
			return 1l;
		}
		public static B03 fromLong(Long o) {
			assertObject(o).asJson().is("1");
			return new B03();
		}
	}
	public static class B04 {
		public long toLong() {
			return 1;
		}
		public static B04 fromLong(long o) {
			assertObject((Object) o).asJson().is("1");
			return new B04();
		}
	}
	public static class B05 {
		public Float toFloat() {
			return 1f;
		}
		public static B05 fromFloat(Float o) {
			assertObject(o).asJson().is("1.0");
			return new B05();
		}
	}
	public static class B06 {
		public float toFloat() {
			return 1;
		}
		public static B06 fromFloat(float o) {
			assertObject((Object) o).asJson().is("1.0");
			return new B06();
		}
	}
	public static class B07 {
		public Double toDouble() {
			return 1d;
		}
		public static B07 fromDouble(Double o) {
			assertObject(o).asJson().is("1.0");
			return new B07();
		}
	}
	public static class B08 {
		public double toDouble() {
			return 1d;
		}
		public static B08 fromDouble(double o) {
			assertObject((Object) o).asJson().is("1.0");
			return new B08();
		}
	}
	public static class B09 {
		public Short toShort() {
			return 1;
		}
		public static B09 fromShort(Short o) {
			assertObject(o).asJson().is("1");
			return new B09();
		}
	}
	public static class B10 {
		public short toShort() {
			return 1;
		}
		public static B10 fromShort(short o) {
			assertObject((Object) o).asJson().is("1");
			return new B10();
		}
	}
	public static class B11 {
		public Byte toByte() {
			return 1;
		}
		public static B11 fromByte(Byte o) {
			assertObject(o).asJson().is("1");
			return new B11();
		}
	}
	public static class B12 {
		public byte toByte() {
			return 1;
		}
		public static B12 fromByte(byte o) {
			assertObject((Object) o).asJson().is("1");
			return new B12();
		}
	}
	public static class B13 {
		public int toInt() {
			return 1;
		}
		public static B13 create(int o) {
			assertObject((Object) o).asJson().is("1");
			return new B13();
		}
	}
	public static class B14 {
		public int toInt() {
			return 1;
		}
		public static B14 valueOf(int o) {
			assertObject((Object) o).asJson().is("1");
			return new B14();
		}
	}
	public static class B15 {
		public int toInt() {
			return 1;
		}
	}

	@Test
	public void b01_unswap_fromInteger() throws Exception {
		assertNotNull(find(B01.class).unswap(null, 1, null));
	}

	@Test
	public void b02_unswap_fromInt() throws Exception {
		assertNotNull(find(B02.class).unswap(null, 1, null));
	}

	@Test
	public void b03_unswap_fromLong() throws Exception {
		assertNotNull(find(B03.class).unswap(null, 1, null));
	}

	@Test
	public void b04_unswap_fromLongPrimitive() throws Exception {
		assertNotNull(find(B04.class).unswap(null, 1, null));
	}

	@Test
	public void b05_unswap_fromFloat() throws Exception {
		assertNotNull(find(B05.class).unswap(null, 1, null));
	}

	@Test
	public void b06_unswap_fromFloatPrimitive() throws Exception {
		assertNotNull(find(B06.class).unswap(null, 1, null));
	}

	@Test
	public void b07_unswap_fromDouble() throws Exception {
		assertNotNull(find(B07.class).unswap(null, 1, null));
	}

	@Test
	public void b08_unswap_fromDoublePrimitive() throws Exception {
		assertNotNull(find(B08.class).unswap(null, 1, null));
	}

	@Test
	public void b09_unswap_fromShort() throws Exception {
		assertNotNull(find(B09.class).unswap(null, 1, null));
	}

	@Test
	public void b10_unswap_fromShortPrimitive() throws Exception {
		assertNotNull(find(B10.class).unswap(null, 1, null));
	}

	@Test
	public void b11_unswap_fromByte() throws Exception {
		assertNotNull(find(B11.class).unswap(null, 1, null));
	}

	@Test
	public void b12_unswap_fromBytePrimitive() throws Exception {
		assertNotNull(find(B12.class).unswap(null, 1, null));
	}

	@Test
	public void b13_unswap_create() throws Exception {
		assertNotNull(find(B13.class).unswap(null, 1, null));
	}

	@Test
	public void b14_unswap_valueOf() throws Exception {
		assertNotNull(find(B14.class).unswap(null, 1, null));
	}

	@Test(expected = ParseException.class)
	public void b15_unswap_noMethod() throws Exception {
		find(B15.class).unswap(null, 1, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unswap constructor
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 {
		public C01() {}
		public C01(Integer o) {
			assertObject(o).asJson().is("1");
		}
		public Integer toInteger() {
			return 1;
		}
	}
	public static class C02 {
		public C02() {}
		public C02(int o) {
			assertObject((Object) o).asJson().is("1");
		}
		public int toInt() {
			return 1;
		}
	}
	public static class C03 {
		public C03() {}
		public C03(Long o) {
			assertObject(o).asJson().is("1");
		}
		public Long toLong() {
			return 1l;
		}
	}
	public static class C04 {
		public C04() {}
		public C04(long o) {
			assertObject((Object) o).asJson().is("1");
		}
		public long toLong() {
			return 1l;
		}
	}
	public static class C05 {
		public C05() {}
		public C05(Float o) {
			assertObject(o).asJson().is("1.0");
		}
		public Float toFloat() {
			return 1f;
		}
	}
	public static class C06 {
		public C06() {}
		public C06(float o) {
			assertObject((Object) o).asJson().is("1.0");
		}
		public float toFloat() {
			return 1f;
		}
	}
	public static class C07 {
		public C07() {}
		public C07(Double o) {
			assertObject(o).asJson().is("1.0");
		}
		public Double toDouble() {
			return 1d;
		}
	}
	public static class C08 {
		public C08() {}
		public C08(double o) {
			assertObject((Object) o).asJson().is("1.0");
		}
		public double toDouble() {
			return 1d;
		}
	}
	public static class C09 {
		public C09() {}
		public C09(Short o) {
			assertObject(o).asJson().is("1");
		}
		public Short toShort() {
			return 1;
		}
	}
	public static class C10 {
		public C10() {}
		public C10(short o) {
			assertObject((Object) o).asJson().is("1");
		}
		public short toShort() {
			return 1;
		}
	}
	public static class C11 {
		public C11() {}
		public C11(Byte o) {
			assertObject(o).asJson().is("1");
		}
		public Byte toByte() {
			return 1;
		}
	}
	public static class C12 {
		public C12() {}
		public C12(byte o) {
			assertObject((Object) o).asJson().is("1");
		}
		public byte toByte() {
			return 1;
		}
	}
	public static class C13 {
		public C13() {}
		public Integer toInteger() {
			return 1;
		}
	}

	@Test
	public void c01_unswapConstructor_Integer() throws Exception {
		assertNotNull(find(C01.class).unswap(null, 1, null));
	}

	@Test
	public void c02_unswapConstructor_int() throws Exception {
		assertNotNull(find(C02.class).unswap(null, 1, null));
	}

	@Test
	public void c03_unswapConstructor_Long() throws Exception {
		assertNotNull(find(C03.class).unswap(null, 1, null));
	}

	@Test
	public void c04_unswapConstructor_long() throws Exception {
		assertNotNull(find(C04.class).unswap(null, 1, null));
	}

	@Test
	public void c05_unswapConstructor_Float() throws Exception {
		assertNotNull(find(C05.class).unswap(null, 1, null));
	}

	@Test
	public void c06_unswapConstructor_float() throws Exception {
		assertNotNull(find(C06.class).unswap(null, 1, null));
	}

	@Test
	public void c07_unswapConstructor_Double() throws Exception {
		assertNotNull(find(C07.class).unswap(null, 1, null));
	}

	@Test
	public void c08_unswapConstructor_double() throws Exception {
		assertNotNull(find(C08.class).unswap(null, 1, null));
	}

	@Test
	public void c09_unswapConstructor_Short() throws Exception {
		assertNotNull(find(C09.class).unswap(null, 1, null));
	}

	@Test
	public void c10_unswapConstructor_short() throws Exception {
		assertNotNull(find(C10.class).unswap(null, 1, null));
	}

	@Test
	public void c11_unswapConstructor_Byte() throws Exception {
		assertNotNull(find(C11.class).unswap(null, 1, null));
	}

	@Test
	public void c12_unswapConstructor_byte() throws Exception {
		assertNotNull(find(C12.class).unswap(null, 1, null));
	}

	@Test(expected = ParseException.class)
	public void c13_unswapConstructor_noConstructor() throws Exception {
		find(C13.class).unswap(null, 1, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore class
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore(on="D01c")
	private static class D01Config {}

	@BeanIgnore
	public static class D01 {
		public Integer toInteger() {
			return 1;
		}
	}
	public static class D01c {
		public Integer toInteger() {
			return 1;
		}
	}
	public static class D02 {
		public class D02A {
			public Integer toInteger() {
				return 1;
			}
		}
	}

	@Test
	public void d01_ignoreClass_beanIgnore() throws Exception {
		assertNull(find(D01.class));
	}

	@Test
	public void d01c_ignoreClass_beanIgnore_usingConfig() throws Exception {
		assertNull(find(bc(D01Config.class), D01c.class));
	}

	@Test
	public void d02_ignoreClass_memberClass() throws Exception {
		assertNull(find(D02.D02A.class));
	}

	@Test
	public void d03_ignoreClass_numberSubclass() throws Exception {
		assertNull(find(Integer.class));
		assertNull(find(Number.class));
		assertNull(find(int.class));
	}

	@Test
	public void d04_ignoreClass_primitive() throws Exception {
		assertNull(find(char.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ignore swap method
	//------------------------------------------------------------------------------------------------------------------

	@BeanIgnore(on="E01c.toInteger")
	private static class E01Config {}

	public static class E01 {
		@BeanIgnore
		public Integer toInteger() {
			return 1;
		}
	}
	public static class E01c {
		public Integer toInteger() {
			return 1;
		}
	}
	public static class E02 {
		@Deprecated
		public Integer toInteger() {
			return 1;
		}
	}
	public static class E03 {
		public Object toInteger() {
			return 1;
		}
	}
	public static class E04 {
		public Integer toInteger(List<String> foo) {
			return 1;
		}
	}
	public static class E05 {
		public static Integer toInteger() {
			return 1;
		}
	}

	@Test
	public void e01_ignoreSwapMethod_beanIgnore() throws Exception {
		assertNull(find(E01.class));
	}

	@Test
	public void e01c_ignoreSwapMethod_beanIgnore_usingConfig() throws Exception {
		assertNull(find(bc(E01Config.class), E01c.class));
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

	@BeanIgnore(on="F01c.create(java.lang.Integer)")
	private static class F01Config {}

	public static class F01 {
		public Integer toInteger() {
			return 1;
		}
		@BeanIgnore
		public static F01 create(Integer o) {
			return null;
		}
	}
	public static class F01c {
		public Integer toInteger() {
			return 1;
		}
		public static F01 create(Integer o) {
			return null;
		}
	}
	public static class F02 {
		public Integer toInteger() {
			return 1;
		}
		@Deprecated
		public static F02 create(Integer o) {
			return null;
		}
	}
	public static class F03 {
		public Integer toInteger() {
			return 1;
		}
		public static Object create(Integer o) {
			return null;
		}
	}
	public static class F04 {
		public Integer toInteger() {
			return 1;
		}
		public static F04 create(Map<String,String> o) {
			return null;
		}
	}
	public static class F05 {
		public Integer toInteger() {
			return 1;
		}
		public F05 create(Integer o) {
			return null;
		}
	}
	public static class F06 {
		public Integer toInteger() {
			return 1;
		}
		public static F06 createx(Integer o) {
			return null;
		}
	}

	@Test(expected = ParseException.class)
	public void f01_ignoreUnswapMethod_beanIgnore() throws Exception {
		find(F01.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void f01c_ignoreUnswapMethod_beanIgnore_usingConfig() throws Exception {
		find(bc(F01Config.class), F01c.class).unswap(null, null, null);
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

	@BeanIgnore(on="G01c(java.lang.Integer)")
	private static class G01Config {}

	public static class G01 {
		@BeanIgnore
		public G01(Integer o) {}
		public Integer toInteger() {
			return 1;
		}
	}

	public static class G01c {
		public G01c(Integer o) {}
		public Integer toInteger() {
			return 1;
		}
	}

	public static class G02 {
		@Deprecated
		public G02(Integer o) {}
		public Integer toInteger() {
			return 1;
		}
	}

	@Test(expected = ParseException.class)
	public void g01_ignoreUnswapConstructor_beanIgnore() throws Exception {
		find(G01.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void g01c_ignoreUnswapConstructor_beanIgnore_usingConfig() throws Exception {
		find(bc(G01Config.class), G01c.class).unswap(null, null, null);
	}

	@Test(expected = ParseException.class)
	public void g02_ignoreUnswapConstructor_deprecated() throws Exception {
		find(G02.class).unswap(null, null, null);
	}
}
