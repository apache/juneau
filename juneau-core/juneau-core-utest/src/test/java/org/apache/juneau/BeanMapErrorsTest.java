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

import org.apache.juneau.annotation.*;
import org.junit.*;

/**
 * Tests various error conditions when defining beans.
 */
public class BeanMapErrorsTest {

	//-----------------------------------------------------------------------------------------------------------------
	// @Beanp(name) on method not in @Bean(properties)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void beanPropertyMethodNotInBeanProperties() {
		BeanContext bc = BeanContext.DEFAULT;

		try {
			bc.getClassMeta(A1.class);
			fail();
		} catch (Exception e) {
			assertEquals("org.apache.juneau.BeanMapErrorsTest$A1: Found @Beanp(\"f2\") but name was not found in @Bean(properties)", e.getMessage());
		}
	}

	@Bean(bpi="f1")
	public static class A1 {
		public int f1;

		@Beanp("f2")
		public int f2() {
			return -1;
		};
	}

	@Test
	public void beanPropertyMethodNotInBeanProperties_usingConfig() {
		BeanContext bc = BeanContext.create().applyAnnotations(B1.class).build();

		try {
			bc.getClassMeta(B1.class);
			fail();
		} catch (Exception e) {
			assertEquals("org.apache.juneau.BeanMapErrorsTest$B1: Found @Beanp(\"f2\") but name was not found in @Bean(properties)", e.getMessage());
		}
	}

	@BeanConfig(
		applyBean={
			@Bean(on="B1", bpi="f1"),
		},
		applyBeanp={
			@Beanp(on="B1.f2", value="f2")
		}
	)
	public static class B1 {
		public int f1;

		public int f2() {
			return -1;
		};
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Beanp(name) on field not in @Bean(properties)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void beanPropertyFieldNotInBeanProperties() {
		BeanContext bc = BeanContext.DEFAULT;

		try {
			bc.getClassMeta(A2.class);
			fail();
		} catch (Exception e) {
			assertEquals("org.apache.juneau.BeanMapErrorsTest$A2: Found @Beanp(\"f2\") but name was not found in @Bean(properties)", e.getMessage());
		}
	}
	@Bean(bpi="f1")
	public static class A2 {
		public int f1;

		@Beanp("f2")
		public int f2;
	}

	@Test
	public void beanPropertyFieldNotInBeanProperties_usingBeanConfig() {
		BeanContext bc = BeanContext.create().applyAnnotations(B2.class).build();

		try {
			bc.getClassMeta(B2.class);
			fail();
		} catch (Exception e) {
			assertEquals("org.apache.juneau.BeanMapErrorsTest$B2: Found @Beanp(\"f2\") but name was not found in @Bean(properties)", e.getMessage());
		}
	}

	@BeanConfig(
		applyBean={
			@Bean(on="B2", bpi="f1")
		},
		applyBeanp={
			@Beanp(on="B2.f2", value="f2")
		}
	)
	public static class B2 {
		public int f1;

		public int f2;
	}
}
