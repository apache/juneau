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

import org.apache.juneau.collections.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestOp_ReqHeaders_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Default values - Default request headers
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestOp(defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap a(RequestHeaders headers) {
			return OMap.create()
				.a("h1", headers.getString("H1").orElse(null))
				.a("h2", headers.getString("H2").orElse(null))
				.a("h3", headers.getString("H3").orElse(null));
		}
		@RestGet(defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap b(RequestHeaders headers) {
			return OMap.create()
				.a("h1", headers.getString("H1").orElse(null))
				.a("h2", headers.getString("H2").orElse(null))
				.a("h3", headers.getString("H3").orElse(null));
		}
		@RestPut(defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap c(RequestHeaders headers) {
			return OMap.create()
				.a("h1", headers.getString("H1").orElse(null))
				.a("h2", headers.getString("H2").orElse(null))
				.a("h3", headers.getString("H3").orElse(null));
		}
		@RestPost(defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap d(RequestHeaders headers) {
			return OMap.create()
				.a("h1", headers.getString("H1").orElse(null))
				.a("h2", headers.getString("H2").orElse(null))
				.a("h3", headers.getString("H3").orElse(null));
		}
		@RestDelete(defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap e(RequestHeaders headers) {
			return OMap.create()
				.a("h1", headers.getString("H1").orElse(null))
				.a("h2", headers.getString("H2").orElse(null))
				.a("h3", headers.getString("H3").orElse(null));
		}
	}

	@Test
	public void a01_reqHeaders() throws Exception {
		RestClient a = MockRestClient.build(A.class);

		a.get("/a").run().assertBody().is("{h1:'1',h2:'2',h3:'3'}");
		a.get("/a").header("H1",4).header("H2",5).header("H3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
		a.get("/a").header("h1",4).header("h2",5).header("h3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");

		a.get("/b").run().assertBody().is("{h1:'1',h2:'2',h3:'3'}");
		a.get("/b").header("H1",4).header("H2",5).header("H3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
		a.get("/b").header("h1",4).header("h2",5).header("h3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");

		a.put("/c").run().assertBody().is("{h1:'1',h2:'2',h3:'3'}");
		a.put("/c").header("H1",4).header("H2",5).header("H3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
		a.put("/c").header("h1",4).header("h2",5).header("h3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");

		a.post("/d").run().assertBody().is("{h1:'1',h2:'2',h3:'3'}");
		a.post("/d").header("H1",4).header("H2",5).header("H3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
		a.post("/d").header("h1",4).header("h2",5).header("h3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");

		a.delete("/e").run().assertBody().is("{h1:'1',h2:'2',h3:'3'}");
		a.delete("/e").header("H1",4).header("H2",5).header("H3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
		a.delete("/e").header("h1",4).header("h2",5).header("h3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default values - Default request headers, case-insensitive matching
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet(defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap a(RequestHeaders headers) {
			return OMap.create()
				.a("h1", headers.getString("h1").orElse(null))
				.a("h2", headers.getString("h2").orElse(null))
				.a("h3", headers.getString("h3").orElse(null));
		}
	}

	@Test
	public void b01_reqHeadersCaseInsensitive() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.get("/a").run().assertBody().is("{h1:'1',h2:'2',h3:'3'}");
		b.get("/a").header("H1",4).header("H2",5).header("H3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
		b.get("/a").header("h1",4).header("h2",5).header("h3",6).run().assertBody().is("{h1:'4',h2:'5',h3:'6'}");
	}
}
