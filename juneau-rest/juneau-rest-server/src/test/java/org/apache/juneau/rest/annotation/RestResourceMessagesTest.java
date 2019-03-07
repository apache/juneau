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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestMethod(messages).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourceMessagesTest {

	//====================================================================================================
	// Setup
	//====================================================================================================

	static ObjectMap convertToMap(ResourceBundle rb) {
		ObjectMap m = new ObjectMap();
		for (String k : rb.keySet())
			m.put(k, rb.getString(k));
		return m;
	}

	//====================================================================================================
	// Basic tests
	//====================================================================================================

	@RestResource(messages="RestResourceMessagesTest1")
	public static class A {
		@RestMethod
		public ObjectMap a01(ResourceBundle rb) {
			return convertToMap(rb);
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01() throws Exception {
		// Parent resource should just pick up values from its bundle.
		a.get("/a01").execute().assertBody("{key1:'value1a',key2:'value2a'}");
	}

	//====================================================================================================
	// Overridden on subclass.
	//====================================================================================================

	@RestResource(messages="RestResourceMessagesTest2")
	public static class B extends A {}
	static MockRest b = MockRest.build(B.class, null);

	@Test
	public void b01() throws Exception {
		// Child resource should pick up values from both parent and child,
		// ordered child before parent.
		b.get("/a01").execute().assertBody("{key1:'value1a',key2:'value2b',key3:'value3b'}");
	}
}
