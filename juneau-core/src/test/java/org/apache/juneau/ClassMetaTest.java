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

@SuppressWarnings({"rawtypes","serial","hiding"})
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
		assertFalse(ooo.hasChildPojoTransforms());
		assertFalse(hi1.hasChildPojoTransforms());
		assertFalse(hc1.hasChildPojoTransforms());
		assertFalse(hi2.hasChildPojoTransforms());
		assertFalse(hc2.hasChildPojoTransforms());
		assertNull(ooo.getPojoTransform());
		assertNull(hi1.getPojoTransform());
		assertNull(hc1.getPojoTransform());
		assertNull(hi2.getPojoTransform());
		assertNull(hc2.getPojoTransform());
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), HC2.class);

		bc = ContextFactory.create().addTransforms(HI1Transform.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoTransforms());
		assertTrue(hi1.hasChildPojoTransforms());
		assertFalse(hc1.hasChildPojoTransforms());
		assertFalse(hi2.hasChildPojoTransforms());
		assertFalse(hc2.hasChildPojoTransforms());
		assertNull(ooo.getPojoTransform());
		assertEquals(hi1.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(hc1.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(hi2.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(hc2.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HC1Transform.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoTransforms());
		assertTrue(hi1.hasChildPojoTransforms());
		assertTrue(hc1.hasChildPojoTransforms());
		assertFalse(hi2.hasChildPojoTransforms());
		assertFalse(hc2.hasChildPojoTransforms());
		assertNull(ooo.getPojoTransform());
		assertNull(hi1.getPojoTransform());
		assertEquals(hc1.getPojoTransform().getClass(), HC1Transform.class);
		assertNull(hi2.getPojoTransform());
		assertEquals(hc2.getPojoTransform().getClass(), HC1Transform.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HI2Transform.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoTransforms());
		assertTrue(hi1.hasChildPojoTransforms());
		assertFalse(hc1.hasChildPojoTransforms());
		assertTrue(hi2.hasChildPojoTransforms());
		assertFalse(hc2.hasChildPojoTransforms());
		assertNull(ooo.getPojoTransform());
		assertNull(hi1.getPojoTransform());
		assertNull(hc1.getPojoTransform());
		assertEquals(hi2.getPojoTransform().getClass(), HI2Transform.class);
		assertEquals(hc2.getPojoTransform().getClass(), HI2Transform.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HC2Transform.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoTransforms());
		assertTrue(hi1.hasChildPojoTransforms());
		assertTrue(hc1.hasChildPojoTransforms());
		assertTrue(hi2.hasChildPojoTransforms());
		assertTrue(hc2.hasChildPojoTransforms());
		assertNull(ooo.getPojoTransform());
		assertNull(hi1.getPojoTransform());
		assertNull(hc1.getPojoTransform());
		assertNull(hi2.getPojoTransform());
		assertEquals(hc2.getPojoTransform().getClass(), HC2Transform.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HI1Transform.class,HC1Transform.class,HI2Transform.class,HC2Transform.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoTransforms());
		assertTrue(hi1.hasChildPojoTransforms());
		assertTrue(hc1.hasChildPojoTransforms());
		assertTrue(hi2.hasChildPojoTransforms());
		assertTrue(hc2.hasChildPojoTransforms());
		assertNull(ooo.getPojoTransform());
		assertEquals(hi1.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(hc1.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(hi2.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(hc2.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(ooo.getTransformedClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getTransformedClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getTransformedClassMeta().getInnerClass(), Map.class);

		bc = ContextFactory.create().addTransforms(HC2Transform.class,HI2Transform.class,HC1Transform.class,HI1Transform.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoTransforms());
		assertTrue(hi1.hasChildPojoTransforms());
		assertTrue(hc1.hasChildPojoTransforms());
		assertTrue(hi2.hasChildPojoTransforms());
		assertTrue(hc2.hasChildPojoTransforms());
		assertNull(ooo.getPojoTransform());
		assertEquals(hi1.getPojoTransform().getClass(), HI1Transform.class);
		assertEquals(hc1.getPojoTransform().getClass(), HC1Transform.class);
		assertEquals(hi2.getPojoTransform().getClass(), HI2Transform.class);
		assertEquals(hc2.getPojoTransform().getClass(), HC2Transform.class);
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
	public static class HC1Transform extends PojoTransform<HC1,Map> {}
	public static class HI1Transform extends PojoTransform<HI1,Map> {}
	public static class HC2Transform extends PojoTransform<HC2,Map> {}
	public static class HI2Transform extends PojoTransform<HI2,Map> {}
}
