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

import java.util.*;

import org.apache.juneau.transform.*;
import org.junit.*;

@SuppressWarnings({"rawtypes","serial","javadoc"})
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

		bc = PropertyStore.create().getBeanContext();
		bs = bc.createSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertFalse(ooo.hasChildPojoSwaps());
		assertFalse(hi1.hasChildPojoSwaps());
		assertFalse(hc1.hasChildPojoSwaps());
		assertFalse(hi2.hasChildPojoSwaps());
		assertFalse(hc2.hasChildPojoSwaps());
		assertNull(ooo.getPojoSwap(bs));
		assertNull(hi1.getPojoSwap(bs));
		assertNull(hc1.getPojoSwap(bs));
		assertNull(hi2.getPojoSwap(bs));
		assertNull(hc2.getPojoSwap(bs));
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), HC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), HI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), HC2.class);

		bc = PropertyStore.create().setPojoSwaps(HI1Swap.class).getBeanContext();
		bs = bc.createSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoSwaps());
		assertTrue(hi1.hasChildPojoSwaps());
		assertFalse(hc1.hasChildPojoSwaps());
		assertFalse(hi2.hasChildPojoSwaps());
		assertFalse(hc2.hasChildPojoSwaps());
		assertNull(ooo.getPojoSwap(bs));
		assertEquals(hi1.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc1.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hi2.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc2.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = PropertyStore.create().setPojoSwaps(HC1Swap.class).getBeanContext();
		bs = bc.createSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoSwaps());
		assertTrue(hi1.hasChildPojoSwaps());
		assertTrue(hc1.hasChildPojoSwaps());
		assertFalse(hi2.hasChildPojoSwaps());
		assertFalse(hc2.hasChildPojoSwaps());
		assertNull(ooo.getPojoSwap(bs));
		assertNull(hi1.getPojoSwap(bs));
		assertEquals(hc1.getPojoSwap(bs).getClass(), HC1Swap.class);
		assertNull(hi2.getPojoSwap(bs));
		assertEquals(hc2.getPojoSwap(bs).getClass(), HC1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), HI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = PropertyStore.create().setPojoSwaps(HI2Swap.class).getBeanContext();
		bs = bc.createSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoSwaps());
		assertTrue(hi1.hasChildPojoSwaps());
		assertFalse(hc1.hasChildPojoSwaps());
		assertTrue(hi2.hasChildPojoSwaps());
		assertFalse(hc2.hasChildPojoSwaps());
		assertNull(ooo.getPojoSwap(bs));
		assertNull(hi1.getPojoSwap(bs));
		assertNull(hc1.getPojoSwap(bs));
		assertEquals(hi2.getPojoSwap(bs).getClass(), HI2Swap.class);
		assertEquals(hc2.getPojoSwap(bs).getClass(), HI2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), HC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = PropertyStore.create().setPojoSwaps(HC2Swap.class).getBeanContext();
		bs = bc.createSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoSwaps());
		assertTrue(hi1.hasChildPojoSwaps());
		assertTrue(hc1.hasChildPojoSwaps());
		assertTrue(hi2.hasChildPojoSwaps());
		assertTrue(hc2.hasChildPojoSwaps());
		assertNull(ooo.getPojoSwap(bs));
		assertNull(hi1.getPojoSwap(bs));
		assertNull(hc1.getPojoSwap(bs));
		assertNull(hi2.getPojoSwap(bs));
		assertEquals(hc2.getPojoSwap(bs).getClass(), HC2Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), HI1.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), HC1.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), HI2.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = PropertyStore.create().setPojoSwaps(HI1Swap.class,HC1Swap.class,HI2Swap.class,HC2Swap.class).getBeanContext();
		bs = bc.createSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoSwaps());
		assertTrue(hi1.hasChildPojoSwaps());
		assertTrue(hc1.hasChildPojoSwaps());
		assertTrue(hi2.hasChildPojoSwaps());
		assertTrue(hc2.hasChildPojoSwaps());
		assertNull(ooo.getPojoSwap(bs));
		assertEquals(hi1.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc1.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hi2.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc2.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(ooo.getSerializedClassMeta(bs).getInnerClass(), Object.class);
		assertEquals(hi1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc1.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hi2.getSerializedClassMeta(bs).getInnerClass(), Map.class);
		assertEquals(hc2.getSerializedClassMeta(bs).getInnerClass(), Map.class);

		bc = PropertyStore.create().setPojoSwaps(HC2Swap.class,HI2Swap.class,HC1Swap.class,HI1Swap.class).getBeanContext();
		bs = bc.createSession();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoSwaps());
		assertTrue(hi1.hasChildPojoSwaps());
		assertTrue(hc1.hasChildPojoSwaps());
		assertTrue(hi2.hasChildPojoSwaps());
		assertTrue(hc2.hasChildPojoSwaps());
		assertNull(ooo.getPojoSwap(bs));
		assertEquals(hi1.getPojoSwap(bs).getClass(), HI1Swap.class);
		assertEquals(hc1.getPojoSwap(bs).getClass(), HC1Swap.class);
		assertEquals(hi2.getPojoSwap(bs).getClass(), HI2Swap.class);
		assertEquals(hc2.getPojoSwap(bs).getClass(), HC2Swap.class);
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
	public static class HC1Swap extends PojoSwap<HC1,Map> {}
	public static class HI1Swap extends PojoSwap<HI1,Map> {}
	public static class HC2Swap extends PojoSwap<HC2,Map> {}
	public static class HI2Swap extends PojoSwap<HI2,Map> {}
}
