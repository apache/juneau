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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class BeanPropertyValue_Test extends TestBase {

	public static class TestBean {
		public String name;
		public int age;
		public boolean active;
	}

	BeanContext bc = BeanContext.DEFAULT;

	//====================================================================================================
	// compareTo() - Line 51 coverage
	//====================================================================================================

	@Test void a01_compareTo_lessThan() {
		var bm = bc.getBeanMeta(TestBean.class);
		var pMeta1 = bm.getPropertyMeta("active");
		var pMeta2 = bm.getPropertyMeta("age");

		var pv1 = new BeanPropertyValue(pMeta1, "active", true, null);
		var pv2 = new BeanPropertyValue(pMeta2, "age", 25, null);

		assertTrue(pv1.compareTo(pv2) < 0);
	}

	@Test void a02_compareTo_greaterThan() {
		var bm = bc.getBeanMeta(TestBean.class);
		var pMeta1 = bm.getPropertyMeta("name");
		var pMeta2 = bm.getPropertyMeta("age");

		var pv1 = new BeanPropertyValue(pMeta1, "name", "John", null);
		var pv2 = new BeanPropertyValue(pMeta2, "age", 25, null);

		assertTrue(pv1.compareTo(pv2) > 0);
	}

	@Test void a03_compareTo_equal() {
		var bm = bc.getBeanMeta(TestBean.class);
		var pMeta = bm.getPropertyMeta("name");

		var pv1 = new BeanPropertyValue(pMeta, "name", "John", null);
		var pv2 = new BeanPropertyValue(pMeta, "name", "Jane", null);

		assertEquals(0, pv1.compareTo(pv2));
	}

	//====================================================================================================
	// properties() and toString() - Lines 89-100 coverage
	//====================================================================================================

	@Test void b01_properties() {
		var bm = bc.getBeanMeta(TestBean.class);
		var pMeta = bm.getPropertyMeta("name");
		var pv = new BeanPropertyValue(pMeta, "name", "John", null);

		var props = pv.properties();
		assertEquals("name", props.get("name"));
		assertEquals("John", props.get("value"));
		assertNotNull(props.get("type"));
	}

	@Test void b02_toString() {
		var bm = bc.getBeanMeta(TestBean.class);
		var pMeta = bm.getPropertyMeta("name");
		var pv = new BeanPropertyValue(pMeta, "name", "John", null);

		var str = pv.toString();
		assertNotNull(str);
		assertTrue(str.contains("name"));
		assertTrue(str.contains("John"));
	}

	@Test void b03_toString_withNullValue() {
		var bm = bc.getBeanMeta(TestBean.class);
		var pMeta = bm.getPropertyMeta("name");
		var pv = new BeanPropertyValue(pMeta, "name", null, null);

		var str = pv.toString();
		assertNotNull(str);
		assertTrue(str.contains("name"));
	}
}

