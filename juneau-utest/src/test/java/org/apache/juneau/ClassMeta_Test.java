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
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.reflect.ClassInfo_Test.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"rawtypes","serial"})
class ClassMeta_Test extends TestBase {

	BeanContext bc = BeanContext.DEFAULT;

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	public Map<String,String> fa;

	@Test void a01_map() throws Exception {
		var t = bc.getClassMeta(this.getClass().getField("fa").getGenericType());
		assertEquals("java.util.Map<java.lang.String,java.lang.String>", t.toString());
		assertTrue(t.isMap());
		assertFalse(t.isCollection());
		assertNull(t.newInstance());
		assertEquals(Map.class, t.getInnerClass());
		assertEquals(String.class, t.getKeyType().getInnerClass());
		assertEquals(String.class, t.getValueType().getInnerClass());
	}

	public String fb;

	@Test void a02_string() throws Exception {
		var t = bc.getClassMeta(this.getClass().getField("fb").getGenericType());
		assertEquals(String.class, t.getInnerClass());
		var t2 = bc.getClassMeta(this.getClass().getField("fb").getType());
		assertEquals(String.class, t2.getInnerClass());
	}

	public Map<String,Map<String,Integer>> fc;

	@Test void a03_mapWithMapValues() throws Exception {
		var t = bc.getClassMeta(this.getClass().getField("fc").getGenericType());
		assertEquals("java.util.Map<java.lang.String,java.util.Map<java.lang.String,java.lang.Integer>>", t.toString());
		var t2 = bc.getClassMeta(this.getClass().getField("fc").getType());
		assertEquals("java.util.Map", t2.toString());
	}

	public List<Map<String,List>> fd;

	@Test void a04_listWithMapValues() throws Exception {
		var t = bc.getClassMeta(this.getClass().getField("fd").getGenericType());
		assertEquals("java.util.List<java.util.Map<java.lang.String,java.util.List>>", t.toString());
	}

	public List<? extends String> fe1;
	public List<? super String> fe2;

	@Test void a05_listWithUpperBoundGenericEntryTypes() throws Exception {
		var t = bc.getClassMeta(this.getClass().getField("fe1").getGenericType());
		assertEquals("java.util.List", t.toString());
		t = bc.getClassMeta(this.getClass().getField("fe2").getGenericType());
		assertEquals("java.util.List", t.toString());
	}

	public class G extends HashMap<String,Object> {}
	public G g;

