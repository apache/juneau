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
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class Rest_Messages_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A1 {
		@RestGet
		public JsonMap a(ResourceBundle rb) {
			return asMap(rb);
		}
		@RestGet
		public JsonMap b(Messages m) {
			return asMap(m);
		}
		@RestGet
		public String c(Messages m, @Query("name") String name) {
			return m.getString(name);
		}
	}

	@Test void a01_default() throws Exception {
		MockRestClient a1 = MockRestClient.build(A1.class);
		a1.get("/a").run().assertContent("{'A1.key2':'A1.value2a',key1:'value1a',key2:'A1.value2a'}");
		a1.get("/b").run().assertContent("{'A1.key2':'A1.value2a',key1:'value1a',key2:'A1.value2a'}");
		a1.get("/c?name=key1").run().assertContent("value1a");
		a1.get("/c?name=key2").run().assertContent("A1.value2a");
		a1.get("/c?name=key3").run().assertContent("{!key3}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Overridden on subclass.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(messages="B1x")
	public static class B1 {
		@RestGet
		public JsonMap a(ResourceBundle rb) {
			return asMap(rb);
		}
		@RestGet
		public JsonMap b(Messages m) {
			return asMap(m);
		}
		@RestGet
		public String c(Messages m, @Query("name") String name) {
			return m.getString(name);
		}
	}

	@Test void b01_customName() throws Exception {
		MockRestClient b1 = MockRestClient.build(B1.class);
		b1.get("/a").run().assertContent("{'B1.key2':'B1.value2a',key1:'value1a',key2:'B1.value2a'}");
		b1.get("/b").run().assertContent("{'B1.key2':'B1.value2a',key1:'value1a',key2:'B1.value2a'}");
		b1.get("/c?name=key1").run().assertContent("value1a");
		b1.get("/c?name=key2").run().assertContent("B1.value2a");
		b1.get("/c?name=key3").run().assertContent("{!key3}");
	}

	@Rest(messages="B2x")
	public static class B2 extends B1 {}

	@Test void b02_subclassed_customName() throws Exception {
		MockRestClient b2 = MockRestClient.build(B2.class);
		b2.get("/a").run().assertContent("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b',key3:'B2.value3b'}");
		b2.get("/b").run().assertContent("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b',key3:'B2.value3b'}");
		b2.get("/c?name=key1").run().assertContent("value1a");
		b2.get("/c?name=key2").run().assertContent("value2b");
		b2.get("/c?name=key3").run().assertContent("B2.value3b");
		b2.get("/c?name=key4").run().assertContent("{!key4}");
	}

	public static class B3 extends B1 {
		@RestInit
		public void init(RestContext.Builder builder) {
			builder.messages().location(null, "B2x").location(B1.class, "B1x");
		}
	}

	@Test void b03_viaBuilder() throws Exception {
		MockRestClient b3 = MockRestClient.build(B3.class);
		b3.get("/a").run().assertContent("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b'}");
		b3.get("/b").run().assertContent("{'B1.key2':'B1.value2a','B2.key3':'B2.value3b',key1:'value1a',key2:'value2b'}");
		b3.get("/c?name=key1").run().assertContent("value1a");
		b3.get("/c?name=key3").run().assertContent("{!key3}");
		b3.get("/c?name=key4").run().assertContent("{!key4}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static JsonMap asMap(ResourceBundle rb) {
		var m = new JsonMap();
		for (String k : new TreeSet<>(rb.keySet()))
			m.put(k, rb.getString(k));
		return m;
	}
}
