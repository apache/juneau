/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;

@SuppressWarnings({"rawtypes","serial","hiding"})
public class CT_ClassMeta {

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
		assertEquals("com.ibm.juno.core.test.CT_ClassMeta$G<java.lang.String,java.lang.Object>", t.toString());
		assertTrue(t.isMap());
		assertFalse(t.isCollection());
	}

	//====================================================================================================
	// testFilters
	// Ensure filters on parent and child classes are properly detected.
	//====================================================================================================
	@Test
	public void testFilters() throws Exception {
		BeanContext bc;
		ClassMeta<?> ooo, hi1, hc1, hi2, hc2;

		bc = new BeanContextFactory().getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertFalse(ooo.hasChildPojoFilters());
		assertFalse(hi1.hasChildPojoFilters());
		assertFalse(hc1.hasChildPojoFilters());
		assertFalse(hi2.hasChildPojoFilters());
		assertFalse(hc2.hasChildPojoFilters());
		assertNull(ooo.getPojoFilter());
		assertNull(hi1.getPojoFilter());
		assertNull(hc1.getPojoFilter());
		assertNull(hi2.getPojoFilter());
		assertNull(hc2.getPojoFilter());
		assertEquals(ooo.getFilteredClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getFilteredClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getFilteredClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getFilteredClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getFilteredClassMeta().getInnerClass(), HC2.class);

		bc = new BeanContextFactory().addFilters(HI1Filter.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoFilters());
		assertTrue(hi1.hasChildPojoFilters());
		assertFalse(hc1.hasChildPojoFilters());
		assertFalse(hi2.hasChildPojoFilters());
		assertFalse(hc2.hasChildPojoFilters());
		assertNull(ooo.getPojoFilter());
		assertEquals(hi1.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(hc1.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(hi2.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(hc2.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(ooo.getFilteredClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getFilteredClassMeta().getInnerClass(), Map.class);

		bc = new BeanContextFactory().addFilters(HC1Filter.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoFilters());
		assertTrue(hi1.hasChildPojoFilters());
		assertTrue(hc1.hasChildPojoFilters());
		assertFalse(hi2.hasChildPojoFilters());
		assertFalse(hc2.hasChildPojoFilters());
		assertNull(ooo.getPojoFilter());
		assertNull(hi1.getPojoFilter());
		assertEquals(hc1.getPojoFilter().getClass(), HC1Filter.class);
		assertNull(hi2.getPojoFilter());
		assertEquals(hc2.getPojoFilter().getClass(), HC1Filter.class);
		assertEquals(ooo.getFilteredClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getFilteredClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getFilteredClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getFilteredClassMeta().getInnerClass(), Map.class);

		bc = new BeanContextFactory().addFilters(HI2Filter.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoFilters());
		assertTrue(hi1.hasChildPojoFilters());
		assertFalse(hc1.hasChildPojoFilters());
		assertTrue(hi2.hasChildPojoFilters());
		assertFalse(hc2.hasChildPojoFilters());
		assertNull(ooo.getPojoFilter());
		assertNull(hi1.getPojoFilter());
		assertNull(hc1.getPojoFilter());
		assertEquals(hi2.getPojoFilter().getClass(), HI2Filter.class);
		assertEquals(hc2.getPojoFilter().getClass(), HI2Filter.class);
		assertEquals(ooo.getFilteredClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getFilteredClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getFilteredClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getFilteredClassMeta().getInnerClass(), Map.class);

		bc = new BeanContextFactory().addFilters(HC2Filter.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoFilters());
		assertTrue(hi1.hasChildPojoFilters());
		assertTrue(hc1.hasChildPojoFilters());
		assertTrue(hi2.hasChildPojoFilters());
		assertTrue(hc2.hasChildPojoFilters());
		assertNull(ooo.getPojoFilter());
		assertNull(hi1.getPojoFilter());
		assertNull(hc1.getPojoFilter());
		assertNull(hi2.getPojoFilter());
		assertEquals(hc2.getPojoFilter().getClass(), HC2Filter.class);
		assertEquals(ooo.getFilteredClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getFilteredClassMeta().getInnerClass(), HI1.class);
		assertEquals(hc1.getFilteredClassMeta().getInnerClass(), HC1.class);
		assertEquals(hi2.getFilteredClassMeta().getInnerClass(), HI2.class);
		assertEquals(hc2.getFilteredClassMeta().getInnerClass(), Map.class);

		bc = new BeanContextFactory().addFilters(HI1Filter.class,HC1Filter.class,HI2Filter.class,HC2Filter.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoFilters());
		assertTrue(hi1.hasChildPojoFilters());
		assertTrue(hc1.hasChildPojoFilters());
		assertTrue(hi2.hasChildPojoFilters());
		assertTrue(hc2.hasChildPojoFilters());
		assertNull(ooo.getPojoFilter());
		assertEquals(hi1.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(hc1.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(hi2.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(hc2.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(ooo.getFilteredClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getFilteredClassMeta().getInnerClass(), Map.class);

		bc = new BeanContextFactory().addFilters(HC2Filter.class,HI2Filter.class,HC1Filter.class,HI1Filter.class).getBeanContext();
		ooo = bc.getClassMeta(Object.class);
		hi1 = bc.getClassMeta(HI1.class);
		hc1 = bc.getClassMeta(HC1.class);
		hi2 = bc.getClassMeta(HI2.class);
		hc2 = bc.getClassMeta(HC2.class);
		assertTrue(ooo.hasChildPojoFilters());
		assertTrue(hi1.hasChildPojoFilters());
		assertTrue(hc1.hasChildPojoFilters());
		assertTrue(hi2.hasChildPojoFilters());
		assertTrue(hc2.hasChildPojoFilters());
		assertNull(ooo.getPojoFilter());
		assertEquals(hi1.getPojoFilter().getClass(), HI1Filter.class);
		assertEquals(hc1.getPojoFilter().getClass(), HC1Filter.class);
		assertEquals(hi2.getPojoFilter().getClass(), HI2Filter.class);
		assertEquals(hc2.getPojoFilter().getClass(), HC2Filter.class);
		assertEquals(ooo.getFilteredClassMeta().getInnerClass(), Object.class);
		assertEquals(hi1.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hc1.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hi2.getFilteredClassMeta().getInnerClass(), Map.class);
		assertEquals(hc2.getFilteredClassMeta().getInnerClass(), Map.class);
	}

	public interface HI1 {}
	public class HC1 implements HI1 {}
	public interface HI2 extends HI1 {}
	public class HC2 extends HC1 implements HI2 {}
	public static class HC1Filter extends PojoFilter<HC1,Map> {}
	public static class HI1Filter extends PojoFilter<HI1,Map> {}
	public static class HC2Filter extends PojoFilter<HC2,Map> {}
	public static class HI2Filter extends PojoFilter<HI2,Map> {}
}
