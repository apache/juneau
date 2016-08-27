/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.transform.*;
import org.junit.*;

@SuppressWarnings({"rawtypes","serial","hiding","javadoc"})
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
	// testTransforms
	// Ensure filters on parent and child classes are properly detected.
	//====================================================================================================
	@Test
	public void testTransforms() throws Exception {
		BeanContext bc;
		ClassMeta<?> ooo, hi1, hc1, hi2, hc2;

		bc = ContextFactory.create().getBeanContext();
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
		assertNull(ooo.getPojoSwap());
		assertNull(hi1.getPojoSwap());
		assertNull(hc1.getPojoSwap());
		assertNull(hi2.getPojoSwap());
		assertNull(hc2.getPojoSwap());
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), HC2.class);

		bc = ContextFactory.create().addTransforms(HI1Swap.class).getBeanContext();
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
		assertNull(ooo.getPojoSwap());
		assertEquals(hi1.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(hc1.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(hi2.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(hc2.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HC1Swap.class).getBeanContext();
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
		assertNull(ooo.getPojoSwap());
		assertNull(hi1.getPojoSwap());
		assertEquals(hc1.getPojoSwap().getClass(), HC1Swap.class);
		assertNull(hi2.getPojoSwap());
		assertEquals(hc2.getPojoSwap().getClass(), HC1Swap.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HI2Swap.class).getBeanContext();
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
		assertNull(ooo.getPojoSwap());
		assertNull(hi1.getPojoSwap());
		assertNull(hc1.getPojoSwap());
		assertEquals(hi2.getPojoSwap().getClass(), HI2Swap.class);
		assertEquals(hc2.getPojoSwap().getClass(), HI2Swap.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HC2Swap.class).getBeanContext();
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
		assertNull(ooo.getPojoSwap());
		assertNull(hi1.getPojoSwap());
		assertNull(hc1.getPojoSwap());
		assertNull(hi2.getPojoSwap());
		assertEquals(hc2.getPojoSwap().getClass(), HC2Swap.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HI1Swap.class,HC1Swap.class,HI2Swap.class,HC2Swap.class).getBeanContext();
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
		assertNull(ooo.getPojoSwap());
		assertEquals(hi1.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(hc1.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(hi2.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(hc2.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HC2Swap.class,HI2Swap.class,HC1Swap.class,HI1Swap.class).getBeanContext();
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
		assertNull(ooo.getPojoSwap());
		assertEquals(hi1.getPojoSwap().getClass(), HI1Swap.class);
		assertEquals(hc1.getPojoSwap().getClass(), HC1Swap.class);
		assertEquals(hi2.getPojoSwap().getClass(), HI2Swap.class);
		assertEquals(hc2.getPojoSwap().getClass(), HC2Swap.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);
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
