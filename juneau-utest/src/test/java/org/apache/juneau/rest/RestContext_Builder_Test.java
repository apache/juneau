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
package org.apache.juneau.rest;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestContext_Builder_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// beanStore
	//-----------------------------------------------------------------------------------------------------------------

	public static class Foo {}

	@Rest
	public static class A1 {
		@RestBean static BeanStore beanStore;
	}

	@Test
	public void a01_createBeanStore_default() {
		MockRestClient.buildLax(A1.class);
		assertString(A1.beanStore.getClass().getSimpleName()).is("BeanStore");
	}

	public static class MyBeanStore extends BeanStore {
		protected MyBeanStore(Builder builder) {
			super(builder.parent(BeanStore.create().build().addBean(Foo.class, new Foo())));
		}
	}

	@Rest(beanStore=MyBeanStore.class)
	public static class A2 {
		@RestBean static BeanStore beanStore;
	}

	@Test
	public void a02_createBeanStore_annotation() {
		MockRestClient.buildLax(A2.class);
		assertObject(A2.beanStore.getBean(Foo.class)).isNotNull();
	}

	@Rest
	public static class A3 {
		@RestBean static BeanStore beanStore;

		@RestBean BeanStore.Builder beanStore(BeanStore.Builder b) {
			return b.type(MyBeanStore.class);
		}
	}

	@Test
	public void a03_createBeanStore_restBean1() {
		MockRestClient.buildLax(A3.class);
		assertObject(A3.beanStore.getBean(Foo.class)).isNotNull();
	}

	@Rest
	public static class A4 {
		@RestBean static BeanStore beanStore;

		@RestBean BeanStore beanStore() {
			return BeanStore.create().type(MyBeanStore.class).build();
		}
	}

	@Test
	public void a04_createBeanStore_restBean2() {
		MockRestClient.buildLax(A4.class);
		assertObject(A4.beanStore.getBean(Foo.class)).isNotNull();
	}
}
