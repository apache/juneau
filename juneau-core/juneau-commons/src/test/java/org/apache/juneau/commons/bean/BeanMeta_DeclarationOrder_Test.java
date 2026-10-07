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
package org.apache.juneau.commons.bean;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests declaration-order property seeding in {@link BeanMeta} (WORK-J0585).
 */
@SuppressWarnings("unused")
class BeanMeta_DeclarationOrder_Test extends TestBase {

	private static final BeanConfigContext SORTED = BeanConfigContext.DEFAULT;
	private static final BeanConfigContext UNSORTED = BeanConfigContext.create().unsortedProperties(true).build();
	private static final BeanConfigContext RECORD_ORDER = BeanConfigContext.create().recordComponentOrder(true).build();

	private static String keys(Class<?> c, BeanConfigContext ctx) {
		return String.join(",", BeanMeta.of(c, ctx).getProperties().keySet());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - records
	//-----------------------------------------------------------------------------------------------------------------

	public record A1(String zeta, int alpha, boolean mid) {}
	public record A2(@BeanProp("z_name") String zeta, int alpha) {}
	public interface A3Extra { default String getExtra() { return "x"; } }
	public record A3(String zeta, int alpha) implements A3Extra {}

	@Test void a01_record_sortedByDefault() {
		assertList(BeanMeta.of(A1.class, SORTED).getProperties().keySet(), "alpha", "mid", "zeta");
	}

	@Test void a02_record_componentOrder_viaRecordComponentOrder() {
		assertList(BeanMeta.of(A1.class, RECORD_ORDER).getProperties().keySet(), "zeta", "alpha", "mid");
	}

	@Test void a03_record_componentOrder_viaUnsorted() {
		assertList(BeanMeta.of(A1.class, UNSORTED).getProperties().keySet(), "zeta", "alpha", "mid");
	}

	@Test void a04_record_renamedComponentKeepsPosition() {
		assertList(BeanMeta.of(A2.class, RECORD_ORDER).getProperties().keySet(), "z_name", "alpha");
		assertList(BeanMeta.of(A2.class, SORTED).getProperties().keySet(), "alpha", "z_name");
	}

	@Test void a05_record_interfaceDefaultGetterAppended() {
		assertList(BeanMeta.of(A3.class, RECORD_ORDER).getProperties().keySet(), "zeta", "alpha", "extra");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - ordinary classes (unsorted mode only)
	//-----------------------------------------------------------------------------------------------------------------

	public static class B1 { public String zeta; public String alpha; public String mid; }

	public static class B2 {
		private String zeta;
		private String alpha;
		public String getZeta() { return zeta; }
		public void setZeta(String v) { zeta = v; }
		public String getAlpha() { return alpha; }
		public void setAlpha(String v) { alpha = v; }
	}

	public static class B3 {
		public String getZeta() { return "z"; }
		public String getAlpha() { return "a"; }
	}

	public static class B4 {
		public String zeta;
		private int mid;
		public int getMid() { return mid; }
		public void setMid(int v) { mid = v; }
		public String getAlpha() { return "a"; }       // method-only: goes after, alphabetically
		public String getBeta() { return "b"; }        // method-only
	}

	@Test void b01_publicFields_declarationOrder() {
		assertList(BeanMeta.of(B1.class, UNSORTED).getProperties().keySet(), "zeta", "alpha", "mid");
	}

	@Test void b02_publicFields_sortedByDefault() {
		assertList(BeanMeta.of(B1.class, SORTED).getProperties().keySet(), "alpha", "mid", "zeta");
	}

	@Test void b03_privateFieldHintForGetterSetter() {
		assertList(BeanMeta.of(B2.class, UNSORTED).getProperties().keySet(), "zeta", "alpha");
	}

	@Test void b04_getterOnly_noFields_staysAlphabetical() {
		assertList(BeanMeta.of(B3.class, UNSORTED).getProperties().keySet(), "alpha", "zeta");
	}

	@Test void b05_mixed_fieldRankedFirst_methodOnlyAlphabeticalAfter() {
		assertList(BeanMeta.of(B4.class, UNSORTED).getProperties().keySet(), "zeta", "mid", "alpha", "beta");
	}

	@Test void b06_recordComponentOrderDoesNotAffectClasses() {
		assertList(BeanMeta.of(B1.class, RECORD_ORDER).getProperties().keySet(), "alpha", "mid", "zeta");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - inheritance
	//-----------------------------------------------------------------------------------------------------------------

	public static class C1Parent { public String zParent; public String aParent; }
	public static class C1Child extends C1Parent { public String zChild; public String aChild; }

	public static class C2Parent {
		private String zeta;
		private String alpha;
		public String getZeta() { return zeta; }
		public void setZeta(String v) { zeta = v; }
		public String getAlpha() { return alpha; }
		public void setAlpha(String v) { alpha = v; }
	}
	public static class C2Child extends C2Parent {
		private String beta;
		public String getBeta() { return beta; }
		public void setBeta(String v) { beta = v; }
		@Override public String getAlpha() { return super.getAlpha(); }
	}

	@Test void c01_superclassFieldsFirst() {
		assertList(BeanMeta.of(C1Child.class, UNSORTED).getProperties().keySet(), "zParent", "aParent", "zChild", "aChild");
	}

	@Test void c02_overriddenGetterKeepsSuperclassPosition() {
		assertList(BeanMeta.of(C2Child.class, UNSORTED).getProperties().keySet(), "zeta", "alpha", "beta");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - precedence and determinism
	//-----------------------------------------------------------------------------------------------------------------

	// d01 (@BeanType(properties) precedence) and d02 (@BeanType(unsorted=true)) are covered in the marshall module
	// (RecordComponentOrder_Test, Task 4): the commons-side BeanMeta.of(...) path does not apply @BeanType filters.

	public interface D3 { String getZeta(); String getAlpha(); }

	@Test void d03_interfaceStaysAlphabetical() {
		assertList(BeanMeta.of(D3.class, UNSORTED).getProperties().keySet(), "alpha", "zeta");
	}

	@Test void d04_deterministicAcrossContexts() {
		var other = BeanConfigContext.create().unsortedProperties(true).build();
		assertEquals(keys(B4.class, UNSORTED), keys(B4.class, other));
	}

	public static class D4 { public String zeta; public String alpha; }

	@Test void d06_beanFilterUnsorted_declarationOrder() {
		// Same fixture pattern as BeanMeta_Discovery_Coverage_Test.c05.
		var filter = new BeanTestFakes.FakeBeanFilter().unsortedProperties(true);
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new BeanTestFakes.FakeBeanInfo<>(D4.class, cfg), null).beanMeta();
		assertList(bm.getProperties().keySet(), "zeta", "alpha");
	}

	@Test void d05_flagExposed() {
		assertTrue(BeanMeta.of(A1.class, RECORD_ORDER).isUnsortedProperties());
		assertFalse(BeanMeta.of(B1.class, RECORD_ORDER).isUnsortedProperties());
	}
}
