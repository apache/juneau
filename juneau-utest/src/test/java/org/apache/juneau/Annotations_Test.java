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

import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

class Annotations_Test extends SimpleTestBase {

	//====================================================================================================
	// Bean with explicitly specified properties.
	//====================================================================================================
	@Test void a01_beanWithExplicitProperties() throws Exception {
		var bc = BeanContext.DEFAULT;

		// Basic test
		var bm = bc.newBeanMap(Person1.class).load("{age:21,name:'foobar'}");
		assertBean(bm.getBean(), "name,age", "foobar,21");

		bm.put("age", 65);
		bm.put("name", "futbol");
		assertMap(bm, "name,age", "futbol,65");
		assertBean(bm.getBean(), "name,age", "futbol,65");
	}

	/** Class with explicitly specified properties */
	@Bean(properties="age,name")
	public static class Person1 {
		public int age;

		private String name;
		public String getName() { return name; }
		public void setName(String v) { name = v; }
	}

	@Test void a02_beanWithExplicitProperties2() throws Exception {
		var bc = BeanContext.DEFAULT;

		// Basic test
		var bm = bc.newBeanMap(Person2.class).load("{age:21,name:'foobar'}");
		assertBean(bm.getBean(), "name,age", "foobar,21");

		bm.put("age", 65);
		bm.put("name", "futbol");
		assertMap(bm, "name,age", "futbol,65");
		assertBean(bm.getBean(), "name,age", "futbol,65");
	}

	/** Class with explicitly specified properties */
	@Bean(p="age,name")
	public static class Person2 {
		public int age;

		private String name;
		public String getName() { return name; }
		public void setName(String v) { name = v; }
	}

	@Test void a03_beanWithExplicitProperties3() throws Exception {
		var bc = BeanContext.DEFAULT;

		// Basic test
		var bm = bc.newBeanMap(Person3.class).load("{age:21,name:'foobar'}");
		assertBean(bm.getBean(), "name,age", "foobar,21");

		bm.put("age", 65);
		bm.put("name", "futbol");
		assertMap(bm, "name,age", "futbol,65");
		assertBean(bm.getBean(), "name,age", "futbol,65");
	}

	/** Class with explicitly specified properties */
	@Bean(properties="age",p="name")
	public static class Person3 {
		public int age;

		private String name;
		public String getName() { return name; }
		public void setName(String v) { name = v; }
	}

	@Test void a04_beanWithExplicitProperties_usingConfig() throws Exception {
		var bc = BeanContext.DEFAULT.copy().applyAnnotations(PersonConfig.class).build();

		// Basic test
		var bm = bc.newBeanMap(Person4.class).load("{age:21,name:'foobar'}");
		assertBean(bm.getBean(), "name,age", "foobar,21");

		bm.put("age", 65);
		bm.put("name", "futbol");
		assertMap(bm, "name,age", "futbol,65");
		assertBean(bm.getBean(), "name,age", "futbol,65");
	}

	/** Class with explicitly specified properties */
	public static class Person4 {
		public int age;

		private String name;
		public String getName() { return name; }
		public void setName(String v) { name = v; }
	}

	@Bean(on="Person4",properties="age,name")
	public static class PersonConfig {}

	//====================================================================================================
	// Private/protected/default fields should be ignored.
	//====================================================================================================
	@Test void a05_forOnlyPublicFields() throws Exception {
		var bc = BeanContext.DEFAULT;

		// Make sure only public fields are detected
		var bm = bc.newBeanMap(A.class).load("{publicField:123}");
		assertJson(bm.getBean(), "{publicField:123}");
	}

	public static class A {
		public int publicField;
		protected int protectedField;
		@SuppressWarnings("unused")
		private int privateField;
		int defaultField;
	}
}