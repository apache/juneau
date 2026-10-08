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
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@code @BeanProp(required)} metadata.
 */
@SuppressWarnings("unused")
class BeanMeta_Required_Test extends TestBase {

	private static BeanMeta<?> meta(Class<?> c) {
		return BeanMeta.of(c, BeanConfigContext.DEFAULT);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - metadata
	//-----------------------------------------------------------------------------------------------------------------

	public static class A1 {
		@BeanProp(required=true) public String a;
		public String b;
		@BeanProp(required=true) public String c;
	}

	public static class A2 {
		private String x;
		@BeanProp(required=true) public String getX() { return x; }   // on the getter
		public void setX(String v) { x = v; }
	}

	public static class A3 {
		private String x;
		public String getX() { return x; }
		@BeanProp(required=true) public void setX(String v) { x = v; }   // on the setter
	}

	public record A4(@BeanProp(required=true) String name, int age) {}

	public static class A5 { public String a; }

	public static class A6 {
		@BeanProp(required=true) public String getX() { return "x"; }    // read-only
	}

	public static class A7 {
		@BeanProp(required=true, ro="true") public String x;              // explicitly read-only
	}

	public static class A8 {
		@BeanProp(required=true) @BeanIgnore public String hidden;
		public String b;
	}

	public static class A9 {
		@BeanProp(name="*", required=true) public java.util.Map<String,Object> extras;   // dynamic
		public String b;
	}

	@Test void a01_fieldAnnotation() {
		var m = meta(A1.class);
		assertList(m.getRequiredPropertyNames(), "a", "c");
		assertTrue(m.hasRequiredProperties());
		assertBean(m.getProperties().get("a"), "required", "true");
		assertBean(m.getProperties().get("b"), "required", "false");
	}

	@Test void a02_getterAnnotation_orMerged() {
		assertList(meta(A2.class).getRequiredPropertyNames(), "x");
	}

	@Test void a03_setterAnnotation_orMerged() {
		assertList(meta(A3.class).getRequiredPropertyNames(), "x");
	}

	@Test void a04_recordComponent() {
		assertList(meta(A4.class).getRequiredPropertyNames(), "name");
	}

	@Test void a05_noneRequired() {
		var m = meta(A5.class);
		assertList(m.getRequiredPropertyNames());
		assertFalse(m.hasRequiredProperties());
	}

	@Test void a06_readOnlyRequired_failsFast() {
		assertThrowsWithMessage(BeanRuntimeException.class, "cannot be written", () -> meta(A6.class));
	}

	@Test void a07_explicitRoRequired_failsFast() {
		assertThrowsWithMessage(BeanRuntimeException.class, "cannot be written", () -> meta(A7.class));
	}

	@Test void a08_ignoredRequiredProperty_isIgnored() {
		assertList(meta(A8.class).getRequiredPropertyNames());
	}

	@Test void a09_dynaRequired_failsFast() {
		assertThrowsWithMessage(BeanRuntimeException.class, "cannot be written", () -> meta(A9.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - presence tracking
	//-----------------------------------------------------------------------------------------------------------------

	public static class B1 {
		@BeanProp(required=true) public String a;
		public String b;
		@BeanProp(required=true) public String c;
		@BeanProp(required=true) public java.util.List<String> tags;
	}

	@SuppressWarnings("unchecked")
	private static <T> BeanMap<T> newMap(T bean) {
		return BeanMap.of(bean, (BeanMeta<T>)BeanMeta.of(bean.getClass(), BeanConfigContext.DEFAULT));
	}

	@Test void b01_allMissingInitially() {
		assertList(newMap(new B1()).getMissingRequiredProperties(), "a", "c", "tags");
	}

	@Test void b02_explicitNullCountsAsPresent() {
		var m = newMap(new B1());
		m.put("a", null);
		assertList(m.getMissingRequiredProperties(), "c", "tags");
	}

	@Test void b03_nonRequiredPutDoesNotSatisfy() {
		var m = newMap(new B1());
		m.put("b", "x");
		assertList(m.getMissingRequiredProperties(), "a", "c", "tags");
	}

	@Test void b04_emptyValuesCountAsPresent() {
		var m = newMap(new B1());
		m.put("tags", new java.util.ArrayList<String>());   // empty list counts as present
		m.put("a", "1");
		m.put("c", "");                                     // empty string counts as present
		assertList(m.getMissingRequiredProperties());
	}

	@Test void b05_noRequiredProperties_noTracking() {
		var m = newMap(new A5());
		m.put("a", "x");
		assertList(m.getMissingRequiredProperties());
	}
}
