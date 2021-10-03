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

import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_Messages_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A1 {
		@RestGet
		public OMap a(ResourceBundle rb) {
			return asMap(rb);
		}
		@RestGet
		public OMap b(Messages m) {
			return asMap(m);
		}
		@RestGet
		public String c(Messages m, @Query("name") String name) {
			return m.getString(name);
		}
	}

	@Test
	public void a01_default() throws Exception {
		MockRestClient a1 = MockRestClient.build(A1.class);
		a1.get("/a").run().assertBody().is("{'A1.key2':'A1.value2a',key1:'value1a',key2:'A1.value2a'}");
		a1.get("/b").run().assertBody().is("{'A1.key2':'A1.value2a',key1:'value1a',key2:'A1.value2a'}");
		a1.get("/c?name=key1").run().assertBody().is("value1a");
		a1.get("/c?name=key2").run().assertBody().is("A1.value2a");
		a1.get("/c?name=key3").run().assertBody().is("{!key3}");
	}

	@Rest
	public static class A2 extends A1 {}

	@Test
	public void a02_subclassed() throws Exception {
		MockRestClient a2 = MockRestClient.build(A2.class);
		a2.get("/a").run().assertBody().is("{'A1.key2':'A1.value2a','A2.key3':'A2.value3b',key1:'value1a',key2:'value2b',key3:'A2.value3b'}");
		a2.get("/b").run().assertBody().is("{'A1.key2':'A1.value2a','A2.key3':'A2.value3b',key1:'value1a',key2:'value2b',key3:'A2.value3b'}");
		a2.get("/c?name=key1").run().assertBody().is("value1a");
		a2.get("/c?name=key2").run().assertBody().is("value2b");
		a2.get("/c?name=key3").run().assertBody().is("A2.value3b");
		a2.get("/c?name=key4").run().assertBody().is("{!key4}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Overridden on subclass.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(messages="B1x")
	public static class B1 {
		@RestGet
		public OMap a(ResourceBundle rb) {
			return asMap(rb);
		}
		@RestGet
		public OMap b(Messages m) {
			return asMap(m);
		}
		@RestGet
		public String c(Messages m, @Query("name") String name) {
			return m.getString(name);
		}
	}

	@Test
	public void b01_customName() throws Exception {
		MockRestClient b1 = MockRestClient.build(B1.class);
		b1.get("/a").run().assertBody().is("{'B1.key2':'B1.value2a',key1:'value1a',key2:'B1.value2a'}");
		b1.get("/b").run().assertBody().is("{'B1.key2':'B1.value2a',key1:'value1a',key2:'B1.value2a'}");
		b1.get("/c?name=key1").run().assertBody().is("value1a");
		b1.get("/c?name=key2").run().assertBody().is("B1.value2a");
		b1.get("/c?name=key3").run().assertBody().is("{!key3}");
	}

	@Rest(messages="B2x")
	public static class B2 extends B1 {}

	@Test
	public void b02_subclassed_customName() throws Exception {
		MockRestClient b2 = MockRestClient.build(B2.class);
		b2.get("/a").run().assertBody().is("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b',key3:'B2.value3b'}");
		b2.get("/b").run().assertBody().is("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b',key3:'B2.value3b'}");
		b2.get("/c?name=key1").run().assertBody().is("value1a");
		b2.get("/c?name=key2").run().assertBody().is("value2b");
		b2.get("/c?name=key3").run().assertBody().is("B2.value3b");
		b2.get("/c?name=key4").run().assertBody().is("{!key4}");
	}

	public static class B3 extends B1 {
		 @RestHook(HookEvent.INIT)
		 public void init(RestContext.Builder builder) throws Exception {
			 builder.messages().location(null, "B2x").location(B1.class, "B1x");
		 }
	}

	@Test
	public void b03_viaBuilder() throws Exception {
		MockRestClient b3 = MockRestClient.build(B3.class);
		b3.get("/a").run().assertBody().is("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b'}");
		b3.get("/b").run().assertBody().is("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b'}");
		b3.get("/c?name=key1").run().assertBody().is("value1a");
		b3.get("/c?name=key2").run().assertBody().is("value2b");
		b3.get("/c?name=key3").run().assertBody().is("{!key3}");
		b3.get("/c?name=key4").run().assertBody().is("{!key4}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static OMap asMap(ResourceBundle rb) {
		OMap m = new OMap();
		for (String k : new TreeSet<>(rb.keySet()))
			m.put(k, rb.getString(k));
		return m;
	}
}
