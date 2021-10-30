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
package org.apache.juneau;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.swap.*;
import org.junit.*;

@SuppressWarnings({"rawtypes","serial"})
@FixMethodOrder(NAME_ASCENDING)
public class ClassMetaTest {

	BeanContext bc = BeanContext.DEFAULT;

	//====================================================================================================
	// Map<String,String> field
	//====================================================================================================
	public Map<String,String> fa;

	@Test
	public void testMap() throws Exception {
		ClassMeta t = bc.getClassMeta(this.getClass().getField("fa").getGenericType());
		assertEquals("java.util.Map<java.lang.String,java.lang.String>", t.toString());
		assertTrue(t.isMap());
		assertFalse(t.isCollection());
		assertNull(t.newInstance());
		assertEquals(Map.class, t.getInnerClass());
		assertEquals(String.class, t.getKeyType().getInnerClass());
		assertEquals(String.class, t.getValueType().getInnerClass());
	}

	//====================================================================================================
	// String field
	//====================================================================================================
	public String fb;

	@Test
	public void testString() throws Exception {
		ClassMeta t = bc.getClassMeta(this.getClass().getField("fb").getGenericType());
		assertEquals(String.class, t.getInnerClass());
		t = bc.getClassMeta(this.getClass().getField("fb").getType());
		assertEquals(String.class, t.getInnerClass());
	}

	//====================================================================================================
	// Map<String,Map<String,Integer>> field
	//====================================================================================================
	public Map<String,Map<String,Integer>> fc;

	@Test
	public void testMapWithMapValues() throws Exception {
		ClassMeta t = bc.getClassMeta(this.getClass().getField("fc").getGenericType());
		assertEquals("java.util.Map<java.lang.String,java.util.Map<java.lang.String,java.lang.Integer>>", t.toString());
		t = bc.getClassMeta(this.getClass().getField("fc").getType());
		assertEquals("java.util.Map", t.toString());
	}

	//====================================================================================================
	// List<Map<String,List>> field
	//====================================================================================================
	public List<Map<String,List>> fd;

	@Test
	public void testListWithMapValues() throws Exception {
		ClassMeta t = bc.getClassMeta(this.getClass().getField("fd").getGenericType());
		assertEquals("java.util.List<java.util.Map<java.lang.String,java.util.List>>", t.toString());
	}

	//====================================================================================================
	// List<? extends String> field, List<? super String> field
	//====================================================================================================
	public List<? extends String> fe1;
	public List<? super String> fe2;

	@Test
	public void testListWithUpperBoundGenericEntryTypes() throws Exception {
		ClassMeta t = bc.getClassMeta(this.getClass().getField("fe1").getGenericType());
		assertEquals("java.util.List", t.toString());
		t = bc.getClassMeta(this.getClass().getField("fe2").getGenericType());
		assertEquals("java.util.List", t.toString());
	}

	//====================================================================================================
	// Bean extends HashMap<String,Object> field
	//====================================================================================================
	public class G extends HashMap<String,Object> {}
	public G g;

	@Test
	public void testBeanExtendsMap() throws Exception {
		ClassMeta t = bc.getClassMeta(this.getClass().getField("g").getGenericType());
		assertEquals("org.apache.juneau.ClassMetaTest$G<java.lang.String,java.lang.Object>", t.toString());
		assertTrue(t.isMap());
		assertFalse(t.isCollection());
	}

	//====================================================================================================
	// testSwaps
	// Ensure swaps on parent and child classes are properly detected.
	//====================================================================================================
	@Test
	public void testSwaps() throws Exception {
		BeanContext bc;
		ClassMeta<?> ooo, hi1, hc1, hi2, hc2;
		BeanSession bs;

		bc = BeanContext.DEFAULT;
		bs = bc.getSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
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
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), HC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), HI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), HC2.class);

		bc = BeanContext.create().swaps(HI1Swap.class).build();
		bs = bc.getSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertFalse(hc1.hasChildSwaps());
		assertFalse(hi2.hasChildSwaps());
		assertFalse(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertEquals(hi1.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc1.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hi2.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = BeanContext.create().swaps(HC1Swap.class).build();
		bs = bc.getSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertFalse(hi2.hasChildSwaps());
		assertFalse(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertNull(hi1.getSwap(bs));
		assertEquals(hc1.getSwap(bs).getClass(), HC1Swap.class);
		assertNull(hi2.getSwap(bs));
		assertEquals(hc2.getSwap(bs).getClass(), HC1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), HI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = BeanContext.create().swaps(HI2Swap.class).build();
		bs = bc.getSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertFalse(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertFalse(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertNull(hi1.getSwap(bs));
		assertNull(hc1.getSwap(bs));
		assertEquals(hi2.getSwap(bs).getClass(), HI2Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), HI2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), HC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = BeanContext.create().swaps(HC2Swap.class).build();
		bs = bc.getSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertTrue(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertNull(hi1.getSwap(bs));
		assertNull(hc1.getSwap(bs));
		assertNull(hi2.getSwap(bs));
		assertEquals(hc2.getSwap(bs).getClass(), HC2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), HC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), HI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = BeanContext.create().swaps(HI1Swap.class,HC1Swap.class,HI2Swap.class, HC2Swap.class).build();
		bs = bc.getSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertTrue(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertEquals(hi1.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc1.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hi2.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = BeanContext.create().swaps(HC2Swap.class,HI2Swap.class,HC1Swap.class, HI1Swap.class).build();
		bs = bc.getSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildSwaps());
		assertTrue(hi1.hasChildSwaps());
		assertTrue(hc1.hasChildSwaps());
		assertTrue(hi2.hasChildSwaps());
		assertTrue(hc2.hasChildSwaps());
		assertNull(ooo.getSwap(bs));
		assertEquals(hi1.getSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc1.getSwap(bs).getClass(), HC1Swap.class);
		assertEquals(hi2.getSwap(bs).getClass(), HI2Swap.class);
		assertEquals(hc2.getSwap(bs).getClass(), HC2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
	}

	public interface HI1 {}
	public class HC1 implements HI1 {}
	public interface HI2 extends HI1 {}
	public class HC2 extends HC1 implements HI2 {}
	public static class HC1Swap extends ObjectSwap<HC1,Map> {}
	public static class HI1Swap extends ObjectSwap<HI1,Map> {}
	public static class HC2Swap extends ObjectSwap<HC2,Map> {}
	public static class HI2Swap extends ObjectSwap<HI2,Map> {}
}
