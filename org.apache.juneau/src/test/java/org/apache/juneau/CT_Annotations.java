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

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.annotation.*;
import org.junit.*;


@SuppressWarnings({"rawtypes"})
public class CT_Annotations {

	//====================================================================================================
	// Bean with explicitly specified properties.
	//====================================================================================================
	@Test
	public void testBeanWithExplicitProperties() throws Exception {
		BeanContext bc = BeanContext.DEFAULT;
		BeanMap bm = null;

		// Basic test
		bm = bc.newBeanMap(Person1.class).load("{age:21,name:'foobar'}");
		assertNotNull(bm);
		assertNotNull(bm.getBean());
		assertEquals(bm.get("age"), 21);
		assertEquals(bm.get("name"), "foobar");

		bm.put("age", 65);
		bm.put("name", "futbol");
		assertEquals(bm.get("age"), 65);
		assertEquals(bm.get("name"), "futbol");
	}

	/** Class with explicitly specified properties */
	@Bean(properties = { "age", "name" })
	public static class Person1 {
		public int age;
		private String name;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}

	//====================================================================================================
	// Private/protected/default fields should be ignored.
	//====================================================================================================
	@Test
	public void testForOnlyPublicFields() throws Exception {
		BeanContext bc = BeanContext.DEFAULT;
		BeanMap bm = null;

		// Make sure only public fields are detected
		bm = bc.newBeanMap(A.class).load("{publicField:123}");
		assertNotNull("F1", bm);
		assertNotNull("F2", bm.getBean());
		assertObjectEquals("{publicField:123}", bm.getBean());

	}

	public static class A {
		public int publicField;
		protected int protectedField;
		@SuppressWarnings("unused")
		private int privateField;
		int defaultField;
	}
}