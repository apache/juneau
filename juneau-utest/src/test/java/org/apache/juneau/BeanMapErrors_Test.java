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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map.*;

import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Tests various error conditions when defining beans.
 */
class BeanMapErrors_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// @Beanp(name) on method not in @Bean(properties)
	// Shouldn't be found in keySet()/entrySet() but should be found in containsKey()/get()
	//-----------------------------------------------------------------------------------------------------------------
	@Test void beanPropertyMethodNotInBeanProperties() {
		var bc = BeanContext.DEFAULT;

		var bm = bc.newBeanMap(A1.class);
		assertTrue(bm.containsKey("f2"));
		assertEquals(-1, bm.get("f2"));
		bm.put("f2", -2);
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@Bean(p="f1")
	public static class A1 {
		public int f1;

		private int f2 = -1;
		@Beanp("f2") public int f2() { return f2; }
		public void setF2(int v) { f2 = v; }
	}

	@Test void beanPropertyMethodNotInBeanProperties_usingConfig() {
		var bc = BeanContext.create().applyAnnotations(B1Config.class).build();

		var bm = bc.newBeanMap(B1.class);
		assertTrue(bm.containsKey("f2"));
		assertEquals(-1, bm.get("f2"));
		bm.put("f2", -2);
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@Bean(on="Dummy",p="dummy")
	@Bean(on="B1", p="f1")
	@Beanp(on="Dummy", value="dummy")
	@Beanp(on="B1.f2", value="f2")
	private static class B1Config {}

	public static class B1 {
		public int f1;

		private int f2 = -1;
		@Beanp("f2") public int f2() { return f2; }
		public void setF2(int v) { f2 = v; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Beanp(name) on field not in @Bean(properties)
	//-----------------------------------------------------------------------------------------------------------------
	@Test void beanPropertyFieldNotInBeanProperties() {
		var bc = BeanContext.DEFAULT;

		var bm = bc.newBeanMap(A2.class);
		assertTrue(bm.containsKey("f2"));
		assertEquals(-1, bm.get("f2"));
		bm.put("f2", -2);
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@Bean(p="f1")
	public static class A2 {
		public int f1;

		@Beanp("f2")
		public int f2 = -1;
	}

	@Test void beanPropertyFieldNotInBeanProperties_usingBeanConfig() {
		var bc = BeanContext.create().applyAnnotations(B2Config.class).build();

		var bm = bc.newBeanMap(B2.class);
		assertTrue(bm.containsKey("f2"));
		assertEquals(-1, bm.get("f2"));
		bm.put("f2", -2);
		assertEquals(-2, bm.get("f2"));
		assertFalse(bm.keySet().contains("f2"));
		assertFalse(bm.entrySet().stream().map(Entry::getKey).toList().contains("f2"));
	}

	@Bean(on="Dummy",p="dummy")
	@Bean(on="B2", p="f1")
	@Beanp(on="Dummy", value="dummy")
	@Beanp(on="B2.f2", value="f2")
	private static class B2Config {}

	public static class B2 {
		public int f1;
		public int f2 = -1;
	}
}