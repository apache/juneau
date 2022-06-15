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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_Context_Test {

	public static class X1 extends RestContext {
		public X1(RestContext.Builder builder) throws Exception {
			super(builder);
		}
	}

	@Rest
	public static class A1 {
		@RestGet
		public String get(RestContext context) {
			return context.getClass().getSimpleName();
		}
	}

	@Test
	public void a01_default() throws Exception {
		MockRestClient a1 = client(A1.class);
		a1.get().run().assertContent().is("RestContext");
	}

	@Rest(contextClass=X1.class)
	public static class A2 extends A1 {}

	@Test
	public void a02_custom() throws Exception {
		MockRestClient a2 = client(A2.class);
		a2.get().run().assertContent().is("X1");
	}

	@Rest
	public static class A3 extends A2 {}

	@Test
	public void a03_notOverriddenByChild() throws Exception {
		MockRestClient a3 = client(A3.class);
		a3.get().run().assertContent().is("X1");
	}

	@Rest
	public static class A4 extends A1 {
		 @RestHook(HookEvent.INIT)
		 public void init(RestContext.Builder builder) throws Exception {
			 builder.type(X1.class);
		 }
	}

	@Test
	public void a04_definedInBuilder() throws Exception {
		MockRestClient a4 = client(A4.class);
		a4.get().run().assertContent().is("X1");
	}


	public static class X2 extends RestContext {
		public X2() throws Exception {
			super(null);
		}
	}

	@Rest(contextClass=X2.class)
	public static class A5 {
		@RestGet
		public String get(RestContext context) {
			return context.getClass().getSimpleName();
		}
	}

	@Test
	public void a05_invalidConstructor() throws Exception {
		assertThrown(()->client(A5.class)).asMessages().isContains("Could not instantiate RestContext.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------------------------------------

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).build();
	}
}
