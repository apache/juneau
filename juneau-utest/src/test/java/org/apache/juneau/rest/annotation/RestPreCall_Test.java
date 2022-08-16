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

import javax.servlet.http.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestPreCall_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @RestPreCall
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A extends A_Parent {
		private boolean pre3Called;
		@RestPreCall
		public void pre3() {
			pre3Called = true;
		}
		@RestPreCall
		public void pre4(HttpServletRequest req, HttpServletResponse res) {
			res.setHeader("pre3-called", ""+pre3Called);
			pre3Called = false;
			if (res.getHeader("pre4-called") != null)
				throw new RuntimeException("pre4 called multiple times.");
			res.setHeader("pre4-called", "true");
		}
		@RestGet(path="/")
		public JsonMap a(RestRequest req, RestResponse res) {
			return JsonMap.create()
				.append("1", res.getHeader("pre1-called"))
				.append("2", res.getHeader("pre2-called"))
				.append("3", res.getHeader("pre3-called"))
				.append("4", res.getHeader("pre4-called"));
		}
	}

	public static class A_Parent {
		private boolean pre1Called;
		@RestPreCall
		public void pre1() {
			pre1Called = true;
		}
		@RestPreCall
		public void pre2(Accept accept, RestRequest req, RestResponse res) {
			res.setHeader("pre1-called", ""+pre1Called);
			pre1Called = false;
			if (res.getHeader("pre2-called") != null)
				throw new RuntimeException("pre2 called multiple times.");
			res.setHeader("pre2-called", "true");
		}
	}

	@Test
	public void a01_preCall() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/").run().assertContent("{'1':'true','2':'true','3':'true','4':'true'}");
	}
}