	@Test void a06_beanExtendsMap() throws Exception {
		var t = bc.getClassMeta(this.getClass().getField("g").getGenericType());
		assertEquals("org.apache.juneau.ClassMeta_Test$G<java.lang.String,java.lang.Object>", t.toString());
		assertTrue(t.isMap());
		assertFalse(t.isCollection());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Swaps
	// Ensure swaps on parent and child classes are properly detected.
	//-----------------------------------------------------------------------------------------------------------------

	public interface BI1 {}
	public class BC1 implements BI1 {}
	public interface BI2 extends BI1 {}
	public class BC2 extends BC1 implements BI2 {}
	public static class BC1Swap extends ObjectSwap<BC1,Map> {}
	public static class BI1Swap extends ObjectSwap<BI1,Map> {}
	public static class BC2Swap extends ObjectSwap<BC2,Map> {}
	public static class BI2Swap extends ObjectSwap<BI2,Map> {}

	@Test void b01_swaps() {

		var bc2 = BeanContext.DEFAULT;
		var bs = bc2.getSession();
		var ooo = bc2.getClassMeta(Object.class);
		var hi1 = bc2.getClassMeta(BI1.class);
		var hc1 = bc2.getClassMeta(BC1.class);
		var hi2 = bc2.getClassMeta(BI2.class);
		var hc2 = bc2.getClassMeta(BC2.class);
		assertFalse(ooo.hasChildSwaps());
		assertFalse(hi1.hasChildSwaps());
		assertFalse(hc1.hasChildSwaps());
		assertFalse(hi2.hasChildSwaps());
		assertFalse(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertNull(hi1.getSwap(bs));
		assertNull(hc1.getSwap(bs));
		assertNull(hi2.getSwap(bs));
		assertNull(hc2.getSwap(bs));
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), BI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), BC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), BI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), BC2.class);

		bc2 = BeanContext.create().swaps(BI1Swap.class).build();
		bs = bc2.getSession();
		ooo = bc2.getClassMeta(Object.class);
		hi1 = bc2.getClassMeta(BI1.class);
		hc1 = bc2.getClassMeta(BC1.class);
		hi2 = bc2.getClassMeta(BI2.class);
		hc2 = bc2.getClassMeta(BC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertFalse(hc1.hasChildSwaps());
		assertFalse(hi2.hasChildSwaps());
		assertFalse(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertEquals(hi1.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(hc1.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(hi2.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc2 = BeanContext.create().swaps(BC1Swap.class).build();
		bs = bc2.getSession();
		ooo = bc2.getClassMeta(Object.class);
		hi1 = bc2.getClassMeta(BI1.class);
		hc1 = bc2.getClassMeta(BC1.class);
		hi2 = bc2.getClassMeta(BI2.class);
		hc2 = bc2.getClassMeta(BC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertFalse(hi2.hasChildSwaps());
		assertFalse(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertNull(hi1.getSwap(bs));
		assertEquals(hc1.getSwap(bs).getClass(), BC1Swap.class);
		assertNull(hi2.getSwap(bs));
		assertEquals(hc2.getSwap(bs).getClass(), BC1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), BI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), BI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc2 = BeanContext.create().swaps(BI2Swap.class).build();
		bs = bc2.getSession();
		ooo = bc2.getClassMeta(Object.class);
		hi1 = bc2.getClassMeta(BI1.class);
		hc1 = bc2.getClassMeta(BC1.class);
		hi2 = bc2.getClassMeta(BI2.class);
		hc2 = bc2.getClassMeta(BC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertFalse(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertFalse(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertNull(hi1.getSwap(bs));
		assertNull(hc1.getSwap(bs));
		assertEquals(hi2.getSwap(bs).getClass(), BI2Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), BI2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), BI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), BC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc2 = BeanContext.create().swaps(BC2Swap.class).build();
		bs = bc2.getSession();
		ooo = bc2.getClassMeta(Object.class);
		hi1 = bc2.getClassMeta(BI1.class);
		hc1 = bc2.getClassMeta(BC1.class);
		hi2 = bc2.getClassMeta(BI2.class);
		hc2 = bc2.getClassMeta(BC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertTrue(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertNull(hi1.getSwap(bs));
		assertNull(hc1.getSwap(bs));
		assertNull(hi2.getSwap(bs));
		assertEquals(hc2.getSwap(bs).getClass(), BC2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), BI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), BC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), BI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc2 = BeanContext.create().swaps(BI1Swap.class,BC1Swap.class,BI2Swap.class, BC2Swap.class).build();
		bs = bc2.getSession();
		ooo = bc2.getClassMeta(Object.class);
		hi1 = bc2.getClassMeta(BI1.class);
		hc1 = bc2.getClassMeta(BC1.class);
		hi2 = bc2.getClassMeta(BI2.class);
		hc2 = bc2.getClassMeta(BC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertTrue(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertEquals(hi1.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(hc1.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(hi2.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc2 = BeanContext.create().swaps(BC2Swap.class,BI2Swap.class,BC1Swap.class, BI1Swap.class).build();
		bs = bc2.getSession();
		ooo = bc2.getClassMeta(Object.class);
		hi1 = bc2.getClassMeta(BI1.class);
		hc1 = bc2.getClassMeta(BC1.class);
		hi2 = bc2.getClassMeta(BI2.class);
		hc2 = bc2.getClassMeta(BC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertTrue(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertEquals(hi1.getSwap(bs).getClass(), BI1Swap.class);
		assertEquals(hc1.getSwap(bs).getClass(), BC1Swap.class);
		assertEquals(hi2.getSwap(bs).getClass(), BI2Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), BC2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@A(1) interface CI1 {}
	@A(2) interface CI2 extends CI1 {}
	@A(3) interface CI3 {}
	@A(4) interface CI4 {}
	@SuppressWarnings("unused") @A(5) static class C1 implements CI1, CI2 {}
	@A(6) static class C2 extends C1 implements CI3 {}
	@A(7) static class C3 extends C2 {}
	static class C4 extends C3 {}
	static class C5 implements CI3 {}

	@Test void forEachAnnotation() {
		var c3 = bc.getClassMeta(C3.class);
		var c4 = bc.getClassMeta(C4.class);
		var c5 = bc.getClassMeta(C5.class);

		var l1 = list();
		c3.forEachAnnotation(A.class, null, x -> l1.add(x.value()));
		assertList(l1, "2", "1", "3", "5", "6", "7");

		var l2 = list();
		c4.forEachAnnotation(A.class, null, x -> l2.add(x.value()));
		assertList(l2, "2", "1", "3", "5", "6", "7");

		var l3 = list();
		c5.forEachAnnotation(A.class, null, x -> l3.add(x.value()));
		assertList(l3, "3");

		var l4 = list();
		c3.forEachAnnotation(A.class, x -> x.value() == 5, x -> l4.add(x.value()));
		assertList(l4, "5");
	}

	@Test void firstAnnotation() {
		var c3 = bc.getClassMeta(C3.class);
		var c4 = bc.getClassMeta(C4.class);
		var c5 = bc.getClassMeta(C5.class);
		assertEquals(2, c3.firstAnnotation(A.class, null).get().value());
		assertEquals(2, c4.firstAnnotation(A.class, null).get().value());
		assertEquals(3, c5.firstAnnotation(A.class, null).get().value());
		assertEquals(5, c3.firstAnnotation(A.class, x -> x.value() == 5).get().value());
	}

	@Test void lastAnnotation() {
		var c3 = bc.getClassMeta(C3.class);
		var c4 = bc.getClassMeta(C4.class);
		var c5 = bc.getClassMeta(C5.class);
		assertEquals(7, c3.lastAnnotation(A.class, null).get().value());
		assertEquals(7, c4.lastAnnotation(A.class, null).get().value());
		assertEquals(3, c5.lastAnnotation(A.class, null).get().value());
		assertEquals(5, c3.lastAnnotation(A.class, x -> x.value() == 5).get().value());
	}
}