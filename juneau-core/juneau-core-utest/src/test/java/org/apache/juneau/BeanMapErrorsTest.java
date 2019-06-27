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
	// @BeanProperty(name) on method not in @Bean(properties)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void beanPropertyMethodNotInBeanProperties() {
		BeanContext bc = BeanContext.DEFAULT;

		try {
			bc.getClassMeta(A1.class);
			fail();
		} catch (Exception e) {
			assertEquals("org.apache.juneau.BeanMapErrorsTest$A1: Found @BeanProperty(\"f2\") but name was not found in @Bean(properties)", e.getMessage());
		}
	}

	@Bean(properties="f1")
	public static class A1 {
		public int f1;

		@BeanProperty("f2")
		public int f2() {
			return -1;
		};
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @BeanProperty(name) on field not in @Bean(properties)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void beanPropertyFieldNotInBeanProperties() {
		BeanContext bc = BeanContext.DEFAULT;

		try {
			bc.getClassMeta(A2.class);
			fail();
		} catch (Exception e) {
			assertEquals("org.apache.juneau.BeanMapErrorsTest$A2: Found @BeanProperty(\"f2\") but name was not found in @Bean(properties)", e.getMessage());
		}
	}
	@Bean(properties="f1")
	public static class A2 {
		public int f1;

		@BeanProperty("f2")
		public int f2;
	}
}
