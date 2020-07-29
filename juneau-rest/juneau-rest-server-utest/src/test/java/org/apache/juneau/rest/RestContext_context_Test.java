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

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestContext_context_Test {

	public static class MyRestContext extends RestContext {
		public MyRestContext(RestContextBuilder builder) throws Exception {
			super(builder);
		}
	}

	@Rest
	public static class A1 {
		@RestMethod
		public String get(RestContext context) {
			return context.getClass().getSimpleName();
		}
	}

	@Test
	public void a01_default() throws Exception {
		MockRestClient x = client(A1.class);
		x.get().run().assertBody().is("RestContext");
	}

	@Rest(context=MyRestContext.class)
	public static class A2 extends A1 {}

	@Test
	public void a02_custom() throws Exception {
		MockRestClient x = client(A2.class);
		x.get().run().assertBody().is("MyRestContext");
	}

	@Rest
	public static class A3 extends A2 {}

	@Test
	public void a03_notOverriddenByChild() throws Exception {
		MockRestClient x = client(A3.class);
		x.get().run().assertBody().is("MyRestContext");
	}

	@Rest
	public static class A4 extends A1 {
		 @RestHook(HookEvent.INIT)
		 public void init(RestContextBuilder builder) throws Exception {
			 builder.context(MyRestContext.class);
		 }
	}

	@Test
	public void a04_definedInBuilder() throws Exception {
		MockRestClient x = client(A4.class);
		x.get().run().assertBody().is("MyRestContext");
	}


	public static class MyBadRestContext extends RestContext {
		public MyBadRestContext() throws Exception {
			super(null);
		}
	}

	@Rest(context=MyBadRestContext.class)
	public static class A5 {
		@RestMethod
		public String get(RestContext context) {
			return context.getClass().getSimpleName();
		}
	}

	@Test
	public void a05_invalidConstructor() throws Exception {
		assertThrown(()->client(A5.class)).contains("Invalid class specified for REST_context");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------------------------------------

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).build();
	}
}
